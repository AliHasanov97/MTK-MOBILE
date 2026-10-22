package az.mtk.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Müraciətlər (appeals) module — mirrors MTK-UI's appeals.ts. The backend's
 * AppealsController has no [JsonStringEnumConverter], so enums cross the wire
 * as their numeric ordinals (1-based), exactly like the TS module assumes.
 */
interface AppealsApi {

    @POST("api/appeals")
    suspend fun createAppeal(@Body body: CreateAppealRequestDto): String

    @POST("api/appeals/search")
    suspend fun searchAppeals(@Body body: SearchBody): Envelope<AppealsSearchPage>

    @GET("api/appeals/{id}")
    suspend fun appeal(@Path("id") id: String): Envelope<AppealDetailDto>

    @PUT("api/appeals/{id}/status")
    suspend fun updateAppealStatus(
        @Path("id") id: String,
        @Body body: UpdateAppealStatusRequestDto,
    ): Envelope<String>

    @PUT("api/appeals/{id}/assign")
    suspend fun assignAppeal(
        @Path("id") id: String,
        @Body body: AssignAppealRequestDto,
    ): Envelope<String>

    @POST("api/appeals/{id}/comments")
    suspend fun addComment(
        @Path("id") id: String,
        @Body body: AddAppealCommentRequestDto,
    ): Envelope<String>
}

object AppealStatus {
    const val SUBMITTED = 1
    const val IN_PROGRESS = 2
    const val RESOLVED = 3
    const val CLOSED = 4
    const val REJECTED = 5

    val ALL = intArrayOf(SUBMITTED, IN_PROGRESS, RESOLVED, CLOSED, REJECTED)
}

object AppealCategory {
    const val MAINTENANCE = 1
    const val TECHNICAL = 2
    const val FINANCIAL = 3
    const val CLEANLINESS = 4
    const val SECURITY = 5
    const val PARKING = 6
    const val NOISE = 7
    const val GENERAL = 8

    val ALL = intArrayOf(
        MAINTENANCE, TECHNICAL, FINANCIAL, CLEANLINESS,
        SECURITY, PARKING, NOISE, GENERAL,
    )
}

object AppealPriority {
    const val NORMAL = 1
    const val HIGH = 2
    const val URGENT = 3

    val ALL = intArrayOf(NORMAL, HIGH, URGENT)
}

data class CreateAppealRequestDto(
    val title: String,
    val description: String,
    val category: Int,
    val apartmentId: String? = null,
    val garageId: String? = null,
)

data class UpdateAppealStatusRequestDto(val newStatus: Int)

data class AssignAppealRequestDto(val assignedToId: String)

data class AddAppealCommentRequestDto(val text: String)

data class AppealListItemDto(
    val id: String,
    val title: String?,
    val category: Int?,
    val status: Int?,
    val priority: Int?,
    val submitterId: String?,
    val submitterName: String?,
    val assignedToId: String?,
    val assignedToName: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val commentsCount: Int?,
)

data class AppealCommentDto(
    val id: String,
    val userId: String?,
    val userName: String?,
    val text: String?,
    val createdAt: String?,
)

data class AppealDetailDto(
    val id: String,
    val title: String?,
    val description: String?,
    val category: Int?,
    val status: Int?,
    val priority: Int?,
    val submitterId: String?,
    val submitterName: String?,
    val assignedToId: String?,
    val assignedToName: String?,
    val apartmentId: String?,
    val apartmentLabel: String?,
    val garageId: String?,
    val garageLabel: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val resolvedAt: String?,
    val closedAt: String?,
    val comments: List<AppealCommentDto>?,
)

data class AppealsSearchPage(
    val appeals: List<AppealListItemDto>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

/** Azerbaijani labels + pill tones, matching appeals.ts label helpers. */
object AppealLabels {
    fun status(value: Int?): String = when (value) {
        AppealStatus.SUBMITTED -> "Təqdim edilib"
        AppealStatus.IN_PROGRESS -> "İşlənir"
        AppealStatus.RESOLVED -> "Həll edilib"
        AppealStatus.CLOSED -> "Bağlanıb"
        AppealStatus.REJECTED -> "Rədd edilib"
        else -> "—"
    }

    fun category(value: Int?): String = when (value) {
        AppealCategory.MAINTENANCE -> "Təmir-bərpa"
        AppealCategory.TECHNICAL -> "Texniki"
        AppealCategory.FINANCIAL -> "Maliyyə"
        AppealCategory.CLEANLINESS -> "Təmizlik"
        AppealCategory.SECURITY -> "Təhlükəsizlik"
        AppealCategory.PARKING -> "Dayanacaq"
        AppealCategory.NOISE -> "Səs-küy"
        AppealCategory.GENERAL -> "Ümumi"
        else -> "—"
    }

    fun priority(value: Int?): String = when (value) {
        AppealPriority.NORMAL -> "Normal"
        AppealPriority.HIGH -> "Yüksək"
        AppealPriority.URGENT -> "Təcili"
        else -> "—"
    }

    /** Bağlı müraciət: yeni şərh/status dəyişikliyi qəbul olunmur. */
    fun isClosed(value: Int?): Boolean =
        value == AppealStatus.CLOSED || value == AppealStatus.REJECTED
}
