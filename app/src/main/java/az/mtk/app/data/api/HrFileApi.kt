package az.mtk.app.data.api

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import az.mtk.app.AppConfig
import az.mtk.app.AppGraph
import az.mtk.app.data.files.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Ərizə/əmrin imzalanmış nüsxəsinin fayl əlavəsi kimi yüklənməsi
 * (MTK-UI `uploadAttachment` / `downloadAttachment` qarşılığı).
 */
object HrFileApi {

    private const val MAX_BYTES = 20L * 1024 * 1024
    private const val SIGNED_DOCUMENT_TYPE = 10

    suspend fun upload(context: Context, uri: Uri, fkColumn: String, recordId: String) =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = displayName(context, uri) ?: "sened.pdf"
            val mime = resolver.getType(uri)
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw ApiException("Fayl açıla bilmədi")

            if (bytes.size > MAX_BYTES) throw ApiException("Faylın ölçüsü 20 MB-dan çox ola bilməz.")

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("File", name, bytes.toRequestBody(mime?.toMediaTypeOrNull()))
                .addFormDataPart(fkColumn, recordId)
                .addFormDataPart("DocumentType", SIGNED_DOCUMENT_TYPE.toString())
                .build()

            val request = Request.Builder()
                .url(url("api/hr/fileattachments"))
                .post(body)
                .build()

            AppGraph.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = response.body?.string().orEmpty()
                    throw ApiException(
                        error.takeIf { it.isNotBlank() } ?: "Fayl yüklənmədi (${response.code})",
                        response.code,
                    )
                }
            }
            Unit
        }

    suspend fun download(context: Context, id: String, fallbackName: String? = null) =
        FileRepository.downloadToInternal(
            context = context,
            path = "api/hr/fileattachments/$id/download",
            fallbackName = fallbackName,
        )

    private fun url(path: String): String =
        AppConfig.ApiBaseUrl.removeSuffix("/") + "/" + path.removePrefix("/")

    private fun displayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
        }.getOrNull()
}
