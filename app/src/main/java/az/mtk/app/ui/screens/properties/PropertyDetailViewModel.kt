package az.mtk.app.ui.screens.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.*
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detail page for one apartment/garage. Mirrors MTK-UI's
 * `ApartmentDetailView` + `usePropertyFinance()` + `useCreatedBy()`:
 *
 *  - the property itself (building, floor, rooms, area, status, owner),
 *  - the authoritative property balance (`/balances/property/{id}`) — not the
 *    owner-level balance, which spans every unit the owner has,
 *  - the charges billed to *this* property and the payments targeted at it,
 *  - the ownership history, plus who performed each transfer,
 *  - who created the property record (audit log "Created" row),
 *  - and the actions the page offers: assign owner, transfer ownership
 *    (blocked while the property still owes money) and register a payment.
 */
class PropertyDetailViewModel(
    initialPropertyId: String,
    initialPropertyType: PropertyType,
) : ViewModel() {

    private var currentPropertyId = initialPropertyId
    private var currentPropertyType = initialPropertyType

    private val buildingsApi = AppGraph.service(BuildingsApi::class.java)
    private val paymentsApi = AppGraph.service(PaymentsApi::class.java)
    private val adminApi = AppGraph.service(AdminApi::class.java)
    private val service = AppGraph.service(MtkService::class.java)

    private var ownerSearchJob: Job? = null

    data class UiState(
        val loading: Boolean = true,
        val property: PropertyDetail? = null,
        val error: String? = null,
        val ownershipHistory: List<OwnershipRecordDto> = emptyList(),
        val charges: List<ChargeDto> = emptyList(),
        val payments: List<PaymentDto> = emptyList(),
        /** OwnershipHistory id -> name of the user who performed the transfer. */
        val transferCreators: Map<String, String> = emptyMap(),
        /** Charge opened in the detail sheet, plus its allocation rows (null = still loading). */
        val selectedCharge: ChargeDto? = null,
        val chargeAllocations: List<ChargeAllocationDto>? = null,
        /** Owner picker search state (assign / transfer dialogs). */
        val ownerQuery: String = "",
        val ownerResults: List<OwnerItemDto> = emptyList(),
        val ownersLoading: Boolean = false,
        /** Debt gate for the transfer dialog. */
        val debtChecking: Boolean = false,
        val outstandingDebt: Double? = null,
        val debtError: String? = null,
        /** Write operations (assign / transfer / pay). */
        val saving: Boolean = false,
        val actionError: String? = null,
        val actionMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load(
        propertyId: String = currentPropertyId,
        propertyType: PropertyType = currentPropertyType,
        silent: Boolean = false,
    ) {
        currentPropertyId = propertyId
        currentPropertyType = propertyType

        viewModelScope.launch {
            if (silent) _state.value = _state.value.copy(error = null)
            else _state.value = UiState(loading = true)

            try {
                val property = when (currentPropertyType) {
                    PropertyType.APARTMENT -> loadApartmentDetail(currentPropertyId)
                    PropertyType.GARAGE -> loadGarageDetail(currentPropertyId)
                }

                val ownershipHistory = if (currentPropertyType == PropertyType.APARTMENT) {
                    runCatching { buildingsApi.getOwnershipHistory(currentPropertyId) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                // This property's own charges, not every charge of its owner —
                // same filter the web page applies. The endpoint returns a plain
                // array (MTK-UI: Envelope<ChargeResponse[]>), not a paged object.
                val ownerId = property.ownerId
                val charges = if (ownerId != null) {
                    runCatching {
                        paymentsApi.getChargesByOwner(ownerId).data.orEmpty()
                    }.getOrDefault(emptyList())
                        .filter { it.apartmentId == currentPropertyId || it.garageId == currentPropertyId }
                } else {
                    emptyList()
                }

                val payments = runCatching {
                    paymentsApi.getPaymentsByProperty(currentPropertyId).data.orEmpty()
                }.getOrDefault(emptyList())

                val balance = runCatching {
                    paymentsApi.propertyBalance(currentPropertyId).data?.currentBalance
                }.getOrNull()

                val lastPayment = payments.maxByOrNull { it.paymentDate.orEmpty() }

                val createdBy = createdByFor(
                    entityType = if (currentPropertyType == PropertyType.APARTMENT) "Apartment" else "Garage",
                    entityId = currentPropertyId,
                )
                val transferCreators = if (ownershipHistory.isEmpty()) {
                    emptyMap()
                } else {
                    creatorsMap("OwnershipHistory")
                }

                _state.value = _state.value.copy(
                    loading = false,
                    error = null,
                    property = property.copy(
                        balance = balance,
                        lastPaymentAmount = lastPayment?.amount,
                        lastPaymentDate = lastPayment?.paymentDate,
                        createdBy = createdBy,
                    ),
                    ownershipHistory = ownershipHistory,
                    charges = charges,
                    payments = payments,
                    transferCreators = transferCreators,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toUserMessage(),
                )
            }
        }
    }

    // ---------------------------------------------------------------- actions

    fun assignOwner(ownerId: String) {
        val property = _state.value.property ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, actionError = null, actionMessage = null)
            try {
                when (currentPropertyType) {
                    PropertyType.APARTMENT ->
                        buildingsApi.assignOwnerToApartment(property.id, AssignOwnerRequest(ownerId))

                    PropertyType.GARAGE ->
                        buildingsApi.assignOwnerToGarage(property.id, AssignOwnerRequest(ownerId))
                }
                _state.value = _state.value.copy(
                    saving = false,
                    actionMessage = "Sahib təyin edildi.",
                    ownerQuery = "",
                    ownerResults = emptyList(),
                )
                load(silent = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, actionError = e.toUserMessage())
            }
        }
    }

    /** Reads the property's outstanding debt (0 when it has an advance). */
    fun checkDebt() {
        viewModelScope.launch {
            _state.value = _state.value.copy(debtChecking = true, debtError = null, outstandingDebt = null)
            val debt = outstandingDebt()
            _state.value = if (debt == null) {
                _state.value.copy(
                    debtChecking = false,
                    debtError = "Borc yoxlanıla bilmədi.",
                )
            } else {
                _state.value.copy(debtChecking = false, outstandingDebt = debt)
            }
        }
    }

    fun transferOwnership(newOwnerId: String) {
        val property = _state.value.property ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, actionError = null, actionMessage = null)

            // Re-checked at submit time, so debt created while the dialog was
            // open is caught too — same rule as the web form.
            val debt = outstandingDebt()
            if (debt == null) {
                _state.value = _state.value.copy(
                    saving = false,
                    actionError = "Borc yoxlanıla bilmədi, əməliyyat dayandırıldı.",
                )
                return@launch
            }
            if (debt > 0.005) {
                _state.value = _state.value.copy(
                    saving = false,
                    outstandingDebt = debt,
                    actionError = "Bu əmlak üzrə ${String.format(java.util.Locale.US, "%.2f", debt)} ₼ qalıq borc var — köçürmə mümkün deyil.",
                )
                return@launch
            }

            try {
                buildingsApi.transferApartmentOwnership(property.id, TransferApartmentOwnershipRequest(newOwnerId))
                _state.value = _state.value.copy(
                    saving = false,
                    actionMessage = "Mülkiyyət köçürüldü.",
                    ownerQuery = "",
                    ownerResults = emptyList(),
                    outstandingDebt = null,
                )
                load(silent = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, actionError = e.toUserMessage())
            }
        }
    }

    fun pay(amount: Double, notes: String?) {
        val property = _state.value.property ?: return
        val ownerId = property.ownerId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, actionError = null, actionMessage = null)
            try {
                paymentsApi.createPayment(
                    CreatePaymentRequestDto(
                        ownerId = ownerId,
                        amount = amount,
                        paymentMethod = PaymentMethods.CASH,
                        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                        apartmentId = if (currentPropertyType == PropertyType.APARTMENT) property.id else null,
                        garageId = if (currentPropertyType == PropertyType.GARAGE) property.id else null,
                    ),
                )
                _state.value = _state.value.copy(
                    saving = false,
                    actionMessage = "Ödəniş qeyd edildi.",
                )
                load(silent = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, actionError = e.toUserMessage())
            }
        }
    }

    // ----------------------------------------------------------- charge sheet

    fun openCharge(charge: ChargeDto) {
        _state.value = _state.value.copy(selectedCharge = charge, chargeAllocations = null)
        viewModelScope.launch {
            val rows = runCatching {
                paymentsApi.chargeAllocations(charge.id).data.orEmpty()
            }.getOrDefault(emptyList())
            _state.value = _state.value.copy(chargeAllocations = rows)
        }
    }

    fun closeCharge() {
        _state.value = _state.value.copy(selectedCharge = null, chargeAllocations = null)
    }

    // ---------------------------------------------------------- owner picker

    fun searchOwners(query: String) {
        _state.value = _state.value.copy(ownerQuery = query)
        ownerSearchJob?.cancel()

        val term = query.trim()
        if (term.isEmpty()) {
            _state.value = _state.value.copy(ownerResults = emptyList(), ownersLoading = false)
            return
        }

        ownerSearchJob = viewModelScope.launch {
            delay(300)
            _state.value = _state.value.copy(ownersLoading = true)
            val results = runCatching {
                service.owners(SearchBody(searchTerm = term, pageSize = 8)).data?.items.orEmpty()
            }.getOrDefault(emptyList())
            _state.value = _state.value.copy(ownerResults = results, ownersLoading = false)
        }
    }

    fun clearOwnerSelectionSearch() {
        ownerSearchJob?.cancel()
        _state.value = _state.value.copy(ownerQuery = "", ownerResults = emptyList(), ownersLoading = false)
    }

    fun clearActionFeedback() {
        _state.value = _state.value.copy(actionError = null, actionMessage = null)
    }

    // ----------------------------------------------------------------- audit

    /** Audit "Created" row for an entity -> the name of the user who made it. */
    private suspend fun createdByFor(entityType: String, entityId: String): String? {
        fun body() = SearchBody(
            filters = listOf(
                QueryFilter("EntityType", Comparison.EQUALS, entityType),
                QueryFilter("EntityId", Comparison.EQUALS, entityId),
                QueryFilter("Action", Comparison.EQUALS, AuditActions.CREATED),
            ),
            pageSize = 1,
        )
        return runCatching {
            adminApi.auditBuildings(body()).data?.auditLogs?.firstOrNull()?.user?.name
        }.getOrNull()
            ?: runCatching {
                adminApi.auditPayments(body()).data?.auditLogs?.firstOrNull()?.user?.name
            }.getOrNull()
    }

    /** entityId -> creator name for a whole entity type (one request instead of N). */
    private suspend fun creatorsMap(entityType: String): Map<String, String> {
        fun body() = SearchBody(
            filters = listOf(
                QueryFilter("EntityType", Comparison.EQUALS, entityType),
                QueryFilter("Action", Comparison.EQUALS, AuditActions.CREATED),
            ),
            pageSize = 200,
        )
        val logs = runCatching {
            adminApi.auditBuildings(body()).data?.auditLogs.orEmpty()
        }.getOrDefault(emptyList()).ifEmpty {
            runCatching {
                adminApi.auditPayments(body()).data?.auditLogs.orEmpty()
            }.getOrDefault(emptyList())
        }
        return logs.mapNotNull { log ->
            val id = log.entityId ?: return@mapNotNull null
            val name = log.user?.name ?: return@mapNotNull null
            id to name
        }.toMap()
    }

    private suspend fun outstandingDebt(): Double? = try {
        val balance = paymentsApi.propertyBalance(currentPropertyId).data
        if (balance == null) null else maxOf(0.0, -(balance.currentBalance ?: 0.0))
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------- data load

    private suspend fun loadApartmentDetail(apartmentId: String): PropertyDetail {
        val apartment = buildingsApi.getApartmentById(apartmentId)

        val building = apartment.building?.id?.let { buildingId ->
            runCatching { buildingsApi.getBuildingById(buildingId) }.getOrNull()
        }
        val owner = apartment.currentOwner?.id?.let { ownerId ->
            runCatching { buildingsApi.getOwnerById(ownerId) }.getOrNull()
        }

        val otherProperties = owner?.let {
            val apartments = it.apartments.orEmpty().filter { apt -> apt.id != apartmentId }
            val garages = it.garages.orEmpty()

            apartments.map { apt ->
                OtherProperty(
                    id = apt.id,
                    type = PropertyItemType.APARTMENT,
                    title = "${apt.building?.name ?: "—"} · Mənzil ${apt.apartmentNumber ?: "—"}",
                    subtitle = "${apt.floor ?: 0}-ci mərtəbə · ${apt.roomCount ?: 0} otaq",
                )
            } + garages.map { garage ->
                OtherProperty(
                    id = garage.id,
                    type = PropertyItemType.GARAGE,
                    title = "Qaraj ${garage.garageNumber ?: "—"}",
                    subtitle = garage.type ?: "—",
                )
            }
        }.orEmpty()

        return PropertyDetail(
            id = apartment.id,
            type = PropertyItemType.APARTMENT,
            title = "Mənzil ${apartment.apartmentNumber ?: "—"}",
            buildingName = apartment.building?.name ?: "—",
            buildingAddress = building?.fullAddress ?: "—",
            floor = apartment.floor,
            roomCount = apartment.roomCount,
            area = apartment.areaSquareMeters,
            direction = null,
            status = apartment.status ?: "unknown",
            ownerId = apartment.currentOwner?.id,
            ownerName = owner?.fullName,
            ownerPhone = owner?.phoneNumber,
            ownerEmail = owner?.email,
            contractNumber = null,
            contractDate = null,
            balance = null,
            lastPaymentAmount = null,
            lastPaymentDate = null,
            monthlyRate = null,
            otherProperties = otherProperties,
            utilities = emptyList(),
            imageUrl = null,
            createdBy = null,
        )
    }

    private suspend fun loadGarageDetail(garageId: String): PropertyDetail {
        val garage = buildingsApi.getGarageById(garageId).data
            ?: throw Exception("Qaraj tapılmadı")

        val owner = garage.owner?.id?.let { ownerId ->
            runCatching { buildingsApi.getOwnerById(ownerId) }.getOrNull()
        }

        val otherProperties = owner?.let {
            val apartments = it.apartments.orEmpty()
            val garages = it.garages.orEmpty().filter { g -> g.id != garageId }

            apartments.map { apt ->
                OtherProperty(
                    id = apt.id,
                    type = PropertyItemType.APARTMENT,
                    title = "${apt.building?.name ?: "—"} · Mənzil ${apt.apartmentNumber ?: "—"}",
                    subtitle = "${apt.floor ?: 0}-ci mərtəbə · ${apt.roomCount ?: 0} otaq",
                )
            } + garages.map { g ->
                OtherProperty(
                    id = g.id,
                    type = PropertyItemType.GARAGE,
                    title = "Qaraj ${g.garageNumber ?: "—"}",
                    subtitle = g.type ?: "—",
                )
            }
        }.orEmpty()

        return PropertyDetail(
            id = garage.id,
            type = PropertyItemType.GARAGE,
            title = "Qaraj ${garage.garageNumber ?: "—"}",
            buildingName = "—",
            buildingAddress = garage.description ?: "Yeraltı dayanacaq",
            floor = null,
            roomCount = null,
            area = null, // Not in the garage API
            direction = null,
            status = if (garage.owner != null) "occupied" else "vacant",
            ownerId = garage.owner?.id,
            ownerName = owner?.fullName,
            ownerPhone = owner?.phoneNumber,
            ownerEmail = owner?.email,
            contractNumber = null,
            contractDate = null,
            balance = null,
            lastPaymentAmount = null,
            lastPaymentDate = null,
            monthlyRate = null,
            otherProperties = otherProperties,
            utilities = emptyList(),
            imageUrl = null,
            createdBy = null,
        )
    }
}

// Data Models

data class PropertyDetail(
    val id: String,
    val type: PropertyItemType,
    val title: String,
    val buildingName: String,
    val buildingAddress: String,
    val floor: Int?,
    val roomCount: Int?,
    val area: Double?,
    val direction: String?,
    val status: String,
    val ownerId: String?,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerEmail: String?,
    val contractNumber: String?,
    val contractDate: String?,
    val balance: Double?,
    val lastPaymentAmount: Double?,
    val lastPaymentDate: String?,
    val monthlyRate: Double?,
    val otherProperties: List<OtherProperty>,
    val utilities: List<Utility>,
    val imageUrl: String?,
    /** "Əməliyyatı icra etdi" — who created the record (audit log). */
    val createdBy: String?,
)

data class Utility(
    val name: String,
    val hasActive: Boolean,
)

data class OtherProperty(
    val id: String,
    val type: PropertyItemType,
    val title: String,
    val subtitle: String,
)
