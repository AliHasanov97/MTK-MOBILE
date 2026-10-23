package az.mtk.app.ui.screens.appeals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.AppealDetailDto
import az.mtk.app.data.api.AppealLabels
import az.mtk.app.data.api.AppealListItemDto
import az.mtk.app.data.api.AppealPropertyOption
import az.mtk.app.data.api.AppealsRepository
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppealListState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val status: Int? = null,
    val category: Int? = null,
    val priority: Int? = null,
    val search: String = "",
    val items: List<AppealListItemDto> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val pageSize: Int = 15,
) {
    val activeFilterCount: Int
        get() = (if (status != null) 1 else 0) +
            (if (category != null) 1 else 0) +
            (if (priority != null) 1 else 0)

    val pageCount: Int
        get() = maxOf(1, (totalCount + pageSize - 1) / pageSize)
}

data class AppealDetailState(
    val id: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val appeal: AppealDetailDto? = null,
    val commentDraft: String = "",
    val sendingComment: Boolean = false,
    val updatingStatus: Boolean = false,
    val notice: String? = null,
    val actionError: String? = null,
) {
    val closed: Boolean get() = AppealLabels.isClosed(appeal?.status)
}

data class AppealCreateState(
    val propertiesLoading: Boolean = false,
    val propertiesLoaded: Boolean = false,
    val properties: List<AppealPropertyOption> = emptyList(),
    val propertiesError: String? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
)

data class AppealsUiState(
    val list: AppealListState = AppealListState(),
    val detail: AppealDetailState = AppealDetailState(),
    val create: AppealCreateState = AppealCreateState(),
)

/**
 * Müraciətlər modulunun vəziyyəti.
 *
 * Siyahı serverdə səhifələnir (15 qeyd), filtr və axtarış da serverə göndərilir.
 * Detal hər açılışda yenidən çəkilir — köhnə müraciətin məlumatı ekranda qalmır.
 */
class AppealsViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppealsUiState())
    val state = _state.asStateFlow()

    private var listRequest = 0
    private var detailRequest = 0
    private var searchJob: Job? = null

    // ---- Siyahı --------------------------------------------------------------

    fun ensureLoaded() {
        val list = _state.value.list
        if (!list.loaded && !list.loading) loadList()
    }

    fun refresh() {
        loadList()
    }

    fun setStatus(status: Int?) {
        if (_state.value.list.status == status) return
        _state.value = _state.value.copy(
            list = _state.value.list.copy(status = status, page = 0),
        )
        loadList()
    }

    fun setCategory(category: Int?) {
        if (_state.value.list.category == category) return
        _state.value = _state.value.copy(
            list = _state.value.list.copy(category = category, page = 0),
        )
        loadList()
    }

    fun setPriority(priority: Int?) {
        if (_state.value.list.priority == priority) return
        _state.value = _state.value.copy(
            list = _state.value.list.copy(priority = priority, page = 0),
        )
        loadList()
    }

    fun setSearch(value: String) {
        _state.value = _state.value.copy(
            list = _state.value.list.copy(search = value, page = 0),
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadList()
        }
    }

    fun clearListError() {
        _state.value = _state.value.copy(
            list = _state.value.list.copy(error = null),
        )
    }

    fun clearFilters() {
        searchJob?.cancel()
        _state.value = _state.value.copy(
            list = _state.value.list.copy(
                status = null,
                category = null,
                priority = null,
                search = "",
                page = 0,
            ),
        )
        loadList()
    }

    fun nextPage() {
        val list = _state.value.list
        if (list.loading || list.page + 1 >= list.pageCount) return
        _state.value = _state.value.copy(list = list.copy(page = list.page + 1))
        loadList()
    }

    fun prevPage() {
        val list = _state.value.list
        if (list.loading || list.page <= 0) return
        _state.value = _state.value.copy(list = list.copy(page = list.page - 1))
        loadList()
    }

    private fun loadList(silent: Boolean = false) {
        val request = ++listRequest
        val snapshot = _state.value.list
        _state.value = _state.value.copy(
            list = snapshot.copy(
                loading = !silent || snapshot.items.isEmpty(),
                error = null,
            ),
        )

        viewModelScope.launch {
            try {
                val page = AppealsRepository.search(
                    status = snapshot.status,
                    category = snapshot.category,
                    priority = snapshot.priority,
                    searchTerm = snapshot.search,
                    page = snapshot.page + 1,
                    pageSize = snapshot.pageSize,
                )
                if (request != listRequest) return@launch

                _state.value = _state.value.copy(
                    list = _state.value.list.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        items = page.appeals.orEmpty(),
                        totalCount = page.totalCount ?: 0,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != listRequest) return@launch
                _state.value = _state.value.copy(
                    list = _state.value.list.copy(
                        loading = false,
                        loaded = true,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    // ---- Detal ---------------------------------------------------------------

    fun openDetail(id: String) {
        if (_state.value.detail.id == id && _state.value.detail.appeal != null) return
        loadDetail(id)
    }

    fun refreshDetail() {
        val id = _state.value.detail.id ?: return
        loadDetail(id, keepContent = _state.value.detail.appeal != null)
    }

    fun clearDetail() {
        detailRequest++
        _state.value = _state.value.copy(detail = AppealDetailState())
    }

    private fun loadDetail(id: String, keepContent: Boolean = false) {
        val request = ++detailRequest
        val previous = _state.value.detail
        _state.value = _state.value.copy(
            detail = previous.copy(
                id = id,
                loading = !keepContent,
                error = null,
                appeal = if (keepContent) previous.appeal else null,
                commentDraft = if (previous.id == id) previous.commentDraft else "",
                notice = null,
                actionError = null,
            ),
        )

        viewModelScope.launch {
            try {
                val appeal = AppealsRepository.detail(id)
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        id = id,
                        loading = false,
                        error = null,
                        appeal = appeal,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != detailRequest) return@launch
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        id = id,
                        loading = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun setCommentDraft(value: String) {
        _state.value = _state.value.copy(
            detail = _state.value.detail.copy(commentDraft = value.take(1000)),
        )
    }

    fun clearDetailFeedback() {
        _state.value = _state.value.copy(
            detail = _state.value.detail.copy(notice = null, actionError = null),
        )
    }

    fun sendComment() {
        val detail = _state.value.detail
        val appeal = detail.appeal ?: return
        val text = detail.commentDraft.trim()
        if (text.isEmpty() || detail.sendingComment || detail.closed) return

        _state.value = _state.value.copy(
            detail = detail.copy(sendingComment = true, actionError = null, notice = null),
        )

        viewModelScope.launch {
            try {
                AppealsRepository.addComment(appeal.id, text)
                val fresh = AppealsRepository.detail(appeal.id)
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        appeal = fresh,
                        commentDraft = "",
                        sendingComment = false,
                        actionError = null,
                        notice = "Şərh əlavə edildi.",
                    ),
                )
                loadList(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        sendingComment = false,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun updateStatus(newStatus: Int) {
        val detail = _state.value.detail
        val appeal = detail.appeal ?: return
        if (detail.updatingStatus) return

        if (appeal.status == newStatus) {
            _state.value = _state.value.copy(
                detail = detail.copy(notice = "Status artıq bu vəziyyətdədir.", actionError = null),
            )
            return
        }

        _state.value = _state.value.copy(
            detail = detail.copy(updatingStatus = true, actionError = null, notice = null),
        )

        viewModelScope.launch {
            try {
                AppealsRepository.updateStatus(appeal.id, newStatus)
                val fresh = AppealsRepository.detail(appeal.id)
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        appeal = fresh,
                        updatingStatus = false,
                        actionError = null,
                        notice = "Status yeniləndi: ${AppealLabels.status(newStatus)}.",
                    ),
                )
                loadList(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    detail = _state.value.detail.copy(
                        updatingStatus = false,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    // ---- Yeni müraciət -------------------------------------------------------

    fun ensureCreateOptions() {
        val create = _state.value.create
        if (create.propertiesLoaded || create.propertiesLoading) return

        _state.value = _state.value.copy(
            create = create.copy(propertiesLoading = true, propertiesError = null),
        )

        viewModelScope.launch {
            try {
                val properties = AppealsRepository.myProperties()
                _state.value = _state.value.copy(
                    create = _state.value.create.copy(
                        propertiesLoading = false,
                        propertiesLoaded = true,
                        properties = properties,
                        propertiesError = null,
                    ),
                )
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    create = _state.value.create.copy(
                        propertiesLoading = false,
                        propertiesLoaded = false,
                        propertiesError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun createAppeal(
        title: String,
        description: String,
        category: Int,
        property: AppealPropertyOption?,
    ) {
        val create = _state.value.create
        if (create.saving) return

        _state.value = _state.value.copy(
            create = create.copy(saving = true, error = null, createdId = null),
        )

        viewModelScope.launch {
            try {
                val id = AppealsRepository.create(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    apartmentId = property?.takeIf { !it.isGarage }?.id,
                    garageId = property?.takeIf { it.isGarage }?.id,
                )
                _state.value = _state.value.copy(
                    create = _state.value.create.copy(
                        saving = false,
                        error = null,
                        createdId = id,
                    ),
                )
                loadList(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    create = _state.value.create.copy(
                        saving = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun consumeCreated() {
        _state.value = _state.value.copy(
            create = _state.value.create.copy(createdId = null),
        )
    }

    fun dismissCreateError() {
        _state.value = _state.value.copy(
            create = _state.value.create.copy(error = null, propertiesError = null),
        )
    }
}
