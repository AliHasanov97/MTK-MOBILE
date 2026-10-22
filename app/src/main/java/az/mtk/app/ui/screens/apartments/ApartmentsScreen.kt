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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import az.mtk.app.data.api.ApartmentDto
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

class ApartmentsViewModel : ViewModel() {
    val pager = Pager<ApartmentDto>(viewModelScope) { page, query ->
        AppGraph.repository.apartments(page, search = query)
    }
}

@Composable
fun ApartmentsScreen(
    onBack: (() -> Unit)?,
    onOpenApartment: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: ApartmentsViewModel = mtkViewModel { ApartmentsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Mənzillər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} mənzil",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Mənzil, bina və ya sahib",
            )
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Mənzillər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Mənzil tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { apartment ->
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
                            if (apartment.status != null) Pill(apartment.status, Tone.Neutral)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            apartment.building?.name ?: "Bina göstərilməyib",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            listOfNotNull(
                                apartment.roomCount?.let { "$it otaq" },
                                apartment.floor?.let { "$it-ci mərtəbə" },
                                apartment.areaSquareMeters?.let { Format.area(it) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MtkColors.MutedSoft,
                        )
                        if (apartment.currentOwner?.name != null) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Sahib: ${apartment.currentOwner.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Green,
                                maxLines = 1,
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
