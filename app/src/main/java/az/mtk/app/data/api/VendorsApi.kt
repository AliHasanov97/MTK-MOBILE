package az.mtk.app.data.api

/**
 * Tədarükçülər (Vendors) + tədarükçü borcları/ödənişləri — mirrors
 * MTK-UI app/lib/api/vendors.ts and vendorCharges.ts. Enums cross the wire as
 * numeric ordinals (index = C# enum ordinal).
 */

object VendorType {
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

object PaymentMethod {
    const val CASH = 0
    const val BANK_TRANSFER = 1
    const val CARD = 2

    fun label(value: Int?): String = when (value) {
        CASH -> "Nağd"
        BANK_TRANSFER -> "Bank köçürməsi"
        CARD -> "Kart"
        else -> "—"
    }

    /** Ödəniş formalarında göstərilən siyahı (Kart istifadəçiyə bağlı olmadığından daxil deyil). */
    val selectable: List<Int> = listOf(CASH, BANK_TRANSFER)
}

data class VendorDto(
    val id: String,
    val name: String,
    val vendorType: Int,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean?,
    val createdAt: String?,
)

data class VendorDetailDto(
    val id: String,
    val name: String,
    val vendorType: Int,
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

data class VendorSearchResultDto(
    val vendors: List<VendorDto>?,
    val totalCount: Int,
    val page: Int?,
    val pageSize: Int?,
)

data class VendorSaveRequest(
    val name: String,
    val vendorType: Int,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val note: String?,
)

data class VendorStatusRequest(
    val isActive: Boolean,
)

data class VendorChargeDto(
    val id: String,
    val contractId: String?,
    val vendorId: String,
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

data class VendorChargeSearchResultDto(
    val items: List<VendorChargeDto>?,
    val totalCount: Int,
    val page: Int?,
    val pageSize: Int?,
)

data class VendorPaymentCreateRequest(
    val vendorId: String,
    val amount: Double,
    val paymentMethod: Int,
    val notes: String?,
)

/** Shared payment response for owner/vendor/customer payments — exactly one of the ids is set. */
data class PartyPaymentDto(
    val id: String,
    val ownerId: String?,
    val vendorId: String?,
    val customerId: String?,
    val amount: Double,
    val paymentMethod: Int,
    val paymentDate: String?,
    val status: Int,
    val notes: String?,
    val createdAt: String?,
    val partyName: String?,
)

interface VendorsApi {

    @retrofit2.http.POST("api/payments/vendors/search")
    suspend fun search(@retrofit2.http.Body body: SearchBody): Envelope<VendorSearchResultDto>

    @retrofit2.http.GET("api/payments/vendors/{id}")
    suspend fun get(@retrofit2.http.Path("id") id: String): Envelope<VendorDetailDto>

    @retrofit2.http.POST("api/payments/vendors")
    suspend fun create(@retrofit2.http.Body body: VendorSaveRequest): Envelope<String>

    @retrofit2.http.PUT("api/payments/vendors/{id}")
    suspend fun update(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: VendorSaveRequest,
    ): MessageResponse

    @retrofit2.http.PUT("api/payments/vendors/{id}/status")
    suspend fun setStatus(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: VendorStatusRequest,
    ): MessageResponse

    @retrofit2.http.DELETE("api/payments/vendors/{id}")
    suspend fun delete(@retrofit2.http.Path("id") id: String): MessageResponse
}

interface VendorChargesApi {

    @retrofit2.http.POST("api/payments/vendorcharges/search")
    suspend fun search(@retrofit2.http.Body body: SearchBody): Envelope<VendorChargeSearchResultDto>

    @retrofit2.http.GET("api/payments/vendorcharges/vendor/{vendorId}")
    suspend fun byVendor(@retrofit2.http.Path("vendorId") vendorId: String): Envelope<List<VendorChargeDto>>

    @retrofit2.http.POST("api/payments/vendorcharges/{id}/cancel")
    suspend fun cancel(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: Map<String, String>,
    ): MessageResponse

    @retrofit2.http.GET("api/payments/vendorpayments/vendor/{vendorId}")
    suspend fun paymentsByVendor(
        @retrofit2.http.Path("vendorId") vendorId: String,
    ): Envelope<List<PartyPaymentDto>>

    @retrofit2.http.POST("api/payments/vendorpayments")
    suspend fun createPayment(@retrofit2.http.Body body: VendorPaymentCreateRequest): Envelope<String>
}
