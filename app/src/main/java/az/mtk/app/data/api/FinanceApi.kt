package az.mtk.app.data.api

import az.mtk.app.AppGraph
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Maliyyə əməliyyatları + Tariflər + Tranzaksiyalar modulu — MTK-UI
 * app/lib/api/payments.ts contract-ını (charges/borclar istisna olmaqla)
 * birebir güzgüləyir. Payments API-də enum-lar [JsonStringEnumConverter]
 * olmadığından həm sorğu, həm cavab üzərində rəqəmli ordinal kimi gedir.
 */

// ---- Enum ordinals (index = C# enum declaration order) ---------------------

object FinPaymentMethod {
    const val CASH = 0
    const val BANK_TRANSFER = 1
    const val CARD = 2
}

object FinRateType {
    const val PER_SQUARE_METER = 0
    const val FIXED_GARAGE = 1
    const val MANUAL = 2
}

object FinGarageType {
    const val OPEN_PARKING = 0
    const val COVERED_GARAGE = 1
    const val STORAGE = 2
}

object FinTransactionDirection {
    const val INCOME = 0
    const val EXPENSE = 1
}

object FinPaymentStatus {
    const val PENDING = 0
    const val COMPLETED = 1
}

// ---- DTOs -------------------------------------------------------------------

data class FinPaymentResponse(
    val id: String,
    val ownerId: String?,
    val vendorId: String?,
    val customerId: String?,
    val amount: Double?,
    val paymentMethod: Int?,
    val paymentDate: String?,
    val status: Int?,
    val notes: String?,
    val createdAt: String?,
    val apartmentId: String?,
    val garageId: String?,
    val partyName: String?,
    val propertyLabel: String?,
)

data class FinPaymentsPage(
    val payments: List<FinPaymentResponse>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class FinRateResponse(
    val id: String,
    val rateType: Int?,
    val amount: Double?,
    val effectiveFrom: String?,
    val effectiveTo: String?,
    val description: String?,
    val garageType: Int?,
)

data class FinTransactionResponse(
    val id: String,
    val direction: Int?,
    val category: String?,
    val amount: Double?,
    val description: String?,
    val transactionDate: String?,
    val createdAt: String?,
    val sourcePaymentId: String?,
    val sourcePurchaseId: String?,
    val documentType: String?,
    val referenceId: String?,
)

data class FinTransactionsPage(
    val transactions: List<FinTransactionResponse>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class FinTransactionsSummary(
    val totalIncome: Double?,
    val totalExpense: Double?,
    val net: Double?,
)

data class FinCreatePaymentRequest(
    val ownerId: String,
    val amount: Double,
    val paymentMethod: Int,
    val notes: String?,
    val apartmentId: String?,
    val garageId: String?,
)

data class FinCreateRateRequest(
    val rateType: Int,
    val amount: Double,
    val effectiveFrom: String,
    val description: String?,
    val garageType: Int?,
)

data class FinUpdateRateRequest(
    val amount: Double,
    val description: String?,
)

data class FinCreateTransactionRequest(
    val direction: Int,
    val category: String,
    val amount: Double,
    val description: String?,
)

data class FinTransactionsSummaryBody(
    val filters: List<QueryFilter>?,
)

data class FinMessageEnvelope(
    val message: String?,
)

// ---- Retrofit service --------------------------------------------------------

interface FinanceService {

    // ---- Payments (Ödənişlər) ----
    @POST("api/payments/payments/search")
    suspend fun searchPayments(@Body body: SearchBody): Envelope<FinPaymentsPage>

    @POST("api/payments/payments")
    suspend fun createPayment(@Body body: FinCreatePaymentRequest): Envelope<String>

    // ---- Rates (Tariflər) ----
    @GET("api/payments/rates/current")
    suspend fun currentRates(): Envelope<List<FinRateResponse>>

    @POST("api/payments/rates")
    suspend fun createRate(@Body body: FinCreateRateRequest): Envelope<String>

    @PUT("api/payments/rates/{id}")
    suspend fun updateRate(@Path("id") id: String, @Body body: FinUpdateRateRequest): FinMessageEnvelope

    // ---- Transactions (Tranzaksiyalar / Xərclər / Əlavə gəlirlər) ----
    @POST("api/payments/transactions/search")
    suspend fun searchTransactions(@Body body: SearchBody): Envelope<FinTransactionsPage>

    @POST("api/payments/transactions/summary")
    suspend fun transactionsSummary(@Body body: FinTransactionsSummaryBody): Envelope<FinTransactionsSummary>

    @POST("api/payments/transactions")
    suspend fun createTransaction(@Body body: FinCreateTransactionRequest): Envelope<String>

    // ---- Owner pickers (Ödənişlər modalı üçün — MTK-UI OwnerPicker/ApartmentPicker-in istifadə etdiyi endpointlər) ----
    @POST("api/buildings/owners/search")
    suspend fun searchOwners(@Body body: SearchBody): Envelope<SearchPage<OwnerItemDto>>

    @GET("api/buildings/owners/{id}")
    suspend fun owner(@Path("id") id: String): Envelope<OwnerDetailDto>
}

// ---- Repository --------------------------------------------------------------

/**
 * Postgres "timestamp with time zone" sütunları üçün yalnız UTC offset qəbul
 * olunur — təqvim tarixini UTC gecə yarısına sabitləyir (MTK-UI dateOnlyToUtcIso).
 */
fun finDateOnlyToUtcIso(value: String): String =
    if (value.length == 10) "$value" + "T00:00:00.000Z" else value

object FinanceRepository {

    private val api: FinanceService by lazy { AppGraph.service(FinanceService::class.java) }

    // ---- Payments ----

    suspend fun payments(page: Int, pageSize: Int = MtkRepository.PAGE_SIZE, search: String? = null): Page<FinPaymentResponse> {
        val env = api.searchPayments(
            SearchBody(
                sortCriteria = SortCriteria("PaymentDate", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Ödənişlər")
        return Page(payload.payments.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun createPayment(request: FinCreatePaymentRequest): String =
        api.createPayment(request).unwrap("Ödəniş").trim('"')

    // ---- Rates ----

    suspend fun currentRates(): List<FinRateResponse> =
        api.currentRates().unwrap("Tariflər")

    suspend fun createRate(request: FinCreateRateRequest): String =
        api.createRate(request).unwrap("Tarif").trim('"')

    suspend fun updateRate(id: String, amount: Double, description: String?) {
        api.updateRate(id, FinUpdateRateRequest(amount, description))
    }

    // ---- Transactions ----

    suspend fun transactions(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
        direction: Int? = null,
    ): Page<FinTransactionResponse> {
        val filters = buildList {
            if (direction != null) add(QueryFilter("Direction", Comparison.EQUALS, direction))
        }
        val env = api.searchTransactions(
            SearchBody(
                filters = filters.ifEmpty { null },
                sortCriteria = SortCriteria("TransactionDate", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Tranzaksiyalar")
        return Page(payload.transactions.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun transactionsSummary(): FinTransactionsSummary =
        api.transactionsSummary(FinTransactionsSummaryBody(filters = null)).unwrap("Xülasə")

    suspend fun createTransaction(request: FinCreateTransactionRequest): String =
        api.createTransaction(request).unwrap("Tranzaksiya").trim('"')

    // ---- Owners (picker) ----

    suspend fun owners(page: Int, pageSize: Int = MtkRepository.PAGE_SIZE, search: String? = null): Page<OwnerItemDto> {
        val env = api.searchOwners(
            SearchBody(searchTerm = search.clean(), page = page, pageSize = pageSize),
        )
        val payload = env.unwrap("Sahiblər")
        return Page(payload.items.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun owner(id: String): OwnerDetailDto =
        api.owner(id).unwrap("Sahib")

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")
}

// ---- Azerbaijani labels -------------------------------------------------------

fun finPaymentMethodLabel(value: Int?): String = when (value) {
    FinPaymentMethod.CASH -> "Nağd"
    FinPaymentMethod.BANK_TRANSFER -> "Bank köçürməsi"
    FinPaymentMethod.CARD -> "Kart"
    else -> "—"
}

fun finPaymentStatusLabel(value: Int?): String = when (value) {
    FinPaymentStatus.PENDING -> "Gözləyir"
    FinPaymentStatus.COMPLETED -> "Tamamlanıb"
    else -> "—"
}

fun finRateTypeLabel(value: Int?): String = when (value) {
    FinRateType.PER_SQUARE_METER -> "m² üzrə (mənzil)"
    FinRateType.FIXED_GARAGE -> "Sabit (qaraj)"
    FinRateType.MANUAL -> "Manual"
    else -> "—"
}

fun finGarageTypeLabel(value: Int?): String = when (value) {
    FinGarageType.OPEN_PARKING -> "Açıq dayanacaq"
    FinGarageType.COVERED_GARAGE -> "Örtülü qaraj"
    FinGarageType.STORAGE -> "Anbar yeri"
    else -> "—"
}

fun finTransactionDirectionLabel(value: Int?): String = when (value) {
    FinTransactionDirection.INCOME -> "Gəlir"
    FinTransactionDirection.EXPENSE -> "Xərc"
    else -> "—"
}
