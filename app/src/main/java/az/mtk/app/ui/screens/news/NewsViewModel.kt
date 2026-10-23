package az.mtk.app.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.ArtArticle
import az.mtk.app.data.api.ArtArticleAttachment
import az.mtk.app.data.api.ArtArticleListItem
import az.mtk.app.data.api.ArticleStatus
import az.mtk.app.data.api.ApiException
import az.mtk.app.data.api.NewsRepository
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.OffsetDateTime

/** Yüklənməyi gözləyən, istifadəçinin seçdiyi fayl. */
data class NewsPendingFile(
    val clientId: String,
    val file: File,
    val fileName: String,
    val mimeType: String?,
    val size: Long,
)

data class NewsDetailState(
    val id: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val article: ArtArticle? = null,
)

data class NewsEditorState(
    /** Redaktə olunan xəbərin id-si; yeni xəbər üçün `null`. */
    val articleId: String? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val article: ArtArticle? = null,
    val files: List<ArtArticleAttachment> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    /** Yaradılmış, lakin fayl yükləməsi yarımçıq qalmış xəbərin id-si. */
    val createdId: String? = null,
    val uploadedClientIds: Set<String> = emptySet(),
    val fileBusyId: String? = null,
)

data class NewsUiState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val search: String = "",
    val status: String? = null,
    val page: Int = 0,
    val pageSize: Int = 10,
    val items: List<ArtArticleListItem> = emptyList(),
    val totalCount: Int = 0,
    val actionBusyId: String? = null,
    val actionError: String? = null,
    val notice: String? = null,
    val detail: NewsDetailState = NewsDetailState(),
    val editor: NewsEditorState? = null,
) {
    val pageCount: Int get() = maxOf(1, (totalCount + pageSize - 1) / pageSize)
}

/**
 * Xəbərlər modulu: serverdə səhifələnən siyahı, status süzgəci, tam mətn
 * axtarışı, redaktə və fayl idarəetməsi.
 */
class NewsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NewsUiState())
    val state = _state.asStateFlow()

    private var listRequest = 0
    private var searchJob: Job? = null
    private var detailRequest = 0
    private var editorRequest = 0

    // ---- Siyahı --------------------------------------------------------------

    fun ensureLoaded() {
        val current = _state.value
        if (!current.loaded && !current.loading) loadList()
    }

    fun refresh() = loadList()

    fun setSearch(value: String) {
        _state.value = _state.value.copy(
            search = value,
            page = 0,
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadList()
        }
    }

    fun setStatus(status: String?) {
        if (_state.value.status == status) return
        _state.value = _state.value.copy(status = status, page = 0)
        loadList()
    }

    fun nextPage() {
        val current = _state.value
        if (current.loading || current.page + 1 >= current.pageCount) return
        _state.value = current.copy(page = current.page + 1)
        loadList()
    }

    fun prevPage() {
        val current = _state.value
        if (current.loading || current.page <= 0) return
        _state.value = current.copy(page = current.page - 1)
        loadList()
    }

    private fun loadList(silent: Boolean = false) {
        val request = ++listRequest
        val snapshot = _state.value
        _state.value = snapshot.copy(
            loading = !silent || snapshot.items.isEmpty(),
            error = null,
        )

        viewModelScope.launch {
            try {
                val result = NewsRepository.search(
                    status = snapshot.status,
                    searchTerm = snapshot.search,
                    page = snapshot.page + 1,
                    pageSize = snapshot.pageSize,
                )
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(
                    loading = false,
                    loaded = true,
                    error = null,
                    items = result.data.orEmpty(),
                    totalCount = result.totalCount,
                )
            } catch (failure: Throwable) {
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(
                    loading = false,
                    loaded = true,
                    error = failure.toUserMessage(),
                )
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionError = null, notice = null)
    }

    // ---- Detal ---------------------------------------------------------------

    fun openDetail(item: ArtArticleListItem) {
        val request = ++detailRequest
        _state.value = _state.value.copy(
            detail = NewsDetailState(id = item.id, loading = true),
            actionError = null,
            notice = null,
        )

        viewModelScope.launch {
            try {
                val article = NewsRepository.article(item.id)
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = NewsDetailState(id = item.id, loading = false, article = article),
                )
            } catch (failure: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = NewsDetailState(
                        id = item.id,
                        loading = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun retryDetail() {
        val id = _state.value.detail.id ?: return
        detailRequest++
        val request = detailRequest
        _state.value = _state.value.copy(
            detail = _state.value.detail.copy(loading = true, error = null),
        )
        viewModelScope.launch {
            try {
                val article = NewsRepository.article(id)
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(loading = false, article = article),
                )
            } catch (failure: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        loading = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun closeDetail(refresh: Boolean = true) {
        detailRequest++
        _state.value = _state.value.copy(detail = NewsDetailState())
        if (refresh && _state.value.loaded) loadList(silent = true)
    }

    // ---- Əməliyyatlar --------------------------------------------------------

    fun publishArticle(id: String) {
        if (_state.value.actionBusyId != null) return
        _state.value = _state.value.copy(actionBusyId = id, actionError = null, notice = null)

        viewModelScope.launch {
            try {
                NewsRepository.publish(id)
                val detail = _state.value.detail
                val updatedDetail = if (detail.article?.id == id) {
                    detail.copy(
                        article = detail.article.copy(
                            status = ArticleStatus.PUBLISHED,
                            publishedAt = detail.article.publishedAt ?: OffsetDateTime.now().toString(),
                        ),
                    )
                } else {
                    detail
                }
                _state.value = _state.value.copy(
                    actionBusyId = null,
                    notice = "Xəbər yayımlandı.",
                    detail = updatedDetail,
                    page = if (_state.value.status == ArticleStatus.DRAFT) 0 else _state.value.page,
                )
                loadList(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    actionBusyId = null,
                    actionError = failure.toUserMessage(),
                )
            }
        }
    }

    fun deleteArticle(id: String) {
        if (_state.value.actionBusyId != null) return
        _state.value = _state.value.copy(
            actionBusyId = id,
            actionError = null,
            notice = null,
        )

        viewModelScope.launch {
            try {
                NewsRepository.delete(id)
                val current = _state.value
                val nextPage = if (current.items.size <= 1 && current.page > 0) {
                    current.page - 1
                } else {
                    current.page
                }
                _state.value = _state.value.copy(
                    actionBusyId = null,
                    page = nextPage,
                    notice = "Xəbər silindi.",
                    detail = if (current.detail.article?.id == id) {
                        NewsDetailState()
                    } else {
                        current.detail
                    },
                )
                loadList(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    actionBusyId = null,
                    actionError = failure.toUserMessage(),
                )
            }
        }
    }

    // ---- Redaktor ------------------------------------------------------------

    fun openNewEditor() {
        editorRequest++
        _state.value = _state.value.copy(
            editor = NewsEditorState(),
            actionError = null,
            notice = null,
        )
    }

    fun openEditor(item: ArtArticleListItem) {
        val request = ++editorRequest
        _state.value = _state.value.copy(
            editor = NewsEditorState(articleId = item.id, loading = true),
            actionError = null,
            notice = null,
        )

        viewModelScope.launch {
            try {
                val article = NewsRepository.article(item.id)
                if (request != editorRequest) return@launch
                _state.value = _state.value.copy(
                    editor = NewsEditorState(
                        articleId = article.id,
                        article = article,
                        files = article.fileAttachments.orEmpty(),
                    ),
                )
            } catch (failure: Throwable) {
                if (request != editorRequest) return@launch
                _state.value = _state.value.copy(
                    editor = NewsEditorState(
                        articleId = item.id,
                        loading = false,
                        loadError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun openEditor(article: ArtArticle) {
        editorRequest++
        _state.value = _state.value.copy(
            editor = NewsEditorState(
                articleId = article.id,
                article = article,
                files = article.fileAttachments.orEmpty(),
            ),
            actionError = null,
            notice = null,
        )
    }

    fun retryEditor() {
        val editor = _state.value.editor ?: return
        val id = editor.articleId ?: return
        val request = ++editorRequest
        _state.value = _state.value.copy(
            editor = editor.copy(loading = true, loadError = null),
        )
        viewModelScope.launch {
            try {
                val article = NewsRepository.article(id)
                if (request != editorRequest) return@launch
                _state.value = _state.value.copy(
                    editor = _state.value.editor?.copy(
                        loading = false,
                        article = article,
                        files = article.fileAttachments.orEmpty(),
                    ),
                )
            } catch (failure: Throwable) {
                if (request != editorRequest) return@launch
                _state.value = _state.value.copy(
                    editor = _state.value.editor?.copy(
                        loading = false,
                        loadError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun closeEditor() {
        if (_state.value.editor?.saving == true) return
        editorRequest++
        _state.value = _state.value.copy(editor = null)
        if (_state.value.loaded) loadList(silent = true)
    }

    fun clearEditorError() {
        val editor = _state.value.editor ?: return
        _state.value = _state.value.copy(editor = editor.copy(error = null))
    }

    fun saveArticle(
        articleId: String?,
        title: String,
        summary: String,
        content: String,
        existingFileIds: List<String>,
        pendingFiles: List<NewsPendingFile>,
        publish: Boolean,
    ) {
        val editor = _state.value.editor ?: return
        if (editor.saving) return

        _state.value = _state.value.copy(
            editor = editor.copy(saving = true, error = null),
        )

        viewModelScope.launch {
            var id = articleId ?: editor.createdId
            try {
                if (id == null) {
                    id = NewsRepository.create(
                        title = title,
                        summary = summary,
                        content = content,
                        fileAttachmentIds = existingFileIds,
                    )
                    _state.value = _state.value.copy(
                        editor = _state.value.editor?.copy(createdId = id),
                    )
                } else {
                    NewsRepository.update(
                        id = id,
                        title = title,
                        summary = summary,
                        content = content,
                        fileAttachmentIds = existingFileIds,
                    )
                }

                val targetId = id ?: throw ApiException("Xəbər saxlanıla bilmədi.")

                for (pending in pendingFiles) {
                    val uploaded = try {
                        NewsRepository.uploadFile(
                            articleId = targetId,
                            fileName = pending.fileName,
                            mimeType = pending.mimeType,
                            file = pending.file,
                        )
                    } catch (failure: Throwable) {
                        throw ApiException(
                            "«${pending.fileName}» yüklənmədi: ${failure.toUserMessage()}",
                        )
                    }

                    val current = _state.value.editor
                    if (current != null) {
                        _state.value = _state.value.copy(
                            editor = current.copy(
                                files = (current.files + uploaded).distinctBy { it.id },
                                uploadedClientIds = current.uploadedClientIds + pending.clientId,
                            ),
                        )
                    }
                }

                if (publish) {
                    NewsRepository.publish(targetId)
                }

                val current = _state.value.editor ?: return@launch
                _state.value = _state.value.copy(
                    editor = current.copy(saving = false, done = true, error = null),
                    notice = if (publish) "Xəbər yayımlandı." else "Xəbər saxlanıldı.",
                    actionError = null,
                )
            } catch (failure: Throwable) {
                val current = _state.value.editor ?: return@launch
                _state.value = _state.value.copy(
                    editor = current.copy(saving = false, error = failure.toUserMessage()),
                )
            }
        }
    }

    fun deleteEditorFile(file: ArtArticleAttachment) {
        val editor = _state.value.editor ?: return
        if (editor.saving || editor.fileBusyId != null) return

        _state.value = _state.value.copy(
            editor = editor.copy(fileBusyId = file.id, error = null),
        )

        viewModelScope.launch {
            try {
                NewsRepository.deleteFile(file.id)
                val current = _state.value.editor ?: return@launch
                _state.value = _state.value.copy(
                    editor = current.copy(
                        files = current.files.filterNot { it.id == file.id },
                        fileBusyId = null,
                    ),
                )
            } catch (failure: Throwable) {
                val current = _state.value.editor ?: return@launch
                _state.value = _state.value.copy(
                    editor = current.copy(
                        fileBusyId = null,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }
}
