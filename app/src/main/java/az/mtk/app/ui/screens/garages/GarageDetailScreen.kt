package az.mtk.app.ui.screens.garages

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.AssignOwnerRequest
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.GarageDto
import az.mtk.app.data.api.GarageTypeOrdinal
import az.mtk.app.data.api.UpdateGarageRequest
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.buildings.canWriteBuildings
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GarageDetailState(
    val garage: GarageDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class GarageDetailViewModel(private val garageId: String) : ViewModel() {
    private val _state = MutableStateFlow(GarageDetailState())
    val state: StateFlow<GarageDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { AppGraph.repository.garage(garageId) }
                .onSuccess { _state.value = GarageDetailState(garage = it, loading = false) }
                .onFailure { _state.value = GarageDetailState(loading = false, error = it.toUserMessage()) }
        }
    }

    fun updateGarage(
        typeOrdinal: Int,
        description: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java).updateGarage(
                    garageId,
                    UpdateGarageRequest(type = typeOrdinal, description = description?.trim()?.takeIf { it.isNotEmpty() }),
                )
            }
                .onSuccess {
                    load()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun deleteGarage(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { AppGraph.service(BuildingsApi::class.java).deleteGarage(garageId) }
                .onSuccess { onDone() }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun assignOwner(ownerId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java)
                    .assignOwnerToGarage(garageId, AssignOwnerRequest(ownerId))
            }
                .onSuccess {
                    load()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun removeOwner(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { AppGraph.service(BuildingsApi::class.java).removeOwnerFromGarage(garageId) }
                .onSuccess {
                    load()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }
}

@Composable
fun GarageDetailScreen(
    garageId: String,
    onBack: () -> Unit,
    onOpenOwner: (String) -> Unit,
    viewModel: GarageDetailViewModel = mtkViewModel { GarageDetailViewModel(garageId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAssignOwner by remember { mutableStateOf(false) }
    var showRemoveOwner by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val canWrite = remember { canWriteBuildings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = "Qaraj ${state.garage?.garageNumber ?: ""}".trim(),
            subtitle = state.garage?.let { Format.garageType(it.type) },
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
                    val garage = state.garage ?: return@Column
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Qaraj ${garage.garageNumber ?: "—"}", style = MaterialTheme.typography.headlineSmall)
                            Pill(Format.garageType(garage.type), Tone.Brand)
                        }
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Növ", Format.garageType(garage.type))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Yaradılma tarixi", Format.date(garage.createdAt))
                        if (garage.updatedAt != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            InfoRow("Son dəyişiklik", Format.date(garage.updatedAt))
                        }
                        if (!garage.description.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(garage.description!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                        }
                        if (canWrite) {
                            Spacer(Modifier.height(12.dp))
                            SecondaryButton(text = "Redaktə et", onClick = { showEdit = true })
                            Spacer(Modifier.height(8.dp))
                            SecondaryButton(text = "Qarajı sil", onClick = { showDelete = true })
                        }
                    }

                    KeyValueHeader("Sahib")
                    val owner = garage.owner
                    if (owner?.id != null) {
                        MtkCard(onClick = { onOpenOwner(owner.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarCircle(Format.initials(owner.name))
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(owner.name ?: "—", style = MaterialTheme.typography.titleSmall)
                                    Text("Sahib məlumatlarına bax", style = MaterialTheme.typography.bodySmall, color = MtkColors.MutedSoft)
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
                                SecondaryButton(text = "Sahibi dəyiş", onClick = { showAssignOwner = true })
                                Spacer(Modifier.height(8.dp))
                                SecondaryButton(text = "Sahibi çıxar", onClick = { showRemoveOwner = true })
                            }
                        }
                    } else {
                        MtkCard {
                            Text("Bu qaraja sahib təyin edilməyib.", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                            if (canWrite) {
                                Spacer(Modifier.height(10.dp))
                                SecondaryButton(text = "Sahib təyin et", onClick = { showAssignOwner = true })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showEdit) {
        EditGarageModal(
            currentType = state.garage?.type,
            currentDescription = state.garage?.description,
            onDismiss = { showEdit = false },
            onSave = { typeOrdinal, description, onDone, onError ->
                viewModel.updateGarage(typeOrdinal, description, onDone, onError)
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Qarajı sil",
            message = "\"${state.garage?.garageNumber ?: "—"}\" qarajı silinsin? Bu əməliyyat geri qaytarılmır.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.deleteGarage(
                    onDone = onBack,
                    onError = { actionError = it },
                )
            },
        )
    }

    if (showAssignOwner) {
        GarageOwnerModal(
            onDismiss = { showAssignOwner = false },
            onConfirm = { ownerId, onDone, onError ->
                viewModel.assignOwner(ownerId, onDone, onError)
            },
        )
    }

    if (showRemoveOwner) {
        ConfirmDialog(
            title = "Sahibi çıxar",
            message = "Bu qarajdan sahib çıxarılsın?",
            confirmLabel = "Çıxar",
            danger = true,
            onDismiss = { showRemoveOwner = false },
            onConfirm = {
                showRemoveOwner = false
                viewModel.removeOwner(onDone = {}, onError = { actionError = it })
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

/** Qaraj redaktəsi — PATCH yalnız növ (rəqəmsal ordinal) və təsvir göndərir. */
@Composable
private fun EditGarageModal(
    currentType: String?,
    currentDescription: String?,
    onDismiss: () -> Unit,
    onSave: (typeOrdinal: Int, description: String?, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    var type by remember { mutableStateOf(currentType ?: "OpenParking") }
    var description by remember { mutableStateOf(currentDescription ?: "") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Qarajı redaktə et",
        onDismiss = onDismiss,
        confirmLabel = "Yadda saxla",
        confirmEnabled = !submitting,
        confirmLoading = submitting,
        onConfirm = {
            submitting = true
            error = null
            onSave(
                GarageTypeOrdinal.fromKey(type),
                description,
                { onDismiss() },
                {
                    submitting = false
                    error = it
                },
            )
        },
    ) {
        MtkSelect(
            value = type,
            onValueChange = { type = it },
            label = "Növ",
            required = true,
            options = listOf(
                MtkOption("OpenParking", "Açıq dayanacaq"),
                MtkOption("CoveredGarage", "Örtülü qaraj"),
                MtkOption("Storage", "Anbar yeri"),
            ),
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Təsvir",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GarageOwnerModal(
    onDismiss: () -> Unit,
    onConfirm: (ownerId: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Sahib təyin et",
        subtitle = "Qaraj üçün sahib seçin",
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
