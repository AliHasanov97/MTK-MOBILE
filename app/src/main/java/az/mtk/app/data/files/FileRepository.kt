package az.mtk.app.data.files

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import az.mtk.app.AppConfig
import az.mtk.app.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Downloads exported PDF/Excel files produced by the backend (receipts, reports,
 * payroll sheets, ...) into the public Downloads/MTK folder and opens them with
 * an external viewer via FileProvider — the Android counterpart of MTK-UI's
 * `apiFetchFile` helper.
 */
object FileRepository {

    data class DownloadResult(val fileName: String, val file: File)

    suspend fun download(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val url = AppConfig.ApiBaseUrl.removeSuffix("/") + "/" + path.removePrefix("/")
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .apply {
                if (method.equals("POST", ignoreCase = true)) {
                    post((body ?: "{}").toRequestBody(mediaType))
                }
            }
            .build()

        android.util.Log.d("FileRepository", "Downloading from: $url")
        AppGraph.httpClient.newCall(request).execute().use { response ->
            android.util.Log.d("FileRepository", "Response: ${response.code} ${response.message}")
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error details"
                android.util.Log.e("FileRepository", "Download failed: ${response.code} - $errorBody")
                throw az.mtk.app.data.api.ApiException("Fayl yüklənmədi (${response.code}): ${response.message}", response.code)
            }
            val fileName = resolveFileName(
                response.header("Content-Disposition"),
                path,
            )
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MTK",
            )
            if (!dir.exists()) dir.mkdirs()
            val target = uniqueFile(dir, fileName)
            response.body?.byteStream()?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            DownloadResult(target.name, target)
        }
    }

    /** Adds the downloaded file to the Downloads list and returns true if a viewer could be launched. */
    fun registerAndOpen(context: Context, result: DownloadResult): Boolean {
        return runCatching {
            val scanner = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            scanner.addCompletedDownload(
                result.fileName,
                "MTK ixrac faylı",
                true,
                guessMime(result.fileName),
                result.file.absolutePath,
                result.file.length(),
                true,
            )
            openFile(context, result.file)
        }.getOrDefault(false)
    }

    fun openFile(context: Context, file: File): Boolean {
        return runCatching {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, guessMime(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var i = 1
        while (candidate.exists()) {
            val name = if (ext.isBlank()) "$base ($i)" else "$base ($i).$ext"
            candidate = File(dir, name)
            i += 1
        }
        return candidate
    }

    private fun resolveFileName(disposition: String?, fallbackPath: String): String {
        disposition?.let {
            val star = Regex("filename\\*=UTF-8''([^;]+)").find(it)?.groupValues?.get(1)
            val plain = Regex("filename=\"?([^\";]+)\"?").find(it)?.groupValues?.get(1)
            (star?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() } ?: plain)
                ?.takeIf { name -> name.isNotBlank() }
                ?.let { return it.trim() }
        }
        val last = fallbackPath.substringAfterLast('/').substringBeforeLast('?')
        return if (last.isNotBlank()) last else "mtk-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}"
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        name.endsWith(".xls", true) -> "application/vnd.ms-excel"
        name.endsWith(".csv", true) -> "text/csv"
        else -> "application/octet-stream"
    }
}
