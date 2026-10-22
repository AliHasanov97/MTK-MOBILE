package az.mtk.app.ui.screens.contracts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.BillingPeriod
import az.mtk.app.data.api.ChargeLabels
import az.mtk.app.data.api.ChargeStatus
import az.mtk.app.data.api.ContractDates
import az.mtk.app.data.api.ContractDto
import az.mtk.app.data.api.ContractStatus
import az.mtk.app.data.api.ContractServiceDto
import az.mtk.app.data.api.ContractServiceRequest
import az.mtk.app.data.api.ContractSuspendRequest
import az.mtk.app.data.api.ContractTerminateRequest
import az.mtk.app.data.api.ContractUpdateRequest
import az.mtk.app.data.api.ContractsApi
import az.mtk.app.data.api.CustomerChargesApi
import az.mtk.app.data.api.CustomerPaymentCreateRequest
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.VendorChargeDto
import az.mtk.app.data.api.VendorChargesApi
import az.mtk.app.data.api.VendorPaymentCreateRequest
import az.mtk.app.data.api.Comparison
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ContractDetailViewModel(private val contractId: String) : ViewModel() {
    private val api = AppGraph.service(ContractsApi::class.java)
    private val vendorChargesApi = AppGraph.service(VendorChargesApi::class.java)
    private val customerChargesApi = AppGraph.service(CustomerChargesApi::class.java)

    data class UiState(
        val contract: ContractDto? = null,
        val charges: List<VendorChargeDto>? = null,
        val loading: Boolean = true,
        val error: String? = null,
        val busy: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun canManage(profile: UserProfile?): Boolean =
        profile != null && profile.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER)

    fun canPay(profile: UserProfile?): Boolean =
        profile != null && profile.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT)

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.contract == null, error = null)
            runCatching { loadAll() }
                .onSuccess { (contract, charges) ->
                    _state.value = _state.value.copy(
                        contract = contract,
                        charges = charges,
                        loading = false,
                        error = null,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.toUserMessage())
                }
        }
    }

    private suspend fun loadAll(): Pair<ContractDto, List<VendorChargeDto>> = coroutineScope {
        val contractDeferred = async { api.get(contractId).data ?: throw IllegalStateException("Müqavilə tapılmadı") }
        // Borclar müqavilə üzrə filtrlənir; tərəf məlum olanda düzgün endpoint seçilir.
        val chargesDeferred = async {
            val contract = contractDeferred.await()
            val filters = listOf(
                QueryFilter(columnName = "contractId", comparison = Comparison.EQUALS, value = contractId),
            )
            if (contract.customerId != null) {
                customerChargesApi.search(
                    SearchBody(filters = filters, pageSize = 50),
                ).data?.items?.map { c ->
                    VendorChargeDto(
                        id = c.id,
                        contractId = c.contractId,
                        vendorId = c.customerId,
                        contractServiceId = c.contractServiceId,
                        period = c.period,
                        description = c.description,
                        amount = c.amount,
                        paidAmount = c.paidAmount,
                        outstandingAmount = c.outstandingAmount,
                        status = c.status,
                        chargeDate = c.chargeDate,
                        dueDate = c.dueDate,
                        isOverdue = c.isOverdue,
                        createdAt = c.createdAt,
                    )
                }.orEmpty()
            } else {
                vendorChargesApi.search(SearchBody(filters = filters, pageSize = 50)).data?.items.orEmpty()
            }
        }
        contractDeferred.await() to chargesDeferred.await()
    }

    private fun run(action: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { action() }
                .onFailure { _state.value = _state.value.copy(error = it.toUserMessage()) }
            _state.value = _state.value.copy(busy = false)
            reload()
        }
    }

    fun updateDates(start: LocalDate, end: LocalDate, note: String?) = run {
        api.update(
            contractId,
            ContractUpdateRequest(
                startDate = ContractDates.toUtcIso(start.toString()),
                endDate = ContractDates.toUtcIso(end.toString()),
                note = note,
            ),
        )
    }

    fun activate() = run { api.activate(contractId) }

    fun suspendContract(note: String?) = run { api.suspend(contractId, ContractSuspendRequest(note)) }

    fun terminate(date: LocalDate, note: String?) = run {
        api.terminate(contractId, ContractTerminateRequest(terminatedOn = ContractDates.toUtcIso(date.toString()), note = note))
    }

    fun delete(onDeleted: () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { api.delete(contractId) }
                .onSuccess { onDeleted() }
                .onFailure { _state.value = _state.value.copy(error = it.toUserMessage()) }
            _state.value = _state.value.copy(busy = false)
        }
    }

    fun saveService(existing: ContractServiceDto?, request: ContractServiceRequest) = run {
        if (existing == null) api.addService(contractId, request)
        else api.updateService(contractId, existing.id, request)
    }

    fun removeService(serviceId: String) = run { api.removeService(contractId, serviceId) }

    fun toggleService(service: ContractServiceDto) = run {
        api.setServiceStatus(contractId, service.id, az.mtk.app.data.api.ContractServiceStatusRequest(service.isActive != true))
    }

    fun payCharge(charge: VendorChargeDto, amount: Double, method: Int, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val contract = _state.value.contract
                if (contract?.customerId != null) {
                    customerChargesApi.createPayment(
                        CustomerPaymentCreateRequest(customerId = charge.vendorId, amount = amount, paymentMethod = method, notes = null),
                    )
                } else {
                    vendorChargesApi.createPayment(
                        VendorPaymentCreateRequest(vendorId = charge.vendorId, amount = amount, paymentMethod = method, notes = null),
                    )
                }
            }.onSuccess { onDone() }
                .onFailure { onError(it.toUserMessage()) }
        }
    }

    fun cancelCharge(charge: VendorChargeDto) = run {
        val contract = _state.value.contract
        if (contract?.customerId != null) customerChargesApi.cancelCharge(charge.id, emptyMap())
        else vendorChargesApi.cancel(charge.id, emptyMap())
    }
}

@Composable
fun ContractDetailScreen(
    contractId: String,
    onBack: (() -> Unit)?,
    onDeleted: (() -> Unit)? = null,
    profile: UserProfile? = null,
    viewModel: ContractDetailViewModel = mtkViewModel { ContractDetailViewModel(contractId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var datesOpen by remember { mutableStateOf(false) }
    var terminateOpen by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var serviceForm by remember { mutableStateOf<ContractServiceDto?>(null) }
    var serviceAddOpen by remember { mutableStateOf(false) }
    var payCharge by remember { mutableStateOf<VendorChargeDto?>(null) }
    var cancelCharge by remember { mutableStateOf<VendorChargeDto?>(null) }
    var suspendConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(contractId) { viewModel.reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.contract?.number ?: "Müqavilə",
            subtitle = state.contract?.let { "Müqavilə · ${ContractStatus.label(it.status)}" } ?: "Yüklənir…",
            onBack = onBack,
        )

        when {
            state.loading -> LoadingState("Müqavilə yüklənir…")
            state.error != null && state.contract == null -> Column(Modifier.padding(16.dp)) {
                ErrorState(state.error!!, onRetry = viewModel::reload)
            }
            state.contract != null -> {
                val contract = state.contract!!
                val isDraft = contract.status == az.mtk.app.data.api.ContractStatus.DRAFT
                val canManage = viewModel.canManage(profile)
                val canPay = viewModel.canPay(profile)
                val customerParty = contract.customerId != null

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 14.dp, 16.dp, 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.error != null) {
                        item { Text(state.error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall) }
                    }

                    // ---- Status əməliyyatları ----
                    if (canManage) {
                        item {
                            MtkCard(padding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = "Müddəti dəyiş",
                                        onClick = { datesOpen = true },
                                        enabled = !state.busy && contract.status != az.mtk.app.data.api.ContractStatus.TERMINATED,
                                    )
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = if (contract.status == az.mtk.app.data.api.ContractStatus.ACTIVE) "Dayandır" else "Aktivləşdir",
                                        onClick = {
                                            if (contract.status == az.mtk.app.data.api.ContractStatus.ACTIVE) suspendConfirm = true
                                            else viewModel.activate()
                                        },
                                        enabled = !state.busy,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = "Ləğv et",
                                        onClick = { terminateOpen = true },
                                        enabled = !state.busy && contract.status != az.mtk.app.data.api.ContractStatus.TERMINATED,
                                    )
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = "Sil",
                                        onClick = { deleteConfirm = true },
                                        enabled = !state.busy && contract.status != az.mtk.app.data.api.ContractStatus.ACTIVE,
                                    )
                                }
                            }
                        }
                    }

                    // ---- Ümumi məlumat ----
                    item {
                        MtkCard {
                            Text(
                                "Ümumi məlumat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            InfoRow(
                                if (customerParty) "Müştəri" else "Tədarükçü",
                                contract.vendorName ?: contract.customerName ?: "Tərəf tapılmadı",
                            )
                            InfoRow(
                                "Müddət",
                                "${Format.date(contract.startDate)} — ${Format.date(contract.endDate)}",
                            )
                            InfoRow("Status", ContractStatus.label(contract.status))
                            if (contract.isExpired == true && contract.status == az.mtk.app.data.api.ContractStatus.ACTIVE) {
                                InfoRow("Diqqət", "Müddəti bitib")
                            }
                            InfoRow("Qeyd", contract.note ?: "—")
                            InfoRow("Yaradılıb", Format.date(contract.createdAt))
                        }
                    }

                    // ---- Maliyyə xülasəsi ----
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(
                                label = "Aylıq yük",
                                value = Format.money(contract.monthlyAmount),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                label = "Dövr üzrə cəm",
                                value = Format.money(contract.totalAmount),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ---- Xidmətlər ----
                    item {
                        SectionHeading(
                            title = "Müqavilə üzrə xidmətlər",
                            subtitle = if (!isDraft) "Xidmətlərin qiyməti yalnız «Hazırlanır» mərhələsində dəyişdirilir." else null,
                        )
                    }
                    val services = contract.services.orEmpty()
                    if (services.isEmpty()) {
                        item {
                            Text(
                                "Hələ xidmət yoxdur. Müqavilə aktivləşməzdən əvvəl ən azı bir xidmət əlavə edilməlidir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                    } else {
                        items(services.size) { index ->
                            val service = services[index]
                            MtkCard(padding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            service.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (!service.description.isNullOrBlank()) {
                                            Text(
                                                service.description!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MtkColors.Muted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            BillingPeriod.label(service.billingPeriod) +
                                                (service.paymentTermDays?.let { " · ödəniş $it gün" } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Pill(
                                            if (service.isActive == true) "Aktiv" else "Dayandırılıb",
                                            if (service.isActive == true) Tone.Success else Tone.Neutral,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (service.billingPeriod == BillingPeriod.ONE_TIME && (service.periodAmount ?: 0.0) <= 0.0) {
                                                "Təyin olunmayıb"
                                            } else {
                                                Format.money(service.periodAmount)
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                if (canManage) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (isDraft) {
                                            az.mtk.app.ui.components.SecondaryButton(
                                                text = "Redaktə",
                                                onClick = { serviceForm = service },
                                                enabled = !state.busy,
                                            )
                                            az.mtk.app.ui.components.SecondaryButton(
                                                text = "Sil",
                                                onClick = { viewModel.removeService(service.id) },
                                                enabled = !state.busy,
                                            )
                                        }
                                        az.mtk.app.ui.components.SecondaryButton(
                                            text = if (service.isActive == true) "Dayandır" else "Bərpa et",
                                            onClick = { viewModel.toggleService(service) },
                                            enabled = !state.busy,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (canManage && isDraft) {
                        item {
                            PrimaryButton(text = "+ Xidmət əlavə et", onClick = { serviceAddOpen = true })
                        }
                    }

                    // ---- Borclar ----
                    item {
                        SectionHeading(
                            title = if (customerParty) "Müştəri borcları" else "Tədarükçü borcları",
                            subtitle = "Müqavilə üzrə xidmət borcları avtomatik yaradılır.",
                        )
                    }
                    when {
                        state.charges == null -> item { LoadingState("Borclar yüklənir…") }
                        state.charges!!.isEmpty() -> item { Text(
                            "Bu müqavilə üzrə hələ borc yaranmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        ) }
                        else -> items(state.charges!!.size) { index ->
                            val charge = state.charges!![index]
                            val status = charge.status
                            val open = status == ChargeStatus.UNPAID || status == ChargeStatus.PARTIALLY_PAID
                            MtkCard(padding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            charge.description ?: "Xidmət borcu",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (charge.period != null) Format.period(charge.period) else "—",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                        Text(
                                            "Son tarix: " + (charge.dueDate?.let { Format.date(it) } ?: "—"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (charge.isOverdue == true) MtkColors.Negative else MtkColors.MutedSoft,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Pill(ChargeLabels.label(status), chargeTone(status))
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Qalıq: ${Format.money(charge.outstandingAmount)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (charge.outstandingAmount > 0.005) MtkColors.Negative else MtkColors.Positive,
                                        )
                                        Text(
                                            "Ödənilib: ${Format.money(charge.paidAmount)} / ${Format.money(charge.amount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                }
                                if ((canPay && open) || (canManage && status != ChargeStatus.PAID && status != ChargeStatus.CANCELLED)) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (canPay && open) {
                                            PrimaryButton(
                                                text = "Ödə",
                                                onClick = { payCharge = charge },
                                                enabled = !state.busy,
                                            )
                                        }
                                        if (canManage && status != ChargeStatus.PAID && status != ChargeStatus.CANCELLED) {
                                            az.mtk.app.ui.components.SecondaryButton(
                                                text = "Ləğv et",
                                                onClick = { cancelCharge = charge },
                                                enabled = !state.busy,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- PDF export ----
                    item {
                        MtkCard(padding = 12.dp) {
                            Text(
                                "Sənədlər",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            ExportActionRow(
                                title = "Müqavilə PDF",
                                subtitle = "Müqavilənin imzalanmaq üçün çap forması",
                                path = "api/payments/contracts/$contractId/export",
                                scope = scope,
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Modallar ----
    if (datesOpen && state.contract != null) {
        DatesModal(
            contract = state.contract!!,
            busy = state.busy,
            onDismiss = { datesOpen = false },
            onSave = { start, end, note ->
                viewModel.updateDates(start, end, note)
                datesOpen = false
            },
        )
    }
    if (terminateOpen && state.contract != null) {
        TerminateModal(
            contract = state.contract!!,
            busy = state.busy,
            onDismiss = { terminateOpen = false },
            onSave = { date, note ->
                viewModel.terminate(date, note)
                terminateOpen = false
            },
        )
    }
    if (deleteConfirm) {
        ConfirmDialog(
            title = "Müqaviləni sil",
            message = "${state.contract?.number ?: ""} müqaviləsi silinsin? Bu əməliyyat geri qaytarıla bilməz.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteConfirm = false },
            onConfirm = {
                deleteConfirm = false
                viewModel.delete { onDeleted?.invoke() }
            },
        )
    }
    if (suspendConfirm) {
        ConfirmDialog(
            title = "Müqaviləni dayandır",
            message = "Dayandırılmış müqavilə üzrə yeni borc yaranmır. Davam edilsin?",
            onDismiss = { suspendConfirm = false },
            onConfirm = {
                suspendConfirm = false
                viewModel.suspendContract(null)
            },
        )
    }
    if (serviceAddOpen) {
        ServiceFormModal(
            existing = null,
            busy = state.busy,
            onDismiss = { serviceAddOpen = false },
            onSave = { request ->
                viewModel.saveService(null, request)
                serviceAddOpen = false
            },
        )
    }
    serviceForm?.let { existing ->
        ServiceFormModal(
            existing = existing,
            busy = state.busy,
            onDismiss = { serviceForm = null },
            onSave = { request ->
                viewModel.saveService(existing, request)
                serviceForm = null
            },
        )
    }
    payCharge?.let { charge ->
        PayChargeModal(
            charge = charge,
            customer = state.contract?.customerId != null,
            onDismiss = { payCharge = null },
            onPay = { amount, method ->
                viewModel.payCharge(
                    charge, amount, method,
                    onDone = { payCharge = null },
                    onError = { /* xəta modaldakı səhvdə göstərilir */ },
                )
            },
        )
    }
    cancelCharge?.let { charge ->
        ConfirmDialog(
            title = "Borcu ləğv et",
            message = "\"${charge.description ?: "Xidmət borcu"}\" borcu ləğv edilsin?",
            confirmLabel = "Ləğv et",
            danger = true,
            onDismiss = { cancelCharge = null },
            onConfirm = {
                cancelCharge = null
                viewModel.cancelCharge(charge)
            },
        )
    }
}

internal fun chargeTone(status: Int?): Tone = when (status) {
    ChargeStatus.UNPAID -> Tone.Danger
    ChargeStatus.PARTIALLY_PAID -> Tone.Warning
    ChargeStatus.PAID -> Tone.Success
    ChargeStatus.CANCELLED -> Tone.Neutral
    else -> Tone.Neutral
}

@Composable
private fun DatesModal(
    contract: ContractDto,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate, String?) -> Unit,
) {
    var start by remember { mutableStateOf(runCatching { LocalDate.parse(contract.startDate.take(10)) }.getOrDefault(LocalDate.now())) }
    var end by remember { mutableStateOf(runCatching { LocalDate.parse(contract.endDate.take(10)) }.getOrDefault(LocalDate.now().plusYears(1))) }
    var note by remember { mutableStateOf(contract.note ?: "") }

    MtkModal(
        title = "Müqavilənin müddəti və qeydi",
        onDismiss = onDismiss,
        confirmLoading = busy,
        onConfirm = { onSave(start, end, note.trim().ifBlank { null }) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkDateField(value = start, onValueChange = { start = it }, label = "Başlanğıc tarixi", required = true, modifier = Modifier.weight(1f))
            MtkDateField(value = end, onValueChange = { end = it }, label = "Bitmə tarixi", required = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = note, onValueChange = { note = it }, label = "Qeyd", singleLine = false, minLines = 3)
    }
}

@Composable
private fun TerminateModal(
    contract: ContractDto,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String?) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf(contract.note ?: "") }

    MtkModal(
        title = "Müqaviləni ləğv et",
        subtitle = "Ləğv edilən müqavilədən artıq borc yaranmır və bitmə tarixi aşağıdakı tarixlə əvəz olunur.",
        onDismiss = onDismiss,
        confirmLabel = "Ləğv et",
        confirmLoading = busy,
        onConfirm = { onSave(date, note.trim().ifBlank { null }) },
    ) {
        MtkDateField(value = date, onValueChange = { date = it }, label = "Ləğv tarixi", required = true)
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = note, onValueChange = { note = it }, label = "Səbəb / qeyd", singleLine = false, minLines = 3)
    }
}

@Composable
private fun ServiceFormModal(
    existing: ContractServiceDto?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (ContractServiceRequest) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var isOneTime by remember { mutableStateOf(existing?.billingPeriod == BillingPeriod.ONE_TIME) }
    var recurringPeriod by remember {
        mutableStateOf(if (existing == null || existing.billingPeriod == BillingPeriod.ONE_TIME) BillingPeriod.MONTHLY else existing.billingPeriod)
    }
    var unitPrice by remember { mutableStateOf(existing?.takeIf { (it.unitPrice ?: 0.0) > 0.0 }?.unitPrice?.toString() ?: "") }
    var paymentTermDays by remember { mutableStateOf(existing?.paymentTermDays?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var serviceStart by remember { mutableStateOf(existing?.serviceStartDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }) }
    var serviceEnd by remember { mutableStateOf(existing?.serviceEndDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = if (existing == null) "Yeni xidmət" else "Xidməti redaktə et",
        onDismiss = onDismiss,
        confirmLabel = if (existing == null) "Əlavə et" else "Saxla",
        confirmEnabled = name.isNotBlank(),
        confirmLoading = busy,
        onConfirm = {
            val price = unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (price < 0) {
                error = "Qiymət mənfi ola bilməz."
                return@MtkModal
            }
            if (!isOneTime && unitPrice.isBlank()) {
                error = "Davamlı xidmət üçün qiymət tələb olunur."
                return@MtkModal
            }
            onSave(
                ContractServiceRequest(
                    name = name.trim(),
                    unitPrice = price,
                    billingPeriod = if (isOneTime) BillingPeriod.ONE_TIME else recurringPeriod,
                    description = description.trim().ifBlank { null },
                    serviceStartDate = if (isOneTime) null else serviceStart?.let { ContractDates.toUtcIso(it.toString()) },
                    serviceEndDate = if (isOneTime) null else serviceEnd?.let { ContractDates.toUtcIso(it.toString()) },
                    paymentTermDays = paymentTermDays.toIntOrNull(),
                ),
            )
        },
    ) {
        MtkTextField(value = name, onValueChange = { name = it }, label = "Xidmətin adı", placeholder = "Liftə aylıq texniki xidmət", required = true)
        Spacer(Modifier.height(12.dp))
        MtkSelect(
            value = if (isOneTime) "onetime" else "recurring",
            onValueChange = { isOneTime = it == "onetime" },
            label = "Xidmət növü",
            options = listOf(
                MtkOption("recurring", "Davamlı (dövri) — aylıq/rüblük/illik haqq"),
                MtkOption("onetime", "Birdəfəlik — məs. təmir"),
            ),
        )
        Spacer(Modifier.height(12.dp))
        if (isOneTime) {
            MtkNumberField(
                value = unitPrice,
                onValueChange = { unitPrice = it },
                label = "Qiymət (₼, opsional)",
                placeholder = "Bilinmirsə boş buraxın",
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MtkNumberField(value = unitPrice, onValueChange = { unitPrice = it }, label = "Qiymət (₼)", required = true, modifier = Modifier.weight(1f))
                MtkSelect(
                    value = recurringPeriod.toString(),
                    onValueChange = { recurringPeriod = it.toInt() },
                    label = "Hesablaşma dövrü",
                    options = listOf(BillingPeriod.MONTHLY, BillingPeriod.QUARTERLY, BillingPeriod.YEARLY)
                        .map { MtkOption(it.toString(), BillingPeriod.label(it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MtkNumberField(value = paymentTermDays, onValueChange = { paymentTermDays = it }, label = "Ödəniş müddəti, gün (opsional)")
        if (!isOneTime) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MtkDateField(value = serviceStart, onValueChange = { serviceStart = it }, label = "Xidmətin başlanğıcı", modifier = Modifier.weight(1f))
                MtkDateField(value = serviceEnd, onValueChange = { serviceEnd = it }, label = "Xidmətin bitməsi", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tarixlər boş buraxılarsa müqavilənin müddəti tətbiq olunur.",
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
            )
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = description, onValueChange = { description = it }, label = "Təsvir")
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PayChargeModal(
    charge: VendorChargeDto,
    customer: Boolean,
    onDismiss: () -> Unit,
    onPay: (Double, Int) -> Unit,
) {
    var amount by remember { mutableStateOf(charge.outstandingAmount.toString()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = if (customer) "Müştəridən ödəniş" else "Tədarükçüyə ödəniş",
        subtitle = "${charge.description ?: "Xidmət borcu"} · qalıq borc ${Format.money(charge.outstandingAmount)}",
        onDismiss = onDismiss,
        confirmLabel = "Ödə",
        onConfirm = {
            val value = amount.replace(',', '.').toDoubleOrNull()
            if (value == null || value <= 0) {
                error = "Ödəniş məbləği müsbət olmalıdır."
                return@MtkModal
            }
            onPay(value, method)
        },
    ) {
        MtkNumberField(value = amount, onValueChange = { amount = it }, label = "Məbləğ (₼)", required = true)
        Spacer(Modifier.height(12.dp))
        MtkSelect(
            value = method.toString(),
            onValueChange = { method = it.toInt() },
            label = "Ödəniş üsulu",
            options = PaymentMethod.selectable.map { MtkOption(it.toString(), PaymentMethod.label(it)) },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (customer) "Ödəniş qeydə alınan kimi tranzaksiyalar jurnalına gəlir kimi düşür."
            else "Ödəniş qeydə alınan kimi tranzaksiyalar jurnalına xərc kimi düşür.",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}


