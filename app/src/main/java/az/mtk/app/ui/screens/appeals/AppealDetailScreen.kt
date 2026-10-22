package az.mtk.app.ui.screens.appeals

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.AddAppealCommentRequestDto
import az.mtk.app.data.api.AppealCommentDto
import az.mtk.app.data.api.AppealDetailDto
import az.mtk.app.data.api.AppealLabels
import az.mtk.app.data.api.AppealStatus
import az.mtk.app.data.api.AppealsApi
import az.mtk.app.data.api.AssignAppealRequestDto
import az.mtk.app.data.api.EmployeeDto
import az.mtk.app.data.api.UpdateAppealStatusRequestDto
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppealDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val appeal: AppealDetailDto? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
    val notice: String? = null,
)

class AppealDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppealDetailState())
    val state = _state.asStateFlow()

    val selectedStatus = MutableStateFlow<Int?>(null)

    fun load(appealId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                AppGraph.service(AppealsApi::class.java).appeal(appealId).data
                    ?: error("Müraciət tapılmadı")
            }
                .onSuccess { appeal ->
                    _state.value = _state.value.copy(loading = false, error = null, appeal = appeal)
                    selectedStatus.value = appeal.status
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.toUserMessage())
                }
        }
    }

    fun selectStatus(value: String) {
        selectedStatus.value = value.toIntOrNull()
    }

    fun changeStatus(appealId: String) {
        val newStatus = selectedStatus.value ?: return
        if (newStatus == _state.value.appeal?.status || _state.value.busy) return
        runAction("Status dəyişdirildi") {
            AppGraph.service(AppealsApi::class.java).updateAppealStatus(
                appealId,
                UpdateAppealStatusRequestDto(newStatus),
            )
        }
    }

    fun assign(appealId: String, employeeId: String) {
        if (_state.value.busy) return
        runAction("Müraciət təhvil verildi") {
            AppGraph.service(AppealsApi::class.java).assignAppeal(
                appealId,
                AssignAppealRequestDto(employeeId),
            )
        }
    }

    fun sendComment(appealId: String, text: String, onSent: () -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null, notice = null)
            runCatching {
                AppGraph.service(AppealsApi::class.java).addComment(
                    appealId,
                    AddAppealCommentRequestDto(trimmed),
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(busy = false)
                    onSent()
                    load(appealId)
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(busy = false, actionError = failure.toUserMessage())
                }
        }
    }

    private fun runAction(success: String, action: suspend () -> Any) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, actionError = null, notice = null)
            runCatching { action() }
                .onSuccess {
                    _state.value = _state.value.copy(busy = false, notice = success)
                    _state.value.appeal?.id?.let { load(it) }
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(busy = false, actionError = failure.toUserMessage())
                }
        }
    }
}

@Composable
fun AppealDetailScreen(
    appealId: String,
    onBack: (() -> Unit)?,
    showTopBar: Boolean = true,
    viewModel: AppealDetailViewModel = mtkViewModel { AppealDetailViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(appealId) { viewModel.load(appealId) }

    val appeal = state.appeal
    val closed = AppealLabels.isClosed(appeal?.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = appeal?.title ?: "Müraciət",
                subtitle = appeal?.let { AppealLabels.status(it.status) },
                onBack = onBack,
            )
        }

        when {
            state.loading && appeal == null -> LoadingState("Müraciət yüklənir…")
            state.error != null && appeal == null -> ErrorState(state.error!!, onRetry = { viewModel.load(appealId) })
            appeal == null -> ErrorState("Müraciət tapılmadı", onRetry = { viewModel.load(appealId) })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(AppealLabels.status(appeal.status), appealStatusTone(appeal.status))
                        Pill(
                            AppealLabels.priority(appeal.priority),
                            appealPriorityTone(appeal.priority),
                            showDot = false,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${appeal.submitterName ?: "Naməlum"} tərəfindən ${Format.dateTime(appeal.createdAt)} tarixində göndərilib",
                        style = MaterialTheme.typography.bodySmall,
                        color = MtkColors.Muted,
                    )
                }

                item {
                    MtkCard {
                        SectionHeading("Təsvir")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            appeal.description ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MtkColors.Ink,
                        )
                    }
                }

                item {
                    MtkCard {
                        SectionHeading("Məlumat")
                        Spacer(Modifier.height(4.dp))
                        InfoRow("Kateqoriya", AppealLabels.category(appeal.category))
                        InfoRow("Müraciətçi", appeal.submitterName ?: "—")
                        appeal.assignedToName?.let { InfoRow("Məsul şəxs", it) }
                        if (!appeal.apartmentLabel.isNullOrBlank() || !appeal.garageLabel.isNullOrBlank()) {
                            InfoRow(
                                "Obyekt",
                                if (!appeal.apartmentLabel.isNullOrBlank()) {
                                    "Mənzil ${appeal.apartmentLabel}"
                                } else {
                                    "Qaraj ${appeal.garageLabel}"
                                },
                            )
                        }
                        appeal.updatedAt?.let { InfoRow("Son yenilənmə", Format.dateTime(it)) }
                        appeal.resolvedAt?.let { InfoRow("Həll edilib", Format.dateTime(it)) }
                        appeal.closedAt?.let { InfoRow("Bağlanıb", Format.dateTime(it)) }
                    }
                }

                // İdarəetmə: status dəyişmə + təhkim
                item {
                    MtkCard {
                        SectionHeading("İdarəetmə")
                        Spacer(Modifier.height(8.dp))
                        MtkSelect(
                            value = selectedStatus?.toString() ?: appeal.status?.toString() ?: "",
                            onValueChange = viewModel::selectStatus,
                            label = "Status",
                            options = AppealStatus.ALL.map { MtkOption(it.toString(), AppealLabels.status(it)) },
                        )
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(
                            text = "Statusu dəyiş",
                            onClick = { viewModel.changeStatus(appealId) },
                            enabled = selectedStatus != null && selectedStatus != appeal.status,
                            loading = state.busy,
                        )
                        if (!closed) {
                            Spacer(Modifier.height(14.dp))
                            SearchableSelect<EmployeeDto>(
                                label = "Məsul şəxs təyin et",
                                selectedLabel = appeal.assignedToName,
                                searchPlaceholder = "İşçi axtar…",
                                onLoad = { query ->
                                    AppGraph.repository.employees(page = 0, pageSize = 20, search = query).items
                                },
                                itemLabel = { Format.person(it.name, it.surname, it.fathersName) },
                                onSelect = { employee -> viewModel.assign(appealId, employee.id) },
                                onClear = null,
                            )
                        }
                        state.actionError?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
                        }
                        state.notice?.let { notice ->
                            Spacer(Modifier.height(8.dp))
                            Text(notice, style = MaterialTheme.typography.bodySmall, color = MtkColors.Positive)
                        }
                    }
                }

                // Yazışma
                item {
                    Text(
                        "Yazışma (${appeal.comments.orEmpty().size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (appeal.comments.orEmpty().isEmpty()) {
                    item { MtkCard { EmptyStateInline() } }
                } else {
                    items(appeal.comments.orEmpty(), key = { it.id }) { c ->
                        CommentCard(
                            comment = c,
                            fromSubmitter = c.userId == appeal.submitterId,
                        )
                    }
                }

                item {
                    MtkCard {
                        if (closed) {
                            Text(
                                "Bu müraciət bağlanıb, yeni şərh əlavə etmək olmur.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        } else {
                            MtkTextField(
                                value = comment,
                                onValueChange = { comment = it.take(2000) },
                                label = "Şərh",
                                placeholder = "Şərh yazın…",
                                singleLine = false,
                                minLines = 3,
                            )
                            Spacer(Modifier.height(10.dp))
                            PrimaryButton(
                                text = "Göndər",
                                onClick = {
                                    viewModel.sendComment(appealId, comment) { comment = "" }
                                },
                                enabled = comment.isNotBlank(),
                                loading = state.busy,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = { viewModel.load(appealId) }) }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateInline() {
    Text(
        "Hələ şərh yoxdur.",
        style = MaterialTheme.typography.bodySmall,
        color = MtkColors.Muted,
    )
}

@Composable
private fun CommentCard(comment: AppealCommentDto, fromSubmitter: Boolean) {
    MtkCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(
                initials = az.mtk.app.util.Format.initials(comment.userName),
                size = 32.dp,
                background = if (fromSubmitter) MtkColors.Green else MtkColors.Info,
            )
            Column(Modifier.padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.userName ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        "  ${if (fromSubmitter) "Müraciətçi" else "İdarə"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                    )
                }
                Text(
                    Format.dateTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedUltraSoft,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            comment.text ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MtkColors.Ink,
        )
    }
}
