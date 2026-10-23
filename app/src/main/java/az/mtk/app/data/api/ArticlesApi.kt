package az.mtk.app.data.api

// Mirrors MTK-UI app/lib/api/articles.ts (Backend: MTK.Modules.Communications).

object ArticleStatus {
    const val DRAFT = "Draft"
    const val PUBLISHED = "Published"
    const val ARCHIVED = "Archived"

    val ALL = listOf(DRAFT, PUBLISHED, ARCHIVED)

    fun label(value: String?): String = when (value) {
        DRAFT -> "Qaralama"
        PUBLISHED -> "Yayımlanıb"
        ARCHIVED -> "Arxivdə"
        else -> value ?: "—"
    }
}

data class ArtArticleAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String?,
    val isPublic: Boolean,
)

data class ArtArticle(
    val id: String,
    val title: String,
    val summary: String?,
    val content: String?,
    val status: String?,
    val publishedAt: String?,
    val createdAt: String?,
    val viewCount: Int?,
    val createdByFullName: String?,
    val fileAttachments: List<ArtArticleAttachment>?,
)

/** Axtarış siyahısının elementi: mətn yoxdur, əvəzində fayl sayı var. */
data class ArtArticleListItem(
    val id: String,
    val title: String,
    val summary: String?,
    val status: String?,
    val publishedAt: String?,
    val createdAt: String?,
    val viewCount: Int?,
    val createdByFullName: String?,
    val fileAttachmentCount: Int?,
)

data class ArtArticlesPage(
    val data: List<ArtArticleListItem>?,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val pageCount: Int,
)

data class ArtArticleInput(
    val title: String,
    val summary: String,
    val content: String,
    val fileAttachmentIds: List<String>? = null,
)

data class ArtCreateArticleResponse(val id: String?)

interface ArticlesApi {

    @retrofit2.http.GET("api/communications/articles/latest")
    suspend fun latestArticles(@retrofit2.http.Query("count") count: Int = 5): Envelope<List<ArtArticle>>

    @retrofit2.http.POST("api/communications/articles/search")
    suspend fun searchArticles(@retrofit2.http.Body body: SearchBody): Envelope<ArtArticlesPage>

    @retrofit2.http.GET("api/communications/articles/{id}")
    suspend fun article(@retrofit2.http.Path("id") id: String): Envelope<ArtArticle>

    @retrofit2.http.POST("api/communications/articles")
    suspend fun createArticle(@retrofit2.http.Body body: ArtArticleInput): Envelope<ArtCreateArticleResponse>

    @retrofit2.http.PUT("api/communications/articles/{id}")
    suspend fun updateArticle(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: ArtArticleUpdateBody,
    )

    @retrofit2.http.POST("api/communications/articles/{id}/publish")
    suspend fun publishArticle(@retrofit2.http.Path("id") id: String)

    @retrofit2.http.DELETE("api/communications/articles/{id}")
    suspend fun deleteArticle(@retrofit2.http.Path("id") id: String)
}

data class ArtArticleUpdateBody(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val fileAttachmentIds: List<String>? = null,
)
