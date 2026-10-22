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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import az.mtk.app.data.api.InvCreatePurchaseRequest
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvPurchaseLineInput
import az.mtk.app.data.api.InvPurchaseResponse
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReceiptsViewModel : ViewModel() {
    val pager = Pager<InvPurchaseResponse>(viewModelScope) { page, query ->
        InventoryRepo.repository.purchasePage(page, 20, query)
    }
}

@Composable
fun ReceiptsScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: ReceiptsViewModel = mtkViewModel { ReceiptsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var createOpen by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<InvPurchaseResponse?>(null) }
    var cancelTarget by remember { mutableStateOf<InvPurchaseResponse?>(null) }
    var workingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Daxilolmalar",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} satınalma",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            az.mtk.app.ui.components.SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (tədarükçü, qaimə…)",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni satınalma", onClick = { createOpen = true })
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Satınalmalar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Satınalma tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { purchase ->
                    MtkCard(onClick = { detailTarget = purchase }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    purchase.vendorName ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${Format.date(purchase.purchaseDate)} · ${purchase.invoiceNumber ?: "Qaimə yoxdur"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Pill(InvEnums.purchaseStatusLabel(purchase.status), purchaseTone(purchase.status))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Cəmi: ${Format.money(purchase.totalAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (purchase.status == InvEnums.PURCHASE_DRAFT) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    androidx.compose.material3.TextButton(
                                        enabled = workingId != purchase.id,
                                        onClick = {
                                            workingId = purchase.id
                                            scope.launch {
                                                runCatching { InventoryRepo.repository.receivePurchase(purchase.id) }
                                                    .onSuccess { viewModel.pager.refresh() }
                                                    .onFailure { error = it.toUserMessage() }
                                                workingId = null
                                            }
                                        },
                                    ) { Text("Qəbul et", fontSize = 13.sp, color = MtkColors.Green) }
                                    androidx.compose.material3.TextButton(
                                        enabled = workingId != purchase.id,
                                        onClick = { cancelTarget = purchase },
                                    ) { Text("Ləğv et", fontSize = 13.sp, color = MtkColors.Negative) }
                                }
                            }
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

    if (createOpen) {
        PurchaseFormModal(
            onDismiss = { createOpen = false },
            onSaved = {
                createOpen = false
                viewModel.pager.refresh()
            },
        )
    }

    detailTarget?.let { purchase ->
        PurchaseDetailModal(
            purchase = purchase,
            working = workingId == purchase.id,
            onDismiss = { detailTarget = null },
            onAccept = {
                workingId = purchase.id
                scope.launch {
                    runCatching { InventoryRepo.repository.receivePurchase(purchase.id) }
                        .onSuccess {
                            detailTarget = null
                            viewModel.pager.refresh()
                        }
                        .onFailure { error = it.toUserMessage() }
                    workingId = null
                }
            },
            onCancel = { detailTarget = null; cancelTarget = purchase },
        )
    }

    cancelTarget?.let { purchase ->
        ConfirmDialog(
            title = "Satınalmanı ləğv et",
            message = "${purchase.vendorName ?: "—"} üzrə satınalma ləğv edilsin?",
            confirmLabel = "Ləğv et",
            danger = true,
            onDismiss = { cancelTarget = null },
            onConfirm = {
                cancelTarget = null
                workingId = purchase.id
                scope.launch {
                    runCatching { InventoryRepo.repository.cancelPurchase(purchase.id, null) }
                        .onSuccess { viewModel.pager.refresh() }
                        .onFailure { error = it.toUserMessage() }
                    workingId = null
                }
            },
        )
    }
}

@Composable
private fun PurchaseDetailModal(
    purchase: InvPurchaseResponse,
    working: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    MtkModal(
        title = purchase.vendorName ?: "Satınalma",
        subtitle = "${Format.date(purchase.purchaseDate)} · ${InvEnums.purchaseStatusLabel(purchase.status)}",
        onDismiss = onDismiss,
    ) {
        MtkCard {
            InfoRow("Tədarükçü", purchase.vendorName ?: "—")
            InfoRow("Tarix", Format.date(purchase.purchaseDate))
            InfoRow("Qaimə nömrəsi", purchase.invoiceNumber ?: "—")
            InfoRow("Qəbul tarixi", purchase.receivedOnUtc?.let { Format.dateTime(it) } ?: "—")
            InfoRow("Qeyd", purchase.note ?: "—")
            InfoRow("Ümumi məbləğ", Format.money(purchase.totalAmount))
        }
        Spacer(Modifier.height(12.dp))

        ExportActionRow(
            title = "PDF qaimə",
            subtitle = "Satınalma sənədini endir və aç",
            path = InventoryRepo.repository.purchasePdfPath(purchase.id),
            scope = androidx.compose.runtime.rememberCoroutineScope(),
        )

        if (purchase.status == InvEnums.PURCHASE_DRAFT) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    PrimaryButton(text = "Qəbul et", onClick = onAccept, enabled = !working, loading = working)
                }
                Column(Modifier.weight(1f)) {
                    SecondaryButton(text = "Ləğv et", onClick = onCancel, enabled = !working)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private data class LineDraft(
    val nomenclatureId: String? = null,
    val nomenclatureLabel: String? = null,
    val quantity: String = "",
    val unitPrice: String = "",
)

@Composable
private fun PurchaseFormModal(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var vendorId by remember { mutableStateOf<String?>(null) }
    var vendorLabel by remember { mutableStateOf<String?>(null) }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf(LineDraft())) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val total = lines.sumOf { line ->
        (line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0) *
            (line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0)
    }

    MtkModal(
        title = "Yeni satınalma",
        subtitle = "Tədarükçü, tarix və məhsul sətirlərini daxil edin",
        onDismiss = onDismiss,
        confirmLabel = "Satınalmanı yarat",
        confirmEnabled = vendorId != null && lines.all { it.nomenclatureId != null },
        confirmLoading = saving,
        onConfirm = {
            val requestLines = lines.map { line ->
                InvPurchaseLineInput(
                    nomenclatureId = line.nomenclatureId!!,
                    quantity = line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    unitPrice = line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0,
                )
            }
            when {
                vendorId == null -> error = "Tədarükçü seçin."
                requestLines.isEmpty() || requestLines.any {
                    it.quantity <= 0.0 || it.unitPrice < 0.0
                } -> error = "Hər sətirdə nomenklatura, müsbət miqdar və düzgün qiymət seçin."
                else -> {
                    saving = true
                    error = null
                    scope.launch {
                        runCatching {
                            InventoryRepo.repository.createPurchase(
                                InvCreatePurchaseRequest(
                                    vendorId = vendorId!!,
                                    purchaseDate = az.mtk.app.data.api.dateOnlyToUtcIso(purchaseDate.toString()),
                                    note = note.trim().takeIf { it.isNotEmpty() },
                                    lines = requestLines,
                                ),
                            )
                        }
                            .onSuccess { onSaved() }
                            .onFailure { error = it.toUserMessage() }
                        saving = false
                    }
                }
            }
        },
    ) {
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MtkColors.Negative,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        SearchableSelect(
            label = "Tədarükçü",
            required = true,
            selectedLabel = vendorLabel,
            searchPlaceholder = "Tədarükçü axtar…",
            onLoad = { query -> InventoryRepo.repository.vendors(query).filter { it.isActive != false } },
            itemLabel = { it.name ?: it.id },
            onSelect = { vendor ->
                vendorId = vendor.id
                vendorLabel = vendor.name
            },
            onClear = {
                vendorId = null
                vendorLabel = null
            },
        )
        Spacer(Modifier.height(10.dp))
        MtkDateField(
            value = purchaseDate,
            onValueChange = { purchaseDate = it },
            label = "Satınalma tarixi",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = note,
            onValueChange = { note = it },
            label = "Qeyd",
            singleLine = false,
            minLines = 2,
        )

        Spacer(Modifier.height(16.dp))
        Text("Sətirlər", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        lines.forEachIndexed { index, line ->
            MtkCard(padding = 12.dp, modifier = Modifier.padding(bottom = 10.dp)) {
                SearchableSelect(
                    label = "Nomenklatura",
                    required = true,
                    selectedLabel = line.nomenclatureLabel,
                    searchPlaceholder = "Ad və ya kod ilə axtar…",
                    onLoad = { query ->
                        InventoryRepo.repository.nomenclaturePage(0, 50, query).items
                            .filter { it.isActive != false }
                    },
                    itemLabel = { "${it.code ?: ""} — ${it.name ?: ""}" },
                    onSelect = { item ->
                        lines = lines.toMutableList().apply {
                            this[index] = line.copy(
                                nomenclatureId = item.id,
                                nomenclatureLabel = "${item.code ?: ""} — ${item.name ?: ""}",
                            )
                        }
                    },
                    onClear = {
                        lines = lines.toMutableList().apply {
                            this[index] = line.copy(nomenclatureId = null, nomenclatureLabel = null)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MtkNumberField(
                        value = line.quantity,
                        onValueChange = { text ->
                            lines = lines.toMutableList().apply {
                                this[index] = line.copy(quantity = text)
                            }
                        },
                        label = "Miqdar",
                        required = true,
                        modifier = Modifier.weight(1f),
                    )
                    MtkNumberField(
                        value = line.unitPrice,
                        onValueChange = { text ->
                            lines = lines.toMutableList().apply {
                                this[index] = line.copy(unitPrice = text)
                            }
                        },
                        label = "Vahid qiymət",
                        required = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Məbləğ: ${Format.money(
                            (line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0) *
                                (line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0),
                        )}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MtkColors.Muted,
                    )
                    if (lines.size > 1) {
                        androidx.compose.material3.TextButton(
                            onClick = { lines = lines.filterIndexed { i, _ -> i != index } },
                        ) { Text("Sətri sil", fontSize = 13.sp, color = MtkColors.Negative) }
                    }
                }
            }
        }

        SecondaryButton(
            text = "+ Sətir əlavə et",
            onClick = { lines = lines + LineDraft() },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Yekun məbləğ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                Format.money(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MtkColors.Green,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}
