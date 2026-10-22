package az.mtk.app.ui.screens.inventory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import az.mtk.app.data.api.InvLowStockDto
import az.mtk.app.data.api.InvStockDto
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private enum class StockSegment(val label: String) {
    Stock("Qalıqlar"),
    Low("Az qalan"),
}

class WarehouseViewModel : ViewModel() {
    private val _stock = MutableStateFlow<List<InvStockDto>?>(null)
    val stock = _stock.asStateFlow()

    private val _low = MutableStateFlow<List<InvLowStockDto>?>(null)
    val low = _low.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _error.value = null
            _stock.value = null
            _low.value = null
            runCatching {
                InventoryRepo.repository.stocks()
            }.onSuccess { _stock.value = it }
                .onFailure { _error.value = it.toUserMessage() }
            runCatching {
                InventoryRepo.repository.lowStock()
            }.onSuccess { _low.value = it }
                .onFailure { failure ->
                    // Az qalan siyahısı yalnız ikinci bölmə üçündür; ilk xəta varsa onu ehtiyatla saxla
                    _error.value = _error.value ?: failure.toUserMessage()
                }
        }
    }
}

@Composable
fun WarehouseScreen(
    onBack: (() -> Unit)?,
    onOpenMaterial: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: WarehouseViewModel = mtkViewModel { WarehouseViewModel() },
) {
    val stock by viewModel.stock.collectAsStateWithLifecycle()
    val low by viewModel.low.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var segment by remember { mutableStateOf(StockSegment.Stock) }
    var search by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Anbar",
                subtitle = "Material qalıqları",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockSegment.entries.forEach { value ->
                    FilterChip(
                        selected = segment == value,
                        onClick = { segment = value },
                        label = { Text(value.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MtkColors.Green,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Axtar (kod, ad…)",
            )
        }

        when {
            error != null && stock == null -> ErrorState(error!!, onRetry = viewModel::refresh)
            segment == StockSegment.Stock -> StockSegmentBody(
                stock = stock,
                search = search,
                onOpenMaterial = onOpenMaterial,
                onRetry = viewModel::refresh,
            )
            else -> LowStockSegmentBody(
                low = low,
                loading = low == null && error == null,
                onOpenMaterial = onOpenMaterial,
                onRetry = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun StockSegmentBody(
    stock: List<InvStockDto>?,
    search: String,
    onOpenMaterial: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val term = search.trim().lowercase()
    val visible = (stock ?: emptyList()).filter { item ->
        term.isBlank() || "${item.nomenclatureCode} ${item.nomenclatureName}".lowercase().contains(term)
    }

    when {
        stock == null -> LoadingState("Anbar qalıqları yüklənir…")
        visible.isEmpty() -> EmptyState(
            if (stock.isEmpty()) "Qalıq yoxdur — hələ heç bir mal daxil edilməyib."
            else "Bu axtarışa uyğun material tapılmadı.",
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Anbar qalıqları · ${visible.size} material",
                    style = MaterialTheme.typography.labelMedium,
                    color = MtkColors.MutedSoft,
                )
            }
            items(visible, key = { it.nomenclatureId }) { item ->
                MtkCard(onClick = { onOpenMaterial(item.nomenclatureId) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.nomenclatureName ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                item.nomenclatureCode ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = MtkColors.MutedSoft,
                            )
                        }
                        Text(
                            Format.number(item.quantityOnHand),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MtkColors.Green,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Son əməliyyat: ${Format.dateTime(item.lastTransactionDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockSegmentBody(
    low: List<InvLowStockDto>?,
    loading: Boolean,
    onOpenMaterial: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loading -> LoadingState("Minimum ehtiyat siyahısı yüklənir…")
        low == null -> ErrorState("Minimum ehtiyat siyahısı alınmadı", onRetry = onRetry)
        low.isEmpty() -> EmptyState("Bütün ehtiyatlar kifayətdir — heç bir material minimum səviyyədən aşağı deyil.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Minimum ehtiyatdan aşağı · ${low.size} material",
                    style = MaterialTheme.typography.labelMedium,
                    color = MtkColors.MutedSoft,
                )
            }
            items(low, key = { it.nomenclatureId }) { item ->
                MtkCard(
                    onClick = { onOpenMaterial(item.nomenclatureId) },
                    borderColor = MtkColors.Warning.copy(alpha = 0.4f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.nomenclatureName ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                item.nomenclatureCode ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = MtkColors.MutedSoft,
                            )
                        }
                        Pill("Çatışmır: ${Format.number(item.deficit)}", Tone.Warning)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Qalıq: ${Format.number(item.quantityOnHand)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Negative,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Min. ehtiyat: ${Format.number(item.minStockLevel)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        )
                    }
                }
            }
        }
    }
}
