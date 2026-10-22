package az.mtk.app.ui.screens.owners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
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
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.ui.components.AvatarCircle
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

class OwnersViewModel : ViewModel() {
    val pager = Pager<OwnerItemDto>(viewModelScope) { page, query ->
        AppGraph.repository.owners(page, search = query)
    }
}

@Composable
fun OwnersScreen(
    onBack: (() -> Unit)?,
    onOpenOwner: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: OwnersViewModel = mtkViewModel { OwnersViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Sahiblər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} sahib",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Ad, e-poçt və ya telefon",
            )
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Sahiblər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Sahib tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { owner ->
                    MtkCard(onClick = { onOpenOwner(owner.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(Format.initials(owner.fullName))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        owner.fullName ?: "—",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (owner.isActive == false) {
                                        Spacer(Modifier.width(6.dp))
                                        Pill("Qeyri-aktiv", Tone.Neutral)
                                    }
                                }
                                if (!owner.email.isNullOrBlank()) {
                                    Text(
                                        owner.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!owner.phoneNumber.isNullOrBlank()) {
                                    Text(owner.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MtkColors.MutedSoft)
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MtkColors.MutedUltraSoft,
                                modifier = Modifier.size(20.dp),
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
