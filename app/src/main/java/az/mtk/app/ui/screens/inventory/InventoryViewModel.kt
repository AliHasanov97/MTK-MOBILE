package az.mtk.app.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvLowStockDto
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InvNomenclatureResponse
import az.mtk.app.data.api.InvPurchaseHistoryItem
import az.mtk.app.data.api.InvRecordIssueRequest
import az.mtk.app.data.api.InvSaveNomenclatureRequest
import az.mtk.app.data.api.InvStockDto
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.data.api.InvUpdateNomenclatureRequest
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.data.api.InventoryRepository
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** İnventar modulunun dörd bölməsi — saytdakı eyni adlı səhifələr. */
enum class InventoryTab(val label: String) {
    STOCK("Anbar"),
    MATERIALS("Materiallar"),
    RECEIPTS("Daxilolmalar"),
    ISSUES("Çıxarışlar"),
}

enum class StockSegment(val label: String) {
    ON_HAND("Qalıqlar"),
    LOW("Az qalan"),
}

/** Formadan gələn material məlumatı (yaratma və redaktə üçün ortaq). */
data class NomenclatureInput(
    val code: String,
    val name: String,
    val description: String?,
    val category: Int,
    val unit: Int,
    val minStockLevel: Double?,
    val isActive: Boolean,
)

class InventoryViewModel : ViewModel() {

    private val repo = InventoryRepo.repository

    data class UiState(
        val selectedTab: InventoryTab = InventoryTab.STOCK,
        val stockSegment: StockSegment = StockSegment.ON_HAND,
        val query: String = "",
        val canManage: Boolean = false,

        // Anbar
        val stocks: List<InvStockDto>? = null,
        val lowStock: List<InvLowStockDto>? = null,
        val stockLoading: Boolean = false,
        val stockError: String? = null,

        // Materiallar
        val nomenclatures: List<InvNomenclatureListItem> = emptyList(),
        val nomenclaturesTotal: Int = 0,
        val nomenclaturesPage: Int = 0,
        val nomenclaturesLoading: Boolean = false,
        val nomenclaturesError: String? = null,

        // Daxilolmalar / Çıxarışlar
        val receipts: List<InvTransactionDto>? = null,
        val issues: List<InvTransactionDto>? = null,
        val transactionsLoading: Boolean = false,
        val transactionsError: String? = null,
        /** referenceId (purchaseId) -> qaimə nömrəsi. */
        val invoiceNumbers: Map<String, String?> = emptyMap(),

        // Ümumi bildirişlər
        val actionMessage: String? = null,
        val actionError: String? = null,

        // Material forması
        val formOpen: Boolean = false,
        val editing: InvNomenclatureResponse? = null,
        val formLoading: Boolean = false,
        val formSaving: Boolean = false,
        val formError: String? = null,

        // Silmə təsdiqi
        val deleteTarget: InvNomenclatureListItem? = null,
        val deleting: Boolean = false,

        // Alış tarixçəsi
        val historyTarget: InvNomenclatureListItem? = null,
        val history: List<InvPurchaseHistoryItem>? = null,
        val historyError: String? = null,

        // Çıxarış forması
        val issueFormOpen: Boolean = false,
        val issueOptions: List<InvNomenclatureListItem>? = null,
        val issueSaving: Boolean = false,
        val issueError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var nomenclatureRequest = 0

    fun setCanManage(value: Boolean) {
        _state.value = _state.value.copy(canManage = value)
    }

    /** İlk açılış: anbar qalıqları və materialların birinci səhifəsi. */
    fun load() {
        if (_state.value.stocks == null) loadStocks()
        if (_state.value.nomenclatures.isEmpty() && _state.value.nomenclaturesTotal == 0) {
            loadNomenclatures(page = 0, term = null)
        }
    }

    /** Yuxarıdaki "Yenilə" düyməsi — aktiv tabı yenidən yükləyir. */
    fun refresh() {
        when (_state.value.selectedTab) {
            InventoryTab.STOCK -> {
                loadStocks()
                if (_state.value.stockSegment == StockSegment.LOW) loadLowStock()
            }
            InventoryTab.MATERIALS ->
                loadNomenclatures(_state.value.nomenclaturesPage, _state.value.query.trim().ifBlank { null })
            InventoryTab.RECEIPTS -> loadTransactions(receipts = true)
            InventoryTab.ISSUES -> loadTransactions(receipts = false)
        }
    }

    fun selectTab(tab: InventoryTab) {
        if (_state.value.selectedTab == tab) return
        _state.value = _state.value.copy(selectedTab = tab, query = "")
        searchJob?.cancel()

        when (tab) {
            InventoryTab.STOCK -> if (_state.value.stocks == null) loadStocks()
            InventoryTab.MATERIALS -> loadNomenclatures(page = 0, term = null)
            InventoryTab.RECEIPTS -> if (_state.value.receipts == null) loadTransactions(receipts = true)
            InventoryTab.ISSUES -> if (_state.value.issues == null) loadTransactions(receipts = false)
        }
    }

    fun setStockSegment(segment: StockSegment) {
        if (_state.value.stockSegment == segment) return
        _state.value = _state.value.copy(stockSegment = segment, stockError = null)
        if (segment == StockSegment.LOW && _state.value.lowStock == null) loadLowStock()
    }

    /**
     * Axtarış: Anbar/Daxilolmalar/Çıxarışlar üçün siyahı daxilində (UI-da)
     * süzülür, Materiallar üçün isə serverdə axtarılır — saytdakı kimi 350 ms
     * gecikmə ilə.
     */
    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        if (_state.value.selectedTab != InventoryTab.MATERIALS) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadNomenclatures(page = 0, term = value.trim().ifBlank { null })
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }

    // ---------------------------------------------------------------- Anbar

    fun loadStocks() {
        _state.value = _state.value.copy(stockLoading = true, stockError = null)
        viewModelScope.launch {
            runCatching { repo.stocks() }
                .onSuccess { rows ->
                    _state.value = _state.value.copy(stocks = rows, stockLoading = false)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        stockLoading = false,
                        stockError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun loadLowStock() {
        _state.value = _state.value.copy(stockLoading = true, stockError = null)
        viewModelScope.launch {
            runCatching { repo.lowStock() }
                .onSuccess { rows ->
                    _state.value = _state.value.copy(lowStock = rows, stockLoading = false)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        stockLoading = false,
                        stockError = failure.toUserMessage(),
                    )
                }
        }
    }

    // ----------------------------------------------------------- Materiallar

    fun loadNomenclatures(page: Int = _state.value.nomenclaturesPage, term: String? = null) {
        val requestId = ++nomenclatureRequest
        _state.value = _state.value.copy(nomenclaturesLoading = true, nomenclaturesError = null)
        viewModelScope.launch {
            runCatching { repo.nomenclaturePage(page, InventoryRepository.PAGE_SIZE, term) }
                .onSuccess { result ->
                    if (requestId != nomenclatureRequest) return@onSuccess
                    _state.value = _state.value.copy(
                        nomenclatures = result.items,
                        nomenclaturesTotal = result.totalCount,
                        nomenclaturesPage = page,
                        nomenclaturesLoading = false,
                    )
                }
                .onFailure { failure ->
                    if (requestId != nomenclatureRequest) return@onFailure
                    _state.value = _state.value.copy(
                        nomenclaturesLoading = false,
                        nomenclaturesError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun nextPage() {
        val page = _state.value.nomenclaturesPage
        if ((page + 1) * InventoryRepository.PAGE_SIZE >= _state.value.nomenclaturesTotal) return
        loadNomenclatures(page + 1, _state.value.query.trim().ifBlank { null })
    }

    fun previousPage() {
        val page = _state.value.nomenclaturesPage
        if (page <= 0) return
        loadNomenclatures(page - 1, _state.value.query.trim().ifBlank { null })
    }

    fun openCreateForm() {
        _state.value = _state.value.copy(
            formOpen = true,
            editing = null,
            formLoading = false,
            formError = null,
        )
    }

    fun openEditForm(item: InvNomenclatureListItem) {
        _state.value = _state.value.copy(
            formOpen = true,
            editing = null,
            formLoading = true,
            formError = null,
        )
        viewModelScope.launch {
            runCatching { repo.nomenclature(item.id) }
                .onSuccess { full ->
                    _state.value = _state.value.copy(editing = full, formLoading = false)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        formOpen = false,
                        formLoading = false,
                        formError = null,
                        actionError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun closeForm() {
        _state.value = _state.value.copy(
            formOpen = false,
            editing = null,
            formLoading = false,
            formSaving = false,
            formError = null,
        )
    }

    fun saveNomenclature(input: NomenclatureInput) {
        val editing = _state.value.editing
        _state.value = _state.value.copy(formSaving = true, formError = null)
        viewModelScope.launch {
            runCatching {
                if (editing != null) {
                    repo.updateNomenclature(
                        editing.id,
                        InvUpdateNomenclatureRequest(
                            id = editing.id,
                            name = input.name,
                            description = input.description,
                            category = input.category,
                            unit = input.unit,
                            minStockLevel = input.minStockLevel,
                        ),
                    )
                } else {
                    repo.createNomenclature(
                        InvSaveNomenclatureRequest(
                            code = input.code,
                            name = input.name,
                            description = input.description,
                            category = input.category,
                            unit = input.unit,
                            minStockLevel = input.minStockLevel,
                            isActive = input.isActive,
                        ),
                    )
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    formSaving = false,
                    formOpen = false,
                    editing = null,
                    actionMessage = if (editing != null) "Material yeniləndi." else "Material yaradıldı.",
                )
                loadNomenclatures(page = 0, term = _state.value.query.trim().ifBlank { null })
                loadStocks()
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    formSaving = false,
                    formError = failure.toUserMessage(),
                )
            }
        }
    }

    fun askDelete(item: InvNomenclatureListItem) {
        _state.value = _state.value.copy(deleteTarget = item)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(deleteTarget = null, deleting = false)
    }

    fun confirmDelete() {
        val item = _state.value.deleteTarget ?: return
        _state.value = _state.value.copy(deleting = true)
        viewModelScope.launch {
            runCatching { repo.deleteNomenclature(item.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        deleting = false,
                        deleteTarget = null,
                        actionMessage = "«${item.name ?: "Material"}» silindi.",
                    )
                    loadNomenclatures(page = 0, term = _state.value.query.trim().ifBlank { null })
                    loadStocks()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        deleting = false,
                        deleteTarget = null,
                        actionError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun openHistory(item: InvNomenclatureListItem) {
        _state.value = _state.value.copy(
            historyTarget = item,
            history = null,
            historyError = null,
        )
        viewModelScope.launch {
            runCatching { repo.purchaseHistory(item.id) }
                .onSuccess { rows ->
                    _state.value = _state.value.copy(history = rows)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(historyError = failure.toUserMessage())
                }
        }
    }

    fun closeHistory() {
        _state.value = _state.value.copy(historyTarget = null, history = null, historyError = null)
    }

    // ------------------------------------------------- Daxilolma / Çıxarış

    fun loadTransactions(receipts: Boolean) {
        _state.value = _state.value.copy(transactionsLoading = true, transactionsError = null)
        viewModelScope.launch {
            val type = if (receipts) InvEnums.TRANSACTION_RECEIPT else InvEnums.TRANSACTION_ISSUE
            runCatching { repo.transactions(type) }
                .onSuccess { rows ->
                    _state.value = if (receipts) {
                        _state.value.copy(receipts = rows, transactionsLoading = false)
                    } else {
                        _state.value.copy(issues = rows, transactionsLoading = false)
                    }
                    if (receipts) loadInvoiceNumbers(rows)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        transactionsLoading = false,
                        transactionsError = failure.toUserMessage(),
                    )
                }
        }
    }

    /** Daxilolmalar üçün qaimə nömrələrini satınalmalardan tamamlayır. */
    private fun loadInvoiceNumbers(rows: List<InvTransactionDto>) {
        val known = _state.value.invoiceNumbers
        val ids = rows.mapNotNull { it.referenceId }
            .distinct()
            .filter { it !in known }
            .take(40)
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val resolved = coroutineScope {
                ids.map { id ->
                    async { id to runCatching { repo.purchase(id).invoiceNumber }.getOrNull() }
                }.awaitAll()
            }.toMap()
            _state.value = _state.value.copy(invoiceNumbers = _state.value.invoiceNumbers + resolved)
        }
    }

    fun openIssueForm() {
        _state.value = _state.value.copy(
            issueFormOpen = true,
            issueOptions = null,
            issueError = null,
        )
        viewModelScope.launch {
            runCatching { repo.nomenclaturePage(0, 500, null) }
                .onSuccess { result ->
                    _state.value = _state.value.copy(issueOptions = result.items)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        issueOptions = emptyList(),
                        issueError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun closeIssueForm() {
        _state.value = _state.value.copy(
            issueFormOpen = false,
            issueOptions = null,
            issueSaving = false,
            issueError = null,
        )
    }

    fun submitIssue(nomenclatureId: String, quantity: Double, notes: String?) {
        _state.value = _state.value.copy(issueSaving = true, issueError = null)
        viewModelScope.launch {
            runCatching {
                repo.recordIssue(
                    InvRecordIssueRequest(
                        nomenclatureId = nomenclatureId,
                        quantity = quantity,
                        notes = notes?.trim()?.ifBlank { null },
                    ),
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    issueSaving = false,
                    issueFormOpen = false,
                    issueOptions = null,
                    actionMessage = "Çıxarış qeydə alındı.",
                )
                loadTransactions(receipts = false)
                loadStocks()
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    issueSaving = false,
                    issueError = failure.toUserMessage(),
                )
            }
        }
    }
}
