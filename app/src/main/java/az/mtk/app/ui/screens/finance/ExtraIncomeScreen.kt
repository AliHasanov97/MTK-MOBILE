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
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch

// MTK-UI ElaveGelirlerView INCOME_CATEGORIES ilə eyni.
private val incomeCategories = listOf(
    "Ümumi sahələrin icarəsi",
    "Avtodayanacaq (qonaq) haqqı",
    "Reklam / antena yerləşdirilməsi",
    "Cərimə",
    "Bank faizi",
    "Sponsorluq / ianə",
    "Satılan materialın dəyəri",
    "Digər",
)

private const val OTHER_CATEGORY = "Digər"

class ExtraIncomeViewModel : ViewModel() {
    val pager = Pager<FinTransactionResponse>(viewModelScope) { page, query ->
        FinanceRepository.transactions(page, search = query, direction = FinTransactionDirection.INCOME)
    }

    fun createIncome(category: String, amount: Double, description: String?, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                FinanceRepository.createTransaction(
                    FinCreateTransactionRequest(
                        direction = FinTransactionDirection.INCOME,
                        category = category,
                        amount = amount,
                        description = description,
                    ),
                )
            }.onSuccess {
                pager.refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun ExtraIncomeScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: ExtraIncomeViewModel = mtkViewModel { ExtraIncomeViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Əlavə gəlirlər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} qeyd",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Kateqoriya və ya qeyd üzrə…",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni gəlir", onClick = { showCreate = true })
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Gəlirlər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Əlavə gəlir tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { tx ->
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
                            Pill(finTransactionDirectionLabel(tx.direction), Tone.Success)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                Format.dateTime(tx.transactionDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MtkColors.MutedSoft,
                            )
                            Text(
                                Format.money(tx.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MtkColors.Positive,
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
        ExtraIncomeCreateModal(
            onDismiss = { showCreate = false },
            onSubmit = { category, amount, description, onDone ->
                viewModel.createIncome(category, amount, description, onDone)
            },
        )
    }
}

@Composable
private fun ExtraIncomeCreateModal(
    onDismiss: () -> Unit,
    onSubmit: (category: String, amount: Double, description: String?, onDone: (String?) -> Unit) -> Unit,
) {
    val categories = remember { incomeCategories }
    var category by remember { mutableStateOf(categories.first()) }
    var customCategory by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val finalCategory = if (category == OTHER_CATEGORY) customCategory.trim() else category
    val numericAmount = amount.replace(',', '.').toDoubleOrNull()
    val canSubmit = finalCategory.isNotBlank() && numericAmount != null && numericAmount > 0

    MtkModal(
        title = "Yeni gəlir",
        subtitle = "Sakin haqq ödənişləri avtomatik qeydə alınır — bununla əlaqəsi olmayan gəlirlər burada daxil edilir.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Gəliri qeyd et",
        confirmEnabled = canSubmit && !saving,
        onConfirm = {
            if (!canSubmit || saving) return@MtkModal
            saving = true
            error = null
            onSubmit(finalCategory, numericAmount!!, description.trim().ifBlank { null }) { err ->
                saving = false
                if (err == null) onDismiss() else error = err
            }
        },
    ) {
        MtkSelect(
            value = category,
            onValueChange = { category = it },
            label = "Kateqoriya",
            required = true,
            options = categories.map { MtkOption(it, it) },
        )
        if (category == OTHER_CATEGORY) {
            Spacer(Modifier.height(10.dp))
            MtkTextField(
                value = customCategory,
                onValueChange = { customCategory = it },
                label = "Kateqoriya adı",
                required = true,
                placeholder = "məs. Dam üzərində avadanlıq icarəsi",
            )
        }
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
