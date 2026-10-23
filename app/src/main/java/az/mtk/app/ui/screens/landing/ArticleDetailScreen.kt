package az.mtk.app.ui.screens.landing

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.AppConfig
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ArtArticle
import az.mtk.app.data.api.ArtArticleAttachment
import az.mtk.app.data.api.LandingRepository
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.parseBBCode
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    repository: LandingRepository,
    onBack: () -> Unit,
    onNavigateToNews: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    var article by remember { mutableStateOf<ArtArticle?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(articleId) {
        loading = true
        error = null
        try {
            val result = repository.getArticle(articleId)
            if (result != null) {
                article = result
                error = null
            } else {
                article = null
                error = "Xəbər tapılmadı"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            article = null
            error = "Xəta: ${e.message ?: "Naməlum xəta"}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xəbər", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MtkColors.Primary)
            }
        } else if (article == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error ?: "Xəbər tapılmadı",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(onClick = onBack) {
                        Text("Geri qayıt")
                    }
                }
            }
        } else {
            ArticleContent(
                article = article!!,
                onNavigateToNews = onNavigateToNews,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ArticleContent(
    article: ArtArticle,
    onNavigateToNews: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val publishedAt = article.publishedAt ?: article.createdAt ?: ""
    val dateLabel = try {
        Format.date(publishedAt)
    } catch (e: Exception) {
        publishedAt
    }

    val publicFiles = article.fileAttachments?.filter { it.isPublic } ?: emptyList()
    val imageFiles = publicFiles.filter { isImageFile(it) }
    val downloadableFiles = publicFiles.filter { !isImageFile(it) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category and date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MtkColors.Primary, RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "Xəbərlər",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MtkColors.Primary
                    )
                    Text(
                        text = "/",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 32.sp
                )

                // Summary
                if (!article.summary.isNullOrBlank()) {
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }

                // Author
                if (!article.createdByFullName.isNullOrBlank()) {
                    Text(
                        text = "Müəllif: ${article.createdByFullName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Content
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!article.content.isNullOrBlank()) {
                        Text(
                            text = parseBBCode(article.content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // Image files
        if (imageFiles.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
            }

            items(imageFiles) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "${AppConfig.ApiBaseUrl}api/communications/fileattachments/${file.id}/download",
                                contentDescription = file.fileName,
                                imageLoader = AppGraph.imageLoader,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                contentScale = ContentScale.Fit,
                                onState = { imageState = it }
                            )

                            when (val state = imageState) {
                                is AsyncImagePainter.State.Loading -> {
                                    CircularProgressIndicator(
                                        color = MtkColors.Primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                is AsyncImagePainter.State.Error -> {
                                    val errorMsg = state.result.throwable.message ?: "Naməlum xəta"
                                    android.util.Log.e("ArticleDetail", "Image load error for ${file.id}: $errorMsg", state.result.throwable)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrokenImage,
                                            contentDescription = "Şəkil yüklənmədi",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Şəkil yüklənmədi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = file.fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> { /* Success or Empty - image is displayed */ }
                            }
                        }

                        if (file.fileName.isNotEmpty()) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Downloadable files
        if (downloadableFiles.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Əlavə fayllar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(downloadableFiles) { file ->
                var isDownloading by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable(enabled = !isDownloading) {
                            scope.launch {
                                isDownloading = true
                                try {
                                    android.util.Log.d("ArticleDetail", "Downloading file: ${file.id} - ${file.fileName}")
                                    val result = FileRepository.downloadToInternal(
                                        context = context,
                                        path = "api/communications/fileattachments/${file.id}/download",
                                        fallbackName = file.fileName,
                                    )
                                    android.util.Log.d("ArticleDetail", "File downloaded: ${result.fileName}")
                                    val opened = FileRepository.openFile(context, result.file)
                                    if (!opened) {
                                        Toast.makeText(
                                            context,
                                            "Fayl yükləndi: ${result.fileName}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Fayl açılır...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ArticleDetail", "Download error for ${file.id}: ${e.message}", e)
                                    val errorMsg = when (e) {
                                        is az.mtk.app.data.api.ApiException -> "Fayl yüklənmədi (${e.status}): ${e.message}"
                                        else -> "Xəta: ${e.message ?: "Fayl yüklənmədi"}"
                                    }
                                    Toast.makeText(
                                        context,
                                        errorMsg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isDownloading = false
                                }
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File type badge
                        val extension = file.fileName.substringAfterLast(".", "FILE").uppercase()
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MtkColors.Primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = extension,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MtkColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            file.mimeType?.takeIf { it.isNotBlank() }?.let { mimeType ->
                                Text(
                                    text = mimeType.uppercase(),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = MtkColors.Primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Endir",
                                tint = MtkColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Footer
        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onNavigateToNews() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MTK · Xəbərlər və elanlar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Digər xəbərlərə bax",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MtkColors.Primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MtkColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun isImageFile(file: ArtArticleAttachment): Boolean {
    val mimeType = file.mimeType?.trim()?.lowercase() ?: ""
    val imageExtensions = listOf(".avif", ".bmp", ".gif", ".jpg", ".jpeg", ".png", ".svg", ".tiff", ".tif", ".webp")
    return mimeType.startsWith("image/") || imageExtensions.any { file.fileName.endsWith(it, ignoreCase = true) }
}
