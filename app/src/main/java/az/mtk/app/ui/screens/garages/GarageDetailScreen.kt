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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.GarageDto
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
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
}

@Composable
fun GarageDetailScreen(
    garageId: String,
    onBack: () -> Unit,
    onOpenOwner: (String) -> Unit,
    viewModel: GarageDetailViewModel = mtkViewModel { GarageDetailViewModel(garageId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                        }
                    } else {
                        MtkCard {
                            Text("Bu qaraja sahib təyin edilməyib.", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
