package az.mtk.app.ui.screens.apartments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApartmentDto
import az.mtk.app.data.api.AssignOwnerRequest
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.OwnershipRecordDto
import az.mtk.app.data.api.TransferApartmentOwnershipRequest
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.buildings.canWriteBuildings
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApartmentDetailState(
    val apartment: ApartmentDto? = null,
    val history: List<OwnershipRecordDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class ApartmentDetailViewModel(private val apartmentId: String) : ViewModel() {
    private val _state = MutableStateFlow(ApartmentDetailState())
    val state: StateFlow<ApartmentDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val repo = AppGraph.repository
            val apartment = async { runCatching { repo.apartment(apartmentId) } }
            val history = async { runCatching { repo.ownershipHistory(apartmentId) } }

            val apartmentResult = apartment.await()
            val historyResult = history.await()
            _state.value = ApartmentDetailState(
                apartment = apartmentResult.getOrNull(),
                history = historyResult.getOrDefault(emptyList()),
                loading = false,
                error = (apartmentResult.exceptionOrNull() ?: historyResult.exceptionOrNull())?.toUserMessage(),
            )
        }
    }

    fun assignOwner(ownerId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java)
                    .assignOwnerToApartment(apartmentId, AssignOwnerRequest(ownerId))
            }
                .onSuccess {
                    load()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun transferOwnership(newOwnerId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java)
                    .transferApartmentOwnership(apartmentId, TransferApartmentOwnershipRequest(newOwnerId))
            }
                .onSuccess {
                    load()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }
}

@Composable
fun ApartmentDetailScreen(
    apartmentId: String,
    onBack: () -> Unit,
    onOpenOwner: (String) -> Unit,
    viewModel: ApartmentDetailViewModel = mtkViewModel { ApartmentDetailViewModel(apartmentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAssignOwner by remember { mutableStateOf(false) }
    var showTransferOwner by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val canWrite = remember { canWriteBuildings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = "Mənzil ${state.apartment?.apartmentNumber ?: ""}".trim(),
            subtitle = state.apartment?.building?.name,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> {
                    val apartment = state.apartment ?: return@Column
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Mənzil ${apartment.apartmentNumber ?: "—"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Text(
                                    apartment.building?.name ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                            if (apartment.status != null) Pill(apartment.status, Tone.Neutral)
                        }
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Mərtəbə", apartment.floor?.toString() ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Otaq sayı", apartment.roomCount?.toString() ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Sahə", Format.area(apartment.areaSquareMeters))
                    }

                    Spacer(Modifier.height(16.dp))
                    KeyValueHeader("Sahib")
                    val owner = apartment.currentOwner
                    if (owner?.id != null) {
                        MtkCard(onClick = { onOpenOwner(owner.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarCircle(Format.initials(owner.name))
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(owner.name ?: "—", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Sahib məlumatlarına bax",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.MutedSoft,
                                    )
                                }
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MtkColors.MutedUltraSoft,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            if (canWrite) {
                                Spacer(Modifier.height(10.dp))
                                SecondaryButton(
                                    text = "Mülkiyyəti transfer et",
                                    onClick = { showTransferOwner = true },
                                )
                            }
                        }
                    } else {
                        MtkCard {
                            Text(
                                "Bu mənzilə sahib təyin edilməyib.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                            if (canWrite) {
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton(
                                    text = "Sahib təyin et",
                                    onClick = { showAssignOwner = true },
                                )
                            }
                        }
                    }

                    KeyValueHeader("Mülkiyyət tarixçəsi")
                    if (state.history.isEmpty()) {
                        EmptyState("Tarixçə qeydi yoxdur")
                    } else {
                        MtkCard(padding = 4.dp) {
                            state.history.forEachIndexed { index, record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text("⇄", color = MtkColors.Positive, fontSize = 16.sp)
                                    Spacer(Modifier.height(0.dp))
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                        Text(
                                            "${record.previousOwnerName ?: "—"} → ${record.newOwnerName ?: "—"}",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            Format.date(record.transferDate),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                }
                                if (index != state.history.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAssignOwner) {
        OwnerPickModal(
            title = "Sahib təyin et",
            subtitle = "Bu mənzil üçün sahib seçin",
            onDismiss = { showAssignOwner = false },
            onConfirm = { ownerId, onDone, onError ->
                viewModel.assignOwner(ownerId, onDone, onError)
            },
        )
    }

    if (showTransferOwner) {
        OwnerPickModal(
            title = "Mülkiyyəti transfer et",
            subtitle = "Yeni sahibi seçin — transfer dərhal qüvvəyə minir",
            onDismiss = { showTransferOwner = false },
            onConfirm = { ownerId, onDone, onError ->
                viewModel.transferOwnership(ownerId, onDone, onError)
            },
        )
    }

    if (actionError != null) {
        ConfirmDialog(
            title = "Xəta baş verdi",
            message = actionError!!,
            confirmLabel = "Bağla",
            onDismiss = { actionError = null },
            onConfirm = { actionError = null },
        )
    }
}

/** Sahib seçimi — mənzilə sahib təyin etmək / mülkiyyəti transfer etmək üçün. */
@Composable
private fun OwnerPickModal(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onConfirm: (ownerId: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        confirmLabel = "Təsdiqlə",
        confirmEnabled = selectedId != null && !submitting,
        confirmLoading = submitting,
        onConfirm = {
            submitting = true
            error = null
            onConfirm(
                selectedId!!,
                { onDismiss() },
                {
                    submitting = false
                    error = it
                },
            )
        },
    ) {
        SearchableSelect(
            label = "Sahib",
            required = true,
            selectedLabel = selectedName,
            searchPlaceholder = "Ad, email və ya telefonla axtar…",
            onLoad = { query ->
                AppGraph.repository.owners(page = 0, pageSize = 8, search = query).items
            },
            itemLabel = { it.fullName ?: "—" },
            onSelect = { owner ->
                selectedId = owner.id
                selectedName = owner.fullName
            },
            onClear = {
                selectedId = null
                selectedName = null
            },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(12.dp))
    }
}
