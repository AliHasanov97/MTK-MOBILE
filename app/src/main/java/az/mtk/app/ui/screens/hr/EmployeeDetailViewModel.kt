package az.mtk.app.ui.screens.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrEducationHistoryBody
import az.mtk.app.data.api.HrEducationHistoryDto
import az.mtk.app.data.api.HrEmployeeDetailDto
import az.mtk.app.data.api.HrInstitutionDto
import az.mtk.app.data.api.HrVacationBalanceDto
import az.mtk.app.data.api.HrWorkHistoryBody
import az.mtk.app.data.api.HrWorkHistoryDto
import az.mtk.app.data.api.HrWorkScheduleDto
import az.mtk.app.util.toUserMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EmployeeDetailTab(val label: String) {
    INFO("Məlumatlar"),
    SCHEDULE("İş qrafiki"),
    WORK("İş tarixçəsi"),
    EDUCATION("Təhsil"),
    VACATION("Məzuniyyət balansı"),
    DOCUMENTS("Ərizə və əmrlər"),
    PAYMENTS("Maaş ödənişləri"),
}

enum class HrEmployeeSection(val title: String) {
    PERSONAL("Şəxsi məlumatlar"),
    FAMILY("Ailə və sosial vəziyyət"),
    CONTACT("Əlaqə"),
    WORK("İş məlumatları"),
    BANK("Müqavilə və bank"),
}

internal val HR_SCHEDULE_DAYS: List<Pair<String, String>> = listOf(
    "monday" to "Bazar ertəsi",
    "tuesday" to "Çərşənbə axşamı",
    "wednesday" to "Çərşənbə",
    "thursday" to "Cümə axşamı",
    "friday" to "Cümə",
    "saturday" to "Şənbə",
    "sunday" to "Bazar",
)

internal fun HrEmployeeDetailDto.toForm(): HrEmployeeForm = HrEmployeeForm(
    surname = surname.orEmpty(),
    name = name.orEmpty(),
    fathersName = fathersName.orEmpty(),
    gender = gender ?: 1,
    birthDate = dateInput(birthDate),
    nationality = nationality.orEmpty(),
    finCode = finCode.orEmpty(),
    idCardNumber = idCardNumber.orEmpty(),
    socialSecurityNumber = socialSecurityNumber.orEmpty(),
    maritalStatus = maritalStatus?.toString().orEmpty(),
    numberOfChildren = numberOfChildren?.toString().orEmpty(),
    childrenUnder14Count = childrenUnder14Count?.toString().orEmpty(),
    militaryService = militaryService?.toString().orEmpty(),
    education = education?.toString().orEmpty(),
    veteran = veteran == true,
    disability = disability == true,
    isKarabakhWorker = isKarabakhWorker == true,
    isSingleParent = isSingleParent == true,
    hasDisabledChild = hasDisabledChild == true,
    phoneNumber = phoneNumber.orEmpty(),
    homePhoneNumber = homePhoneNumber.orEmpty(),
    email = email.orEmpty(),
    registeredAddress = registeredAddress.orEmpty(),
    currentAddress = currentAddress.orEmpty(),
    jobId = job?.id.orEmpty(),
    startWorkDate = dateInput(startWorkDate),
    employmentType = employmentType ?: 1,
    isActive = isActive ?: 1,
    workingDays = workingDays?.toString().orEmpty(),
    vacationDays = vacationDays?.toString().orEmpty(),
    contractNumber = contractNumber.orEmpty(),
    baseSalary = baseSalary?.toString().orEmpty(),
    salaryBankName = salaryBankName.orEmpty(),
    employeeBankAccountNumber = employeeBankAccountNumber.orEmpty(),
)

class EmployeeDetailViewModel : ViewModel() {

    private val repo = HrApi.repository

    data class UiState(
        val employee: HrEmployeeDetailDto? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val tab: EmployeeDetailTab = EmployeeDetailTab.INFO,
        val editSection: HrEmployeeSection? = null,
        val editForm: HrEmployeeForm = HrEmployeeForm(),
        val saving: Boolean = false,
        val saveError: String? = null,
        val scheduleLoading: Boolean = false,
        val scheduleError: String? = null,
        val scheduleSaving: Boolean = false,
        val scheduleSaved: Boolean = false,
        val effectiveFrom: String? = null,
        val hours: Map<String, String> = emptyMap(),
        val workHistories: List<HrWorkHistoryDto>? = null,
        val workError: String? = null,
        val workSaving: Boolean = false,
        val addWorkOpen: Boolean = false,
        val addWorkError: String? = null,
        val education: List<HrEducationHistoryDto>? = null,
        val educationError: String? = null,
        val educationSaving: Boolean = false,
        val addEducationOpen: Boolean = false,
        val addEducationError: String? = null,
        val institutions: List<HrInstitutionDto> = emptyList(),
        val balances: List<HrVacationBalanceDto>? = null,
        val balanceLoading: Boolean = false,
        val balanceError: String? = null,
        val recalculating: Boolean = false,
        val balanceNotice: String? = null,
        val actionMessage: String? = null,
        val actionError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load(employeeId: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.employeeDetail(employeeId) }
                .onSuccess { employee ->
                    _state.value = _state.value.copy(employee = employee, loading = false)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(error = failure.toUserMessage(), loading = false)
                }
        }
    }

    fun selectTab(employeeId: String, tab: EmployeeDetailTab) {
        _state.value = _state.value.copy(tab = tab)
        when (tab) {
            EmployeeDetailTab.SCHEDULE -> if (_state.value.hours.isEmpty()) loadSchedule(employeeId)
            EmployeeDetailTab.WORK -> if (_state.value.workHistories == null) loadWork(employeeId)
            EmployeeDetailTab.EDUCATION -> {
                if (_state.value.education == null) loadEducation(employeeId)
                if (_state.value.institutions.isEmpty()) loadInstitutions()
            }
            EmployeeDetailTab.VACATION -> if (_state.value.balances == null) loadBalances(employeeId)
            else -> Unit
        }
    }

    fun refresh(employeeId: String) {
        load(employeeId)
        when (_state.value.tab) {
            EmployeeDetailTab.SCHEDULE -> loadSchedule(employeeId)
            EmployeeDetailTab.WORK -> loadWork(employeeId)
            EmployeeDetailTab.EDUCATION -> loadEducation(employeeId)
            EmployeeDetailTab.VACATION -> loadBalances(employeeId)
            else -> Unit
        }
    }

    /* ------------------------------ Bölmə redaktəsi ------------------------------ */

    fun openEdit(section: HrEmployeeSection) {
        val employee = _state.value.employee ?: return
        _state.value = _state.value.copy(
            editSection = section,
            editForm = employee.toForm(),
            saveError = null,
        )
    }

    fun closeEdit() {
        _state.value = _state.value.copy(editSection = null, saveError = null, saving = false)
    }

    fun updateEditForm(form: HrEmployeeForm) {
        _state.value = _state.value.copy(editForm = form, saveError = null)
    }

    fun saveSection(employeeId: String) {
        val s = _state.value
        val section = s.editSection ?: return
        val body = sectionBody(section, s.editForm)
        _state.value = s.copy(saving = true, saveError = null)
        viewModelScope.launch {
            runCatching { repo.updateEmployeeSection(employeeId, body) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        saving = false,
                        editSection = null,
                        actionMessage = "Məlumat yeniləndi.",
                    )
                    load(employeeId)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(saving = false, saveError = failure.toUserMessage())
                }
        }
    }

    private fun sectionBody(section: HrEmployeeSection, f: HrEmployeeForm): JsonObject = JsonObject().apply {
        fun text(key: String, value: String) = addProperty(key, value.trim())
        fun number(key: String, value: String) {
            val parsed = value.trim().toIntOrNull()
            if (parsed != null) addProperty(key, parsed)
        }
        when (section) {
            HrEmployeeSection.PERSONAL -> {
                text("surname", f.surname)
                text("name", f.name)
                text("fathersName", f.fathersName)
                addProperty("gender", f.gender)
                if (f.birthDate.isNotBlank()) addProperty("birthDate", isoDate(f.birthDate))
                text("nationality", f.nationality)
                text("finCode", f.finCode)
                text("idCardNumber", f.idCardNumber)
                text("socialSecurityNumber", f.socialSecurityNumber)
            }
            HrEmployeeSection.FAMILY -> {
                number("maritalStatus", f.maritalStatus)
                number("numberOfChildren", f.numberOfChildren)
                number("childrenUnder14Count", f.childrenUnder14Count)
                number("militaryService", f.militaryService)
                number("education", f.education)
                addProperty("veteran", f.veteran)
                addProperty("disability", f.disability)
                addProperty("isKarabakhWorker", f.isKarabakhWorker)
                addProperty("isSingleParent", f.isSingleParent)
                addProperty("hasDisabledChild", f.hasDisabledChild)
            }
            HrEmployeeSection.CONTACT -> {
                text("phoneNumber", f.phoneNumber)
                text("homePhoneNumber", f.homePhoneNumber)
                text("email", f.email)
                text("registeredAddress", f.registeredAddress)
                text("currentAddress", f.currentAddress)
            }
            HrEmployeeSection.WORK -> {
                addProperty("employmentType", f.employmentType)
                number("workingDays", f.workingDays)
                number("vacationDays", f.vacationDays)
                addProperty("isActive", f.isActive)
            }
            HrEmployeeSection.BANK -> {
                text("contractNumber", f.contractNumber)
                f.baseSalary.trim().toDoubleOrNull()?.let { addProperty("baseSalary", it) }
                text("salaryBankName", f.salaryBankName)
                text("employeeBankAccountNumber", f.employeeBankAccountNumber)
            }
        }
        addProperty("id", employeeIdSafe())
    }

    private fun employeeIdSafe(): String = _state.value.employee?.id.orEmpty()

    /* ------------------------------ İş qrafiki ------------------------------ */

    private fun loadSchedule(employeeId: String) {
        _state.value = _state.value.copy(scheduleLoading = true, scheduleError = null)
        viewModelScope.launch {
            runCatching { repo.workSchedule(employeeId) }
                .onSuccess { schedule ->
                    _state.value = _state.value.copy(
                        scheduleLoading = false,
                        scheduleSaved = false,
                        effectiveFrom = schedule.effectiveFrom,
                        hours = schedule.toHours(),
                    )
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(scheduleLoading = false, scheduleError = failure.toUserMessage())
                }
        }
    }

    private fun HrWorkScheduleDto.toHours(): Map<String, String> = mapOf(
        "monday" to (monday?.toString() ?: ""),
        "tuesday" to (tuesday?.toString() ?: ""),
        "wednesday" to (wednesday?.toString() ?: ""),
        "thursday" to (thursday?.toString() ?: ""),
        "friday" to (friday?.toString() ?: ""),
        "saturday" to (saturday?.toString() ?: ""),
        "sunday" to (sunday?.toString() ?: ""),
    )

    fun setHour(day: String, value: String) {
        _state.value = _state.value.copy(
            hours = _state.value.hours + (day to value),
            scheduleSaved = false,
        )
    }

    fun applySchedulePreset(sixDays: Boolean) {
        val weekend = if (sixDays) mapOf("saturday" to "6", "sunday" to "") else mapOf("saturday" to "", "sunday" to "")
        _state.value = _state.value.copy(
            hours = mapOf(
                "monday" to "8",
                "tuesday" to "8",
                "wednesday" to "8",
                "thursday" to "8",
                "friday" to "8",
            ) + weekend,
            scheduleSaved = false,
        )
    }

    fun saveSchedule(employeeId: String) {
        val hours = _state.value.hours
        _state.value = _state.value.copy(scheduleSaving = true, scheduleError = null, scheduleSaved = false)
        val body = HrWorkScheduleDto(
            effectiveFrom = null,
            monday = hours["monday"]?.toDoubleOrNull(),
            tuesday = hours["tuesday"]?.toDoubleOrNull(),
            wednesday = hours["wednesday"]?.toDoubleOrNull(),
            thursday = hours["thursday"]?.toDoubleOrNull(),
            friday = hours["friday"]?.toDoubleOrNull(),
            saturday = hours["saturday"]?.toDoubleOrNull(),
            sunday = hours["sunday"]?.toDoubleOrNull(),
        )
        viewModelScope.launch {
            runCatching { repo.setWorkSchedule(employeeId, body) }
                .onSuccess {
                    _state.value = _state.value.copy(scheduleSaving = false, scheduleSaved = true)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(scheduleSaving = false, scheduleError = failure.toUserMessage())
                }
        }
    }

    /* ------------------------------ İş tarixçəsi ------------------------------ */

    private fun loadWork(employeeId: String) {
        _state.value = _state.value.copy(workError = null)
        viewModelScope.launch {
            runCatching { repo.workHistories(employeeId) }
                .onSuccess { list ->
                    _state.value = _state.value.copy(workHistories = list.sortedByDescending { it.startDate })
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(workError = failure.toUserMessage())
                }
        }
    }

    fun openAddWork() {
        _state.value = _state.value.copy(addWorkOpen = true, addWorkError = null)
    }

    fun closeAddWork() {
        _state.value = _state.value.copy(addWorkOpen = false, addWorkError = null, workSaving = false)
    }

    fun addWorkHistory(
        employeeId: String,
        companyName: String,
        position: String,
        startDate: String,
        endDate: String,
        notes: String,
    ) {
        if (companyName.isBlank() || position.isBlank() || startDate.isBlank()) {
            _state.value = _state.value.copy(addWorkError = "Təşkilat, vəzifə və başlanğıc tarixi doldurulmalıdır.")
            return
        }
        _state.value = _state.value.copy(workSaving = true, addWorkError = null)
        viewModelScope.launch {
            runCatching {
                repo.addWorkHistory(
                    HrWorkHistoryBody(
                        employeeId = employeeId,
                        companyName = companyName.trim(),
                        position = position.trim(),
                        startDate = isoDate(startDate),
                        endDate = isoDate(endDate),
                        notes = notes.trim().ifBlank { null },
                    ),
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    workSaving = false,
                    addWorkOpen = false,
                    actionMessage = "İş yeri əlavə edildi.",
                )
                loadWork(employeeId)
            }.onFailure { failure ->
                _state.value = _state.value.copy(workSaving = false, addWorkError = failure.toUserMessage())
            }
        }
    }

    fun deleteWorkHistory(employeeId: String, id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteWorkHistory(id) }
                .onSuccess {
                    _state.value = _state.value.copy(actionMessage = "Qeyd silindi.")
                    loadWork(employeeId)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(workError = failure.toUserMessage())
                }
        }
    }

    /* ------------------------------ Təhsil ------------------------------ */

    private fun loadEducation(employeeId: String) {
        _state.value = _state.value.copy(educationError = null)
        viewModelScope.launch {
            runCatching { repo.educationHistories(employeeId) }
                .onSuccess { list ->
                    _state.value = _state.value.copy(education = list.sortedByDescending { it.startDate })
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(educationError = failure.toUserMessage())
                }
        }
    }

    private fun loadInstitutions() {
        viewModelScope.launch {
            runCatching { repo.institutions(page = 0, pageSize = 200) }
                .onSuccess { page -> _state.value = _state.value.copy(institutions = page.items) }
        }
    }

    fun openAddEducation() {
        _state.value = _state.value.copy(addEducationOpen = true, addEducationError = null)
    }

    fun closeAddEducation() {
        _state.value = _state.value.copy(addEducationOpen = false, addEducationError = null, educationSaving = false)
    }

    fun addEducation(
        employeeId: String,
        institutionId: String,
        level: Int,
        faculty: String,
        specialty: String,
        startDate: String,
        endDate: String,
        diplomaNumber: String,
        registerNumber: String,
    ) {
        if (institutionId.isBlank() || faculty.isBlank() || startDate.isBlank()) {
            _state.value = _state.value.copy(addEducationError = "Ocaq, fakültə və başlanğıc tarixi doldurulmalıdır.")
            return
        }
        _state.value = _state.value.copy(educationSaving = true, addEducationError = null)
        viewModelScope.launch {
            runCatching {
                repo.addEducationHistory(
                    HrEducationHistoryBody(
                        employeeId = employeeId,
                        educationalInstitutionId = institutionId,
                        educationLevel = level,
                        faculty = faculty.trim(),
                        specialty = specialty.trim().ifBlank { null },
                        startDate = isoDate(startDate),
                        endDate = isoDate(endDate),
                        diplomaNumber = diplomaNumber.trim().ifBlank { null },
                        registerNumber = registerNumber.trim().ifBlank { null },
                    ),
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    educationSaving = false,
                    addEducationOpen = false,
                    actionMessage = "Təhsil qeydi əlavə edildi.",
                )
                loadEducation(employeeId)
            }.onFailure { failure ->
                _state.value = _state.value.copy(educationSaving = false, addEducationError = failure.toUserMessage())
            }
        }
    }

    fun deleteEducation(employeeId: String, id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteEducationHistory(id) }
                .onSuccess {
                    _state.value = _state.value.copy(actionMessage = "Təhsil qeydi silindi.")
                    loadEducation(employeeId)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(educationError = failure.toUserMessage())
                }
        }
    }

    /* ------------------------------ Məzuniyyət balansı ------------------------------ */

    private fun loadBalances(employeeId: String) {
        _state.value = _state.value.copy(balanceLoading = true, balanceError = null)
        viewModelScope.launch {
            runCatching { repo.vacationBalances(employeeId) }
                .onSuccess { list ->
                    if (list.isEmpty()) {
                        recalculateBalance(employeeId, auto = true)
                    } else {
                        _state.value = _state.value.copy(balances = list, balanceLoading = false)
                    }
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(balanceLoading = false, balanceError = failure.toUserMessage())
                }
        }
    }

    fun recalculateBalance(employeeId: String, auto: Boolean = false) {
        _state.value = _state.value.copy(recalculating = true, balanceError = null, balanceNotice = null)
        viewModelScope.launch {
            runCatching { repo.recalculateVacationBalance(employeeId) }
                .then {
                    runCatching { repo.vacationBalances(employeeId) }
                }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        balances = list,
                        balanceLoading = false,
                        recalculating = false,
                        balanceNotice = if (auto) null else "Balans yenidən hesablandı.",
                    )
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        recalculating = false,
                        balanceLoading = false,
                        balanceError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }
}

/** `Result.then` köməkçisi — birinci sorğu uğurludursa ikincisini işlədir. */
private inline fun <T, R> Result<T>.then(next: (T) -> Result<R>): Result<R> =
    fold(onSuccess = { next(it) }, onFailure = { Result.failure(it) })
