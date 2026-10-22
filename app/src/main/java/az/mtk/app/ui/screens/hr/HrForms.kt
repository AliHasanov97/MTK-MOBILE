package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import az.mtk.app.AppGraph
import az.mtk.app.data.api.HrApplicationKind
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrDirectOrderKind
import az.mtk.app.data.api.HrLaborCodeCaseDto
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

// ---- Ortak tab çipi -----------------------------------------------------------

@Composable
fun HrTabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
            .background(if (selected) MtkColors.Green else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MtkColors.Muted,
            maxLines = 1,
        )
    }
}

// ---- Enum seçimləri (hr.ts) -------------------------------------------------

object HrOptions {
    val genders = listOf(MtkOption("1", "Kişi"), MtkOption("2", "Qadın"))
    val maritalStatuses = listOf(MtkOption("0", "Subay"), MtkOption("1", "Evli"))
    val militaryServices = listOf(
        MtkOption("1", "Xidmət etməyib"),
        MtkOption("2", "Xidmət edib"),
        MtkOption("3", "Hərbi xidmətdən azaddır"),
    )
    val educationLevels = listOf(
        MtkOption("0", "Yoxdur"),
        MtkOption("1", "Ali (bakalavr)"),
        MtkOption("2", "Ali (magistr)"),
        MtkOption("3", "Ali (doktorantura)"),
        MtkOption("4", "Ümumi orta"),
        MtkOption("5", "Tam orta"),
        MtkOption("6", "İlk peşə-ixtisas"),
        MtkOption("7", "Orta ixtisas"),
        MtkOption("8", "Orta ixtisas (ümumi orta əsasında)"),
        MtkOption("9", "Orta ixtisas (tam orta əsasında)"),
        MtkOption("10", "Ali (natamam, 1993-dən əvvəl)"),
        MtkOption("11", "Ali (tam, 1997-dən əvvəl)"),
    )
    val employmentTypes = listOf(MtkOption("1", "Tam ştat"), MtkOption("2", "Yarım ştat"))
    val employeeStatuses = listOf(MtkOption("1", "Aktiv"), MtkOption("2", "Qeyri-aktiv"))
    val workingDays = listOf(MtkOption("1", "5 günlük iş həftəsi"), MtkOption("2", "6 günlük iş həftəsi"))
    val disciplinaryTypes = listOf(
        MtkOption("1", "Xəbərdarlıq"),
        MtkOption("2", "Töhmət"),
        MtkOption("3", "Ciddi töhmət"),
    )
    val institutionTypes = listOf(MtkOption("1", "Ali məktəb"), MtkOption("2", "Kollec"))
    val calendarDayTypes = listOf(
        MtkOption("1", "Bayram"),
        MtkOption("2", "İş günü"),
        MtkOption("3", "Qeyri-iş günü"),
    )
    val months = listOf(
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
        "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
    ).mapIndexed { i, name -> MtkOption((i + 1).toString(), name) }

    fun label(options: List<MtkOption>, value: Int?): String =
        options.firstOrNull { it.value == value?.toString() }?.label ?: "—"
}

/** hr.ts dateOnlyToUtcIso — <input type="date"> dəyəri üçün UTC ISO. */
fun toIsoDate(value: LocalDate?): String? =
    value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toString()

// ---- Dinamik sahələr (documentConfig.ts) ------------------------------------

enum class HrFieldKind { TEXT, NUMBER, DATE, SELECT, JOB, EMPLOYEE }

data class HrFieldSpec(
    val key: String,
    val label: String,
    val kind: HrFieldKind,
    val required: Boolean = false,
    val options: List<MtkOption> = emptyList(),
    val hint: String? = null,
)

/** Ərizə növünə görə sahələr (hr APPLICATION_FIELDS). */
val HrApplicationFields: Map<Int, List<HrFieldSpec>> = mapOf(
    HrApplicationKind.JOB_APPLICATION to listOf(
        HrFieldSpec("surname", "Soyad", HrFieldKind.TEXT, required = true),
        HrFieldSpec("name", "Ad", HrFieldKind.TEXT, required = true),
        HrFieldSpec("fathersName", "Ata adı", HrFieldKind.TEXT, required = true),
        HrFieldSpec("gender", "Cins", HrFieldKind.SELECT, required = true, options = HrOptions.genders),
        HrFieldSpec("telephone", "Telefon", HrFieldKind.TEXT, required = true),
        HrFieldSpec("homeTelephoneNumber", "Ev telefonu", HrFieldKind.TEXT),
        HrFieldSpec("address", "Ünvan", HrFieldKind.TEXT, required = true),
        HrFieldSpec("jobId", "Vəzifə", HrFieldKind.JOB, required = true),
        HrFieldSpec("startDate", "İşə başlama tarixi", HrFieldKind.DATE, required = true),
    ),
    HrApplicationKind.CHANGE_OF_POSITION to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("newJobId", "Yeni vəzifə", HrFieldKind.JOB, required = true),
        HrFieldSpec("setDate", "Dəyişiklik tarixi", HrFieldKind.DATE, required = true),
    ),
    HrApplicationKind.VACATION_COMPENSATION to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("requestedDays", "Kompensasiya ediləcək gün sayı", HrFieldKind.NUMBER, required = true),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    HrApplicationKind.UNPAID_LEAVE to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    HrApplicationKind.VACATION to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec(
            "endDate", "Bitmə tarixi", HrFieldKind.DATE,
            hint = "Bitmə tarixi və ya gün sayından yalnız birini doldurun. Bayram günləri avtomatik çıxılır.",
        ),
        HrFieldSpec("requestedDays", "Gün sayı", HrFieldKind.NUMBER),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    HrApplicationKind.EDUCATION_LEAVE to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("reason", "Səbəb", HrFieldKind.TEXT),
    ),
    HrApplicationKind.EMPLOYMENT_STATUS_CHANGE to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("newEmploymentType", "Yeni iş rejimi", HrFieldKind.SELECT, required = true, options = HrOptions.employmentTypes),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, required = true),
    ),
    HrApplicationKind.VACATION_RETURN to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("returnDate", "İşə qayıtma tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
)

/** Ərizəsiz verilən əmrlər (hr DIRECT_ORDER_FIELDS). */
val HrDirectOrderFields: Map<String, Pair<String, List<HrFieldSpec>>> = mapOf(
    HrDirectOrderKind.BONUS to ("Mükafat" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("bonusQuantity", "Mükafat məbləği (₼)", HrFieldKind.NUMBER, required = true),
        HrFieldSpec("salaryMonth", "Maaş ayı", HrFieldKind.SELECT, required = true, options = HrOptions.months),
        HrFieldSpec("salaryYear", "Maaş ili", HrFieldKind.NUMBER, required = true),
    )),
    HrDirectOrderKind.UNEXCUSED_ABSENCE to ("İcazəsiz qeyb" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("setDate", "Qeyb tarixi", HrFieldKind.DATE, required = true),
    )),
    HrDirectOrderKind.WARNING to ("Xəbərdarlıq / töhmət" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, required = true),
        HrFieldSpec("disciplinaryType", "Növ", HrFieldKind.SELECT, required = true, options = HrOptions.disciplinaryTypes),
        HrFieldSpec("setDate", "Tarix", HrFieldKind.DATE, required = true),
    )),
    HrDirectOrderKind.WORK_ON_NON_WORKDAY to ("İstirahət / bayram günü işi" to listOf(
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, required = true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, hint = "Boş buraxsanız yalnız başlanğıc günü nəzərdə tutulur."),
    )),
)

// ---- Dinamik forma ----------------------------------------------------------

@Composable
fun HrDynamicFields(
    specs: List<HrFieldSpec>,
    values: Map<String, String>,
    onChange: (key: String, value: String) -> Unit,
) {
    specs.forEach { spec ->
        when (spec.kind) {
            HrFieldKind.TEXT -> MtkTextField(
                value = values[spec.key].orEmpty(),
                onValueChange = { onChange(spec.key, it) },
                label = spec.label,
                required = spec.required,
            )
            HrFieldKind.NUMBER -> MtkNumberField(
                value = values[spec.key].orEmpty(),
                onValueChange = { onChange(spec.key, it) },
                label = spec.label,
                required = spec.required,
            )
            HrFieldKind.DATE -> HrOptionalDateField(
                value = values[spec.key],
                onValueChange = { onChange(spec.key, it) },
                label = spec.label,
                required = spec.required,
            )
            HrFieldKind.SELECT -> MtkSelect(
                value = values[spec.key],
                onValueChange = { onChange(spec.key, it) },
                label = spec.label,
                options = spec.options,
                required = spec.required,
            )
            HrFieldKind.JOB -> SearchableSelect(
                label = spec.label,
                required = spec.required,
                selectedLabel = values[spec.key + "Label"],
                searchPlaceholder = "Vəzifə axtar…",
                onLoad = { query -> HrApi.repository.jobs(0, 50, query).items },
                itemLabel = { it.name },
                onSelect = {
                    onChange(spec.key, it.id)
                    onChange(spec.key + "Label", it.name)
                },
            )
            HrFieldKind.EMPLOYEE -> SearchableSelect(
                label = spec.label,
                required = spec.required,
                selectedLabel = values[spec.key + "Label"],
                searchPlaceholder = "İşçi axtar…",
                onLoad = { query -> AppGraph.repository.employees(0, 50, query).items },
                itemLabel = { az.mtk.app.util.Format.person(it.name, it.surname, it.fathersName) },
                onSelect = {
                    onChange(spec.key, it.id)
                    onChange(spec.key + "Label", az.mtk.app.util.Format.person(it.name, it.surname, it.fathersName))
                },
            )
        }
        spec.hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Boş dəyərə icazə verən tarix sahəsi (MtkDateField örtüyü). */
@Composable
fun HrOptionalDateField(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean = false,
) {
    MtkDateField(
        value = value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        onValueChange = { onValueChange(it.toString()) },
        label = label,
        required = required,
    )
}

/** Dinamik sahələrdən POST gövdəsi qurur (hr buildBody). */
fun buildHrBody(specs: List<HrFieldSpec>, values: Map<String, String>): JsonObject {
    val body = JsonObject()
    specs.forEach { spec ->
        val raw = values[spec.key]?.trim().orEmpty()
        when (spec.kind) {
            HrFieldKind.TEXT -> if (raw.isNotEmpty()) body.addProperty(spec.key, raw)
            HrFieldKind.NUMBER -> if (raw.isNotEmpty()) raw.toDoubleOrNull()?.let { body.addProperty(spec.key, it) }
            HrFieldKind.DATE -> if (raw.isNotEmpty()) {
                runCatching { toIsoDate(LocalDate.parse(raw)) }
                    .getOrNull()?.let { body.addProperty(spec.key, it) }
            }
            HrFieldKind.SELECT -> if (raw.isNotEmpty()) raw.toIntOrNull()?.let { body.addProperty(spec.key, it) }
            HrFieldKind.JOB, HrFieldKind.EMPLOYEE -> if (raw.isNotEmpty()) body.addProperty(spec.key, raw)
        }
    }
    return body
}

fun missingRequired(specs: List<HrFieldSpec>, values: Map<String, String>): String? =
    specs.firstOrNull { it.required && values[it.key]?.trim().isNullOrEmpty() }?.let { "${it.label} doldurulmalıdır." }

// ---- İşçi yaratma (EmployeeForm ekvivalenti) --------------------------------

data class HrEmployeeFormState(
    val name: String = "",
    val surname: String = "",
    val fathersName: String = "",
    val gender: String = "1",
    val startWorkDate: String = "",
    val jobId: String = "",
    val jobLabel: String? = null,
    val nationality: String = "",
    val birthDate: String = "",
    val finCode: String = "",
    val idCardNumber: String = "",
    val socialSecurityNumber: String = "",
    val contractNumber: String = "",
    val salaryBankName: String = "",
    val employeeBankAccountNumber: String = "",
    val baseSalary: String = "0",
    val maritalStatus: String = "",
    val numberOfChildren: String = "",
    val childrenUnder14Count: String = "",
    val militaryService: String = "",
    val veteran: Boolean = false,
    val disability: Boolean = false,
    val isKarabakhWorker: Boolean = false,
    val isSingleParent: Boolean = false,
    val hasDisabledChild: Boolean = false,
    val education: String = "",
    val phoneNumber: String = "",
    val homePhoneNumber: String = "",
    val email: String = "",
    val registeredAddress: String = "",
    val currentAddress: String = "",
    val workingDays: String = "1",
    val vacationDays: String = "21",
    val employmentType: String = "1",
    val isActive: String = "1",
)

fun numOrNull(v: String): Int? = v.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

/** Tam işçi gövdəsi (hr employeeBody + jobId/startWorkDate). */
fun HrEmployeeFormState.toCreateBody(): JsonObject {
    val body = JsonObject()
    body.addProperty("name", name.trim())
    body.addProperty("surname", surname.trim())
    body.addProperty("fathersName", fathersName.trim())
    body.addProperty("gender", gender.toIntOrNull() ?: 1)
    fun str(key: String, value: String) { if (value.isNotBlank()) body.addProperty(key, value.trim()) }
    str("nationality", nationality)
    toIsoDate(runCatching { LocalDate.parse(birthDate) }.getOrNull())?.let { body.addProperty("birthDate", it) }
    str("finCode", finCode)
    str("idCardNumber", idCardNumber)
    str("socialSecurityNumber", socialSecurityNumber)
    str("contractNumber", contractNumber)
    str("salaryBankName", salaryBankName)
    str("employeeBankAccountNumber", employeeBankAccountNumber)
    body.addProperty("baseSalary", baseSalary.toDoubleOrNull() ?: 0.0)
    numOrNull(maritalStatus)?.let { body.addProperty("maritalStatus", it) }
    numOrNull(numberOfChildren)?.let { body.addProperty("numberOfChildren", it) }
    numOrNull(childrenUnder14Count)?.let { body.addProperty("childrenUnder14Count", it) }
    numOrNull(militaryService)?.let { body.addProperty("militaryService", it) }
    body.addProperty("veteran", veteran)
    body.addProperty("disability", disability)
    body.addProperty("isKarabakhWorker", isKarabakhWorker)
    body.addProperty("isSingleParent", isSingleParent)
    body.addProperty("hasDisabledChild", hasDisabledChild)
    numOrNull(education)?.let { body.addProperty("education", it) }
    str("phoneNumber", phoneNumber)
    str("homePhoneNumber", homePhoneNumber)
    str("email", email)
    str("registeredAddress", registeredAddress)
    str("currentAddress", currentAddress)
    numOrNull(workingDays)?.let { body.addProperty("workingDays", it) }
    numOrNull(vacationDays)?.let { body.addProperty("vacationDays", it) }
    body.addProperty("employmentType", employmentType.toIntOrNull() ?: 1)
    body.addProperty("jobId", jobId)
    toIsoDate(runCatching { LocalDate.parse(startWorkDate) }.getOrNull())?.let { body.addProperty("startWorkDate", it) }
    return body
}

fun validateEmployeeForm(f: HrEmployeeFormState): String? = when {
    f.name.isBlank() -> "Ad doldurulmalıdır."
    f.surname.isBlank() -> "Soyad doldurulmalıdır."
    f.jobId.isBlank() -> "Vəzifə seçilməlidir."
    f.startWorkDate.isBlank() -> "İşə başlama tarixi seçilməlidir."
    else -> null
}

@Composable
fun HrEmployeeCreateModal(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var form by remember { mutableStateOf(HrEmployeeFormState()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Yeni işçi",
        subtitle = "İşçinin əsas məlumatlarını daxil edin",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            validateEmployeeForm(form)?.let { error = it; return@MtkModal }
            saving = true
            error = null
            scope.launch {
                runCatching { HrApi.repository.createEmployee(form.toCreateBody()) }
                    .onSuccess { onCreated() }
                    .onFailure {
                        error = it.toUserMessage()
                        saving = false
                    }
            }
        },
    ) {
        MtkTextField(form.name, { form = form.copy(name = it) }, "Ad", required = true)
        MtkTextField(form.surname, { form = form.copy(surname = it) }, "Soyad", required = true)
        MtkTextField(form.fathersName, { form = form.copy(fathersName = it) }, "Ata adı")
        MtkSelect(form.gender, { form = form.copy(gender = it) }, "Cins", HrOptions.genders, required = true)
        SearchableSelect(
            label = "Vəzifə",
            required = true,
            selectedLabel = form.jobLabel,
            searchPlaceholder = "Vəzifə axtar…",
            onLoad = { q -> HrApi.repository.jobs(0, 50, q).items },
            itemLabel = { it.name },
            onSelect = { form = form.copy(jobId = it.id, jobLabel = it.name) },
        )
        HrOptionalDateField(form.startWorkDate.ifBlank { null }, { form = form.copy(startWorkDate = it) }, "İşə başlama tarixi", required = true)
        MtkSelect(form.employmentType, { form = form.copy(employmentType = it) }, "İş rejimi", HrOptions.employmentTypes)
        MtkNumberField(form.baseSalary, { form = form.copy(baseSalary = it) }, "Əsas əmək haqqı (₼)")
        MtkTextField(form.finCode, { form = form.copy(finCode = it) }, "FİN kod")
        HrOptionalDateField(form.birthDate.ifBlank { null }, { form = form.copy(birthDate = it) }, "Doğum tarixi")
        MtkTextField(form.phoneNumber, { form = form.copy(phoneNumber = it) }, "Mobil telefon")
        MtkTextField(form.email, { form = form.copy(email = it) }, "E-poçt")
        MtkTextField(form.currentAddress, { form = form.copy(currentAddress = it) }, "Faktiki ünvan")
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---- Yeni ərizə -------------------------------------------------------------

@Composable
fun HrCreateApplicationModal(
    employeeId: String? = null,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val allKinds = HrApplicationFields.keys.toList()
    val kinds = if (employeeId != null) allKinds.filter { it != HrApplicationKind.JOB_APPLICATION } else allKinds
    var kind by remember { mutableStateOf(kinds.firstOrNull() ?: HrApplicationKind.VACATION) }
    var values by remember(kind) { mutableStateOf(emptyMap<String, String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val specs = HrApplicationFields[kind].orEmpty().filter { !(employeeId != null && it.key == "employeeId") }

    MtkModal(
        title = "Yeni ərizə",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            missingRequired(specs, values)?.let { error = it; return@MtkModal }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    val body = buildHrBody(specs, values)
                    employeeId?.let { body.addProperty("employeeId", it) }
                    if (kind == HrApplicationKind.VACATION || kind == HrApplicationKind.VACATION_COMPENSATION) {
                        body.addProperty("vacationBalanceId", "00000000-0000-0000-0000-000000000000")
                    }
                    HrApi.repository.createApplication(kind, body)
                }
                    .onSuccess { onCreated() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkSelect(
            value = kind.toString(),
            onValueChange = { kind = it.toInt() },
            label = "Ərizə növü",
            options = kinds.map { MtkOption(it.toString(), HrApplicationKind.label(it)) },
        )
        HrDynamicFields(specs, values) { key, value -> values = values + (key to value) }
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---- Yeni əmr ---------------------------------------------------------------

@Composable
fun HrCreateOrderModal(
    employeeId: String? = null,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val allKinds = HrDirectOrderFields.keys.toList()
    val kinds = if (employeeId != null) {
        allKinds.filter { kind -> HrDirectOrderFields[kind]!!.second.any { it.key == "employeeId" } }
    } else allKinds
    var kind by remember { mutableStateOf(kinds.first()) }
    var values by remember(kind) { mutableStateOf(emptyMap<String, String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val specs = HrDirectOrderFields[kind]!!.second.filter { !(employeeId != null && it.key == "employeeId") }

    MtkModal(
        title = "Yeni əmr",
        subtitle = "Yalnız ərizəsiz verilən əmrlər burada yaradılır. İşə qəbul, məzuniyyət, vəzifə və iş rejimi dəyişikliyi əmrləri müvafiq ərizənin təsdiqi ilə avtomatik yaranır.",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            missingRequired(specs, values)?.let { error = it; return@MtkModal }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    val body = buildHrBody(specs, values)
                    employeeId?.let { body.addProperty("employeeId", it) }
                    HrApi.repository.createDirectOrder(kind, body)
                }
                    .onSuccess { onCreated() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkSelect(
            value = kind,
            onValueChange = { kind = it },
            label = "Əmr növü",
            options = kinds.map { MtkOption(it, HrDirectOrderFields[it]!!.first) },
        )
        HrDynamicFields(specs, values) { key, value -> values = values + (key to value) }
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---- Əmri əmrə çevirmə -------------------------------------------------------

@Composable
fun HrLaborCaseSelect(
    value: String?,
    onValueChange: (String) -> Unit,
    cases: List<HrLaborCodeCaseDto>,
) {
    val childCases = cases.filter { it.parentId != null }
    MtkSelect(
        value = value,
        onValueChange = onValueChange,
        label = "Müddətli müqavilənin əsası",
        options = childCases.map { case -> MtkOption(case.id, "${case.parentName ?: "—"} — ${case.code} ${case.name}") },
    )
}

@Composable
fun HrConvertApplicationModal(
    type: Int,
    applicationId: String,
    applicantName: String?,
    onDismiss: () -> Unit,
    onConverted: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (type != HrApplicationKind.JOB_APPLICATION) {
        ConfirmDialog(
            title = "Əmri əmrə çevir",
            message = "${HrApplicationKind.label(type)} ərizəsi təsdiqlənib əmrə çevrilsin?",
            confirmLabel = "Əmrə çevir",
            onDismiss = onDismiss,
            onConfirm = {
                saving = true
                scope.launch {
                    runCatching { HrApi.repository.convertApplication(type, applicationId) }
                        .onSuccess { onConverted() }
                        .onFailure { error = it.toUserMessage(); saving = false }
                }
            },
        )
        return
    }

    var cases by remember { mutableStateOf<List<HrLaborCodeCaseDto>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var caseId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching { HrApi.repository.laborCodeCases() }
            .onSuccess { cases = it }
            .onFailure { loadError = it.toUserMessage() }
    }

    MtkModal(
        title = "${applicantName ?: "Namizəd"} — işə qəbul əmri",
        onDismiss = onDismiss,
        confirmLabel = "Əmrə çevir",
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            if (caseId.isBlank()) { error = "Müddətli müqavilənin əsası seçilməlidir."; return@MtkModal }
            if (startDate.isBlank() || endDate.isBlank()) { error = "Müqavilə tarixləri seçilməlidir."; return@MtkModal }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    val body = JsonObject().apply {
                        addProperty("startDate", toIsoDate(LocalDate.parse(startDate)))
                        addProperty("endDate", toIsoDate(LocalDate.parse(endDate)))
                        addProperty("laborCodeCaseId", caseId)
                    }
                    HrApi.repository.convertApplication(HrApplicationKind.JOB_APPLICATION, applicationId, body)
                }
                    .onSuccess { onConverted() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        HrOptionalDateField(startDate.ifBlank { null }, { startDate = it }, "Müqavilənin başlanğıcı", required = true)
        HrOptionalDateField(endDate.ifBlank { null }, { endDate = it }, "Müqavilənin bitməsi", required = true)
        Text(
            "Azərbaycan Respublikasının Əmək Məcəlləsinin müvafiq maddəsi və bəndi seçilməlidir.",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        when {
            loadError != null -> ErrorState(loadError!!)
            cases == null -> LoadingState("Maddələr yüklənir…")
            cases!!.isEmpty() -> EmptyState("Əmək məcəlləsi maddəsi tapılmadı")
            else -> HrLaborCaseSelect(caseId, { caseId = it }, cases!!)
        }
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---- Təsdiq pəncərəsi --------------------------------------------------------

@Composable
fun HrDeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmLabel = "Sil",
        danger = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
