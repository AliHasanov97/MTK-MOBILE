package az.mtk.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.RESIDENT_PAYMENT_CATEGORY
import az.mtk.app.data.api.ReportCompanyBalanceDto
import az.mtk.app.data.api.ReportMonthCell
import az.mtk.app.data.api.ReportProperty
import az.mtk.app.data.api.ReportPropertyAnnualRowDto
import az.mtk.app.data.api.ReportPropertyType
import az.mtk.app.data.api.ReportScheduleRow
import az.mtk.app.data.api.ReportTransactionDto
import az.mtk.app.data.api.ReportsRepository
import az.mtk.app.data.api.TransactionDirection
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** İki hesabat növü — saytdakı iki ayrı səhifənin mobil tab qarşılığı. */
enum class ReportsTab(val label: String) {
    SCHEDULE("Ödəniş qrafiki"),
    MONTHLY("Ümumi hesabat"),
}

enum class ReportKind(val label: String, val propertyType: Int) {
    APARTMENT("Mənzillər", ReportPropertyType.APARTMENT),
    GARAGE("Qarajlar", ReportPropertyType.GARAGE),
}

enum class ReportDebtFilter(val id: String, val label: String) {
    DEBT("debt", "Borcu var"),
    CLEAR("clear", "Borcu yoxdur"),
}

data class ReportExpenseLine(
    val id: String,
    val category: String,
    val description: String,
    val amount: Double,
)

data class ReportExpenseGroup(
    val category: String,
    val lines: List<ReportExpenseLine>,
    val total: Double,
)

data class ReportMonthlySummary(
    val collected: Double = 0.0,
    val otherIncome: Double = 0.0,
    val previousBalance: Double = 0.0,
    val groups: List<ReportExpenseGroup> = emptyList(),
    val totalExpense: Double = 0.0,
) {
    val monthlyIncome: Double get() = collected + otherIncome
    val monthlyBalance: Double get() = previousBalance + monthlyIncome
    val finalBalance: Double get() = monthlyBalance - totalExpense
}

data class ScheduleState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val year: Int = LocalDate.now().year,
    val kind: ReportKind = ReportKind.APARTMENT,
    val search: String = "",
    /** Bütün əmlaklar (hər iki növ) — il dəyişəndə yenidən çəkilmir. */
    val allRows: List<ReportScheduleRow> = emptyList(),
    val visibleRows: List<ReportScheduleRow> = emptyList(),
    val buildingOptions: List<Pair<String, String>> = emptyList(),
    val propertyOptions: List<Pair<String, String>> = emptyList(),
    val buildingFilter: Set<String> = emptySet(),
    val propertyFilter: Set<String> = emptySet(),
    val debtFilter: Set<ReportDebtFilter> = emptySet(),
    val totalDebt: Double = 0.0,
    val debtors: Int = 0,
) {
    val activeFilterCount: Int
        get() = (if (kind == ReportKind.APARTMENT && buildingFilter.isNotEmpty()) 1 else 0) +
            (if (propertyFilter.isNotEmpty()) 1 else 0) +
            (if (debtFilter.isNotEmpty()) 1 else 0)
}

data class MonthlyState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val summary: ReportMonthlySummary = ReportMonthlySummary(),
    val companyBalance: ReportCompanyBalanceDto? = null,
    val transactionCount: Int = 0,
)

data class ReportsUiState(
    val schedule: ScheduleState = ScheduleState(),
    val monthly: MonthlyState = MonthlyState(),
)

/**
 * Hesabatlar modulunun vəziyyəti.
 *
 * Ödəniş qrafiki: mənzil/qaraj siyahıları + illik hesabat bir dəfə yüklənir,
 * il dəyişəndə yalnız illik hesabat yenilənir.
 * Ümumi hesabat: tranzaksiya jurnalı bir dəfə yüklənir, ay dəyişəndə aqreqasiya
 * tamamilə müştəri tərəfində yenidən hesablanır.
 */
class ReportsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state = _state.asStateFlow()

    private var transactions: List<ReportTransactionDto> = emptyList()
    private var scheduleRequest = 0
    private var monthlyRequest = 0

    fun yearOptions(): List<Int> {
        val current = LocalDate.now().year
        return (current + 1 downTo current - 4).toList()
    }

    // ---- Ödəniş qrafiki ------------------------------------------------------

    fun ensureSchedule() {
        val schedule = _state.value.schedule
        if (!schedule.loaded && !schedule.loading) refreshSchedule()
    }

    fun refreshSchedule() {
        val request = ++scheduleRequest
        val year = _state.value.schedule.year
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(loading = true, error = null),
        )
        viewModelScope.launch {
            try {
                val properties = ReportsRepository.loadProperties()
                val report = ReportsRepository.annualRows(year)
                if (request != scheduleRequest) return@launch
                val rows = buildScheduleRows(properties, report)
                _state.value = _state.value.copy(
                    schedule = _state.value.schedule.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        allRows = rows,
                    ),
                )
                recomputeSchedule()
            } catch (t: Throwable) {
                if (request != scheduleRequest) return@launch
                _state.value = _state.value.copy(
                    schedule = _state.value.schedule.copy(
                        loading = false,
                        error = t.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun setYear(year: Int) {
        val schedule = _state.value.schedule
        if (schedule.year == year) return

        if (!schedule.loaded || schedule.allRows.isEmpty()) {
            _state.value = _state.value.copy(
                schedule = schedule.copy(year = year, propertyFilter = emptySet()),
            )
            refreshSchedule()
            return
        }

        val request = ++scheduleRequest
        _state.value = _state.value.copy(
            schedule = schedule.copy(
                year = year,
                loading = true,
                error = null,
                propertyFilter = emptySet(),
                visibleRows = emptyList(),
                totalDebt = 0.0,
                debtors = 0,
            ),
        )
        viewModelScope.launch {
            try {
                val report = ReportsRepository.annualRows(year)
                if (request != scheduleRequest) return@launch
                val properties = _state.value.schedule.allRows.map { it.property }
                val rows = buildScheduleRows(properties, report)
                _state.value = _state.value.copy(
                    schedule = _state.value.schedule.copy(
                        loading = false,
                        error = null,
                        allRows = rows,
                    ),
                )
                recomputeSchedule()
            } catch (t: Throwable) {
                if (request != scheduleRequest) return@launch
                _state.value = _state.value.copy(
                    schedule = _state.value.schedule.copy(
                        loading = false,
                        error = t.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun setKind(kind: ReportKind) {
        if (_state.value.schedule.kind == kind) return
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(kind = kind, propertyFilter = emptySet()),
        )
        recomputeSchedule()
    }

    fun setSearch(value: String) {
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(search = value),
        )
        recomputeSchedule()
    }

    fun toggleBuilding(id: String) {
        val selected = _state.value.schedule.buildingFilter
        val next = if (id in selected) selected - id else selected + id
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(buildingFilter = next),
        )
        recomputeSchedule()
    }

    fun clearBuildingFilter() {
        if (_state.value.schedule.buildingFilter.isEmpty()) return
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(buildingFilter = emptySet()),
        )
        recomputeSchedule()
    }

    fun toggleProperty(key: String) {
        val selected = _state.value.schedule.propertyFilter
        val next = if (key in selected) selected - key else selected + key
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(propertyFilter = next),
        )
        recomputeSchedule()
    }

    fun clearPropertyFilter() {
        if (_state.value.schedule.propertyFilter.isEmpty()) return
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(propertyFilter = emptySet()),
        )
        recomputeSchedule()
    }

    fun toggleDebt(filter: ReportDebtFilter) {
        val selected = _state.value.schedule.debtFilter
        val next = if (filter in selected) selected - filter else selected + filter
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(debtFilter = next),
        )
        recomputeSchedule()
    }

    fun clearDebtFilter() {
        if (_state.value.schedule.debtFilter.isEmpty()) return
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(debtFilter = emptySet()),
        )
        recomputeSchedule()
    }

    fun clearFilters() {
        _state.value = _state.value.copy(
            schedule = _state.value.schedule.copy(
                buildingFilter = emptySet(),
                propertyFilter = emptySet(),
                debtFilter = emptySet(),
            ),
        )
        recomputeSchedule()
    }

    private fun recomputeSchedule() {
        val schedule = _state.value.schedule
        val term = schedule.search.trim().lowercase()

        val buildingOptions = schedule.allRows.asSequence()
            .map { it.property }
            .filter { it.isApartment && it.buildingId != null }
            .distinctBy { it.buildingId }
            .map { it.buildingId!! to (it.buildingName ?: "—") }
            .sortedBy { it.second.lowercase() }
            .toList()

        val propertyOptions = schedule.allRows.asSequence()
            .map { it.property }
            .filter { it.kind == schedule.kind.propertyType }
            .map { property ->
                val label = if (property.isApartment && !property.buildingName.isNullOrBlank()) {
                    "${property.label} · ${property.buildingName}"
                } else {
                    property.label
                }
                property.key to label
            }
            .sortedBy { it.second.lowercase() }
            .toList()

        val visible = schedule.allRows.filter { row ->
            if (row.property.kind != schedule.kind.propertyType) return@filter false
            if (schedule.kind == ReportKind.APARTMENT &&
                schedule.buildingFilter.isNotEmpty() &&
                row.property.buildingId !in schedule.buildingFilter
            ) {
                return@filter false
            }
            if (schedule.propertyFilter.isNotEmpty() && row.property.key !in schedule.propertyFilter) {
                return@filter false
            }
            if (term.isNotEmpty()) {
                val haystack = "${row.property.label} ${row.property.buildingName.orEmpty()}".lowercase()
                if (!haystack.contains(term)) return@filter false
            }
            if (schedule.debtFilter.isNotEmpty()) {
                val matches = if (row.hasDebt) ReportDebtFilter.DEBT else ReportDebtFilter.CLEAR
                if (matches !in schedule.debtFilter) return@filter false
            }
            true
        }

        _state.value = _state.value.copy(
            schedule = schedule.copy(
                visibleRows = visible,
                buildingOptions = buildingOptions,
                propertyOptions = propertyOptions,
                totalDebt = visible.sumOf { it.currentDebt.coerceAtLeast(0.0) },
                debtors = visible.count { it.hasDebt },
            ),
        )
    }

    private fun buildScheduleRows(
        properties: List<ReportProperty>,
        report: Map<String, ReportPropertyAnnualRowDto>,
    ): List<ReportScheduleRow> = properties
        .map { property -> buildScheduleRow(property, report[property.id]) }
        .sortedWith(
            compareBy(
                { it.property.buildingName.orEmpty().lowercase() },
                { it.property.label.lowercase() },
            ),
        )

    private fun buildScheduleRow(
        property: ReportProperty,
        dto: ReportPropertyAnnualRowDto?,
    ): ReportScheduleRow {
        val byMonth = dto?.months.orEmpty()
            .mapNotNull { cell -> cell.month?.let { it to cell } }
            .toMap()
        val months = (1..12).map { month ->
            val cell = byMonth[month] ?: dto?.months?.getOrNull(month - 1)
            ReportMonthCell(
                month = month,
                amount = cell?.amount ?: 0.0,
                paidAmount = cell?.paidAmount ?: 0.0,
                status = cell?.status,
            )
        }
        return ReportScheduleRow(
            property = property,
            months = months,
            currentDebt = dto?.currentDebt ?: 0.0,
        )
    }

    // ---- Ümumi hesabat -------------------------------------------------------

    fun ensureMonthly() {
        val monthly = _state.value.monthly
        if (!monthly.loaded && !monthly.loading) refreshMonthly()
    }

    fun refreshMonthly() {
        val request = ++monthlyRequest
        _state.value = _state.value.copy(
            monthly = _state.value.monthly.copy(loading = true, error = null),
        )
        viewModelScope.launch {
            try {
                val loadedTransactions = ReportsRepository.loadTransactions()
                val company = runCatching { ReportsRepository.companyBalance() }.getOrNull()
                if (request != monthlyRequest) return@launch
                transactions = loadedTransactions
                _state.value = _state.value.copy(
                    monthly = _state.value.monthly.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        companyBalance = company,
                        transactionCount = loadedTransactions.size,
                    ),
                )
                recomputeMonthly()
            } catch (t: Throwable) {
                if (request != monthlyRequest) return@launch
                _state.value = _state.value.copy(
                    monthly = _state.value.monthly.copy(
                        loading = false,
                        error = t.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun setMonth(year: Int, month: Int) {
        val monthly = _state.value.monthly
        if (monthly.year == year && monthly.month == month) return

        if (!monthly.loaded) {
            _state.value = _state.value.copy(
                monthly = monthly.copy(year = year, month = month),
            )
            refreshMonthly()
            return
        }

        _state.value = _state.value.copy(
            monthly = monthly.copy(year = year, month = month),
        )
        recomputeMonthly()
    }

    fun shiftMonth(delta: Int) {
        val monthly = _state.value.monthly
        val shifted = LocalDate.of(monthly.year, monthly.month, 1).plusMonths(delta.toLong())
        setMonth(shifted.year, shifted.monthValue)
    }

    private fun recomputeMonthly() {
        val monthly = _state.value.monthly
        val start = LocalDate.of(monthly.year, monthly.month, 1)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(monthly.year, monthly.month, 1).plusMonths(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        var collected = 0.0
        var otherIncome = 0.0
        var previousBalance = 0.0
        val lines = mutableListOf<ReportExpenseLine>()

        transactions.forEach { transaction ->
            val millis = parseIsoMillis(transaction.transactionDate) ?: return@forEach
            val amount = transaction.amount ?: 0.0
            val income = (transaction.direction ?: TransactionDirection.INCOME) != TransactionDirection.EXPENSE
            val signed = if (income) amount else -amount

            if (millis < start) {
                previousBalance += signed
                return@forEach
            }
            if (millis >= end) return@forEach

            if (income) {
                if (transaction.category == RESIDENT_PAYMENT_CATEGORY) {
                    collected += amount
                } else {
                    otherIncome += amount
                }
            } else {
                lines += ReportExpenseLine(
                    id = transaction.id,
                    category = transaction.category?.takeIf { it.isNotBlank() } ?: "—",
                    description = transaction.description?.takeIf { it.isNotBlank() } ?: "—",
                    amount = amount,
                )
            }
        }

        val sorted = lines.sortedWith(
            compareBy({ it.category.lowercase() }, { it.description.lowercase() }),
        )
        val groups = sorted.groupBy { it.category }.map { (category, categoryLines) ->
            ReportExpenseGroup(
                category = category,
                lines = categoryLines,
                total = categoryLines.sumOf { it.amount },
            )
        }

        _state.value = _state.value.copy(
            monthly = monthly.copy(
                summary = ReportMonthlySummary(
                    collected = collected,
                    otherIncome = otherIncome,
                    previousBalance = previousBalance,
                    groups = groups,
                    totalExpense = groups.sumOf { it.total },
                ),
            ),
        )
    }

    private fun parseIsoMillis(raw: String?): Long? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            .recoverCatching {
                LocalDate.parse(value.take(10)).atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli()
            }
            .getOrNull()
    }
}
