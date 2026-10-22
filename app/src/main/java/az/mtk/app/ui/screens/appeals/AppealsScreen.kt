package az.mtk.app.ui.screens.appeals

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
import az.mtk.app.data.api.AppealCategory
import az.mtk.app.data.api.AppealLabels
import az.mtk.app.data.api.AppealListItemDto
import az.mtk.app.data.api.AppealPriority
import az.mtk.app.data.api.AppealStatus
import az.mtk.app.data.api.AppealsApi
import az.mtk.app.data.api.AppealsSearchPage
import az.mtk.app.data.api.CreateAppealRequestDto
import az.mtk.app.data.api.Page
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.data.api.SearchBody
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Status → Pill tone (TS appeals.css çiplərinin mənası). */
fun appealStatusTone(status: Int?): Tone = when (status) {
    AppealStatus.SUBMITTED -> Tone.Info
    AppealStatus.IN_PROGRESS -> Tone.Warning
    AppealStatus.RESOLVED -> Tone.Success
    AppealStatus.REJECTED -> Tone.Danger
    else -> Tone.Neutral // Closed
}

fun appealPriorityTone(priority: Int?): Tone = when (priority) {
    AppealPriority.URGENT -> Tone.Danger
    AppealPriority.HIGH -> Tone.Warning
    AppealPriority.NORMAL -> Tone.Neutral
    else -> Tone.Neutral
}

class AppealsViewModel : ViewModel() {
    val status = MutableStateFlow<Int?>(null)
    val category = MutableStateFlow<Int?>(null)
    val priority = MutableStateFlow<Int?>(null)

    val pager = Pager<AppealListItemDto>(viewModelScope) { page, query ->
        val filters = buildList {
            status.value?.let { add(az.mtk.app.data.api.QueryFilter("Status", az.mtk.app.data.api.Comparison.EQUALS, it)) }
            category.value?.let { add(az.mtk.app.data.api.QueryFilter("Category", az.mtk.app.data.api.Comparison.EQUALS, it)) }
            priority.value?.let { add(az.mtk.app.data.api.QueryFilter("Priority", az.mtk.app.data.api.Comparison.EQUALS, it)) }
        }
        val env = AppGraph.service(AppealsApi::class.java).searchAppeals(
            SearchBody(
                filters = filters.ifEmpty { null },
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = query.trim().takeIf { it.isNotEmpty() },
                page = page,
                pageSize = 15,
            ),
        )
        val payload = env.data ?: throw az.mtk.app.data.api.ApiException(
            env.message?.takeIf { it.isNotBlank() } ?: "Müraciətlər üçün boş cavab gəldi",
        )
        Page(
            payload.appeals.orEmpty(),
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: 15,
        )
    }

    val statusOptions: List<MtkOption> =
        listOf(MtkOption("", "Hamısı")) +
            AppealStatus.ALL.map { MtkOption(it.toString(), AppealLabels.status(it)) }

    val categoryOptions: List<MtkOption> =
        listOf(MtkOption("", "Bütün kateqoriyalar")) +
            AppealCategory.ALL.map { MtkOption(it.toString(), AppealLabels.category(it)) }

    val priorityOptions: List<MtkOption> =
        listOf(MtkOption("", "Bütün prioritetlər")) +
            AppealPriority.ALL.map { MtkOption(it.toString(), AppealLabels.priority(it)) }

    fun selectStatus(value: String) {
        status.value = value.toIntOrNull()
        pager.refresh()
    }

    fun selectCategory(value: String) {
        category.value = value.toIntOrNull()
        pager.refresh()
    }

    fun selectPriority(value: String) {
        priority.value = value.toIntOrNull()
        pager.refresh()
    }

    val hasFilters: Boolean
        get() = status.value != null || category.value != null || priority.value != null

    fun clearFilters() {
        status.value = null
        category.value = null
        priority.value = null
        pager.refresh()
    }

    // ---- Create modal ----

    data class CreateForm(
        val title: String = "",
        val description: String = "",
        val category: Int = AppealCategory.MAINTENANCE,
        val priority: Int = AppealPriority.NORMAL,
    )

    val createForm = MutableStateFlow(CreateForm())
    val creating = MutableStateFlow(false)
    val createError = MutableStateFlow<String?>(null)

    fun updateTitle(value: String) { createForm.value = createForm.value.copy(title = value.take(200)) }
    fun updateDescription(value: String) { createForm.value = createForm.value.copy(description = value.take(2000)) }
    fun updateCategory(value: String) {
        value.toIntOrNull()?.let { createForm.value = createForm.value.copy(category = it) }
    }
    fun updatePriority(value: String) {
        value.toIntOrNull()?.let { createForm.value = createForm.value.copy(priority = it) }
    }

    fun submitCreate(onCreated: (String) -> Unit) {
        val form = createForm.value
        if (form.title.isBlank() || form.description.isBlank() || creating.value) return
        viewModelScope.launch {
            creating.value = true
            createError.value = null
            runCatching {
                AppGraph.service(AppealsApi::class.java).createAppeal(
                    CreateAppealRequestDto(
                        title = form.title.trim(),
                        description = form.description.trim(),
                        category = form.category,
                    ),
                )
            }
                .onSuccess { id ->
                    creating.value = false
                    createForm.value = CreateForm()
                    onCreated(id)
                }
                .onFailure { failure ->
                    creating.value = false
                    createError.value = failure.toUserMessage()
                }
        }
    }
}

@Composable
fun AppealsScreen(
    onBack: (() -> Unit)?,
    onOpenAppeal: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: AppealsViewModel = mtkViewModel { AppealsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.status.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.category.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.priority.collectAsStateWithLifecycle()
    val createForm by viewModel.createForm.collectAsStateWithLifecycle()
    val creating by viewModel.creating.collectAsStateWithLifecycle()
    val createError by viewModel.createError.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Müraciətlər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} müraciət",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { viewModel.selectStatus("") },
                    label = { Text("Hamısı", fontSize = 12.sp) },
                    colors = appealChipColors(),
                )
                AppealStatus.ALL.forEach { s ->
                    FilterChip(
                        selected = selectedStatus == s,
                        onClick = { viewModel.selectStatus(s.toString()) },
                        label = { Text(AppealLabels.status(s), fontSize = 12.sp) },
                        colors = appealChipColors(),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MtkSelect(
                    value = selectedCategory?.toString() ?: "",
                    onValueChange = viewModel::selectCategory,
                    label = "Kateqoriya",
                    options = viewModel.categoryOptions,
                    modifier = Modifier.weight(1f),
                )
                MtkSelect(
                    value = selectedPriority?.toString() ?: "",
                    onValueChange = viewModel::selectPriority,
                    label = "Prioritet",
                    options = viewModel.priorityOptions,
                    modifier = Modifier.weight(1f),
                )
            }
            if (viewModel.hasFilters) {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = "Süzgəcləri təmizlə", onClick = viewModel::clearFilters)
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni müraciət", onClick = { showCreate = true })
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Müraciətlər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState(
                if (viewModel.hasFilters) "Bu süzgəclərə uyğun müraciət yoxdur" else "Hələ müraciət yoxdur",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { appeal ->
                    MtkCard(onClick = { onOpenAppeal(appeal.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    appeal.title ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    AppealLabels.category(appeal.category),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                            Pill(AppealLabels.status(appeal.status), appealStatusTone(appeal.status))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                appeal.submitterName ?: "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = MtkColors.MutedSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Pill(
                                    AppealLabels.priority(appeal.priority),
                                    appealPriorityTone(appeal.priority),
                                    showDot = false,
                                )
                                Text(
                                    "  ${appeal.commentsCount ?: 0} şərh · ${Format.date(appeal.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
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
    }

    if (showCreate) {
        MtkModal(
            title = "Yeni müraciət",
            subtitle = "Problem, təklif və suallarınızı buradan göndərin",
            onDismiss = { showCreate = false },
            confirmLabel = "Göndər",
            confirmEnabled = createForm.title.isNotBlank() && createForm.description.isNotBlank(),
            confirmLoading = creating,
            onConfirm = {
                viewModel.submitCreate { id ->
                    showCreate = false
                    onOpenAppeal(id)
                }
            },
        ) {
            MtkSelect(
                value = createForm.category.toString(),
                onValueChange = viewModel::updateCategory,
                label = "Kateqoriya",
                required = true,
                options = AppealCategory.ALL.map { MtkOption(it.toString(), AppealLabels.category(it)) },
            )
            Spacer(Modifier.height(12.dp))
            MtkTextField(
                value = createForm.title,
                onValueChange = viewModel::updateTitle,
                label = "Başlıq",
                required = true,
                placeholder = "Məs: Liftin işləməməsi",
            )
            Spacer(Modifier.height(12.dp))
            MtkTextField(
                value = createForm.description,
                onValueChange = viewModel::updateDescription,
                label = "Təsvir",
                required = true,
                placeholder = "Problemi ətraflı izah edin: harada, nə vaxtdan, nə baş verir…",
                singleLine = false,
                minLines = 5,
            )
            Spacer(Modifier.height(12.dp))
            MtkSelect(
                value = createForm.priority.toString(),
                onValueChange = viewModel::updatePriority,
                label = "Prioritet",
                options = AppealPriority.ALL.map { MtkOption(it.toString(), AppealLabels.priority(it)) },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${createForm.description.length}/2000",
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
            )
            createError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
            }
        }
    }
}

@Composable
private fun appealChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MtkColors.Green,
    selectedLabelColor = Color.White,
)
