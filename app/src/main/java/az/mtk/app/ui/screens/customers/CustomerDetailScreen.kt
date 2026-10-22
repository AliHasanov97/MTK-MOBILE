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
import az.mtk.app.data.api.ChargeLabels
import az.mtk.app.data.api.ChargeStatus
import az.mtk.app.data.api.ContractDates
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.ContractStatus
import az.mtk.app.data.api.ContractsApi
import az.mtk.app.data.api.CustomerBalanceDto
import az.mtk.app.data.api.CustomerChargeCreateRequest
import az.mtk.app.data.api.CustomerChargeDto
import az.mtk.app.data.api.CustomerChargesApi
import az.mtk.app.data.api.CustomerDetailDto
import az.mtk.app.data.api.CustomerPaymentCreateRequest
import az.mtk.app.data.api.CustomerType
import az.mtk.app.data.api.CustomersApi
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.data.api.PartyPaymentDto
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Comparison
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
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
import az.mtk.app.ui.screens.contracts.chargeTone
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class CustomerDetailViewModel(private val customerId: String) : ViewModel() {
    private val api = AppGraph.service(CustomersApi::class.java)
    private val chargesApi = AppGraph.service(CustomerChargesApi::class.java)
    private val contractsApi = AppGraph.service(ContractsApi::class.java)

    data class UiState(
        val customer: CustomerDetailDto? = null,
        val charges: List<CustomerChargeDto>? = null,
        val payments: List<PartyPaymentDto>? = null,
        val balance: CustomerBalanceDto? = null,
        val contracts: List<ContractListItemDto>? = null,
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
            _state.value = _state.value.copy(loading = _state.value.customer == null, error = null)
            runCatching {
                coroutineScope {
                    val customerDeferred = async {
                        api.get(customerId).data ?: throw IllegalStateException("Müştəri tapılmadı")
                    }
                    // Hər bölmə ayrıca yüklənir: biri uğursuz olsa digərləri yenə də görünür.
                    val chargesDeferred = async {
                        runCatching { chargesApi.byCustomer(customerId).data.orEmpty() }.getOrDefault(emptyList())
                    }
                    val paymentsDeferred = async {
                        runCatching { chargesApi.paymentsByCustomer(customerId).data.orEmpty() }.getOrDefault(emptyList())
                    }
                    val balanceDeferred = async {
                        runCatching { api.balance(customerId).data }.getOrNull()
                    }
                    val contractsDeferred = async {
                        runCatching {
                            contractsApi.search(
                                SearchBody(
                                    filters = listOf(
                                        QueryFilter(columnName = "customerId", comparison = Comparison.EQUALS, value = customerId),
                                    ),
                                    pageSize = 100,
                                ),
                            ).data?.contracts.orEmpty()
                        }.getOrDefault(emptyList())
                    }
                    CustomerLoaded(
                        customerDeferred.await(),
                        chargesDeferred.await(),
                        paymentsDeferred.await(),
                        balanceDeferred.await(),
                        contractsDeferred.await(),
                    )
                }
            }.onSuccess { loaded ->
                _state.value = _state.value.copy(
                    customer = loaded.customer,
                    charges = loaded.charges,
                    payments = loaded.payments,
                    balance = loaded.balance,
                    contracts = loaded.contracts,
                    loading = false,
                    error = null,
                )
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.toUserMessage())
            }
        }
    }

    private data class CustomerLoaded(
        val customer: CustomerDetailDto,
        val charges: List<CustomerChargeDto>,
        val payments: List<PartyPaymentDto>,
        val balance: CustomerBalanceDto?,
        val contracts: List<ContractListItemDto>,
    )

    fun createCharge(description: String, amount: Double, dueDate: LocalDate?, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                chargesApi.createCharge(
                    CustomerChargeCreateRequest(
                        customerId = customerId,
                        description = description,
                        amount = amount,
                        dueDate = dueDate?.let { ContractDates.toUtcIso(it.toString()) },
                    ),
                )
            }.onSuccess {
                onDone()
                reload()
            }.onFailure { onError(it.toUserMessage()) }
        }
    }

    fun pay(amount: Double, method: Int, notes: String?, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                chargesApi.createPayment(
                    CustomerPaymentCreateRequest(customerId = customerId, amount = amount, paymentMethod = method, notes = notes),
                )
            }.onSuccess {
                onDone()
                reload()
            }.onFailure { onError(it.toUserMessage()) }
        }
    }

    fun cancelCharge(charge: CustomerChargeDto) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching { chargesApi.cancelCharge(charge.id, emptyMap()) }
                .onFailure { _state.value = _state.value.copy(error = it.toUserMessage()) }
            _state.value = _state.value.copy(busy = false)
            reload()
        }
    }
}

@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: (() -> Unit)?,
    onOpenContract: ((String) -> Unit)? = null,
    profile: UserProfile? = null,
    viewModel: CustomerDetailViewModel = mtkViewModel { CustomerDetailViewModel(customerId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var chargeOpen by remember { mutableStateOf(false) }
    var payOpen by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<CustomerChargeDto?>(null) }
    val canManage = viewModel.canManage(profile)
    val canPay = viewModel.canPay(profile)

    LaunchedEffect(customerId) { viewModel.reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.customer?.name ?: "Müştəri",
            subtitle = state.customer?.let { CustomerType.label(it.customerType) } ?: "Yüklənir…",
            onBack = onBack,
        )

        when {
            state.loading -> LoadingState("Müştəri yüklənir…")
            state.error != null && state.customer == null -> Column(Modifier.padding(16.dp)) {
                ErrorState(state.error!!, onRetry = viewModel::reload)
            }
            state.customer != null -> {
                val customer = state.customer!!
                val charges = state.charges.orEmpty()
                val openCharges = charges.filter { it.status != ChargeStatus.PAID && it.status != ChargeStatus.CANCELLED }
                val totalOpen = openCharges.sumOf { it.outstandingAmount }
                val totalOverdue = openCharges.filter { it.isOverdue == true }.sumOf { it.outstandingAmount }
                val advance = state.balance?.advance ?: 0.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.error != null) {
                        item { Text(state.error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall) }
                    }

                    item {
                        MtkCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        CustomerType.label(customer.customerType) + (customer.director?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                    )
                                }
                                Pill(
                                    if (customer.isActive == true) "Aktiv" else "Dayandırılıb",
                                    if (customer.isActive == true) Tone.Success else Tone.Neutral,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            InfoRow("VÖEN", customer.voen ?: "—")
                            InfoRow("Telefon", customer.phone ?: "—")
                            InfoRow("E-poçt", customer.email ?: "—")
                            InfoRow("Ünvan", customer.address ?: "—")
                            InfoRow("Qeyd", customer.note ?: "—")
                            InfoRow("Yaradılıb", Format.date(customer.createdAt))
                            if (canPay) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = "+ Borc yaz",
                                        onClick = { chargeOpen = true },
                                        modifier = Modifier.weight(1f),
                                    )
                                    PrimaryButton(
                                        text = "Ödəniş qəbul et",
                                        onClick = { payOpen = true },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(label = "Açıq borc", value = Format.money(totalOpen), modifier = Modifier.weight(1f))
                            StatTile(label = "Avans", value = Format.money(advance), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(label = "Gecikmiş", value = Format.money(totalOverdue), modifier = Modifier.weight(1f))
                            StatTile(label = "Cəmi ödənilib", value = Format.money(state.balance?.totalPaid), modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        SectionHeading(title = "Müqavilələr")
                    }
                    val contracts = state.contracts.orEmpty()
                    if (contracts.isEmpty()) {
                        item { Text(
                            "Bu müştəri ilə hələ müqavilə bağlanmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        ) }
                    } else {
                        items(contracts.size) { index ->
                            val contract = contracts[index]
                            MtkCard(
                                padding = 12.dp,
                                onClick = onOpenContract?.let { open -> { open(contract.id) } },
                            ) {
                                ListRow(
                                    title = contract.number,
                                    subtitle = "${Format.date(contract.startDate)} — ${Format.date(contract.endDate)}",
                                    trailing = {
                                        Pill(
                                            ContractStatus.label(contract.status),
                                            if (contract.status == ContractStatus.ACTIVE) Tone.Success else Tone.Neutral,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    item {
                        SectionHeading(
                            title = "Borclar",
                            subtitle = "Müştəri borcları müqavilə üzrə avtomatik yaranır, «Borc yaz» ilə əl ilə də əlavə olunur.",
                        )
                    }
                    when {
                        state.charges == null -> item { LoadingState("Borclar yüklənir…") }
                        charges.isEmpty() -> item { Text(
                            "Hələ heç bir borc yaranmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        ) }
                        else -> items(charges.size) { index ->
                            val charge = charges.sortedBy { it.createdAt ?: "" }[index]
                            val open = charge.status == ChargeStatus.UNPAID || charge.status == ChargeStatus.PARTIALLY_PAID
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
                                            charge.period?.let { Format.period(it) } ?: "—",
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
                                        Pill(ChargeLabels.label(charge.status), chargeTone(charge.status))
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Qalıq: ${Format.money(charge.outstandingAmount)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (charge.outstandingAmount > 0.005) MtkColors.Negative else MtkColors.Positive,
                                        )
                                        Text(
                                            "Məbləğ: ${Format.money(charge.amount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                }
                                if (canManage && open) {
                                    Spacer(Modifier.height(8.dp))
                                    az.mtk.app.ui.components.SecondaryButton(
                                        text = "Ləğv et",
                                        onClick = { cancelTarget = charge },
                                        enabled = !state.busy,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeading(title = "Daxil olan ödənişlər", subtitle = "Borcdan artıq ödəniş avans kimi qalır və növbəti borca avtomatik tətbiq olunur.")
                    }
                    val payments = state.payments.orEmpty()
                    if (payments.isEmpty()) {
                        item { Text(
                            "Hələ ödəniş qeydə alınmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        ) }
                    } else {
                        items(payments.size) { index ->
                            val payment = payments.sortedByDescending { it.paymentDate ?: "" }[index]
                            MtkCard(padding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            Format.dateTime(payment.paymentDate),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            PaymentMethod.label(payment.paymentMethod) + (payment.notes?.let { " · $it" } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MtkColors.MutedSoft,
                                        )
                                    }
                                    Text(
                                        Format.money(payment.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (chargeOpen) {
        CreateCustomerChargeModal(
            onDismiss = { chargeOpen = false },
            onSave = { description, amount, dueDate ->
                viewModel.createCharge(description, amount, dueDate, onDone = { chargeOpen = false }, onError = {})
            },
        )
    }
    if (payOpen) {
        ReceivePaymentModal(
            customerName = state.customer?.name ?: "",
            outstanding = state.charges.orEmpty()
                .filter { it.status != ChargeStatus.PAID && it.status != ChargeStatus.CANCELLED }
                .sumOf { it.outstandingAmount },
            onDismiss = { payOpen = false },
            onPay = { amount, method, notes ->
                viewModel.pay(amount, method, notes, onDone = { payOpen = false }, onError = {})
            },
        )
    }
    cancelTarget?.let { charge ->
        ConfirmDialog(
            title = "Borcu ləğv et",
            message = "\"${charge.description ?: "Xidmət borcu"}\" borcu ləğv edilsin?",
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
internal fun CreateCustomerChargeModal(
    onDismiss: () -> Unit,
    onSave: (String, Double, LocalDate?) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Müştəriyə borc yaz",
        onDismiss = onDismiss,
        confirmLabel = "Yaz",
        confirmEnabled = description.isNotBlank(),
        onConfirm = {
            val value = amount.replace(',', '.').toDoubleOrNull()
            if (value == null || value <= 0) {
                error = "Məbləğ müsbət olmalıdır."
                return@MtkModal
            }
            onSave(description.trim(), value, dueDate)
        },
    ) {
        MtkTextField(value = description, onValueChange = { description = it }, label = "Təsvir", placeholder = "məs. Əlavə xidmət haqqı", required = true)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkNumberField(value = amount, onValueChange = { amount = it }, label = "Məbləğ (₼)", required = true, modifier = Modifier.weight(1f))
            MtkDateField(value = dueDate, onValueChange = { dueDate = it }, label = "Son ödəniş tarixi", modifier = Modifier.weight(1f))
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ReceivePaymentModal(
    customerName: String,
    outstanding: Double,
    onDismiss: () -> Unit,
    onPay: (Double, Int, String?) -> Unit,
) {
    var amount by remember { mutableStateOf(if (outstanding > 0) String.format(java.util.Locale.US, "%.2f", outstanding) else "") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Müştəridən ödəniş",
        subtitle = "$customerName · qalıq borc ${Format.money(outstanding)}",
        onDismiss = onDismiss,
        confirmLabel = "Qəbul et",
        onConfirm = {
            val value = amount.replace(',', '.').toDoubleOrNull()
            if (value == null || value <= 0) {
                error = "Ödəniş məbləği müsbət olmalıdır."
                return@MtkModal
            }
            onPay(value, method, notes.trim().ifBlank { null })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkNumberField(value = amount, onValueChange = { amount = it }, label = "Məbləğ (₼)", required = true, modifier = Modifier.weight(1f))
            MtkSelect(
                value = method.toString(),
                onValueChange = { method = it.toInt() },
                label = "Ödəniş üsulu",
                options = PaymentMethod.selectable.map { MtkOption(it.toString(), PaymentMethod.label(it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(value = notes, onValueChange = { notes = it }, label = "Qeyd")
        Spacer(Modifier.height(8.dp))
        Text(
            "Ödəniş ən köhnə borcdan başlayaraq bağlanır, artıq hissə avans kimi qalır.",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}


