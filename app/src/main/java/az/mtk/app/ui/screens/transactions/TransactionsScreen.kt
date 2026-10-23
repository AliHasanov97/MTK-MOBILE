package az.mtk.app.ui.screens.transactions

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InventoryRepository
import az.mtk.app.data.api.finPaymentMethodLabel
import az.mtk.app.data.api.finPaymentStatusLabel
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsDateField
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFormSheet
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPill
import az.mtk.app.ui.screens.operations.OpsPillRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsSectionTitle
import az.mtk.app.ui.screens.operations.OpsTopBar
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

/**
 * Tranzaksiyalar (avtomatik kassa jurnalı). Saytdakı `TranzaksiyalarView`-in
 * mobil qarşılığı: cədvəl yoxdur — statistika kartları, filtr çipləri, ay üzrə
 * qruplaşdırılmış sətirlər və toxunanda açılan sənəd paneli.
 */
@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    canDownload: Boolean = true,
    viewModel: TransactionsViewModel = mtkViewModel { TransactionsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCategories by remember { mutableStateOf(false) }
    var showDates by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = "Tranzaksiyalar",
                subtitle = "Avtomatik kassa jurnalı",
                onBack = onBack,
                onRefresh = viewModel::load,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { StatsBlock(state) }

            item {
                OpsSearchField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    placeholder = "Kateqoriya və ya qeyd üzrə…",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                OpsPillRow(
                    pills = TxKind.entries.map { kind ->
                        OpsPill(
                            label = kind.label,
                            selected = state.kind == kind,
                            onClick = { viewModel.setKind(kind) },
                        )
                    },
                )
            }

            item {
                OpsPillRow(
                    pills = TxRangePreset.entries.map { preset ->
                        OpsPill(
                            label = preset.label,
                            selected = state.preset == preset,
                            onClick = { viewModel.applyPreset(preset) },
                        )
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpsSecondaryButton(
                        text = if (state.selectedCategories.isEmpty()) {
                            "Kateqoriya"
                        } else {
                            "Kateqoriya (${state.selectedCategories.size})"
                        },
                        onClick = { showCategories = true },
                        modifier = Modifier.weight(1f),
                    )
                    OpsSecondaryButton(
                        text = "Tarix aralığı",
                        onClick = { showDates = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Əməliyyatlar",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpsColors.Ink,
                    )
                    Text(
                        text = "${state.rows.size} qeyd",
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                }
            }

            when {
                state.loading && state.rows.isEmpty() -> item { OpsLoading() }

                state.error != null && state.rows.isEmpty() -> item {
                    OpsError(state.error!!, onRetry = viewModel::load)
                }

                state.rows.isEmpty() -> item {
                    OpsEmpty(
                        title = "Bu aralıqda tranzaksiya yoxdur",
                        text = "Tarix aralığını genişləndirin və ya axtarışı təmizləyin.",
                    )
                }

                else -> {
                    state.groups.forEach { group ->
                        item(key = "month-${group.key}") { MonthHeader(group) }
                        items(group.rows, key = { it.id }) { row ->
                            TxRowCard(row = row, onClick = { viewModel.openDocument(row) })
                        }
                    }
                }
            }

            item { FooterNote() }
        }
    }

    if (showCategories) {
        CategorySheet(
            state = state,
            onToggle = viewModel::toggleCategory,
            onClear = viewModel::clearCategories,
            onDismiss = { showCategories = false },
        )
    }

    if (showDates) {
        DateSheet(
            state = state,
            onApply = { start, end ->
                viewModel.setRange(start, end)
                showDates = false
            },
            onDismiss = { showDates = false },
        )
    }

    val detailRow = state.detailRow
    if (detailRow != null) {
        TxDocumentSheet(
            row = detailRow,
            detail = state.detail,
            loading = state.detailLoading,
            error = state.detailError,
            canDownload = canDownload,
            onRetry = { viewModel.openDocument(detailRow) },
            onDismiss = viewModel::closeDocument,
        )
    }
}

// ---- Statistika -------------------------------------------------------------

@Composable
private fun StatsBlock(state: TransactionsViewModel.UiState) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Daxilolma",
                value = Format.money(state.totalIncome),
                color = OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Xərc",
                value = Format.money(state.totalExpense),
                color = OpsColors.Error,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Xalis",
                value = signedMoney(state.net),
                color = if (state.net < 0) OpsColors.Error else OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = periodCaption(state),
            modifier = Modifier.padding(horizontal = 2.dp),
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = OpsColors.Surface,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = OpsColors.MutedSoft,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun periodCaption(state: TransactionsViewModel.UiState): String {
    val start = state.startDate
    val end = state.endDate
    val range = if (start == null && end == null) {
        "Bütün tarix"
    } else {
        val from = start?.let(::formatShortDate) ?: "əvvəldən"
        val to = end?.let(::formatShortDate) ?: "bu günə"
        "$from – $to"
    }
    return "$range · ${state.rows.size} əməliyyat"
}

private fun formatShortDate(date: LocalDate): String =
    "%02d.%02d.%d".format(date.dayOfMonth, date.monthValue, date.year)

// ---- Ay başlığı və sətirlər --------------------------------------------------

@Composable
private fun MonthHeader(group: TxMonthGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            Text(
                text = signedMoney(group.net),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (group.net < 0) OpsColors.Error else OpsColors.Olive,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${group.rows.size} əməliyyat · Daxilolma ${Format.money(group.income)} · Xərc ${Format.money(group.expense)}",
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
            maxLines = 2,
        )
    }
}

@Composable
private fun TxRowCard(row: TxRow, onClick: () -> Unit) {
    OpsCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (row.income) {
                OpsChip("Gəlir", OpsColors.Olive, OpsColors.OliveSoft)
            } else {
                OpsChip("Xərc", OpsColors.Error, OpsColors.ErrorSoft)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = Format.date(row.date),
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.note,
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = signedMoney(row.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (row.income) OpsColors.Olive else OpsColors.Error,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = documentLabel(row.documentType),
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                color = OpsColors.MutedSoft,
            )
            Text(
                text = "Detala bax ›",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpsColors.Terracotta,
            )
        }
    }
}

private fun documentLabel(type: String): String = when (type) {
    "Purchase" -> "Satınalma qaiməsi"
    "ResidentPayment" -> "Sakin ödənişi"
    "VendorPayment" -> "Tədarükçü ödənişi"
    else -> "Jurnal qeydi"
}

// ---- Filtr panelləri ---------------------------------------------------------

@Composable
private fun CategorySheet(
    state: TransactionsViewModel.UiState,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    OpsFormSheet(title = "Kateqoriya", onDismiss = onDismiss) {
        if (state.categoryOptions.isEmpty()) {
            Text("Kateqoriya tapılmadı.", fontSize = 12.sp, color = OpsColors.MutedSoft)
        } else {
            state.categoryOptions.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle(category) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = category in state.selectedCategories,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = OpsColors.Terracotta,
                            uncheckedColor = OpsColors.Border,
                        ),
                    )
                    Text(
                        text = category,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = OpsColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Təmizlə",
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = state.selectedCategories.isNotEmpty(),
            )
            OpsPrimaryButton(
                text = "Hazırdır",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DateSheet(
    state: TransactionsViewModel.UiState,
    onApply: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var start by remember(state.startDate) { mutableStateOf(state.startDate?.toString().orEmpty()) }
    var end by remember(state.endDate) { mutableStateOf(state.endDate?.toString().orEmpty()) }

    val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
    val endDate = runCatching { LocalDate.parse(end) }.getOrNull()
    val invalid = (start.isNotBlank() && startDate == null) || (end.isNotBlank() && endDate == null)

    OpsFormSheet(title = "Tarix aralığı", onDismiss = onDismiss) {
        Text(
            text = "Boş buraxılan sərhəd tətbiq olunmur. Tarixlər daxil olmaqla hesablanır.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Spacer(Modifier.height(10.dp))
        OpsDateField(label = "Başlanğıc", value = start, onValueChange = { start = it })
        Spacer(Modifier.height(10.dp))
        OpsDateField(label = "Son", value = end, onValueChange = { end = it })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Təmizlə",
                onClick = {
                    start = ""
                    end = ""
                },
                modifier = Modifier.weight(1f),
            )
            OpsPrimaryButton(
                text = "Tətbiq et",
                onClick = { onApply(startDate, endDate) },
                modifier = Modifier.weight(1f),
                enabled = !invalid,
            )
        }
    }
}

// ---- Sənəd paneli ------------------------------------------------------------

@Composable
private fun TxDocumentSheet(
    row: TxRow,
    detail: TxDocument?,
    loading: Boolean,
    error: String?,
    canDownload: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (row.documentType) {
        "Purchase" -> "Satınalma qaiməsi"
        "VendorPayment" -> "Tədarükçü ödənişi"
        "ResidentPayment" -> "Sakin ödənişi"
        else -> "Tranzaksiya sənədi"
    }

    OpsFormSheet(title = title, onDismiss = onDismiss) {
        when {
            loading -> OpsLoading()
            error != null -> OpsError(error, onRetry = onRetry)
            detail is TxDocument.Payment -> PaymentDocument(detail, canDownload)
            detail is TxDocument.Purchase -> PurchaseDocument(detail, canDownload)
            detail is TxDocument.Plain -> PlainDocument(detail.row)
            else -> Unit
        }
    }
}

@Composable
private fun PaymentDocument(document: TxDocument.Payment, canDownload: Boolean) {
    val payment = document.payment
    val allocations = document.allocations
    val allocated = allocations.sumOf { it.allocatedAmount ?: 0.0 }
    val unallocated = (payment.amount ?: 0.0) - allocated

    OpsCard {
        OpsInfoRow("Ödəyən", payment.partyName ?: payment.ownerName ?: "—")
        OpsInfoRow("Əlaqəli əmlak", payment.propertyLabel ?: "Ümumi ödəniş")
        OpsInfoRow("Tarix", Format.dateTime(payment.paymentDate))
        OpsInfoRow("Ödəniş üsulu", finPaymentMethodLabel(payment.paymentMethod))
        OpsInfoRow(
            label = "Status",
            value = finPaymentStatusLabel(payment.status),
            valueColor = if (payment.status == 1) OpsColors.Olive else OpsColors.Amber,
        )
        OpsInfoRow("Məbləğ", Format.money(payment.amount), OpsColors.Terracotta)
        payment.notes?.takeIf { it.isNotBlank() }?.let { OpsInfoRow("Qeyd", it) }
    }

    if (allocations.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        OpsSectionTitle("Bölgü (${allocations.size})")
        Spacer(Modifier.height(6.dp))
        allocations.forEach { allocation ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.Subtle,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = allocation.propertyLabel ?: "—",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpsColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = Format.money(allocation.allocatedAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpsColors.Olive,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${Format.period(allocation.period)} · ${allocation.description ?: "Aylıq haqq"}",
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                    (allocation.remainingDebtAfterPayment ?: 0.0)
                        .takeIf { it > 0.005 }
                        ?.let { remaining ->
                            Text(
                                text = "Qalıq borc: ${Format.money(remaining)}",
                                fontSize = 10.sp,
                                color = OpsColors.MutedSoft,
                            )
                        }
                }
            }
        }

        OpsInfoRow("Bölgüyə ayrılıb", Format.money(allocated))
        if (abs(unallocated) > 0.005) {
            OpsInfoRow(
                label = "Bölgüsüz qalıq",
                value = Format.money(unallocated),
                valueColor = if (unallocated < 0) OpsColors.Error else OpsColors.Amber,
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    if (canDownload) {
        PdfDownloadButton(
            label = "Qəbzi yüklə (PDF)",
            path = "api/payments/payments/${payment.id}/receipt",
            fallbackName = "qebz-${payment.id.take(8)}.pdf",
        )
    }
}

@Composable
private fun PurchaseDocument(document: TxDocument.Purchase, canDownload: Boolean) {
    val purchase = document.purchase

    OpsCard {
        OpsInfoRow("Tədarükçü", purchase.vendorName ?: "—")
        OpsInfoRow("Qaimə", purchase.invoiceNumber ?: "—")
        OpsInfoRow("Tarix", Format.dateTime(purchase.purchaseDate))
        OpsInfoRow("Status", InvEnums.purchaseStatusLabel(purchase.status))
        OpsInfoRow("Yekun", Format.money(purchase.totalAmount), OpsColors.Terracotta)
        purchase.note?.takeIf { it.isNotBlank() }?.let { OpsInfoRow("Qeyd", it) }
    }

    val lines = purchase.lines.orEmpty()
    Spacer(Modifier.height(14.dp))
    OpsSectionTitle("Sətirlər (${lines.size})")
    Spacer(Modifier.height(6.dp))

    if (lines.isEmpty()) {
        Text("Sətir tapılmadı.", fontSize = 12.sp, color = OpsColors.MutedSoft)
    } else {
        lines.forEach { line ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.Subtle,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = line.nomenclatureName ?: line.nomenclatureCode ?: "Nomenklatura",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpsColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${Format.number(line.quantity)} × ${Format.money(line.unitPrice)}",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = OpsColors.MutedSoft,
                        )
                        Text(
                            text = Format.money(line.lineTotal),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpsColors.Ink,
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    if (canDownload) {
        PdfDownloadButton(
            label = "Qaiməni yüklə (PDF)",
            path = InventoryRepository.purchasePdfPath(purchase.id),
            fallbackName = "qaime-${purchase.id.take(8)}.pdf",
        )
    }
}

@Composable
private fun PlainDocument(row: TxRow) {
    OpsCard {
        OpsInfoRow("Tarix", Format.dateTime(row.date))
        OpsInfoRow(
            label = "Növ",
            value = if (row.income) "Gəlir" else "Xərc",
            valueColor = if (row.income) OpsColors.Olive else OpsColors.Error,
        )
        OpsInfoRow("Kateqoriya", row.category)
        OpsInfoRow(
            label = "Məbləğ",
            value = signedMoney(row.amount),
            valueColor = if (row.income) OpsColors.Olive else OpsColors.Error,
        )
        OpsInfoRow("Açıqlama", row.note)
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = "Bu, ödəniş və ya satınalma sənədinə bağlı olmayan jurnal qeydidir.",
        fontSize = 11.sp,
        color = OpsColors.MutedSoft,
    )
}

// ---- Köməkçilər --------------------------------------------------------------

@Composable
private fun PdfDownloadButton(label: String, path: String, fallbackName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }

    OpsPrimaryButton(
        text = if (downloading) "Yüklənir…" else label,
        onClick = {
            scope.launch {
                downloading = true
                try {
                    val result = FileRepository.downloadToInternal(
                        context = context,
                        path = path,
                        fallbackName = fallbackName,
                    )
                    if (!FileRepository.openFile(context, result.file)) {
                        Toast.makeText(
                            context,
                            "Fayl yükləndi: ${result.fileName}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } catch (t: Throwable) {
                    Toast.makeText(context, t.toUserMessage(), Toast.LENGTH_LONG).show()
                } finally {
                    downloading = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !downloading,
    )
}

@Composable
private fun FooterNote() {
    Text(
        text = "Sakin və tədarükçü ödənişlərindən yaranan qeydlər avtomatikdir. " +
            "Birbaşa xərc və gəlirlər Maliyyə əməliyyatları bölməsindən əlavə olunur.",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontSize = 11.sp,
        color = OpsColors.MutedSoft,
    )
}

private fun signedMoney(value: Double): String {
    if (abs(value) < 0.005) return "0,00 ₼"
    val sign = if (value > 0) "+" else "−"
    return "$sign${Format.money(abs(value))}"
}
