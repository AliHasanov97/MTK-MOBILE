package az.mtk.app.ui.screens.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureResponse
import az.mtk.app.data.api.InvPurchaseHistoryItem
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format

/**
 * Material detalı — ümumi məlumat, qalıq, son alış qiyməti, alış tarixçəsi
 * və çıxarışlar. Saytdakı `NomenclatureDetailView` ilə eyni məlumat dəsti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemScreen(
    nomenclatureId: String,
    onBack: () -> Unit,
    viewModel: InventoryItemViewModel = mtkViewModel { InventoryItemViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(nomenclatureId) { viewModel.load(nomenclatureId) }

    Scaffold(
        containerColor = InvColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.nomenclature?.name?.takeIf { it.isNotBlank() } ?: "Material",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = InvColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.nomenclature?.code?.takeIf { it.isNotBlank() } ?: "İnventar",
                            fontSize = 12.sp,
                            color = InvColors.Muted,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Geri",
                            tint = InvColors.Ink,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(nomenclatureId) }) {
                        Icon(Icons.Default.Refresh, "Yenilə", tint = InvColors.Terracotta)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InvColors.Background),
            )
        },
    ) { padding ->
        when {
            state.loading -> DetailLoading(Modifier.padding(padding))

            state.error != null -> DetailError(
                message = state.error ?: "",
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load(nomenclatureId) },
            )

            state.nomenclature != null && state.nomenclature?.id == nomenclatureId -> {
                DetailContent(
                    nomenclature = state.nomenclature!!,
                    stockQuantity = state.stockQuantity,
                    history = state.history,
                    issues = state.issues,
                    modifier = Modifier.padding(padding),
                )
            }

            else -> DetailLoading(Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(
    nomenclature: InvNomenclatureResponse,
    stockQuantity: Double,
    history: List<InvPurchaseHistoryItem>,
    issues: List<InvTransactionDto>,
    modifier: Modifier = Modifier,
) {
    val minLevel = nomenclature.minStockLevel
    val lowStock = minLevel != null && stockQuantity < minLevel

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeroCard(nomenclature) }

        if (lowStock) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = InvColors.Error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, InvColors.Error.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = InvColors.Error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Qalıq minimum ehtiyatdan aşağıdır: " +
                                "${Format.number(stockQuantity)} / ${Format.number(minLevel)}",
                            fontSize = 12.sp,
                            color = InvColors.Error,
                        )
                    }
                }
            }
        }

        item { InfoCard(nomenclature, stockQuantity) }
        item { LastPurchaseCard(history.firstOrNull()) }

        item { SectionTitle("Əvvəlki alış qiymətləri", history.size) }
        if (history.isEmpty()) {
            item { EmptyCard("Bu nomenklatura hələ satın alınmayıb.") }
        } else {
            itemsIndexed(
                items = history,
                key = { index, entry -> "${entry.purchaseId}-$index" },
            ) { _, entry ->
                PurchaseRow(entry)
            }
        }

        item { SectionTitle("Bu nomenklatura üzrə çıxarışlar", issues.size) }
        if (issues.isEmpty()) {
            item { EmptyCard("Hələ bu nomenklatura üzrə çıxarış qeydi yoxdur.") }
        } else {
            items(items = issues, key = { it.id }) { issue ->
                IssueRow(issue)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun HeroCard(nomenclature: InvNomenclatureResponse) {
    val active = nomenclature.isActive != false

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(InvColors.Container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    null,
                    tint = InvColors.Terracotta,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = nomenclature.name ?: "Adsız material",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                )
                Text(
                    text = "Kod: ${nomenclature.code ?: "—"}",
                    fontSize = 12.sp,
                    color = InvColors.MutedSoft,
                )
            }
            DetailTag(
                text = if (active) "Aktiv" else "Deaktiv",
                color = if (active) InvColors.Olive else InvColors.MutedSoft,
            )
        }
    }
}

@Composable
private fun InfoCard(nomenclature: InvNomenclatureResponse, stockQuantity: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ümumi məlumat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCell("Kateqoriya", InvEnums.categoryLabel(nomenclature.category), Modifier.weight(1f))
                InfoCell("Ölçü vahidi", InvEnums.unitLabel(nomenclature.unit), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCell(
                    "Minimum ehtiyat",
                    nomenclature.minStockLevel?.let { Format.number(it) } ?: "—",
                    Modifier.weight(1f),
                )
                InfoCell(
                    "Status",
                    if (nomenclature.isActive != false) "Aktiv" else "Deaktiv",
                    Modifier.weight(1f),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = InvColors.Subtle,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Anbarda olan", fontSize = 12.sp, color = InvColors.Muted)
                    Text(
                        text = Format.number(stockQuantity),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InvColors.Ink,
                    )
                }
            }

            if (!nomenclature.description.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Təsvir", fontSize = 10.sp, color = InvColors.MutedSoft)
                    Text(
                        text = nomenclature.description,
                        fontSize = 13.sp,
                        color = InvColors.Ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastPurchaseCard(entry: InvPurchaseHistoryItem?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Son alış qiyməti", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)

            if (entry == null) {
                Text(
                    text = "Qəbul edilmiş alış qeydi yoxdur.",
                    fontSize = 12.sp,
                    color = InvColors.Muted,
                )
            } else {
                Text(
                    text = Format.money(entry.unitPrice),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InvColors.Terracotta,
                )
                Text(
                    text = listOfNotNull(
                        entry.vendorName,
                        Format.date(entry.purchaseDate),
                    ).joinToString(" · ").ifBlank { "Tədarükçü məlum deyil" },
                    fontSize = 12.sp,
                    color = InvColors.Muted,
                )
                if (!entry.invoiceNumber.isNullOrBlank()) {
                    DetailTag("Qaimə ${entry.invoiceNumber}", InvColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(entry: InvPurchaseHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Format.date(entry.purchaseDate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                )
                DetailTag(
                    text = InvEnums.purchaseStatusLabel(entry.status),
                    color = if (entry.status == InvEnums.PURCHASE_RECEIVED) InvColors.Olive else InvColors.MutedSoft,
                )
            }
            Text(
                text = listOfNotNull(
                    entry.vendorName,
                    entry.invoiceNumber?.let { "Qaimə $it" },
                ).joinToString(" · ").ifBlank { "Tədarükçü məlum deyil" },
                fontSize = 11.sp,
                color = InvColors.MutedSoft,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Miqdar ${Format.number(entry.quantity)}", fontSize = 11.sp, color = InvColors.Muted)
                Text("Vahid ${Format.money(entry.unitPrice)}", fontSize = 11.sp, color = InvColors.Muted)
                Text(
                    text = "Cəm ${Format.money(entry.lineTotal)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                )
            }
        }
    }
}

@Composable
private fun IssueRow(issue: InvTransactionDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Format.date(issue.transactionDate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                )
                Text(
                    text = Format.number(issue.quantity),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InvColors.Terracotta,
                )
            }
            issue.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    fontSize = 11.sp,
                    color = InvColors.MutedSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, color = InvColors.MutedSoft)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = InvColors.Ink,
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)
        DetailTag(text = count.toString(), color = InvColors.Muted)
    }
}

@Composable
private fun DetailTag(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = InvColors.Surface,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            fontSize = 12.sp,
            color = InvColors.MutedSoft,
        )
    }
}

@Composable
private fun DetailLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = InvColors.Terracotta, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Yüklənir…", fontSize = 12.sp, color = InvColors.Muted)
        }
    }
}

@Composable
private fun DetailError(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, fontSize = 14.sp, color = InvColors.Muted)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = InvColors.Terracotta),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Yenidən cəhd et")
            }
        }
    }
}
