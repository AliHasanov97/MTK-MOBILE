package az.mtk.app.ui.screens.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.Comparison
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrEmployeeListItemDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrJobDto
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.util.toUserMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** HR modulunun altı bölməsi — MTK-UI panelindəki eyni adlı səhifələr. */
enum class HrTab(val label: String) {
    EMPLOYEES("İşçilər"),
    APPLICATIONS("Ərizələr"),
    ORDERS("Əmrlər"),
    TIMESHEET("Tabel"),
    CALENDAR("Təqvim"),
    DICTIONARIES("Lüğətlər"),
}

/**
 * Yeni işçi forması — bütün sahələr mətn kimi saxlanılır, göndərmə zamanı
 * `hr.ts`-dəki `employeeBody` ilə eyni tiplərə çevrilir.
 */
data class HrEmployeeForm(
    val surname: String = "",
    val name: String = "",
    val fathersName: String = "",
    val gender: Int = 1,
    val birthDate: String = "",
    val nationality: String = "",
    val finCode: String = "",
    val idCardNumber: String = "",
    val socialSecurityNumber: String = "",
    val maritalStatus: String = "",
    val numberOfChildren: String = "",
    val childrenUnder14Count: String = "",
    val militaryService: String = "",
    val education: String = "",
    val veteran: Boolean = false,
    val disability: Boolean = false,
    val isKarabakhWorker: Boolean = false,
    val isSingleParent: Boolean = false,
    val hasDisabledChild: Boolean = false,
    val phoneNumber: String = "",
    val homePhoneNumber: String = "",
    val email: String = "",
    val registeredAddress: String = "",
    val currentAddress: String = "",
    val jobId: String = "",
    val startWorkDate: String = "",
    val employmentType: Int = 1,
    val isActive: Int = 1,
    val workingDays: String = "1",
    val vacationDays: String = "21",
    val contractNumber: String = "",
    val baseSalary: String = "0",
    val salaryBankName: String = "",
    val employeeBankAccountNumber: String = "",
) {
    fun validationError(): String? = when {
        surname.isBlank() -> "Soyad doldurulmalıdır."
        name.isBlank() -> "Ad doldurulmalıdır."
        fathersName.isBlank() -> "Ata adı doldurulmalıdır."
        jobId.isBlank() -> "Vəzifə seçilməlidir."
        startWorkDate.isBlank() -> "İşə başlama tarixi doldurulmalıdır."
        else -> null
    }

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("name", name.trim())
        addProperty("surname", surname.trim())
        addProperty("fathersName", fathersName.trim())
        addProperty("gender", gender)
        addProperty("jobId", jobId)
        addProperty("startWorkDate", isoDate(startWorkDate))
        text("nationality", nationality)
        text("birthDate", isoDate(birthDate))
        text("finCode", finCode)
        text("idCardNumber", idCardNumber)
        text("socialSecurityNumber", socialSecurityNumber)
        text("contractNumber", contractNumber)
        text("salaryBankName", salaryBankName)
        text("employeeBankAccountNumber", employeeBankAccountNumber)
        addProperty("baseSalary", baseSalary.trim().toDoubleOrNull() ?: 0.0)
        number("maritalStatus", maritalStatus)
        number("numberOfChildren", numberOfChildren)
        number("childrenUnder14Count", childrenUnder14Count)
        number("militaryService", militaryService)
        addProperty("veteran", veteran)
        addProperty("disability", disability)
        addProperty("isKarabakhWorker", isKarabakhWorker)
        addProperty("isSingleParent", isSingleParent)
        addProperty("hasDisabledChild", hasDisabledChild)
        number("education", education)
        text("phoneNumber", phoneNumber)
        text("homePhoneNumber", homePhoneNumber)
        text("email", email)
        text("registeredAddress", registeredAddress)
        text("currentAddress", currentAddress)
        number("workingDays", workingDays)
        number("vacationDays", vacationDays)
        addProperty("employmentType", employmentType)
    }

    private fun JsonObject.text(key: String, value: String?) {
        addProperty(key, value?.trim()?.ifBlank { null })
    }

    private fun JsonObject.number(key: String, value: String) {
        val parsed = value.trim().toIntOrNull()
        if (parsed != null) addProperty(key, parsed)
    }
}

internal fun isoDate(value: String): String? =
    value.trim().takeIf { it.isNotBlank() }?.let { "${it.take(10)}T00:00:00.000Z" }

internal fun dateInput(value: String?): String = value?.take(10).orEmpty()

/** Əmrlər jurnalının sıralama variantları. */
internal val HR_SORTS: List<Pair<String, Pair<String, Boolean>>> = listOf(
    "Tabel № ↑" to ("RegisterNumber" to true),
    "Tabel № ↓" to ("RegisterNumber" to false),
    "Soyad A→Z" to ("Surname" to true),
    "Soyad Z→A" to ("Surname" to false),
    "Ən yeni işə qəbul" to ("StartWorkDate" to false),
    "Ən köhnə işə qəbul" to ("StartWorkDate" to true),
)

class HrViewModel : ViewModel() {

    private val repo = HrApi.repository

    data class UiState(
        val tab: HrTab = HrTab.EMPLOYEES,
        val employees: List<HrEmployeeListItemDto>? = null,
        val employeesLoading: Boolean = false,
        val employeesError: String? = null,
        val page: Int = 1,
        val pageCount: Int = 1,
        val totalCount: Int = 0,
        val query: String = "",
        val sortIndex: Int = 0,
        val jobFilter: HrJobDto? = null,
        val typeFilter: Int? = null,
        val statusFilter: Int? = null,
        val jobs: List<HrJobDto> = emptyList(),
        val jobsLoading: Boolean = false,
        val formOpen: Boolean = false,
        val formSaving: Boolean = false,
        val formError: String? = null,
        val deleteTarget: HrEmployeeListItemDto? = null,
        val deleting: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    fun selectTab(tab: HrTab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun load() {
        loadEmployees(
            page = _state.value.page,
            query = _state.value.query,
            job = _state.value.jobFilter,
            type = _state.value.typeFilter,
            status = _state.value.statusFilter,
            sort = _state.value.sortIndex,
        )
        if (_state.value.jobs.isEmpty() && !_state.value.jobsLoading) loadJobs()
    }

    fun refresh() {
        load()
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            _state.value = _state.value.copy(page = 1)
            loadEmployees(page = 1, query = value.trim(), job = _state.value.jobFilter, type = _state.value.typeFilter, status = _state.value.statusFilter, sort = _state.value.sortIndex)
        }
    }

    fun setSort(index: Int) {
        _state.value = _state.value.copy(sortIndex = index, page = 1)
        reloadCurrent()
    }

    fun setJobFilter(job: HrJobDto?) {
        _state.value = _state.value.copy(jobFilter = job, page = 1)
        reloadCurrent()
    }

    fun setTypeFilter(type: Int?) {
        _state.value = _state.value.copy(typeFilter = type, page = 1)
        reloadCurrent()
    }

    fun setStatusFilter(status: Int?) {
        _state.value = _state.value.copy(statusFilter = status, page = 1)
        reloadCurrent()
    }

    fun clearFilters() {
        _state.value = _state.value.copy(query = "", jobFilter = null, typeFilter = null, statusFilter = null, page = 1)
        reloadCurrent()
    }

    fun goToPage(page: Int) {
        _state.value = _state.value.copy(page = page.coerceIn(1, _state.value.pageCount.coerceAtLeast(1)))
        reloadCurrent()
    }

    private fun reloadCurrent() {
        val s = _state.value
        loadEmployees(s.page, s.query.trim(), s.jobFilter, s.typeFilter, s.statusFilter, s.sortIndex)
    }

    private fun loadEmployees(
        page: Int,
        query: String,
        job: HrJobDto?,
        type: Int?,
        status: Int?,
        sort: Int,
    ) {
        val (column, ascending) = HR_SORTS.getOrElse(sort) { HR_SORTS[0] }.second
        _state.value = _state.value.copy(employeesLoading = true, employeesError = null)
        viewModelScope.launch {
            runCatching {
                repo.employeeList(
                    page = page - 1,
                    pageSize = 10,
                    search = query,
                    jobId = job?.id,
                    employmentType = type,
                    isActive = status,
                    sortColumn = column,
                    ascending = ascending,
                )
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    employees = result.items,
                    totalCount = result.totalCount,
                    pageCount = ((result.totalCount + 9) / 10).coerceAtLeast(1),
                    page = page,
                    employeesLoading = false,
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    employeesError = failure.toUserMessage(),
                    employeesLoading = false,
                )
            }
        }
    }

    private fun loadJobs() {
        _state.value = _state.value.copy(jobsLoading = true)
        viewModelScope.launch {
            runCatching { repo.jobs(page = 0, pageSize = 200) }
                .onSuccess { page -> _state.value = _state.value.copy(jobs = page.items, jobsLoading = false) }
                .onFailure { _state.value = _state.value.copy(jobsLoading = false) }
        }
    }

    fun openCreateForm() {
        _state.value = _state.value.copy(formOpen = true, formError = null)
    }

    fun closeForm() {
        _state.value = _state.value.copy(formOpen = false, formError = null, formSaving = false)
    }

    fun createEmployee(form: HrEmployeeForm) {
        val validation = form.validationError()
        if (validation != null) {
            _state.value = _state.value.copy(formError = validation)
            return
        }
        _state.value = _state.value.copy(formSaving = true, formError = null)
        viewModelScope.launch {
            runCatching { repo.createEmployee(form.toJson()) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        formSaving = false,
                        formOpen = false,
                        actionMessage = "İşçi yaradıldı.",
                        page = 1,
                    )
                    reloadCurrent()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(formSaving = false, formError = failure.toUserMessage())
                }
        }
    }

    fun requestDelete(item: HrEmployeeListItemDto) {
        _state.value = _state.value.copy(deleteTarget = item)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(deleteTarget = null, deleting = false)
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        _state.value = _state.value.copy(deleting = true, actionError = null)
        viewModelScope.launch {
            runCatching { repo.deleteEmployee(target.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        deleting = false,
                        deleteTarget = null,
                        actionMessage = "İşçi silindi.",
                    )
                    reloadCurrent()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        deleting = false,
                        deleteTarget = null,
                        actionError = failure.toUserMessage(),
                    )
                }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }
}

/* ------------------------------------------------------------------ */
/* Lüğətlər                                                           */
/* ------------------------------------------------------------------ */

enum class HrDictionarySection(val label: String) {
    JOBS("Vəzifələr"),
    INSTITUTIONS("Təhsil ocaqları"),
}

data class HrDictionaryState(
    val section: HrDictionarySection = HrDictionarySection.JOBS,
    val jobs: List<HrJobDto> = emptyList(),
    val institutions: List<az.mtk.app.data.api.HrInstitutionDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val page: Int = 1,
    val pageCount: Int = 1,
    val totalCount: Int = 0,
    val formOpen: Boolean = false,
    val editingId: String? = null,
    val formName: String = "",
    val formType: Int = 1,
    val saving: Boolean = false,
    val formError: String? = null,
    val deleteTargetId: String? = null,
    val deleteTargetName: String? = null,
    val deleting: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
)

class HrDictionariesViewModel : ViewModel() {

    private val repo = HrApi.repository
    private val _state = MutableStateFlow(HrDictionaryState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    fun load() = reload()

    fun selectSection(section: HrDictionarySection) {
        _state.value = _state.value.copy(section = section, page = 1, error = null, query = "")
        reload()
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            _state.value = _state.value.copy(page = 1)
            reload()
        }
    }

    fun goToPage(page: Int) {
        _state.value = _state.value.copy(page = page.coerceIn(1, _state.value.pageCount.coerceAtLeast(1)))
        reload()
    }

    private fun reload() {
        val s = _state.value
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (s.section) {
                HrDictionarySection.JOBS -> runCatching {
                    repo.jobs(page = s.page - 1, pageSize = 10, search = s.query.trim())
                }.onSuccess { page ->
                    _state.value = _state.value.copy(
                        loading = false,
                        jobs = page.items,
                        totalCount = page.totalCount,
                        pageCount = ((page.totalCount + 9) / 10).coerceAtLeast(1),
                    )
                }.onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.toUserMessage())
                }

                HrDictionarySection.INSTITUTIONS -> runCatching {
                    repo.institutions(page = s.page - 1, pageSize = 10, search = s.query.trim())
                }.onSuccess { page ->
                    _state.value = _state.value.copy(
                        loading = false,
                        institutions = page.items,
                        totalCount = page.totalCount,
                        pageCount = ((page.totalCount + 9) / 10).coerceAtLeast(1),
                    )
                }.onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.toUserMessage())
                }
            }
        }
    }

    fun openCreate() {
        _state.value = _state.value.copy(formOpen = true, editingId = null, formName = "", formType = 1, formError = null)
    }

    fun openEdit(id: String, name: String, type: Int = 1) {
        _state.value = _state.value.copy(formOpen = true, editingId = id, formName = name, formType = type, formError = null)
    }

    fun closeForm() {
        _state.value = _state.value.copy(formOpen = false, formError = null, saving = false)
    }

    fun setFormName(value: String) {
        _state.value = _state.value.copy(formName = value)
    }

    fun setFormType(value: Int) {
        _state.value = _state.value.copy(formType = value)
    }

    fun save() {
        val s = _state.value
        if (s.formName.isBlank()) {
            _state.value = s.copy(formError = "Ad doldurulmalıdır.")
            return
        }
        _state.value = s.copy(saving = true, formError = null)
        viewModelScope.launch {
            runCatching {
                when (s.section) {
                    HrDictionarySection.JOBS -> {
                        if (s.editingId == null) repo.createJob(s.formName.trim())
                        else repo.updateJob(s.editingId, s.formName.trim())
                    }
                    HrDictionarySection.INSTITUTIONS -> {
                        if (s.editingId == null) repo.createInstitution(s.formName.trim(), s.formType)
                        else repo.updateInstitution(s.editingId, s.formName.trim(), s.formType)
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    formOpen = false,
                    actionMessage = "Yadda saxlanıldı.",
                )
                reload()
            }.onFailure { failure ->
                _state.value = _state.value.copy(saving = false, formError = failure.toUserMessage())
            }
        }
    }

    fun requestDelete(id: String, name: String) {
        _state.value = _state.value.copy(deleteTargetId = id, deleteTargetName = name)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(deleteTargetId = null, deleteTargetName = null, deleting = false)
    }

    fun confirmDelete() {
        val id = _state.value.deleteTargetId ?: return
        val section = _state.value.section
        _state.value = _state.value.copy(deleting = true)
        viewModelScope.launch {
            runCatching {
                when (section) {
                    HrDictionarySection.JOBS -> repo.deleteJob(id)
                    HrDictionarySection.INSTITUTIONS -> repo.deleteInstitution(id)
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    deleting = false,
                    deleteTargetId = null,
                    deleteTargetName = null,
                    actionMessage = "Silindi.",
                )
                reload()
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    deleting = false,
                    deleteTargetId = null,
                    deleteTargetName = null,
                    actionError = failure.toUserMessage(),
                )
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }
}

/** Axtarış sorğularında istifadə olunan köməkçi (gələcək filtrlər üçün). */
internal fun hrEqualsFilter(column: String, value: String): QueryFilter =
    QueryFilter(column, Comparison.EQUALS, value)

internal fun hrSort(column: String, ascending: Boolean): SortCriteria =
    SortCriteria(column, if (ascending) Sort.ASCENDING else Sort.DESCENDING)

internal fun hrStatusLabel(status: String?): String = when (status) {
    "PendingApproval" -> "Gözləmədə"
    "ConvertedToOrder" -> "Əmrə çevrilib"
    null -> "—"
    else -> status
}

internal fun hrPaymentStatus(status: Int?): String = when (status) {
    1 -> "Gözləyir"
    2 -> "Ödənilib"
    else -> "—"
}

internal fun hrPayrollStatus(status: Int?): String = when (status) {
    1 -> "Qaralama"
    2 -> "Hesablanıb"
    3 -> "Təsdiqlənib"
    4 -> "Ödənilib"
    else -> "—"
}

internal fun hrPaymentMethodLabel(value: Int?): String =
    HrEnums.PAYMENT_METHODS.firstOrNull { it.value == value }?.label ?: (value?.toString() ?: "—")
