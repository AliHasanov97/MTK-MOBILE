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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.GarageDto
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
