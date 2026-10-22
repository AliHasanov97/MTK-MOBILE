package az.mtk.app.ui.screens.garages

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.CreateGarageRequest
import az.mtk.app.data.api.GarageDto
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
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.buildings.canWriteBuildings
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val typeFilters = listOf(
    "Hamısı" to null,
    "Açıq dayanacaq" to "OpenParking",
    "Örtülü qaraj" to "CoveredGarage",
    "Anbar yeri" to "Storage",
)

class GaragesViewModel : ViewModel() {
    private val selectedType = MutableStateFlow<String?>(null)
    val type = selectedType.asStateFlow()

    val pager = Pager<GarageDto>(viewModelScope) { page, query ->
        AppGraph.repository.garages(page, search = query, type = selectedType.value)
    }

    fun selectType(value: String?) {
        if (value == selectedType.value) return
        selectedType.value = value
        pager.refresh()
    }

    fun createGarage(
        garageNumber: String,
        garageType: String,
        description: String?,
        ownerId: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java).createGarage(
                    CreateGarageRequest(
                        ownerId = ownerId,
                        garageNumber = garageNumber.trim(),
                        garageType = garageType,
                        description = description?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
                .onSuccess {
                    pager.refresh()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }
}

@Composable
fun GaragesScreen(
    onBack: (() -> Unit)?,
    onOpenGarage: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: GaragesViewModel = mtkViewModel { GaragesViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val selectedType by viewModel.type.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    val canWrite = remember { canWriteBuildings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Qarajlar",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} qeyd",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Qaraj nömrəsi və ya sahib",
            )
            if (canWrite) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "Qaraj yarat", onClick = { showCreate = true })
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeFilters.forEach { (label, value) ->
                    FilterChip(
                        selected = selectedType == value,
                        onClick = { viewModel.selectType(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MtkColors.Green,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    )
                }
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Qarajlar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Qaraj tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { garage ->
                    MtkCard(onClick = { onOpenGarage(garage.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Qaraj ${garage.garageNumber ?: "—"}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Pill(Format.garageType(garage.type), Tone.Neutral)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            garage.owner?.name?.let { "Sahib: $it" } ?: "Sahib təyin edilməyib",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (garage.owner == null) MtkColors.MutedSoft else MtkColors.Green,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!garage.description.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                garage.description!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
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
        CreateGarageModal(
            onDismiss = { showCreate = false },
            onCreate = { number, type, description, ownerId, onDone, onError ->
                viewModel.createGarage(number, type, description, ownerId, onDone, onError)
            },
        )
    }
}

private val garageTypeOptions = listOf(
    MtkOption("OpenParking", "Açıq dayanacaq"),
    MtkOption("CoveredGarage", "Örtülü qaraj"),
    MtkOption("Storage", "Anbar yeri"),
)

/** "Qaraj yarat" modalı — CreateGarageRequest sahələri üzrə. */
@Composable
private fun CreateGarageModal(
    onDismiss: () -> Unit,
    onCreate: (
        garageNumber: String,
        garageType: String,
        description: String?,
        ownerId: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("OpenParking") }
    var description by remember { mutableStateOf("") }
    var ownerId by remember { mutableStateOf<String?>(null) }
    var ownerName by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Qaraj yarat",
        subtitle = "Yeni qarajın məlumatlarını daxil edin",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = number.isNotBlank() && !submitting,
        confirmLoading = submitting,
        onConfirm = {
            submitting = true
            error = null
            onCreate(
                number,
                type,
                description,
                ownerId,
                { onDismiss() },
                {
                    submitting = false
                    error = it
                },
            )
        },
    ) {
        MtkTextField(
            value = number,
            onValueChange = { number = it },
            label = "Qaraj nömrəsi",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkSelect(
            value = type,
            onValueChange = { type = it },
            label = "Növ",
            required = true,
            options = garageTypeOptions,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Təsvir",
        )
        Spacer(Modifier.height(10.dp))
        SearchableSelect(
            label = "Sahib (opsional)",
            selectedLabel = ownerName,
            searchPlaceholder = "Ad, email və ya telefonla axtar…",
            onLoad = { query ->
                AppGraph.repository.owners(page = 0, pageSize = 8, search = query).items
            },
            itemLabel = { it.fullName ?: "—" },
            onSelect = { owner ->
                ownerId = owner.id
                ownerName = owner.fullName
            },
            onClear = {
                ownerId = null
                ownerName = null
            },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(12.dp))
    }
}
