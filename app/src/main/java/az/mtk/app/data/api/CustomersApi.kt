package az.mtk.app.data.api

/**
 * Müştərilər (Customers) + müştəri borcları/ödənişləri — mirrors
 * MTK-UI app/lib/api/customers.ts and customerCharges.ts. Enums cross the wire
 * as numeric ordinals (index = C# enum ordinal).
 */

object CustomerType {
    const val LEGAL_ENTITY = 0
    const val INDIVIDUAL = 1
    const val SOLE_PROPRIETOR = 2

    fun label(value: Int?): String = when (value) {
        LEGAL_ENTITY -> "Hüquqi şəxs"
        INDIVIDUAL -> "Fiziki şəxs"
        SOLE_PROPRIETOR -> "Fərdi sahibkar"
        else -> "—"
    }
}

object ChargeLabels {
    // ChargeStatus ordinal constants live in MtkRepository.kt (shared aggregate).
    fun label(value: Int?): String = when (value) {
        ChargeStatus.UNPAID -> "Ödənilməyib"
        ChargeStatus.PARTIALLY_PAID -> "Qismən ödənilib"
        ChargeStatus.PAID -> "Ödənilib"
        ChargeStatus.CANCELLED -> "Ləğv edilib"
        else -> "—"
    }
}

data class CustomerDto(
    val id: String,
    val name: String,
    val customerType: Int,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean?,
    val createdAt: String?,
)

data class CustomerDetailDto(
    val id: String,
    val name: String,
    val customerType: Int,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean?,
    val createdAt: String?,
    val note: String?,
    val updatedAt: String?,
)

data class CustomerSearchResultDto(
    val customers: List<CustomerDto>?,
    val totalCount: Int,
    val page: Int?,
    val pageSize: Int?,
)

data class CustomerBalanceDto(
    val customerId: String,
    val outstandingDebt: Double?,
    val advance: Double?,
    val totalPaid: Double?,
)

data class CustomerSaveRequest(
    val name: String,
    val customerType: Int,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val note: String?,
)

data class CustomerStatusRequest(
    val isActive: Boolean,
)

data class CustomerChargeDto(
    val id: String,
    val contractId: String?,
    val customerId: String,
    val contractServiceId: String?,
    val period: String?,
    val description: String?,
    val amount: Double,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val status: Int,
    val chargeDate: String?,
    val dueDate: String?,
    val isOverdue: Boolean?,
    val createdAt: String?,
)

data class CustomerChargeSearchResultDto(
    val items: List<CustomerChargeDto>?,
    val totalCount: Int,
    val page: Int?,
    val pageSize: Int?,
)

data class CustomerChargeCreateRequest(
    val customerId: String,
    val description: String,
    val amount: Double,
    val dueDate: String?,
)

data class CustomerPaymentCreateRequest(
    val customerId: String,
    val amount: Double,
    val paymentMethod: Int,
    val notes: String?,
)

typealias CustomerPaymentDto = PartyPaymentDto

interface CustomersApi {

    @retrofit2.http.POST("api/payments/customers/search")
    suspend fun search(@retrofit2.http.Body body: SearchBody): Envelope<CustomerSearchResultDto>

    @retrofit2.http.GET("api/payments/customers/{id}")
    suspend fun get(@retrofit2.http.Path("id") id: String): Envelope<CustomerDetailDto>

    @retrofit2.http.GET("api/payments/customers/{id}/balance")
    suspend fun balance(@retrofit2.http.Path("id") id: String): Envelope<CustomerBalanceDto>

    @retrofit2.http.POST("api/payments/customers")
    suspend fun create(@retrofit2.http.Body body: CustomerSaveRequest): Envelope<String>

    @retrofit2.http.PUT("api/payments/customers/{id}")
    suspend fun update(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: CustomerSaveRequest,
    ): MessageResponse

    @retrofit2.http.PUT("api/payments/customers/{id}/status")
    suspend fun setStatus(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: CustomerStatusRequest,
    ): MessageResponse

    @retrofit2.http.DELETE("api/payments/customers/{id}")
    suspend fun delete(@retrofit2.http.Path("id") id: String): MessageResponse
}

interface CustomerChargesApi {

    @retrofit2.http.POST("api/payments/customercharges/search")
    suspend fun search(@retrofit2.http.Body body: SearchBody): Envelope<CustomerChargeSearchResultDto>

    @retrofit2.http.GET("api/payments/customercharges/customer/{customerId}")
    suspend fun byCustomer(@retrofit2.http.Path("customerId") customerId: String): Envelope<List<CustomerChargeDto>>

    @retrofit2.http.GET("api/payments/customerpayments/customer/{customerId}")
    suspend fun paymentsByCustomer(
        @retrofit2.http.Path("customerId") customerId: String,
    ): Envelope<List<CustomerPaymentDto>>

    @retrofit2.http.POST("api/payments/customercharges")
    suspend fun createCharge(@retrofit2.http.Body body: CustomerChargeCreateRequest): Envelope<String>

    @retrofit2.http.POST("api/payments/customercharges/{id}/cancel")
    suspend fun cancelCharge(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: Map<String, String>,
    ): MessageResponse

    @retrofit2.http.POST("api/payments/customerpayments")
    suspend fun createPayment(@retrofit2.http.Body body: CustomerPaymentCreateRequest): Envelope<String>
}
