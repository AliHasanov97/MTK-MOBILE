package az.mtk.app.ui.screens.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import az.mtk.app.ui.components.AppSheetDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.BillingPeriod
import az.mtk.app.data.api.ContractDto
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.ContractParty
import az.mtk.app.data.api.ContractServiceDto
import az.mtk.app.data.api.ContractStatus
import az.mtk.app.data.api.PartyChargeRow
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.util.Format
import java.time.LocalDate

/* ====================================================================== */
/* Siyahı                                                                 */
/* ====================================================================== */

@Composable
internal fun ContractsTab(
    state: ContractsViewModel.UiState,
    viewModel: ContractsViewModel,
    onOpenContract: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OpsSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Müqavilə nömrəsi, tərəf və ya qeyd…",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        OpsPillRow(
            pills = ContractParty.entries.map { party ->
                OpsPill(
                    label = party.label,
                    selected = state.party == party,
                    onClick = { viewModel.setParty(party) },
                )
            },
        )
        Spacer(Modifier.height(8.dp))

        state.message?.let { OpsFeedback(it, isError = false) { viewModel.clearFeedback() } }
        state.actionError?.let { OpsFeedback(it, isError = true) { viewModel.clearFeedback() } }
        state.formError?.let { OpsFeedback(it, isError = true) { viewModel.clearFormError() } }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.total} müqavilə",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
            if (state.canManage) {
                OpsPrimaryButton(
                    text = "+ Yeni müqavilə",
                    onClick = {
                        viewModel.loadOptions()
                        showCreate = true
                    },
                )
            }
        }

        when {
            state.loading && state.list.isEmpty() -> OpsLoading()
            state.error != null && state.list.isEmpty() ->
                OpsError(state.error!!) { viewModel.load() }
            state.list.isEmpty() -> OpsEmpty(
                title = "Müqavilə tapılmadı",
                text = "Filtrləri dəyişin və ya yeni müqavilə yaradın.",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.list, key = { it.id }) { item ->
                    ContractCard(item) { onOpenContract(item.id) }
                }
                item {
                    OpsPagination(
                        page = state.page,
                        total = state.total,
                        onPrev = viewModel::prevPage,
                        onNext = viewModel::nextPage,
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateContractSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                showCreate = false
                viewModel.clearFormError()
            },
            onCreated = { id ->
                showCreate = false
                onOpenContract(id)
            },
        )
    }
}

@Composable
private fun ContractCard(item: ContractListItemDto, onClick: () -> Unit) {
    OpsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.number,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            ContractStatusChip(item.status, item.isExpired == true)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.vendorName ?: item.customerName ?: "Tərəf tapılmadı",
            fontSize = 13.sp,
            color = OpsColors.Ink,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (item.customerId != null) "Müştəri müqaviləsi" else "Tədarükçü müqaviləsi",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${Format.date(item.startDate)} — ${Format.date(item.endDate)}",
                fontSize = 11.sp,
                color = OpsColors.Muted,
            )
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = OpsColors.MutedSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun ContractStatusChip(status: Int, expired: Boolean = false) {
    val (label, fg, bg) = when (status) {
        ContractStatus.DRAFT -> Triple("Hazırlanır", OpsColors.MutedSoft, OpsColors.Subtle)
        ContractStatus.ACTIVE -> Triple("Qüvvədədir", OpsColors.Olive, OpsColors.OliveSoft)
        ContractStatus.SUSPENDED -> Triple("Dayandırılıb", OpsColors.Amber, OpsColors.AmberSoft)
        ContractStatus.TERMINATED -> Triple("Ləğv edilib", OpsColors.Error, OpsColors.ErrorSoft)
        else -> Triple("—", OpsColors.MutedSoft, OpsColors.Subtle)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OpsChip(label, fg, bg)
        if (expired && status == ContractStatus.ACTIVE) {
            OpsChip("Müddəti bitib", OpsColors.Amber, OpsColors.AmberSoft)
        }
    }
}

@Composable
private fun CreateContractSheet(
    state: ContractsViewModel.UiState,
    viewModel: ContractsViewModel,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
) {
    var party by remember { mutableStateOf(ContractParty.VENDOR) }
    var partyId by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusYears(1).toString()) }
    var note by remember { mutableStateOf("") }

    val options = if (party == ContractParty.VENDOR) {
        state.vendorOptions.map { it.id to it.name }
    } else {
        state.customerOptions.map { it.id to it.name }
    }

    OpsFormSheet(title = "Yeni müqavilə", onDismiss = onDismiss) {
        if (state.optionsError != null) {
            Text(state.optionsError, fontSize = 12.sp, color = OpsColors.Error)
            Spacer(Modifier.height(8.dp))
        }
        OpsTextField(number, { number = it }, "Müqavilə nömrəsi", placeholder = "2026/045")
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = "Müqavilə tərəfi",
            options = ContractParty.entries.filter { it != ContractParty.ALL }.map { it.name to it.label },
            selectedId = party.name,
            onSelect = { id ->
                party = ContractParty.valueOf(id)
                partyId = ""
            },
        )
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = if (party == ContractParty.VENDOR) "Tədarükçü" else "Müştəri",
            options = options,
            selectedId = partyId.ifBlank { null },
            onSelect = { partyId = it },
            placeholder = if (state.optionsLoading) "Yüklənir…" else "Seçin…",
            enabled = !state.optionsLoading,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsDateField("Başlanğıc", startDate, { startDate = it }, modifier = Modifier.weight(1f))
            OpsDateField("Bitmə", endDate, { endDate = it }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        OpsTextField(note, { note = it }, "Qeyd", singleLine = false)
        state.formError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Yarat",
            onCancel = onDismiss,
            onSubmit = {
                viewModel.createContract(
                    number = number,
                    vendorId = if (party == ContractParty.VENDOR) partyId else null,
                    customerId = if (party == ContractParty.CUSTOMER) partyId else null,
                    startDate = startDate,
                    endDate = endDate,
                    note = note,
                    onCreated = onCreated,
                )
            },
            enabled = number.isNotBlank() && partyId.isNotBlank(),
        )
    }
}

/* ====================================================================== */
/* Detal                                                                  */
/* ====================================================================== */

@Composable
internal fun ContractDetailScreen(
    contractId: String,
    viewModel: ContractsViewModel,
    onBack: () -> Unit,
    onOpenContract: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(contractId) { viewModel.openContract(contractId) }

    var serviceForm by remember { mutableStateOf<ContractServiceDto?>(null) }
    var showServiceCreate by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<PartyChargeRow?>(null) }
    var showDates by remember { mutableStateOf(false) }
    var showTerminate by remember { mutableStateOf(false) }
    var showSuspend by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteServiceTarget by remember { mutableStateOf<ContractServiceDto?>(null) }
    var cancelTarget by remember { mutableStateOf<PartyChargeRow?>(null) }

    val contract = state.detail

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = contract?.number ?: "Müqavilə",
                subtitle = contract?.let { it.vendorName ?: it.customerName } ?: "Yüklənir…",
                onBack = onBack,
                onRefresh = viewModel::refreshDetail,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.message?.let { OpsFeedback(it, isError = false) { viewModel.clearFeedback() } }
            state.actionError?.let { OpsFeedback(it, isError = true) { viewModel.clearFeedback() } }

            when {
                state.detailLoading && contract == null -> OpsLoading()
                state.detailError != null && contract == null ->
                    OpsError(state.detailError!!) { viewModel.refreshDetail() }
                contract == null -> OpsLoading()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 6.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { ContractHero(contract) }

                    if (state.canManage) {
                        item {
                            ContractActions(
                                contract = contract,
                                busy = state.busy,
                                onDates = { showDates = true },
                                onActivate = { viewModel.activate() },
                                onSuspend = { showSuspend = true },
                                onTerminate = { showTerminate = true },
                                onDelete = { showDelete = true },
                            )
                        }
                    }

                    item {
                        OpsSectionTitle(
                            title = "Müqavilə üzrə xidmətlər",
                            actionLabel = if (state.canManage && contract.status == ContractStatus.DRAFT) {
                                "+ Xidmət"
                            } else {
                                null
                            },
                            onAction = { showServiceCreate = true },
                        )
                    }

                    val services = contract.services.orEmpty()
                    if (services.isEmpty()) {
                        item {
                            OpsCard {
                                Text(
                                    "Hələ xidmət yoxdur. Müqavilə aktivləşməzdən əvvəl ən azı bir xidmət əlavə edilməlidir.",
                                    fontSize = 12.sp,
                                    color = OpsColors.MutedSoft,
                                )
                            }
                        }
                    } else {
                        items(services, key = { it.id }) { service ->
                            ServiceCard(
                                service = service,
                                canManage = state.canManage,
                                isDraft = contract.status == ContractStatus.DRAFT,
                                busy = state.busy,
                                onEdit = { serviceForm = service },
                                onToggle = { viewModel.toggleService(service.id, service.isActive != true) },
                                onDelete = { deleteServiceTarget = service },
                            )
                        }
                    }

                    item {
                        OpsSectionTitle(title = "Müqavilə borcları")
                    }

                    when {
                        state.chargesLoading && state.charges.isEmpty() -> item { OpsLoading() }
                        state.chargesError != null -> item {
                            Text(state.chargesError!!, fontSize = 12.sp, color = OpsColors.Error)
                        }
                        state.charges.isEmpty() -> item {
                            OpsCard {
                                Text(
                                    "Bu müqavilə üzrə hələ borc yaranmayıb. Dövr üzrə xidmət borcları avtomatik yaradılır.",
                                    fontSize = 12.sp,
                                    color = OpsColors.MutedSoft,
                                )
                            }
                        }
                        else -> items(state.charges, key = { it.id }) { charge ->
                            ContractChargeCard(
                                charge = charge,
                                canPay = state.canPay,
                                canManage = state.canManage,
                                busy = state.busy,
                                isCustomer = contract.customerId != null,
                                onPay = { payTarget = charge },
                                onCancel = { cancelTarget = charge },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showServiceCreate || serviceForm != null) {
        ServiceFormSheet(
            service = serviceForm,
            saving = state.saving,
            error = state.formError,
            onDismiss = {
                showServiceCreate = false
                serviceForm = null
                viewModel.clearFormError()
            },
            onSave = { name, price, period, description, start, end, term ->
                viewModel.saveService(
                    serviceId = serviceForm?.id,
                    name = name,
                    unitPrice = price,
                    billingPeriod = period,
                    description = description,
                    startDate = start,
                    endDate = end,
                    paymentTermDays = term,
                    onSaved = {
                        showServiceCreate = false
                        serviceForm = null
                    },
                )
            },
        )
    }

    payTarget?.let { charge ->
        PayChargeSheet(
            charge = charge,
            isCustomer = contract?.customerId != null,
            saving = state.busy,
            error = state.formError,
            onDismiss = {
                payTarget = null
                viewModel.clearFormError()
            },
            onPay = { amount, method, notes ->
                viewModel.payCharge(charge, amount, method, notes) { payTarget = null }
            },
        )
    }

    if (showDates && contract != null) {
        DatesDialog(
            contract = contract,
            saving = state.busy,
            onDismiss = { showDates = false },
            onSave = { start, end, note ->
                viewModel.updateDates(start, end, note) { showDates = false }
            },
        )
    }

    if (showTerminate) {
        TerminateDialog(
            saving = state.busy,
            onDismiss = { showTerminate = false },
            onConfirm = { date, note ->
                viewModel.terminate(date, note) { showTerminate = false }
            },
        )
    }

    if (showSuspend) {
        ConfirmDialog(
            title = "Müqavilə dayandırılsın?",
            text = "Dayandırılmış müqavilə üzrə yeni borc yaradılmır. Sonradan yenidən aktivləşdirə bilərsiniz.",
            confirmLabel = "Dayandır",
            onDismiss = { showSuspend = false },
            onConfirm = {
                showSuspend = false
                viewModel.suspend(null)
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Müqavilə silinsin?",
            text = "Bu əməliyyat geri qaytarılmır. Yalnız aktiv olmayan müqavilələr silinə bilər.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.delete { onBack() }
            },
        )
    }

    deleteServiceTarget?.let { service ->
        ConfirmDialog(
            title = "\"${service.name}\" silinsin?",
            text = "Xidmət müqavilədən çıxarılacaq.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteServiceTarget = null },
            onConfirm = {
                deleteServiceTarget = null
                viewModel.deleteService(service.id)
            },
        )
    }

    cancelTarget?.let { charge ->
        ConfirmDialog(
            title = "Borc ləğv edilsin?",
            text = "${charge.description ?: "Borc"} — ${Format.money(charge.amount)}. Ləğv edilmiş borc açıq borc sayılmır.",
            confirmLabel = "Ləğv et",
            danger = true,
            onDismiss = { cancelTarget = null },
            onConfirm = {
                cancelTarget = null
                viewModel.cancelCharge(charge)
            },
        )
    }
}

@Composable
private fun ContractHero(contract: ContractDto) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (contract.customerId != null) "Müştəri müqaviləsi" else "Tədarükçü müqaviləsi",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
            ContractStatusChip(contract.status, contract.isExpired == true)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = contract.vendorName ?: contract.customerName ?: "Tərəf tapılmadı",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(10.dp))
        OpsInfoRow("Müddət", "${Format.date(contract.startDate)} — ${Format.date(contract.endDate)}")
        OpsInfoRow("Aylıq yük", Format.money(contract.monthlyAmount), OpsColors.Terracotta)
        OpsInfoRow("Dövr üzrə cəm", Format.money(contract.totalAmount))
        OpsInfoRow("Xidmət sayı", (contract.services?.size ?: 0).toString())
        if (!contract.note.isNullOrBlank()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = OpsColors.Border,
            )
            Text(contract.note, fontSize = 12.sp, color = OpsColors.Muted)
        }
    }
}

@Composable
private fun ContractActions(
    contract: ContractDto,
    busy: Boolean,
    onDates: () -> Unit,
    onActivate: () -> Unit,
    onSuspend: () -> Unit,
    onTerminate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (contract.status != ContractStatus.TERMINATED) {
                OpsSecondaryButton(
                    text = "Müddəti dəyiş",
                    onClick = onDates,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                )
            }
            if (contract.status == ContractStatus.DRAFT || contract.status == ContractStatus.SUSPENDED) {
                OpsPrimaryButton(
                    text = "Aktivləşdir",
                    onClick = onActivate,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                )
            }
            if (contract.status == ContractStatus.ACTIVE) {
                OpsSecondaryButton(
                    text = "Dayandır",
                    onClick = onSuspend,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (contract.status != ContractStatus.TERMINATED) {
                OpsSecondaryButton(
                    text = "Ləğv et",
                    onClick = onTerminate,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    danger = true,
                )
            }
            if (contract.status != ContractStatus.ACTIVE) {
                OpsSecondaryButton(
                    text = "Sil",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    danger = true,
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: ContractServiceDto,
    canManage: Boolean,
    isDraft: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                if (!service.description.isNullOrBlank()) {
                    Text(service.description, fontSize = 11.sp, color = OpsColors.MutedSoft)
                }
            }
            OpsChip(
                text = if (service.isActive != false) "Aktiv" else "Dayandırılıb",
                foreground = if (service.isActive != false) OpsColors.Olive else OpsColors.MutedSoft,
                background = if (service.isActive != false) OpsColors.OliveSoft else OpsColors.Subtle,
            )
        }
        Spacer(Modifier.height(8.dp))
        OpsInfoRow("Növ", BillingPeriod.label(service.billingPeriod))
        OpsInfoRow("Qiymət", Format.money(service.periodAmount ?: service.unitPrice))
        if (service.paymentTermDays != null) {
            OpsInfoRow("Ödəniş müddəti", "${service.paymentTermDays} gün")
        }
        if (canManage) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isDraft) {
                    OpsSecondaryButton("Redaktə", onEdit, enabled = !busy)
                    OpsSecondaryButton("Sil", onDelete, enabled = !busy, danger = true)
                }
                OpsSecondaryButton(
                    text = if (service.isActive != false) "Dayandır" else "Bərpa et",
                    onClick = onToggle,
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
private fun ContractChargeCard(
    charge: PartyChargeRow,
    canPay: Boolean,
    canManage: Boolean,
    busy: Boolean,
    isCustomer: Boolean,
    onPay: () -> Unit,
    onCancel: () -> Unit,
) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    charge.description ?: "Borc",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = OpsColors.Ink,
                )
                Text(
                    text = buildString {
                        if (!charge.period.isNullOrBlank()) append(Format.period(charge.period))
                        if (charge.dueDate != null) {
                            if (isNotEmpty()) append(" · ")
                            append("son tarix ${Format.date(charge.dueDate)}")
                        }
                    }.ifBlank { "—" },
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChargeStatusChip(charge.status)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Məbləğ", fontSize = 10.sp, color = OpsColors.MutedSoft)
                Text(Format.money(charge.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
            }
            Column {
                Text("Ödənilib", fontSize = 10.sp, color = OpsColors.MutedSoft)
                Text(Format.money(charge.paidAmount), fontSize = 13.sp, color = OpsColors.Olive)
            }
            Column {
                Text("Qalıq", fontSize = 10.sp, color = OpsColors.MutedSoft)
                Text(
                    Format.money(charge.outstandingAmount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (charge.outstandingAmount > 0.005) OpsColors.Error else OpsColors.Olive,
                )
            }
        }
        if (charge.isOverdue) {
            Spacer(Modifier.height(4.dp))
            OpsChip("Gecikib", OpsColors.Error, OpsColors.ErrorSoft)
        }
        if (canPay || canManage) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canPay && (charge.status == 0 || charge.status == 1)) {
                    OpsPrimaryButton(
                        text = if (isCustomer) "Ödəniş al" else "Ödə",
                        onClick = onPay,
                        enabled = !busy,
                    )
                }
                if (canManage && charge.status != 2 && charge.status != 3) {
                    OpsSecondaryButton(
                        text = "Ləğv et",
                        onClick = onCancel,
                        enabled = !busy,
                        danger = true,
                    )
                }
            }
        }
    }
}

/* ====================================================================== */
/* Formalar                                                               */
/* ====================================================================== */

@Composable
private fun ServiceFormSheet(
    service: ContractServiceDto?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, String?, String?, String?, Int?) -> Unit,
) {
    var name by remember(service) { mutableStateOf(service?.name ?: "") }
    var price by remember(service) { mutableStateOf(service?.unitPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var period by remember(service) { mutableStateOf(service?.billingPeriod ?: BillingPeriod.MONTHLY) }
    var description by remember(service) { mutableStateOf(service?.description ?: "") }
    var startDate by remember(service) { mutableStateOf(service?.serviceStartDate?.take(10) ?: "") }
    var endDate by remember(service) { mutableStateOf(service?.serviceEndDate?.take(10) ?: "") }
    var termDays by remember(service) { mutableStateOf(service?.paymentTermDays?.toString() ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(
        title = if (service == null) "Yeni xidmət" else "Xidməti redaktə et",
        onDismiss = onDismiss,
    ) {
        OpsTextField(name, { name = it }, "Xidmətin adı")
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = "Hesablaşma dövrü",
            options = listOf(
                BillingPeriod.MONTHLY to "Aylıq",
                BillingPeriod.QUARTERLY to "Rüblük",
                BillingPeriod.YEARLY to "İllik",
                BillingPeriod.ONE_TIME to "Birdəfəlik",
            ).map { it.first.toString() to it.second },
            selectedId = period.toString(),
            onSelect = { period = it.toInt() },
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            price,
            { price = it },
            "Qiymət (₼)",
            keyboardType = KeyboardType.Decimal,
            placeholder = if (period == BillingPeriod.ONE_TIME) "Sonradan təyin oluna bilər" else null,
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(description, { description = it }, "Təsvir", singleLine = false)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsDateField("Başlanğıc", startDate, { startDate = it }, modifier = Modifier.weight(1f))
            OpsDateField("Bitmə", endDate, { endDate = it }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            termDays,
            { termDays = it.filter(Char::isDigit) },
            "Ödəniş müddəti (gün)",
            keyboardType = KeyboardType.Number,
        )
        (localError ?: error)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = if (service == null) "Əlavə et" else "Saxla",
            onCancel = onDismiss,
            onSubmit = {
                val numeric = price.trim().replace(',', '.').toDoubleOrNull()
                if (name.isBlank()) {
                    localError = "Xidmətin adı tələb olunur."
                } else if (period != BillingPeriod.ONE_TIME && (numeric == null || numeric <= 0)) {
                    localError = "Qiymət 0-dan böyük olmalıdır."
                } else {
                    localError = null
                    onSave(
                        name,
                        numeric ?: 0.0,
                        period,
                        description,
                        startDate.takeIf { it.isNotBlank() },
                        endDate.takeIf { it.isNotBlank() },
                        termDays.toIntOrNull(),
                    )
                }
            },
            enabled = name.isNotBlank(),
        )
    }
}

@Composable
internal fun PayChargeSheet(
    charge: PartyChargeRow,
    isCustomer: Boolean,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPay: (Double, Int, String?) -> Unit,
) {
    var amount by remember(charge.id) { mutableStateOf(charge.outstandingAmount.toString()) }
    var method by remember(charge.id) { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember(charge.id) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(
        title = if (isCustomer) "Müştəridən ödəniş" else "Tədarükçüyə ödəniş",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "${charge.description ?: "Borc"} · qalıq ${Format.money(charge.outstandingAmount)}",
            fontSize = 12.sp,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            amount,
            { amount = it },
            "Məbləğ (₼)",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = "Ödəniş üsulu",
            options = listOf(
                PaymentMethod.CASH to "Nağd",
                PaymentMethod.BANK_TRANSFER to "Bank köçürməsi",
            ).map { it.first.toString() to it.second },
            selectedId = method.toString(),
            onSelect = { method = it.toInt() },
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(notes, { notes = it }, "Qeyd", singleLine = false)
        Text(
            text = if (isCustomer) {
                "Ödəniş qeydə alınan kimi tranzaksiyalar jurnalına gəlir kimi düşür."
            } else {
                "Ödəniş qeydə alınan kimi tranzaksiyalar jurnalına xərc kimi düşür."
            },
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
            modifier = Modifier.padding(top = 6.dp),
        )
        (localError ?: error)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = "Ödə",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0) {
                    localError = "Ödəniş məbləği müsbət olmalıdır."
                } else {
                    localError = null
                    onPay(value, method, notes)
                }
            },
        )
    }
}

@Composable
private fun DatesDialog(
    contract: ContractDto,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    var start by remember { mutableStateOf(contract.startDate.take(10)) }
    var end by remember { mutableStateOf(contract.endDate.take(10)) }
    var note by remember { mutableStateOf(contract.note ?: "") }

    OpsFormSheet(title = "Müddət və qeyd", onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsDateField("Başlanğıc", start, { start = it }, modifier = Modifier.weight(1f))
            OpsDateField("Bitmə", end, { end = it }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        OpsTextField(note, { note = it }, "Qeyd", singleLine = false)
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = "Saxla",
            onCancel = onDismiss,
            onSubmit = { onSave(start, end, note) },
            enabled = start.isNotBlank() && end.isNotBlank(),
        )
    }
}

@Composable
private fun TerminateDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }

    OpsFormSheet(title = "Müqaviləni ləğv et", onDismiss = onDismiss) {
        Text(
            "Ləğv edilmiş müqavilə üzrə yeni borc yaradılmır. Bu əməliyyat geri qaytarılmır.",
            fontSize = 12.sp,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
        OpsDateField("Ləğv tarixi", date, { date = it })
        Spacer(Modifier.height(10.dp))
        OpsTextField(note, { note = it }, "Səbəb / qeyd", singleLine = false)
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = "Ləğv et",
            onCancel = onDismiss,
            onSubmit = { onConfirm(date, note) },
            enabled = date.isNotBlank(),
        )
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = false,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink) },
        text = { Text(text, fontSize = 13.sp, color = OpsColors.Muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) OpsColors.Error else OpsColors.Terracotta, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = OpsColors.Muted)
            }
        },
        containerColor = OpsColors.Surface,
    )
}
