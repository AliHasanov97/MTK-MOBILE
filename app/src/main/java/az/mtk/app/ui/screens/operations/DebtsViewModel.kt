package az.mtk.app.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.ChargeAllocationDto
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.CreateChargeRequestDto
import az.mtk.app.data.api.CreatePaymentRequestDto
import az.mtk.app.data.api.DebtScope
import az.mtk.app.data.api.GenerateChargesResultDto
import az.mtk.app.data.api.OperationsRepository
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Borclar bölməsi: sakin haqları, bölgülər, dövr generasiyası və ödənişlər. */
class DebtsViewModel : ViewModel() {

    private val repo = OperationsRepository

    data class UiState(
        val charges: List<ChargeDto> = emptyList(),
        val total: Int = 0,
        val page: Int = 0,
        val scope: DebtScope = DebtScope.OPEN,
        val query: String = "",
        val loading: Boolean = false,
        val error: String? = null,

        val allocations: Map<String, List<ChargeAllocationDto>> = emptyMap(),
        val allocationsLoadingId: String? = null,

        val busy: Boolean = false,
        val message: String? = null,
        val actionError: String? = null,
        val saving: Boolean = false,
        val formError: String? = null,

        val generating: Boolean = false,
        val generateError: String? = null,
        val generateResult: GenerateChargesResultDto? = null,

        val ownerOptions: List<OwnerItemDto> = emptyList(),
        val ownerOptionsLoading: Boolean = false,
        val ownerOptionsError: String? = null,
        val selectedOwner: OwnerItemDto? = null,
        val selectedOwnerDetail: OwnerDetailDto? = null,
        val ownerDetailLoading: Boolean = false,

        val canManage: Boolean = false,
        val canPay: Boolean = false,
        val isAdmin: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var listRequest = 0
    private var ownerRequest = 0

    fun setPermissions(canManage: Boolean, canPay: Boolean, isAdmin: Boolean) {
        _state.value = _state.value.copy(canManage = canManage, canPay = canPay, isAdmin = isAdmin)
    }

    // ---- Siyahı -------------------------------------------------------------

    fun load(reset: Boolean = true) {
        val current = _state.value
        val page = if (reset) 0 else current.page
        val request = ++listRequest
        _state.value = current.copy(loading = true, error = null, page = page)
        viewModelScope.launch {
            try {
                val result = repo.charges(
                    page = page,
                    search = current.query.trim().ifBlank { null },
                    scope = current.scope,
                )
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(
                    charges = result.items,
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

    fun setScope(scope: DebtScope) {
        if (_state.value.scope == scope) return
        _state.value = _state.value.copy(scope = scope, allocations = emptyMap())
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

    // ---- Bölgü --------------------------------------------------------------

    fun toggleAllocations(chargeId: String) {
        val current = _state.value
        if (current.allocations.containsKey(chargeId)) {
            _state.value = current.copy(allocations = current.allocations - chargeId)
            return
        }
        if (current.allocationsLoadingId != null) return
        _state.value = current.copy(allocationsLoadingId = chargeId)
        viewModelScope.launch {
            try {
                val rows = repo.chargeAllocations(chargeId)
                _state.value = _state.value.copy(
                    allocations = _state.value.allocations + (chargeId to rows),
                    allocationsLoadingId = null,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    allocationsLoadingId = null,
                    actionError = t.toUserMessage(),
                )
            }
        }
    }

    // ---- Dövr generasiyası --------------------------------------------------

    fun generate(period: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(generating = true, generateError = null, generateResult = null)
            try {
                val result = repo.generateCharges(period)
                _state.value = _state.value.copy(generating = false, generateResult = result)
                onDone()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(generating = false, generateError = t.toUserMessage())
            }
        }
    }

    fun closeGenerate() {
        _state.value = _state.value.copy(generateResult = null, generateError = null)
    }

    // ---- Sahib seçimi (yeni haqq üçün) --------------------------------------

    fun loadOwners(search: String? = null) {
        _state.value = _state.value.copy(ownerOptionsLoading = true, ownerOptionsError = null)
        viewModelScope.launch {
            try {
                val result = repo.owners(0, pageSize = 50, search = search?.trim()?.ifBlank { null })
                _state.value = _state.value.copy(ownerOptions = result.items, ownerOptionsLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(ownerOptionsLoading = false, ownerOptionsError = t.toUserMessage())
            }
        }
    }

    fun selectOwner(owner: OwnerItemDto?) {
        if (owner == null) {
            ownerRequest++
            _state.value = _state.value.copy(selectedOwner = null, selectedOwnerDetail = null)
            return
        }
        val request = ++ownerRequest
        _state.value = _state.value.copy(
            selectedOwner = owner,
            selectedOwnerDetail = null,
            ownerDetailLoading = true,
        )
        viewModelScope.launch {
            try {
                val detail = repo.owner(owner.id)
                if (request != ownerRequest) return@launch
                _state.value = _state.value.copy(selectedOwnerDetail = detail, ownerDetailLoading = false)
            } catch (t: Throwable) {
                if (request != ownerRequest) return@launch
                _state.value = _state.value.copy(
                    ownerDetailLoading = false,
                    formError = t.toUserMessage(),
                )
            }
        }
    }

    fun resetChargeForm() {
        ownerRequest++
        _state.value = _state.value.copy(
            selectedOwner = null,
            selectedOwnerDetail = null,
            ownerDetailLoading = false,
            formError = null,
        )
    }

    // ---- Yeni haqq ----------------------------------------------------------

    fun addCharge(
        ownerId: String,
        apartmentId: String?,
        garageId: String?,
        amount: Double,
        description: String,
        period: String?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            try {
                repo.createOwnerCharge(
                    CreateChargeRequestDto(
                        ownerId = ownerId,
                        apartmentId = apartmentId,
                        garageId = garageId,
                        amount = amount,
                        description = description.trim(),
                        period = period?.trim()?.ifBlank { null },
                    ),
                )
                _state.value = _state.value.copy(saving = false, message = "Borc yaradıldı")
                onDone()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    // ---- Ödəniş / ləğv ------------------------------------------------------

    fun pay(
        ownerId: String,
        amount: Double,
        method: Int,
        notes: String?,
        apartmentId: String?,
        garageId: String?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, formError = null, actionError = null)
            try {
                repo.createOwnerPayment(
                    CreatePaymentRequestDto(
                        ownerId = ownerId,
                        amount = amount,
                        paymentMethod = method,
                        notes = notes?.trim()?.ifBlank { null },
                        apartmentId = apartmentId,
                        garageId = garageId,
                    ),
                )
                _state.value = _state.value.copy(busy = false, message = "Ödəniş qeydə alındı")
                onDone()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, formError = t.toUserMessage())
            }
        }
    }

    fun cancel(chargeId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null)
            try {
                repo.cancelOwnerCharge(chargeId)
                _state.value = _state.value.copy(busy = false, message = "Borc ləğv edildi")
                onDone()
                load(reset = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, actionError = t.toUserMessage())
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
