package az.mtk.app.ui.screens.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ArtArticle
import az.mtk.app.data.api.ArtArticleInput
import az.mtk.app.data.api.ArtArticleListItem
import az.mtk.app.data.api.ArtArticleUpdateBody
import az.mtk.app.data.api.ArticlesApi
import az.mtk.app.data.api.ArticleStatus
import az.mtk.app.data.api.Page
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticlesViewModel : ViewModel() {
    private val api = AppGraph.service(ArticlesApi::class.java)

    val status = MutableStateFlow<String?>(null)

    val pager = Pager<ArtArticleListItem>(viewModelScope) { page, query ->
        val filters = status.value?.let {
            listOf(az.mtk.app.data.api.QueryFilter("Status", az.mtk.app.data.api.Comparison.EQUALS, it))
        }
        val env = api.searchArticles(
            SearchBody(
                filters = filters,
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = query.trim().takeIf { it.isNotEmpty() },
                page = page,
                pageSize = 20,
            ),
        )
        val payload = env.data ?: throw az.mtk.app.data.api.ApiException(env.message?.takeIf { m -> m.isNotBlank() } ?: "Xəbərlər üçün boş cavab gəldi")
        Page(payload.data.orEmpty(), payload.totalCount, payload.page, payload.pageSize)
    }

    fun selectStatus(value: String?) {
        if (value == status.value) return
        status.value = value
        pager.refresh()
    }

    fun articleForEdit(id: String, onDone: (ArtArticle?, String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.article(id) }
                .onSuccess { onDone(it, null) }
                .onFailure { onDone(null, it.toUserMessage()) }
        }
    }

    fun save(
        articleId: String?,
        title: String,
        summary: String,
        content: String,
        publish: Boolean,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                var id = articleId
                if (id == null) {
                    id = api.createArticle(ArtArticleInput(title, summary, content)).data?.id
                        ?: throw az.mtk.app.data.api.ApiException("Xəbərin ID-si alınmadı")
                } else {
                    api.updateArticle(id, ArtArticleUpdateBody(id = id, title = title, summary = summary, content = content))
                }
                if (publish) api.publishArticle(id)
            }.onSuccess {
                pager.refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun publish(id: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.publishArticle(id) }
                .onSuccess { pager.refresh(); onDone(null) }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun delete(id: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.deleteArticle(id) }
                .onSuccess { pager.refresh(); onDone(null) }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }
}

private fun statusTone(status: String?): Tone = when (status) {
    ArticleStatus.PUBLISHED -> Tone.Success
    ArticleStatus.DRAFT -> Tone.Warning
    ArticleStatus.ARCHIVED -> Tone.Neutral
    else -> Tone.Neutral
}

@Composable
fun ArticlesAdminScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: ArticlesViewModel = mtkViewModel { ArticlesViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<ArtArticle?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ArtArticleListItem?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    val statusFilters: List<Pair<String, String?>> = listOf(
        "Hamısı" to null,
        "Qaralama" to ArticleStatus.DRAFT,
        "Yayımlanıb" to ArticleStatus.PUBLISHED,
        "Arxivdə" to ArticleStatus.ARCHIVED,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Xəbərlər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} xəbər",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Başlıq və ya məzmun üzrə axtar…",
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                statusFilters.forEach { (label, value) ->
                    FilterChip(
                        selected = status == value,
                        onClick = { viewModel.selectStatus(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MtkColors.Green,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = "+ Yeni xəbər", onClick = { creating = true })
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Xəbərlər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Xəbər tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { article ->
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    article.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!article.summary.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        article.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Pill(ArticleStatus.label(article.status), statusTone(article.status))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                append("Müəllif: ${article.createdByFullName ?: "—"}")
                                append(" · Yayım: ${if (article.publishedAt != null) Format.date(article.publishedAt) else "—"}")
                                append(" · Baxış: ${article.viewCount ?: 0}")
                                if ((article.fileAttachmentCount ?: 0) > 0) append(" · ${article.fileAttachmentCount} fayl")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MtkColors.MutedSoft,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (article.status == ArticleStatus.DRAFT) {
                                OutlinedButton(
                                    onClick = { viewModel.publish(article.id) { m -> editError = m } },
                                    content = { Text("Yayımla", fontSize = 12.sp) },
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.articleForEdit(article.id) { full, message ->
                                        if (full != null) editing = full else editError = message
                                    }
                                },
                                content = { Text("Redaktə", fontSize = 12.sp) },
                            )
                            OutlinedButton(onClick = { deleting = article }, content = {
                                Text("Sil", fontSize = 12.sp, color = MtkColors.Negative)
                            })
                        }
                    }
                }

                if (state.items.size < state.total) {
                    item {
                        OutlinedButton(
                            onClick = viewModel.pager::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = viewModel.pager::refresh) }
                }
            }
        }

        editError?.let { message ->
            Text(
                message,
                color = MtkColors.Negative,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (creating) {
        ArticleFormModal(
            article = null,
            onDismiss = { creating = false },
            onSave = { id, title, summary, content, publish, onDone ->
                viewModel.save(id, title, summary, content, publish, onDone)
            },
        )
    }

    editing?.let { article ->
        ArticleFormModal(
            article = article,
            onDismiss = { editing = null },
            onSave = { id, title, summary, content, publish, onDone ->
                viewModel.save(id, title, summary, content, publish, onDone)
            },
        )
    }

    deleting?.let { article ->
        ConfirmDialog(
            title = "Xəbəri sil",
            message = "${article.title} xəbəri silinsin? Bu əməliyyat geri qaytarılmır.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.delete(article.id) { deleting = null }
            },
        )
    }
}

@Composable
private fun ArticleFormModal(
    article: ArtArticle?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        summary: String,
        content: String,
        publish: Boolean,
        onDone: (String?) -> Unit,
    ) -> Unit,
) {
    var title by remember { mutableStateOf(article?.title ?: "") }
    var summary by remember { mutableStateOf(article?.summary ?: "") }
    var content by remember { mutableStateOf(article?.content ?: "") }
    var publishNow by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val valid = title.isNotBlank() && summary.isNotBlank() && content.isNotBlank()

    MtkModal(
        title = if (article == null) "Yeni xəbər" else "Xəbəri redaktə et",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else if (article == null && publishNow) "Saxla və yayımla" else "Yadda saxla",
        confirmEnabled = valid && !saving,
        onConfirm = {
            saving = true
            onSave(
                article?.id,
                title.trim(),
                summary.trim(),
                content.trim(),
                publishNow,
            ) { message ->
                saving = false
                if (message == null) onDismiss() else error = message
            }
        },
    ) {
        MtkTextField(title, { title = it }, "Başlıq", required = true)
        Spacer(Modifier.height(12.dp))
        MtkTextField(summary, { summary = it }, "Qısa məzmun", required = true, minLines = 2, singleLine = false)
        Spacer(Modifier.height(12.dp))
        MtkTextField(
            content,
            { content = it },
            "Mətn",
            required = true,
            singleLine = false,
            minLines = 6,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(
                checked = publishNow,
                onCheckedChange = { publishNow = it },
                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = MtkColors.Green),
            )
            Text(
                "Dərhal yayımla",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            "Yayımlanan xəbərlər ictimai saytın “Xəbərlər” səhifəsində və ana səhifədə görünür.",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}
