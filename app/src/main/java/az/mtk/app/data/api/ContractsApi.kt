package az.mtk.app.data.api

/**
 * Müqavilələr (Contracts) API — mirrors MTK-UI app/lib/api/contracts.ts.
 * The Payments API has no enum converter, so every enum crosses the wire as
 * its numeric ordinal (index = C# enum ordinal).
 */

/** A bare-date (yyyy-MM-dd) has no time-of-day meaning: pin to UTC midnight, otherwise Npgsql rejects it. */
object ContractDates {
    fun toUtcIso(value: String): String =
        if (value.length == 10) "$value" + "T00:00:00.000Z" else value
}

object ContractStatus {
    const val DRAFT = 0
    const val ACTIVE = 1
    const val SUSPENDED = 2
    const val TERMINATED = 3

    fun label(value: Int?): String = when (value) {
        DRAFT -> "Hazırlanır"
        ACTIVE -> "Qüvvədədir"
        SUSPENDED -> "Dayandırılıb"
        TERMINATED -> "Ləğv edilib"
        else -> "—"
    }
}

object BillingPeriod {
    const val MONTHLY = 0
    const val QUARTERLY = 1
    const val YEARLY = 2
    const val ONE_TIME = 3

    fun label(value: Int?): String = when (value) {
        MONTHLY -> "Aylıq"
        QUARTERLY -> "Rüblük"
        YEARLY -> "İllik"
        ONE_TIME -> "Birdəfəlik"
        else -> "—"
    }
}

/** Response for MessageOnly endpoints. */
data class MessageResponse(
    val message: String?,
)

data class ContractListItemDto(
    val id: String,
    val number: String,
    val vendorId: String?,
    val vendorName: String?,
    val customerId: String?,
    val customerName: String?,
    val startDate: String,
    val endDate: String,
    val status: Int,
    val isExpired: Boolean?,
    val note: String?,
    val createdAt: String?,
)

data class ContractServiceDto(
    val id: String,
    val name: String,
    val description: String?,
    val unitPrice: Double?,
    val billingPeriod: Int,
    val periodAmount: Double?,
    val serviceStartDate: String?,
    val serviceEndDate: String?,
    val paymentTermDays: Int?,
    val isActive: Boolean?,
)

data class ContractDto(
    val id: String,
    val number: String,
    val vendorId: String?,
    val vendorName: String?,
    val customerId: String?,
    val customerName: String?,
    val createdByUserId: String?,
    val startDate: String,
    val endDate: String,
    val status: Int,
    val note: String?,
    val isExpired: Boolean?,
    val isActive: Boolean?,
    val monthlyAmount: Double?,
    val totalAmount: Double?,
    val createdAt: String?,
    val updatedAt: String?,
    val services: List<ContractServiceDto>?,
)

data class ContractSearchResultDto(
    val contracts: List<ContractListItemDto>?,
    val totalCount: Int,
    val page: Int?,
    val pageSize: Int?,
)

data class ContractCreateRequest(
    val number: String,
    val vendorId: String?,
    val customerId: String?,
    val startDate: String,
    val endDate: String,
    val createdByUserId: String? = null,
    val note: String?,
)

data class ContractUpdateRequest(
    val startDate: String,
    val endDate: String,
    val note: String?,
)

data class ContractServiceRequest(
    val name: String,
    val unitPrice: Double,
    val billingPeriod: Int,
    val description: String?,
    val serviceStartDate: String?,
    val serviceEndDate: String?,
    val paymentTermDays: Int?,
)

data class ContractServiceStatusRequest(
    val isActive: Boolean,
)

data class ContractSuspendRequest(
    val note: String?,
)

data class ContractTerminateRequest(
    val terminatedOn: String,
    val note: String?,
)

interface ContractsApi {

    @retrofit2.http.POST("api/payments/contracts/search")
    suspend fun search(@retrofit2.http.Body body: SearchBody): Envelope<ContractSearchResultDto>

    @retrofit2.http.GET("api/payments/contracts/{id}")
    suspend fun get(@retrofit2.http.Path("id") id: String): Envelope<ContractDto>

    @retrofit2.http.POST("api/payments/contracts")
    suspend fun create(@retrofit2.http.Body body: ContractCreateRequest): Envelope<String>

    @retrofit2.http.PUT("api/payments/contracts/{id}")
    suspend fun update(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: ContractUpdateRequest,
    ): MessageResponse

    @retrofit2.http.POST("api/payments/contracts/{id}/services")
    suspend fun addService(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: ContractServiceRequest,
    ): Envelope<String>

    @retrofit2.http.PUT("api/payments/contracts/{id}/services/{serviceId}")
    suspend fun updateService(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("serviceId") serviceId: String,
        @retrofit2.http.Body body: ContractServiceRequest,
    ): MessageResponse

    @retrofit2.http.DELETE("api/payments/contracts/{id}/services/{serviceId}")
    suspend fun removeService(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("serviceId") serviceId: String,
    ): MessageResponse

    @retrofit2.http.PUT("api/payments/contracts/{id}/services/{serviceId}/status")
    suspend fun setServiceStatus(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("serviceId") serviceId: String,
        @retrofit2.http.Body body: ContractServiceStatusRequest,
    ): MessageResponse

    @retrofit2.http.POST("api/payments/contracts/{id}/activate")
    suspend fun activate(@retrofit2.http.Path("id") id: String): MessageResponse

    @retrofit2.http.POST("api/payments/contracts/{id}/suspend")
    suspend fun suspend(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: ContractSuspendRequest,
    ): MessageResponse

    @retrofit2.http.POST("api/payments/contracts/{id}/terminate")
    suspend fun terminate(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: ContractTerminateRequest,
    ): MessageResponse

    @retrofit2.http.DELETE("api/payments/contracts/{id}")
    suspend fun delete(@retrofit2.http.Path("id") id: String): MessageResponse
}
