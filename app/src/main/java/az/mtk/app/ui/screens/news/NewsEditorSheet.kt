package az.mtk.app.ui.screens.news

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val MAX_FILE_BYTES = 20L * 1024 * 1024
private const val TITLE_MAX = 500
private const val SUMMARY_MAX = 1000

@Composable
internal fun NewsEditorSheet(
    editor: NewsEditorState,
    viewModel: NewsViewModel,
) {
    when {
        editor.loading -> {
            AppSheet(
                onDismissRequest = viewModel::closeEditor,
                containerColor = OpsColors.Background,
            ) {
                Text(
                    text = "Xəbər yüklənir",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                OpsLoading()
            }
        }

        editor.loadError != null -> {
            AppSheet(
                onDismissRequest = viewModel::closeEditor,
                containerColor = OpsColors.Background,
            ) {
                Text(
                    text = "Xəbər yüklənmədi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(8.dp))
                NewsInlineError(editor.loadError)
                Spacer(Modifier.height(12.dp))
                OpsPrimaryButton(
                    text = "Yenidən cəhd et",
                    onClick = viewModel::retryEditor,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OpsSecondaryButton(
                    text = "Bağla",
                    onClick = viewModel::closeEditor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        else -> NewsEditorForm(editor = editor, viewModel = viewModel)
    }
}

@Composable
private fun NewsEditorForm(
    editor: NewsEditorState,
    viewModel: NewsViewModel,
) {
    val article = editor.article
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(article?.id, editor.articleId) {
        mutableStateOf(article?.title.orEmpty())
    }
    var summary by remember(article?.id, editor.articleId) {
        mutableStateOf(article?.summary.orEmpty())
    }
    var content by remember(article?.id, editor.articleId) {
        mutableStateOf(article?.content.orEmpty())
    }
    var pending by remember(article?.id, editor.articleId) {
        mutableStateOf<List<NewsPendingFile>>(emptyList())
    }
    var picking by remember { mutableStateOf(false) }
    var pickError by remember { mutableStateOf<String?>(null) }

    val latestPending by rememberUpdatedState(pending)

    DisposableEffect(Unit) {
        onDispose {
            latestPending.forEach { it.file.delete() }
        }
    }

    LaunchedEffect(editor.uploadedClientIds) {
        if (editor.uploadedClientIds.isNotEmpty()) {
            pending.filter { it.clientId in editor.uploadedClientIds }
                .forEach { it.file.delete() }
            pending = pending.filterNot { it.clientId in editor.uploadedClientIds }
        }
    }

    LaunchedEffect(editor.done) {
        if (editor.done) {
            pending.forEach { it.file.delete() }
            pending = emptyList()
            viewModel.closeEditor()
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                picking = true
                pickError = null
                val result = copyPickedFiles(context, uris)
                pending = pending + result.added
                picking = false
                if (result.rejected.isNotEmpty()) {
                    pickError = "«${result.rejected.first()}» seçilmədi: maksimum 20 MB."
                }
            }
        }
    }

    val isFreshNew = article == null && editor.createdId == null
    val effectiveId = article?.id ?: editor.createdId
    val canSave = title.isNotBlank() &&
        summary.isNotBlank() &&
        content.isNotBlank() &&
        !editor.saving &&
        !picking

    fun save(publish: Boolean) {
        viewModel.saveArticle(
            articleId = effectiveId,
            title = title,
            summary = summary,
            content = content,
            existingFileIds = editor.files.map { it.id },
            pendingFiles = pending,
            publish = publish,
        )
    }

    AppSheet(
        onDismissRequest = { if (!editor.saving) viewModel.closeEditor() },
        scrollable = true,
        containerColor = OpsColors.Background,
        modifier = Modifier.imePadding(),
    ) {
        Text(
            text = if (isFreshNew) "Yeni xəbər" else "Xəbəri redaktə et",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = if (isFreshNew) {
                "Qaralama kimi saxlayın və ya dərhal yayımlayın."
            } else {
                "Başlıq, məzmun və əlavə faylları yeniləyin."
            },
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(14.dp))

        OpsTextField(
            value = title,
            onValueChange = {
                title = it.take(TITLE_MAX)
                viewModel.clearEditorError()
            },
            label = "Başlıq",
            placeholder = "Xəbərin başlığı",
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = summary,
            onValueChange = {
                summary = it.take(SUMMARY_MAX)
                viewModel.clearEditorError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Qısa məzmun", fontSize = 12.sp) },
            placeholder = {
                Text(
                    "Siyahıda və ana səhifədə göstərilən 1–2 cümlə",
                    fontSize = 13.sp,
                    color = OpsColors.MutedSoft,
                )
            },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp),
            colors = editorFieldColors(),
        )
        Text(
            text = "${summary.length}/$SUMMARY_MAX",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, end = 4.dp),
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = content,
            onValueChange = {
                content = it
                viewModel.clearEditorError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mətn", fontSize = 12.sp) },
            placeholder = {
                Text(
                    "Xəbərin tam mətni. BBCode dəstəklənir: [b]qalın[/b], [url=…]link[/url].",
                    fontSize = 13.sp,
                    color = OpsColors.MutedSoft,
                )
            },
            minLines = 12,
            maxLines = 20,
            shape = RoundedCornerShape(12.dp),
            colors = editorFieldColors(),
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Əlavə fayllar",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "Maksimum 20 MB. Fayllar xəbər saxlanılanda yüklənir və ictimai olur.",
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(6.dp))

        if (editor.files.isEmpty() && pending.isEmpty()) {
            Text(
                text = "Bu xəbərə fayl əlavə olunmayıb.",
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
            )
        } else {
            editor.files.forEachIndexed { index, file ->
                NewsFileRow(
                    name = file.fileName,
                    supporting = file.mimeType,
                    actionLabel = if (editor.fileBusyId == file.id) "Silinir…" else "Sil",
                    enabled = !editor.saving && editor.fileBusyId == null,
                    onAction = { viewModel.deleteEditorFile(file) },
                )
                if (index != editor.files.lastIndex || pending.isNotEmpty()) {
                    HorizontalDivider(color = OpsColors.Border)
                }
            }

            pending.forEachIndexed { index, file ->
                NewsFileRow(
                    name = file.fileName,
                    supporting = "${formatBytes(file.size)} · yüklənməyi gözləyir",
                    actionLabel = "Çıxar",
                    enabled = !editor.saving,
                    onAction = {
                        file.file.delete()
                        pending = pending.filterNot { it.clientId == file.clientId }
                    },
                )
                if (index != pending.lastIndex) {
                    HorizontalDivider(color = OpsColors.Border)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OpsSecondaryButton(
            text = if (picking) "Fayl seçilir…" else "+ Fayl əlavə et",
            onClick = { picker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !editor.saving && !picking,
        )

        if (pickError != null) {
            Spacer(Modifier.height(8.dp))
            NewsInlineError(pickError!!)
        }

        if (editor.error != null) {
            Spacer(Modifier.height(10.dp))
            NewsInlineError(editor.error)
        }

        Spacer(Modifier.height(16.dp))

        if (isFreshNew) {
            OpsPrimaryButton(
                text = if (editor.saving) "Saxlanılır…" else "Saxla və yayımla",
                onClick = { save(true) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            )
            Spacer(Modifier.height(8.dp))
            OpsSecondaryButton(
                text = "Qaralama kimi saxla",
                onClick = { save(false) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            )
        } else {
            OpsPrimaryButton(
                text = if (editor.saving) "Saxlanılır…" else "Yadda saxla",
                onClick = { save(false) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            )
            Spacer(Modifier.height(8.dp))
            OpsSecondaryButton(
                text = "Bağla",
                onClick = viewModel::closeEditor,
                modifier = Modifier.fillMaxWidth(),
                enabled = !editor.saving,
            )
        }

        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = viewModel::closeEditor,
            modifier = Modifier.fillMaxWidth(),
            enabled = !editor.saving,
        ) {
            Text(
                text = if (isFreshNew) "Ləğv et" else "Dəyişiklikləri burax",
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
            )
        }
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OpsColors.Terracotta,
    unfocusedBorderColor = OpsColors.Border,
    focusedContainerColor = OpsColors.Surface,
    unfocusedContainerColor = OpsColors.Surface,
)

private data class PickedFiles(
    val added: List<NewsPendingFile>,
    val rejected: List<String>,
)

private suspend fun copyPickedFiles(
    context: Context,
    uris: List<Uri>,
): PickedFiles = withContext(Dispatchers.IO) {
    val added = mutableListOf<NewsPendingFile>()
    val rejected = mutableListOf<String>()

    uris.forEach { uri ->
        val (name, size) = queryNameAndSize(context, uri)
        if (size != null && size > MAX_FILE_BYTES) {
            rejected += name
            return@forEach
        }

        val target = File(context.cacheDir, "news-${UUID.randomUUID()}-${safeFileName(name)}")
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Fayl açıla bilmədi")
            stream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (target.length() > MAX_FILE_BYTES) {
                target.delete()
                rejected += name
                return@forEach
            }
            added += NewsPendingFile(
                clientId = UUID.randomUUID().toString(),
                file = target,
                fileName = name,
                mimeType = context.contentResolver.getType(uri),
                size = target.length(),
            )
        } catch (_: Throwable) {
            target.delete()
            rejected += name
        }
    }

    PickedFiles(added = added, rejected = rejected)
}

private fun queryNameAndSize(context: Context, uri: Uri): Pair<String, Long?> {
    var name = "fayl-${System.currentTimeMillis()}"
    var size: Long? = null

    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
    }

    return name to size
}

private fun safeFileName(name: String): String = name
    .substringAfterLast('/')
    .substringAfterLast('\\')
    .replace(Regex("[^\\p{L}\\p{N}._-]"), "_")
    .takeLast(60)
    .ifBlank { "file" }
