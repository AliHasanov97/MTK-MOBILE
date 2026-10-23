package az.mtk.app.data.api

// Mirrors the owner-facing parts of MTK-UI app/lib/api/owners.ts and
// payments.ts (getOwnerByUserId, changeMyInfo lives in IdentityApi,
// owner balance / owner charges / owner payments).

data class OprOwnedApartment(
    val id: String,
    val building: NamedRef?,
    val apartmentNumber: String?,
    val floor: Int?,
    val areaSquareMeters: Double?,
    val roomCount: Int?,
    val status: String?,
)

data class OprOwnedGarage(
    val id: String,
    val garageNumber: String?,
    val type: String?,
    val description: String?,
)

data class OprOwner(
    val id: String,
    val userId: String?,
    val fullName: String,
    val email: String?,
    val phoneNumber: String?,
    val isActive: Boolean,
    val apartments: List<OprOwnedApartment>?,
    val garages: List<OprOwnedGarage>?,
)

data class OprOwnerBalanceResponse(
    val id: String,
    val ownerId: String,
    val totalDebt: Double,
    val totalPaid: Double,
    val currentBalance: Double,
)

data class OprPaymentResponse(
    val id: String,
    val ownerId: String?,
    val amount: Double,
    val paymentMethod: Int?,
    val paymentDate: String?,
    val status: Int?,
    val notes: String?,
    val createdAt: String?,
    val partyName: String?,
    val propertyLabel: String?,
)

interface OwnerProfileApi {

    /** "Mənim profilim" — sakinin öz Owner qeydi öz Users.Id-si ilə. */
    @retrofit2.http.GET("api/buildings/owners/by-user/{userId}")
    suspend fun ownerByUserId(@retrofit2.http.Path("userId") userId: String): OprOwner

    // ---- Owner payments balance ----
    @retrofit2.http.GET("api/payments/balances/owner/{ownerId}")
    suspend fun ownerBalance(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<OprOwnerBalanceResponse>

    // ---- Owner charges ----
    @retrofit2.http.GET("api/payments/charges/owner/{ownerId}")
    suspend fun chargesByOwner(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<List<ChargeDto>>

    // ---- Owner payments ----
    @retrofit2.http.GET("api/payments/payments/owner/{ownerId}")
    suspend fun paymentsByOwner(@retrofit2.http.Path("ownerId") ownerId: String): Envelope<List<OprPaymentResponse>>
}
