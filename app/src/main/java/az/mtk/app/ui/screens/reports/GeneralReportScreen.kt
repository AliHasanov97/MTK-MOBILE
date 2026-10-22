package az.mtk.app.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ReportCompanyBalanceDto
import az.mtk.app.data.api.ReportExports
import az.mtk.app.data.api.ReportTransactionDto
import az.mtk.app.data.api.ReportTransactionsPage
import az.mtk.app.data.api.ReportsApi
import az.mtk.app.data.api.RESIDENT_PAYMENT_CATEGORY
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.TransactionDirection
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GeneralReportExpenseRow(
    val id: String,
    val category: String,
    val description: String,
    val amount: Double,
)

data class GeneralReportSummary(
    val collected: Double = 0.0,
    val otherIncome: Double = 0.0,
    val previousBalance: Double = 0.0,
    val expenseRows: List<GeneralReportExpenseRow> = emptyList(),
) {
    val totalExpense: Double get() = expenseRows.sumOf { it.amount }
    val monthlyIncome: Double get() = collected + otherIncome
    val monthlyBalance: Double get() = previousBalance + monthlyIncome
    val finalBalance: Double get() = monthlyBalance - totalExpense
}

data class GeneralReportState(
    val loading: Boolean = true,
    val error: String? = null,
    val transactions: List<ReportTransactionDto> = emptyList(),
    val companyBalance: ReportCompanyBalanceDto? = null,
) {
    fun summaryFor(year: Int, month: Int): GeneralReportSummary {
        val startMs = LocalDate.of(year, month, 1).atStartOfDay()
            .toInstant(ZoneIdUtc).toEpochMilli()
        val endMs = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay()
            .toInstant(ZoneIdUtc).toEpochMilli()

        var collected = 0.0
        var otherIncome = 0.0
        var previousBalance = 0.0
        val expenses = mutableListOf<GeneralReportExpenseRow>()

        for (t in transactions) {
            val ms = transactionMs(t) ?: continue
            val amount = t.amount ?: 0.0
            val signed = if (t.direction == TransactionDirection.INCOME) amount else -amount

            if (ms < startMs) {
                previousBalance += signed
                continue
            }
            if (ms >= endMs) continue

            if (t.direction == TransactionDirection.INCOME) {
                if (t.category == RESIDENT_PAYMENT_CATEGORY) collected += amount else otherIncome += amount
            } else {
                expenses.add(
                    GeneralReportExpenseRow(
                        id = t.id,
                        category = t.category ?: "—",
                        description = t.description?.takeIf { it.isNotBlank() } ?: "—",
                        amount = amount,
                    ),
                )
            }
        }

        val sorted = expenses.sortedWith(
            compareBy({ it.category }, { it.description }),
        )
        return GeneralReportSummary(collected, otherIncome, previousBalance, sorted)
    }

    companion object {
        private val ZoneIdUtc = java.time.ZoneOffset.UTC
        private fun transactionMs(t: ReportTransactionDto): Long? =
            t.transactionDate?.let { raw ->
                runCatching { java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
                    .recoverCatching { java.time.LocalDateTime.parse(raw).toInstant(ZoneIdUtc).toEpochMilli() }
                    .getOrNull()
            }
    }
}

class GeneralReportViewModel : ViewModel() {
    val now = LocalDate.now()

    val year = MutableStateFlow(now.year)
    val month = MutableStateFlow(now.monthValue)
    val state = MutableStateFlow(GeneralReportState())

    init {
        load()
    }

    fun shiftMonth(delta: Int) {
        val shifted = LocalDate.of(year.value, month.value, 1).plusMonths(delta.toLong())
        year.value = shifted.year
        month.value = shifted.monthValue
    }

    fun load() {
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, error = null)
            runCatching {
                val api = AppGraph.service(ReportsApi::class.java)
                val txPage = api.transactions(
                    SearchBody(
                        sortCriteria = SortCriteria("TransactionDate", Sort.ASCENDING),
                        pageSize = 3000,
                    ),
                ).data ?: ReportTransactionsPage(emptyList(), 0, 0, 0)
                val balance = runCatching { api.companyBalance().data }.getOrNull()
                txPage to balance
            }
                .onSuccess { (txPage, balance) ->
                    state.value = state.value.copy(
                        loading = false,
                        error = null,
                        transactions = txPage.transactions.orEmpty(),
                        companyBalance = balance,
                    )
                }
                .onFailure { failure ->
                    state.value = state.value.copy(loading = false, error = failure.toUserMessage())
                }
        }
    }
}

private val MONTHS_FULL = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
    "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
)

@Composable
fun GeneralReportScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: GeneralReportViewModel = mtkViewModel { GeneralReportViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val year by viewModel.year.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val summary = state.summaryFor(year, month)
    val today = LocalDate.now()
    val generatedOn = "%02d.%02d.%d".format(today.dayOfMonth, today.monthValue, today.year)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Ümumi hesabat",
                subtitle = "${MONTHS_FULL[month - 1]} $year",
                onBack = onBack,
            )
        }

        if (state.loading && state.transactions.isEmpty()) {
            LoadingState("Hesabat hazırlanır…")
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Əvvəlki ay")
                    }
                    Text(
                        "${MONTHS_FULL[month - 1]} ayının hesabatı ($year)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { viewModel.shiftMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Növbəti ay")
                    }
                }
            }

            state.companyBalance?.let { balance ->
                item {
                    MtkCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Rəsmi balans (bütün tarix üzrə)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                                Text(
                                    Format.money(balance.currentBalance),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((balance.currentBalance ?: 0.0) < 0) {
                                        MtkColors.Negative
                                    } else {
                                        MtkColors.Positive
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                item { ErrorState(state.error!!, onRetry = viewModel::load) }
            }

            // Gəlirlər
            item {
                MtkCard {
                    SectionHeading("Aylıq gəlirlər", subtitle = "manatla")
                    Spacer(Modifier.height(6.dp))
                    InfoRow("Cari ay faktiki yığım", Format.money(summary.collected))
                    InfoRow("Digər gəlirlər", Format.money(summary.otherIncome))
                    InfoRow(
                        "Əvvəlki balans",
                        Format.money(summary.previousBalance),
                    )
                    InfoRow("Cari ay ümumi gəlir", Format.money(summary.monthlyIncome))
                }
            }

            // Xərclər — kateqoriyalar üzrə qruplaşdırılmış siyahı
            item {
                MtkCard {
                    SectionHeading(
                        "Aylıq xərclər",
                        subtitle = if (summary.expenseRows.isEmpty()) "Bu ay üzrə xərc qeydə alınmayıb" else null,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (summary.expenseRows.isEmpty()) {
                        EmptyState("Bu ay üzrə xərc qeydə alınmayıb")
                    } else {
                        var lastCategory: String? = null
                        var groupNo = 0
                        summary.expenseRows.forEach { row ->
                            if (row.category != lastCategory) {
                                groupNo += 1
                                lastCategory = row.category
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "$groupNo. ${row.category}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            ListRow(
                                title = row.description,
                                trailing = {
                                    Text(
                                        Format.money(row.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Cəmi xərc", Format.money(summary.totalExpense))
                    }
                }
            }

            // Yekun
            item {
                MtkCard {
                    SectionHeading("Yekun hesabat", subtitle = "manatla")
                    Spacer(Modifier.height(6.dp))
                    InfoRow("Əvvəlki balans", Format.money(summary.previousBalance))
                    InfoRow("Cari aylıq gəlir", Format.money(summary.collected))
                    InfoRow("Digər gəlirlər", Format.money(summary.otherIncome))
                    InfoRow("Aylıq cəmi balans", Format.money(summary.monthlyBalance))
                    InfoRow("Cəmi aylıq xərc", Format.money(summary.totalExpense))
                    InfoRow("Yekun balans", Format.money(summary.finalBalance))
                }
            }

            // Statistikadakı iki əsas rəqəm də tile kimi təqdim olunur
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Ümumi gəlir",
                        value = Format.money(summary.monthlyIncome),
                        caption = "${MONTHS_FULL[month - 1]} ayı üçün",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Yekun balans",
                        value = Format.money(summary.finalBalance),
                        caption = "Xərclərdən sonra",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                MtkCard {
                    SectionHeading("İxrac", subtitle = "Seçilmiş dövr üçün fayllar")
                    ExportActionRow(
                        title = "Aylıq hesabat (PDF)",
                        subtitle = "${MONTHS_FULL[month - 1]} $year",
                        path = ReportExports.monthlyPdf(year, month),
                        scope = scope,
                    )
                }
            }

            item {
                Text(
                    "Hazırlanma tarixi: $generatedOn",
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedSoft,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
