package az.mtk.app.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.CustomerBalanceDto
import az.mtk.app.data.api.CustomerSaveRequest
import az.mtk.app.data.api.OperationsRepository
import az.mtk.app.data.api.PartyChargeRow
import az.mtk.app.data.api.PartyPaymentRow
import az.mtk.app.data.api.PartyRow
import az.mtk.app.data.api.VendorSaveRequest
import az.mtk.app.data.api.finDateOnlyToUtcIso
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PartyKind(val label: String, val singular: String) {
    VENDOR("Tədarükçülər", "Tədarükçü"),
    CUSTOMER("Müştərilər", "Müştəri"),
}

/**
 * Tədarükçülər və müştərilər üçün ortaq ViewModel. Hər iki tərəfin
 * siyahı/detal/borc/ödəniş axını eynidir; fərq yalnız API çağırışlarındadır.
 */
class PartiesViewModel : ViewModel() {

    private val repo = OperationsRepository

    data class UiState(
        val kind: PartyKind = PartyKind.VENDOR,

        // Siyahı
        val list: List<PartyRow> = emptyList(),
        val total: Int = 0,
        val page: Int = 0,
        val query: String = "",
        val loading: Boolean = false,
        val error: String? = null,

        // Detal
        val detailId: String? = null,
        val detail: PartyRow? = null,
        val detailLoading: Boolean = false,
        val detailError: String? = null,

        // Detala bağlı məlumatlar
        val charges: List<PartyChargeRow> = emptyList(),
        val payments: List<PartyPaymentRow> = emptyList(),
        val balance: CustomerBalanceDto? = null,
        val contracts: List<ContractListItemDto> = emptyList(),
        val relatedLoading: Boolean = false,
        val relatedError: String? = null,

        // Əməliyyatlar / formalar
        val busy: Boolean = false,
        val message: String? = null,
        val actionError: String? = null,
        val saving: Boolean = false,
        val formError: String? = null,

        val canManage: Boolean = false,
        val canPay: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var listRequest = 0
    private var detailRequest = 0

    fun setCanManage(value: Boolean) {
        _state.value = _state.value.copy(canManage = value)
    }

    fun setCanPay(value: Boolean) {
        _state.value = _state.value.copy(canPay = value)
    }

    /** Tab dəyişəndə növü təyin edir və siyahını (bir dəfə) yükləyir. */
    fun setKind(kind: PartyKind) {
        if (_state.value.kind == kind) {
            if (_state.value.list.isEmpty() && !_state.value.loading && _state.value.error == null) {
                load(reset = true)
            }
            return
        }
        val canManage = _state.value.canManage
        val canPay = _state.value.canPay
        listRequest++
        detailRequest++
        _state.value = UiState(kind = kind, canManage = canManage, canPay = canPay)
        load(reset = true)
    }

    // ---- Siyahı -------------------------------------------------------------

    fun load(reset: Boolean = true) {
        val current = _state.value
        val page = if (reset) 0 else current.page
        val request = ++listRequest
        _state.value = current.copy(loading = true, error = null, page = page)
        viewModelScope.launch {
            try {
                val result = if (current.kind == PartyKind.VENDOR) {
                    repo.vendors(page, search = current.query.trim().ifBlank { null })
                } else {
                    repo.customers(page, search = current.query.trim().ifBlank { null })
                }
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(
                    list = result.items,
                    total = result.totalCount,
                    page = result.page,
                    loading = false,
                )
            } catch (t: Throwable) {
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(loading = false, error = t.toUserMessage())
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            load(reset = true)
        }
    }

    fun nextPage() {
        val s = _state.value
        if ((s.page + 1) * 20 >= s.total) return
        _state.value = s.copy(page = s.page + 1)
        load(reset = false)
    }

    fun prevPage() {
        if (_state.value.page <= 0) return
        _state.value = _state.value.copy(page = _state.value.page - 1)
        load(reset = false)
    }

    // ---- Detal --------------------------------------------------------------

    fun openParty(id: String) {
        if (_state.value.detailId == id && _state.value.detail != null) return
        _state.value = _state.value.copy(
            detailId = id,
            detail = null,
            detailError = null,
            charges = emptyList(),
            payments = emptyList(),
            balance = null,
            contracts = emptyList(),
            relatedError = null,
            message = null,
            actionError = null,
            formError = null,
        )
        loadDetail(id)
    }

    fun closeDetail() {
        detailRequest++
        _state.value = _state.value.copy(
            detailId = null,
            detail = null,
            detailError = null,
            charges = emptyList(),
            payments = emptyList(),
            balance = null,
            contracts = emptyList(),
            relatedError = null,
            busy = false,
            message = null,
            actionError = null,
            formError = null,
        )
    }

    private fun loadDetail(id: String) {
        val request = ++detailRequest
        _state.value = _state.value.copy(detailLoading = true, detailError = null)
        viewModelScope.launch {
            try {
                val detail = if (_state.value.kind == PartyKind.VENDOR) {
                    repo.vendor(id)
                } else {
                    repo.customer(id)
                }
                if (request != detailRequest || _state.value.detailId != id) return@launch
                _state.value = _state.value.copy(detail = detail, detailLoading = false)
                loadRelated(id, _state.value.kind)
            } catch (t: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(detailLoading = false, detailError = t.toUserMessage())
            }
        }
    }

    private fun loadRelated(id: String, kind: PartyKind) {
        _state.value = _state.value.copy(relatedLoading = true, relatedError = null)
        viewModelScope.launch {
            val errors = mutableListOf<String>()

            val charges = runCatching {
                if (kind == PartyKind.VENDOR) repo.vendorCharges(id) else repo.customerCharges(id)
            }.getOrElse {
                errors += "Borclar: ${it.toUserMessage()}"
                emptyList()
            }

            val payments = runCatching {
                if (kind == PartyKind.VENDOR) repo.vendorPayments(id) else repo.customerPayments(id)
            }.getOrElse {
                errors += "Ödənişlər: ${it.toUserMessage()}"
                emptyList()
            }

            var balance: CustomerBalanceDto? = null
            var contracts: List<ContractListItemDto> = emptyList()
            if (kind == PartyKind.CUSTOMER) {
                balance = runCatching { repo.customerBalance(id) }.getOrElse {
                    errors += "Avans: ${it.toUserMessage()}"
                    null
                }
                contracts = runCatching { repo.contractsByCustomer(id) }.getOrElse {
                    errors += "Müqavilələr: ${it.toUserMessage()}"
                    emptyList()
                }
            }

            if (_state.value.detailId != id) return@launch
            _state.value = _state.value.copy(
                charges = charges,
                payments = payments,
                balance = balance,
                contracts = contracts,
                relatedLoading = false,
                relatedError = errors.ifEmpty { null }?.joinToString(" · "),
            )
        }
    }

    fun refreshDetail() {
        val id = _state.value.detailId ?: return
        loadDetail(id)
    }

    // ---- Tərəfin redaktəsi --------------------------------------------------

    fun saveParty(
        id: String?,
        name: String,
        type: Int,
        voen: String?,
        director: String?,
        email: String?,
        phone: String?,
        address: String?,
        note: String?,
        onSaved: () -> Unit,
    ) {
        val kind = _state.value.kind
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            try {
                if (kind == PartyKind.VENDOR) {
                    val body = VendorSaveRequest(
                        name.trim(),
                        type,
                        voen?.trim()?.ifBlank { null },
                        director?.trim()?.ifBlank { null },
                        email?.trim()?.ifBlank { null },
                        phone?.trim()?.ifBlank { null },
                        address?.trim()?.ifBlank { null },
                        note?.trim()?.ifBlank { null },
                    )
                    if (id == null) repo.createVendor(body) else repo.updateVendor(id, body)
                } else {
                    val body = CustomerSaveRequest(
                        name.trim(),
                        type,
                        voen?.trim()?.ifBlank { null },
                        director?.trim()?.ifBlank { null },
                        email?.trim()?.ifBlank { null },
                        phone?.trim()?.ifBlank { null },
                        address?.trim()?.ifBlank { null },
                        note?.trim()?.ifBlank { null },
                    )
                    if (id == null) repo.createCustomer(body) else repo.updateCustomer(id, body)
                }
                _state.value = _state.value.copy(saving = false)
                onSaved()
                val detailId = _state.value.detailId
                if (detailId != null) loadDetail(detailId) else load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    private fun runAction(onDone: () -> Unit = {}, block: suspend () -> Unit) {
        val id = _state.value.detailId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null, message = null)
            try {
                block()
                _state.value = _state.value.copy(busy = false, message = "Əməliyyat icra olundu")
                onDone()
                loadDetail(id)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, actionError = t.toUserMessage())
            }
        }
    }

    fun setStatus(active: Boolean) = runAction {
        val id = _state.value.detailId!!
        if (_state.value.kind == PartyKind.VENDOR) repo.setVendorStatus(id, active)
        else repo.setCustomerStatus(id, active)
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.detailId ?: return
        val kind = _state.value.kind
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null)
            try {
                if (kind == PartyKind.VENDOR) repo.deleteVendor(id) else repo.deleteCustomer(id)
                _state.value = _state.value.copy(busy = false)
                onDeleted()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, actionError = t.toUserMessage())
            }
        }
    }

    // ---- Borc / ödəniş ------------------------------------------------------

    fun createCharge(
        description: String,
        amount: Double,
        dueDate: String?,
        onDone: () -> Unit = {},
    ) = runAction(onDone) {
        val id = _state.value.detailId!!
        repo.createCustomerCharge(
            id,
            description.trim(),
            amount,
            dueDate?.takeIf { it.isNotBlank() }?.let(::finDateOnlyToUtcIso),
        )
    }

    fun receivePayment(
        amount: Double,
        method: Int,
        notes: String?,
        onDone: () -> Unit = {},
    ) = runAction(onDone) {
        val id = _state.value.detailId!!
        repo.createCustomerPayment(id, amount, method, notes?.trim()?.ifBlank { null })
    }

    fun payVendor(
        amount: Double,
        method: Int,
        notes: String?,
        onDone: () -> Unit = {},
    ) = runAction(onDone) {
        val id = _state.value.detailId!!
        repo.createVendorPayment(id, amount, method, notes?.trim()?.ifBlank { null })
    }

    fun cancelCharge(charge: PartyChargeRow, onDone: () -> Unit = {}) = runAction(onDone) {
        if (_state.value.kind == PartyKind.VENDOR) repo.cancelVendorCharge(charge.id)
        else repo.cancelCustomerCharge(charge.id)
    }

    fun payCharge(
        charge: PartyChargeRow,
        amount: Double,
        method: Int,
        notes: String?,
        onDone: () -> Unit = {},
    ) = runAction(onDone) {
        if (_state.value.kind == PartyKind.VENDOR) {
            repo.createVendorPayment(charge.partyId, amount, method, notes?.trim()?.ifBlank { null })
        } else {
            repo.createCustomerPayment(charge.partyId, amount, method, notes?.trim()?.ifBlank { null })
        }
    }

    fun clearFormError() {
        _state.value = _state.value.copy(formError = null)
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, actionError = null)
    }
}
