package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrApplicationKind
import az.mtk.app.data.api.HrEducationHistoryDto
import az.mtk.app.data.api.HrEducationHistoryBody
import az.mtk.app.data.api.HrEmployeeDetailDto
import az.mtk.app.data.api.HrOrderKind
import az.mtk.app.data.api.HrVacationBalanceDto
import az.mtk.app.data.api.HrWorkHistoryBody
import az.mtk.app.data.api.HrWorkHistoryDto
import az.mtk.app.data.api.HrWorkScheduleDto
import az.mtk.app.util.toUserMessage
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
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
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EmployeeDetailState(
    val employee: HrEmployeeDetailDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class EmployeeDetailViewModel(private val employeeId: String) : ViewModel() {
    private val _state = MutableStateFlow(EmployeeDetailState())
    val state: StateFlow<EmployeeDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { HrApi.repository.employeeDetail(employeeId) }
                .onSuccess { _state.value = EmployeeDetailState(employee = it, loading = false) }
                .onFailure { _state.value = EmployeeDetailState(loading = false, error = it.toUserMessage()) }
        }
    }
}

private enum class HrDetailTab(val label: String) {
    INFO("Məlumatlar"),
    SCHEDULE("İş qrafiki"),
    WORK_HISTORY("İş tarixçəsi"),
    EDUCATION("Təhsil"),
    VACATION("Məzuniyyət balansı"),
    DOCUMENTS("Ərizə və əmrlər"),
}

private enum class EmployeeSection(val title: String) {
    PERSONAL("Şəxsi məlumatlar"),
    FAMILY("Ailə və sosial vəziyyət"),
    CONTACT("Əlaqə"),
    WORK("İş məlumatları"),
    BANK("Müqavilə və bank"),
}

@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onBack: () -> Unit,
    viewModel: EmployeeDetailViewModel = mtkViewModel { EmployeeDetailViewModel(employeeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(HrDetailTab.INFO) }
    var editSection by remember { mutableStateOf<EmployeeSection?>(null) }
    var showArchive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var archiveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.employee?.let { Format.person(it.name, it.surname, it.fathersName) } ?: "İşçi",
            subtitle = state.employee?.job?.name,
            onBack = onBack,
            action = {
                if (state.employee != null) {
                    TextButton(onClick = { showArchive = true }) {
                        Text("Arxivlə", color = MtkColors.Negative, style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
        )

        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
            else -> {
                val employee = state.employee ?: return@Column
                val scope2 = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    MtkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(
                                Format.initials(listOfNotNull(employee.name, employee.surname).joinToString(" ")),
                                size = 52.dp,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Format.person(employee.name, employee.surname, employee.fathersName),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "${employee.job?.name ?: "Vəzifə təyin edilməyib"} · Tabel №${employee.registerNumber ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                            Pill(
                                Format.isActiveLabel(employee.isActive),
                                if (employee.isActive == 1) Tone.Success else Tone.Neutral,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HrDetailTab.entries.forEach { t ->
                            HrTabChip(t.label, tab == t) { tab = t }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    when (tab) {
                        HrDetailTab.INFO -> InfoTab(employee, onEdit = { editSection = it })
                        HrDetailTab.SCHEDULE -> ScheduleTab(employeeId)
                        HrDetailTab.WORK_HISTORY -> WorkHistoryTab(employeeId)
                        HrDetailTab.EDUCATION -> EducationTab(employeeId)
                        HrDetailTab.VACATION -> VacationBalanceTab(employeeId)
                        HrDetailTab.DOCUMENTS -> DocumentsTab(employee, scope2)
                    }
                    Spacer(Modifier.height(28.dp))
                }

                archiveError?.let { message ->
                    MtkCard(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MtkColors.NegativeBg.copy(alpha = 0.4f),
                    ) {
                        Text(message, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { archiveError = null }) { Text("Bağla") }
                    }
                }
            }
        }
    }

    editSection?.let { section ->
        val employee = state.employee
        if (employee != null) {
            SectionEditModal(
                section = section,
                employee = employee,
                onDismiss = { editSection = null },
                onSaved = {
                    editSection = null
                    viewModel.load()
                },
            )
        }
    }

    if (showArchive && state.employee != null) {
        val employee = state.employee!!
        az.mtk.app.ui.components.ConfirmDialog(
            title = "İşçini arxivlə",
            message = "${Format.person(employee.name, employee.surname, employee.fathersName)} arxivləşdirilsin? Bu əməliyyat işçini qeyri-aktiv siyahıya keçirir.",
            confirmLabel = "Arxivlə",
            danger = true,
            onDismiss = { showArchive = false },
            onConfirm = {
                showArchive = false
                scope.launch {
                    runCatching { HrApi.repository.deleteEmployee(employee.id) }
                        .onSuccess { viewModel.load() }
                        .onFailure { archiveError = it.toUserMessage() }
                }
            },
        )
    }

    archiveError?.let { _ -> } // aşağıda göstərilir
}

// ------------------------------ Məlumatlar -----------------------------------

@Composable
private fun InfoSectionCard(title: String, onEdit: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
        )
        if (onEdit != null) {
            TextButton(onClick = onEdit) { Text("Redaktə et", fontSize = 12.sp) }
        }
    }
    MtkCard {
        content()
        // divider-lər InfoRow-lar arasında
    }
}

@Composable
private fun Rows(vararg rows: Pair<String, String>) {
    rows.forEachIndexed { index, (label, value) ->
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        InfoRow(label, value)
    }
}

@Composable
private fun InfoTab(employee: HrEmployeeDetailDto, onEdit: (EmployeeSection) -> Unit) {
    InfoSectionCard(EmployeeSection.PERSONAL.title, { onEdit(EmployeeSection.PERSONAL) }) {
        Rows(
            "Cins" to HrOptions.label(HrOptions.genders, employee.gender),
            "Doğum tarixi" to Format.date(employee.birthDate),
            "Milliyyət" to (employee.nationality ?: "—"),
            "FİN kod" to (employee.finCode ?: "—"),
            "Şəxsiyyət vəsiqəsi" to (employee.idCardNumber ?: "—"),
            "SSN" to (employee.socialSecurityNumber ?: "—"),
        )
    }

    InfoSectionCard(EmployeeSection.FAMILY.title, { onEdit(EmployeeSection.FAMILY) }) {
        Rows(
            "Ailə vəziyyəti" to HrOptions.label(HrOptions.maritalStatuses, employee.maritalStatus),
            "Uşaqların sayı" to (employee.numberOfChildren?.toString() ?: "—"),
            "14 yaşadək uşaqlar" to (employee.childrenUnder14Count?.toString() ?: "—"),
            "Hərbi xidmət" to HrOptions.label(HrOptions.militaryServices, employee.militaryService),
            "Təhsil" to HrOptions.label(HrOptions.educationLevels, employee.education),
            "Veteran" to Format.yesNo(employee.veteran),
            "Əlillik" to Format.yesNo(employee.disability),
            "Qarabağ və azad olunmuş ərazilərdə" to Format.yesNo(employee.isKarabakhWorker),
            "Tək valideyn" to Format.yesNo(employee.isSingleParent),
            "Əlil uşağı var" to Format.yesNo(employee.hasDisabledChild),
        )
    }

    InfoSectionCard(EmployeeSection.CONTACT.title, { onEdit(EmployeeSection.CONTACT) }) {
        Rows(
            "Mobil telefon" to (employee.phoneNumber ?: "—"),
            "Ev telefonu" to (employee.homePhoneNumber ?: "—"),
            "E-poçt" to (employee.email ?: "—"),
            "Qeydiyyat ünvanı" to (employee.registeredAddress ?: "—"),
            "Faktiki ünvan" to (employee.currentAddress ?: "—"),
        )
    }

    InfoSectionCard(EmployeeSection.WORK.title, { onEdit(EmployeeSection.WORK) }) {
        Rows(
            "Vəzifə" to (employee.job?.name ?: "—"),
            "İş rejimi" to Format.employmentType(employee.employmentType),
            "İş həftəsi" to HrOptions.label(HrOptions.workingDays, employee.workingDays),
            "İllik əsas məzuniyyət" to "${employee.vacationDays ?: "—"} gün",
            "Ümumi iş stajı" to Format.experience(employee.totalWorkExperience),
            "Bu təşkilatdakı staj" to Format.experience(employee.organizationWorkExperience),
        )
    }

    InfoSectionCard(EmployeeSection.BANK.title, { onEdit(EmployeeSection.BANK) }) {
        Rows(
            "Əmək müqaviləsi №" to (employee.contractNumber ?: "—"),
            "Əsas əmək haqqı" to Format.money(employee.baseSalary),
            "Maaş bankı" to (employee.salaryBankName ?: "—"),
            "Bank hesabı" to (employee.employeeBankAccountNumber ?: "—"),
            "Qeydə alan" to (employee.createdBy?.name ?: "—"),
        )
    }
}

// ------------------------------ Bölmə redaktəsi -------------------------------

private fun HrEmployeeDetailDto.toFormState(): HrEmployeeFormState = HrEmployeeFormState(
    name = name.orEmpty(),
    surname = surname.orEmpty(),
    fathersName = fathersName.orEmpty(),
    gender = (gender ?: 1).toString(),
    startWorkDate = startWorkDate?.take(10).orEmpty(),
    jobId = job?.id.orEmpty(),
    jobLabel = job?.name,
    nationality = nationality.orEmpty(),
    birthDate = birthDate?.take(10).orEmpty(),
    finCode = finCode.orEmpty(),
    idCardNumber = idCardNumber.orEmpty(),
    socialSecurityNumber = socialSecurityNumber.orEmpty(),
    contractNumber = contractNumber.orEmpty(),
    salaryBankName = salaryBankName.orEmpty(),
    employeeBankAccountNumber = employeeBankAccountNumber.orEmpty(),
    baseSalary = baseSalary?.toString().orEmpty(),
    maritalStatus = maritalStatus?.toString().orEmpty(),
    numberOfChildren = numberOfChildren?.toString().orEmpty(),
    childrenUnder14Count = childrenUnder14Count?.toString().orEmpty(),
    militaryService = militaryService?.toString().orEmpty(),
    veteran = veteran == true,
    disability = disability == true,
    isKarabakhWorker = isKarabakhWorker == true,
    isSingleParent = isSingleParent == true,
    hasDisabledChild = hasDisabledChild == true,
    education = education?.toString().orEmpty(),
    phoneNumber = phoneNumber.orEmpty(),
    homePhoneNumber = homePhoneNumber.orEmpty(),
    email = email.orEmpty(),
    registeredAddress = registeredAddress.orEmpty(),
    currentAddress = currentAddress.orEmpty(),
    workingDays = workingDays?.toString().orEmpty(),
    vacationDays = vacationDays?.toString().orEmpty(),
    employmentType = (employmentType ?: 1).toString(),
    isActive = (isActive ?: 1).toString(),
)

/** PATCH yalnız null-olmayan sahələri tətbiq edir — mətn boşdursa "" göndərilir, tarix/rəqəm boşdursa göndərilmir. */
private fun buildSectionPatch(section: EmployeeSection, f: HrEmployeeFormState): JsonObject {
    val body = JsonObject()
    fun text(key: String, value: String) { body.addProperty(key, value.trim()) }
    fun optNum(key: String, value: String) { value.trim().toDoubleOrNull()?.let { body.addProperty(key, it) } }
    fun optDate(key: String, value: String) {
        runCatching { toIsoDate(LocalDate.parse(value.trim())) }.getOrNull()?.let { body.addProperty(key, it) }
    }
    when (section) {
        EmployeeSection.PERSONAL -> {
            body.addProperty("gender", f.gender.toIntOrNull() ?: 1)
            text("nationality", f.nationality)
            optDate("birthDate", f.birthDate)
            text("finCode", f.finCode)
            text("idCardNumber", f.idCardNumber)
            text("socialSecurityNumber", f.socialSecurityNumber)
        }
        EmployeeSection.FAMILY -> {
            optNum("maritalStatus", f.maritalStatus)
            optNum("numberOfChildren", f.numberOfChildren)
            optNum("childrenUnder14Count", f.childrenUnder14Count)
            optNum("militaryService", f.militaryService)
            body.addProperty("veteran", f.veteran)
            body.addProperty("disability", f.disability)
            body.addProperty("isKarabakhWorker", f.isKarabakhWorker)
            body.addProperty("isSingleParent", f.isSingleParent)
            body.addProperty("hasDisabledChild", f.hasDisabledChild)
            optNum("education", f.education)
        }
        EmployeeSection.CONTACT -> {
            text("phoneNumber", f.phoneNumber)
            text("homePhoneNumber", f.homePhoneNumber)
            text("email", f.email)
            text("registeredAddress", f.registeredAddress)
            text("currentAddress", f.currentAddress)
        }
        EmployeeSection.WORK -> {
            if (f.jobId.isNotBlank()) body.addProperty("jobId", f.jobId)
            optDate("startWorkDate", f.startWorkDate)
            body.addProperty("employmentType", f.employmentType.toIntOrNull() ?: 1)
            optNum("workingDays", f.workingDays)
            optNum("vacationDays", f.vacationDays)
        }
        EmployeeSection.BANK -> {
            text("contractNumber", f.contractNumber)
            optNum("baseSalary", f.baseSalary)
            text("salaryBankName", f.salaryBankName)
            text("employeeBankAccountNumber", f.employeeBankAccountNumber)
        }
    }
    return body
}

@Composable
private fun SectionEditModal(
    section: EmployeeSection,
    employee: HrEmployeeDetailDto,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var f by remember(section) { mutableStateOf(employee.toFormState()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = section.title,
        subtitle = "Yalnız seçilmiş bölmə yenilənir",
        onDismiss = onDismiss,
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            saving = true
            error = null
            scope.launch {
                runCatching { HrApi.repository.updateEmployeeSection(employee.id, buildSectionPatch(section, f)) }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        when (section) {
            EmployeeSection.PERSONAL -> {
                MtkSelect(f.gender, { f = f.copy(gender = it) }, "Cins", HrOptions.genders, required = true)
                MtkTextField(f.nationality, { f = f.copy(nationality = it) }, "Milliyyət")
                HrOptionalDateField(f.birthDate.ifBlank { null }, { f = f.copy(birthDate = it) }, "Doğum tarixi")
                MtkTextField(f.finCode, { f = f.copy(finCode = it) }, "FİN kod")
                MtkTextField(f.idCardNumber, { f = f.copy(idCardNumber = it) }, "Şəxsiyyət vəsiqəsi №")
                MtkTextField(f.socialSecurityNumber, { f = f.copy(socialSecurityNumber = it) }, "SSN")
            }
            EmployeeSection.FAMILY -> {
                MtkSelect(f.maritalStatus.ifBlank { null }, { f = f.copy(maritalStatus = it ?: "") }, "Ailə vəziyyəti", HrOptions.maritalStatuses)
                MtkNumberField(f.numberOfChildren, { f = f.copy(numberOfChildren = it) }, "Uşaqların sayı")
                MtkNumberField(f.childrenUnder14Count, { f = f.copy(childrenUnder14Count = it) }, "14 yaşadək uşaqlar")
                MtkSelect(f.militaryService.ifBlank { null }, { f = f.copy(militaryService = it ?: "") }, "Hərbi xidmət", HrOptions.militaryServices)
                MtkSelect(f.education.ifBlank { null }, { f = f.copy(education = it ?: "") }, "Təhsil", HrOptions.educationLevels)
            }
            EmployeeSection.CONTACT -> {
                MtkTextField(f.phoneNumber, { f = f.copy(phoneNumber = it) }, "Mobil telefon")
                MtkTextField(f.homePhoneNumber, { f = f.copy(homePhoneNumber = it) }, "Ev telefonu")
                MtkTextField(f.email, { f = f.copy(email = it) }, "E-poçt")
                MtkTextField(f.registeredAddress, { f = f.copy(registeredAddress = it) }, "Qeydiyyat ünvanı")
                MtkTextField(f.currentAddress, { f = f.copy(currentAddress = it) }, "Faktiki ünvan")
            }
            EmployeeSection.WORK -> {
                SearchableSelect(
                    label = "Vəzifə",
                    required = true,
                    selectedLabel = f.jobLabel,
                    searchPlaceholder = "Vəzifə axtar…",
                    onLoad = { q -> HrApi.repository.jobs(0, 50, q).items },
                    itemLabel = { it.name },
                    onSelect = { f = f.copy(jobId = it.id, jobLabel = it.name) },
                    onClear = { f = f.copy(jobId = "", jobLabel = null) },
                )
                HrOptionalDateField(f.startWorkDate.ifBlank { null }, { f = f.copy(startWorkDate = it) }, "İşə başlama tarixi")
                MtkSelect(f.employmentType, { f = f.copy(employmentType = it) }, "İş rejimi", HrOptions.employmentTypes)
                MtkSelect(f.workingDays.ifBlank { null }, { f = f.copy(workingDays = it ?: "") }, "İş həftəsi", HrOptions.workingDays)
                MtkNumberField(f.vacationDays, { f = f.copy(vacationDays = it) }, "İllik əsas məzuniyyət (gün)")
            }
            EmployeeSection.BANK -> {
                MtkTextField(f.contractNumber, { f = f.copy(contractNumber = it) }, "Əmək müqaviləsi №")
                MtkNumberField(f.baseSalary, { f = f.copy(baseSalary = it) }, "Əsas əmək haqqı (₼)")
                MtkTextField(f.salaryBankName, { f = f.copy(salaryBankName = it) }, "Maaş bankı")
                MtkTextField(f.employeeBankAccountNumber, { f = f.copy(employeeBankAccountNumber = it) }, "Bank hesabı")
            }
        }
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ------------------------------ İş qrafiki ------------------------------------

private val SCHEDULE_DAYS = listOf(
    "monday" to "Bazar ertəsi",
    "tuesday" to "Çərşənbə axşamı",
    "wednesday" to "Çərşənbə",
    "thursday" to "Cümə axşamı",
    "friday" to "Cümə",
    "saturday" to "Şənbə",
    "sunday" to "Bazar",
)

@Composable
private fun ScheduleTab(employeeId: String) {
    val scope = rememberCoroutineScope()
    var hours by remember { mutableStateOf<Map<String, String>?>(null) }
    var effectiveFrom by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(employeeId) {
        runCatching { HrApi.repository.workSchedule(employeeId) }
            .onSuccess { schedule ->
                effectiveFrom = schedule.effectiveFrom
                hours = SCHEDULE_DAYS.associate { (key, _) -> key to (schedule.dayValue(key)?.let { v -> if (v == 0.0) "" else trimNum(v) } ?: "") }
            }
            .onFailure { error = it.toUserMessage() }
    }

    val current = hours
    if (current == null) {
        if (error != null) ErrorState(error!!, onRetry = { hours = null; error = null }) else LoadingState("Qrafik yüklənir…")
        return
    }

    val workDays = current.values.count { it.isNotBlank() }
    val totalHours = current.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    KeyValueHeader2("Həftəlik iş qrafiki")
    Text(
        "Hər gün üçün iş saatını təyin edin, istirahət günlərini söndürün." +
            (effectiveFrom?.let { " Cari qrafik ${Format.date(it)} tarixindən." } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MtkColors.Muted,
    )
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        listOf(
            "5 günlük · 8 saat" to mapOf("monday" to "8", "tuesday" to "8", "wednesday" to "8", "thursday" to "8", "friday" to "8"),
            "6 günlük · 8 saat" to mapOf("monday" to "8", "tuesday" to "8", "wednesday" to "8", "thursday" to "8", "friday" to "8", "saturday" to "8"),
            "Yarım ştat · 4 saat" to mapOf("monday" to "4", "tuesday" to "4", "wednesday" to "4", "thursday" to "4", "friday" to "4"),
        ).forEach { (label, preset) ->
            SecondaryButton(
                text = label,
                onClick = {
                    hours = SCHEDULE_DAYS.associate { (key, _) -> key to (preset[key] ?: "") }
                    saved = false
                },
                modifier = Modifier.width(150.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    MtkCard {
        SCHEDULE_DAYS.forEach { (key, label) ->
            val value = current[key].orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    hours = current + (key to if (value.isBlank()) "8" else "")
                    saved = false
                }) {
                    Text(if (value.isBlank()) "İş günü et" else "İstirahət", fontSize = 12.sp)
                }
                if (value.isNotBlank()) {
                    MtkNumberField(
                        value = value,
                        onValueChange = { hours = current + (key to it) },
                        label = "Saat",
                        modifier = Modifier.width(120.dp),
                    )
                } else {
                    Pill("İstirahət", Tone.Neutral, showDot = false)
                    Spacer(Modifier.width(24.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("$workDays iş günü · $totalHours həftəlik saat", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
    Spacer(Modifier.height(8.dp))
    if (saved) Text("Qrafik saxlanıldı ✓", style = MaterialTheme.typography.labelMedium, color = MtkColors.Positive)
    PrimaryButton(
        text = "Qrafiki yadda saxla",
        loading = saving,
        onClick = {
            saving = true
            error = null
            scope.launch {
                runCatching {
                    HrApi.repository.setWorkSchedule(
                        employeeId,
                        HrWorkScheduleDto(
                            monday = current["monday"]?.toDoubleOrNull(),
                            tuesday = current["tuesday"]?.toDoubleOrNull(),
                            wednesday = current["wednesday"]?.toDoubleOrNull(),
                            thursday = current["thursday"]?.toDoubleOrNull(),
                            friday = current["friday"]?.toDoubleOrNull(),
                            saturday = current["saturday"]?.toDoubleOrNull(),
                            sunday = current["sunday"]?.toDoubleOrNull(),
                        ),
                    )
                }
                    .onSuccess { saved = true }
                    .onFailure { error = it.toUserMessage() }
                saving = false
            }
        },
    )
    error?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
    }
}

private fun trimNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun HrWorkScheduleDto.dayValue(key: String): Double? = when (key) {
    "monday" -> monday
    "tuesday" -> tuesday
    "wednesday" -> wednesday
    "thursday" -> thursday
    "friday" -> friday
    "saturday" -> saturday
    "sunday" -> sunday
    else -> null
}

// ------------------------------ İş tarixçəsi -----------------------------------

@Composable
private fun WorkHistoryTab(employeeId: String) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<HrWorkHistoryDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HrWorkHistoryDto?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(employeeId, reloadKey) {
        runCatching { HrApi.repository.workHistories(employeeId) }
            .onSuccess { items = it.sortedByDescending { h -> h.startDate }; error = null }
            .onFailure { error = it.toUserMessage() }
    }

    SectionHeader("Əvvəlki iş yerləri", "+ İş yeri əlavə et") { showAdd = true }
    Text(
        "İşçinin bu təşkilatdan əvvəlki iş yerləri. Burada daxil edilən müddətlər ümumi iş stajının hesablanmasına daxil olur.",
        style = MaterialTheme.typography.bodySmall,
        color = MtkColors.Muted,
    )
    Spacer(Modifier.height(8.dp))

    when {
        error != null -> ErrorState(error!!, onRetry = { reloadKey++ })
        items == null -> LoadingState("Yüklənir…")
        items!!.isEmpty() -> EmptyState("Əvvəlki iş yeri qeydə alınmayıb")
        else -> items!!.forEach { h ->
            MtkCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(h.position, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${Format.date(h.startDate)} – ${h.endDate?.let { Format.date(it) } ?: "davam edir"} · ${Format.experience(h.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MtkColors.MutedSoft,
                        )
                        h.notes?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MtkColors.Muted)
                        }
                    }
                    TextButton(onClick = { deleteTarget = h }) { Text("Sil", color = MtkColors.Negative, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAdd) {
        WorkHistoryModal(
            employeeId = employeeId,
            onDismiss = { showAdd = false },
            onSaved = {
                showAdd = false
                reloadKey++
            },
        )
    }

    deleteTarget?.let { target ->
        az.mtk.app.ui.components.ConfirmDialog(
            title = "Qeydi sil",
            message = "\"${target.companyName}\" qeydi silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { HrApi.repository.deleteWorkHistory(target.id) }
                        .onSuccess { reloadKey++ }
                        .onFailure { error = it.toUserMessage() }
                }
            },
        )
    }
}

@Composable
private fun WorkHistoryModal(
    employeeId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var companyName by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Əvvəlki iş yeri",
        onDismiss = onDismiss,
        confirmEnabled = !saving && companyName.isNotBlank() && position.isNotBlank() && startDate.isNotBlank(),
        confirmLoading = saving,
        onConfirm = {
            saving = true
            error = null
            scope.launch {
                runCatching {
                    HrApi.repository.addWorkHistory(
                        HrWorkHistoryBody(
                            employeeId = employeeId,
                            companyName = companyName.trim(),
                            position = position.trim(),
                            startDate = runCatching { toIsoDate(LocalDate.parse(startDate)) }.getOrNull(),
                            endDate = runCatching { toIsoDate(LocalDate.parse(endDate)) }.getOrNull(),
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkTextField(companyName, { companyName = it }, "Təşkilat", required = true)
        MtkTextField(position, { position = it }, "Vəzifə", required = true)
        HrOptionalDateField(startDate.ifBlank { null }, { startDate = it }, "Başlanğıc tarixi", required = true)
        HrOptionalDateField(endDate.ifBlank { null }, { endDate = it }, "Bitmə tarixi")
        MtkTextField(notes, { notes = it }, "Qeyd")
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ------------------------------ Təhsil ------------------------------------------

@Composable
private fun EducationTab(employeeId: String) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<HrEducationHistoryDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HrEducationHistoryDto?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(employeeId, reloadKey) {
        runCatching { HrApi.repository.educationHistories(employeeId) }
            .onSuccess { items = it.sortedByDescending { h -> h.startDate }; error = null }
            .onFailure { error = it.toUserMessage() }
    }

    SectionHeader("Təhsil", "+ Təhsil əlavə et") { showAdd = true }
    Text(
        "İşçinin bitirdiyi və ya davam etdirdiyi təhsil müəssisələri, diplom məlumatları.",
        style = MaterialTheme.typography.bodySmall,
        color = MtkColors.Muted,
    )
    Spacer(Modifier.height(8.dp))

    when {
        error != null -> ErrorState(error!!, onRetry = { reloadKey++ })
        items == null -> LoadingState("Yüklənir…")
        items!!.isEmpty() -> EmptyState("Təhsil qeydi yoxdur")
        else -> items!!.forEach { h ->
            MtkCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            h.educationalInstitutionName ?: "—",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            h.faculty + (h.specialty?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(HrOptions.label(HrOptions.educationLevels, h.educationLevel), Tone.Info, showDot = false)
                            Pill(
                                "${Format.date(h.startDate)} – ${h.endDate?.let { Format.date(it) } ?: "davam edir"}",
                                Tone.Neutral,
                                showDot = false,
                            )
                        }
                        if (h.diplomaNumber != null || h.registerNumber != null) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                listOfNotNull(
                                    h.diplomaNumber?.let { "Diplom: $it" },
                                    h.registerNumber?.let { "Qeydiyyat №: $it" },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MtkColors.MutedSoft,
                            )
                        }
                    }
                    TextButton(onClick = { deleteTarget = h }) { Text("Sil", color = MtkColors.Negative, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAdd) {
        EducationModal(
            employeeId = employeeId,
            onDismiss = { showAdd = false },
            onSaved = {
                showAdd = false
                reloadKey++
            },
        )
    }

    deleteTarget?.let { target ->
        az.mtk.app.ui.components.ConfirmDialog(
            title = "Təhsil qeydini sil",
            message = "\"${target.educationalInstitutionName ?: "Təhsil"}\" qeydi silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { HrApi.repository.deleteEducationHistory(target.id) }
                        .onSuccess { reloadKey++ }
                        .onFailure { error = it.toUserMessage() }
                }
            },
        )
    }
}

@Composable
private fun EducationModal(
    employeeId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var institutionId by remember { mutableStateOf("") }
    var institutionLabel by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableStateOf("1") }
    var faculty by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var diplomaNumber by remember { mutableStateOf("") }
    var registerNumber by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Təhsil məlumatı",
        onDismiss = onDismiss,
        confirmEnabled = !saving && institutionId.isNotBlank() && faculty.isNotBlank() && startDate.isNotBlank(),
        confirmLoading = saving,
        onConfirm = {
            saving = true
            error = null
            scope.launch {
                runCatching {
                    HrApi.repository.addEducationHistory(
                        HrEducationHistoryBody(
                            employeeId = employeeId,
                            educationalInstitutionId = institutionId,
                            educationLevel = level.toIntOrNull() ?: 1,
                            faculty = faculty.trim(),
                            specialty = specialty.trim().takeIf { it.isNotEmpty() },
                            startDate = runCatching { toIsoDate(LocalDate.parse(startDate)) }.getOrNull(),
                            endDate = runCatching { toIsoDate(LocalDate.parse(endDate)) }.getOrNull(),
                            diplomaNumber = diplomaNumber.trim().takeIf { it.isNotEmpty() },
                            registerNumber = registerNumber.trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        SearchableSelect(
            label = "Təhsil ocağı",
            required = true,
            selectedLabel = institutionLabel,
            searchPlaceholder = "Təhsil ocağı axtar…",
            onLoad = { q -> HrApi.repository.institutions(0, 50, q).items },
            itemLabel = { it.name },
            onSelect = { institutionId = it.id; institutionLabel = it.name },
        )
        MtkSelect(level, { level = it }, "Təhsil səviyyəsi", HrOptions.educationLevels, required = true)
        MtkTextField(faculty, { faculty = it }, "Fakültə", required = true)
        MtkTextField(specialty, { specialty = it }, "İxtisas")
        HrOptionalDateField(startDate.ifBlank { null }, { startDate = it }, "Başlanğıc", required = true)
        HrOptionalDateField(endDate.ifBlank { null }, { endDate = it }, "Bitmə")
        MtkTextField(diplomaNumber, { diplomaNumber = it }, "Diplomun seriya və nömrəsi")
        MtkTextField(registerNumber, { registerNumber = it }, "Qeydiyyat nömrəsi")
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ------------------------------ Məzuniyyət balansı -------------------------------

@Composable
private fun VacationBalanceTab(employeeId: String) {
    val scope = rememberCoroutineScope()
    var balances by remember { mutableStateOf<List<HrVacationBalanceDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recalculating by remember { mutableStateOf(false) }
    var recalculated by remember { mutableStateOf(false) }

    LaunchedEffect(employeeId) {
        runCatching { HrApi.repository.vacationBalances(employeeId) }
            .onSuccess { list ->
                if (list.isEmpty()) {
                    runCatching { HrApi.repository.recalculateVacationBalance(employeeId) }
                        .onSuccess { balances = HrApi.repository.vacationBalances(employeeId) }
                        .onFailure { error = it.toUserMessage() }
                } else {
                    balances = list
                }
            }
            .onFailure { error = it.toUserMessage() }
    }

    when {
        error != null -> ErrorState(error!!, onRetry = { error = null; balances = null })
        balances == null -> LoadingState("Balans yüklənir…")
        balances!!.isEmpty() -> EmptyState("Məzuniyyət balansı yoxdur")
        else -> {
            val list = balances!!
            val totalEntitled = list.sumOf { it.totalEntitledDays ?: 0 }
            val totalUsed = list.sumOf { it.usedDays ?: 0 }
            val totalComp = list.sumOf { it.compensatedDays ?: 0 }
            val totalLeft = list.sumOf { it.remainingDays ?: 0 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "MƏZUNİYYƏT BALANSI".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedSoft,
                )
                TextButton(
                    enabled = !recalculating,
                    onClick = {
                        recalculating = true
                        recalculated = false
                        scope.launch {
                            runCatching { HrApi.repository.recalculateVacationBalance(employeeId) }
                                .onSuccess {
                                    balances = HrApi.repository.vacationBalances(employeeId)
                                    recalculated = true
                                }
                                .onFailure { error = it.toUserMessage() }
                            recalculating = false
                        }
                    },
                ) { Text(if (recalculating) "Hesablanır…" else "Yenidən hesabla", fontSize = 12.sp) }
            }

            MtkCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BalanceStat("Ümumi hüquq", totalEntitled)
                    BalanceStat("İstifadə", totalUsed)
                    BalanceStat("Kompensasiya", totalComp)
                    BalanceStat("Qalıq", totalLeft, highlight = true)
                }
                Spacer(Modifier.height(10.dp))
                Text("${list.size} iş ili", style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft)
            }
            if (recalculated) {
                Spacer(Modifier.height(4.dp))
                Text("Balans yenidən hesablandı ✓", style = MaterialTheme.typography.labelSmall, color = MtkColors.Positive)
            }

            Spacer(Modifier.height(8.dp))
            list.asReversed().forEach { balance ->
                MtkCard(padding = 12.dp) {
                    Text(
                        "${Format.date(balance.workYearStart)} – ${Format.date(balance.workYearEnd)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Rows(
                        "Əsas" to (balance.baseEntitledDays?.toString() ?: "—"),
                        "Staj bonusu" to (balance.experienceBonusDays?.toString() ?: "—"),
                        "Qarabağ" to (balance.karabakhWorkerDays?.toString() ?: "—"),
                        "Uşaq (14 yaşadək)" to (balance.childrenUnder14Days?.toString() ?: "—"),
                        "Əlil uşağı" to (balance.disabledChildDays?.toString() ?: "—"),
                        "İl üzrə cəmi" to (balance.totalEntitledDays?.toString() ?: "—"),
                        "İstifadə" to (balance.usedDays?.toString() ?: "—"),
                        "Kompensasiya" to (balance.compensatedDays?.toString() ?: "—"),
                        "Qalıq" to (balance.remainingDays?.toString() ?: "—"),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BalanceStat(label: String, value: Int, highlight: Boolean = false) {
    Column {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) (if (value > 0) MtkColors.Green else MtkColors.Muted) else MtkColors.Ink,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft)
    }
}

// ------------------------------ Sənədlər ---------------------------------------

/** İşçiyə bağlı ərizə və əmrlər + PDF/Excel yükləmə (imzalanmış sənəd əvəzi). */
@Composable
private fun DocumentsTab(employee: HrEmployeeDetailDto, scope: kotlinx.coroutines.CoroutineScope) {
    var applications by remember { mutableStateOf<List<az.mtk.app.data.api.ApplicationDto>?>(null) }
    var orders by remember { mutableStateOf<List<az.mtk.app.data.api.HrOrderItemDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(employee.id, reloadKey) {
        error = null
        runCatching {
            val apps = AppGraph.repository.applications(0, 100).items.filter { it.employee?.id == employee.id }
            val ords = HrApi.repository.orders(0, 100).items.filter { it.employee?.id == employee.id }
            apps to ords
        }
            .onSuccess { (apps, ords) -> applications = apps; orders = ords }
            .onFailure { error = it.toUserMessage() }
    }

    SectionHeader("Ərizə və əmrlər", "Yenilə") { reloadKey++ }
    Text(
        "İşçiyə bağlı sənədlərin PDF nüsxələrini yükləyin. İmzalanmış fayllar müvafiq ərizə/əmrə əlavə olunur.",
        style = MaterialTheme.typography.bodySmall,
        color = MtkColors.Muted,
    )
    Spacer(Modifier.height(8.dp))

    when {
        error != null -> ErrorState(error!!, onRetry = { reloadKey++ })
        applications == null || orders == null -> LoadingState("Sənədlər yüklənir…")
        else -> {
            Text("Əmrlər", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (orders!!.isEmpty()) {
                EmptyState("Əmr tapılmadı")
            } else {
                orders!!.forEach { order ->
                    MtkCard(padding = 10.dp) {
                        Text(
                            "Əmr № ${order.orderNumber ?: "—"} · ${HrOrderKind.label(order.type)} · ${Format.dateTime(order.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ExportActionRow(
                            title = "PDF yüklə",
                            path = "api/hr/${HrOrderKind.route(order.type)}/${order.id}/export-pdf",
                            scope = scope,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Ərizələr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (applications!!.isEmpty()) {
                EmptyState("Ərizə tapılmadı")
            } else {
                applications!!.forEach { application ->
                    MtkCard(padding = 10.dp) {
                        Text(
                            "Ərizə № ${application.applicationNumber ?: "—"} · ${Format.applicationType(application.type)} · " +
                                (application.status ?: "—"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        application.type?.let { type ->
                            ExportActionRow(
                                title = "PDF yüklə",
                                path = "api/hr/${HrApplicationKind.route(type)}/${application.id}/export-pdf",
                                scope = scope,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ------------------------------ Ortak UI ---------------------------------------

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
        )
        TextButton(onClick = onAction) { Text(actionLabel, fontSize = 12.sp) }
    }
}

@Composable
private fun KeyValueHeader2(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MtkColors.MutedSoft,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
    )
}
