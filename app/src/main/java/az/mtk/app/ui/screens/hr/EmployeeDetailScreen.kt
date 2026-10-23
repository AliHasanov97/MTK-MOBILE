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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.HrEducationHistoryDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrVacationBalanceDto
import az.mtk.app.data.api.HrWorkHistoryDto
import az.mtk.app.data.api.WorkExperienceDto
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format

/**
 * İşçi kartı — MTK-UI `hr/isciler/[id]` səhifəsinin mobil qarşılığı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onBack: () -> Unit,
    viewModel: EmployeeDetailViewModel = mtkViewModel { EmployeeDetailViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(employeeId) { viewModel.load(employeeId) }

    val employee = state.employee

    Scaffold(
        containerColor = HrColors.Background,
        topBar = {
            HrTopBar(
                title = "İşçi kartı",
                subtitle = employee?.let {
                    "${Format.person(it.name, it.surname, it.fathersName)} · ${it.job?.name ?: "Vəzifə yoxdur"}"
                },
                onBack = onBack,
                onRefresh = { viewModel.refresh(employeeId) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(HrColors.Background),
        ) {
            when {
                state.loading && employee == null -> HrLoading()
                state.error != null && employee == null -> HrErrorState(state.error!!) { viewModel.load(employeeId) }
                employee == null -> HrLoading()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) {
                        item {
                            EmployeeProfileCard(employee)
                            Spacer(Modifier.height(10.dp))
                            HrPillRow(
                                modifier = Modifier.padding(horizontal = 0.dp),
                                pills = EmployeeDetailTab.entries.map { tab ->
                                    HrPill(tab.label, state.tab == tab) { viewModel.selectTab(employeeId, tab) }
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            state.actionMessage?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearFeedback() } }
                            state.actionError?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearFeedback() } }
                            Spacer(Modifier.height(4.dp))
                        }

                        when (state.tab) {
                            EmployeeDetailTab.INFO -> item { InfoTab(employee, viewModel) }
                            EmployeeDetailTab.SCHEDULE -> item { ScheduleTab(state, viewModel, employeeId) }
                            EmployeeDetailTab.WORK -> item {
                                WorkHistoryTab(
                                    state = state,
                                    onAdd = viewModel::openAddWork,
                                    onDelete = { viewModel.deleteWorkHistory(employeeId, it.id) },
                                )
                            }
                            EmployeeDetailTab.EDUCATION -> item {
                                EducationTab(
                                    state = state,
                                    onAdd = viewModel::openAddEducation,
                                    onDelete = { viewModel.deleteEducation(employeeId, it.id) },
                                )
                            }
                            EmployeeDetailTab.VACATION -> item {
                                VacationTab(state, viewModel, employeeId)
                            }
                            EmployeeDetailTab.DOCUMENTS -> item {
                                Box(modifier = Modifier.fillMaxWidth().height(560.dp)) {
                                    HrEmployeeDocumentsTab(employeeId)
                                }
                            }
                            EmployeeDetailTab.PAYMENTS -> item {
                                Box(modifier = Modifier.fillMaxWidth().height(620.dp)) {
                                    EmployeePaymentsSection(
                                        employeeId = employeeId,
                                        employeeName = employee.let {
                                            Format.person(it.name, it.surname, it.fathersName)
                                        },
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    val section = state.editSection
    if (section != null) {
        EditSectionSheet(
            section = section,
            form = state.editForm,
            saving = state.saving,
            error = state.saveError,
            onChange = viewModel::updateEditForm,
            onSave = { viewModel.saveSection(employeeId) },
            onDismiss = viewModel::closeEdit,
        )
    }

    if (state.addWorkOpen) {
        AddWorkSheet(
            saving = state.workSaving,
            error = state.addWorkError,
            onSave = { company, position, start, end, notes ->
                viewModel.addWorkHistory(employeeId, company, position, start, end, notes)
            },
            onDismiss = viewModel::closeAddWork,
        )
    }

    if (state.addEducationOpen) {
        AddEducationSheet(
            state = state,
            onSave = { institution, level, faculty, specialty, start, end, diploma, register ->
                viewModel.addEducation(
                    employeeId = employeeId,
                    institutionId = institution,
                    level = level,
                    faculty = faculty,
                    specialty = specialty,
                    startDate = start,
                    endDate = end,
                    diplomaNumber = diploma,
                    registerNumber = register,
                )
            },
            onDismiss = viewModel::closeAddEducation,
        )
    }
}

/* ------------------------------ Profil kartı ------------------------------ */

@Composable
private fun EmployeeProfileCard(employee: az.mtk.app.data.api.HrEmployeeDetailDto) {
    val active = employee.isActive == 1
    HrCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (employee.gender == 2) HrColors.BlueSoft else HrColors.OliveSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = Format.initials("${employee.surname} ${employee.name}"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (employee.gender == 2) HrColors.Blue else HrColors.Olive,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Format.person(employee.name, employee.surname, employee.fathersName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${employee.job?.name ?: "Vəzifə təyin edilməyib"} · Tabel №${employee.registerNumber ?: "—"}",
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                )
            }
            HrTag(
                text = if (active) "Aktiv" else "Qeyri-aktiv",
                foreground = if (active) HrColors.Olive else HrColors.Error,
                background = if (active) HrColors.OliveSoft else HrColors.ErrorSoft,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStat("İşə başlama", Format.date(employee.startWorkDate), Modifier.weight(1f))
            ProfileStat("Bu təşkilatda", experienceText(employee.organizationWorkExperience), Modifier.weight(1f))
            ProfileStat(
                "İş rejimi",
                HrEnums.label(HrEnums.EMPLOYMENT_TYPES, employee.employmentType),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(HrColors.Subtle)
            .padding(8.dp),
    ) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink, maxLines = 1)
        Text(label, fontSize = 9.sp, color = HrColors.MutedSoft, maxLines = 1)
    }
}

private fun experienceText(value: WorkExperienceDto?): String {
    if (value == null) return "—"
    return buildList {
        value.years?.takeIf { it > 0 }?.let { add("$it il") }
        value.months?.takeIf { it > 0 }?.let { add("$it ay") }
    }.joinToString(" ").ifBlank { "—" }
}

/* ------------------------------ Məlumatlar ------------------------------ */

@Composable
private fun InfoTab(employee: az.mtk.app.data.api.HrEmployeeDetailDto, viewModel: EmployeeDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard("Şəxsi məlumatlar", { viewModel.openEdit(HrEmployeeSection.PERSONAL) }) {
            HrKeyValueGrid(
                listOf(
                    "Cins" to HrEnums.label(HrEnums.GENDERS, employee.gender),
                    "Doğum tarixi" to Format.date(employee.birthDate),
                    "Milliyyət" to employee.nationality,
                    "FİN kod" to employee.finCode,
                    "Şəxsiyyət vəsiqəsi" to employee.idCardNumber,
                    "SSN" to employee.socialSecurityNumber,
                ),
            )
        }

        SectionCard("Ailə və sosial vəziyyət", { viewModel.openEdit(HrEmployeeSection.FAMILY) }) {
            HrKeyValueGrid(
                listOf(
                    "Ailə vəziyyəti" to HrEnums.label(HrEnums.MARITAL_STATUSES, employee.maritalStatus),
                    "Uşaqların sayı" to employee.numberOfChildren?.toString(),
                    "14 yaşadək uşaqlar" to employee.childrenUnder14Count?.toString(),
                    "Hərbi xidmət" to HrEnums.label(HrEnums.MILITARY_SERVICES, employee.militaryService),
                    "Təhsil" to HrEnums.label(HrEnums.EDUCATION_LEVELS, employee.education),
                    "Veteran" to Format.yesNo(employee.veteran),
                    "Əlillik" to Format.yesNo(employee.disability),
                    "Qarabağ və azad olunmuş ərazilərdə" to Format.yesNo(employee.isKarabakhWorker),
                    "Tək valideyn" to Format.yesNo(employee.isSingleParent),
                    "Əlil uşağı var" to Format.yesNo(employee.hasDisabledChild),
                ),
            )
        }

        SectionCard("Əlaqə", { viewModel.openEdit(HrEmployeeSection.CONTACT) }) {
            HrKeyValueGrid(
                listOf(
                    "Mobil telefon" to employee.phoneNumber,
                    "Ev telefonu" to employee.homePhoneNumber,
                    "E-poçt" to employee.email,
                    "Qeydiyyat ünvanı" to employee.registeredAddress,
                    "Faktiki ünvan" to employee.currentAddress,
                ),
            )
        }

        SectionCard("İş məlumatları", { viewModel.openEdit(HrEmployeeSection.WORK) }) {
            HrKeyValueGrid(
                listOf(
                    "Vəzifə" to employee.job?.name,
                    "Tabel №" to employee.registerNumber?.toString(),
                    "İşə başlama" to Format.date(employee.startWorkDate),
                    "İş rejimi" to HrEnums.label(HrEnums.EMPLOYMENT_TYPES, employee.employmentType),
                    "İş həftəsi" to HrEnums.label(HrEnums.WORKING_DAYS, employee.workingDays),
                    "İllik məzuniyyət" to employee.vacationDays?.let { "$it gün" },
                    "Status" to HrEnums.label(HrEnums.EMPLOYEE_STATUSES, employee.isActive),
                ),
            )
        }

        SectionCard("Müqavilə və bank", { viewModel.openEdit(HrEmployeeSection.BANK) }) {
            HrKeyValueGrid(
                listOf(
                    "Müqavilə №" to employee.contractNumber,
                    "Əsas əmək haqqı" to Format.money(employee.baseSalary),
                    "Maaş bankı" to employee.salaryBankName,
                    "Bank hesabı" to employee.employeeBankAccountNumber,
                ),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, onEdit: () -> Unit, content: @Composable () -> Unit) {
    HrCard {
        HrSectionHeader(title = title, actionLabel = "Redaktə et", onAction = onEdit)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/* ------------------------------ İş qrafiki ------------------------------ */

@Composable
private fun ScheduleTab(
    state: EmployeeDetailViewModel.UiState,
    viewModel: EmployeeDetailViewModel,
    employeeId: String,
) {
    var sixDays by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HrCard {
            HrSectionHeader("Həftəlik iş qrafiki")
            Text(
                text = buildString {
                    append("Hər gün üçün iş saatını təyin edin, istirahət günlərini söndürün.")
                    state.effectiveFrom?.let { append(" Cari qrafik ${Format.date(it)} tarixindən qüvvədədir.") }
                },
                fontSize = 11.sp,
                color = HrColors.MutedSoft,
                modifier = Modifier.padding(vertical = 6.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HrSecondaryButton("5 günlük şablon", onClick = { sixDays = false; viewModel.applySchedulePreset(false) })
                HrSecondaryButton("6 günlük şablon", onClick = { sixDays = true; viewModel.applySchedulePreset(true) })
            }

            when {
                state.scheduleLoading && state.hours.isEmpty() -> HrLoading()
                state.scheduleError != null && state.hours.isEmpty() ->
                    HrErrorState(state.scheduleError) { viewModel.refresh(employeeId) }
                else -> {
                    Spacer(Modifier.height(8.dp))
                    HR_SCHEDULE_DAYS.forEach { (key, label) ->
                        val raw = state.hours[key].orEmpty()
                        val on = raw.isNotBlank()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = on,
                                onCheckedChange = { checked ->
                                    viewModel.setHour(key, if (checked) "8" else "")
                                },
                                colors = CheckboxDefaults.colors(checkedColor = HrColors.Terracotta),
                            )
                            Text(label, fontSize = 13.sp, color = HrColors.Ink, modifier = Modifier.weight(1f))
                            if (on) {
                                HrTextField(
                                    label = "",
                                    value = raw,
                                    onValueChange = { value ->
                                        viewModel.setHour(key, value.filter { it.isDigit() || it == '.' || it == ',' })
                                    },
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.width(92.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("saat", fontSize = 10.sp, color = HrColors.MutedSoft)
                            } else {
                                Text("İstirahət", fontSize = 11.sp, color = HrColors.MutedSoft)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            val workDays = state.hours.values.count { it.isNotBlank() }
            val total = state.hours.values.mapNotNull { it.replace(',', '.').toDoubleOrNull() }.sum()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("İş günü", workDays.toString(), Modifier.weight(1f))
                ProfileStat("Həftəlik saat", Format.number(total), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.scheduleSaved) {
                    Text("Qrafik saxlanıldı ✓", fontSize = 11.sp, color = HrColors.Olive)
                }
                HrPrimaryButton(
                    text = if (state.scheduleSaving) "Saxlanılır…" else "Qrafiki yadda saxla",
                    onClick = { viewModel.saveSchedule(employeeId) },
                    enabled = !state.scheduleSaving,
                )
            }
            state.scheduleError?.takeIf { state.hours.isNotEmpty() }?.let {
                Text(it, fontSize = 11.sp, color = HrColors.Error, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/* ------------------------------ İş tarixçəsi ------------------------------ */

@Composable
private fun WorkHistoryTab(
    state: EmployeeDetailViewModel.UiState,
    onAdd: () -> Unit,
    onDelete: (HrWorkHistoryDto) -> Unit,
) {
    var pending by remember { mutableStateOf<HrWorkHistoryDto?>(null) }

    HrCard {
        HrSectionHeader("Əvvəlki iş yerləri", "＋ İş yeri əlavə et", onAction = onAdd)
        Text(
            text = "Burada daxil edilən müddətlər ümumi iş stajının hesablanmasına daxil olur.",
            fontSize = 11.sp,
            color = HrColors.MutedSoft,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        state.workError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(bottom = 6.dp))
        }
        when {
            state.workHistories == null -> HrLoading()
            state.workHistories.isEmpty() -> Text(
                "Əvvəlki iş yeri qeydə alınmayıb.",
                fontSize = 12.sp,
                color = HrColors.MutedSoft,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            else -> state.workHistories.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.companyName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink)
                        Text(item.position, fontSize = 11.sp, color = HrColors.Muted)
                        Text(
                            text = "${Format.date(item.startDate)} – ${item.endDate?.let { Format.date(it) } ?: "davam edir"}",
                            fontSize = 10.sp,
                            color = HrColors.MutedSoft,
                        )
                        Text(
                            text = experienceText(item.duration),
                            fontSize = 10.sp,
                            color = HrColors.Olive,
                        )
                        item.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontSize = 11.sp, color = HrColors.Muted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    IconButton(onClick = { pending = item }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, "Sil", tint = HrColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    pending?.let { item ->
        AppSheetDialog(
            onDismissRequest = { pending = null },
            title = { Text("Qeyd silinsin?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink) },
            text = { Text("«${item.companyName}» iş yeri qeydi silinəcək.", fontSize = 13.sp, color = HrColors.Muted) },
            confirmButton = {
                TextButton(onClick = { pending = null; onDelete(item) }) {
                    Text("Sil", color = HrColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Ləğv et", color = HrColors.Muted) }
            },
        )
    }
}

/* ------------------------------ Təhsil ------------------------------ */

@Composable
private fun EducationTab(
    state: EmployeeDetailViewModel.UiState,
    onAdd: () -> Unit,
    onDelete: (HrEducationHistoryDto) -> Unit,
) {
    var pending by remember { mutableStateOf<HrEducationHistoryDto?>(null) }

    HrCard {
        HrSectionHeader("Təhsil tarixçəsi", "＋ Təhsil əlavə et", onAction = onAdd)
        state.educationError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(bottom = 6.dp))
        }
        when {
            state.education == null -> HrLoading()
            state.education.isEmpty() -> Text(
                "Təhsil qeydi yoxdur.",
                fontSize = 12.sp,
                color = HrColors.MutedSoft,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            else -> state.education.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.educationalInstitutionName ?: "Təhsil ocağı",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HrColors.Ink,
                        )
                        Text(
                            text = listOfNotNull(
                                HrEnums.label(HrEnums.EDUCATION_LEVELS, item.educationLevel),
                                item.faculty,
                                item.specialty,
                            ).joinToString(" · "),
                            fontSize = 11.sp,
                            color = HrColors.Muted,
                        )
                        Text(
                            text = "${Format.date(item.startDate)} – ${item.endDate?.let { Format.date(it) } ?: "davam edir"}",
                            fontSize = 10.sp,
                            color = HrColors.MutedSoft,
                        )
                        if (item.diplomaNumber != null) {
                            Text("Diplom: ${item.diplomaNumber}", fontSize = 10.sp, color = HrColors.MutedSoft)
                        }
                    }
                    IconButton(onClick = { pending = item }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, "Sil", tint = HrColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    pending?.let { item ->
        AppSheetDialog(
            onDismissRequest = { pending = null },
            title = { Text("Qeyd silinsin?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink) },
            text = {
                Text(
                    "«${item.educationalInstitutionName ?: "Təhsil qeydi"}» silinəcək.",
                    fontSize = 13.sp,
                    color = HrColors.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { pending = null; onDelete(item) }) {
                    Text("Sil", color = HrColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Ləğv et", color = HrColors.Muted) }
            },
        )
    }
}

/* ------------------------------ Məzuniyyət balansı ------------------------------ */

@Composable
private fun VacationTab(
    state: EmployeeDetailViewModel.UiState,
    viewModel: EmployeeDetailViewModel,
    employeeId: String,
) {
    val balances = state.balances.orEmpty()
    val totalEntitled = balances.sumOf { it.totalEntitledDays ?: 0 }
    val totalUsed = balances.sumOf { it.usedDays ?: 0 }
    val totalComp = balances.sumOf { it.compensatedDays ?: 0 }
    val totalRemaining = balances.sumOf { it.remainingDays ?: 0 }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HrCard {
            HrSectionHeader(
                title = "Məzuniyyət balansı",
                actionLabel = if (state.recalculating) "Hesablanır…" else "Yenidən hesabla",
                actionEnabled = !state.recalculating,
                onAction = { viewModel.recalculateBalance(employeeId) },
            )
            state.balanceNotice?.let {
                Text(it, fontSize = 11.sp, color = HrColors.Olive, modifier = Modifier.padding(top = 4.dp))
            }
            state.balanceError?.let {
                Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 4.dp))
            }

            when {
                state.balanceLoading -> HrLoading()
                balances.isEmpty() -> Text(
                    "Məzuniyyət balansı yoxdur.",
                    fontSize = 12.sp,
                    color = HrColors.MutedSoft,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                else -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileStat("Ümumi hüquq", totalEntitled.toString(), Modifier.weight(1f))
                        ProfileStat("İstifadə", totalUsed.toString(), Modifier.weight(1f))
                        ProfileStat("Kompensasiya", totalComp.toString(), Modifier.weight(1f))
                        ProfileStat("Qalıq", totalRemaining.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "İş illəri üzrə (${balances.size} il)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HrColors.Ink,
                    )
                    balances.sortedByDescending { it.workYearStart }.forEach { balance ->
                        BalanceCard(balance)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: HrVacationBalanceDto) {
    val current = runCatching {
        val start = java.time.LocalDate.parse(balance.workYearStart.take(10))
        val end = java.time.LocalDate.parse(balance.workYearEnd.take(10))
        val today = java.time.LocalDate.now()
        !today.isBefore(start) && !today.isAfter(end)
    }.getOrDefault(false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (current) HrColors.OliveSoft else HrColors.Subtle)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${Format.date(balance.workYearStart)} – ${Format.date(balance.workYearEnd)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
                modifier = Modifier.weight(1f),
            )
            if (current) HrTag("Cari il", HrColors.Olive, HrColors.Surface)
        }
        Spacer(Modifier.height(6.dp))
        HrKeyValueGrid(
            listOf(
                "Əsas" to (balance.baseEntitledDays?.toString() ?: "—"),
                "Staj bonusu" to (balance.experienceBonusDays?.toString() ?: "—"),
                "Qarabağ" to (balance.karabakhWorkerDays?.toString() ?: "—"),
                "Uşaq" to (balance.childrenUnder14Days?.toString() ?: "—"),
                "Əlil uşaq" to (balance.disabledChildDays?.toString() ?: "—"),
                "İl üzrə cəmi" to (balance.totalEntitledDays?.toString() ?: "—"),
                "İstifadə" to (balance.usedDays?.toString() ?: "—"),
                "Kompensasiya" to (balance.compensatedDays?.toString() ?: "—"),
                "Qalıq" to (balance.remainingDays?.toString() ?: "—"),
            ),
            columns = 3,
        )
    }
}

/* ------------------------------ Bölmə redaktəsi ------------------------------ */

@Composable
private fun EditSectionSheet(
    section: HrEmployeeSection,
    form: HrEmployeeForm,
    saving: Boolean,
    error: String?,
    onChange: (HrEmployeeForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle(section.title, onDismiss)

        when (section) {
            HrEmployeeSection.PERSONAL -> {
                HrTextField("Soyad", form.surname) { onChange(form.copy(surname = it)) }
                HrTextField("Ad", form.name) { onChange(form.copy(name = it)) }
                HrTextField("Ata adı", form.fathersName) { onChange(form.copy(fathersName = it)) }
                HrSelectField(
                    label = "Cins",
                    value = form.gender.toString(),
                    options = HrEnums.GENDERS.map { it.value.toString() to it.label },
                    onSelect = { onChange(form.copy(gender = it.toIntOrNull() ?: 1)) },
                )
                HrDateField("Doğum tarixi", form.birthDate) { onChange(form.copy(birthDate = it)) }
                HrTextField("Milliyyət", form.nationality) { onChange(form.copy(nationality = it)) }
                HrTextField("FİN kod", form.finCode) { onChange(form.copy(finCode = it)) }
                HrTextField("Şəxsiyyət vəsiqəsi", form.idCardNumber) { onChange(form.copy(idCardNumber = it)) }
                HrTextField("SSN", form.socialSecurityNumber) { onChange(form.copy(socialSecurityNumber = it)) }
            }
            HrEmployeeSection.FAMILY -> {
                HrSelectField(
                    label = "Ailə vəziyyəti",
                    value = form.maritalStatus,
                    options = hrOptionPairs(HrEnums.MARITAL_STATUSES, "—"),
                    onSelect = { onChange(form.copy(maritalStatus = it)) },
                )
                HrTextField("Uşaqların sayı", form.numberOfChildren) { onChange(form.copy(numberOfChildren = it)) }
                HrTextField("14 yaşadək uşaqlar", form.childrenUnder14Count) {
                    onChange(form.copy(childrenUnder14Count = it))
                }
                HrSelectField(
                    label = "Hərbi xidmət",
                    value = form.militaryService,
                    options = hrOptionPairs(HrEnums.MILITARY_SERVICES, "—"),
                    onSelect = { onChange(form.copy(militaryService = it)) },
                )
                HrSelectField(
                    label = "Təhsil",
                    value = form.education,
                    options = hrOptionPairs(HrEnums.EDUCATION_LEVELS, "—"),
                    onSelect = { onChange(form.copy(education = it)) },
                )
                FormCheck("Veteran", form.veteran) { onChange(form.copy(veteran = it)) }
                FormCheck("Əlillik", form.disability) { onChange(form.copy(disability = it)) }
                FormCheck("Qarabağ və azad olunmuş ərazilərdə", form.isKarabakhWorker) {
                    onChange(form.copy(isKarabakhWorker = it))
                }
                FormCheck("Tək valideyn", form.isSingleParent) { onChange(form.copy(isSingleParent = it)) }
                FormCheck("Əlil uşağı var", form.hasDisabledChild) { onChange(form.copy(hasDisabledChild = it)) }
            }
            HrEmployeeSection.CONTACT -> {
                HrTextField("Mobil telefon", form.phoneNumber) { onChange(form.copy(phoneNumber = it)) }
                HrTextField("Ev telefonu", form.homePhoneNumber) { onChange(form.copy(homePhoneNumber = it)) }
                HrTextField("E-poçt", form.email) { onChange(form.copy(email = it)) }
                HrTextField("Qeydiyyat ünvanı", form.registeredAddress) {
                    onChange(form.copy(registeredAddress = it))
                }
                HrTextField("Faktiki ünvan", form.currentAddress) { onChange(form.copy(currentAddress = it)) }
            }
            HrEmployeeSection.WORK -> {
                Text(
                    "Vəzifə dəyişikliyi müvafiq ərizə və əmr vasitəsilə aparılır.",
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                HrSelectField(
                    label = "İş rejimi",
                    value = form.employmentType.toString(),
                    options = HrEnums.EMPLOYMENT_TYPES.map { it.value.toString() to it.label },
                    onSelect = { onChange(form.copy(employmentType = it.toIntOrNull() ?: 1)) },
                )
                HrSelectField(
                    label = "İş həftəsi",
                    value = form.workingDays,
                    options = HrEnums.WORKING_DAYS.map { it.value.toString() to it.label },
                    onSelect = { onChange(form.copy(workingDays = it)) },
                )
                HrTextField("İllik əsas məzuniyyət (gün)", form.vacationDays) {
                    onChange(form.copy(vacationDays = it.filter(Char::isDigit)))
                }
            }
            HrEmployeeSection.BANK -> {
                HrTextField("Əmək müqaviləsinin nömrəsi", form.contractNumber) {
                    onChange(form.copy(contractNumber = it))
                }
                HrTextField("Əsas əmək haqqı (₼)", form.baseSalary, keyboardType = KeyboardType.Decimal) {
                    onChange(form.copy(baseSalary = it))
                }
                HrTextField("Maaş bankı", form.salaryBankName) { onChange(form.copy(salaryBankName = it)) }
                HrTextField("Bank hesabı", form.employeeBankAccountNumber) {
                    onChange(form.copy(employeeBankAccountNumber = it))
                }
            }
        }

        error?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton("Ləğv et", onDismiss, enabled = !saving)
            HrPrimaryButton(
                text = if (saving) "Saxlanılır…" else "Yadda saxla",
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

/* ------------------------------ İş yeri əlavə et ------------------------------ */

@Composable
private fun AddWorkSheet(
    saving: Boolean,
    error: String?,
    onSave: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var company by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle("Əvvəlki iş yeri", onDismiss)
        HrTextField("Təşkilat", company) { company = it }
        HrTextField("Vəzifə", position) { position = it }
        HrDateField("Başlanğıc tarixi", start) { start = it }
        HrDateField("Bitmə tarixi", end) { end = it }
        HrTextField("Qeyd", notes, singleLine = false) { notes = it }
        error?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton("Ləğv et", onDismiss, enabled = !saving)
            HrPrimaryButton(
                text = if (saving) "Saxlanılır…" else "Əlavə et",
                onClick = { onSave(company, position, start, end, notes) },
                enabled = !saving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

/* ------------------------------ Təhsil əlavə et ------------------------------ */

@Composable
private fun AddEducationSheet(
    state: EmployeeDetailViewModel.UiState,
    onSave: (String, Int, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var institution by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("1") }
    var faculty by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var diploma by remember { mutableStateOf("") }
    var register by remember { mutableStateOf("") }

    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle("Təhsil qeydi", onDismiss)
        HrSelectField(
            label = "Təhsil ocağı",
            value = institution,
            options = listOf("" to "Seçin…") + state.institutions.map { it.id to it.name },
            onSelect = { institution = it },
        )
        HrSelectField(
            label = "Təhsil səviyyəsi",
            value = level,
            options = HrEnums.EDUCATION_LEVELS.map { it.value.toString() to it.label },
            onSelect = { level = it },
        )
        HrTextField("Fakültə", faculty) { faculty = it }
        HrTextField("İxtisas", specialty) { specialty = it }
        HrDateField("Başlanğıc tarixi", start) { start = it }
        HrDateField("Bitmə tarixi", end) { end = it }
        HrTextField("Diplom №", diploma) { diploma = it }
        HrTextField("Qeydiyyat №", register) { register = it }
        state.addEducationError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton("Ləğv et", onDismiss, enabled = !state.educationSaving)
            HrPrimaryButton(
                text = if (state.educationSaving) "Saxlanılır…" else "Əlavə et",
                onClick = {
                    onSave(
                        institution,
                        level.toIntOrNull() ?: 1,
                        faculty,
                        specialty,
                        start,
                        end,
                        diploma,
                        register,
                    )
                },
                enabled = !state.educationSaving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
