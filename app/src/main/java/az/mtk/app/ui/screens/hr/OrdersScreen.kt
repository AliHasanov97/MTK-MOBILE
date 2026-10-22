package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrOrderItemDto
import az.mtk.app.data.api.HrOrderKind
import az.mtk.app.data.api.Page
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

class OrdersViewModel : ViewModel() {
    private val _typeFilter = MutableStateFlow<Int?>(null)
    val typeFilter = _typeFilter.asStateFlow()

    val orders = Pager<HrOrderItemDto>(viewModelScope) { page, query ->
        HrApi.repository.orders(page, search = query, types = _typeFilter.value?.let { listOf(it) })
    }

    fun setTypeFilter(type: Int?) {
        _typeFilter.value = type
        orders.refresh()
    }
}

@Composable
fun OrdersScreen(
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false,
    viewModel: OrdersViewModel = mtkViewModel { OrdersViewModel() },
) {
    val orders by viewModel.orders.state.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Əmrlər",
                subtitle = "${orders.total} əmr",
                onBack = onBack,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MtkSelect(
                value = typeFilter?.toString(),
                onValueChange = { viewModel.setTypeFilter(it.toIntOrNull()) },
                label = "Növ üzrə filtr",
                options = listOf(MtkOption("", "Hamısı")) +
                    HrOrderKind.LABELS.entries.sortedBy { it.key }.map { MtkOption(it.key.toString(), it.value) },
                modifier = Modifier.weight(1f),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SearchField(
                value = orders.query,
                onValueChange = viewModel.orders::onQueryChange,
                placeholder = "Əmr nömrəsi və ya işçi",
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
            PrimaryButton(
                text = "+ Yeni əmr",
                onClick = { showCreate = true },
                modifier = Modifier.width(160.dp),
            )
        }
        Spacer(Modifier.height(6.dp))

        when {
            orders.loading && orders.items.isEmpty() -> LoadingState("Əmrlər yüklənir…")
            orders.error != null && orders.items.isEmpty() ->
                ErrorState(orders.error!!, onRetry = viewModel.orders::refresh)
            orders.items.isEmpty() -> EmptyState("Əmr tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(orders.items, key = { it.id }) { order ->
                    MtkCard(padding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Əmr № ${order.orderNumber ?: "—"}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    HrOrderKind.label(order.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Green,
                                )
                                Text(
                                    order.employee?.name
                                        ?: order.jobApplicant?.name
                                        ?: "Bütün işçilər",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${order.createdBy?.name ?: "—"} · ${Format.dateTime(order.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Pill(
                                HrOrderKind.label(order.type).take(18),
                                Tone.Brand,
                                showDot = false,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        ExportActionRow(
                            title = "PDF yüklə",
                            subtitle = "Əmrin PDF nüsxəsi",
                            path = "api/hr/${HrOrderKind.route(order.type)}/${order.id}/export-pdf",
                            scope = scope,
                        )
                    }
                }

                if (orders.items.size < orders.total) {
                    item {
                        OutlinedButton(
                            onClick = viewModel.orders::loadMore,
                            enabled = !orders.loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (orders.loadingMore) "Yüklənir…" else "Daha çox (${orders.items.size}/${orders.total})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (orders.error != null) {
                    item { ErrorState(orders.error!!, onRetry = viewModel.orders::refresh) }
                }
            }
        }
    }

    if (showCreate) {
        HrCreateOrderModal(
            onDismiss = { showCreate = false },
            onCreated = {
                showCreate = false
                viewModel.orders.refresh()
            },
        )
    }
}
