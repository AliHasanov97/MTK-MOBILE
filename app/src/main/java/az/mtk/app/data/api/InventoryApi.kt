package az.mtk.app.data.api

import az.mtk.app.AppGraph
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Warehouse (İnventar) + purchase API, mirroring MTK-UI's lib/api/inventory.ts
 * and purchases-client.ts exactly.
 *
 * The Warehouse API has no [JsonStringEnumConverter], so every enum crosses the
 * wire as its numeric ordinal. Unlike Payments enums these do NOT start at 0
 * (NomenclatureCategory has a gap: Other = 99), so values are mapped explicitly.
 */
object InvEnums {
    // NomenclatureCategory
    const val CATEGORY_MATERIAL = 1
    const val CATEGORY_EQUIPMENT = 2
    const val CATEGORY_SERVICE = 3
    const val CATEGORY_SUPPLY = 4
    const val CATEGORY_OTHER = 99

    // Unit
    const val UNIT_PIECE = 1
    const val UNIT_METER = 2
    const val UNIT_KILOGRAM = 3
    const val UNIT_LITER = 4
    const val UNIT_SET = 5
    const val UNIT_SQUARE_METER = 6
    const val UNIT_CUBIC_METER = 7
    const val UNIT_BOX = 8
    const val UNIT_PACKAGE = 9
    const val UNIT_SACK = 10

    // TransactionType
    const val TRANSACTION_RECEIPT = 1
    const val TRANSACTION_ISSUE = 2

    // PurchaseStatus
    const val PURCHASE_DRAFT = 0
    const val PURCHASE_RECEIVED = 1
    const val PURCHASE_CANCELLED = 2

    fun categoryLabel(ordinal: Int?): String = when (ordinal) {
        CATEGORY_MATERIAL -> "Material"
        CATEGORY_EQUIPMENT -> "Avadanlıq"
        CATEGORY_SERVICE -> "Xidmət"
        CATEGORY_SUPPLY -> "Təchizat"
        CATEGORY_OTHER -> "Digər"
        else -> "Digər"
    }

    fun unitLabel(ordinal: Int?): String = when (ordinal) {
        UNIT_PIECE -> "Ədəd"
        UNIT_METER -> "Metr"
        UNIT_KILOGRAM -> "Kiloqram"
        UNIT_LITER -> "Litr"
        UNIT_SET -> "Komplekt"
        UNIT_SQUARE_METER -> "kv. metr"
        UNIT_CUBIC_METER -> "kub metr"
        UNIT_BOX -> "Qutu"
        UNIT_PACKAGE -> "Paket"
        UNIT_SACK -> "Çuval"
        else -> "Ədəd"
    }

    fun purchaseStatusLabel(status: Int?): String = when (status) {
        PURCHASE_DRAFT -> "Hazırlanır"
        PURCHASE_RECEIVED -> "Qəbul edilib"
        PURCHASE_CANCELLED -> "Ləğv edilib"
        else -> "Naməlum"
    }
}

/** "2026-03-15" → UTC ISO instant, the same as MTK-UI's dateOnlyToUtcIso. */
fun dateOnlyToUtcIso(date: String): String = "${date.take(10)}T00:00:00.000Z"

// ---------------------------------------------------------------------------
// DTOs (Inv-prefixed; field names match the camelCase JSON)
// ---------------------------------------------------------------------------

data class InvNomenclatureDto(
    val id: String,
    val code: String?,
    val name: String?,
    val category: Int?,
    val unit: Int?,
    val isActive: Boolean?,
)

data class InvNomenclatureListItem(
    val id: String,
    val code: String?,
    val name: String?,
    val description: String?,
    val category: Int?,
    val unit: Int?,
    val isActive: Boolean?,
)

data class InvNomenclatureResponse(
    val id: String,
    val code: String?,
    val name: String?,
    val description: String?,
    val category: Int?,
    val unit: Int?,
    val minStockLevel: Double?,
    val isActive: Boolean?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class InvSearchNomenclaturesResult(
    val nomenclatures: List<InvNomenclatureListItem>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class InvSaveNomenclatureRequest(
    val code: String,
    val name: String,
    val description: String?,
    val category: Int,
    val unit: Int,
    val minStockLevel: Double?,
    val isActive: Boolean,
)

data class InvUpdateNomenclatureRequest(
    val id: String,
    val name: String,
    val description: String?,
    val category: Int,
    val unit: Int,
    val minStockLevel: Double?,
)

data class InvStockDto(
    val nomenclatureId: String,
    val nomenclatureCode: String?,
    val nomenclatureName: String?,
    val quantityOnHand: Double?,
    val lastTransactionDate: String?,
)

data class InvLowStockDto(
    val nomenclatureId: String,
    val nomenclatureCode: String?,
    val nomenclatureName: String?,
    val quantityOnHand: Double?,
    val minStockLevel: Double?,
    val deficit: Double?,
)

data class InvStockResponse(
    val nomenclatureId: String,
    val nomenclatureCode: String?,
    val nomenclatureName: String?,
    val quantityOnHand: Double?,
    val lastTransactionDate: String?,
    val minStockLevel: Double?,
    val isLowStock: Boolean?,
)

data class InvTransactionDto(
    val id: String,
    val transactionType: Int?,
    val nomenclatureId: String?,
    val nomenclatureName: String?,
    val quantity: Double?,
    val unitPrice: Double?,
    val transactionDate: String?,
    val referenceId: String?,
    val referenceType: String?,
    val notes: String?,
)

data class InvTransactionResponse(
    val id: String,
    val transactionType: Int?,
    val nomenclatureId: String?,
    val nomenclatureName: String?,
    val quantity: Double?,
    val unitPrice: Double?,
    val totalPrice: Double?,
    val transactionDate: String?,
    val referenceType: String?,
    val referenceId: String?,
    val notes: String?,
    val createdAt: String?,
)

/** Body of api/warehouse/transactions/search (its own shape, not the generic SearchBody). */
data class InvTransactionSearchBody(
    val nomenclatureId: String? = null,
    val transactionType: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val pageNumber: Int = 1,
    val pageSize: Int = 100,
)

data class InvRecordIssueRequest(
    val nomenclatureId: String,
    val quantity: Double,
    val notes: String?,
    val transactionDate: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdByUserId: String? = null,
)

data class InvPurchaseLineInput(
    val nomenclatureId: String,
    val quantity: Double,
    val unitPrice: Double,
)

data class InvCreatePurchaseRequest(
    val vendorId: String,
    val purchaseDate: String,
    val note: String?,
    val lines: List<InvPurchaseLineInput>,
)

data class InvPurchaseLineResponse(
    val id: String,
    val nomenclatureId: String,
    val nomenclatureCode: String?,
    val nomenclatureName: String?,
    val quantity: Double?,
    val unitPrice: Double?,
    val lineTotal: Double?,
)

data class InvPurchaseResponse(
    val id: String,
    val vendorId: String,
    val vendorName: String?,
    val purchaseDate: String?,
    val invoiceNumber: String?,
    val note: String?,
    val receivedOnUtc: String?,
    val status: Int?,
    val totalAmount: Double?,
    val createdAt: String?,
    val updatedAt: String?,
    val lines: List<InvPurchaseLineResponse>?,
)

data class InvSearchPurchasesResult(
    val purchases: List<InvPurchaseResponse>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class InvPurchaseHistoryItem(
    val purchaseId: String,
    val purchaseDate: String?,
    val invoiceNumber: String?,
    val vendorName: String?,
    val vendorId: String?,
    val status: Int?,
    val quantity: Double?,
    val unitPrice: Double?,
    val lineTotal: Double?,
)

data class InvCancelPurchaseRequest(
    val purchaseId: String,
    val note: String?,
)

data class InvVendorListResult(
    val id: String,
    val name: String?,
    val vendorType: Int?,
    val voen: String?,
    val director: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean?,
    val createdAt: String?,
)

data class InvSearchVendorsResult(
    val vendors: List<InvVendorListResult>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

// ---------------------------------------------------------------------------
// Retrofit interface
// ---------------------------------------------------------------------------

interface InventoryApi {

    // Anbar qalıqları
    @GET("api/warehouse/stocks")
    suspend fun stocks(
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 200,
    ): Envelope<List<InvStockDto>>

    @GET("api/warehouse/stocks/low-stock")
    suspend fun lowStock(): Envelope<List<InvLowStockDto>>

    @GET("api/warehouse/stocks/nomenclature/{id}")
    suspend fun stockByNomenclature(@Path("id") nomenclatureId: String): Envelope<InvStockResponse>

    // Materiallar (nomenklaturalar)
    @POST("api/warehouse/nomenclatures/search")
    suspend fun searchNomenclatures(@Body body: SearchBody): Envelope<InvSearchNomenclaturesResult>

    @GET("api/warehouse/nomenclatures")
    suspend fun nomenclatures(
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("isActive") isActive: Boolean? = null,
    ): Envelope<List<InvNomenclatureDto>>

    @GET("api/warehouse/nomenclatures/{id}")
    suspend fun nomenclature(@Path("id") id: String): Envelope<InvNomenclatureResponse>

    @POST("api/warehouse/nomenclatures")
    suspend fun createNomenclature(@Body body: InvSaveNomenclatureRequest): Envelope<String>

    @PUT("api/warehouse/nomenclatures/{id}")
    suspend fun updateNomenclature(
        @Path("id") id: String,
        @Body body: InvUpdateNomenclatureRequest,
    ): Envelope<Unit?>

    @DELETE("api/warehouse/nomenclatures/{id}")
    suspend fun deleteNomenclature(@Path("id") id: String): Envelope<Unit?>

    // Əməliyyatlar (daxilolma / çıxarış). Daxilolma yalnız oxunur — mallar
    // Payments alışlarında qəbul edilir və integration event ilə anbara düşür.
    @POST("api/warehouse/transactions/search")
    suspend fun searchTransactions(@Body body: InvTransactionSearchBody): Envelope<List<InvTransactionDto>>

    @GET("api/warehouse/transactions/{id}")
    suspend fun transaction(@Path("id") id: String): Envelope<InvTransactionResponse>

    @POST("api/warehouse/transactions/issue")
    suspend fun recordIssue(@Body body: InvRecordIssueRequest): Envelope<String>

    // Satınalmalar (daxilolmalar) — Payments modulunda
    @POST("api/payments/purchases/search")
    suspend fun searchPurchases(@Body body: SearchBody): Envelope<InvSearchPurchasesResult>

    @POST("api/payments/purchases")
    suspend fun createPurchase(@Body body: InvCreatePurchaseRequest): Envelope<String>

    @PUT("api/payments/purchases/{id}")
    suspend fun updatePurchase(
        @Path("id") purchaseId: String,
        @Body body: InvCreatePurchaseRequest,
    ): Envelope<Unit?>

    @GET("api/payments/purchases/{id}")
    suspend fun purchase(@Path("id") purchaseId: String): Envelope<InvPurchaseResponse>

    @GET("api/payments/purchases/nomenclature/{id}/history")
    suspend fun nomenclaturePurchaseHistory(@Path("id") nomenclatureId: String): Envelope<List<InvPurchaseHistoryItem>>

    @POST("api/payments/purchases/{id}/receive")
    suspend fun receivePurchase(@Path("id") purchaseId: String): Envelope<Unit?>

    @POST("api/payments/purchases/{id}/cancel")
    suspend fun cancelPurchase(
        @Path("id") purchaseId: String,
        @Body body: InvCancelPurchaseRequest,
    ): Envelope<Unit?>

    // Tədarükçülər (yalnız seçim üçün axtarış)
    @POST("api/payments/vendors/search")
    suspend fun searchVendors(@Body body: SearchBody): Envelope<InvSearchVendorsResult>
}

// ---------------------------------------------------------------------------
// Repository
// ---------------------------------------------------------------------------

/** Feature-module repository over [InventoryApi]; errors surface as user messages. */
class InventoryRepository(private val api: InventoryApi) {

    // -- Anbar --------------------------------------------------------------

    suspend fun stocks(): List<InvStockDto> =
        api.stocks().data ?: throw ApiException("Anbar qalıqları üçün boş cavab gəldi")

    suspend fun lowStock(): List<InvLowStockDto> =
        api.lowStock().data ?: throw ApiException("Minimum ehtiyat siyahısı üçün boş cavab gəldi")

    suspend fun stockOf(nomenclatureId: String): InvStockResponse? = runCatching {
        api.stockByNomenclature(nomenclatureId).data
    }.onFailure {
        // 400/404 (hələ əməliyyat yoxdur) → qalıq 0 kimi qəbul edilir
    }.getOrNull()

    // -- Materiallar ----------------------------------------------------------

    suspend fun nomenclaturePage(page: Int, pageSize: Int, search: String?): Page<InvNomenclatureListItem> {
        val env = api.searchNomenclatures(
            SearchBody(
                sortCriteria = SortCriteria("Code", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Materiallar")
        return Page(
            payload.nomenclatures.orEmpty(),
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun nomenclature(id: String): InvNomenclatureResponse =
        api.nomenclature(id).data ?: throw ApiException("Material tapılmadı")

    suspend fun createNomenclature(request: InvSaveNomenclatureRequest): String =
        api.createNomenclature(request).data ?: throw ApiException("Material yaradılmadı")

    suspend fun updateNomenclature(id: String, request: InvUpdateNomenclatureRequest) {
        api.updateNomenclature(id, request)
    }

    suspend fun deleteNomenclature(id: String) {
        api.deleteNomenclature(id)
    }

    // -- Əməliyyatlar ---------------------------------------------------------

    suspend fun transactions(
        transactionType: Int?,
        nomenclatureId: String? = null,
        pageSize: Int = 200,
    ): List<InvTransactionDto> =
        api.searchTransactions(
            InvTransactionSearchBody(
                nomenclatureId = nomenclatureId,
                transactionType = transactionType,
                pageNumber = 1,
                pageSize = pageSize,
            ),
        ).data ?: throw ApiException("Əməliyyatlar üçün boş cavab gəldi")

    suspend fun recordIssue(request: InvRecordIssueRequest): String =
        api.recordIssue(request).data ?: throw ApiException("Çıxarış qeydə alınmadı")

    // -- Satınalmalar ---------------------------------------------------------

    suspend fun purchasePage(page: Int, pageSize: Int, search: String?): Page<InvPurchaseResponse> {
        val env = api.searchPurchases(
            SearchBody(
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Satınalmalar")
        return Page(
            payload.purchases.orEmpty(),
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun purchase(id: String): InvPurchaseResponse =
        api.purchase(id).data ?: throw ApiException("Satınalma tapılmadı")

    suspend fun purchaseHistory(nomenclatureId: String): List<InvPurchaseHistoryItem> =
        api.nomenclaturePurchaseHistory(nomenclatureId).data.orEmpty()

    suspend fun createPurchase(request: InvCreatePurchaseRequest): String =
        api.createPurchase(request).data ?: throw ApiException("Satınalma yaradılmadı")

    suspend fun updatePurchase(id: String, request: InvCreatePurchaseRequest) {
        api.updatePurchase(id, request)
    }

    suspend fun receivePurchase(purchaseId: String) {
        api.receivePurchase(purchaseId)
    }

    suspend fun cancelPurchase(purchaseId: String, note: String?) {
        api.cancelPurchase(purchaseId, InvCancelPurchaseRequest(purchaseId, note))
    }

    // -- Tədarükçülər ----------------------------------------------------------

    suspend fun vendors(search: String?): List<InvVendorListResult> =
        api.searchVendors(SearchBody(searchTerm = search.clean(), pageSize = 200))
            .data?.vendors.orEmpty()

    companion object {
        const val PAGE_SIZE = 20
        fun purchasePdfPath(purchaseId: String) = "api/payments/purchases/$purchaseId/export"
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")
}

/** Lazily-created shared instance, same style as AppGraph.repository. */
object InventoryRepo {
    val repository: InventoryRepository by lazy {
        InventoryRepository(AppGraph.service(InventoryApi::class.java))
    }
}
