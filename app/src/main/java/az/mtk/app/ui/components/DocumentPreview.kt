package az.mtk.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import az.mtk.app.data.files.FileRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Tətbiqin içində göstəriləcək sənəd (PDF və ya şəkil). */
data class PreviewDocument(
    val filePath: String,
    val fileName: String,
    val mimeHint: String? = null,
    /** Başlıqda göstərilən insan-oxunaqlı ad (məs. "Qəbz", "Müqavilə №12"). */
    val title: String? = null,
)

private object ViewerColors {
    val Backdrop = Color(0xFF111413)
    val BackdropSoft = Color(0xFF1B1F1D)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFFF4F6F2)
    val Muted = Color(0xFF98A29B)
    val Accent = Color(0xFFC9E3A1)
    val Error = Color(0xFFE9A3A3)
}

/**
 * İstənilən ekrandan sənəd önizləməsini idarə edən yüngül state holder.
 * `show(...)` çağırılır, ekranın kökünə `DocumentPreviewHost(controller)` qoyulur.
 */
class DocumentPreviewController {
    var document by mutableStateOf<PreviewDocument?>(null)
        private set

    fun show(document: PreviewDocument) {
        this.document = document
    }

    fun show(
        filePath: String,
        fileName: String,
        mimeHint: String? = null,
        title: String? = null,
    ) {
        document = PreviewDocument(filePath, fileName, mimeHint, title)
    }

    fun dismiss() {
        document = null
    }
}

@Composable
fun rememberDocumentPreviewController(): DocumentPreviewController = remember { DocumentPreviewController() }

@Composable
fun DocumentPreviewHost(controller: DocumentPreviewController) {
    controller.document?.let { document ->
        DocumentPreviewDialog(document = document, onClose = controller::dismiss)
    }
}

/** Fayl endirildikdən sonra `PreviewDocument` yaradan köməkçi. */
fun PreviewDocument(
    result: FileRepository.DownloadResult,
    mimeHint: String? = null,
    title: String? = null,
): PreviewDocument = PreviewDocument(
    filePath = result.file.absolutePath,
    fileName = result.fileName,
    mimeHint = mimeHint,
    title = title,
)

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

/** Yalnız PDF və şəkillər tətbiqin içində göstərilə bilər; qalanları xarici proqramda açılır. */
fun PreviewDocument.supportsInAppPreview(): Boolean {
    val name = fileName.lowercase()
    val mime = mimeHint.orEmpty().lowercase()
    val extension = name.substringAfterLast('.', "")
    return mime.startsWith("image/") || mime == "application/pdf" ||
        extension in IMAGE_EXTENSIONS || extension == "pdf"
}

private fun PreviewDocument.isImage(): Boolean {
    val name = fileName.lowercase()
    val mime = mimeHint.orEmpty().lowercase()
    return mime.startsWith("image/") || name.substringAfterLast('.', "") in IMAGE_EXTENSIONS
}

/**
 * Sənədi yükləmədən birbaşa ekranda göstərir: PDF səhifələri `PdfRenderer` ilə
 * şəklə çevrilir, şəkillər isə olduğu kimi göstərilir.
 */
@Composable
fun DocumentPreviewDialog(document: PreviewDocument, onClose: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ViewerColors.Backdrop,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                ViewerTopBar(
                    title = document.title ?: document.fileName,
                    subtitle = if (document.isImage()) "Şəkil" else "PDF · iki barmaqla yaxınlaşdırın",
                    onClose = onClose,
                    onOpenExternal = {
                        FileRepository.openFile(context, File(document.filePath))
                    },
                )

                if (document.isImage()) {
                    ImagePreview(document)
                } else {
                    PdfPages(path = document.filePath)
                }
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundIconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Bağla", tint = ViewerColors.Ink, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ViewerColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = ViewerColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RoundIconButton(onClick = onOpenExternal) {
            Icon(
                Icons.Default.OpenInNew,
                "Başqa proqramda aç",
                tint = ViewerColors.Muted,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) { content() }
    }
}

@Composable
private fun ImagePreview(document: PreviewDocument) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(bottom = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ViewerColors.Paper,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = File(document.filePath),
                contentDescription = document.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun PdfPages(path: String) {
    var pageCount by remember(path) { mutableStateOf<Int?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    LaunchedEffect(path) {
        runCatching { pdfPageCount(path) }
            .onSuccess { pageCount = it }
            .onFailure { error = it.message ?: "Sənəd açıla bilmədi" }
    }

    when {
        error != null -> ViewerError(error!!)
        pageCount == null -> ViewerLoading()
        pageCount == 0 -> ViewerError("Sənəddə səhifə yoxdur.")
        else -> Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(pageCount!!) { index ->
                    PdfPageItem(path = path, index = index)
                }
            }

            if (pageCount!! > 1) {
                PageIndicator(
                    current = currentPage.coerceIn(1, pageCount!!),
                    total = pageCount!!,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                )
            }
        }
    }
}

/** Hər səhifə yalnız görünəndə render olunur — yaddaş uzun sənədlərdə də sabit qalır. */
@Composable
private fun PdfPageItem(path: String, index: Int) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path, index) {
        value = withContext(Dispatchers.IO) { runCatching { renderPdfPage(path, index) }.getOrNull() }
    }

    DisposableEffect(bitmap) {
        val current = bitmap
        onDispose { current?.recycle() }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ViewerColors.Paper,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (bitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF8BA46A),
                    strokeWidth = 2.dp,
                )
            }
        } else {
            ZoomablePage(bitmap = bitmap!!, label = "Səhifə ${index + 1}")
        }
    }
}

/**
 * Səhifəni iki barmaqla yaxınlaşdırma / uzaqlaşdırma və böyüdülmüş halda sürüşdürmə.
 * Səhifə böyüdülməyəndə bir barmaq hərəkəti siyahıya verilir — sənəd rahat sürüşür.
 */
@Composable
private fun ZoomablePage(bitmap: Bitmap, label: String) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val maxScale = 4f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it }
            .clipToBounds()
            .pointerInput(bitmap, viewport) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (pointers > 1) {
                            val next = (scale * zoomChange).coerceIn(1f, maxScale)
                            scale = next
                            offset = if (next <= 1f) Offset.Zero else clampOffset(offset + panChange, next, viewport)
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            offset = clampOffset(offset + panChange, scale, viewport)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(bitmap, viewport) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val target = 2.5f
                            val center = Offset(viewport.width / 2f, viewport.height / 2f)
                            scale = target
                            offset = clampOffset((center - tap) * (target - 1f), target, viewport)
                        }
                    },
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}

private fun clampOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    if (scale <= 1f || viewport.width == 0 || viewport.height == 0) return Offset.Zero
    val maxX = viewport.width * (scale - 1f) / 2f
    val maxY = viewport.height * (scale - 1f) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

@Composable
private fun PageIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.1f),
    ) {
        Text(
            text = "$current / $total",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ViewerColors.Ink,
        )
    }
}

@Composable
private fun ViewerLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            color = ViewerColors.Accent,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(10.dp))
        Text("Sənəd hazırlanır…", fontSize = 11.sp, color = ViewerColors.Muted)
    }
}

@Composable
private fun ViewerError(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sənəd göstərilə bilmədi",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ViewerColors.Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = ViewerColors.Muted,
        )
    }
}

private suspend fun pdfPageCount(path: String): Int = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists()) throw IllegalStateException("Fayl tapılmadı")
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }
}

private suspend fun renderPdfPage(path: String, index: Int): Bitmap = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists()) throw IllegalStateException("Fayl tapılmadı")
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(index).use { page ->
                val targetWidth = 1240
                val height = (targetWidth.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }
}
