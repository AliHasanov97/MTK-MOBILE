package az.mtk.app.ui.screens.operations

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InvPurchaseLineResponse
import az.mtk.app.data.api.InvPurchaseResponse
import az.mtk.app.data.api.InventoryRepository
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.components.DocumentPreviewHost
import az.mtk.app.ui.components.rememberDocumentPreviewController
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch

/**
 * Satınalmalar — MTK-UI-dakı `SalinmalarView`, `SalinmaDetailView` və
 * `CreatePurchaseModal`-un mobil qarşılığı: axtarış + səhifələmə, tam detal,
 * qəbul/ləğv əməliyyatları, PDF sənədi və sətirli yaratma/redaktə forması.
 */
@Composable
internal fun PurchasesSection(
    canManage: Boolean,
    canDownload: Boolean,
    viewModel: PurchasesViewModel = mtkViewModel { PurchasesViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preview = rememberDocumentPreviewController()
    val scope = rememberCoroutineScope()
    var confirmCancel by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Satınalmalar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                        Text(
                            "Tədarükçü qaimələri, qəbul vəziyyəti və məbləğlər bir yerdə.",
                            fontSize = 12.sp,
                            color = OpsColors.Muted,
                        )
                    }
                    if (canManage) {
                        Box(modifier = Modifier.width(130.dp)) {
                            OpsPrimaryButton("+ Yeni", { viewModel.openCreate() })
                        }
                    }
                }
            }
        }

        state.message?.let { item { OpsFeedback(it, isError = false) { viewModel.clearFeedback() } } }
        state.actionError?.let { item { OpsFeedback(it, isError = true) { viewModel.clearFeedback() } } }

        item {
            OpsSearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = "Qaimə nömrəsi, tədarükçü, qeyd…",
            )
        }

        when {
            state.loading && state.purchases.isEmpty() -> item { OpsLoading() }
            state.error != null && state.purchases.isEmpty() ->
                item { OpsError(state.error!!) { viewModel.load(reset = true) } }
            state.purchases.isEmpty() -> item {
                OpsEmpty(
                    title = if (state.query.isBlank()) "Satınalma yoxdur" else "Nəticə tapılmadı",
                    text = if (state.query.isBlank()) {
                        "Yeni qaimə ilə mal qəbulunu qeydə alın."
                    } else {
                        "Axtarış sözünü dəyişin və ya təmizləyin."
                    },
                )
            }
            else -> items(state.purchases, key = { it.id }) { purchase ->
                PurchaseCard(
                    purchase = purchase,
                    canManage = canManage,
                    onOpen = { viewModel.openDetail(purchase) },
                    onEdit = { viewModel.openEdit(purchase) },
                    onCancel = {
                        viewModel.openDetail(purchase)
                        confirmCancel = true
                    },
                )
            }
        }

        if (state.total > state.pageSize) {
            item {
                OpsPagination(
                    page = state.page,
                    total = state.total,
                    pageSize = state.pageSize,
                    onPrev = viewModel::previousPage,
                    onNext = viewModel::nextPage,
                )
            }
        }
    }

    DocumentPreviewHost(preview)

    state.detail?.let { purchase ->
        PurchaseDetailSheet(
            purchase = purchase,
            loading = state.detailLoading,
            detailError = state.detailError,
            message = state.message,
            onDismissMessage = viewModel::clearFeedback,
            actionBusy = state.actionBusy,
            canManage = canManage,
            canDownload = canDownload,
            exporting = exporting,
            onDismiss = {
                viewModel.closeDetail()
                viewModel.clearFeedback()
            },
            onRetry = { viewModel.refreshDetail(purchase.id) },
            onEdit = { viewModel.openEdit(purchase) },
            onReceive = viewModel::receive,
            onCancel = { confirmCancel = true },
            onExportPdf = export@{
                if (exporting) return@export
                exporting = true
                scope.launch {
                    runCatching {
                        FileRepository.downloadToInternal(
                            context = context,
                            path = InventoryRepository.purchasePdfPath(purchase.id),
                            fallbackName = "satinalma-${purchase.invoiceNumber ?: purchase.id.take(8)}.pdf",
                        )
                    }.onSuccess { result ->
                        preview.show(
                            filePath = result.file.absolutePath,
                            fileName = result.fileName,
                            mimeHint = "application/pdf",
                            title = purchase.invoiceNumber?.takeIf { it.isNotBlank() }
                                ?.let { "Satınalma qaiməsi $it" }
                                ?: "Satınalma qaiməsi",
                        )
                    }.onFailure { failure ->
                        Toast.makeText(context, failure.toUserMessage(), Toast.LENGTH_LONG).show()
                    }
                    exporting = false
                }
            },
        )
    }

    state.detail?.let { purchase ->
        if (confirmCancel) {
            OpsDialog(
                title = "Satınalmanı ləğv et",
                onDismiss = { confirmCancel = false },
            ) {
                Text(
                    "«${purchase.invoiceNumber ?: purchase.id.take(8)}» qaiməsi ləğv edilsin? " +
                        "Bu əməliyyat anbara qəbul edilmiş qalıqları geri qaytarmır.",
                    fontSize = 13.sp,
                    color = OpsColors.Muted,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OpsSecondaryButton("Bağla", { confirmCancel = false }, modifier = Modifier.weight(1f))
                    OpsSecondaryButton(
                        text = if (state.actionBusy == "cancel") "Ləğv edilir…" else "Ləğv et",
                        onClick = {
                            confirmCancel = false
                            viewModel.cancel()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = state.actionBusy == null,
                        danger = true,
                    )
                }
            }
        }
    }

    if (state.formOpen) {
        PurchaseFormSheet(
            state = state,
            referenceLoading = state.referenceLoading,
            onDismiss = viewModel::closeForm,
            onVendor = viewModel::setVendorId,
            onDate = viewModel::setDate,
            onNote = viewModel::setNote,
            onAddLine = viewModel::addLine,
            onRemoveLine = viewModel::removeLine,
            onLineQuery = viewModel::updateLineQuery,
            onSelectNomenclature = viewModel::selectNomenclature,
            onQuantity = viewModel::updateLineQuantity,
            onPrice = viewModel::updateLinePrice,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun PurchaseCard(
    purchase: InvPurchaseResponse,
    canManage: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    OpsCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    purchase.invoiceNumber?.takeIf { it.isNotBlank() } ?: "Qaimə yoxdur",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    purchase.vendorName ?: "Tədarükçü göstərilməyib",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        Format.date(purchase.purchaseDate),
                        purchase.receivedOnUtc
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "Qəbul: ${Format.date(it)}" },
                    ).joinToString(" · "),
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.money(purchase.totalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                PurchaseStatusChip(purchase.status)
            }
        }

        if (!purchase.note.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                purchase.note,
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (canManage && purchase.status == InvEnums.PURCHASE_DRAFT) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = OpsColors.Border)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEdit) {
                    Text("Redaktə", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpsColors.Terracotta)
                }
                TextButton(onClick = onCancel) {
                    Text("Ləğv et", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpsColors.Error)
                }
            }
        }
    }
}

@Composable
private fun PurchaseStatusChip(status: Int?) {
    val (foreground, background) = when (status) {
        InvEnums.PURCHASE_RECEIVED -> OpsColors.Olive to OpsColors.OliveSoft
        InvEnums.PURCHASE_CANCELLED -> OpsColors.MutedSoft to OpsColors.Subtle
        else -> OpsColors.Amber to OpsColors.AmberSoft
    }
    OpsChip(InvEnums.purchaseStatusLabel(status), foreground, background, Modifier.padding(top = 4.dp))
}

@Composable
private fun PurchaseDetailSheet(
    purchase: InvPurchaseResponse,
    loading: Boolean,
    detailError: String?,
    message: String?,
    onDismissMessage: () -> Unit,
    actionBusy: String?,
    canManage: Boolean,
    canDownload: Boolean,
    exporting: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onReceive: () -> Unit,
    onCancel: () -> Unit,
    onExportPdf: () -> Unit,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        scrollable = true,
        title = {
            Column {
                Text(
                    purchase.invoiceNumber?.takeIf { it.isNotBlank() } ?: "Satınalma",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                PurchaseStatusChip(purchase.status)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                message?.let { OpsFeedback(it, isError = false, onDismiss = onDismissMessage) }
                detailError?.let { OpsFeedback(it, isError = true, onDismiss = onRetry) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = OpsColors.Container,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Yekun məbləğ",
                            fontSize = 11.sp,
                            color = OpsColors.MutedSoft,
                        )
                        Text(
                            Format.money(purchase.totalAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpsColors.Ink,
                        )
                    }
                }

                OpsCard {
                    OpsInfoRow("Tədarükçü", purchase.vendorName ?: "—")
                    OpsInfoRow("Satınalma tarixi", Format.date(purchase.purchaseDate))
                    OpsInfoRow("Qəbul tarixi", Format.date(purchase.receivedOnUtc))
                    OpsInfoRow("Status", InvEnums.purchaseStatusLabel(purchase.status))
                    purchase.note?.takeIf { it.isNotBlank() }?.let { OpsInfoRow("Qeyd", it) }
                }

                OpsSectionTitle("Sətirlər (${purchase.lines.orEmpty().size})")

                if (loading) {
                    OpsLoading()
                } else if (purchase.lines.isNullOrEmpty()) {
                    Text("Bu satınalma üçün sətir məlumatı yoxdur.", fontSize = 12.sp, color = OpsColors.MutedSoft)
                } else {
                    purchase.lines.orEmpty().forEach { line -> PurchaseLineCard(line) }
                }

                if (canDownload) {
                    OpsPrimaryButton(
                        text = if (exporting) "PDF hazırlanır…" else "Satınalma sənədini aç (PDF)",
                        onClick = onExportPdf,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !exporting,
                    )
                }

                if (canManage && purchase.status == InvEnums.PURCHASE_DRAFT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OpsSecondaryButton(
                            text = "Redaktə et",
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            enabled = actionBusy == null,
                        )
                        OpsSecondaryButton(
                            text = if (actionBusy == "cancel") "Ləğv edilir…" else "Ləğv et",
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            enabled = actionBusy == null,
                            danger = true,
                        )
                    }
                    OpsPrimaryButton(
                        text = if (actionBusy == "receive") "Qəbul edilir…" else "Qəbul et və anbara göndər",
                        onClick = onReceive,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = actionBusy == null && !purchase.lines.isNullOrEmpty(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Bağla", color = OpsColors.Terracotta, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = OpsColors.Surface,
    )
}

@Composable
private fun PurchaseLineCard(line: InvPurchaseLineResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OpsColors.Subtle,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                line.nomenclatureName ?: line.nomenclatureCode ?: "Material",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpsColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            line.nomenclatureCode?.takeIf { it.isNotBlank() }?.let { code ->
                Text(code, fontSize = 10.sp, color = OpsColors.MutedSoft)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${Format.number(line.quantity)} × ${Format.money(line.unitPrice)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                Text(
                    Format.money(line.lineTotal),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
            }
        }
    }
}

/* ------------------------------- Forma ------------------------------- */

@Composable
private fun PurchaseFormSheet(
    state: PurchasesViewModel.UiState,
    referenceLoading: Boolean,
    onDismiss: () -> Unit,
    onVendor: (String) -> Unit,
    onDate: (String) -> Unit,
    onNote: (String) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Long) -> Unit,
    onLineQuery: (Long, String) -> Unit,
    onSelectNomenclature: (Long, InvNomenclatureListItem) -> Unit,
    onQuantity: (Long, String) -> Unit,
    onPrice: (Long, String) -> Unit,
    onSave: () -> Unit,
) {
    val editing = state.editingId != null
    val total = state.lines.sumOf { line ->
        val quantity = line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0
        val price = line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
        quantity * price
    }

    OpsFormSheet(
        title = if (editing) "Satınalmanı redaktə et" else "Yeni satınalma",
        onDismiss = onDismiss,
    ) {
        state.formError?.let {
            OpsFeedback(it, isError = true) { }
            Spacer(Modifier.height(8.dp))
        }

        if (editing) {
            Text("Tədarükçü", fontSize = 12.sp, color = OpsColors.MutedSoft)
            Spacer(Modifier.height(2.dp))
            Text(
                state.editingVendorName ?: "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpsColors.Ink,
            )
        } else if (referenceLoading && state.vendors.isEmpty()) {
            Text("Tədarükçülər yüklənir…", fontSize = 12.sp, color = OpsColors.MutedSoft)
        } else if (state.vendors.isEmpty()) {
            OpsFeedback(
                "Aktiv tədarükçü yoxdur — əvvəlcə Əməliyyatlar → Tədarükçülər bölməsindən əlavə edin.",
                isError = true,
                onDismiss = {},
            )
        } else {
            OpsSelectField(
                label = "Tədarükçü",
                options = state.vendors.map { it.id to (it.name ?: "Tədarükçü") },
                selectedId = state.vendorId.takeIf { it.isNotBlank() },
                onSelect = onVendor,
                placeholder = "Tədarükçü seçin…",
            )
        }

        Spacer(Modifier.height(12.dp))
        OpsDateField("Satınalma tarixi", state.purchaseDate, onDate)
        Spacer(Modifier.height(12.dp))
        OpsTextField(
            value = state.note,
            onValueChange = onNote,
            label = "Qeyd",
            singleLine = false,
            placeholder = "Qaimə ilə bağlı qeyd (istəyə bağlı)",
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Satınalma sətirləri", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
            TextButton(onClick = onAddLine, enabled = !referenceLoading) {
                Text("+ Sətir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpsColors.Terracotta)
            }
        }
        Text(
            "Materialı axtarıb seçin, miqdar və vahid qiyməti daxil edin.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Spacer(Modifier.height(8.dp))

        state.lines.forEachIndexed { index, line ->
            PurchaseLineEditor(
                index = index,
                line = line,
                nomenclatures = state.nomenclatures,
                loading = referenceLoading && state.nomenclatures.isEmpty(),
                onQuery = { onLineQuery(line.key, it) },
                onSelect = { onSelectNomenclature(line.key, it) },
                onQuantity = { onQuantity(line.key, it) },
                onPrice = { onPrice(line.key, it) },
                onRemove = { onRemoveLine(line.key) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OpsColors.Border),
            color = OpsColors.Subtle,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Yekun məbləğ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpsColors.Muted)
                Text(
                    Format.money(total),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OpsFormActions(
            saving = state.formSaving,
            submitLabel = if (editing) "Dəyişiklikləri saxla" else "Satınalmanı yarat",
            onCancel = onDismiss,
            onSubmit = onSave,
            enabled = state.vendors.isNotEmpty() && state.nomenclatures.isNotEmpty(),
        )
    }
}

@Composable
private fun PurchaseLineEditor(
    index: Int,
    line: PurchaseLineDraft,
    nomenclatures: List<InvNomenclatureListItem>,
    loading: Boolean,
    onQuery: (String) -> Unit,
    onSelect: (InvNomenclatureListItem) -> Unit,
    onQuantity: (String) -> Unit,
    onPrice: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val lineTotal = (line.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0) *
        (line.unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0)
    val matches = remember(line.query, nomenclatures, line.nomenclatureId) {
        if (line.nomenclatureId.isNotBlank() || line.query.isBlank()) {
            emptyList()
        } else {
            val term = line.query.trim().lowercase()
            nomenclatures.filter {
                it.name.orEmpty().lowercase().contains(term) || it.code.orEmpty().lowercase().contains(term)
            }.take(5)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpsColors.Border),
        color = OpsColors.Surface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sətir ${index + 1}",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                TextButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, null, tint = OpsColors.Error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sil", fontSize = 11.sp, color = OpsColors.Error)
                }
            }

            if (loading) {
                Text("Nomenklaturalar yüklənir…", fontSize = 11.sp, color = OpsColors.MutedSoft)
            } else {
                OpsTextField(
                    value = line.query,
                    onValueChange = onQuery,
                    label = "Nomenklatura",
                    placeholder = "Ad və ya kod ilə axtar…",
                )
                if (matches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OpsColors.Border),
                        color = OpsColors.Subtle,
                    ) {
                        Column {
                            matches.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(item) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        item.name ?: "Material",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OpsColors.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    item.code?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, fontSize = 10.sp, color = OpsColors.MutedSoft)
                                    }
                                }
                            }
                        }
                    }
                }
                if (line.nomenclatureId.isBlank() && line.query.isNotBlank() && matches.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Nomenklatura tapılmadı.", fontSize = 11.sp, color = OpsColors.Error)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpsTextField(
                    value = line.quantity,
                    onValueChange = onQuantity,
                    label = "Miqdar",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "0",
                )
                OpsTextField(
                    value = line.unitPrice,
                    onValueChange = onPrice,
                    label = "Vahid qiymət",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "0,00",
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Məbləğ: ${Format.money(lineTotal)}",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
                textAlign = TextAlign.End,
            )
        }
    }
}

