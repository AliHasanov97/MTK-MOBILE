package az.mtk.app.ui.components

import az.mtk.app.data.api.Page
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val pageSize: Int = 20,
)

/**
 * Debounced, server-side paginated list loader shared by the search screens.
 * Keeps the same zero-based paging contract MTK-UI uses.
 */
class Pager<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = 20,
    private val loader: suspend (page: Int, query: String) -> Page<T>,
) {
    private val _state = MutableStateFlow(PagedState<T>(pageSize = pageSize))
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var loadedPage = 0

    init {
        refresh()
    }

    fun refresh() {
        searchJob?.cancel()
        scope.launch { load(0, replace = true) }
    }

    fun loadMore() {
        if (_state.value.loading || _state.value.loadingMore) return
        if (_state.value.items.size >= _state.value.total) return
        scope.launch { load(loadedPage + 1, replace = false) }
    }

    fun onQueryChange(query: String) {
        if (query == _state.value.query) return
        _state.value = _state.value.copy(query = query, loading = true)
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(350)
            load(0, replace = true)
        }
    }

    private suspend fun load(page: Int, replace: Boolean) {
        val snapshot = _state.value.query
        _state.value = if (replace) {
            _state.value.copy(loading = true, error = null)
        } else {
            _state.value.copy(loadingMore = true, error = null)
        }

        runCatching { loader(page, snapshot) }
            .onSuccess { result ->
                if (_state.value.query != snapshot) return@onSuccess
                loadedPage = page
                _state.value = _state.value.copy(
                    items = if (replace) result.items else _state.value.items + result.items,
                    total = result.totalCount,
                    loading = false,
                    loadingMore = false,
                    error = null,
                )
            }
            .onFailure { failure ->
                if (_state.value.query != snapshot) return@onFailure
                _state.value = _state.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = failure.toUserMessage(),
                )
            }
    }
}
