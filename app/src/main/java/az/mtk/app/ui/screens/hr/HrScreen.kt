package az.mtk.app.ui.screens.hr

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.HrEmployeeListItemDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrOption
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * HR modulu — MTK-UI `panel/hr` bölməsinin mobil qarşılığı:
 * İşçilər, Ərizələr, Əmrlər, Tabel, Təqvim və Lüğətlər bir axında.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrScreen(
    onBack: () -> Unit,
    onOpenEmployee: (String) -> Unit,
) {
    val viewModel: HrViewModel = mtkViewModel { HrViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = HrColors.Background,
        topBar = {
            HrTopBar(
                title = "HR",
                subtitle = when (state.tab) {
                    HrTab.EMPLOYEES -> "İşçi reyestri və şəxsi kartlar"
                    HrTab.APPLICATIONS -> "Təsdiq gözləyən ərizələr"
                    HrTab.ORDERS -> "HR əmrləri və sənədlər"
                    HrTab.TIMESHEET -> "Aylıq iş vaxtı cədvəli"
                    HrTab.CALENDAR -> "İş və bayram günləri"
                    HrTab.DICTIONARIES -> "Vəzifələr və təhsil ocaqları"
                },
                onBack = onBack,
                onRefresh = if (state.tab == HrTab.EMPLOYEES) ({ viewModel.refresh() }) else null,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(HrColors.Background),
        ) {
            if (state.tab == HrTab.EMPLOYEES) {
                HrSearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Ad, soyad və ya ata adı üzrə axtar…",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            HrPillRow(
                pills = HrTab.entries.map { tab ->
                    HrPill(
                        label = tab.label,
                        selected = state.tab == tab,
                        count = if (tab == HrTab.EMPLOYEES) state.totalCount.takeIf { it > 0 } else null,
                        onClick = { viewModel.selectTab(tab) },
                    )
                },
            )

            state.actionMessage?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearFeedback() } }
            state.actionError?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearFeedback() } }

            Spacer(Modifier.height(6.dp))

            when (state.tab) {
                HrTab.EMPLOYEES -> EmployeesTab(
                    state = state,
                    onOpenEmployee = onOpenEmployee,
                    onOpenFilters = { filtersOpen = true },
                    onCreate = viewModel::openCreateForm,
                    onDelete = viewModel::requestDelete,
                    onPage = viewModel::goToPage,
                )
                HrTab.APPLICATIONS -> HrDocumentsTab(kind = HrDocumentKind.APPLICATION)
                HrTab.ORDERS -> HrDocumentsTab(kind = HrDocumentKind.ORDER)
                HrTab.TIMESHEET -> HrTimesheetTab()
                HrTab.CALENDAR -> HrCalendarTab()
                HrTab.DICTIONARIES -> HrDictionariesTab()
            }
        }
    }

    if (state.formOpen) {
        HrEmployeeFormSheet(
            jobs = state.jobs,
            saving = state.formSaving,
            error = state.formError,
            onDismiss = viewModel::closeForm,
            onSave = viewModel::createEmployee,
        )
    }

    if (filtersOpen) {
        HrFiltersSheet(
            state = state,
            onJob = viewModel::setJobFilter,
            onType = viewModel::setTypeFilter,
            onStatus = viewModel::setStatusFilter,
            onSort = viewModel::setSort,
            onClear = viewModel::clearFilters,
            onDismiss = { filtersOpen = false },
        )
    }

    state.deleteTarget?.let { target ->
        AppSheetDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = {
                Text(
                    text = "İşçi silinsin?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                )
            },
            text = {
                Text(
                    text = "${employeeName(target)} silinəcək. Bu əməliyyat geri qaytarılmır.",
                    fontSize = 13.sp,
                    color = HrColors.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete, enabled = !state.deleting) {
                    Text(
                        text = if (state.deleting) "Silinir…" else "Sil",
                        color = HrColors.Error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Ləğv et", color = HrColors.Muted)
                }
            },
        )
    }
}

/* ------------------------------------------------------------------ */
/* İşçilər                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun EmployeesTab(
    state: HrViewModel.UiState,
    onOpenEmployee: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCreate: () -> Unit,
    onDelete: (HrEmployeeListItemDto) -> Unit,
    onPage: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.weight(1f).clickable(onClick = onOpenFilters),
                shape = RoundedCornerShape(10.dp),
                color = HrColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HrColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.FilterList, null, tint = HrColors.Muted, modifier = Modifier.size(16.dp))
                    Text(
                        text = filterSummary(state),
                        fontSize = 12.sp,
                        color = HrColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HrPrimaryButton(text = "+ Yeni işçi", onClick = onCreate)
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.employeesLoading && state.employees == null -> HrLoading()
            state.employeesError != null && state.employees == null ->
                HrErrorState(state.employeesError) { onPage(state.page) }
            else -> {
                val employees = state.employees.orEmpty()
                if (employees.isEmpty()) {
                    HrEmptyState(
                        title = if (state.query.isBlank()) "Hələ işçi yoxdur" else "Nəticə tapılmadı",
                        text = if (state.query.isBlank()) {
                            "İlk işçini əlavə etmək üçün “+ Yeni işçi” düyməsindən istifadə edin."
                        } else {
                            "Axtarışı və ya filtrləri dəyişin."
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(employees, key = { it.id }) { employee ->
                            EmployeeCard(
                                employee = employee,
                                onOpen = { onOpenEmployee(employee.id) },
                                onDelete = { onDelete(employee) },
                            )
                        }
                        item {
                            HrPaginationRow(
                                page = state.page,
                                pageCount = state.pageCount,
                                totalCount = state.totalCount,
                                onPage = onPage,
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(
    employee: HrEmployeeListItemDto,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val active = employee.isActive == 1
    HrCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (employee.gender == 2) HrColors.BlueSoft else HrColors.OliveSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(employee),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (employee.gender == 2) HrColors.Blue else HrColors.Olive,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employeeName(employee),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(employee.job?.name ?: "Vəzifə yoxdur")
                        employee.registerNumber?.let { append(" · Tabel №$it") }
                    },
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HrTag(
                text = if (active) "Aktiv" else "Qeyri-aktiv",
                foreground = if (active) HrColors.Olive else HrColors.Error,
                background = if (active) HrColors.OliveSoft else HrColors.ErrorSoft,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.phoneNumber ?: "Telefon yoxdur",
                    fontSize = 12.sp,
                    color = HrColors.Muted,
                    maxLines = 1,
                )
                Text(
                    text = "FİN: ${employee.finCode ?: "—"}",
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = HrEnums.label(HrEnums.EMPLOYMENT_TYPES, employee.employmentType),
                    fontSize = 11.sp,
                    color = HrColors.Muted,
                )
                Text(
                    text = "${Format.date(employee.startWorkDate)} · ${tenure(employee.startWorkDate)}",
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Kartı aç",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrColors.Terracotta,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Sil", tint = HrColors.Error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HrFiltersSheet(
    state: HrViewModel.UiState,
    onJob: (az.mtk.app.data.api.HrJobDto?) -> Unit,
    onType: (Int?) -> Unit,
    onStatus: (Int?) -> Unit,
    onSort: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    az.mtk.app.ui.components.AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle("Filtrlər", onDismiss)
        Spacer(Modifier.height(8.dp))

        HrSelectField(
            label = "Sıralama",
            value = state.sortIndex.toString(),
            options = HR_SORTS.mapIndexed { index, pair -> index.toString() to pair.first },
            onSelect = { onSort(it.toIntOrNull() ?: 0) },
        )
        Spacer(Modifier.height(10.dp))

        HrSelectField(
            label = "Vəzifə",
            value = state.jobFilter?.id.orEmpty(),
            options = listOf("" to "Hamısı") + state.jobs.map { it.id to it.name },
            onSelect = { id -> onJob(state.jobs.firstOrNull { it.id == id }) },
        )
        Spacer(Modifier.height(10.dp))

        HrSelectField(
            label = "İş rejimi",
            value = state.typeFilter?.toString().orEmpty(),
            options = listOf("" to "Hamısı") + HrEnums.EMPLOYMENT_TYPES.map { it.value.toString() to it.label },
            onSelect = { onType(it.toIntOrNull()) },
        )
        Spacer(Modifier.height(10.dp))

        HrSelectField(
            label = "Status",
            value = state.statusFilter?.toString().orEmpty(),
            options = listOf("" to "Hamısı") + HrEnums.EMPLOYEE_STATUSES.map { it.value.toString() to it.label },
            onSelect = { onStatus(it.toIntOrNull()) },
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HrSecondaryButton(text = "Təmizlə", onClick = { onClear(); onDismiss() })
            HrPrimaryButton(text = "Tətbiq et", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Köməkçi funksiyalar                                                */
/* ------------------------------------------------------------------ */

internal fun employeeName(employee: HrEmployeeListItemDto): String =
    Format.person(employee.name, employee.surname, employee.fathersName)

internal fun employeeName(employee: az.mtk.app.data.api.HrEmployeeDetailDto): String =
    Format.person(employee.name, employee.surname, employee.fathersName)

internal fun initials(employee: HrEmployeeListItemDto): String =
    Format.initials("${employee.surname} ${employee.name}")

internal fun tenure(startIso: String?): String {
    val raw = startIso?.take(10) ?: return "—"
    val start = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return "—"
    val now = LocalDate.now()
    if (start.isAfter(now)) return "—"
    val months = ChronoUnit.MONTHS.between(start, now).toInt()
    if (months < 1) return "1 aydan az"
    val years = months / 12
    val rest = months % 12
    return buildList {
        if (years > 0) add("$years il")
        if (rest > 0) add("$rest ay")
    }.joinToString(" ")
}

private fun filterSummary(state: HrViewModel.UiState): String {
    val parts = buildList {
        state.jobFilter?.let { add(it.name) }
        state.typeFilter?.let { add(HrEnums.label(HrEnums.EMPLOYMENT_TYPES, it)) }
        state.statusFilter?.let { add(HrEnums.label(HrEnums.EMPLOYEE_STATUSES, it)) }
    }
    return if (parts.isEmpty()) {
        "Filtr yoxdur · ${HR_SORTS.getOrElse(state.sortIndex) { HR_SORTS[0] }.first}"
    } else {
        parts.joinToString(" · ")
    }
}

internal fun hrOptionPairs(options: List<HrOption>, allLabel: String = "Hamısı"): List<Pair<String, String>> =
    listOf("" to allLabel) + options.map { it.value.toString() to it.label }

@Suppress("unused")
private val unusedColor = Color.White
