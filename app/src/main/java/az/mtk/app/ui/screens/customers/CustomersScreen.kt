package az.mtk.app.ui.screens.customers

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.CustomerDetailDto
import az.mtk.app.data.api.CustomerDto
import az.mtk.app.data.api.CustomerSaveRequest
import az.mtk.app.data.api.CustomerStatusRequest
import az.mtk.app.data.api.CustomerType
import az.mtk.app.data.api.CustomersApi
import az.mtk.app.data.api.Page
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
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

class CustomersViewModel : ViewModel() {
    private val api = AppGraph.service(CustomersApi::class.java)

    val pager = Pager<CustomerDto>(viewModelScope) { page, query ->
        val result = api.search(
            SearchBody(
                sortCriteria = SortCriteria(columnName = "CreatedAt", direction = Sort.DESCENDING),
                searchTerm = query.ifBlank { null },
                page = page,
                pageSize = 20,
            ),
        ).data ?: throw IllegalStateException("Məlumat alınmadı")
        Page(result.customers.orEmpty(), result.totalCount, result.page ?: page, result.pageSize ?: 20)
    }

    fun canManage(profile: UserProfile?): Boolean =
        profile != null && profile.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER)

    suspend fun getCustomer(id: String): CustomerDetailDto =
        api.get(id).data ?: throw IllegalStateException("Müştəri tapılmadı")

    private val _editTarget = MutableStateFlow<CustomerDetailDto?>(null)
    val editTarget = _editTarget.asStateFlow()

    fun startEdit(customer: CustomerDto) {
        viewModelScope.launch {
            runCatching { getCustomer(customer.id) }
                .onSuccess { _editTarget.value = it }
                .onFailure { }
        }
    }

    fun closeEdit() {
        _editTarget.value = null
    }

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()

    fun save(existing: CustomerDetailDto?, request: CustomerSaveRequest, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                if (existing == null) api.create(request) else api.update(existing.id, request)
            }.onSuccess { onDone() }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun toggleStatus(customer: CustomerDto) {
        viewModelScope.launch {
            runCatching { api.setStatus(customer.id, CustomerStatusRequest(customer.isActive != true)) }
                .onFailure { _actionError.value = it.toUserMessage() }
            pager.refresh()
        }
    }

    fun delete(customer: CustomerDto) {
        viewModelScope.launch {
            runCatching { api.delete(customer.id) }
                .onFailure { _actionError.value = it.toUserMessage() }
            pager.refresh()
        }
    }
}

@Composable
fun CustomersScreen(
    onBack: (() -> Unit)?,
    onOpenDetail: (String) -> Unit,
    showTopBar: Boolean = false,
    profile: UserProfile? = null,
    viewModel: CustomersViewModel = mtkViewModel { CustomersViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val editTarget by viewModel.editTarget.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var editingError by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomerDto?>(null) }
    val canManage = viewModel.canManage(profile)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Müştərilər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} müştəri",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (ad, VÖEN, rəhbər, e-poçt…)",
            )
            actionError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Müştərilər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Müştəri tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                items(state.items, key = { it.id }) { customer ->
                    MtkCard(onClick = { onOpenDetail(customer.id) }, modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    customer.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    CustomerType.label(customer.customerType) + (customer.director?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOfNotNull(customer.phone, customer.email).joinToString(" · ").ifBlank { "—" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Pill(
                                if (customer.isActive == true) "Aktiv" else "Dayandırılıb",
                                if (customer.isActive == true) Tone.Success else Tone.Neutral,
                            )
                        }
                        if (!customer.voen.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "VÖEN: ${customer.voen}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MtkColors.MutedSoft,
                            )
                        }
                        if (canManage) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                az.mtk.app.ui.components.SecondaryButton(
                                    text = "Redaktə",
                                    onClick = {
                                        editingError = null
                                        viewModel.startEdit(customer)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                az.mtk.app.ui.components.SecondaryButton(
                                    text = if (customer.isActive == true) "Dayandır" else "Aktivləşdir",
                                    onClick = { viewModel.toggleStatus(customer) },
                                    modifier = Modifier.weight(1f),
                                )
                                az.mtk.app.ui.components.SecondaryButton(
                                    text = "Sil",
                                    onClick = { deleteTarget = customer },
                                    modifier = Modifier.weight(1f),
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

    editTarget?.let { editing ->
        CustomerFormModal(
            existing = editing,
            error = editingError,
            onDismiss = { viewModel.closeEdit() },
            onSave = { existing, request ->
                viewModel.save(
                    existing, request,
                    onDone = { viewModel.closeEdit() },
                    onError = { editingError = it },
                )
            },
        )
    }
    deleteTarget?.let { customer ->
        ConfirmDialog(
            title = "Müştərini sil",
            message = "${customer.name} silinsin? Bu əməliyyat geri qaytarıla bilməz.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                viewModel.delete(customer)
            },
        )
    }
}

@Composable
internal fun CustomerFormModal(
    existing: CustomerDetailDto?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (CustomerDetailDto?, CustomerSaveRequest) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.customerType ?: CustomerType.LEGAL_ENTITY) }
    var voen by remember { mutableStateOf(existing?.voen ?: "") }
    var director by remember { mutableStateOf(existing?.director ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    MtkModal(
        title = if (existing == null) "Yeni müştəri" else "Müştərini redaktə et",
        onDismiss = onDismiss,
        confirmLabel = if (existing == null) "Yarat" else "Saxla",
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
            onSave(
                existing,
                CustomerSaveRequest(
                    name = name.trim(),
                    customerType = type,
                    voen = voen.trim().ifBlank { null },
                    director = director.trim().ifBlank { null },
                    email = email.trim().ifBlank { null },
                    phone = phone.trim().ifBlank { null },
                    address = address.trim().ifBlank { null },
                    note = note.trim().ifBlank { null },
                ),
            )
        },
    ) {
        MtkTextField(value = name, onValueChange = { name = it }, label = "Ad", required = true)
        Spacer(Modifier.height(12.dp))
        MtkSelect(
            value = type.toString(),
            onValueChange = { type = it.toInt() },
            label = "Tip",
            options = listOf(CustomerType.LEGAL_ENTITY, CustomerType.INDIVIDUAL, CustomerType.SOLE_PROPRIETOR)
                .map { MtkOption(it.toString(), CustomerType.label(it)) },
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkTextField(value = voen, onValueChange = { voen = it }, label = "VÖEN", modifier = Modifier.weight(1f))
            MtkTextField(value = director, onValueChange = { director = it }, label = "Rəhbər / təmsilçi", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkTextField(value = phone, onValueChange = { phone = it }, label = "Telefon", modifier = Modifier.weight(1f))
            MtkTextField(value = email, onValueChange = { email = it }, label = "E-poçt", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = address, onValueChange = { address = it }, label = "Ünvan")
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = note, onValueChange = { note = it }, label = "Qeyd", singleLine = false, minLines = 3)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

