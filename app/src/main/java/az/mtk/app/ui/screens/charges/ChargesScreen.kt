package az.mtk.app.ui.screens.charges

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.ChargeStatus
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

private val chargeFilters = listOf(
    "Hamısı" to null,
    "Qalıq borc" to true,
    "Ödənilib" to false,
)

class ChargesViewModel : ViewModel() {
    private val debtOnly = MutableStateFlow<Boolean?>(null)
    val filter = debtOnly.asStateFlow()

    val pager = Pager<ChargeDto>(viewModelScope) { page, query ->
        AppGraph.repository.charges(page, search = query, debtOnly = debtOnly.value)
    }

    fun selectFilter(value: Boolean?) {
        if (value == debtOnly.value) return
        debtOnly.value = value
        pager.refresh()
    }
}

private fun statusTone(status: Int?): Tone = when (status) {
    ChargeStatus.UNPAID -> Tone.Danger
    ChargeStatus.PARTIALLY_PAID -> Tone.Warning
    ChargeStatus.PAID -> Tone.Success
    else -> Tone.Neutral
}

@Composable
fun ChargesScreen(
    onBack: (() -> Unit)?,
    onOpenOwner: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: ChargesViewModel = mtkViewModel { ChargesViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Borclar",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} hesab",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Sakin, əmlak və ya dövr",
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chargeFilters.forEach { (label, value) ->
                    FilterChip(
                        selected = selectedFilter == value,
                        onClick = { viewModel.selectFilter(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MtkColors.Green,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Borclar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Hesab tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { charge ->
                    val outstanding = ((charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)).coerceAtLeast(0.0)
                    MtkCard(
                        onClick = charge.ownerId?.let { ownerId -> { onOpenOwner(ownerId) } },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    charge.partyName ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    charge.propertyLabel ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Pill(Format.chargeStatus(charge.status), statusTone(charge.status))
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text(Format.period(charge.period), style = MaterialTheme.typography.labelMedium, color = MtkColors.MutedSoft)
                                if (!charge.description.isNullOrBlank()) {
                                    Text(
                                        charge.description!!,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MtkColors.MutedSoft,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    Format.money(charge.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (charge.status == ChargeStatus.PAID) "Tam ödənilib" else "Qalıq: ${Format.money(outstanding)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (charge.status == ChargeStatus.PAID) MtkColors.Positive else MtkColors.Negative,
                                )
                            }
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
