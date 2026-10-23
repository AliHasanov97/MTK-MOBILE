package az.mtk.app.data.api

import az.mtk.app.AppGraph
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Xəbərlər modulunun məlumat qatı — `app/lib/api/articles.ts` ilə eyni
 * endpoint-lər. Bütün cavablar `Envelope` içində gəlir.
 */
object NewsRepository {

    private val api: ArticlesApi by lazy { AppGraph.service(ArticlesApi::class.java) }

    suspend fun search(
        status: String?,
        searchTerm: String,
        page: Int,
        pageSize: Int,
    ): ArtArticlesPage {
        val filters = status?.let {
            listOf(QueryFilter(columnName = "Status", comparison = Comparison.EQUALS, value = it))
        }
        val body = SearchBody(
            filters = filters,
            sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
            searchTerm = searchTerm.trim().takeIf { it.isNotEmpty() },
            page = page,
            pageSize = pageSize,
        )
        return api.searchArticles(body).data
            ?: ArtArticlesPage(emptyList(), 0, page, pageSize, 1)
    }

    suspend fun article(id: String): ArtArticle =
        api.article(id).data ?: throw ApiException("Xəbər tapılmadı.", 404)

    suspend fun create(
        title: String,
        summary: String,
        content: String,
        fileAttachmentIds: List<String>,
    ): String {
        val response = api.createArticle(
            ArtArticleInput(
                title = title.trim(),
                summary = summary.trim(),
                content = content.trim(),
                fileAttachmentIds = fileAttachmentIds.takeIf { it.isNotEmpty() },
            ),
        )
        return response.data?.id
            ?: throw ApiException("Xəbər yaradıla bilmədi.")
    }

    suspend fun update(
        id: String,
        title: String,
        summary: String,
        content: String,
        fileAttachmentIds: List<String>,
    ) {
        api.updateArticle(
            id,
            ArtArticleUpdateBody(
                id = id,
                title = title.trim(),
                summary = summary.trim(),
                content = content.trim(),
                fileAttachmentIds = fileAttachmentIds.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun publish(id: String) {
        api.publishArticle(id)
    }

    suspend fun delete(id: String) {
        api.deleteArticle(id)
    }

    suspend fun uploadFile(
        articleId: String,
        fileName: String,
        mimeType: String?,
        file: File,
    ): ArtArticleAttachment {
        val mediaType = (mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            .toMediaTypeOrNull()
        val body = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData(
            name = "File",
            filename = fileName,
            body = body,
        )
        val textType = "text/plain".toMediaType()
        val response = api.uploadArticleFile(
            file = part,
            articleId = articleId.toRequestBody(textType),
            isPublic = "true".toRequestBody(textType),
        )
        return response.data ?: throw ApiException("Fayl yüklənmədi.")
    }

    suspend fun deleteFile(fileId: String) {
        api.deleteArticleFile(fileId)
    }
}
