package az.mtk.app.ui.screens.owners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.*
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "Sakin detalları" — MTK-UI `OwnerDetailView`-in Android qarşılığı:
 *
 *  - sakinin özü (ad, e-poçt, telefon, hesab növü, aktivlik),
 *  - ümumi balans (`/balances/owner/{id}`) və son ödəniş,
 *  - hər əmlak üzrə ayrıca balans (`/balances/property/{id}`),
 *  - sakinin bütün haqları və ödənişləri,
 *  - əmlak üzrə hədəflənmiş ödəniş qeydə alma,
 *  - audit jurnalından "Əməliyyatı icra etdi" məlumatı.
 */
class OwnerDetailViewModel(initialOwnerId: String) : ViewModel() {

    private var currentOwnerId = initialOwnerId

    private val buildingsApi = AppGraph.service(BuildingsApi::class.java)
    private val paymentsApi = AppGraph.service(PaymentsApi::class.java)
    private val adminApi = AppGraph.service(AdminApi::class.java)

    enum class Tab {
        APARTMENTS,
        GARAGES,
        CHARGES,
        PAYMENTS,
    }

    data class UiState(
        val loading: Boolean = true,
        val owner: OwnerDetailDto? = null,
        val error: String? = null,
        val selectedTab: Tab = Tab.APARTMENTS,
        val charges: List<ChargeDto> = emptyList(),
        val payments: List<PaymentDto> = emptyList(),
        val ownerBalance: Double = 0.0,
        val ownerBalanceId: String? = null,
        /** Əmlak id-si -> onun balansı. */
        val propertyBalances: Map<String, Double> = emptyMap(),
        /** Audit: Owner qeydini kim yaradıb. */
        val createdBy: String? = null,
        /** Audit: OwnerBalance qeydini kim yaradıb. */
        val balanceCreatedBy: String? = null,
        val selectedCharge: ChargeDto? = null,
        val chargeAllocations: List<ChargeAllocationDto>? = null,
        /** "Ödəniş sənədi" dialoqu — seçilmiş ödəniş və onun bölgüsü. */
        val selectedPayment: PaymentDto? = null,
        val paymentAllocations: List<PaymentAllocationDto>? = null,
        val paymentCreatedBy: String? = null,
        /** PaymentAllocation id -> əməliyyatı icra edən. */
        val allocationCreators: Map<String, String> = emptyMap(),
        val saving: Boolean = false,
        val actionError: String? = null,
        val actionMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load(ownerId: String = currentOwnerId, silent: Boolean = false) {
        currentOwnerId = ownerId

        viewModelScope.launch {
            if (silent) {
                _state.value = _state.value.copy(error = null)
            } else {
                _state.value = UiState(loading = true)
            }

            try {
                val owner = buildingsApi.getOwnerById(currentOwnerId)

                val charges = runCatching {
                    paymentsApi.getChargesByOwner(currentOwnerId).data.orEmpty()
                }.getOrDefault(emptyList())

                val payments = runCatching {
                    paymentsApi.getPaymentsByOwner(currentOwnerId).data.orEmpty()
                }.getOrDefault(emptyList())

                val balance = runCatching {
                    paymentsApi.ownerBalance(currentOwnerId).data
                }.getOrNull()

                val propertyIds = buildList {
                    owner.apartments.orEmpty().forEach { add(it.id) }
                    owner.garages.orEmpty().forEach { add(it.id) }
                }

                val propertyBalances = coroutineScope {
                    propertyIds.map { propertyId ->
                        async {
                            val value = runCatching {
                                paymentsApi.propertyBalance(propertyId).data?.currentBalance
                            }.getOrNull() ?: 0.0
                            propertyId to value
                        }
                    }.map { it.await() }.toMap()
                }

                val createdBy = createdByFor("Owner", currentOwnerId)
                val balanceCreatedBy = balance?.id?.let { createdByFor("OwnerBalance", it) }

                _state.value = _state.value.copy(
                    loading = false,
                    error = null,
                    owner = owner,
                    charges = charges,
                    payments = payments,
                    ownerBalance = balance?.currentBalance ?: 0.0,
                    ownerBalanceId = balance?.id,
                    propertyBalances = propertyBalances,
                    createdBy = createdBy,
                    balanceCreatedBy = balanceCreatedBy,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toUserMessage(),
                )
            }
        }
    }

    fun setTab(tab: Tab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    /**
     * Əmlak üzrə hədəflənmiş ödəniş. `apartmentId`/`garageId` null olduqda
     * ödəniş sakinin ümumi balansına (avans kimi) yazılır.
     */
    fun pay(amount: Double, notes: String?, apartmentId: String?, garageId: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, actionError = null, actionMessage = null)
            try {
                paymentsApi.createPayment(
                    CreatePaymentRequestDto(
                        ownerId = currentOwnerId,
                        amount = amount,
                        paymentMethod = PaymentMethods.CASH,
                        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                        apartmentId = apartmentId,
                        garageId = garageId,
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

    /**
     * Ödəniş kartına toxunanda açılan "Ödəniş sənədi": ödənişin bölgüsü
     * (hansı haqları qarşıladığı) və onu kimin icra etdiyi yalnız bu anda
     * yüklənir — saytdakı PaymentDetailModal kimi.
     */
    fun openPayment(payment: PaymentDto) {
        _state.value = _state.value.copy(
            selectedPayment = payment,
            paymentAllocations = null,
            paymentCreatedBy = null,
            allocationCreators = emptyMap(),
        )
        viewModelScope.launch {
            val createdBy = createdByFor("Payment", payment.id)
            val rows = runCatching {
                paymentsApi.paymentAllocations(payment.id).data.orEmpty()
            }.getOrDefault(emptyList())
            val creators = if (rows.isEmpty()) emptyMap() else creatorsMap("PaymentAllocation")

            _state.value = _state.value.copy(
                paymentAllocations = rows,
                paymentCreatedBy = createdBy,
                allocationCreators = creators,
            )
        }
    }

    fun closePayment() {
        _state.value = _state.value.copy(
            selectedPayment = null,
            paymentAllocations = null,
            paymentCreatedBy = null,
            allocationCreators = emptyMap(),
        )
    }

    fun clearActionFeedback() {
        _state.value = _state.value.copy(actionError = null, actionMessage = null)
    }

    // ----------------------------------------------------------------- audit

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

    /** entityId -> icra edən, bir sorğu ilə bütün sətirlər üçün. */
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
}
