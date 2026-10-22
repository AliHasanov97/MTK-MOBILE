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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialsViewModel : ViewModel() {
    val pager = Pager<InvNomenclatureListItem>(viewModelScope) { page, query ->
        InventoryRepo.repository.nomenclaturePage(page, InventoryRepositoryPageSize, query)
    }

    companion object {
        const val InventoryRepositoryPageSize = 20
    }
}

/** Kateqoriya / vahid / status seçimləri — həm yaradma, həm redaktə formasında istifadə olunur. */
internal val categoryOptions = listOf(
    MtkOption(InvEnums.CATEGORY_MATERIAL.toString(), "Material"),
    MtkOption(InvEnums.CATEGORY_EQUIPMENT.toString(), "Avadanlıq"),
    MtkOption(InvEnums.CATEGORY_SERVICE.toString(), "Xidmət"),
    MtkOption(InvEnums.CATEGORY_SUPPLY.toString(), "Təchizat"),
    MtkOption(InvEnums.CATEGORY_OTHER.toString(), "Digər"),
)

internal val unitOptions = listOf(
    MtkOption(InvEnums.UNIT_PIECE.toString(), "Ədəd"),
    MtkOption(InvEnums.UNIT_METER.toString(), "Metr"),
    MtkOption(InvEnums.UNIT_KILOGRAM.toString(), "Kiloqram"),
    MtkOption(InvEnums.UNIT_LITER.toString(), "Litr"),
    MtkOption(InvEnums.UNIT_SET.toString(), "Komplekt"),
    MtkOption(InvEnums.UNIT_SQUARE_METER.toString(), "kv. metr"),
    MtkOption(InvEnums.UNIT_CUBIC_METER.toString(), "kub metr"),
    MtkOption(InvEnums.UNIT_BOX.toString(), "Qutu"),
    MtkOption(InvEnums.UNIT_PACKAGE.toString(), "Paket"),
    MtkOption(InvEnums.UNIT_SACK.toString(), "Çuval"),
)

internal val statusOptions = listOf(
    MtkOption("true", "Aktiv"),
    MtkOption("false", "Deaktiv"),
)

/**
 * Yeni material (editing == null) və ya redaktə forması.
 * Redaktədə [initial] tam [az.mtk.app.data.api.InvNomenclatureResponse] obyektindən doldurulur.
 */
@Composable
internal fun NomenclatureFormModal(
    editingId: String?,
    initial: az.mtk.app.data.api.InvNomenclatureResponse?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf((initial?.category ?: InvEnums.CATEGORY_MATERIAL).toString()) }
    var unit by remember { mutableStateOf((initial?.unit ?: InvEnums.UNIT_PIECE).toString()) }
    var minStockLevel by remember { mutableStateOf(initial?.minStockLevel?.let { it.toString().trimEnd('0', '.').takeIf(String::isNotEmpty) } ?: "") }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    MtkModal(
        title = if (editingId == null) "Yeni material" else "Materialı redaktə et",
        subtitle = if (editingId == null) "Anbar reyestrına yeni nomenklatura əlavə edin" else initial?.code,
        onDismiss = onDismiss,
        confirmLabel = if (editingId == null) "Yarat" else "Yadda saxla",
        confirmEnabled = name.isNotBlank() && (editingId != null || code.isNotBlank()),
        confirmLoading = saving,
        onConfirm = {
            val minLevel = minStockLevel.trim().replace(',', '.').toDoubleOrNull()
            if (minLevel != null && minLevel < 0) {
                error = "Minimum ehtiyat səviyyəsi mənfi ola bilməz."
                return@MtkModal
            }
            if (editingId == null && code.isBlank()) {
                error = "Kod daxil edin."
                return@MtkModal
            }
            if (name.isBlank()) {
                error = "Ad daxil edin."
                return@MtkModal
            }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    if (editingId == null) {
                        InventoryRepo.repository.createNomenclature(
                            az.mtk.app.data.api.InvSaveNomenclatureRequest(
                                code = code.trim(),
                                name = name.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                category = category.toInt(),
                                unit = unit.toInt(),
                                minStockLevel = minLevel,
                                isActive = isActive,
                            ),
                        )
                    } else {
                        InventoryRepo.repository.updateNomenclature(
                            editingId,
                            az.mtk.app.data.api.InvUpdateNomenclatureRequest(
                                id = editingId,
                                name = name.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                category = category.toInt(),
                                unit = unit.toInt(),
                                minStockLevel = minLevel,
                            ),
                        )
                    }
                }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage() }
                saving = false
            }
        },
    ) {
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MtkColors.Negative,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (editingId == null) {
            MtkTextField(
                value = code,
                onValueChange = { code = it },
                label = "Kod",
                required = true,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        MtkTextField(
            value = name,
            onValueChange = { name = it },
            label = "Ad",
            required = true,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MtkSelect(
                value = category,
                onValueChange = { category = it },
                label = "Kateqoriya",
                options = categoryOptions,
                modifier = Modifier.weight(1f),
            )
            MtkSelect(
                value = unit,
                onValueChange = { unit = it },
                label = "Ölçü vahidi",
                options = unitOptions,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MtkTextField(
                value = minStockLevel,
                onValueChange = { minStockLevel = it },
                label = "Minimum ehtiyat",
                modifier = Modifier.weight(1f),
            )
            if (editingId == null) {
                MtkSelect(
                    value = isActive.toString(),
                    onValueChange = { isActive = it.toBoolean() },
                    label = "Status",
                    options = statusOptions,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f)) {}
            }
        }
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Təsvir",
            singleLine = false,
            minLines = 3,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun MaterialsScreen(
    onBack: (() -> Unit)?,
    onOpenMaterial: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: MaterialsViewModel = mtkViewModel { MaterialsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var formOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<InvNomenclatureListItem?>(null) }
    var deleting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Materiallar",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} qeyd",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (kod, ad…)",
            )
            Spacer(Modifier.height(10.dp))
            az.mtk.app.ui.components.PrimaryButton(
                text = "+ Yeni material",
                onClick = { formOpen = true },
            )
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Materiallar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Material tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    MtkCard(onClick = { onOpenMaterial(item.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.code ?: "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                                if (!item.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        item.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Pill(
                                if (item.isActive == true) "Aktiv" else "Deaktiv",
                                if (item.isActive == true) Tone.Success else Tone.Neutral,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${InvEnums.categoryLabel(item.category)} · ${InvEnums.unitLabel(item.unit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MtkColors.Muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.TextButton(
                                onClick = { onOpenMaterial(item.id) },
                            ) { Text("Detal", fontSize = 13.sp, color = MtkColors.Green) }
                            androidx.compose.material3.TextButton(
                                onClick = { deleteTarget = item },
                            ) { Text("Sil", fontSize = 13.sp, color = MtkColors.Negative) }
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

    if (formOpen) {
        NomenclatureFormModal(
            editingId = null,
            initial = null,
            onDismiss = { formOpen = false },
            onSaved = {
                formOpen = false
                viewModel.pager.refresh()
            },
        )
    }

    deleteTarget?.let { target ->
        az.mtk.app.ui.components.ConfirmDialog(
            title = "Materialı sil",
            message = "${target.name ?: "—"} silinsin? Bu əməliyyat geri qaytarılmır.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleting = true
                scope.launch {
                    runCatching { InventoryRepo.repository.deleteNomenclature(target.id) }
                        .onSuccess {
                            deleteTarget = null
                            viewModel.pager.refresh()
                        }
                        .onFailure { error = it.toUserMessage() }
                    deleting = false
                }
            },
        )
    }
}
