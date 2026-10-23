package az.mtk.app.ui.screens.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvLowStockDto
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InvNomenclatureResponse
import az.mtk.app.data.api.InvPurchaseHistoryItem
import az.mtk.app.data.api.InvStockDto
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.data.api.InventoryRepository
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format

internal object InvColors {
    val Background = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF4F4EE)
    val Container = Color(0xFFF8EBE6)
    val Border = Color(0xFFE2E0D8)
    val Ink = Color(0xFF201A18)
    val Muted = Color(0xFF594139)
    val MutedSoft = Color(0xFF8D7168)
    val Terracotta = Color(0xFFD9531E)
    val TerracottaVivid = Color(0xFFE85D04)
    val Olive = Color(0xFF6F8559)
    val Error = Color(0xFFBA1A1A)
}

/**
 * İnventar modulu — saytdakı `panel/inventar` bölməsinin mobil qarşılığı:
 * Anbar qalıqları, material reyestri, daxilolmalar və çıxarışlar bir ekranda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onOpenMaterial: (String) -> Unit,
    canManage: Boolean,
    viewModel: InventoryViewModel = mtkViewModel { InventoryViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(canManage) { viewModel.setCanManage(canManage) }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = InvColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "İnventar",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = InvColors.Ink,
                        )
                        Text(
                            text = when (state.selectedTab) {
                                InventoryTab.STOCK -> "Anbar qalıqları və minimum ehtiyat"
                                InventoryTab.MATERIALS -> "Material reyestri və alış tarixçəsi"
                                InventoryTab.RECEIPTS -> "Anbara daxil olan mallar"
                                InventoryTab.ISSUES -> "Anbardan mal çıxarışı"
                            },
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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Yenilə", tint = InvColors.Terracotta)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InvColors.Background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                placeholder = {
                    Text(
                        text = when (state.selectedTab) {
                            InventoryTab.RECEIPTS, InventoryTab.ISSUES -> "Axtar (material, qeyd…)"
                            else -> "Axtar (kod, ad…)"
                        },
                        fontSize = 14.sp,
                        color = InvColors.MutedSoft,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = InvColors.MutedSoft)
                },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.setQuery("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Təmizlə",
                                tint = InvColors.MutedSoft,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InvColors.Terracotta,
                    unfocusedBorderColor = InvColors.Border,
                    focusedContainerColor = InvColors.Surface,
                    unfocusedContainerColor = InvColors.Surface,
                ),
            )

            InventoryTabs(
                selected = state.selectedTab,
                counts = mapOf(
                    InventoryTab.STOCK to state.stocks?.size,
                    InventoryTab.MATERIALS to state.nomenclaturesTotal,
                    InventoryTab.RECEIPTS to state.receipts?.size,
                    InventoryTab.ISSUES to state.issues?.size,
                ),
                onSelect = viewModel::selectTab,
            )

            if (state.selectedTab == InventoryTab.MATERIALS && state.nomenclaturesLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    color = InvColors.Terracotta,
                    trackColor = InvColors.Subtle,
                )
            }

            state.actionMessage?.let { message ->
                FeedbackBanner(message, isError = false) { viewModel.clearFeedback() }
            }
            state.actionError?.let { message ->
                FeedbackBanner(message, isError = true) { viewModel.clearFeedback() }
            }

            Spacer(Modifier.height(4.dp))

            when (state.selectedTab) {
                InventoryTab.STOCK -> StockTab(state, viewModel, onOpenMaterial)
                InventoryTab.MATERIALS -> MaterialsTab(state, viewModel, onOpenMaterial)
                InventoryTab.RECEIPTS -> MovementsTab(
                    receipts = true,
                    state = state,
                    viewModel = viewModel,
                    onOpenMaterial = onOpenMaterial,
                )
                InventoryTab.ISSUES -> MovementsTab(
                    receipts = false,
                    state = state,
                    viewModel = viewModel,
                    onOpenMaterial = onOpenMaterial,
                )
            }
        }
    }

    if (state.formOpen) {
        NomenclatureFormDialog(
            editing = state.editing,
            loading = state.formLoading,
            saving = state.formSaving,
            error = state.formError,
            onDismiss = viewModel::closeForm,
            onSave = viewModel::saveNomenclature,
        )
    }

    state.deleteTarget?.let { target ->
        DeleteNomenclatureDialog(
            item = target,
            deleting = state.deleting,
            onDismiss = viewModel::cancelDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }

    state.historyTarget?.let { target ->
        PurchaseHistoryDialog(
            item = target,
            history = state.history,
            error = state.historyError,
            onDismiss = viewModel::closeHistory,
        )
    }

    if (state.issueFormOpen) {
        IssueFormDialog(
            options = state.issueOptions,
            saving = state.issueSaving,
            error = state.issueError,
            onDismiss = viewModel::closeIssueForm,
            onSubmit = viewModel::submitIssue,
        )
    }
}

/* ------------------------------------------------------------- naviqasiya */

@Composable
private fun InventoryTabs(
    selected: InventoryTab,
    counts: Map<InventoryTab, Int?>,
    onSelect: (InventoryTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InventoryTab.entries.forEach { tab ->
            val active = tab == selected
            val count = counts[tab]
            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clickable { onSelect(tab) },
                shape = RoundedCornerShape(19.dp),
                color = if (active) InvColors.Terracotta else InvColors.Subtle,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else InvColors.Ink,
                    )
                    if (count != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (active) Color.White.copy(alpha = 0.3f) else InvColors.Border,
                        ) {
                            Text(
                                text = count.toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else InvColors.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ anbar */

@Composable
private fun StockTab(
    state: InventoryViewModel.UiState,
    viewModel: InventoryViewModel,
    onOpenMaterial: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StockSegment.entries.forEach { segment ->
                val active = state.stockSegment == segment
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { viewModel.setStockSegment(segment) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) InvColors.Container else InvColors.Surface,
                    border = BorderStroke(
                        1.dp,
                        if (active) InvColors.Terracotta.copy(alpha = 0.4f) else InvColors.Border,
                    ),
                ) {
                    Text(
                        text = segment.label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) InvColors.Terracotta else InvColors.Muted,
                    )
                }
            }
        }

        when {
            state.stockLoading && state.stocks == null && state.lowStock == null -> LoadingState()

            state.stockError != null -> ErrorState(state.stockError) {
                if (state.stockSegment == StockSegment.LOW) viewModel.loadLowStock() else viewModel.loadStocks()
            }

            state.stockSegment == StockSegment.ON_HAND -> {
                val rows = state.stocks
                if (rows == null) {
                    LoadingState()
                } else {
                    val filtered = remember(rows, state.query) {
                        val term = state.query.trim().lowercase()
                        if (term.isEmpty()) rows else rows.filter { row ->
                            "${row.nomenclatureCode} ${row.nomenclatureName}".lowercase().contains(term)
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = if (rows.isEmpty()) "Qalıq yoxdur" else "Nəticə tapılmadı",
                            text = if (rows.isEmpty()) {
                                "Hələ heç bir mal anbara daxil edilməyib."
                            } else {
                                "Bu axtarışa uyğun material tapılmadı."
                            },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items = filtered, key = { it.nomenclatureId }) { row ->
                                StockCard(row) { onOpenMaterial(row.nomenclatureId) }
                            }
                        }
                    }
                }
            }

            else -> {
                val rows = state.lowStock
                if (rows == null) {
                    LoadingState()
                } else {
                    val filtered = remember(rows, state.query) {
                        val term = state.query.trim().lowercase()
                        if (term.isEmpty()) rows else rows.filter { row ->
                            "${row.nomenclatureCode} ${row.nomenclatureName}".lowercase().contains(term)
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = "Bütün ehtiyatlar kifayətdir",
                            text = "Heç bir material minimum ehtiyat səviyyəsindən aşağı deyil.",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items = filtered, key = { it.nomenclatureId }) { row ->
                                LowStockCard(row) { onOpenMaterial(row.nomenclatureId) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockCard(row: InvStockDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = InvColors.Surface),
        border = BorderStroke(1.dp, InvColors.Border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = row.nomenclatureName ?: "Adsız material",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.nomenclatureCode ?: "—",
                    fontSize = 11.sp,
                    color = InvColors.MutedSoft,
                )
                Text(
                    text = "Son əməliyyat: ${Format.dateTime(row.lastTransactionDate)}",
                    fontSize = 11.sp,
                    color = InvColors.MutedSoft,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Format.number(row.quantityOnHand),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InvColors.Ink,
                )
                Text("vahid", fontSize = 10.sp, color = InvColors.MutedSoft)
            }
        }
    }
}

@Composable
private fun LowStockCard(row: InvLowStockDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = InvColors.Surface),
        border = BorderStroke(1.dp, InvColors.Error.copy(alpha = 0.35f)),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = row.nomenclatureName ?: "Adsız material",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InvColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.nomenclatureCode ?: "—",
                    fontSize = 11.sp,
                    color = InvColors.MutedSoft,
                )
                Text(
                    text = "Min: ${Format.number(row.minStockLevel)} · Çatışmır: ${Format.number(row.deficit)}",
                    fontSize = 11.sp,
                    color = InvColors.Error,
                )
            }
            Text(
                text = Format.number(row.quantityOnHand),
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InvColors.Error,
            )
        }
    }
}

/* ------------------------------------------------------------- materiallar */

@Composable
private fun MaterialsTab(
    state: InventoryViewModel.UiState,
    viewModel: InventoryViewModel,
    onOpenMaterial: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.nomenclaturesTotal} qeyd",
                fontSize = 12.sp,
                color = InvColors.Muted,
            )
            if (state.canManage) {
                Button(
                    onClick = viewModel::openCreateForm,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InvColors.Terracotta),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Yeni material", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when {
            state.nomenclaturesLoading && state.nomenclatures.isEmpty() -> LoadingState()

            state.nomenclaturesError != null && state.nomenclatures.isEmpty() ->
                ErrorState(state.nomenclaturesError) {
                    viewModel.loadNomenclatures(0, state.query.trim().ifBlank { null })
                }

            state.nomenclatures.isEmpty() -> EmptyState(
                title = "Material yoxdur",
                text = if (state.query.isBlank()) {
                    "Hələ heç bir material yaradılmayıb."
                } else {
                    "Bu axtarışa uyğun material tapılmadı."
                },
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = state.nomenclatures, key = { it.id }) { item ->
                    MaterialCard(
                        item = item,
                        canManage = state.canManage,
                        onOpen = { onOpenMaterial(item.id) },
                        onHistory = { viewModel.openHistory(item) },
                        onEdit = { viewModel.openEditForm(item) },
                        onDelete = { viewModel.askDelete(item) },
                    )
                }
                item {
                    PaginationRow(
                        page = state.nomenclaturesPage,
                        total = state.nomenclaturesTotal,
                        loading = state.nomenclaturesLoading,
                        onPrevious = viewModel::previousPage,
                        onNext = viewModel::nextPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    item: InvNomenclatureListItem,
    canManage: Boolean,
    onOpen: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val active = item.isActive != false
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = InvColors.Surface),
        border = BorderStroke(1.dp, InvColors.Border),
        onClick = onOpen,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!item.code.isNullOrBlank()) {
                            SmallTag(item.code, InvColors.Terracotta)
                        }
                        SmallTag(
                            text = if (active) "Aktiv" else "Deaktiv",
                            color = if (active) InvColors.Olive else InvColors.MutedSoft,
                        )
                    }
                    Text(
                        text = item.name ?: "Adsız material",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InvColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = InvColors.MutedSoft,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = InvColors.MutedSoft.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag(InvEnums.categoryLabel(item.category), InvColors.Muted)
                SmallTag(InvEnums.unitLabel(item.unit), InvColors.Muted)
            }

            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("Alış tarixçəsi", fontSize = 11.sp, color = InvColors.Muted)
                    }
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("Redaktə", fontSize = 11.sp, color = InvColors.Terracotta)
                    }
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("Sil", fontSize = 11.sp, color = InvColors.Error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaginationRow(
    page: Int,
    total: Int,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val pageCount = maxOf(1, (total + InventoryRepository.PAGE_SIZE - 1) / InventoryRepository.PAGE_SIZE)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Səhifə ${page + 1}/$pageCount", fontSize = 12.sp, color = InvColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = page > 0 && !loading,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text("Əvvəlki", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onNext,
                enabled = (page + 1) < pageCount && !loading,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text("Növbəti", fontSize = 11.sp)
            }
        }
    }
}

/* ------------------------------------------------- daxilolma / çıxarışlar */

@Composable
private fun MovementsTab(
    receipts: Boolean,
    state: InventoryViewModel.UiState,
    viewModel: InventoryViewModel,
    onOpenMaterial: (String) -> Unit,
) {
    val rows = if (receipts) state.receipts else state.issues
    val error = state.transactionsError

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val filteredCount = rows?.let { list ->
                val term = state.query.trim().lowercase()
                if (term.isEmpty()) list.size else list.count { row ->
                    "${row.nomenclatureName} ${row.notes}".lowercase().contains(term)
                }
            }
            Text(
                text = if (filteredCount != null) "$filteredCount qeyd" else "Yüklənir…",
                fontSize = 12.sp,
                color = InvColors.Muted,
            )
            if (!receipts && state.canManage) {
                Button(
                    onClick = viewModel::openIssueForm,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InvColors.Terracotta),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Yeni çıxarış", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when {
            state.transactionsLoading && rows == null -> LoadingState()

            error != null && rows == null -> ErrorState(error) {
                viewModel.loadTransactions(receipts)
            }

            rows == null -> LoadingState()

            rows.isEmpty() -> EmptyState(
                title = if (receipts) "Daxilolma yoxdur" else "Çıxarış yoxdur",
                text = if (receipts) {
                    "Hələ daxilolma yoxdur — alışlar Payments bölməsində qəbul edilir."
                } else {
                    "Hələ anbardan mal çıxarılmayıb."
                },
            )

            else -> {
                val filtered = remember(rows, state.query) {
                    val term = state.query.trim().lowercase()
                    if (term.isEmpty()) rows else rows.filter { row ->
                        "${row.nomenclatureName} ${row.notes}".lowercase().contains(term)
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyState(title = "Nəticə tapılmadı", text = "Axtarışa uyğun qeyd yoxdur.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = filtered, key = { it.id }) { row ->
                            MovementCard(
                                row = row,
                                receipts = receipts,
                                invoiceNumber = row.referenceId?.let { state.invoiceNumbers[it] },
                                onClick = { row.nomenclatureId?.let(onOpenMaterial) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementCard(
    row: InvTransactionDto,
    receipts: Boolean,
    invoiceNumber: String?,
    onClick: () -> Unit,
) {
    val quantity = row.quantity ?: 0.0
    val unitPrice = row.unitPrice
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = InvColors.Surface),
        border = BorderStroke(1.dp, InvColors.Border),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.nomenclatureName ?: "Adsız material",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InvColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Format.dateTime(row.transactionDate),
                        fontSize = 11.sp,
                        color = InvColors.MutedSoft,
                    )
                }
                Text(
                    text = Format.number(quantity),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (receipts) InvColors.Olive else InvColors.Terracotta,
                )
            }

            if (receipts) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallTag(
                        text = "Qaimə: ${invoiceNumber ?: row.referenceId?.take(8) ?: "—"}",
                        color = InvColors.Muted,
                    )
                    if (unitPrice != null) {
                        Text(
                            text = "Vahid ${Format.money(unitPrice)} · Cəm ${Format.money(unitPrice * quantity)}",
                            fontSize = 11.sp,
                            color = InvColors.Muted,
                        )
                    }
                }
            }

            row.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    fontSize = 12.sp,
                    color = InvColors.MutedSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* --------------------------------------------------------------- dialoqlar */

@Composable
private fun NomenclatureFormDialog(
    editing: InvNomenclatureResponse?,
    loading: Boolean,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (NomenclatureInput) -> Unit,
) {
    var code by remember(editing?.id) { mutableStateOf(editing?.code.orEmpty()) }
    var name by remember(editing?.id) { mutableStateOf(editing?.name.orEmpty()) }
    var description by remember(editing?.id) { mutableStateOf(editing?.description.orEmpty()) }
    var category by remember(editing?.id) { mutableIntStateOf(editing?.category ?: InvEnums.CATEGORY_MATERIAL) }
    var unit by remember(editing?.id) { mutableIntStateOf(editing?.unit ?: InvEnums.UNIT_PIECE) }
    var minStock by remember(editing?.id) {
        mutableStateOf(editing?.minStockLevel?.let { Format.number(it).replace(" ", "") }.orEmpty())
    }
    var isActive by remember(editing?.id) { mutableStateOf(editing?.isActive ?: true) }
    var localError by remember(editing?.id) { mutableStateOf<String?>(null) }

    val categoryOptions = listOf(
        InvEnums.CATEGORY_MATERIAL,
        InvEnums.CATEGORY_EQUIPMENT,
        InvEnums.CATEGORY_SERVICE,
        InvEnums.CATEGORY_SUPPLY,
        InvEnums.CATEGORY_OTHER,
    ).map { it to InvEnums.categoryLabel(it) }

    val unitOptions = (1..10).map { it to InvEnums.unitLabel(it) }

    fun submit() {
        val codeValue = code.trim()
        val nameValue = name.trim()
        if (nameValue.isEmpty()) {
            localError = "Ad mütləqdir."
            return
        }
        if (editing == null && codeValue.isEmpty()) {
            localError = "Kod mütləqdir."
            return
        }
        val minValue = minStock.trim().replace(',', '.').toDoubleOrNull()
        if (minStock.isNotBlank() && (minValue == null || minValue < 0)) {
            localError = "Minimum ehtiyat mənfi ola bilməz."
            return
        }
        localError = null
        onSave(
            NomenclatureInput(
                code = codeValue,
                name = nameValue,
                description = description.trim().ifBlank { null },
                category = category,
                unit = unit,
                minStockLevel = minValue,
                isActive = isActive,
            ),
        )
    }

    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editing != null) "Materialı redaktə et" else "Yeni material",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = InvColors.Ink,
            )
        },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = InvColors.Terracotta)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kod") },
                        enabled = editing == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    ChoiceField(
                        label = "Kateqoriya",
                        value = InvEnums.categoryLabel(category),
                        options = categoryOptions,
                        onSelect = { category = it },
                    )
                    ChoiceField(
                        label = "Ölçü vahidi",
                        value = InvEnums.unitLabel(unit),
                        options = unitOptions,
                        onSelect = { unit = it },
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { input ->
                            minStock = input.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        },
                        label = { Text("Minimum ehtiyat") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    if (editing == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Aktiv", fontSize = 13.sp, color = InvColors.Muted)
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = InvColors.Olive,
                                ),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Təsvir") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )

                    (localError ?: error)?.let { message ->
                        Text(message, fontSize = 12.sp, color = InvColors.Error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::submit,
                enabled = !loading && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = InvColors.Terracotta),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = if (saving) "Saxlanılır…" else if (editing != null) "Saxla" else "Yarat",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = InvColors.Muted)
            }
        },
    )
}

@Composable
private fun ChoiceField(
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = InvColors.Muted)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = InvColors.Ink,
                    maxLines = 1,
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = InvColors.Muted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (ordinal, text) ->
                    DropdownMenuItem(
                        text = { Text(text, fontSize = 13.sp) },
                        onClick = {
                            expanded = false
                            onSelect(ordinal)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteNomenclatureDialog(
    item: InvNomenclatureListItem,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = { Text("Materialı sil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink) },
        text = {
            Text(
                text = "«${item.name ?: "Material"}» silinsin? Bu əməliyyat geri qaytarıla bilməz.",
                fontSize = 13.sp,
                color = InvColors.Muted,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = InvColors.Error),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (deleting) "Silinir…" else "Sil", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ləğv et", color = InvColors.Muted) }
        },
    )
}

@Composable
private fun PurchaseHistoryDialog(
    item: InvNomenclatureListItem,
    history: List<InvPurchaseHistoryItem>?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${item.name ?: "Material"} — alış tarixçəsi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = InvColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    error != null -> Text(error, fontSize = 12.sp, color = InvColors.Error)
                    history == null -> Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = InvColors.Terracotta)
                    }
                    history.isEmpty() -> Text(
                        text = "Bu material üzrə alış qeydi yoxdur.",
                        fontSize = 12.sp,
                        color = InvColors.Muted,
                    )
                    else -> history.forEach { entry -> HistoryRow(entry) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Bağla", color = InvColors.Terracotta) }
        },
    )
}

@Composable
private fun HistoryRow(entry: InvPurchaseHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = InvColors.Subtle,
        border = BorderStroke(1.dp, InvColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                SmallTag(
                    text = InvEnums.purchaseStatusLabel(entry.status),
                    color = if (entry.status == InvEnums.PURCHASE_RECEIVED) InvColors.Olive else InvColors.Muted,
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
private fun IssueFormDialog(
    options: List<InvNomenclatureListItem>?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String?) -> Unit,
) {
    var selected by remember(options) { mutableStateOf(options?.firstOrNull()) }
    var pickerOpen by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val item = selected
        if (item == null) {
            localError = "Material seçilməlidir."
            return
        }
        val qty = quantity.trim().replace(',', '.').toDoubleOrNull()
        if (qty == null || qty <= 0) {
            localError = "Miqdar 0-dan böyük olmalıdır."
            return
        }
        localError = null
        onSubmit(item.id, qty, notes)
    }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = InvColors.Surface,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
                Text("Yeni çıxarış", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)

                if (options == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = InvColors.Terracotta)
                    }
                } else if (options.isEmpty()) {
                    Text("Material yoxdur — əvvəlcə material yaradın.", fontSize = 13.sp, color = InvColors.Muted)
                } else {
                    Text("Material", fontSize = 11.sp, color = InvColors.Muted)
                    OutlinedButton(
                        onClick = { pickerOpen = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(
                            text = selected?.let { "${it.code ?: "—"} — ${it.name ?: "Material"}" }
                                ?: "Material seçin",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = InvColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Default.ArrowDropDown, null, tint = InvColors.Muted)
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { input ->
                            quantity = input.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        },
                        label = { Text("Miqdar") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Qeyd") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                }

                (localError ?: error)?.let { message ->
                    Text(message, fontSize = 12.sp, color = InvColors.Error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Ləğv et", color = InvColors.Muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = ::submit,
                        enabled = !saving && options?.isNotEmpty() == true,
                        colors = ButtonDefaults.buttonColors(containerColor = InvColors.Terracotta),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(if (saving) "Qeyd olunur…" else "Qeyd et", fontWeight = FontWeight.Bold)
                    }
                }
    }

    if (pickerOpen && options != null) {
        MaterialPickerDialog(
            options = options,
            onSelect = {
                selected = it
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun MaterialPickerDialog(
    options: List<InvNomenclatureListItem>,
    onSelect: (InvNomenclatureListItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(options, search) {
        val term = search.trim().lowercase()
        if (term.isEmpty()) options else options.filter { item ->
            "${item.code} ${item.name}".lowercase().contains(term)
        }
    }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = InvColors.Surface,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
                Text("Material seç", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Axtar (kod, ad…)", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = InvColors.MutedSoft) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                if (filtered.isEmpty()) {
                    Text("Nəticə tapılmadı.", fontSize = 12.sp, color = InvColors.Muted)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = filtered, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) },
                                shape = RoundedCornerShape(10.dp),
                                color = InvColors.Subtle,
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text(
                                        text = item.name ?: "Adsız material",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InvColors.Ink,
                                    )
                                    Text(
                                        text = item.code ?: "—",
                                        fontSize = 11.sp,
                                        color = InvColors.MutedSoft,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Bağla", color = InvColors.Muted) }
                }
    }
}

/* ---------------------------------------------------------------- ümumi UI */

@Composable
private fun FeedbackBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val accent = if (isError) InvColors.Error else InvColors.Olive
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                color = if (isError) InvColors.Error else InvColors.Ink,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    "Bağla",
                    tint = InvColors.MutedSoft,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun SmallTag(text: String?, color: Color) {
    if (text.isNullOrBlank()) return
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
    ) {
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
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(vertical = 48.dp),
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
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
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
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yenidən cəhd et")
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Inventory2,
                null,
                tint = InvColors.MutedSoft.copy(alpha = 0.6f),
                modifier = Modifier.size(34.dp),
            )
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InvColors.Ink)
            Text(
                text = text,
                fontSize = 12.sp,
                color = InvColors.MutedSoft,
            )
        }
    }
}
