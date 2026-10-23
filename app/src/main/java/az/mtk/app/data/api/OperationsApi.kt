package az.mtk.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Əməliyyatlar modulunun əvvəllər Android tərəfdə olmayan endpoint-ləri:
 * əmək haqqı hesablamaları (payroll), məzuniyyət ödənişləri və müqavilə
 * xidmətinə görə birdəfəlik xərc. Qalan əməliyyatlar mövcud
 * ContractsApi / VendorsApi / CustomersApi / FinanceService üzərindən gedir.
 */

// ---- Əmək haqqı (PayrollRun / Payslip) --------------------------------------

object PayrollStatus {
    const val DRAFT = 1
    const val CALCULATED = 2
    const val APPROVED = 3
    const val PAID = 4

    fun label(value: Int?): String = when (value) {
        DRAFT -> "Qaralama"
        CALCULATED -> "Hesablanıb"
        APPROVED -> "Təsdiqlənib"
        PAID -> "Ödənilib"
        else -> "—"
    }
}

data class OpsPayslipDto(
    val employeeId: String?,
    val employeeFullName: String?,
    val baseSalary: Double?,
    val bonusAmount: Double?,
    val adjustmentAmount: Double?,
    val grossSalary: Double?,
    val finalNetSalary: Double?,
    val totalEmployerCost: Double?,
    val paidAt: String?,
    val paymentMethod: Int?,
)

data class OpsPayrollRunDto(
    val id: String?,
    val year: Int?,
    val month: Int?,
    val status: Int?,
    val calculatedAt: String?,
    val approvedAt: String?,
    val paidAt: String?,
    val totalGross: Double?,
    val totalNet: Double?,
    val totalEmployerCost: Double?,
    val payslips: List<OpsPayslipDto>?,
)

data class OpsPayrollPeriodRequest(val year: Int, val month: Int)

data class OpsPayMethodRequest(val paymentMethod: Int)

// ---- Məzuniyyət ödənişləri (VacationPay) ------------------------------------

object VacationPayStatus {
    const val PENDING = 1
    const val PAID = 2

    fun label(value: Int?): String = when (value) {
        PENDING -> "Gözləyir"
        PAID -> "Ödənilib"
        else -> "—"
    }
}

data class OpsVacationPayDto(
    val id: String?,
    val vacationOrderId: String?,
    val employeeId: String?,
    val employeeFullName: String?,
    val averageMonthlyWage: Double?,
    val averageDailyWage: Double?,
    val vacationDays: Double?,
    val totalAmount: Double?,
    val vacationStartDate: String?,
    val vacationEndDate: String?,
    val status: Int?,
    val dueDate: String?,
    val paidAt: String?,
    val paymentMethod: Int?,
    val createdAt: String?,
)

data class OpsVacationPayPage(
    val vacationPays: List<OpsVacationPayDto>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

// ---- Birdəfəlik müqavilə xidməti xərci --------------------------------------

data class OpsOneTimeExpenseRequest(
    val contractId: String,
    val contractServiceId: String,
    val amount: Double,
    val paymentMethod: Int,
    val notes: String?,
)

interface OperationsService {

    @POST("api/payroll-runs/calculate")
    suspend fun calculatePayroll(@Body body: OpsPayrollPeriodRequest): Envelope<OpsPayrollRunDto>

    @GET("api/payroll-runs/by-period")
    suspend fun payrollByPeriod(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): Envelope<OpsPayrollRunDto>

    @POST("api/payroll-runs/{id}/recalculate")
    suspend fun recalculatePayroll(@Path("id") id: String): Envelope<OpsPayrollRunDto>

    @POST("api/payroll-runs/{id}/approve")
    suspend fun approvePayroll(@Path("id") id: String): Envelope<OpsPayrollRunDto>

    @POST("api/payroll-runs/{id}/pay")
    suspend fun payAllPayslips(
        @Path("id") id: String,
        @Body body: OpsPayMethodRequest,
    ): Envelope<OpsPayrollRunDto>

    @POST("api/payroll-runs/{id}/payslips/{employeeId}/pay")
    suspend fun payPayslip(
        @Path("id") id: String,
        @Path("employeeId") employeeId: String,
        @Body body: OpsPayMethodRequest,
    ): Envelope<OpsPayrollRunDto>

    @GET("api/vacation-pays/upcoming")
    suspend fun upcomingVacationPays(): Envelope<List<OpsVacationPayDto>>

    @POST("api/vacation-pays/search")
    suspend fun searchVacationPays(@Body body: SearchBody): Envelope<OpsVacationPayPage>

    @POST("api/vacation-pays/{id}/pay")
    suspend fun payVacationPay(
        @Path("id") id: String,
        @Body body: OpsPayMethodRequest,
    ): Envelope<OpsVacationPayDto>

    @POST("api/payments/charges/one-time-service-expense")
    suspend fun oneTimeServiceExpense(@Body body: OpsOneTimeExpenseRequest): Envelope<String>
}
