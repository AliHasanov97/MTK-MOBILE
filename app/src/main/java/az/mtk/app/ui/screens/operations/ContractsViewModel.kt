package az.mtk.app.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.ContractCreateRequest
import az.mtk.app.data.api.ContractDates
import az.mtk.app.data.api.ContractDto
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.ContractParty
import az.mtk.app.data.api.ContractServiceRequest
import az.mtk.app.data.api.ContractUpdateRequest
import az.mtk.app.data.api.OperationsRepository
import az.mtk.app.data.api.PartyChargeRow
import az.mtk.app.data.api.PartyRow
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Müqavilələr bölməsi: siyahı, detal, xidmətlər, borclar və status əməliyyatları. */
class ContractsViewModel : ViewModel() {

    private val repo = OperationsRepository

    data class UiState(
        // Siyahı
        val list: List<ContractListItemDto> = emptyList(),
        val total: Int = 0,
        val page: Int = 0,
        val query: String = "",
        val party: ContractParty = ContractParty.ALL,
        val loading: Boolean = false,
        val error: String? = null,

        // Detal
        val detailId: String? = null,
        val detail: ContractDto? = null,
        val detailLoading: Boolean = false,
        val detailError: String? = null,

        // Müqavilə borcları
        val charges: List<PartyChargeRow> = emptyList(),
        val chargesLoading: Boolean = false,
        val chargesError: String? = null,

        // Əməliyyatlar
        val busy: Boolean = false,
        val message: String? = null,
        val actionError: String? = null,

        // Formalar
        val saving: Boolean = false,
        val formError: String? = null,

        // Yaratma forması üçün tərəf seçimləri
        val vendorOptions: List<PartyRow> = emptyList(),
        val customerOptions: List<PartyRow> = emptyList(),
        val optionsLoading: Boolean = false,
        val optionsError: String? = null,

        val canManage: Boolean = false,
        val canPay: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var listRequest = 0
    private var detailRequest = 0
    private var chargesRequest = 0
    private var optionsLoaded = false

    fun setCanManage(value: Boolean) {
        _state.value = _state.value.copy(canManage = value)
    }

    fun setCanPay(value: Boolean) {
        _state.value = _state.value.copy(canPay = value)
    }

    // ---- Siyahı -------------------------------------------------------------

    fun load(reset: Boolean = true) {
        val current = _state.value
        val page = if (reset) 0 else current.page
        val request = ++listRequest
        _state.value = current.copy(
            loading = true,
            error = null,
            page = page,
        )
        viewModelScope.launch {
            try {
                val result = repo.contracts(
                    page = page,
                    search = current.query.trim().ifBlank { null },
                    party = current.party,
                )
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

    fun setParty(value: ContractParty) {
        if (_state.value.party == value) return
        _state.value = _state.value.copy(party = value)
        load(reset = true)
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

    fun openContract(id: String) {
        if (_state.value.detailId == id && _state.value.detail != null) return
        _state.value = _state.value.copy(
            detailId = id,
            detail = null,
            detailError = null,
            charges = emptyList(),
            chargesError = null,
            message = null,
            actionError = null,
            formError = null,
        )
        loadDetail(id)
    }

    fun closeDetail() {
        detailRequest++
        chargesRequest++
        _state.value = _state.value.copy(
            detailId = null,
            detail = null,
            detailError = null,
            charges = emptyList(),
            chargesError = null,
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
                val contract = repo.contract(id)
                if (request != detailRequest || _state.value.detailId != id) return@launch
                _state.value = _state.value.copy(detail = contract, detailLoading = false)
                loadCharges(contract)
            } catch (t: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(detailLoading = false, detailError = t.toUserMessage())
            }
        }
    }

    private fun loadCharges(contract: ContractDto) {
        val request = ++chargesRequest
        _state.value = _state.value.copy(chargesLoading = true, chargesError = null)
        viewModelScope.launch {
            try {
                val rows = if (contract.customerId != null) {
                    repo.customerChargesByContract(contract.id)
                } else {
                    repo.vendorChargesByContract(contract.id)
                }
                if (request != chargesRequest) return@launch
                _state.value = _state.value.copy(charges = rows, chargesLoading = false)
            } catch (t: Throwable) {
                if (request != chargesRequest) return@launch
                _state.value = _state.value.copy(chargesLoading = false, chargesError = t.toUserMessage())
            }
        }
    }

    fun refreshDetail() {
        val id = _state.value.detailId ?: return
        loadDetail(id)
    }

    // ---- Status əməliyyatları ----------------------------------------------

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

    fun activate(onDone: () -> Unit = {}) = runAction(onDone) {
        repo.activateContract(_state.value.detailId!!)
    }

    fun suspend(note: String?, onDone: () -> Unit = {}) = runAction(onDone) {
        repo.suspendContract(_state.value.detailId!!, note?.trim()?.ifBlank { null })
    }

    fun terminate(date: String, note: String?, onDone: () -> Unit = {}) = runAction(onDone) {
        repo.terminateContract(
            _state.value.detailId!!,
            ContractDates.toUtcIso(date),
            note?.trim()?.ifBlank { null },
        )
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.detailId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null)
            try {
                repo.deleteContract(id)
                _state.value = _state.value.copy(busy = false)
                onDeleted()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, actionError = t.toUserMessage())
            }
        }
    }

    fun updateDates(startDate: String, endDate: String, note: String?, onDone: () -> Unit = {}) = runAction(onDone) {
        repo.updateContract(
            _state.value.detailId!!,
            ContractUpdateRequest(
                ContractDates.toUtcIso(startDate),
                ContractDates.toUtcIso(endDate),
                note?.trim()?.ifBlank { null },
            ),
        )
    }

    // ---- Xidmətlər ----------------------------------------------------------

    fun saveService(
        serviceId: String?,
        name: String,
        unitPrice: Double,
        billingPeriod: Int,
        description: String?,
        startDate: String?,
        endDate: String?,
        paymentTermDays: Int?,
        onSaved: () -> Unit,
    ) {
        val contractId = _state.value.detailId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            try {
                val body = ContractServiceRequest(
                    name = name.trim(),
                    unitPrice = unitPrice,
                    billingPeriod = billingPeriod,
                    description = description?.trim()?.ifBlank { null },
                    serviceStartDate = startDate?.takeIf { it.isNotBlank() }?.let(ContractDates::toUtcIso),
                    serviceEndDate = endDate?.takeIf { it.isNotBlank() }?.let(ContractDates::toUtcIso),
                    paymentTermDays = paymentTermDays,
                )
                if (serviceId == null) {
                    repo.addContractService(contractId, body)
                } else {
                    repo.updateContractService(contractId, serviceId, body)
                }
                _state.value = _state.value.copy(saving = false, message = "Xidmət saxlanıldı")
                onSaved()
                loadDetail(contractId)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    fun toggleService(serviceId: String, active: Boolean) = runAction {
        repo.setContractServiceStatus(_state.value.detailId!!, serviceId, active)
    }

    fun deleteService(serviceId: String, onDone: () -> Unit = {}) = runAction(onDone) {
        repo.removeContractService(_state.value.detailId!!, serviceId)
    }

    // ---- Borclar ------------------------------------------------------------

    fun cancelCharge(charge: PartyChargeRow, onDone: () -> Unit = {}) = runAction(onDone) {
        val contract = _state.value.detail ?: return@runAction
        if (contract.customerId != null) repo.cancelCustomerCharge(charge.id)
        else repo.cancelVendorCharge(charge.id)
    }

    fun payCharge(
        charge: PartyChargeRow,
        amount: Double,
        method: Int,
        notes: String?,
        onDone: () -> Unit = {},
    ) = runAction(onDone) {
        val contract = _state.value.detail ?: return@runAction
        if (contract.customerId != null) {
            repo.createCustomerPayment(charge.partyId, amount, method, notes?.trim()?.ifBlank { null })
        } else {
            repo.createVendorPayment(charge.partyId, amount, method, notes?.trim()?.ifBlank { null })
        }
    }

    // ---- Yaratma ------------------------------------------------------------

    fun loadOptions() {
        if (optionsLoaded || _state.value.optionsLoading) return
        _state.value = _state.value.copy(optionsLoading = true, optionsError = null)
        viewModelScope.launch {
            try {
                val vendors = repo.vendors(0, pageSize = 200).items.filter { it.isActive }
                val customers = repo.customers(0, pageSize = 200).items.filter { it.isActive }
                optionsLoaded = true
                _state.value = _state.value.copy(
                    vendorOptions = vendors,
                    customerOptions = customers,
                    optionsLoading = false,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(optionsLoading = false, optionsError = t.toUserMessage())
            }
        }
    }

    fun createContract(
        number: String,
        vendorId: String?,
        customerId: String?,
        startDate: String,
        endDate: String,
        note: String?,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            try {
                val id = repo.createContract(
                    ContractCreateRequest(
                        number = number.trim(),
                        vendorId = vendorId,
                        customerId = customerId,
                        startDate = ContractDates.toUtcIso(startDate),
                        endDate = ContractDates.toUtcIso(endDate),
                        note = note?.trim()?.ifBlank { null },
                    ),
                )
                _state.value = _state.value.copy(saving = false)
                onCreated(id)
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    fun clearFormError() {
        _state.value = _state.value.copy(formError = null)
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, actionError = null)
    }
}
