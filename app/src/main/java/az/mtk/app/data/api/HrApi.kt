package az.mtk.app.data.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/*
 * HR modulu — MTK-UI app/lib/api/hr.ts ilə birebir əlaqə.
 * Bütün HR endpoint-ləri Envelope ({ data, message }) qaytarır; axtarış
 * endpoint-ləri POST SearchBody qəbul edib HrPaged qaytarır.
 */

// ---- Paged -----------------------------------------------------------------

data class HrPaged<T>(
    val data: List<T>? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
    val pageCount: Int? = null,
    val totalCount: Int? = null,
)

// ---- Lüğətlər ---------------------------------------------------------------

data class HrJobDto(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class HrInstitutionDto(
    val id: String,
    val name: String,
    val type: Int? = null,
)

data class HrLaborCodeCaseDto(
    val id: String,
    val code: String,
    val name: String,
    val parentId: String? = null,
    val parentName: String? = null,
    val isActive: Boolean? = null,
)

data class HrNameBody(val name: String)
data class HrInstitutionBody(val name: String, val type: Int)

// ---- İşçilər ----------------------------------------------------------------

/** hr.ts EmployeeDetail — Mövcud EmployeeDetailDto-dan daha genişdir, ona görə ayrı DTO. */
data class HrEmployeeDetailDto(
    val id: String,
    val registerNumber: Int? = null,
    val name: String? = null,
    val surname: String? = null,
    val fathersName: String? = null,
    val nationality: String? = null,
    val gender: Int? = null,
    val birthDate: String? = null,
    val finCode: String? = null,
    val idCardNumber: String? = null,
    val socialSecurityNumber: String? = null,
    val contractNumber: String? = null,
    val salaryBankName: String? = null,
    val employeeBankAccountNumber: String? = null,
    val baseSalary: Double? = null,
    val maritalStatus: Int? = null,
    val numberOfChildren: Int? = null,
    val childrenUnder14Count: Int? = null,
    val militaryService: Int? = null,
    val veteran: Boolean? = null,
    val disability: Boolean? = null,
    val isKarabakhWorker: Boolean? = null,
    val isSingleParent: Boolean? = null,
    val hasDisabledChild: Boolean? = null,
    val education: Int? = null,
    val phoneNumber: String? = null,
    val homePhoneNumber: String? = null,
    val email: String? = null,
    val registeredAddress: String? = null,
    val currentAddress: String? = null,
    val workingDays: Int? = null,
    val vacationDays: Int? = null,
    val totalWorkExperience: WorkExperienceDto? = null,
    val organizationWorkExperience: WorkExperienceDto? = null,
    val startWorkDate: String? = null,
    val job: NamedRef? = null,
    val createdBy: NamedRef? = null,
    val isActive: Int? = null,
    val employmentType: Int? = null,
    val createdAt: String? = null,
)

data class HrCreatedDto(val id: String?)

data class HrWorkScheduleDto(
    val id: String? = null,
    val effectiveFrom: String? = null,
    val monday: Double? = null,
    val tuesday: Double? = null,
    val wednesday: Double? = null,
    val thursday: Double? = null,
    val friday: Double? = null,
    val saturday: Double? = null,
    val sunday: Double? = null,
)

// ---- İş tarixçəsi / təhsil --------------------------------------------------

data class HrWorkHistoryDto(
    val id: String,
    val companyName: String,
    val position: String,
    val startDate: String,
    val endDate: String? = null,
    val notes: String? = null,
    val duration: WorkExperienceDto? = null,
)

data class HrEducationHistoryDto(
    val id: String,
    val educationalInstitutionId: String,
    val educationalInstitutionName: String? = null,
    val educationLevel: Int? = null,
    val faculty: String,
    val specialty: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val diplomaNumber: String? = null,
    val registerNumber: String? = null,
)

data class HrWorkHistoryBody(
    val employeeId: String,
    val companyName: String,
    val position: String,
    val startDate: String?,
    val endDate: String?,
    val notes: String?,
)

data class HrEducationHistoryBody(
    val employeeId: String,
    val educationalInstitutionId: String,
    val educationLevel: Int,
    val faculty: String,
    val specialty: String?,
    val startDate: String?,
    val endDate: String?,
    val diplomaNumber: String?,
    val registerNumber: String?,
)

// ---- Məzuniyyət balansı -----------------------------------------------------

data class HrVacationBalanceDto(
    val employeeId: String,
    val workYearStart: String,
    val workYearEnd: String,
    val baseEntitledDays: Int? = null,
    val experienceBonusDays: Int? = null,
    val karabakhWorkerDays: Int? = null,
    val childrenUnder14Days: Int? = null,
    val disabledChildDays: Int? = null,
    val additionalDays: Int? = null,
    val totalEntitledDays: Int? = null,
    val usedDays: Int? = null,
    val compensatedDays: Int? = null,
    val remainingDays: Int? = null,
)

// ---- Əmrlər -----------------------------------------------------------------

data class HrOrderItemDto(
    val id: String,
    val orderNumber: Int? = null,
    val type: Int? = null,
    val employee: NamedRef? = null,
    val jobApplicant: NamedRef? = null,
    val createdBy: NamedRef? = null,
    val createdAt: String? = null,
)

/** Əmr növləri (hr.ts ORDER_KINDS). */
object HrOrderKind {
    const val EMPLOYMENT = 1
    const val CHANGE_OF_POSITION = 2
    const val UNEXCUSED_ABSENCE = 3
    const val WARNING = 4
    const val VACATION_COMPENSATION = 5
    const val UNPAID_LEAVE = 6
    const val VACATION = 7
    const val EDUCATION_LEAVE = 8
    const val REPRIMAND = 9
    const val SEVERE_REPRIMAND = 10
    const val BONUS = 11
    const val WORK_ON_NON_WORKDAY = 13
    const val EMPLOYMENT_STATUS_CHANGE = 14
    const val VACATION_RETURN = 15

    val LABELS: Map<Int, String> = mapOf(
        EMPLOYMENT to "İşə qəbul",
        CHANGE_OF_POSITION to "Vəzifə dəyişikliyi",
        UNEXCUSED_ABSENCE to "İcazəsiz qeyb",
        WARNING to "Xəbərdarlıq",
        VACATION_COMPENSATION to "Məzuniyyət əvəzi",
        UNPAID_LEAVE to "Ödənişsiz məzuniyyət",
        VACATION to "Əmək məzuniyyəti",
        EDUCATION_LEAVE to "Təhsil məzuniyyəti",
        REPRIMAND to "Töhmət",
        SEVERE_REPRIMAND to "Ciddi töhmət",
        BONUS to "Mükafat",
        WORK_ON_NON_WORKDAY to "İstirahət/bayram günü işi",
        EMPLOYMENT_STATUS_CHANGE to "İş rejimi dəyişikliyi",
        VACATION_RETURN to "Məzuniyyətdən geri çağırma",
    )

    /** controller marşrutu — PDF export üçün. */
    val ROUTES: Map<Int, String> = mapOf(
        EMPLOYMENT to "employmentorders",
        CHANGE_OF_POSITION to "ordersforchangeofposition",
        UNEXCUSED_ABSENCE to "unexcusedabsences",
        WARNING to "warnings",
        REPRIMAND to "warnings",
        SEVERE_REPRIMAND to "warnings",
        VACATION_COMPENSATION to "compensationorders",
        UNPAID_LEAVE to "unpaidleaveorders",
        VACATION to "vacationorders",
        EDUCATION_LEAVE to "educationleaveorders",
        BONUS to "bonusorders",
        WORK_ON_NON_WORKDAY to "workonnonworkdayorders",
        EMPLOYMENT_STATUS_CHANGE to "employmentstatuschangeorders",
        VACATION_RETURN to "vacationreturnorders",
    )

    fun label(type: Int?): String = LABELS[type] ?: (type?.toString() ?: "—")
    fun route(type: Int?): String = ROUTES[type] ?: "orders"
}

/** Ərizəsiz verilən əmr növləri (hr.ts DirectOrderKind). */
object HrDirectOrderKind {
    const val BONUS = "Bonus"
    const val UNEXCUSED_ABSENCE = "UnexcusedAbsence"
    const val WARNING = "Warning"
    const val WORK_ON_NON_WORKDAY = "WorkOnNonWorkday"

    val ROUTES = mapOf(
        BONUS to "bonusorders",
        UNEXCUSED_ABSENCE to "unexcusedabsences",
        WARNING to "warnings",
        WORK_ON_NON_WORKDAY to "workonnonworkdayorders",
    )
}

// ---- Tabel ------------------------------------------------------------------

data class HrTimesheetDayDto(
    val date: String? = null,
    val day: Int? = null,
    val dayType: String? = null,
    val dayCode: String? = null,
    val workedHours: Double? = null,
)

data class HrEmployeeTimesheetDto(
    val employee: NamedRef? = null,
    val position: NamedRef? = null,
    val registerNumber: Int? = null,
    val days: List<HrTimesheetDayDto>? = null,
    val actualWorkingDays: Int? = null,
    val actualWorkedHours: Double? = null,
    val normWorkingDays: Int? = null,
    val normWorkedHours: Double? = null,
    val annualLeaveDays: Int? = null,
    val socialLeaveDays: Int? = null,
    val educationLeaveDays: Int? = null,
    val unpaidLeaveDays: Int? = null,
    val sickLeaveDays: Int? = null,
    val businessTripDays: Int? = null,
    val holidayDays: Int? = null,
    val nonWorkingDays: Int? = null,
)

data class HrTimesheetDto(
    val year: Int? = null,
    val month: Int? = null,
    val daysInMonth: Int? = null,
    val employees: List<HrEmployeeTimesheetDto>? = null,
)

// ---- Təqvim -----------------------------------------------------------------

data class HrCalendarDayDto(
    val id: String,
    val name: String,
    val date: String,
    val year: Int? = null,
    val dayType: Int? = null,
    val reason: String? = null,
    val applicableWorkingDays: Int? = null,
)

data class HrCalendarMonthDto(
    val month: Int? = null,
    val monthName: String? = null,
    val totalDays: Int? = null,
    val workingDays: Int? = null,
    val nonWorkingDays: Int? = null,
    val nonWorkingDaysList: List<HrCalendarDayDto>? = null,
    val days: List<HrCalendarDayDto>? = null,
)

data class HrCalendarYearDto(
    val year: Int? = null,
    val months: List<HrCalendarMonthDto>? = null,
    val yearTotal: HrCalendarYearTotalDto? = null,
)

data class HrCalendarYearTotalDto(
    val totalDays: Int? = null,
    val workingDays: Int? = null,
    val nonWorkingDays: Int? = null,
)

data class HrCalendarDayBody(
    val name: String,
    val date: String,
    val dayType: Int,
    val reason: String?,
    val applicableWorkingDays: Int?,
)

data class HrBulkCalendarBody(val days: List<HrCalendarDayBody>)

data class HrBulkCalendarResultDto(
    val createdCount: Int? = null,
    val skippedDates: List<String>? = null,
)

// ---- Fayl əlavələri ---------------------------------------------------------

data class HrFileAttachmentDto(
    val id: String,
    val fileName: String,
    val mimeType: String? = null,
    val createdAt: String? = null,
    val documentType: Int? = null,
)

// ---- Retrofit ---------------------------------------------------------------

interface HrService {

    // Əmrlər
    @POST("api/hr/orders/search")
    suspend fun ordersSearch(@Body body: SearchBody): Envelope<HrPaged<HrOrderItemDto>>

    @POST("api/hr/bonusorders")
    suspend fun createBonusOrder(@Body body: JsonObject): Envelope<JsonElement>

    @POST("api/hr/unexcusedabsences")
    suspend fun createUnexcusedAbsence(@Body body: JsonObject): Envelope<JsonElement>

    @POST("api/hr/warnings")
    suspend fun createWarning(@Body body: JsonObject): Envelope<JsonElement>

    @POST("api/hr/workonnonworkdayorders")
    suspend fun createWorkOnNonWorkday(@Body body: JsonObject): Envelope<JsonElement>

    // Tabel
    @GET("api/hr/timesheets")
    suspend fun timesheet(@Query("year") year: Int, @Query("month") month: Int): Envelope<HrTimesheetDto>

    // Təqvim
    @GET("api/hr/calendardays")
    suspend fun calendarYear(
        @Query("year") year: Int,
        @Query("workingDays") workingDays: Int,
    ): Envelope<HrCalendarYearDto>

    @POST("api/hr/calendardays")
    suspend fun createCalendarDay(@Body body: HrCalendarDayBody): Envelope<HrCalendarDayDto>

    @PATCH("api/hr/calendardays/{id}")
    suspend fun updateCalendarDay(@Path("id") id: String, @Body body: HrCalendarDayBody): Envelope<HrCalendarDayDto>

    @DELETE("api/hr/calendardays/{id}")
    suspend fun deleteCalendarDay(@Path("id") id: String): Envelope<JsonElement>

    @POST("api/hr/calendardays/bulk")
    suspend fun bulkCreateCalendarDays(@Body body: HrBulkCalendarBody): Envelope<HrBulkCalendarResultDto>

    // Lüğətlər
    @POST("api/hr/jobs/search")
    suspend fun jobsSearch(@Body body: SearchBody): Envelope<HrPaged<HrJobDto>>

    @POST("api/hr/jobs")
    suspend fun createJob(@Body body: HrNameBody): Envelope<HrJobDto>

    @PATCH("api/hr/jobs/{id}")
    suspend fun updateJob(@Path("id") id: String, @Body body: HrNameBody): Envelope<HrJobDto>

    @DELETE("api/hr/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String): Envelope<JsonElement>

    @POST("api/hr/educationalinstitutions/search")
    suspend fun institutionsSearch(@Body body: SearchBody): Envelope<HrPaged<HrInstitutionDto>>

    @POST("api/hr/educationalinstitutions")
    suspend fun createInstitution(@Body body: HrInstitutionBody): Envelope<HrInstitutionDto>

    @PATCH("api/hr/educationalinstitutions/{id}")
    suspend fun updateInstitution(@Path("id") id: String, @Body body: HrInstitutionBody): Envelope<HrInstitutionDto>

    @DELETE("api/hr/educationalinstitutions/{id}")
    suspend fun deleteInstitution(@Path("id") id: String): Envelope<JsonElement>

    @POST("api/hr/laborcodecases/search")
    suspend fun laborCodeCasesSearch(@Body body: SearchBody): Envelope<HrPaged<HrLaborCodeCaseDto>>

    // İşçilər
    @GET("api/hr/employees/{id}")
    suspend fun employeeDetail(@Path("id") id: String): Envelope<HrEmployeeDetailDto>

    @POST("api/hr/employees")
    suspend fun createEmployee(@Body body: JsonObject): Envelope<HrCreatedDto>

    @PATCH("api/hr/employees/{id}")
    suspend fun updateEmployee(@Path("id") id: String, @Body body: JsonObject): Envelope<JsonElement>

    @DELETE("api/hr/employees/{id}")
    suspend fun deleteEmployee(@Path("id") id: String): Envelope<JsonElement>

    @GET("api/hr/employeeworkhistories/employee/{id}")
    suspend fun workHistories(@Path("id") employeeId: String): Envelope<JsonElement>

    @POST("api/hr/employeeworkhistories")
    suspend fun addWorkHistory(@Body body: HrWorkHistoryBody): Envelope<JsonElement>

    @DELETE("api/hr/employeeworkhistories/{id}")
    suspend fun deleteWorkHistory(@Path("id") id: String): Envelope<JsonElement>

    @GET("api/hr/employeeeducationhistories/employee/{id}")
    suspend fun educationHistories(@Path("id") employeeId: String): Envelope<JsonElement>

    @POST("api/hr/employeeeducationhistories")
    suspend fun addEducationHistory(@Body body: HrEducationHistoryBody): Envelope<JsonElement>

    @DELETE("api/hr/employeeeducationhistories/{id}")
    suspend fun deleteEducationHistory(@Path("id") id: String): Envelope<JsonElement>

    @GET("api/hr/employeeworkschedules/employee/{id}")
    suspend fun workSchedule(@Path("id") employeeId: String): Envelope<HrWorkScheduleDto>

    @PATCH("api/hr/employeeworkschedules/employee/{id}")
    suspend fun setWorkSchedule(@Path("id") employeeId: String, @Body body: HrWorkScheduleDto): Envelope<JsonElement>

    // Məzuniyyət balansı
    @GET("api/hr/vacationbalances/{id}/all")
    suspend fun vacationBalances(@Path("id") employeeId: String): Envelope<List<HrVacationBalanceDto>>

    @POST("api/hr/vacationbalances/{id}/recalculate")
    suspend fun recalculateVacationBalance(@Path("id") employeeId: String): Envelope<HrVacationBalanceDto>

    // Ərizələr — marşrut növə görə dəyişir, path ilə ötürülür
    @POST("api/hr/{route}")
    suspend fun createApplication(@Path("route", encoded = true) route: String, @Body body: JsonObject): Envelope<JsonElement>

    @DELETE("api/hr/{route}/{id}")
    suspend fun deleteApplication(
        @Path("route", encoded = true) route: String,
        @Path("id") id: String,
    ): Envelope<JsonElement>

    @POST("api/hr/{route}/{id}/convert-to-order")
    suspend fun convertApplication(
        @Path("route", encoded = true) route: String,
        @Path("id") id: String,
    ): Envelope<JsonElement>

    @POST("api/hr/jobapplications/{id}/convert-to-employment-order")
    suspend fun convertJobApplication(@Path("id") id: String, @Body body: JsonObject): Envelope<JsonElement>

    // Fayl əlavələri
    @POST("api/hr/fileattachments/search")
    suspend fun attachmentsSearch(@Body body: SearchBody): Envelope<HrPaged<HrFileAttachmentDto>>
}

/** Ərizə növləri (hr.ts APPLICATION_KINDS). */
object HrApplicationKind {
    const val JOB_APPLICATION = 1
    const val CHANGE_OF_POSITION = 2
    const val VACATION_COMPENSATION = 3
    const val UNPAID_LEAVE = 4
    const val VACATION = 5
    const val EDUCATION_LEAVE = 6
    const val EMPLOYMENT_STATUS_CHANGE = 7
    const val VACATION_RETURN = 8

    val LABELS: Map<Int, String> = mapOf(
        JOB_APPLICATION to "Vakansiyaya müraciət",
        CHANGE_OF_POSITION to "Vəzifə dəyişikliyi",
        VACATION_COMPENSATION to "Məzuniyyət əvəzi",
        UNPAID_LEAVE to "Ödənişsiz məzuniyyət",
        VACATION to "Əmək məzuniyyəti",
        EDUCATION_LEAVE to "Təhsil məzuniyyəti",
        EMPLOYMENT_STATUS_CHANGE to "İş rejimi dəyişikliyi",
        VACATION_RETURN to "Məzuniyyətdən geri çağırma",
    )

    val ROUTES: Map<Int, String> = mapOf(
        JOB_APPLICATION to "jobapplications",
        CHANGE_OF_POSITION to "applicationsforchangeofposition",
        VACATION_COMPENSATION to "vacationcompensationapplications",
        UNPAID_LEAVE to "unpaidleaveapplications",
        VACATION to "vacationapplications",
        EDUCATION_LEAVE to "educationleaveapplications",
        EMPLOYMENT_STATUS_CHANGE to "employmentstatuschangeapplications",
        VACATION_RETURN to "vacationreturnapplications",
    )

    fun label(type: Int?): String = LABELS[type] ?: "Müraciət"
    fun route(type: Int): String = ROUTES[type] ?: "jobapplications"
}

// ---- Repository -------------------------------------------------------------

/** Lazily-wired shared HrRepository (AppGraph-a toxunmadan). */
object HrApi {
    val repository: HrRepository by lazy { HrRepository(az.mtk.app.AppGraph.service(HrService::class.java)) }
}

class HrRepository(private val api: HrService) {

    // Əmrlər
    suspend fun orders(page: Int, pageSize: Int = 20, search: String? = null, types: List<Int>? = null): Page<HrOrderItemDto> {
        val filters = types?.takeIf { it.isNotEmpty() }
            ?.let { listOf(QueryFilter("Type", Comparison.IN, it)) }
        val env = api.ordersSearch(
            SearchBody(
                filters = filters,
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Əmrlər")
        return Page(payload.data.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun createDirectOrder(kind: String, body: JsonObject) {
        when (kind) {
            HrDirectOrderKind.BONUS -> api.createBonusOrder(body)
            HrDirectOrderKind.UNEXCUSED_ABSENCE -> api.createUnexcusedAbsence(body)
            HrDirectOrderKind.WARNING -> api.createWarning(body)
            HrDirectOrderKind.WORK_ON_NON_WORKDAY -> api.createWorkOnNonWorkday(body)
            else -> throw IllegalArgumentException("Bilinməyən əmr növü: $kind")
        }.unwrap("Əmr")
    }

    // Tabel
    suspend fun timesheet(year: Int, month: Int): HrTimesheetDto =
        api.timesheet(year, month).unwrap("Tabel")

    // Təqvim
    suspend fun calendarYear(year: Int, workingDays: Int): HrCalendarYearDto =
        api.calendarYear(year, workingDays).unwrap("Təqvim")

    suspend fun createCalendarDay(body: HrCalendarDayBody) { api.createCalendarDay(body).unwrap("Təqvim günü") }
    suspend fun updateCalendarDay(id: String, body: HrCalendarDayBody) { api.updateCalendarDay(id, body).unwrap("Təqvim günü") }
    suspend fun deleteCalendarDay(id: String) { api.deleteCalendarDay(id) }
    suspend fun bulkCreateCalendarDays(days: List<HrCalendarDayBody>): HrBulkCalendarResultDto =
        api.bulkCreateCalendarDays(HrBulkCalendarBody(days)).unwrap("Toplu əlavə")

    // Lüğətlər
    suspend fun jobs(page: Int, pageSize: Int = 20, search: String? = null): Page<HrJobDto> {
        val payload = api.jobsSearch(
            SearchBody(
                sortCriteria = SortCriteria("Name", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        ).unwrap("Vəzifələr")
        return Page(payload.data.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun createJob(name: String) { api.createJob(HrNameBody(name)).unwrap("Vəzifə") }
    suspend fun updateJob(id: String, name: String) { api.updateJob(id, HrNameBody(name)).unwrap("Vəzifə") }
    suspend fun deleteJob(id: String) { api.deleteJob(id) }

    suspend fun institutions(page: Int, pageSize: Int = 20, search: String? = null): Page<HrInstitutionDto> {
        val payload = api.institutionsSearch(
            SearchBody(
                sortCriteria = SortCriteria("Name", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        ).unwrap("Təhsil ocaqları")
        return Page(payload.data.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun createInstitution(name: String, type: Int) { api.createInstitution(HrInstitutionBody(name, type)).unwrap("Təhsil ocağı") }
    suspend fun updateInstitution(id: String, name: String, type: Int) {
        api.updateInstitution(id, HrInstitutionBody(name, type)).unwrap("Təhsil ocağı")
    }

    suspend fun deleteInstitution(id: String) { api.deleteInstitution(id) }

    suspend fun laborCodeCases(pageSize: Int = 100): List<HrLaborCodeCaseDto> {
        val payload = api.laborCodeCasesSearch(SearchBody(page = 0, pageSize = pageSize)).unwrap("Əmək məcəlləsi")
        return payload.data.orEmpty()
    }

    // İşçilər
    suspend fun employeeDetail(id: String): HrEmployeeDetailDto =
        api.employeeDetail(id).data ?: throw ApiException("İşçi tapılmadı")

    suspend fun createEmployee(body: JsonObject): String =
        api.createEmployee(body).unwrap("İşçi").id ?: throw ApiException("İşçi yaradıla bilmədi")

    suspend fun updateEmployeeSection(id: String, patch: JsonObject) { api.updateEmployee(id, patch) }

    suspend fun deleteEmployee(id: String) { api.deleteEmployee(id) }

    // İş tarixçəsi (backend ya birbaşa massiv, ya da { data: [...] } qaytarır)
    suspend fun workHistories(employeeId: String): List<HrWorkHistoryDto> =
        parseHistoryArray(api.workHistories(employeeId).data).map {
            GsonHolder.gson.fromJson(it, HrWorkHistoryDto::class.java)
        }

    suspend fun addWorkHistory(body: HrWorkHistoryBody) { api.addWorkHistory(body) }
    suspend fun deleteWorkHistory(id: String) { api.deleteWorkHistory(id) }

    suspend fun educationHistories(employeeId: String): List<HrEducationHistoryDto> =
        parseHistoryArray(api.educationHistories(employeeId).data).map {
            GsonHolder.gson.fromJson(it, HrEducationHistoryDto::class.java)
        }

    suspend fun addEducationHistory(body: HrEducationHistoryBody) { api.addEducationHistory(body) }
    suspend fun deleteEducationHistory(id: String) { api.deleteEducationHistory(id) }

    // İş qrafiki
    suspend fun workSchedule(employeeId: String): HrWorkScheduleDto =
        api.workSchedule(employeeId).data ?: HrWorkScheduleDto()

    suspend fun setWorkSchedule(employeeId: String, schedule: HrWorkScheduleDto) {
        api.setWorkSchedule(employeeId, schedule)
    }

    // Məzuniyyət balansı
    suspend fun vacationBalances(employeeId: String): List<HrVacationBalanceDto> =
        api.vacationBalances(employeeId).data.orEmpty()

    suspend fun recalculateVacationBalance(employeeId: String) {
        api.recalculateVacationBalance(employeeId).unwrap("Balans")
    }

    // Ərizələr
    suspend fun createApplication(type: Int, body: JsonObject) {
        api.createApplication(HrApplicationKind.route(type), body).unwrap("Ərizə")
    }

    suspend fun deleteApplication(type: Int, id: String) {
        api.deleteApplication(HrApplicationKind.route(type), id)
    }

    suspend fun convertApplication(type: Int, id: String, jobApplicationBody: JsonObject? = null) {
        if (type == HrApplicationKind.JOB_APPLICATION) {
            api.convertJobApplication(id, jobApplicationBody ?: JsonObject()).unwrap("Əmr")
        } else {
            api.convertApplication(HrApplicationKind.route(type), id).unwrap("Əmr")
        }
    }

    // Fayl əlavələri
    suspend fun attachments(fkColumn: String, id: String): List<HrFileAttachmentDto> {
        val payload = api.attachmentsSearch(
            SearchBody(
                filters = listOf(QueryFilter(fkColumn, Comparison.EQUALS, id)),
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                page = 0,
                pageSize = 100,
            ),
        ).unwrap("Fayllar")
        return payload.data.orEmpty()
    }

    private fun parseHistoryArray(raw: JsonElement?): com.google.gson.JsonArray = when (raw) {
        is com.google.gson.JsonArray -> raw
        is com.google.gson.JsonObject -> raw.getAsJsonArray("data") ?: com.google.gson.JsonArray()
        else -> com.google.gson.JsonArray()
    }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private object GsonHolder {
        val gson = com.google.gson.Gson()
    }
}
