package az.mtk.app.ui.screens.appeals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.AppealCategory
import az.mtk.app.data.api.AppealCommentDto
import az.mtk.app.data.api.AppealDetailDto
import az.mtk.app.data.api.AppealLabels
import az.mtk.app.data.api.AppealListItemDto
import az.mtk.app.data.api.AppealPriority
import az.mtk.app.data.api.AppealPropertyOption
import az.mtk.app.data.api.AppealStatus
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsFormActions
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPagination
import az.mtk.app.ui.screens.operations.OpsPill
import az.mtk.app.ui.screens.operations.OpsPillRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTextField
import az.mtk.app.ui.screens.operations.OpsTopBar
import az.mtk.app.util.Format

/**
 * Müraciətlər modulu — saytdakı `muracietler` səhifəsinin mobil qarşılığı.
 * Cədvəl yoxdur: status çipləri, kartlar və aşağıdan açılan panellər.
 *
 * Siyahıdan müraciətə keçid modul daxilində saxlanılır ki, geri qayıdanda
 * filtr/axtarış vəziyyəti itməsin. Detal hər açılışda yenidən çəkilir.
 */
@Composable
fun AppealsScreen(
    onBack: () -> Unit,
    isStaff: Boolean,
    startWithCreate: Boolean = false,
    onStartCreateConsumed: () -> Unit = {},
    viewModel: AppealsViewModel = mtkViewModel { AppealsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.ensureLoaded()
        if (startWithCreate) {
            showCreate = true
            onStartCreateConsumed()
        }
    }

    LaunchedEffect(selectedId) {
        val id = selectedId
        if (id != null) viewModel.openDetail(id) else viewModel.clearDetail()
    }

    LaunchedEffect(state.create.createdId) {
        val createdId = state.create.createdId
        if (createdId != null) {
            viewModel.consumeCreated()
            showCreate = false
            selectedId = createdId
        }
    }

    // Detal açıqdırsa sistem "geri" düyməsi əvvəlcə siyahıya qaytarır.
    BackHandler(enabled = selectedId != null) {
        selectedId = null
    }

    val currentId = selectedId
    if (currentId != null) {
        AppealDetailContent(
            state = state.detail,
            isStaff = isStaff,
            onBack = { selectedId = null },
            onRefresh = viewModel::refreshDetail,
            onCommentChange = viewModel::setCommentDraft,
            onSendComment = viewModel::sendComment,
            onUpdateStatus = viewModel::updateStatus,
            onClearFeedback = viewModel::clearDetailFeedback,
        )
    } else {
        AppealsListContent(
            state = state.list,
            isStaff = isStaff,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onStatus = viewModel::setStatus,
            onCategory = viewModel::setCategory,
            onPriority = viewModel::setPriority,
            onSearch = viewModel::setSearch,
            onClearFilters = viewModel::clearFilters,
            onPrevPage = viewModel::prevPage,
            onNextPage = viewModel::nextPage,
            onDismissError = viewModel::clearListError,
            onCreate = { showCreate = true },
            onOpenDetail = { selectedId = it },
        )

        if (showCreate) {
            CreateAppealSheet(
                state = state.create,
                onDismiss = { showCreate = false },
                onEnsureOptions = viewModel::ensureCreateOptions,
                onClearError = viewModel::dismissCreateError,
                onSubmit = { title, description, category, property ->
                    viewModel.createAppeal(title, description, category, property)
                },
            )
        }
    }
}

// ---- Siyahı ------------------------------------------------------------------

@Composable
private fun AppealsListContent(
    state: AppealListState,
    isStaff: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStatus: (Int?) -> Unit,
    onCategory: (Int?) -> Unit,
    onPriority: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onClearFilters: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissError: () -> Unit,
    onCreate: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    var showCategories by remember { mutableStateOf(false) }
    var showPriorities by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = if (isStaff) "Müraciətlər" else "Mənim müraciətlərim",
                subtitle = if (isStaff) {
                    "Sakin sorğuları · icra vəziyyəti"
                } else {
                    "Müraciətlərinizi göndərin və izləyin"
                },
                onBack = onBack,
                onRefresh = onRefresh,
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(Icons.Default.Add, "Yeni müraciət", tint = OpsColors.Terracotta)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { AppealsSummaryCard(state, isStaff) }

            item {
                OpsPrimaryButton(
                    text = "+ Yeni müraciət",
                    onClick = onCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }

            item {
                OpsSearchField(
                    value = state.search,
                    onValueChange = onSearch,
                    placeholder = "Başlıq və ya təsvir üzrə axtar…",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                OpsPillRow(
                    pills = buildList {
                        add(
                            OpsPill(
                                label = "Hamısı",
                                selected = state.status == null,
                                onClick = { onStatus(null) },
                            ),
                        )
                        AppealStatus.ALL.forEach { status ->
                            add(
                                OpsPill(
                                    label = AppealLabels.status(status),
                                    selected = state.status == status,
                                    onClick = { onStatus(status) },
                                ),
                            )
                        }
                    },
                )
            }

            item {
                val hasFilters = state.activeFilterCount > 0 || state.search.isNotBlank()
                OpsPillRow(
                    pills = buildList {
                        add(
                            OpsPill(
                                label = state.category?.let { AppealLabels.category(it) } ?: "Kateqoriya",
                                selected = state.category != null,
                                onClick = { showCategories = true },
                            ),
                        )
                        add(
                            OpsPill(
                                label = state.priority?.let { AppealLabels.priority(it) } ?: "Prioritet",
                                selected = state.priority != null,
                                onClick = { showPriorities = true },
                            ),
                        )
                        if (hasFilters) {
                            add(
                                OpsPill(
                                    label = "Təmizlə",
                                    selected = false,
                                    onClick = onClearFilters,
                                ),
                            )
                        }
                    },
                )
            }

            if (state.error != null && state.items.isNotEmpty()) {
                item {
                    OpsFeedback(
                        message = state.error,
                        isError = true,
                        onDismiss = onDismissError,
                    )
                }
            }

            when {
                state.loading && state.items.isEmpty() -> item { OpsLoading() }

                state.error != null && state.items.isEmpty() -> item {
                    OpsError(state.error, onRetry = onRefresh)
                }

                state.items.isEmpty() -> {
                    item {
                        OpsEmpty(
                            title = if (state.activeFilterCount > 0 || state.search.isNotBlank()) {
                                "Uyğun müraciət tapılmadı"
                            } else {
                                "Hələ müraciət yoxdur"
                            },
                            text = if (state.activeFilterCount > 0 || state.search.isNotBlank()) {
                                "Süzgəcləri dəyişin və ya təmizləyin."
                            } else if (isStaff) {
                                "Sakinlər müraciət göndərəndə burada görünəcək."
                            } else {
                                "Problem, təklif və ya sualınızı göndərin."
                            },
                        )
                    }
                    if (!isStaff) {
                        item {
                            OpsPrimaryButton(
                                text = "İlk müraciəti göndər",
                                onClick = onCreate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            )
                        }
                    }
                }

                else -> items(state.items, key = { it.id }) { item ->
                    AppealCard(
                        item = item,
                        isStaff = isStaff,
                        onClick = { onOpenDetail(item.id) },
                    )
                }
            }

            if (state.totalCount > state.pageSize) {
                item {
                    OpsPagination(
                        page = state.page,
                        total = state.totalCount,
                        pageSize = state.pageSize,
                        onPrev = onPrevPage,
                        onNext = onNextPage,
                    )
                }
            }
        }
    }

    if (showCategories) {
        AppealFilterSheet(
            title = "Kateqoriya seç",
            options = listOf<Pair<Int?, String>>(null to "Bütün kateqoriyalar") +
                AppealCategory.ALL.map { it to AppealLabels.category(it) },
            selected = state.category,
            onSelect = onCategory,
            onDismiss = { showCategories = false },
        )
    }

    if (showPriorities) {
        AppealFilterSheet(
            title = "Prioritet seç",
            options = listOf<Pair<Int?, String>>(null to "Bütün prioritetlər") +
                AppealPriority.ALL.map { it to AppealLabels.priority(it) },
            selected = state.priority,
            onSelect = onPriority,
            onDismiss = { showPriorities = false },
        )
    }
}

@Composable
private fun AppealsSummaryCard(state: AppealListState, isStaff: Boolean) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isStaff) "Müraciət jurnalı" else "Müraciətlərim",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Cəmi ${state.totalCount} müraciət",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = "Səhifə ${state.page + 1}/${state.pageCount}",
                foreground = OpsColors.Terracotta,
                background = OpsColors.Container,
            )
        }

        if (state.activeFilterCount > 0 || state.search.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.activeFilterCount > 0) {
                    OpsChip(
                        text = "${state.activeFilterCount} filtr",
                        foreground = OpsColors.Blue,
                        background = OpsColors.BlueSoft,
                    )
                }
                if (state.search.isNotBlank()) {
                    OpsChip(
                        text = "Axtarış",
                        foreground = OpsColors.Blue,
                        background = OpsColors.BlueSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppealCard(
    item: AppealListItemDto,
    isStaff: Boolean,
    onClick: () -> Unit,
) {
    OpsCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title?.takeIf { it.isNotBlank() } ?: "Müraciət",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppealStatusChip(item.status)
                    AppealPriorityChip(item.priority)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "${AppealLabels.category(item.category)} · ${Format.date(item.createdAt)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                if (isStaff) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Müraciətçi: ${item.submitterName ?: "—"}",
                        fontSize = 11.sp,
                        color = OpsColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                OpsChip(
                    text = "${item.commentsCount ?: 0} şərh",
                    foreground = OpsColors.Muted,
                    background = OpsColors.Subtle,
                )
                Spacer(Modifier.height(10.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OpsColors.MutedSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---- Detal -------------------------------------------------------------------

@Composable
private fun AppealDetailContent(
    state: AppealDetailState,
    isStaff: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCommentChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onUpdateStatus: (Int) -> Unit,
    onClearFeedback: () -> Unit,
) {
    val appeal = state.appeal
    var showStatusSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = "Müraciət",
                subtitle = appeal?.let {
                    "${AppealLabels.status(it.status)} · ${AppealLabels.category(it.category)}"
                },
                onBack = onBack,
                onRefresh = onRefresh,
            )
        },
    ) { padding ->
        when {
            state.loading && appeal == null -> OpsLoading()

            state.error != null && appeal == null -> OpsError(state.error, onRetry = onRefresh)

            appeal == null -> OpsEmpty(
                title = "Müraciət tapılmadı",
                text = "Müraciət silinib və ya ona giriş icazəniz yoxdur.",
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { AppealHeaderCard(appeal) }

                    if (state.notice != null) {
                        item {
                            OpsFeedback(
                                message = state.notice,
                                isError = false,
                                onDismiss = onClearFeedback,
                            )
                        }
                    }

                    if (state.actionError != null) {
                        item {
                            OpsFeedback(
                                message = state.actionError,
                                isError = true,
                                onDismiss = onClearFeedback,
                            )
                        }
                    }

                    item { AppealDescriptionCard(appeal.description) }
                    item { AppealInfoCard(appeal) }

                    item {
                        AppealConversationCard(
                            appeal = appeal,
                            isStaff = isStaff,
                            draft = state.commentDraft,
                            sending = state.sendingComment,
                            closed = state.closed,
                            onCommentChange = onCommentChange,
                            onSend = onSendComment,
                        )
                    }

                    if (isStaff) {
                        item {
                            AppealManagementCard(
                                status = appeal.status,
                                updating = state.updatingStatus,
                                onChangeStatus = { showStatusSheet = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStatusSheet && isStaff) {
        AppealStatusSheet(
            current = appeal?.status,
            saving = state.updatingStatus,
            onApply = onUpdateStatus,
            onDismiss = { showStatusSheet = false },
        )
    }
}

@Composable
private fun AppealHeaderCard(appeal: AppealDetailDto) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = appeal.title?.takeIf { it.isNotBlank() } ?: "Müraciət",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppealStatusChip(appeal.status)
            AppealPriorityChip(appeal.priority)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${appeal.submitterName ?: "Naməlum"} · ${Format.dateTime(appeal.createdAt)}",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
    }
}

@Composable
private fun AppealDescriptionCard(description: String?) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Təsvir",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description?.takeIf { it.isNotBlank() } ?: "—",
            fontSize = 14.sp,
            color = OpsColors.Ink,
        )
    }
}

@Composable
private fun AppealInfoCard(appeal: AppealDetailDto) {
    val objectLabel = when {
        !appeal.apartmentLabel.isNullOrBlank() -> "Mənzil ${appeal.apartmentLabel}"
        !appeal.garageLabel.isNullOrBlank() -> "Qaraj ${appeal.garageLabel}"
        else -> null
    }

    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Məlumat",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(4.dp))
        OpsInfoRow("Kateqoriya", AppealLabels.category(appeal.category))
        OpsInfoRow("Prioritet", AppealLabels.priority(appeal.priority))
        OpsInfoRow("Müraciətçi", appeal.submitterName ?: "—")
        if (objectLabel != null) OpsInfoRow("Obyekt", objectLabel)
        if (!appeal.assignedToName.isNullOrBlank()) {
            OpsInfoRow("Məsul şəxs", appeal.assignedToName)
        }
        OpsInfoRow("Yaradılıb", Format.dateTime(appeal.createdAt))
        if (!appeal.updatedAt.isNullOrBlank()) {
            OpsInfoRow("Son yenilənmə", Format.dateTime(appeal.updatedAt))
        }
        if (!appeal.resolvedAt.isNullOrBlank()) {
            OpsInfoRow("Həll edilib", Format.dateTime(appeal.resolvedAt), OpsColors.Olive)
        }
        if (!appeal.closedAt.isNullOrBlank()) {
            OpsInfoRow("Bağlanıb", Format.dateTime(appeal.closedAt), OpsColors.Muted)
        }
    }
}

@Composable
private fun AppealConversationCard(
    appeal: AppealDetailDto,
    isStaff: Boolean,
    draft: String,
    sending: Boolean,
    closed: Boolean,
    onCommentChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val comments = appeal.comments.orEmpty()

    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Yazışma",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Muted,
            )
            OpsChip(
                text = comments.size.toString(),
                foreground = OpsColors.Muted,
                background = OpsColors.Subtle,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (comments.isEmpty()) {
            Text(
                text = "Hələ şərh yoxdur.",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.forEach { comment ->
                    val fromSubmitter = comment.userId == appeal.submitterId
                    val mine = if (isStaff) !fromSubmitter else fromSubmitter
                    AppealCommentBubble(
                        comment = comment,
                        mine = mine,
                        fromSubmitter = fromSubmitter,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(10.dp))

        if (closed) {
            Text(
                text = "Bu müraciət bağlanıb, yeni şərh əlavə etmək olmur.",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = onCommentChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Şərh yazın…", fontSize = 13.sp, color = OpsColors.MutedSoft)
                },
                minLines = 2,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OpsColors.Terracotta,
                    unfocusedBorderColor = OpsColors.Border,
                    focusedContainerColor = OpsColors.Surface,
                    unfocusedContainerColor = OpsColors.Surface,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${draft.length}/1000",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
                OpsPrimaryButton(
                    text = if (sending) "Göndərilir…" else "Göndər",
                    onClick = onSend,
                    enabled = draft.isNotBlank() && !sending,
                )
            }
        }
    }
}

@Composable
private fun AppealCommentBubble(
    comment: AppealCommentDto,
    mine: Boolean,
    fromSubmitter: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (mine) 14.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 14.dp,
            ),
            color = if (mine) OpsColors.Container else OpsColors.Subtle,
            border = BorderStroke(1.dp, if (mine) OpsColors.Border else OpsColors.Border),
        ) {
            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName ?: "—",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpsColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (fromSubmitter) "Müraciətçi" else "İdarə",
                        fontSize = 9.sp,
                        color = OpsColors.Terracotta,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = comment.text?.takeIf { it.isNotBlank() } ?: "—",
                    fontSize = 13.sp,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Format.dateTime(comment.createdAt),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (mine) TextAlign.End else TextAlign.Start,
                    fontSize = 9.sp,
                    color = OpsColors.MutedSoft,
                )
            }
        }
    }
}

@Composable
private fun AppealManagementCard(
    status: Int?,
    updating: Boolean,
    onChangeStatus: () -> Unit,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "İdarəetmə",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(4.dp))
        OpsInfoRow("Cari status", AppealLabels.status(status))
        Spacer(Modifier.height(8.dp))
        OpsPrimaryButton(
            text = if (updating) "Yenilənir…" else "Statusu dəyiş",
            onClick = onChangeStatus,
            modifier = Modifier.fillMaxWidth(),
            enabled = !updating,
        )
    }
}

// ---- Aşağıdan açılan panellər -------------------------------------------------

@Composable
private fun AppealFilterSheet(
    title: String,
    options: List<Pair<Int?, String>>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        options.forEachIndexed { index, (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(value)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (value == selected) OpsColors.Terracotta else OpsColors.Ink,
                )
                if (value == selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OpsColors.Terracotta,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (index != options.lastIndex) {
                HorizontalDivider(color = OpsColors.Border)
            }
        }
    }
}

@Composable
private fun AppealStatusSheet(
    current: Int?,
    saving: Boolean,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(current) { mutableIntStateOf(current ?: AppealStatus.SUBMITTED) }

    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = "Statusu dəyiş",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "Müraciətin icra vəziyyətini seçin.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Spacer(Modifier.height(10.dp))

        AppealStatus.ALL.forEach { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !saving) { selected = status }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppealStatusChip(status)
                Spacer(Modifier.weight(1f))
                if (status == current) {
                    Text(
                        text = "Cari",
                        fontSize = 10.sp,
                        color = OpsColors.MutedSoft,
                    )
                }
                if (status == selected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OpsColors.Terracotta,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Ləğv et",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            OpsPrimaryButton(
                text = if (saving) "Yenilənir…" else "Tətbiq et",
                onClick = {
                    onApply(selected)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = selected != current && !saving,
            )
        }
    }
}

@Composable
private fun CreateAppealSheet(
    state: AppealCreateState,
    onDismiss: () -> Unit,
    onEnsureOptions: () -> Unit,
    onClearError: () -> Unit,
    onSubmit: (String, String, Int, AppealPropertyOption?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableIntStateOf(AppealCategory.MAINTENANCE) }
    var property by remember { mutableStateOf<AppealPropertyOption?>(null) }
    var showProperties by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onEnsureOptions() }

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
        modifier = Modifier.imePadding(),
    ) {
        Text(
            text = "Yeni müraciət",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "Problem, təklif və ya sualınızı göndərin.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Kateqoriya",
            fontSize = 12.sp,
            color = OpsColors.MutedSoft,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppealCategory.ALL.forEach { value ->
                val active = category == value
                Surface(
                    modifier = Modifier.clickable {
                        category = value
                        onClearError()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) OpsColors.Terracotta else OpsColors.Subtle,
                ) {
                    Text(
                        text = AppealLabels.category(value),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else OpsColors.Ink,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OpsTextField(
            value = title,
            onValueChange = {
                if (it.length <= 200) {
                    title = it
                    onClearError()
                }
            },
            label = "Başlıq",
            placeholder = "Məs: Liftin işləməməsi",
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = {
                if (it.length <= 2000) {
                    description = it
                    onClearError()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Təsvir", fontSize = 12.sp) },
            placeholder = {
                Text(
                    "Harada, nə vaxtdan, nə baş verir…",
                    fontSize = 13.sp,
                    color = OpsColors.MutedSoft,
                )
            },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OpsColors.Terracotta,
                unfocusedBorderColor = OpsColors.Border,
                focusedContainerColor = OpsColors.Surface,
                unfocusedContainerColor = OpsColors.Surface,
            ),
        )
        Text(
            text = "${description.length}/2000",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            fontSize = 10.sp,
            color = OpsColors.MutedSoft,
        )

        if (state.propertiesLoading) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Obyekt siyahısı yüklənir…",
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
            )
        } else if (state.propertiesError != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Obyekt siyahısı yüklənmədi.",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = OpsColors.Error,
                )
                TextButton(onClick = onEnsureOptions) {
                    Text("Yenidən", fontSize = 11.sp, color = OpsColors.Terracotta)
                }
            }
        }

        if (state.properties.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Obyekt (istəyə bağlı)",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
            Spacer(Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProperties = !showProperties },
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.Surface,
                border = BorderStroke(1.dp, OpsColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = property?.label ?: "Seçilməyib",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = if (property == null) OpsColors.MutedSoft else OpsColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = if (showProperties) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = OpsColors.MutedSoft,
                    )
                }
            }

            if (showProperties) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = OpsColors.Surface,
                    border = BorderStroke(1.dp, OpsColors.Border),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        PropertyChoiceRow(
                            label = "Seçilməyib",
                            selected = property == null,
                            onClick = {
                                property = null
                                showProperties = false
                            },
                        )
                        state.properties.forEach { option ->
                            PropertyChoiceRow(
                                label = option.label,
                                selected = property?.id == option.id,
                                onClick = {
                                    property = option
                                    showProperties = false
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.ErrorSoft,
            ) {
                Text(
                    text = state.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    fontSize = 11.sp,
                    color = OpsColors.Error,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        OpsFormActions(
            saving = state.saving,
            submitLabel = "Göndər",
            onCancel = onDismiss,
            onSubmit = { onSubmit(title, description, category, property) },
            enabled = title.isNotBlank() && description.isNotBlank(),
        )
    }
}

@Composable
private fun PropertyChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) OpsColors.Terracotta else OpsColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = OpsColors.Terracotta,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---- Status / prioritet çipləri ----------------------------------------------

@Composable
private fun AppealStatusChip(status: Int?, modifier: Modifier = Modifier) {
    val foreground = when (status) {
        AppealStatus.SUBMITTED -> OpsColors.Blue
        AppealStatus.IN_PROGRESS -> OpsColors.Amber
        AppealStatus.RESOLVED -> OpsColors.Olive
        AppealStatus.CLOSED -> OpsColors.Muted
        AppealStatus.REJECTED -> OpsColors.Error
        else -> OpsColors.MutedSoft
    }
    val background = when (status) {
        AppealStatus.SUBMITTED -> OpsColors.BlueSoft
        AppealStatus.IN_PROGRESS -> OpsColors.AmberSoft
        AppealStatus.RESOLVED -> OpsColors.OliveSoft
        AppealStatus.CLOSED -> OpsColors.Subtle
        AppealStatus.REJECTED -> OpsColors.ErrorSoft
        else -> OpsColors.Subtle
    }
    OpsChip(
        text = AppealLabels.status(status),
        foreground = foreground,
        background = background,
        modifier = modifier,
    )
}

@Composable
private fun AppealPriorityChip(priority: Int?, modifier: Modifier = Modifier) {
    val foreground = when (priority) {
        AppealPriority.HIGH -> OpsColors.Amber
        AppealPriority.URGENT -> OpsColors.Error
        else -> OpsColors.Muted
    }
    val background = when (priority) {
        AppealPriority.HIGH -> OpsColors.AmberSoft
        AppealPriority.URGENT -> OpsColors.ErrorSoft
        else -> OpsColors.Subtle
    }
    OpsChip(
        text = AppealLabels.priority(priority),
        foreground = foreground,
        background = background,
        modifier = modifier,
    )
}
