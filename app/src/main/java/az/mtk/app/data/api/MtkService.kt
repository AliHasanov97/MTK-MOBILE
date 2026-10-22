package az.mtk.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MtkService {

    // ---- Buildings ----
    @GET("api/buildings/buildings")
    suspend fun buildings(): List<BuildingDto>

    @GET("api/buildings/buildings/{id}")
    suspend fun building(@Path("id") id: String): BuildingDto

    @GET("api/buildings/apartments/{id}")
    suspend fun apartment(@Path("id") id: String): ApartmentDto

    @GET("api/buildings/apartments/building/{id}")
    suspend fun apartmentsByBuilding(@Path("id") id: String): List<ApartmentDto>

    @GET("api/buildings/apartments/{id}/ownership-history")
    suspend fun ownershipHistory(@Path("id") id: String): List<OwnershipRecordDto>

    @POST("api/buildings/apartments/search")
    suspend fun apartments(@Body body: SearchBody): Envelope<SearchPage<ApartmentDto>>

    // ---- Owners ----
    @POST("api/buildings/owners/search")
    suspend fun owners(@Body body: SearchBody): Envelope<SearchPage<OwnerItemDto>>

    @GET("api/buildings/owners/{id}")
    suspend fun owner(@Path("id") id: String): OwnerDetailDto

    // ---- Garages ----
    @POST("api/buildings/garages/search")
    suspend fun garages(@Body body: SearchBody): Envelope<SearchPage<GarageDto>>

    @GET("api/buildings/garages/{id}")
    suspend fun garage(@Path("id") id: String): Envelope<GarageDto>

    // ---- Charges (Borclar) ----
    @POST("api/payments/charges/search")
    suspend fun charges(@Body body: SearchBody): Envelope<ChargesPage>

    // ---- HR ----
    @POST("api/hr/employees/search")
    suspend fun employees(@Body body: SearchBody): Envelope<HrPage<EmployeeDto>>

    @GET("api/hr/employees/{id}")
    suspend fun employee(@Path("id") id: String): Envelope<EmployeeDetailDto>

    @POST("api/hr/applications/search")
    suspend fun applications(@Body body: SearchBody): Envelope<HrPage<ApplicationDto>>
}
