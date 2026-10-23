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

    data class UiState(
        val loading: Boolean = true,
        val properties: List<PropertyItem> = emptyList(),
        val stats: PropertyStatsData = PropertyStatsData(),
        val error: String? = null,
        val selectedTab: PropertyTabType = PropertyTabType.ALL,
        val searchQuery: String = "",
        val availableBuildings: List<FilterOption> = emptyList(),
        val availableFloors: List<Int> = emptyList(),
        val availableRooms: List<Int> = emptyList(),
        val filters: PropertyFilters = PropertyFilters(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    // Raw data caches
    private var allApartments: List<ApartmentDto> = emptyList()
    private var allGarages: List<GarageDto> = emptyList()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState(loading = true)

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

                // Calculate stats
                val stats = calculateStats(allApartments, allGarages)

                // Convert to UI items
                val properties = combineProperties(allApartments, allGarages)

                // Extract filter options
                val buildings = allApartments
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
                    properties = properties,
                    stats = stats,
                    error = null,
                    availableBuildings = buildings,
                    availableFloors = floors,
                    availableRooms = rooms,
                )
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
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
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

            // Owner filter
            val matchesOwner = !filters.hasOwnerOnly || apartment.currentOwner != null

            matchesBuilding && matchesFloor && matchesRoom && matchesArea &&
                    matchesStatus && matchesOwner
        }

        // Filter garages
        val filteredGarages = allGarages.filter { garage ->
            // Status filter
            val matchesStatus = filters.status == null ||
                    (filters.status == "occupied" && garage.owner != null) ||
                    (filters.status == "vacant" && garage.owner == null)

            // Owner filter
            val matchesOwner = !filters.hasOwnerOnly || garage.owner != null

            matchesStatus && matchesOwner
        }

        // Combine and convert to UI items
        val allProperties = combineProperties(filteredApartments, filteredGarages)

        // Apply tab and search filters
        val filtered = allProperties.filter { property ->
            // Tab filter
            val matchesTab = when (tab) {
                PropertyTabType.ALL -> true
                PropertyTabType.APARTMENTS -> property.type == PropertyItemType.APARTMENT
                PropertyTabType.GARAGES -> property.type == PropertyItemType.GARAGE
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
    val totalApartments: Int = 0,
    val totalGarages: Int = 0,
    val occupancyRate: Int = 0,
    val overdue: Int = 0,
)

enum class PropertyTabType {
    ALL,
    APARTMENTS,
    GARAGES,
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
)

data class FilterOption(
    val id: String,
    val name: String,
)
