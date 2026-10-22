package az.mtk.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Hesabatlar (reports) endpoints, mirroring MTK-UI's payments.ts exactly:
 * annual payment report (data), general ledger search and company balance.
 * Export endpoints (annual Excel, monthly PDF) go through FileRepository/ExportActionRow.
 */
interface ReportsApi {

    @GET("api/payments/charges/reports/annual")
    suspend fun annualPaymentReport(
        @Query("year") year: Int,
        @Query("propertyType") propertyType: Int? = null,
    ): Envelope<ReportAnnualPaymentResponseDto>

    @POST("api/payments/transactions/search")
    suspend fun transactions(@Body body: SearchBody): Envelope<ReportTransactionsPage>

    @GET("api/payments/balances/company")
    suspend fun companyBalance(): Envelope<ReportCompanyBalanceDto>
}

object ReportExports {
    /** Excel ixracı — illik ödəniş qrafiki (propertyType: 0 = Mənzil, 1 = Qaraj). */
    fun annualExcel(year: Int, propertyType: Int?): String =
        buildString {
            append("api/payments/charges/reports/annual/export?year=").append(year)
            if (propertyType != null) append("&propertyType=").append(propertyType)
        }

    /** PDF ixracı — seçilmiş ayın ümumi hesabatı. */
    fun monthlyPdf(year: Int, month: Int): String =
        "api/reports/monthly/export-pdf?year=$year&month=$month"
}

object ReportPropertyType {
    const val APARTMENT = 0
    const val GARAGE = 1
}

object TransactionDirection {
    const val INCOME = 0
    const val EXPENSE = 1
}

/**
 * "Sakin ödənişi" — backend-də sakindən gələn hər ödənişin yazıldığı sabit
 * Transaction kateqoriyası (UmumiHesabatView-dakı RESIDENT_PAYMENT_CATEGORY).
 */
const val RESIDENT_PAYMENT_CATEGORY = "Sakin ödənişi"

/** Null status — həmin ay üçün heç bir haqq yaradılmayıb (borc deyil). */
data class ReportMonthlyChargeDto(
    val month: Int?,
    val amount: Double?,
    val paidAmount: Double?,
    val status: Int?,
)

data class ReportPropertyAnnualRowDto(
    val propertyId: String,
    val propertyType: Int?,
    val months: List<ReportMonthlyChargeDto>?,
    /** Bütün tarix üzrə cari qalıq borc (yalnız hesabat ilinə aid deyil). */
    val currentDebt: Double?,
)

data class ReportAnnualPaymentResponseDto(
    val year: Int?,
    val properties: List<ReportPropertyAnnualRowDto>?,
)

data class ReportTransactionDto(
    val id: String,
    val direction: Int?,
    val category: String?,
    val amount: Double?,
    val description: String?,
    val transactionDate: String?,
    val sourcePaymentId: String?,
    val sourcePurchaseId: String?,
)

data class ReportTransactionsPage(
    val transactions: List<ReportTransactionDto>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class ReportCompanyBalanceDto(
    val totalIncome: Double?,
    val totalExpense: Double?,
    val currentBalance: Double?,
)
