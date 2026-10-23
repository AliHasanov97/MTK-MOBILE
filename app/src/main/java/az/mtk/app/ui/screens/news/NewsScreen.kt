package az.mtk.app.ui.screens.news

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.ArtArticle
import az.mtk.app.data.api.ArtArticleAttachment
import az.mtk.app.data.api.ArtArticleListItem
import az.mtk.app.data.api.ArticleStatus
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPagination
import az.mtk.app.ui.screens.operations.OpsPill
import az.mtk.app.ui.screens.operations.OpsPillRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTopBar
import az.mtk.app.util.Format
import az.mtk.app.util.parseBBCode
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch

/**
 * Xəbərlər modulu — yayım, qaralama və arxivdə olan elanların mobil idarəsi.
 * Cədvəl yoxdur: kart siyahısı, sürüşən süzgəclər və aşağıdan açılan panellər.
 */
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    viewModel: NewsViewModel = mtkViewModel { NewsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deletingArticle by remember { mutableStateOf<ArtArticle?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val actionError = state.actionError
    val notice = state.notice
    val listError = state.error

    LaunchedEffect(Unit) { viewModel.ensureLoaded() }

    LaunchedEffect(state.actionBusyId) {
        if (pendingDeleteId != null && state.actionBusyId == null) {
            deletingArticle = null
            pendingDeleteId = null
        }
    }

    androidx.compose.material3.Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = "Xəbərlər",
                subtitle = "Elanlar · Yayım · Arxiv",
                onBack = onBack,
                onRefresh = viewModel::refresh,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                NewsSummaryCard(
                    totalCount = state.totalCount,
                    filtered = state.status != null || state.search.isNotBlank(),
                )
            }

            item {
                OpsPrimaryButton(
                    text = "+ Yeni xəbər",
                    onClick = viewModel::openNewEditor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }

            item {
                OpsPillRow(
                    pills = listOf(
                        OpsPill(
                            label = "Hamısı",
                            selected = state.status == null,
                            onClick = { viewModel.setStatus(null) },
                        ),
                        OpsPill(
                            label = ArticleStatus.label(ArticleStatus.DRAFT),
                            selected = state.status == ArticleStatus.DRAFT,
                            onClick = { viewModel.setStatus(ArticleStatus.DRAFT) },
                        ),
                        OpsPill(
                            label = ArticleStatus.label(ArticleStatus.PUBLISHED),
                            selected = state.status == ArticleStatus.PUBLISHED,
                            onClick = { viewModel.setStatus(ArticleStatus.PUBLISHED) },
                        ),
                        OpsPill(
                            label = ArticleStatus.label(ArticleStatus.ARCHIVED),
                            selected = state.status == ArticleStatus.ARCHIVED,
                            onClick = { viewModel.setStatus(ArticleStatus.ARCHIVED) },
                        ),
                    ),
                )
            }

            item {
                OpsSearchField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    placeholder = "Başlıq və ya məzmun üzrə…",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (actionError != null) {
                item {
                    OpsFeedback(
                        message = actionError,
                        isError = true,
                        onDismiss = viewModel::clearFeedback,
                    )
                }
            }

            if (notice != null) {
                item {
                    OpsFeedback(
                        message = notice,
                        isError = false,
                        onDismiss = viewModel::clearFeedback,
                    )
                }
            }

            if (listError != null && state.items.isNotEmpty()) {
                item {
                    OpsFeedback(
                        message = listError,
                        isError = true,
                        onDismiss = viewModel::refresh,
                    )
                }
            }

            when {
                state.loading && state.items.isEmpty() -> item { OpsLoading() }

                listError != null && state.items.isEmpty() -> item {
                    OpsError(listError, onRetry = viewModel::refresh)
                }

                state.items.isEmpty() -> item {
                    OpsEmpty(
                        title = if (state.search.isNotBlank() || state.status != null) {
                            "Xəbər tapılmadı"
                        } else {
                            "Hələ xəbər yoxdur"
                        },
                        text = if (state.search.isNotBlank() || state.status != null) {
                            "Süzgəci dəyişin və ya axtarış sözünü təmizləyin."
                        } else {
                            "İlk xəbəri yaratmaq üçün «+ Yeni xəbər» düyməsini basın."
                        },
                    )
                }

                else -> items(state.items, key = { it.id }) { article ->
                    NewsCard(
                        article = article,
                        publishBusy = state.actionBusyId == article.id,
                        actionsEnabled = state.actionBusyId == null,
                        onOpen = { viewModel.openDetail(article) },
                        onPublish = { viewModel.publishArticle(article.id) },
                    )
                }
            }

            if (state.totalCount > state.pageSize) {
                item {
                    OpsPagination(
                        page = state.page,
                        total = state.totalCount,
                        pageSize = state.pageSize,
                        onPrev = viewModel::prevPage,
                        onNext = viewModel::nextPage,
                    )
                }
            }
        }
    }

    if (state.detail.id != null) {
        NewsDetailSheet(
            detail = state.detail,
            actionBusy = state.actionBusyId != null,
            actionError = state.actionError,
            onDismiss = { viewModel.closeDetail() },
            onRetry = viewModel::retryDetail,
            onClearError = viewModel::clearFeedback,
            onPublish = viewModel::publishArticle,
            onEdit = { article ->
                viewModel.closeDetail(refresh = false)
                viewModel.openEditor(article)
            },
            onDelete = { article ->
                viewModel.closeDetail(refresh = false)
                deletingArticle = article
            },
        )
    }

    state.editor?.let { editor ->
        NewsEditorSheet(
            editor = editor,
            viewModel = viewModel,
        )
    }

    deletingArticle?.let { article ->
        NewsDeleteSheet(
            title = article.title,
            busy = state.actionBusyId == article.id,
            onConfirm = {
                pendingDeleteId = article.id
                viewModel.deleteArticle(article.id)
            },
            onDismiss = {
                deletingArticle = null
                pendingDeleteId = null
            },
        )
    }
}

@Composable
private fun NewsCard(
    article: ArtArticleListItem,
    publishBusy: Boolean,
    actionsEnabled: Boolean,
    onOpen: () -> Unit,
    onPublish: () -> Unit,
) {
    OpsCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewsStatusChip(article.status)
                    NewsAttachmentChip(article.fileAttachmentCount ?: 0)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!article.summary.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = article.summary,
                        fontSize = 11.sp,
                        color = OpsColors.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(9.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.createdByFullName?.takeIf { it.isNotBlank() } ?: "Müəllif yoxdur",
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(Format.date(article.publishedAt ?: article.createdAt))
                        append(" · ")
                        append("${article.viewCount ?: 0} baxış")
                    },
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }

            if (article.status == ArticleStatus.DRAFT) {
                TextButton(
                    onClick = onPublish,
                    enabled = actionsEnabled,
                ) {
                    Text(
                        text = if (publishBusy) "Yayımlanır…" else "Yayımla",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpsColors.Terracotta,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsDetailSheet(
    detail: NewsDetailState,
    actionBusy: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onPublish: (String) -> Unit,
    onEdit: (ArtArticle) -> Unit,
    onDelete: (ArtArticle) -> Unit,
) {
    val article = detail.article

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = article != null,
        containerColor = OpsColors.Background,
    ) {
        if (detail.loading) {
            Text(
                text = "Xəbər",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            OpsLoading()
        } else if (detail.error != null || article == null) {
            Text(
                text = "Xəbər yüklənmədi",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            Spacer(Modifier.height(10.dp))
            NewsInlineError(detail.error ?: "Xəbər tapılmadı.")
            Spacer(Modifier.height(12.dp))
            OpsPrimaryButton(
                text = "Yenidən cəhd et",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OpsSecondaryButton(
                text = "Bağla",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ArticleDetailContent(
                article = article,
                actionBusy = actionBusy,
                actionError = actionError,
                onClearError = onClearError,
                onPublish = { onPublish(article.id) },
                onEdit = { onEdit(article) },
                onDelete = { onDelete(article) },
                onClose = onDismiss,
            )
        }
    }
}

@Composable
private fun ArticleDetailContent(
    article: ArtArticle,
    actionBusy: Boolean,
    actionError: String?,
    onClearError: () -> Unit,
    onPublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadingId by remember(article.id) { mutableStateOf<String?>(null) }
    var downloadError by remember(article.id) { mutableStateOf<String?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        NewsStatusChip(article.status)
        Spacer(Modifier.width(6.dp))
        Text(
            text = Format.date(article.publishedAt ?: article.createdAt),
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = article.title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OpsColors.Ink,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = buildString {
            append(article.createdByFullName?.takeIf { it.isNotBlank() } ?: "Müəllif yoxdur")
            append(" · ")
            append("${article.viewCount ?: 0} baxış")
        },
        fontSize = 10.sp,
        color = OpsColors.MutedSoft,
    )

    if (!article.summary.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OpsColors.Subtle,
        ) {
            Text(
                text = article.summary,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = OpsColors.Muted,
            )
        }
    }

    if (!article.content.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(
                text = parseBBCode(article.content),
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = OpsColors.Ink,
            )
        }
    }

    val files = article.fileAttachments.orEmpty()
    if (files.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Fayllar (${files.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(4.dp))
        files.forEachIndexed { index, file ->
            NewsFileRow(
                name = file.fileName,
                supporting = file.mimeType,
                actionLabel = if (downloadingId == file.id) "Yüklənir…" else "Endir",
                enabled = downloadingId == null,
                onAction = {
                    scope.launch {
                        downloadingId = file.id
                        downloadError = null
                        try {
                            val result = FileRepository.downloadToInternal(
                                context = context,
                                path = "api/communications/fileattachments/${file.id}/download",
                                fallbackName = file.fileName,
                            )
                            if (!FileRepository.openFile(context, result.file)) {
                                Toast.makeText(
                                    context,
                                    "Fayl yükləndi: ${result.fileName}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } catch (failure: Throwable) {
                            downloadError = failure.toUserMessage()
                        } finally {
                            downloadingId = null
                        }
                    }
                },
            )
            if (index != files.lastIndex) {
                HorizontalDivider(color = OpsColors.Border)
            }
        }
    }

    if (downloadError != null) {
        Spacer(Modifier.height(8.dp))
        NewsInlineError(downloadError!!)
    }

    if (actionError != null) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NewsInlineError(actionError, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearError) {
                Text("Bağla", fontSize = 11.sp, color = OpsColors.Muted)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = OpsColors.Border)
    Spacer(Modifier.height(12.dp))

    if (article.status == ArticleStatus.DRAFT) {
        OpsPrimaryButton(
            text = if (actionBusy) "Yayımlanır…" else "Yayımla",
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionBusy,
        )
        Spacer(Modifier.height(8.dp))
        OpsSecondaryButton(
            text = "Redaktə et",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionBusy,
        )
    } else {
        OpsPrimaryButton(
            text = "Redaktə et",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionBusy,
        )
    }

    Spacer(Modifier.height(8.dp))

    OpsSecondaryButton(
        text = "Sil",
        onClick = onDelete,
        modifier = Modifier.fillMaxWidth(),
        enabled = !actionBusy,
        danger = true,
    )

    Spacer(Modifier.height(8.dp))

    OpsSecondaryButton(
        text = "Bağla",
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
        enabled = !actionBusy,
    )
}

@Composable
private fun NewsDeleteSheet(
    title: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = "Xəbəri sil",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "«$title» xəbəri silinsin? Bu əməliyyat geri qaytarılmır.",
            fontSize = 13.sp,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Ləğv et",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
            OpsPrimaryButton(
                text = if (busy) "Silinir…" else "Sil",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
        }
    }
}
