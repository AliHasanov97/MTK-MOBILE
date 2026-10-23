package az.mtk.app.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.ReportExports
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.DocumentPreviewHost
import az.mtk.app.ui.components.rememberDocumentPreviewController
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPill
import az.mtk.app.ui.screens.operations.OpsPillRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTopBar
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch
import java.time.LocalDate

private val SHORT_MONTHS = listOf(
    "Yan", "Fev", "Mar", "Apr", "May", "İyn",
    "İyl", "Avq", "Sen", "Okt", "Noy", "Dek",
)

private val FULL_MONTHS = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
    "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
)

private data class MonthDetail(
    val row: az.mtk.app.data.api.ReportScheduleRow,
    val cell: az.mtk.app.data.api.ReportMonthCell,
)

/**
 * Hesabatlar modulu — saytdakı "Ödəniş qrafiki" və "Ümumi hesabat"
 * səhifələrinin mobil qarşılığı. Cədvəl yoxdur: kartlar, çiplər və
 * aşağıdan açılan panellər.
 */
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    canDownload: Boolean = true,
    viewModel: ReportsViewModel = mtkViewModel { ReportsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(ReportsTab.SCHEDULE.name) }
    val tab = runCatching { ReportsTab.valueOf(tabName) }.getOrDefault(ReportsTab.SCHEDULE)

    LaunchedEffect(Unit) { viewModel.ensureSchedule() }
    LaunchedEffect(tab) {
        if (tab == ReportsTab.MONTHLY) viewModel.ensureMonthly()
    }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            Column(modifier = Modifier.background(OpsColors.Background)) {
                OpsTopBar(
                    title = "Hesabatlar",
                    subtitle = "Ödəniş qrafiki · maliyyə hesabatı",
                    onBack = onBack,
                    onRefresh = {
                        if (tab == ReportsTab.SCHEDULE) {
                            viewModel.refreshSchedule()
                        } else {
                            viewModel.refreshMonthly()
                        }
                    },
                )
                ReportsSegmented(
                    selected = tab,
                    onSelect = { tabName = it.name },
                )
            }
        },
    ) { padding ->
        when (tab) {
            ReportsTab.SCHEDULE -> ScheduleContent(
                state = state.schedule,
                yearOptions = viewModel.yearOptions(),
                canDownload = canDownload,
                modifier = Modifier.padding(padding),
                onKind = viewModel::setKind,
                onYear = viewModel::setYear,
                onSearch = viewModel::setSearch,
                onToggleBuilding = viewModel::toggleBuilding,
                onToggleProperty = viewModel::toggleProperty,
                onToggleDebt = viewModel::toggleDebt,
                onClearBuildings = viewModel::clearBuildingFilter,
                onClearProperties = viewModel::clearPropertyFilter,
                onClearDebt = viewModel::clearDebtFilter,
                onClearFilters = viewModel::clearFilters,
                onRetry = viewModel::refreshSchedule,
            )

            ReportsTab.MONTHLY -> MonthlyContent(
                state = state.monthly,
                canDownload = canDownload,
                modifier = Modifier.padding(padding),
                onPrev = { viewModel.shiftMonth(-1) },
                onNext = { viewModel.shiftMonth(1) },
                onMonth = viewModel::setMonth,
                onRetry = viewModel::refreshMonthly,
            )
        }
    }
}

// ---- Tab seçimi --------------------------------------------------------------

@Composable
private fun ReportsSegmented(
    selected: ReportsTab,
    onSelect: (ReportsTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = OpsColors.Subtle,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ReportsTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (active) OpsColors.Surface else Color.Transparent,
                    shadowElevation = if (active) 1.dp else 0.dp,
                ) {
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(vertical = 9.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) OpsColors.Ink else OpsColors.MutedSoft,
                    )
                }
            }
        }
    }
}

// ---- Ödəniş qrafiki ----------------------------------------------------------

@Composable
private fun ScheduleContent(
    state: ScheduleState,
    yearOptions: List<Int>,
    canDownload: Boolean,
    modifier: Modifier,
    onKind: (ReportKind) -> Unit,
    onYear: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onToggleBuilding: (String) -> Unit,
    onToggleProperty: (String) -> Unit,
    onToggleDebt: (ReportDebtFilter) -> Unit,
    onClearBuildings: () -> Unit,
    onClearProperties: () -> Unit,
    onClearDebt: () -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
) {
    var showBuildings by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var showDebt by remember { mutableStateOf(false) }
    var monthDetail by remember { mutableStateOf<MonthDetail?>(null) }
    val errorMessage = state.error

    val exportPath = ReportExports.annualExcel(state.year, state.kind.propertyType)
    val exportName = "illik-odenis-qrafiki-${state.kind.propertyType}-${state.year}.xlsx"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScheduleSummaryCard(state) }

        if (canDownload) {
            item {
                ReportFileButton(
                    label = "Excel-ə ixrac",
                    path = exportPath,
                    fallbackName = exportName,
                    primary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }

        item {
            OpsPillRow(
                pills = ReportKind.entries.map { kind ->
                    OpsPill(
                        label = kind.label,
                        selected = state.kind == kind,
                        onClick = { onKind(kind) },
                    )
                },
            )
        }

        item {
            OpsPillRow(
                pills = yearOptions.map { year ->
                    OpsPill(
                        label = year.toString(),
                        selected = state.year == year,
                        onClick = { onYear(year) },
                    )
                },
            )
        }

        item {
            OpsSearchField(
                value = state.search,
                onValueChange = onSearch,
                placeholder = "Mənzil, bina və ya qaraj üzrə…",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item {
            OpsPillRow(
                pills = buildList {
                    if (state.kind == ReportKind.APARTMENT) {
                        add(
                            OpsPill(
                                label = filterLabel("Bina", state.buildingFilter.size),
                                selected = state.buildingFilter.isNotEmpty(),
                                onClick = { showBuildings = true },
                            ),
                        )
                    }
                    add(
                        OpsPill(
                            label = filterLabel("Əmlak", state.propertyFilter.size),
                            selected = state.propertyFilter.isNotEmpty(),
                            onClick = { showProperties = true },
                        ),
                    )
                    add(
                        OpsPill(
                            label = filterLabel("Borc", state.debtFilter.size),
                            selected = state.debtFilter.isNotEmpty(),
                            onClick = { showDebt = true },
                        ),
                    )
                    if (state.activeFilterCount > 0) {
                        add(
                            OpsPill(
                                label = "Təmizlə (${state.activeFilterCount})",
                                selected = false,
                                onClick = onClearFilters,
                            ),
                        )
                    }
                },
            )
        }

        item { MonthLegend() }

        when {
            state.loading -> item { OpsLoading() }

            state.error != null -> item {
                OpsError(errorMessage, onRetry = onRetry)
            }

            state.visibleRows.isEmpty() -> item {
                OpsEmpty(
                    title = "Əmlak tapılmadı",
                    text = "Bu axtarışa və ya filtrə uyğun mənzil/qaraj yoxdur.",
                )
            }

            else -> {
                items(state.visibleRows, key = { it.property.key }) { row ->
                    ScheduleCard(
                        row = row,
                        onMonthClick = { cell -> monthDetail = MonthDetail(row, cell) },
                    )
                }
            }
        }

        item { ScheduleFooter() }
    }

    if (showBuildings) {
        ReportFilterSheet(
            title = "Bina seç",
            options = state.buildingOptions,
            selected = state.buildingFilter,
            onToggle = onToggleBuilding,
            onClear = onClearBuildings,
            onDismiss = { showBuildings = false },
            searchable = true,
        )
    }

    if (showProperties) {
        ReportFilterSheet(
            title = "Əmlak seç",
            options = state.propertyOptions,
            selected = state.propertyFilter,
            onToggle = onToggleProperty,
            onClear = onClearProperties,
            onDismiss = { showProperties = false },
            searchable = true,
        )
    }

    if (showDebt) {
        ReportFilterSheet(
            title = "Borc vəziyyəti",
            options = ReportDebtFilter.entries.map { it.id to it.label },
            selected = state.debtFilter.map { it.id }.toSet(),
            onToggle = { id ->
                ReportDebtFilter.entries.firstOrNull { it.id == id }?.let(onToggleDebt)
            },
            onClear = onClearDebt,
            onDismiss = { showDebt = false },
            searchable = false,
        )
    }

    monthDetail?.let { detail ->
        MonthDetailSheet(
            detail = detail,
            year = state.year,
            onDismiss = { monthDetail = null },
        )
    }
}

private fun filterLabel(base: String, count: Int): String =
    if (count > 0) "$base ($count)" else base

@Composable
private fun ScheduleSummaryCard(state: ScheduleState) {
    val unpaidMonths = state.visibleRows.sumOf { row ->
        row.months.count { it.status == 0 }
    }
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "İllik ödəniş qrafiki",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${state.year} · ${state.kind.label}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = "${state.visibleRows.size} əmlak",
                foreground = OpsColors.Terracotta,
                background = OpsColors.Container,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(
                label = "Cari borc",
                value = Format.money(state.totalDebt),
                color = if (state.totalDebt > 0.005) OpsColors.Error else OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Borclu əmlak",
                value = state.debtors.toString(),
                color = if (state.debtors > 0) OpsColors.Amber else OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Ödənilməyib",
                value = "$unpaidMonths ay",
                color = if (unpaidMonths > 0) OpsColors.Error else OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = OpsColors.Subtle,
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = OpsColors.MutedSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LegendItem(OpsColors.Olive, "Ödənilib", Modifier.weight(1f))
        LegendItem(OpsColors.Blue, "Qismən", Modifier.weight(1f))
        LegendItem(OpsColors.Error, "Ödənilməyib", Modifier.weight(1f))
        LegendItem(OpsColors.MutedSoft, "Haqq yoxdur", Modifier.weight(1.3f))
    }
}

@Composable
private fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier.width(7.dp).height(7.dp),
            shape = RoundedCornerShape(2.dp),
            color = color,
        ) {}
        Text(
            text = label,
            fontSize = 9.sp,
            color = OpsColors.MutedSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScheduleCard(
    row: az.mtk.app.data.api.ReportScheduleRow,
    onMonthClick: (az.mtk.app.data.api.ReportMonthCell) -> Unit,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.property.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!row.property.buildingName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.property.buildingName,
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (row.hasDebt) {
                OpsChip("Borclu", OpsColors.Error, OpsColors.ErrorSoft)
            } else {
                OpsChip("Təmiz", OpsColors.Olive, OpsColors.OliveSoft)
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            row.months.chunked(6).forEach { chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    chunk.forEach { cell ->
                        MonthCell(
                            cell = cell,
                            modifier = Modifier.weight(1f),
                            onClick = { onMonthClick(cell) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Cari borc",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                color = OpsColors.MutedSoft,
            )
            Text(
                text = Format.money(row.currentDebt),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (row.hasDebt) OpsColors.Error else OpsColors.Olive,
            )
        }
    }
}

@Composable
private fun MonthCell(
    cell: az.mtk.app.data.api.ReportMonthCell,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (background, foreground, symbol) = when (cell.status) {
        2 -> Triple(OpsColors.OliveSoft, OpsColors.Olive, "✓")
        1 -> Triple(OpsColors.BlueSoft, OpsColors.Blue, "±")
        0 -> Triple(OpsColors.ErrorSoft, OpsColors.Error, "−")
        3 -> Triple(OpsColors.Subtle, OpsColors.MutedSoft, "×")
        else -> Triple(OpsColors.Subtle, OpsColors.MutedSoft, "·")
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = SHORT_MONTHS[cell.month - 1],
                fontSize = 9.sp,
                color = foreground,
                maxLines = 1,
            )
            Text(
                text = symbol,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = foreground,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ScheduleFooter() {
    Text(
        text = "Qrafik bütün mənzil və qarajları əhatə edir. Cari borc yalnız seçilmiş ilə " +
            "deyil, bütün tarix üzrə qalıq borcu göstərir.",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        fontSize = 10.sp,
        color = OpsColors.MutedSoft,
    )
}

@Composable
private fun MonthDetailSheet(
    detail: MonthDetail,
    year: Int,
    onDismiss: () -> Unit,
) {
    val cell = detail.cell
    val statusLabel = if (cell.status == null) {
        "Haqq yaradılmayıb"
    } else {
        Format.chargeStatus(cell.status)
    }
    val statusColor = when (cell.status) {
        2 -> OpsColors.Olive
        1 -> OpsColors.Blue
        0 -> OpsColors.Error
        else -> OpsColors.MutedSoft
    }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = "${FULL_MONTHS[cell.month - 1]} $year",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = buildString {
                append(detail.row.property.label)
                detail.row.property.buildingName?.takeIf { it.isNotBlank() }?.let {
                    append(" · ")
                    append(it)
                }
            },
            fontSize = 12.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = OpsColors.Surface,
            border = BorderStroke(1.dp, OpsColors.Border),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                OpsInfoRow("Vəziyyət", statusLabel, statusColor)
                if (cell.hasCharge) {
                    OpsInfoRow("Aylıq haqq", Format.money(cell.amount))
                    OpsInfoRow("Ödənilib", Format.money(cell.paidAmount))
                    OpsInfoRow(
                        label = "Qalıq borc",
                        value = Format.money(cell.remaining),
                        valueColor = if (cell.remaining > 0.005) OpsColors.Error else OpsColors.Olive,
                    )
                }
            }
        }

        if (!cell.hasCharge) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Bu ay üçün haqq yaradılmayıb.",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
        }

        Spacer(Modifier.height(16.dp))
        OpsPrimaryButton(
            text = "Bağla",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReportFilterSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    searchable: Boolean,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(options, search) {
        if (search.isBlank()) {
            options
        } else {
            options.filter { it.second.contains(search.trim(), ignoreCase = true) }
        }
    }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = OpsColors.Background,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            if (selected.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Təmizlə", fontSize = 12.sp, color = OpsColors.Terracotta)
                }
            }
        }

        if (searchable) {
            Spacer(Modifier.height(8.dp))
            OpsSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Axtar…",
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Text(
                text = "Uyğun seçim yoxdur.",
                modifier = Modifier.padding(vertical = 20.dp),
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered, key = { it.first }) { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(id) }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = id in selected,
                            onCheckedChange = { onToggle(id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OpsColors.Terracotta,
                                uncheckedColor = OpsColors.MutedSoft,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = OpsColors.Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OpsPrimaryButton(
            text = "Bağla",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---- Ümumi hesabat -----------------------------------------------------------

@Composable
private fun MonthlyContent(
    state: MonthlyState,
    canDownload: Boolean,
    modifier: Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMonth: (Int, Int) -> Unit,
    onRetry: () -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val errorMessage = state.error

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            MonthNavigator(
                year = state.year,
                month = state.month,
                onPrev = onPrev,
                onNext = onNext,
                onPick = { showMonthPicker = true },
            )
        }

        if (canDownload) {
            item {
                ReportFileButton(
                    label = "PDF hesabatı aç",
                    path = ReportExports.monthlyPdf(state.year, state.month),
                    fallbackName = "ayliq-hesabat-${state.year}-${"%02d".format(state.month)}.pdf",
                    previewTitle = "Aylıq hesabat — ${Format.period("${state.year}-${"%02d".format(state.month)}")}",
                    primary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }

        item { OfficialBalanceCard(state.companyBalance) }

        if (state.error != null && state.loaded) {
            item { OpsError(errorMessage, onRetry = onRetry) }
        }

        when {
            state.loading && !state.loaded -> item { OpsLoading() }

            !state.loaded -> item {
                OpsError(errorMessage ?: "Hesabat yüklənmədi", onRetry = onRetry)
            }

            else -> {
            item {
                IncomeSummaryCard(
                    year = state.year,
                    month = state.month,
                    summary = state.summary,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Aylıq xərclər",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpsColors.Ink,
                    )
                    OpsChip(
                        text = Format.money(state.summary.totalExpense),
                        foreground = OpsColors.Error,
                        background = OpsColors.ErrorSoft,
                    )
                }
            }

            item {
                Text(
                    text = "${state.summary.groups.sumOf { it.lines.size }} qeyd · " +
                        "${state.summary.groups.size} kateqoriya",
                    modifier = Modifier.padding(horizontal = 22.dp),
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }

            if (state.summary.groups.isEmpty()) {
                item {
                    OpsEmpty(
                        title = "Xərc qeydə alınmayıb",
                        text = "Seçilmiş ay üzrə xərc tranzaksiyası yoxdur.",
                    )
                }
            } else {
                items(state.summary.groups, key = { it.category }) { group ->
                    ExpenseGroupCard(group)
                }
            }

            item {
                FinalSummaryCard(
                    year = state.year,
                    month = state.month,
                    summary = state.summary,
                )
            }

            item {
                Text(
                    text = "Hazırlanma tarixi: ${todayLabel()} · " +
                        "${state.transactionCount} tranzaksiya yükləndi",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }
        }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            year = state.year,
            month = state.month,
            onSelect = { year, month ->
                showMonthPicker = false
                onMonth(year, month)
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun MonthNavigator(
    year: Int,
    month: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = OpsColors.Surface,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Əvvəlki ay",
                    tint = OpsColors.Terracotta,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPick)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${FULL_MONTHS[month - 1]} $year",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    text = "Dövrü dəyişmək üçün toxunun",
                    fontSize = 9.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Növbəti ay",
                    tint = OpsColors.Terracotta,
                )
            }
        }
    }
}

@Composable
private fun OfficialBalanceCard(company: az.mtk.app.data.api.ReportCompanyBalanceDto?) {
    if (company == null) return
    val balance = company.currentBalance ?: 0.0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = OpsColors.Subtle,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rəsmi balans",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Ink,
                )
                Text(
                    text = "Bütün tarix üzrə kassa qalığı",
                    fontSize = 9.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            Text(
                text = Format.money(balance),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (balance < 0) OpsColors.Error else OpsColors.Olive,
            )
        }
    }
}

@Composable
private fun IncomeSummaryCard(
    year: Int,
    month: Int,
    summary: ReportMonthlySummary,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aylıq gəlirlər",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    text = "${FULL_MONTHS[month - 1]} $year",
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = Format.money(summary.monthlyIncome),
                foreground = OpsColors.Olive,
                background = OpsColors.OliveSoft,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(
                label = "Faktiki yığım",
                value = Format.money(summary.collected),
                color = OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Digər gəlirlər",
                value = Format.money(summary.otherIncome),
                color = OpsColors.Blue,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(
                label = "Əvvəlki balans",
                value = Format.money(summary.previousBalance),
                color = if (summary.previousBalance < 0) OpsColors.Error else OpsColors.Olive,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Cari ay ümumi gəlir",
                value = Format.money(summary.monthlyIncome),
                color = OpsColors.Terracotta,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExpenseGroupCard(group: ReportExpenseGroup) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.category,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Format.money(group.total),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Error,
            )
        }

        Spacer(Modifier.height(6.dp))

        group.lines.forEachIndexed { index, line ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp),
                    color = OpsColors.Border,
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = line.description,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = Format.money(line.amount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OpsColors.Ink,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FinalSummaryCard(
    year: Int,
    month: Int,
    summary: ReportMonthlySummary,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Yekun hesabat",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "${FULL_MONTHS[month - 1]} $year",
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(10.dp))

        OpsInfoRow(
            label = "Əvvəlki balans",
            value = Format.money(summary.previousBalance),
            valueColor = if (summary.previousBalance < 0) OpsColors.Error else OpsColors.Ink,
        )
        OpsInfoRow("Cari aylıq gəlir", Format.money(summary.collected), OpsColors.Olive)
        OpsInfoRow("Digər gəlirlər", Format.money(summary.otherIncome), OpsColors.Blue)
        OpsInfoRow(
            label = "Aylıq cəmi balans",
            value = Format.money(summary.monthlyBalance),
            valueColor = if (summary.monthlyBalance < 0) OpsColors.Error else OpsColors.Ink,
        )
        OpsInfoRow("Cəmi aylıq xərc", Format.money(summary.totalExpense), OpsColors.Error)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = OpsColors.Border,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Yekun balans",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            Text(
                text = Format.money(summary.finalBalance),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (summary.finalBalance < 0) OpsColors.Error else OpsColors.Olive,
            )
        }
    }
}

@Composable
private fun MonthPickerSheet(
    year: Int,
    month: Int,
    onSelect: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickedYear by remember { mutableIntStateOf(year) }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = "Dövr seç",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { pickedYear = (pickedYear - 1).coerceAtLeast(2000) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Əvvəlki il",
                    tint = OpsColors.Terracotta,
                )
            }
            Text(
                text = pickedYear.toString(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            IconButton(onClick = { pickedYear = (pickedYear + 1).coerceAtMost(2200) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Növbəti il",
                    tint = OpsColors.Terracotta,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0 until 4).forEach { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until 3).forEach { columnIndex ->
                        val value = rowIndex * 3 + columnIndex + 1
                        val active = pickedYear == year && value == month
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(pickedYear, value) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) OpsColors.Terracotta else OpsColors.Subtle,
                        ) {
                            Text(
                                text = SHORT_MONTHS[value - 1],
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else OpsColors.Ink,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val today = LocalDate.now()
            OpsSecondaryButton(
                text = "Bu ay",
                onClick = { onSelect(today.year, today.monthValue) },
                modifier = Modifier.weight(1f),
            )
            OpsPrimaryButton(
                text = "Bağla",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---- Fayl ixracı -------------------------------------------------------------

@Composable
private fun ReportFileButton(
    label: String,
    path: String,
    fallbackName: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    previewTitle: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preview = rememberDocumentPreviewController()
    var downloading by remember { mutableStateOf(false) }

    val onClick: () -> Unit = {
        scope.launch {
            downloading = true
            try {
                val result = FileRepository.downloadToInternal(
                    context = context,
                    path = path,
                    fallbackName = fallbackName,
                )
                if (fallbackName.endsWith(".pdf", ignoreCase = true)) {
                    preview.show(
                        filePath = result.file.absolutePath,
                        fileName = result.fileName,
                        mimeHint = "application/pdf",
                        title = previewTitle ?: label,
                    )
                } else if (!FileRepository.openFile(context, result.file)) {
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
    }

    val text = if (downloading) "Yüklənir…" else label
    if (primary) {
        OpsPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = !downloading,
        )
    } else {
        OpsSecondaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = !downloading,
        )
    }

    DocumentPreviewHost(preview)
}

private fun todayLabel(): String {
    val today = LocalDate.now()
    return "%02d.%02d.%d".format(today.dayOfMonth, today.monthValue, today.year)
}
