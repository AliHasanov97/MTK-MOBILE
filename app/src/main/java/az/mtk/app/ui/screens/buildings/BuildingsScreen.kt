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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ChevronRight
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
import az.mtk.app.data.api.BuildingDto
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.ModuleIcon
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BuildingsState(
    val buildings: List<BuildingDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class BuildingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(BuildingsState())
    val state: StateFlow<BuildingsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { AppGraph.repository.buildings() }
                .onSuccess { _state.value = BuildingsState(buildings = it, loading = false) }
                .onFailure { _state.value = BuildingsState(loading = false, error = it.toUserMessage()) }
        }
    }
}

@Composable
fun BuildingsScreen(
    onBack: (() -> Unit)?,
    onOpenBuilding: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: BuildingsViewModel = mtkViewModel { BuildingsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filtered = remember(state.buildings, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) state.buildings
        else state.buildings.filter {
            it.name.lowercase().contains(needle) ||
                (it.fullAddress ?: "").lowercase().contains(needle)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Binalar",
                subtitle = if (state.loading) "Yüklənir…" else "${state.buildings.size} bina",
                onBack = onBack,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Bina adı və ya ünvan",
            )
            Spacer(Modifier.height(14.dp))

            when {
                state.loading -> LoadingState("Binalar yüklənir…")
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                filtered.isEmpty() -> EmptyState("Bina tapılmadı")
                else -> filtered.forEachIndexed { index, building ->
                    MtkCard(onClick = { onOpenBuilding(building.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ModuleIcon(icon = Icons.Outlined.Apartment)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(
                                    building.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    building.fullAddress ?: "Ünvan göstərilməyib",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (building.status != null) {
                                    Pill(building.status, if (building.status.equals("Active", true)) Tone.Success else Tone.Neutral)
                                }
                                Spacer(Modifier.height(8.dp))
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MtkColors.MutedUltraSoft,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    if (index != filtered.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
