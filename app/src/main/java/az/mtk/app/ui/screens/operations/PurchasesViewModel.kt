package az.mtk.app.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvCreatePurchaseRequest
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InvPurchaseLineInput
import az.mtk.app.data.api.InvPurchaseResponse
import az.mtk.app.data.api.InvVendorListResult
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Redaktə olunan sətir — nomenklatura seçimi və mətn girişləri formada saxlanılır. */
data class PurchaseLineDraft(
    val key: Long,
    val nomenclatureId: String = "",
    val query: String = "",
    val quantity: String = "",
    val unitPrice: String = "",
)

class PurchasesViewModel : ViewModel() {

    data class UiState(
        val purchases: List<InvPurchaseResponse> = emptyList(),
        val total: Int = 0,
        val page: Int = 0,
        val pageSize: Int = 12,
        val query: String = "",
        val loading: Boolean = false,
        val error: String? = null,

        val detail: InvPurchaseResponse? = null,
        val detailLoading: Boolean = false,
        val detailError: String? = null,

        val actionBusy: String? = null,
        val message: String? = null,
        val actionError: String? = null,

        val formOpen: Boolean = false,
        val formSaving: Boolean = false,
        val formError: String? = null,
        val editingId: String? = null,
        val editingVendorName: String? = null,
        val vendorId: String = "",
        val purchaseDate: String = LocalDate.now().toString(),
        val note: String = "",
        val lines: List<PurchaseLineDraft> = emptyList(),

        val vendors: List<InvVendorListResult> = emptyList(),
        val nomenclatures: List<InvNomenclatureListItem> = emptyList(),
        val referenceLoading: Boolean = false,
    )

    private val repo = InventoryRepo.repository
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var nextLineKey = 1L

    init {
        load(reset = true)
    }

    /* ------------------------------- Siyahı ------------------------------- */

    fun load(reset: Boolean = false) {
        val snapshot = _state.value
        val page = if (reset) 0 else snapshot.page
        _state.value = snapshot.copy(loading = true, error = null, page = page)
        viewModelScope.launch {
            runCatching { repo.purchasePage(page = page, pageSize = snapshot.pageSize, search = snapshot.query) }
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        purchases = result.items,
                        total = result.totalCount,
                        page = result.page,
                        loading = false,
                    )
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.toUserMessage())
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
        if ((_state.value.page + 1) * _state.value.pageSize >= _state.value.total) return
        _state.value = _state.value.copy(page = _state.value.page + 1)
        load()
    }

    fun previousPage() {
        if (_state.value.page <= 0) return
        _state.value = _state.value.copy(page = _state.value.page - 1)
        load()
    }

    /* -------------------------------- Detal -------------------------------- */

    fun openDetail(purchase: InvPurchaseResponse) {
        _state.value = _state.value.copy(
            detail = purchase,
            detailError = null,
            detailLoading = true,
            message = null,
            actionError = null,
        )
        refreshDetail(purchase.id)
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detail = null, detailLoading = false, detailError = null)
    }

    fun refreshDetail(purchaseId: String) {
        viewModelScope.launch {
            runCatching { repo.purchase(purchaseId) }
                .onSuccess { fresh -> _state.value = _state.value.copy(detail = fresh, detailLoading = false) }
                .onFailure { failure ->
                    _state.value = _state.value.copy(detailLoading = false, detailError = failure.toUserMessage())
                }
        }
    }

    fun receive() {
        val purchase = _state.value.detail ?: return
        if (_state.value.actionBusy != null) return
        _state.value = _state.value.copy(actionBusy = "receive", actionError = null, message = null)
        viewModelScope.launch {
            runCatching { repo.receivePurchase(purchase.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        actionBusy = null,
                        message = "Satınalma qəbul edildi və anbara göndərildi.",
                    )
                    refreshDetail(purchase.id)
                    load()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(actionBusy = null, actionError = failure.toUserMessage())
                }
        }
    }

    fun cancel() {
        val purchase = _state.value.detail ?: return
        if (_state.value.actionBusy != null) return
        _state.value = _state.value.copy(actionBusy = "cancel", actionError = null, message = null)
        viewModelScope.launch {
            runCatching { repo.cancelPurchase(purchase.id, null) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        actionBusy = null,
                        message = "Satınalma ləğv edildi.",
                    )
                    refreshDetail(purchase.id)
                    load()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(actionBusy = null, actionError = failure.toUserMessage())
                }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, actionError = null, detailError = null)
    }

    /* -------------------------------- Forma -------------------------------- */

    fun openCreate() {
        _state.value = _state.value.copy(
            formOpen = true,
            formError = null,
            editingId = null,
            editingVendorName = null,
            vendorId = "",
            purchaseDate = LocalDate.now().toString(),
            note = "",
            lines = listOf(newLine()),
        )
        loadReferences()
    }

    fun openEdit(purchase: InvPurchaseResponse) {
        val lines = purchase.lines.orEmpty().map { line ->
            PurchaseLineDraft(
                key = nextLineKey++,
                nomenclatureId = line.nomenclatureId,
                query = listOfNotNull(line.nomenclatureCode, line.nomenclatureName)
                    .filter { it.isNotBlank() }
                    .joinToString(" — "),
                quantity = line.quantity?.toPlainNumber().orEmpty(),
                unitPrice = line.unitPrice?.toPlainNumber().orEmpty(),
            )
        }
        _state.value = _state.value.copy(
            formOpen = true,
            formError = null,
            editingId = purchase.id,
            editingVendorName = purchase.vendorName,
            vendorId = purchase.vendorId,
            purchaseDate = purchase.purchaseDate?.take(10).orEmpty().ifBlank { LocalDate.now().toString() },
            note = purchase.note.orEmpty(),
            lines = lines.ifEmpty { listOf(newLine()) },
        )
        loadReferences()
    }

    fun closeForm() {
        _state.value = _state.value.copy(formOpen = false, formError = null, formSaving = false)
    }

    fun setVendorId(value: String) {
        _state.value = _state.value.copy(vendorId = value, formError = null)
    }

    fun setDate(value: String) {
        _state.value = _state.value.copy(purchaseDate = value, formError = null)
    }

    fun setNote(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun addLine() {
        _state.value = _state.value.copy(lines = _state.value.lines + newLine())
    }

    fun removeLine(key: Long) {
        val remaining = _state.value.lines.filterNot { it.key == key }
        _state.value = _state.value.copy(lines = remaining.ifEmpty { listOf(newLine()) })
    }

    fun updateLineQuery(key: Long, value: String) {
        updateLine(key) { it.copy(query = value, nomenclatureId = "") }
    }

    fun selectNomenclature(key: Long, item: InvNomenclatureListItem) {
        val label = listOfNotNull(item.code, item.name).filter { it.isNotBlank() }.joinToString(" — ")
        updateLine(key) { it.copy(nomenclatureId = item.id, query = label) }
    }

    fun updateLineQuantity(key: Long, value: String) = updateLine(key) { it.copy(quantity = value) }

    fun updateLinePrice(key: Long, value: String) = updateLine(key) { it.copy(unitPrice = value) }

    private fun updateLine(key: Long, transform: (PurchaseLineDraft) -> PurchaseLineDraft) {
        _state.value = _state.value.copy(
            lines = _state.value.lines.map { if (it.key == key) transform(it) else it },
            formError = null,
        )
    }

    fun save() {
        val snapshot = _state.value
        if (snapshot.formSaving) return

        if (snapshot.vendorId.isBlank()) {
            _state.value = snapshot.copy(formError = "Tədarükçü seçin.")
            return
        }
        if (snapshot.purchaseDate.isBlank()) {
            _state.value = snapshot.copy(formError = "Satınalma tarixini qeyd edin.")
            return
        }
        if (snapshot.lines.isEmpty()) {
            _state.value = snapshot.copy(formError = "Ən azı bir sətir əlavə edin.")
            return
        }

        val requestLines = snapshot.lines.map { line ->
            InvPurchaseLineInput(
                nomenclatureId = line.nomenclatureId,
                quantity = line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0,
                unitPrice = line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0,
            )
        }
        val invalid = snapshot.lines.indices.any { index ->
            val line = snapshot.lines[index]
            val amount = requestLines[index]
            line.nomenclatureId.isBlank() || amount.quantity <= 0 || amount.unitPrice < 0
        }
        if (invalid) {
            _state.value = snapshot.copy(
                formError = "Hər sətirdə nomenklatura, müsbət miqdar və düzgün qiymət seçin.",
            )
            return
        }

        val request = InvCreatePurchaseRequest(
            vendorId = snapshot.vendorId,
            purchaseDate = snapshot.purchaseDate.take(10),
            note = snapshot.note.trim().takeIf { it.isNotEmpty() },
            lines = requestLines,
        )

        _state.value = snapshot.copy(formSaving = true, formError = null)
        viewModelScope.launch {
            val editingId = snapshot.editingId
            runCatching {
                if (editingId != null) {
                    repo.updatePurchase(editingId, request)
                    editingId
                } else {
                    repo.createPurchase(request)
                }
            }.onSuccess { purchaseId ->
                _state.value = _state.value.copy(
                    formSaving = false,
                    formOpen = false,
                    message = if (editingId != null) {
                        "Satınalma məlumatları yeniləndi."
                    } else {
                        "Satınalma yaradıldı."
                    },
                )
                load(reset = true)
                val current = _state.value.detail
                if (editingId != null) {
                    if (current?.id == purchaseId) refreshDetail(purchaseId)
                } else {
                    runCatching { repo.purchase(purchaseId) }.onSuccess { created ->
                        _state.value = _state.value.copy(detail = created)
                    }
                }
            }.onFailure { failure ->
                _state.value = _state.value.copy(formSaving = false, formError = failure.toUserMessage())
            }
        }
    }

    private fun loadReferences() {
        if (_state.value.vendors.isNotEmpty() && _state.value.nomenclatures.isNotEmpty()) return
        if (_state.value.referenceLoading) return
        _state.value = _state.value.copy(referenceLoading = true)
        viewModelScope.launch {
            runCatching {
                val vendors = repo.vendors(search = null).filter { it.isActive != false }
                val nomenclatures = repo.nomenclaturePage(page = 0, pageSize = 500, search = null)
                    .items.filter { it.isActive != false }
                vendors to nomenclatures
            }.onSuccess { (vendors, nomenclatures) ->
                _state.value = _state.value.copy(
                    vendors = vendors,
                    nomenclatures = nomenclatures,
                    referenceLoading = false,
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    referenceLoading = false,
                    formError = failure.toUserMessage(),
                )
            }
        }
    }

    private fun newLine(): PurchaseLineDraft = PurchaseLineDraft(key = nextLineKey++)

    private fun Double.toPlainNumber(): String =
        if (this == toLong().toDouble()) toLong().toString() else toString()
}
