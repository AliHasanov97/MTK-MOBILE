package az.mtk.app.ui.screens.contracts

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
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ContractDates
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.ContractStatus
import az.mtk.app.data.api.ContractsApi
import az.mtk.app.data.api.ContractCreateRequest
import az.mtk.app.data.api.CustomerDto
import az.mtk.app.data.api.CustomersApi
import az.mtk.app.data.api.Page
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.data.api.VendorDto
import az.mtk.app.data.api.VendorsApi
import az.mtk.app.data.api.Comparison
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchableSelect
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
import java.time.LocalDate

private val contractPartyFilters = listOf(
    "Hamısı" to null,
    "Tədarükçü" to "vendorId",
    "Müştəri" to "customerId",
)

private fun statusTone(status: Int?): Tone = when (status) {
    ContractStatus.ACTIVE -> Tone.Success
    ContractStatus.DRAFT -> Tone.Brand
    ContractStatus.SUSPENDED -> Tone.Warning
    ContractStatus.TERMINATED -> Tone.Danger
    else -> Tone.Neutral
}

class ContractsViewModel : ViewModel() {
    private val api = AppGraph.service(ContractsApi::class.java)
    private val vendorsApi = AppGraph.service(VendorsApi::class.java)
    private val customersApi = AppGraph.service(CustomersApi::class.java)

    private val partyFilter = MutableStateFlow<String?>(null)
    val filter = partyFilter.asStateFlow()

    val pager = Pager<ContractListItemDto>(viewModelScope) { page, query ->
        val filters = partyFilter.value?.let { column ->
            listOf(QueryFilter(columnName = column, comparison = Comparison.IS_NOT_NULL, value = null))
        }
        val result = api.search(
            SearchBody(
                filters = filters,
                sortCriteria = SortCriteria(columnName = "CreatedAt", direction = Sort.DESCENDING),
                searchTerm = query.ifBlank { null },
                page = page,
                pageSize = 20,
            ),
        ).data ?: throw IllegalStateException("Məlumat alınmadı")
        Page(result.contracts.orEmpty(), result.totalCount, result.page ?: page, result.pageSize ?: 20)
    }

    fun selectFilter(column: String?) {
        if (column == partyFilter.value) return
        partyFilter.value = column
        pager.refresh()
    }

    suspend fun loadVendors(): List<VendorDto> =
        vendorsApi.search(SearchBody(page = 0, pageSize = 200)).data?.vendors
            ?.filter { it.isActive != false }.orEmpty()

    suspend fun loadCustomers(): List<CustomerDto> =
        customersApi.search(SearchBody(page = 0, pageSize = 200)).data?.customers
            ?.filter { it.isActive != false }.orEmpty()

    fun createContract(
        number: String,
        vendorId: String?,
        customerId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        note: String?,
        onDone: (String?) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                api.create(
                    ContractCreateRequest(
                        number = number,
                        vendorId = vendorId,
                        customerId = customerId,
                        startDate = ContractDates.toUtcIso(startDate.toString()),
                        endDate = ContractDates.toUtcIso(endDate.toString()),
                        note = note,
                    ),
                )
            }.onSuccess { onDone(it.data) }
                .onFailure { onError(it.toUserMessage()) }
        }
    }
}

@Composable
fun ContractsScreen(
    onBack: (() -> Unit)?,
    onOpenDetail: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: ContractsViewModel = mtkViewModel { ContractsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.filter.collectAsStateWithLifecycle()
    var createOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Müqavilələr",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} müqavilə",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (müqavilə nömrəsi, qeyd…)",
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                contractPartyFilters.forEach { (label, column) ->
                    FilterChip(
                        selected = selectedFilter == column,
                        onClick = { viewModel.selectFilter(column) },
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
            state.loading && state.items.isEmpty() -> LoadingState("Müqavilələr yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Müqavilə tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { contract ->
                    MtkCard(onClick = { onOpenDetail(contract.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    contract.number,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    contract.vendorName ?: contract.customerName ?: "Tərəf tapılmadı",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Pill(ContractStatus.label(contract.status), statusTone(contract.status))
                                if (contract.isExpired == true && contract.status == ContractStatus.ACTIVE) {
                                    Spacer(Modifier.height(4.dp))
                                    Pill("Müddəti bitib", Tone.Warning, showDot = false)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                if (contract.customerId != null) "Müştəri müqaviləsi" else "Tədarükçü müqaviləsi",
                                style = MaterialTheme.typography.labelMedium,
                                color = MtkColors.MutedSoft,
                            )
                            Text(
                                "${Format.date(contract.startDate)} — ${Format.date(contract.endDate)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MtkColors.MutedSoft,
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

    if (createOpen) {
        CreateContractModal(
            viewModel = viewModel,
            onDismiss = { createOpen = false },
            onSaved = { id ->
                createOpen = false
                // Yeni müqavilə "Hazırlanır" statusunda yaranır — xidmətləri əlavə
                // etmək üçün dərhal detal səhifəsinə keçirik.
                id?.let(onOpenDetail) ?: viewModel.pager.refresh()
            },
        )
    }
}

@Composable
private fun CreateContractModal(
    viewModel: ContractsViewModel,
    onDismiss: () -> Unit,
    onSaved: (String?) -> Unit,
) {
    var partyKind by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("vendor") }
    var vendorId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var vendorLabel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var customerId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var customerLabel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var number by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var startDate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(LocalDate.now()) }
    var endDate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(LocalDate.now().plusYears(1)) }
    var note by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var saving by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    MtkModal(
        title = "Yeni müqavilə",
        subtitle = "Müqavilə «Hazırlanır» statusunda yaranır — xidmətləri əlavə edib aktivləşdirə bilərsiniz.",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = number.isNotBlank(),
        confirmLoading = saving,
        onConfirm = {
            if (partyKind == "vendor" && vendorId == null) {
                error = "Tədarükçü seçin."
                return@MtkModal
            }
            if (partyKind == "customer" && customerId == null) {
                error = "Müştəri seçin."
                return@MtkModal
            }
            saving = true
            error = null
            viewModel.createContract(
                number = number.trim(),
                vendorId = if (partyKind == "vendor") vendorId else null,
                customerId = if (partyKind == "customer") customerId else null,
                startDate = startDate,
                endDate = endDate,
                note = note.trim().ifBlank { null },
                onDone = onSaved,
                onError = {
                    error = it
                    saving = false
                },
            )
        },
    ) {
        MtkTextField(
            value = number,
            onValueChange = { number = it },
            label = "Müqavilə nömrəsi",
            placeholder = "2026/045",
            required = true,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = partyKind == "vendor",
                onClick = { partyKind = "vendor" },
                label = { Text("Tədarükçü", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MtkColors.Green,
                    selectedLabelColor = Color.White,
                ),
            )
            FilterChip(
                selected = partyKind == "customer",
                onClick = { partyKind = "customer" },
                label = { Text("Müştəri", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MtkColors.Green,
                    selectedLabelColor = Color.White,
                ),
            )
        }
        if (partyKind == "vendor") {
            SearchableSelect(
                label = "Tədarükçü",
                required = true,
                selectedLabel = vendorLabel,
                searchPlaceholder = "Tədarükçü axtar…",
                onLoad = { q -> viewModel.loadVendors().filter { it.name.contains(q, ignoreCase = true) } },
                itemLabel = { it.name },
                onSelect = { vendorId = it.id; vendorLabel = it.name },
                onClear = { vendorId = null; vendorLabel = null },
            )
        } else {
            SearchableSelect(
                label = "Müştəri",
                required = true,
                selectedLabel = customerLabel,
                searchPlaceholder = "Müştəri axtar…",
                onLoad = { q -> viewModel.loadCustomers().filter { it.name.contains(q, ignoreCase = true) } },
                itemLabel = { it.name },
                onSelect = { customerId = it.id; customerLabel = it.name },
                onClear = { customerId = null; customerLabel = null },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkDateField(
                value = startDate,
                onValueChange = { startDate = it },
                label = "Başlanğıc tarixi",
                required = true,
                modifier = Modifier.weight(1f),
            )
            MtkDateField(
                value = endDate,
                onValueChange = { endDate = it },
                label = "Bitmə tarixi",
                required = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(
            value = note,
            onValueChange = { note = it },
            label = "Qeyd",
            singleLine = false,
            minLines = 3,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

