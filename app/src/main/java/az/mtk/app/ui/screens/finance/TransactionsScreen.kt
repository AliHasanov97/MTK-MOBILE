package az.mtk.app.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.FinCreateTransactionRequest
import az.mtk.app.data.api.FinTransactionDirection
import az.mtk.app.data.api.FinTransactionResponse
import az.mtk.app.data.api.FinTransactionsSummary
import az.mtk.app.data.api.FinanceRepository
import az.mtk.app.data.api.finTransactionDirectionLabel
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionsSummaryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val summary: FinTransactionsSummary? = null,
)

class TransactionsViewModel : ViewModel() {
    private val _summary = MutableStateFlow(TransactionsSummaryUiState())
    val summary = _summary.asStateFlow()

    val pager = Pager<FinTransactionResponse>(viewModelScope) { page, query ->
        FinanceRepository.transactions(page, search = query)
    }

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = _summary.value.copy(loading = true, error = null)
            runCatching { FinanceRepository.transactionsSummary() }
                .onSuccess { _summary.value = TransactionsSummaryUiState(loading = false, summary = it) }
                .onFailure { failure ->
                    _summary.value = _summary.value.copy(loading = false, error = failure.toUserMessage())
                }
        }
    }

    fun createTransaction(
        direction: Int,
        category: String,
        amount: Double,
        description: String?,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                FinanceRepository.createTransaction(
                    FinCreateTransactionRequest(
                        direction = direction,
                        category = category,
                        amount = amount,
                        description = description,
                    ),
                )
            }.onSuccess {
                pager.refresh()
                loadSummary()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun TransactionsScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: TransactionsViewModel = mtkViewModel { TransactionsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val summaryState by viewModel.summary.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Tranzaksiyalar",
                subtitle = "Avtomatik jurnal",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val s = summaryState.summary
                StatTile(
                    label = "Daxilolma",
                    value = Format.money(s?.totalIncome),
                    caption = if (summaryState.loading) "Yüklənir…" else null,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Xərc",
                    value = Format.money(s?.totalExpense),
                    caption = if (summaryState.loading) "Yüklənir…" else null,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Kateqoriya və ya qeyd üzrə…",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni tranzaksiya", onClick = { showCreate = true })
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Tranzaksiyalar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Bu aralıqda tranzaksiya yoxdur")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { tx ->
                    val isIncome = tx.direction == FinTransactionDirection.INCOME
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    tx.category ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!tx.description.isNullOrBlank()) {
                                    Text(
                                        tx.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Pill(
                                finTransactionDirectionLabel(tx.direction),
                                if (isIncome) Tone.Success else Tone.Danger,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text(
                                    Format.dateTime(tx.transactionDate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                )
                                if (tx.documentType != null && tx.documentType != "LedgerEntry") {
                                    Text(
                                        "Avtomatik qeyd",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MtkColors.MutedSoft,
                                    )
                                }
                            }
                            Text(
                                if (isIncome) "+${Format.money(tx.amount)}" else "−${Format.money(tx.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isIncome) MtkColors.Positive else MtkColors.Negative,
                            )
                        }
                    }
                }

                if (state.items.size < state.total) {
                    item {
                        OutlinedButton(
                            onClick = viewModel.pager::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = viewModel.pager::refresh) }
                }
            }
        }
    }

    if (showCreate) {
        TransactionCreateModal(
            onDismiss = { showCreate = false },
            onSubmit = { direction, category, amount, description, onDone ->
                viewModel.createTransaction(direction, category, amount, description, onDone)
            },
        )
    }
}

@Composable
private fun TransactionCreateModal(
    onDismiss: () -> Unit,
    onSubmit: (direction: Int, category: String, amount: Double, description: String?, onDone: (String?) -> Unit) -> Unit,
) {
    var direction by remember { mutableStateOf(FinTransactionDirection.EXPENSE) }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val numericAmount = amount.replace(',', '.').toDoubleOrNull()
    val canSubmit = category.isNotBlank() && numericAmount != null && numericAmount > 0

    MtkModal(
        title = "Yeni tranzaksiya",
        subtitle = "Haqq və ya ödənişlə əlaqəsi olmayan əl ilə jurnal qeydi.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Qeyd et",
        confirmEnabled = canSubmit && !saving,
        onConfirm = {
            if (!canSubmit || saving) return@MtkModal
            saving = true
            error = null
            onSubmit(
                direction,
                category.trim(),
                numericAmount!!,
                description.trim().ifBlank { null },
            ) { err ->
                saving = false
                if (err == null) onDismiss() else error = err
            }
        },
    ) {
        MtkSelect(
            value = direction.toString(),
            onValueChange = { direction = it.toIntOrNull() ?: FinTransactionDirection.EXPENSE },
            label = "Növ",
            required = true,
            options = listOf(
                MtkOption(FinTransactionDirection.EXPENSE.toString(), "Xərc"),
                MtkOption(FinTransactionDirection.INCOME.toString(), "Gəlir"),
            ),
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = category,
            onValueChange = { category = it },
            label = "Kateqoriya",
            required = true,
            placeholder = "məs. Kommunal xərc",
        )
        Spacer(Modifier.height(10.dp))
        MtkNumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Məbləğ (₼)",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Qeyd (istəyə bağlı)",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(4.dp))
    }
}
