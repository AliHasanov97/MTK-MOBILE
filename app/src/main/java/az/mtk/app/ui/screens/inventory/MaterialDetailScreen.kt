package az.mtk.app.ui.screens.inventory

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureResponse
import az.mtk.app.data.api.InvPurchaseHistoryItem
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private data class MaterialDetailState(
    val nomenclature: InvNomenclatureResponse? = null,
    val stockQuantity: Double? = null,
    val history: List<InvPurchaseHistoryItem> = emptyList(),
    val issues: List<InvTransactionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class MaterialDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(MaterialDetailState())
    val state = _state.asStateFlow()

    fun load(nomenclatureId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                coroutineScope {
                    val nomenclature = async { InventoryRepo.repository.nomenclature(nomenclatureId) }
                    val history = async { InventoryRepo.repository.purchaseHistory(nomenclatureId) }
                    val issues = async {
                        InventoryRepo.repository.transactions(
                            transactionType = InvEnumsIssue,
                            nomenclatureId = nomenclatureId,
                        )
                    }
                    val stock = InventoryRepo.repository.stockOf(nomenclatureId)
                    Triple(nomenclature.await(), history.await(), issues.await()) to stock
                }
            }
                .onSuccess { (payload, stock) ->
                    _state.value = MaterialDetailState(
                        nomenclature = payload.first,
                        stockQuantity = stock?.quantityOnHand ?: 0.0,
                        history = payload.second,
                        issues = payload.third,
                        loading = false,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.toUserMessage())
                }
        }
    }

    companion object {
        /** InvEnums.TRANSACTION_ISSUE (local const to keep the import list small). */
        const val InvEnumsIssue = 2
    }
}

@Composable
fun MaterialDetailScreen(
    nomenclatureId: String,
    onBack: (() -> Unit)?,
    viewModel: MaterialDetailViewModel = mtkViewModel { MaterialDetailViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editOpen by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(nomenclatureId) { viewModel.load(nomenclatureId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = "Material",
            subtitle = state.nomenclature?.name,
            onBack = onBack,
        )

        when {
            state.loading -> LoadingState("Məlumat yüklənir…")
            state.error != null -> Column(Modifier.padding(16.dp)) {
                ErrorState(state.error!!, onRetry = { viewModel.load(nomenclatureId) })
            }
            state.nomenclature != null -> {
                val item = state.nomenclature!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        SectionHeading(
                            title = "Ümumi məlumat",
                            action = {
                                Pill(
                                    if (item.isActive == true) "Aktiv" else "Deaktiv",
                                    if (item.isActive == true) Tone.Success else Tone.Neutral,
                                )
                            },
                        )
                    }
                    item {
                        MtkCard {
                            InfoRow("Kod", item.code ?: "—")
                            InfoRow("Nomenklatura", item.name ?: "—")
                            InfoRow("Kateqoriya", InvEnums.categoryLabel(item.category))
                            InfoRow("Ölçü vahidi", InvEnums.unitLabel(item.unit))
                            InfoRow("Minimum ehtiyat", item.minStockLevel?.let { Format.number(it) } ?: "—")
                            InfoRow("Anbarda olan", Format.number(state.stockQuantity))
                            InfoRow("Təsvir", item.description ?: "—")
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                PrimaryButton(
                                    text = "Redaktə et",
                                    onClick = { editOpen = true },
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                az.mtk.app.ui.components.SecondaryButton(
                                    text = "Sil",
                                    onClick = { deleteConfirm = true },
                                )
                            }
                        }
                        actionError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
                        }
                    }
                    item {
                        SectionHeading("Son alış qiyməti")
                        Spacer(Modifier.height(8.dp))
                        MtkCard {
                            val last = state.history.firstOrNull()
                            if (last == null) {
                                Text(
                                    "Qəbul edilmiş alış qeydi yoxdur.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            } else {
                                Text(
                                    Format.money(last.unitPrice),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MtkColors.Green,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${last.vendorName ?: "Tədarükçü məlum deyil"} · ${Format.date(last.purchaseDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                        }
                    }
                    item { SectionHeading("Əvvəlki alış qiymətləri") }
                    if (state.history.isEmpty()) {
                        item {
                            Text(
                                "Bu nomenklatura hələ satın alınmayıb.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                    } else {
                        items(state.history.size) { index ->
                            val entry = state.history[index]
                            MtkCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.vendorName ?: "—",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "${Format.date(entry.purchaseDate)} · ${entry.invoiceNumber ?: "Qaimə yoxdur"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                    Pill(InvEnums.purchaseStatusLabel(entry.status), purchaseTone(entry.status))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Miqdar: ${Format.number(entry.quantity)} · Vahid: ${Format.money(entry.unitPrice)} · Cəmi: ${Format.money(entry.lineTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                        }
                    }
                    item { SectionHeading("Bu nomenklatura üzrə çıxarışlar") }
                    if (state.issues.isEmpty()) {
                        item {
                            Text(
                                "Hələ bu nomenklatura üzrə çıxarış qeydi yoxdur.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                    } else {
                        items(state.issues.size) { index ->
                            val issue = state.issues[index]
                            MtkCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        Format.date(issue.transactionDate),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MtkColors.MutedSoft,
                                    )
                                    Text(
                                        Format.number(issue.quantity),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (!issue.notes.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        issue.notes!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editOpen && state.nomenclature != null) {
        NomenclatureFormModal(
            editingId = nomenclatureId,
            initial = state.nomenclature,
            onDismiss = { editOpen = false },
            onSaved = {
                editOpen = false
                viewModel.load(nomenclatureId)
            },
        )
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = "Materialı sil",
            message = "${state.nomenclature?.name ?: "—"} silinsin? Bu əməliyyat geri qaytarılmır.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteConfirm = false },
            onConfirm = {
                deleteConfirm = false
                scope.launch {
                    runCatching { InventoryRepo.repository.deleteNomenclature(nomenclatureId) }
                        .onSuccess { deleted = true }
                        .onFailure { actionError = it.toUserMessage() }
                }
            },
        )
    }

    LaunchedEffect(deleted) {
        if (deleted) onBack?.invoke()
    }
}

private fun purchaseTone(status: Int?): Tone = when (status) {
    0 -> Tone.Warning
    1 -> Tone.Success
    2 -> Tone.Danger
    else -> Tone.Neutral
}
