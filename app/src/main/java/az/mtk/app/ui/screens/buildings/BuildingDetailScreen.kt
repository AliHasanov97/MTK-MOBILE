package az.mtk.app.ui.screens.buildings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApartmentDto
import az.mtk.app.data.api.BuildingDto
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BuildingDetailState(
    val building: BuildingDto? = null,
    val apartments: List<ApartmentDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class BuildingDetailViewModel(private val buildingId: String) : ViewModel() {
    private val _state = MutableStateFlow(BuildingDetailState())
    val state: StateFlow<BuildingDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val repo = AppGraph.repository
            val building = async { runCatching { repo.building(buildingId) } }
            val apartments = async { runCatching { repo.apartmentsByBuilding(buildingId) } }

            val buildingResult = building.await()
            val apartmentsResult = apartments.await()
            val error = buildingResult.exceptionOrNull() ?: apartmentsResult.exceptionOrNull()

            _state.value = BuildingDetailState(
                building = buildingResult.getOrNull(),
                apartments = apartmentsResult.getOrDefault(emptyList()),
                loading = false,
                error = error?.toUserMessage(),
            )
        }
    }
}

@Composable
fun BuildingDetailScreen(
    buildingId: String,
    onBack: () -> Unit,
    onOpenApartment: (String) -> Unit,
    viewModel: BuildingDetailViewModel = mtkViewModel { BuildingDetailViewModel(buildingId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filtered = remember(state.apartments, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) state.apartments
        else state.apartments.filter {
            (it.apartmentNumber ?: "").lowercase().contains(needle) ||
                (it.currentOwner?.name ?: "").lowercase().contains(needle)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.building?.name ?: "Bina",
            subtitle = state.building?.fullAddress,
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
                    val building = state.building
                    if (building != null) {
                        MtkCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(building.name, style = MaterialTheme.typography.titleLarge)
                                if (building.status != null) {
                                    Pill(
                                        building.status,
                                        if (building.status.equals("Active", true)) Tone.Success else Tone.Neutral,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                building.fullAddress ?: "Ünvan göstərilməyib",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                            if (!building.description.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    building.description!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            InfoRow("Mənzil sayı", state.apartments.size.toString())
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    KeyValueHeader("Mənzillər")
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Mənzil nömrəsi və ya sahib",
                    )
                    Spacer(Modifier.height(10.dp))

                    if (filtered.isEmpty()) {
                        EmptyState("Mənzil tapılmadı")
                    } else {
                        filtered.forEachIndexed { index, apartment ->
                            MtkCard(onClick = { onOpenApartment(apartment.id) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Mənzil ${apartment.apartmentNumber ?: "—"}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    if (apartment.status != null) {
                                        Pill(apartment.status, Tone.Neutral)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    listOfNotNull(
                                        apartment.roomCount?.let { "$it otaq" },
                                        apartment.floor?.let { "$it-ci mərtəbə" },
                                        apartment.areaSquareMeters?.let { Format.area(it) },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    apartment.currentOwner?.name?.let { "Sahib: $it" } ?: "Sahib təyin edilməyib",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (apartment.currentOwner == null) MtkColors.MutedSoft else MtkColors.Green,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (index != filtered.lastIndex) Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
