package az.mtk.app.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Write operations of the Buildings module — mirrors exactly what
 * MTK-UI's app/lib/api/buildings.ts, garages.ts, owners.ts (and the
 * identity user search used by UserPicker) call. Read-only search
 * endpoints stay in MtkService/MtkRepository.
 */
interface BuildingsApi {

    // ---- Buildings (buildings.ts) ----

    @POST("api/buildings/buildings")
    suspend fun createBuilding(@Body body: CreateBuildingRequest): String

    @GET("api/buildings/buildings/{buildingId}/apartment-types")
    suspend fun apartmentTypes(@Path("buildingId") buildingId: String): List<ApartmentTypeDto>

    @POST("api/buildings/buildings/{buildingId}/apartment-types")
    suspend fun createApartmentType(
        @Path("buildingId") buildingId: String,
        @Body body: CreateApartmentTypeRequest,
    ): String

    // ---- Apartments (buildings.ts) ----

    @POST("api/buildings/apartments")
    suspend fun createApartment(@Body body: CreateApartmentRequest): String

    @POST("api/buildings/apartments/{id}/assign-owner")
    suspend fun assignOwnerToApartment(
        @Path("id") apartmentId: String,
        @Body body: AssignOwnerRequest,
    )

    @POST("api/buildings/apartments/{id}/transfer-ownership")
    suspend fun transferApartmentOwnership(
        @Path("id") apartmentId: String,
        @Body body: TransferApartmentOwnershipRequest,
    )

    // ---- Garages (garages.ts) ----

    @POST("api/buildings/garages")
    suspend fun createGarage(@Body body: CreateGarageRequest): Envelope<String>

    @PATCH("api/buildings/garages/{id}")
    suspend fun updateGarage(
        @Path("id") id: String,
        @Body body: UpdateGarageRequest,
    ): ApiMessage

    @POST("api/buildings/garages/{id}/assign-owner")
    suspend fun assignOwnerToGarage(
        @Path("id") id: String,
        @Body body: AssignOwnerRequest,
    ): ApiMessage

    @POST("api/buildings/garages/{id}/remove-owner")
    suspend fun removeOwnerFromGarage(@Path("id") id: String): ApiMessage

    @DELETE("api/buildings/garages/{id}")
    suspend fun deleteGarage(@Path("id") id: String): ApiMessage

    // ---- Owners (owners.ts) ----

    @POST("api/buildings/owners/passive")
    suspend fun createPassiveOwner(@Body body: CreatePassiveOwnerRequest): String

    @POST("api/buildings/owners/{ownerId}/link-user")
    suspend fun linkOwnerToUser(
        @Path("ownerId") ownerId: String,
        @Body body: LinkUserRequest,
    ): ApiMessage

    // ---- Users (identity.ts — used by the link-user user picker) ----

    @POST("api/identity/users/search")
    suspend fun searchUsers(@Body body: SearchUsersBody): SearchUsersResult
}

// ---- Request/response DTOs (camelCase JSON, nullable except ids) ----

data class CreateBuildingRequest(
    val name: String,
    val street: String,
    val city: String,
    val district: String? = null,
    val description: String? = null,
)

data class ApartmentTypeDto(
    val id: String,
    val buildingId: String?,
    val roomCount: Int?,
    val areaSquareMeters: Double?,
)

data class CreateApartmentTypeRequest(
    val roomCount: Int,
    val areaSquareMeters: Double,
)

data class CreateApartmentRequest(
    val buildingId: String,
    val apartmentNumber: String,
    val floor: Int,
    val apartmentTypeId: String,
)

data class AssignOwnerRequest(val ownerId: String)

/** Backend stamps transferDate itself (no transferDate/salePrice/notes). */
data class TransferApartmentOwnershipRequest(val newOwnerId: String)

data class CreateGarageRequest(
    val ownerId: String? = null,
    val garageNumber: String,
    val garageType: String,
    val description: String? = null,
)

/** UpdateGarage binds the enum straight from the body — numeric ordinal. */
data class UpdateGarageRequest(
    val type: Int,
    val description: String? = null,
)

object GarageTypeOrdinal {
    const val OPEN_PARKING = 0
    const val COVERED_GARAGE = 1
    const val STORAGE = 2

    fun fromKey(key: String?): Int = when (key) {
        "CoveredGarage" -> COVERED_GARAGE
        "Storage" -> STORAGE
        else -> OPEN_PARKING
    }
}

data class CreatePassiveOwnerRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val email: String,
    val notes: String? = null,
)

data class LinkUserRequest(val userId: String)

data class ApiMessage(
    val success: Boolean?,
    val statusCode: Int?,
    val message: String?,
)

/** api/identity/users/search — note its own page contract (pageNumber). */
data class SearchUsersBody(
    val searchTerm: String? = null,
    val pageNumber: Int = 1,
    val pageSize: Int = 8,
)

data class SearchUsersResult(
    val users: List<UserSummaryDto>?,
    val totalCount: Int?,
    val pageNumber: Int?,
    val pageSize: Int?,
)

data class UserSummaryDto(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val status: String?,
)
