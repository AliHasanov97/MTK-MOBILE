package az.mtk.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.FinTransactionDirection
import az.mtk.app.data.api.FinTransactionResponse
import az.mtk.app.data.api.InvPurchaseResponse
import az.mtk.app.data.api.OperationsRepository
import az.mtk.app.data.api.PaymentAllocationDto
import az.mtk.app.data.api.PaymentDto
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.abs

/** Saytdakı `Tranzaksiyalar` jurnalının mobil modeli. */
enum class TxKind(val label: String) {
    ALL("Hamısı"),
    INCOME("Gəlir"),
    EXPENSE("Xərc"),
}

/** Tarix aralığının hazır seçimləri — saytdakı `QUICK_RANGES` ilə eyni. */
enum class TxRangePreset(val label: String) {
    CURRENT_MONTH("Bu ay"),
    PREVIOUS_MONTH("Keçən ay"),
    CURRENT_YEAR("Bu il"),
    ALL("Hamısı"),
}

/** Jurnal sətri — avtomatik yazılan gəlir/xərc qeydi. */
data class TxRow(
    val id: String,
    val date: String,
    val income: Boolean,
    val direction: Int,
    val category: String,
    val note: String,
    /** İşarəli məbləğ: gəlir müsbət, xərc mənfi. */
    val amount: Double,
    val documentType: String,
    val referenceId: String,
) {
    val kind: TxKind get() = if (income) TxKind.INCOME else TxKind.EXPENSE
}

/** Ay üzrə qruplaşdırma — ay başlığında cəmlər göstərilir. */
data class TxMonthGroup(
    val key: String,
    val label: String,
    val rows: List<TxRow>,
    val income: Double,
    val expense: Double,
) {
    val net: Double get() = income - expense
}

/** Sətirə toxunanda açılan sənəd. */
sealed interface TxDocument {
    data class Payment(
        val payment: PaymentDto,
        val allocations: List<PaymentAllocationDto>,
    ) : TxDocument

    data class Purchase(val purchase: InvPurchaseResponse) : TxDocument

    data class Plain(val row: TxRow) : TxDocument
}

class TransactionsViewModel : ViewModel() {

    private val repo = OperationsRepository

    /** Saytın `FETCH_SIZE` dəyəri — filtrasiya tamamilə müştəri tərəfindədir. */
    private val fetchSize = 500

    data class UiState(
        val loading: Boolean = true,
        val error: String? = null,

        // Filtr nəticəsi
        val rows: List<TxRow> = emptyList(),
        val groups: List<TxMonthGroup> = emptyList(),
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val net: Double = 0.0,
        val categoryOptions: List<String> = emptyList(),

        // Filtrlər
        val search: String = "",
        val kind: TxKind = TxKind.ALL,
        val selectedCategories: Set<String> = emptySet(),
        val startDate: LocalDate? = LocalDate.now().withDayOfMonth(1),
        val endDate: LocalDate? = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
        val preset: TxRangePreset? = TxRangePreset.CURRENT_MONTH,

        // Sənəd görünüşü
        val detailRow: TxRow? = null,
        val detail: TxDocument? = null,
        val detailLoading: Boolean = false,
        val detailError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var allRows: List<TxRow> = emptyList()
    private var detailRequest = 0

    // ---- Yükləmə ------------------------------------------------------------

    fun load() {
        _state.value = _state.value.copy(
            loading = allRows.isEmpty(),
            error = null,
        )
        viewModelScope.launch {
            try {
                val page = repo.transactions(0, pageSize = fetchSize)
                allRows = page.items
                    .map { it.toRow() }
                    .sortedByDescending { parseIsoMillis(it.date) ?: 0L }
                _state.value = _state.value.copy(loading = false, error = null)
                recompute()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.toUserMessage())
            }
        }
    }

    // ---- Filtrlər -----------------------------------------------------------

    fun setSearch(value: String) {
        _state.value = _state.value.copy(search = value)
        recompute()
    }

    fun setKind(kind: TxKind) {
        _state.value = _state.value.copy(kind = kind)
        recompute()
    }

    fun toggleCategory(category: String) {
        val selected = _state.value.selectedCategories
        val next = if (category in selected) selected - category else selected + category
        _state.value = _state.value.copy(selectedCategories = next)
        recompute()
    }

    fun clearCategories() {
        _state.value = _state.value.copy(selectedCategories = emptySet())
        recompute()
    }

    fun applyPreset(preset: TxRangePreset) {
        val today = LocalDate.now()
        val (start, end) = when (preset) {
            TxRangePreset.CURRENT_MONTH ->
                today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            TxRangePreset.PREVIOUS_MONTH -> {
                val previous = today.minusMonths(1)
                previous.withDayOfMonth(1) to previous.withDayOfMonth(previous.lengthOfMonth())
            }
            TxRangePreset.CURRENT_YEAR -> LocalDate.of(today.year, 1, 1) to today
            TxRangePreset.ALL -> null to null
        }
        _state.value = _state.value.copy(startDate = start, endDate = end, preset = preset)
        recompute()
    }

    fun setRange(start: LocalDate?, end: LocalDate?) {
        _state.value = _state.value.copy(
            startDate = start,
            endDate = end,
            preset = null,
        )
        recompute()
    }

    // ---- Sənəd --------------------------------------------------------------

    fun openDocument(row: TxRow) {
        val request = ++detailRequest
        val type = row.documentType
        val isPayment = type == "ResidentPayment" || type == "VendorPayment"

        if (type != "Purchase" && !isPayment) {
            _state.value = _state.value.copy(
                detailRow = row,
                detail = TxDocument.Plain(row),
                detailLoading = false,
                detailError = null,
            )
            return
        }

        _state.value = _state.value.copy(
            detailRow = row,
            detail = null,
            detailLoading = true,
            detailError = null,
        )
        viewModelScope.launch {
            try {
                when {
                    type == "Purchase" -> {
                        val purchase = repo.purchase(row.referenceId)
                        if (request != detailRequest) return@launch
                        _state.value = _state.value.copy(
                            detail = TxDocument.Purchase(purchase),
                            detailLoading = false,
                        )
                    }
                    else -> {
                        val payment = repo.payment(row.referenceId)
                        val allocations = runCatching {
                            repo.paymentAllocations(row.referenceId)
                        }.getOrDefault(emptyList())
                        if (request != detailRequest) return@launch
                        _state.value = _state.value.copy(
                            detail = TxDocument.Payment(payment, allocations),
                            detailLoading = false,
                        )
                    }
                }
            } catch (t: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detailLoading = false,
                    detailError = t.toUserMessage(),
                )
            }
        }
    }

    fun closeDocument() {
        detailRequest++
        _state.value = _state.value.copy(
            detailRow = null,
            detail = null,
            detailLoading = false,
            detailError = null,
        )
    }

    // ---- Hesablama ----------------------------------------------------------

    private fun recompute() {
        val s = _state.value
        val term = s.search.trim().lowercase()
        val startMs = s.startDate?.let(::startOfDayMs)
        val endMs = s.endDate?.let(::endOfDayMs)

        val filtered = allRows.filter { row ->
            val rowMs = parseIsoMillis(row.date)
            if (startMs != null && (rowMs == null || rowMs < startMs)) return@filter false
            if (endMs != null && (rowMs == null || rowMs > endMs)) return@filter false
            if (s.kind != TxKind.ALL && row.kind != s.kind) return@filter false
            if (s.selectedCategories.isNotEmpty() && row.category !in s.selectedCategories) return@filter false
            if (term.isNotEmpty() &&
                !row.category.lowercase().contains(term) &&
                !row.note.lowercase().contains(term)
            ) {
                return@filter false
            }
            true
        }

        val totalIncome = filtered.filter { it.income }.sumOf { it.amount }
        val totalExpense = filtered.filter { !it.income }.sumOf { abs(it.amount) }

        val grouped = LinkedHashMap<String, MutableList<TxRow>>()
        filtered.forEach { row ->
            grouped.getOrPut(row.date.take(7)) { mutableListOf() }.add(row)
        }
        val groups = grouped.map { (key, rows) ->
            TxMonthGroup(
                key = key,
                label = monthLabel(key),
                rows = rows,
                income = rows.filter { it.income }.sumOf { it.amount },
                expense = rows.filter { !it.income }.sumOf { abs(it.amount) },
            )
        }

        _state.value = _state.value.copy(
            rows = filtered,
            groups = groups,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            net = totalIncome - totalExpense,
            categoryOptions = allRows
                .map { it.category }
                .distinct()
                .sortedBy { it.lowercase() },
        )
    }

    private fun FinTransactionResponse.toRow(): TxRow {
        val income = direction == FinTransactionDirection.INCOME
        val raw = amount ?: 0.0
        return TxRow(
            id = id,
            date = transactionDate ?: createdAt ?: "",
            income = income,
            direction = direction ?: FinTransactionDirection.INCOME,
            category = category?.takeIf { it.isNotBlank() } ?: "—",
            note = description?.takeIf { it.isNotBlank() } ?: "—",
            amount = if (income) raw else -raw,
            documentType = documentType ?: "LedgerEntry",
            referenceId = referenceId?.takeIf { it.isNotBlank() } ?: id,
        )
    }

    private fun parseIsoMillis(raw: String): Long? {
        if (raw.isBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC).toEpochMilli() }
            .recoverCatching {
                LocalDate.parse(raw.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
            .getOrNull()
    }

    private fun startOfDayMs(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun endOfDayMs(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1

    private fun monthLabel(key: String): String {
        val parts = key.split("-")
        val month = parts.getOrNull(1)?.toIntOrNull()
        val year = parts.getOrNull(0) ?: key
        return if (month != null && month in 1..12) "${MONTHS[month - 1]} $year" else key
    }

    private companion object {
        val MONTHS = listOf(
            "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
            "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
        )
    }
}
