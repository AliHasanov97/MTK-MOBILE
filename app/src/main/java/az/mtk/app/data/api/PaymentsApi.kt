package az.mtk.app.data.api

/**
 * Payments (Borclar) API — mirrors MTK-UI app/lib/api/payments.ts exactly.
 * The Payments module has no [JsonStringEnumConverter]: every enum crosses the
 * wire as its numeric ordinal (Unpaid=0, PartiallyPaid=1, Paid=2, Cancelled=3;
 * payment methods Cash=0, BankTransfer=1, Card=2).
 */

data class ChargeGenerateBody(val period: String)

data class GenerateChargesResultDto(
    val period: String?,
    val residentChargesCreated: Int?,
    val apartmentsSkippedNoRate: Int?,
    val garagesSkippedNoRate: Int?,
    val vendorChargesCreated: Int?,
)

data class CreateChargeRequestDto(
    val ownerId: String,
    val apartmentId: String? = null,
    val garageId: String? = null,
    val amount: Double,
    val description: String,
    val period: String? = null,
)

data class CreatePaymentRequestDto(
    val ownerId: String,
    val amount: Double,
    val paymentMethod: Int,
    val notes: String? = null,
    val apartmentId: String? = null,
    val garageId: String? = null,
)

/** Body of the "no data" endpoints (cancel). */
data class PaymentMessageDto(val message: String?)

/** Allocations of a charge (who paid it, from advance or directly). */
data class ChargeAllocationDto(
    val id: String,
    val paymentId: String?,
    val allocatedAmount: Double?,
    val paymentDate: String?,
    val paymentStatus: String?,
    val remainingDebtAfterPayment: Double?,
    val isFromAdvance: Boolean?,
)

/** Allocations of a payment (which charges it covered). */
data class PaymentAllocationDto(
    val id: String,
    val chargeId: String?,
    val apartmentId: String?,
    val garageId: String?,
    val period: String?,
    val description: String?,
    val chargeAmount: Double?,
    val allocatedAmount: Double?,
    val remainingDebtAfterPayment: Double?,
    val propertyLabel: String?,
)

data class OwnerBalanceDto(
    val id: String?,
    val ownerId: String?,
    val totalDebt: Double?,
    val totalPaid: Double?,
    val currentBalance: Double?,
)

data class PropertyBalanceDto(
    val propertyId: String?,
    val totalDebt: Double?,
    val totalPaid: Double?,
    val currentBalance: Double?,
)

data class CompanyBalanceDto(
    val totalIncome: Double?,
    val totalExpense: Double?,
    val currentBalance: Double?,
)

data class PaymentDto(
    val id: String,
    val ownerId: String?,
    val ownerName: String?,
    val amount: Double?,
    val paymentMethod: Int?,
    val paymentDate: String?,
    val notes: String?,
    val apartmentId: String?,
    val garageId: String?,
    val status: Int?,
    // Backend həm sakin, həm tədarükçü ödənişi üçün eyni cavabı qaytarır;
    // aşağıdakılar sənəd görünüşündə istifadə olunur.
    val vendorId: String? = null,
    val customerId: String? = null,
    val partyName: String? = null,
    val propertyLabel: String? = null,
    val createdAt: String? = null,
)



interface PaymentsApi {

    @retrofit2.http.POST("api/payments/charges/generate")
    suspend fun generateChargesForPeriod(@retrofit2.http.Body body: ChargeGenerateBody): Envelope<GenerateChargesResultDto>

    @retrofit2.http.POST("api/payments/charges")
    suspend fun createCharge(@retrofit2.http.Body body: CreateChargeRequestDto): Envelope<String>

    @retrofit2.http.POST("api/payments/charges/{chargeId}/cancel")
    suspend fun cancelCharge(@retrofit2.http.Path("chargeId") chargeId: String): PaymentMessageDto

    @retrofit2.http.POST("api/payments/payments")
    suspend fun createPayment(@retrofit2.http.Body body: CreatePaymentRequestDto): Envelope<String>

    /** Tək ödəniş — tranzaksiya jurnalındakı sənədə keçid üçün. */
    @retrofit2.http.GET("api/payments/payments/{paymentId}")
    suspend fun payment(@retrofit2.http.Path("paymentId") paymentId: String): Envelope<PaymentDto>

    @retrofit2.http.GET("api/payments/charges/{chargeId}/allocations")
    suspend fun chargeAllocations(@retrofit2.http.Path("chargeId") chargeId: String): Envelope<List<ChargeAllocationDto>>

    @retrofit2.http.GET("api/payments/payments/{paymentId}/allocations")
    suspend fun paymentAllocations(@retrofit2.http.Path("paymentId") paymentId: String): Envelope<List<PaymentAllocationDto>>

    @retrofit2.http.GET("api/payments/balances/owner/{ownerId}")
    suspend fun ownerBalance(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<OwnerBalanceDto>

    @retrofit2.http.GET("api/payments/balances/property/{propertyId}")
    suspend fun propertyBalance(@retrofit2.http.Path("propertyId") propertyId: String): Envelope<PropertyBalanceDto>

    @retrofit2.http.GET("api/payments/balances/company")
    suspend fun companyBalance(): Envelope<CompanyBalanceDto>

    @retrofit2.http.GET("api/payments/charges/owner/{ownerId}")
    suspend fun getChargesByOwner(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<List<ChargeDto>>

    @retrofit2.http.GET("api/payments/payments/property/{propertyId}")
    suspend fun getPaymentsByProperty(@retrofit2.http.Path("propertyId") propertyId: String): Envelope<List<PaymentDto>>

    @retrofit2.http.GET("api/payments/payments/owner/{ownerId}")
    suspend fun getPaymentsByOwner(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<List<PaymentDto>>
}

object PaymentMethods {
    const val CASH = 0
    const val BANK_TRANSFER = 1
    const val CARD = 2
}
