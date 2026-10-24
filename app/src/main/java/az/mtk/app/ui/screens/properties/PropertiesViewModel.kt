package az.mtk.app.ui.screens.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.*
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PropertiesViewModel : ViewModel() {
    private val buildingsApi = AppGraph.service(BuildingsApi::class.java)
    private val service = AppGraph.service(MtkService::class.java)

    data class UiState(
        val loading: Boolean = true,
        val properties: List<PropertyItem> = emptyList(),
        val stats: PropertyStatsData = PropertyStatsData(),
        val error: String? = null,
        val selectedTab: PropertyTabType = PropertyTabType.APARTMENTS,
        val searchQuery: String = "",
        val availableBuildings: List<FilterOption> = emptyList(),
        val availableFloors: List<Int> = emptyList(),
        val availableRooms: List<Int> = emptyList(),
        val filters: PropertyFilters = PropertyFilters(),
        // Binalar tab — the building register (GET api/buildings/buildings).
        val buildings: List<BuildingDto> = emptyList(),
        val buildingsLoading: Boolean = false,
        val buildingsError: String? = null,
        val buildingsQuery: String = "",
        /** Bina id-si → binadakı mənzil sayı (Binalar tabındaki kart üçün). */
        val apartmentCounts: Map<String, Int> = emptyMap(),
        // Sakinlər (owners) tab — separate paged endpoint, loaded on demand.
        val residents: List<OwnerItemDto> = emptyList(),
        val residentsTotal: Int = 0,
        val residentsLoading: Boolean = false,
        val residentsError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    // Raw data caches
    private var allApartments: List<ApartmentDto> = emptyList()
    private var allGarages: List<GarageDto> = emptyList()

    fun load() {
        viewModelScope.launch {
            // Tab/arama və filtrləri saxlayırıq — ekrana qayıdanda istifadəçi
            // yenidən "Mənzillər"ə atılmır.
            _state.value = _state.value.copy(loading = true, error = null)

            try {
                // Load apartments
                val apartmentsResult = buildingsApi.searchApartments(
                    SearchApartmentsRequest(pageSize = 1000)
                ).data ?: SearchApartmentsResult(emptyList(), 0, 0, 0)
                allApartments = apartmentsResult.items.orEmpty()

                // Load garages
                val garagesResult = buildingsApi.searchGarages(
                    SearchGaragesRequest(pageSize = 1000)
                ).data ?: SearchGaragesResult(emptyList(), 0, 0, 0)
                allGarages = garagesResult.items.orEmpty()

                // Load the building register. A failure here must not blank the
                // apartment/garage lists — the tab reports its own error.
                _state.value = _state.value.copy(buildingsLoading = true)
                val buildings = runCatching { service.buildings() }.getOrNull()

                // Calculate stats
                val stats = calculateStats(allApartments, allGarages, buildings.orEmpty())

                // Extract filter options
                val buildingsOptions = allApartments
                    .mapNotNull { it.building }
                    .mapNotNull { building ->
                        val id = building.id ?: return@mapNotNull null
                        val name = building.name ?: ""
                        FilterOption(id, name)
                    }
                    .distinctBy { it.id }
                    .sortedBy { it.name }

                val floors = allApartments
                    .mapNotNull { it.floor }
                    .distinct()
                    .sorted()

                val rooms = allApartments
                    .mapNotNull { it.roomCount }
                    .distinct()
                    .sorted()

                _state.value = _state.value.copy(
                    loading = false,
                    stats = stats,
                    error = null,
                    availableBuildings = buildingsOptions,
                    availableFloors = floors,
                    availableRooms = rooms,
                    buildings = buildings.orEmpty(),
                    buildingsLoading = false,
                    buildingsError = if (buildings == null) "Binaları yükləmək mümkün olmadı." else null,
                    apartmentCounts = allApartments
                        .mapNotNull { it.building?.id }
                        .groupingBy { it }
                        .eachCount(),
                )

                // Seçilmiş tab və axtarış/filtr tətbiq olunur.
                applyFilters()

                // Sakinlər tabının sayı dərhal görünsün deyə ilk səhifəni əvvəlcədən yükləyirik.
                loadResidents()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toUserMessage(),
                )
            }
        }
    }

    fun setTab(tab: PropertyTabType) {
        _state.value = _state.value.copy(selectedTab = tab)
        applyFilters()
        if (tab == PropertyTabType.RESIDENTS) loadResidents()
    }

    /** Loads the owners list shown in the "Sakinlər" tab (first page). */
    fun loadResidents(force: Boolean = false) {
        val state = _state.value
        if (state.residentsLoading) return
        if (!force && state.residents.isNotEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(residentsLoading = true, residentsError = null)
            val page = runCatching {
                service.owners(SearchBody(page = 0, pageSize = 200)).data
            }.getOrNull()

            _state.value = _state.value.copy(
                residents = page?.items.orEmpty(),
                residentsTotal = page?.totalCount ?: 0,
                residentsLoading = false,
                residentsError = if (page == null) "Sakinləri yükləmək mümkün olmadı." else null,
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setBuildingsQuery(query: String) {
        _state.value = _state.value.copy(buildingsQuery = query)
    }

    /** Reloads after a create action (building / apartment / garage / owner). */
    fun refresh() {
        loadResidents(force = true)
        load()
    }

    fun setFilters(filters: PropertyFilters) {
        println("🔍 Filter tətbiq olunur: buildings=${filters.buildingIds.size}, floors=${filters.floors.size}, rooms=${filters.roomCounts.size}, status=${filters.status}, hasOwner=${filters.hasOwnerOnly}")
        _state.value = _state.value.copy(filters = filters)
        applyFilters()
    }

    private fun applyFilters() {
        val tab = _state.value.selectedTab
        val query = _state.value.searchQuery.lowercase().trim()
        val filters = _state.value.filters

        // First filter apartments by criteria
        val filteredApartments = allApartments.filter { apartment ->
            // Building filter
            val matchesBuilding = filters.buildingIds.isEmpty() ||
                    apartment.building?.id in filters.buildingIds

            // Floor filter
            val matchesFloor = filters.floors.isEmpty() ||
                    apartment.floor in filters.floors

            // Room filter
            val matchesRoom = filters.roomCounts.isEmpty() ||
                    apartment.roomCount in filters.roomCounts

            // Area filter
            val matchesArea = apartment.areaSquareMeters?.let { area ->
                area >= filters.minArea && area <= filters.maxArea
            } ?: true

            // Status filter
            val matchesStatus = filters.status == null ||
                    apartment.status?.lowercase() == filters.status.lowercase()

            val matchesHasOwner = !filters.hasOwnerOnly || apartment.currentOwner != null

            // Owner filter
            val matchesOwner = when {
                // "Sahibsiz" + konkret sahiblər birlikdə seçilibsə, backend-də
                // OR filtri olmadığı üçün UI da bu halda sahib filtrini tətbiq
                // etmir — eyni davranış burada saxlanılır.
                filters.ownerIds.isNotEmpty() && filters.noOwner -> true
                filters.ownerIds.isNotEmpty() -> apartment.currentOwner?.id in filters.ownerIds
                filters.noOwner -> apartment.currentOwner == null
                else -> true
            }

            matchesBuilding && matchesFloor && matchesRoom && matchesArea &&
                    matchesStatus && matchesOwner && matchesHasOwner
        }

        // Filter garages
        val filteredGarages = allGarages.filter { garage ->
            // Status filter
            val matchesStatus = filters.status == null ||
                    (filters.status == "occupied" && garage.owner != null) ||
                    (filters.status == "vacant" && garage.owner == null)

            // Owner filter
            val matchesOwner = when {
                filters.ownerIds.isNotEmpty() && filters.noOwner -> true
                filters.ownerIds.isNotEmpty() -> garage.owner?.id in filters.ownerIds
                filters.noOwner -> garage.owner == null
                else -> true
            }

            matchesStatus && matchesOwner &&
                    (!filters.hasOwnerOnly || garage.owner != null)
        }

        // Combine and convert to UI items
        val allProperties = combineProperties(filteredApartments, filteredGarages)

        // Apply tab and search filters
        val filtered = allProperties.filter { property ->
            // Tab filter
            val matchesTab = when (tab) {
                PropertyTabType.APARTMENTS -> property.type == PropertyItemType.APARTMENT
                PropertyTabType.GARAGES -> property.type == PropertyItemType.GARAGE
                // Binalar və Sakinlər tabları ayrı siyahılar göstərir.
                PropertyTabType.BUILDINGS -> false
                PropertyTabType.RESIDENTS -> false
            }

            // Search filter
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                property.title.lowercase().contains(query) ||
                property.subtitle.lowercase().contains(query) ||
                property.ownerName?.lowercase()?.contains(query) == true ||
                property.ownerPhone?.contains(query) == true
            }

            matchesTab && matchesSearch
        }

        _state.value = _state.value.copy(properties = filtered)
    }

    private fun combineProperties(
        apartments: List<ApartmentDto>,
        garages: List<GarageDto>,
    ): List<PropertyItem> {
        val apartmentItems = apartments.mapNotNull { apartment ->
            val buildingName = apartment.building?.name ?: return@mapNotNull null
            val apartmentNumber = apartment.apartmentNumber ?: return@mapNotNull null
            val floor = apartment.floor ?: return@mapNotNull null
            val roomCount = apartment.roomCount ?: return@mapNotNull null
            val area = apartment.areaSquareMeters ?: return@mapNotNull null

            PropertyItem(
                id = apartment.id,
                type = PropertyItemType.APARTMENT,
                title = "$buildingName · Mənzil $apartmentNumber",
                subtitle = "$floor-ci mərtəbə · $roomCount otaq · ${formatArea(area)}",
                badges = buildApartmentBadges(apartment),
                ownerName = apartment.currentOwner?.name,
                ownerPhone = null,
                buildingName = buildingName,
                buildingId = apartment.building?.id,
                number = apartmentNumber,
                floor = floor,
                roomCount = roomCount,
                area = area,
                status = apartment.status,
                ownerId = apartment.currentOwner?.id,
            ).also {
                println("🏢 Created apartment PropertyItem: ID=${it.id}, title=${it.title}")
            }
        }

        val garageItems = garages.mapNotNull { garage ->
            val garageNumber = garage.garageNumber ?: return@mapNotNull null
            val type = garage.type ?: "unknown"

            PropertyItem(
                id = garage.id,
                type = PropertyItemType.GARAGE,
                title = "Qaraj $garageNumber",
                subtitle = formatGarageType(type) + if (garage.description.isNullOrBlank()) "" else " · ${garage.description}",
                badges = buildGarageBadges(garage),
                ownerName = garage.owner?.name,
                ownerPhone = null,
                buildingName = null,
                number = garageNumber,
                status = if (garage.owner != null) "occupied" else "vacant",
                ownerId = garage.owner?.id,
            ).also {
                println("🚗 Created garage PropertyItem: ID=${it.id}, title=${it.title}")
            }
        }

        return (apartmentItems + garageItems).sortedBy { it.title }
    }

    private fun buildApartmentBadges(apartment: ApartmentDto): List<PropertyBadgeItem> {
        val badges = mutableListOf<PropertyBadgeItem>()

        // Status badge
        when (apartment.status?.lowercase()) {
            "occupied" -> badges.add(
                PropertyBadgeItem(
                    text = "Məskunlaşıb",
                    type = PropertyBadgeType.SUCCESS,
                )
            )
            "vacant" -> badges.add(
                PropertyBadgeItem(
                    text = "Boş",
                    type = PropertyBadgeType.WARNING,
                )
            )
        }

        // Owner badge
        if (apartment.currentOwner == null) {
            badges.add(
                PropertyBadgeItem(
                    text = "Sahibsiz",
                    type = PropertyBadgeType.ERROR,
                )
            )
        }

        return badges
    }

    private fun buildGarageBadges(garage: GarageDto): List<PropertyBadgeItem> {
        val badges = mutableListOf<PropertyBadgeItem>()

        if (garage.owner != null) {
            badges.add(
                PropertyBadgeItem(
                    text = "Aktiv",
                    type = PropertyBadgeType.SUCCESS,
                )
            )
        } else {
            badges.add(
                PropertyBadgeItem(
                    text = "Boş",
                    type = PropertyBadgeType.NEUTRAL,
                )
            )
        }

        return badges
    }

    private fun calculateStats(
        apartments: List<ApartmentDto>,
        garages: List<GarageDto>,
        buildings: List<BuildingDto>,
    ): PropertyStatsData {
        val totalApartments = apartments.size
        val totalGarages = garages.size
        val occupiedApartments = apartments.count { it.currentOwner != null }
        val occupancyRate = if (totalApartments > 0) {
            (occupiedApartments * 100) / totalApartments
        } else {
            0
        }

        return PropertyStatsData(
            totalBuildings = buildings.size,
            totalApartments = totalApartments,
            totalGarages = totalGarages,
            occupancyRate = occupancyRate,
            overdue = 0, // TODO: Calculate from payments API
        )
    }

    private fun formatArea(area: Double): String {
        return "${area.toInt()} m²"
    }

    private fun formatGarageType(type: String): String {
        return when (type.lowercase()) {
            "openparking" -> "Açıq dayanacaq"
            "coveredgarage" -> "Örtülü qaraj"
            "storage" -> "Anbar yeri"
            else -> type
        }
    }
}

// UI Data Models

data class PropertyStatsData(
    val totalBuildings: Int = 0,
    val totalApartments: Int = 0,
    val totalGarages: Int = 0,
    val occupancyRate: Int = 0,
    val overdue: Int = 0,
)

enum class PropertyTabType {
    BUILDINGS,
    APARTMENTS,
    GARAGES,
    RESIDENTS,
}

enum class PropertyItemType {
    APARTMENT,
    GARAGE,
}

data class PropertyItem(
    val id: String,
    val type: PropertyItemType,
    val title: String,
    val subtitle: String,
    val badges: List<PropertyBadgeItem>,
    val ownerName: String?,
    val ownerPhone: String?,
    val buildingName: String?,
    // Sorting keys for the list toolbar (Bina, Nömrə, Mərtəbə, Otaq, Sahə, Status).
    val buildingId: String? = null,
    val number: String? = null,
    val floor: Int? = null,
    val roomCount: Int? = null,
    val area: Double? = null,
    val status: String? = null,
    val ownerId: String? = null,
)

data class PropertyBadgeItem(
    val text: String,
    val type: PropertyBadgeType,
)

enum class PropertyBadgeType {
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL,
}

data class PropertyFilters(
    val buildingIds: List<String> = emptyList(),
    val floors: List<Int> = emptyList(),
    val roomCounts: List<Int> = emptyList(),
    val minArea: Double = 0.0,
    val maxArea: Double = 1000.0,
    val status: String? = null,
    val hasOwnerOnly: Boolean = false,
    /** Konkret sahib(lər); boşdursa sahib üzrə filtr yoxdur. */
    val ownerIds: List<String> = emptyList(),
    /** Yalnız sahibsiz əmlaklar (UI-dakı "Sahibsiz" seçimi). */
    val noOwner: Boolean = false,
)

data class FilterOption(
    val id: String,
    val name: String,
)
