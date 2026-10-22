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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureListItem
import az.mtk.app.data.api.InvRecordIssueRequest
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.data.api.Page
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IssuesViewModel : ViewModel() {
    /**
     * transactions/search səhifələməni özü aparmır (pageNumber 1-based, böyük
     * page size ilə bir dəfə çəkir), ona görə axtarış lokal filtrlənir.
     */
    val pager = Pager<InvTransactionDto>(viewModelScope) { _, query ->
        val rows = InventoryRepo.repository.transactions(
            transactionType = InvEnums.TRANSACTION_ISSUE,
            pageSize = 200,
        )
        val term = query.trim().lowercase()
        val visible = if (term.isBlank()) rows else rows.filter { row ->
            "${row.nomenclatureName} ${row.notes}".lowercase().contains(term)
        }
        Page(visible, visible.size, 0, visible.size.coerceAtLeast(1))
    }
}

@Composable
fun IssuesScreen(
    onBack: (() -> Unit)?,
    onOpenMaterial: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: IssuesViewModel = mtkViewModel { IssuesViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var createOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Çıxarışlar",
                subtitle = if (state.loading) "Yüklənir…" else "Anbardan mal çıxarışı",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            az.mtk.app.ui.components.SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (material, qeyd…)",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni çıxarış", onClick = { createOpen = true })
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Çıxarışlar yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Hələ çıxarış yoxdur")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { issue ->
                    MtkCard(onClick = { issue.nomenclatureId?.let(onOpenMaterial) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    issue.nomenclatureName ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    Format.dateTime(issue.transactionDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Text(
                                "−${Format.number(issue.quantity)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MtkColors.Negative,
                            )
                        }
                        if (!issue.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                issue.notes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
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

    if (createOpen) {
        IssueFormModal(
            onDismiss = { createOpen = false },
            onSaved = {
                createOpen = false
                viewModel.pager.refresh()
            },
        )
    }
}

@Composable
private fun IssueFormModal(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var nomenclatureId by remember { mutableStateOf<String?>(null) }
    var nomenclatureLabel by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    MtkModal(
        title = "Yeni çıxarış",
        subtitle = "Anbardan mal çıxarışı. Kifayət qədər qalıq yoxdursa, çıxarış qeydə alınmır.",
        onDismiss = onDismiss,
        confirmLabel = "Qeyd et",
        confirmEnabled = nomenclatureId != null && quantity.toDoubleOrNull()?.let { it > 0 } == true,
        confirmLoading = saving,
        onConfirm = {
            val qty = quantity.replace(',', '.').toDoubleOrNull()
            when {
                nomenclatureId == null -> error = "Material seçilməlidir."
                qty == null || qty <= 0.0 -> error = "Miqdar 0-dan böyük olmalıdır."
                else -> {
                    saving = true
                    error = null
                    scope.launch {
                        runCatching {
                            InventoryRepo.repository.recordIssue(
                                InvRecordIssueRequest(
                                    nomenclatureId = nomenclatureId!!,
                                    quantity = qty,
                                    notes = destination.trim().takeIf { it.isNotEmpty() },
                                ),
                            )
                        }
                            .onSuccess { onSaved() }
                            .onFailure { error = it.toUserMessage() }
                        saving = false
                    }
                }
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
        SearchableSelect(
            label = "Material",
            required = true,
            selectedLabel = nomenclatureLabel,
            searchPlaceholder = "Ad və ya kod ilə axtar…",
            onLoad = { query ->
                InventoryRepo.repository.nomenclaturePage(0, 50, query).items
                    .filter { it.isActive != false }
            },
            itemLabel = { item: InvNomenclatureListItem -> "${item.code ?: ""} — ${item.name ?: ""}" },
            onSelect = { item ->
                nomenclatureId = item.id
                nomenclatureLabel = "${item.code ?: ""} — ${item.name ?: ""}"
            },
            onClear = {
                nomenclatureId = null
                nomenclatureLabel = null
            },
        )
        Spacer(Modifier.height(10.dp))
        MtkNumberField(
            value = quantity,
            onValueChange = { quantity = it },
            label = "Miqdar",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = destination,
            onValueChange = { destination = it },
            label = "Təyinat / qeyd",
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(12.dp))
    }
}
