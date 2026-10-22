package az.mtk.app.ui.screens.vendors

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
import az.mtk.app.data.api.PartyPaymentDto
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.data.api.VendorChargeDto
import az.mtk.app.data.api.VendorChargesApi
import az.mtk.app.data.api.VendorDetailDto
import az.mtk.app.data.api.VendorPaymentCreateRequest
import az.mtk.app.data.api.VendorType
import az.mtk.app.data.api.VendorsApi
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VendorDetailViewModel(private val vendorId: String) : ViewModel() {
    private val api = AppGraph.service(VendorsApi::class.java)
    private val chargesApi = AppGraph.service(VendorChargesApi::class.java)

    data class UiState(
        val vendor: VendorDetailDto? = null,
        val charges: List<VendorChargeDto>? = null,
        val payments: List<PartyPaymentDto>? = null,
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
            _state.value = _state.value.copy(loading = _state.value.vendor == null, error = null)
            runCatching {
                val vendor = api.get(vendorId).data ?: throw IllegalStateException("Tədarükçü tapılmadı")
                val charges = chargesApi.byVendor(vendorId).data.orEmpty()
                val payments = runCatching { chargesApi.paymentsByVendor(vendorId).data.orEmpty() }.getOrDefault(emptyList())
                Triple(vendor, charges, payments)
            }.onSuccess { (vendor, charges, payments) ->
                _state.value = _state.value.copy(
                    vendor = vendor, charges = charges, payments = payments,
                    loading = false, error = null,
                )
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.toUserMessage())
            }
        }
    }

    fun pay(amount: Double, method: Int, notes: String?, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                chargesApi.createPayment(
                    VendorPaymentCreateRequest(vendorId = vendorId, amount = amount, paymentMethod = method, notes = notes),
                )
            }.onSuccess {
                onDone()
                reload()
            }.onFailure { onError(it.toUserMessage()) }
        }
    }

    fun cancelCharge(charge: VendorChargeDto) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching { chargesApi.cancel(charge.id, emptyMap()) }
                .onFailure { _state.value = _state.value.copy(error = it.toUserMessage()) }
            _state.value = _state.value.copy(busy = false)
            reload()
        }
    }
}

@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: (() -> Unit)?,
    profile: UserProfile? = null,
    viewModel: VendorDetailViewModel = mtkViewModel { VendorDetailViewModel(vendorId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var payOpen by remember { mutableStateOf(false) }
    var payCharge by remember { mutableStateOf<VendorChargeDto?>(null) }
    var cancelTarget by remember { mutableStateOf<VendorChargeDto?>(null) }
    val canManage = viewModel.canManage(profile)
    val canPay = viewModel.canPay(profile)

    LaunchedEffect(vendorId) { viewModel.reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.vendor?.name ?: "Tədarükçü",
            subtitle = state.vendor?.let { VendorType.label(it.vendorType) } ?: "Yüklənir…",
            onBack = onBack,
        )

        when {
            state.loading -> LoadingState("Tədarükçü yüklənir…")
            state.error != null && state.vendor == null -> Column(Modifier.padding(16.dp)) {
                ErrorState(state.error!!, onRetry = viewModel::reload)
            }
            state.vendor != null -> {
                val vendor = state.vendor!!
                val charges = state.charges.orEmpty()
                val openCharges = charges.filter { it.status != ChargeStatus.PAID && it.status != ChargeStatus.CANCELLED }
                val totalOpen = openCharges.sumOf { it.outstandingAmount }
                val totalOverdue = openCharges.filter { it.isOverdue == true }.sumOf { it.outstandingAmount }

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
                                        vendor.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        VendorType.label(vendor.vendorType) + (vendor.director?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                    )
                                }
                                Pill(
                                    if (vendor.isActive == true) "Aktiv" else "Dayandırılıb",
                                    if (vendor.isActive == true) Tone.Success else Tone.Neutral,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            InfoRow("VÖEN", vendor.voen ?: "—")
                            InfoRow("Telefon", vendor.phone ?: "—")
                            InfoRow("E-poçt", vendor.email ?: "—")
                            InfoRow("Ünvan", vendor.address ?: "—")
                            InfoRow("Qeyd", vendor.note ?: "—")
                            InfoRow("Yaradılıb", Format.date(vendor.createdAt))
                            if (canPay && totalOpen > 0.005) {
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton(text = "Ödəniş et", onClick = { payOpen = true })
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(label = "Açıq borc", value = Format.money(totalOpen), modifier = Modifier.weight(1f))
                            StatTile(label = "Gecikmiş", value = Format.money(totalOverdue), modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        SectionHeading(title = "Borclar", subtitle = "Xidmət borcları müqavilə üzrə avtomatik yaradılır.")
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
                                if ((canPay && open) || (canManage && charge.status != ChargeStatus.PAID && charge.status != ChargeStatus.CANCELLED)) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (canPay && open) {
                                            PrimaryButton(text = "Ödə", onClick = { payCharge = charge })
                                        }
                                        if (canManage && charge.status != ChargeStatus.PAID && charge.status != ChargeStatus.CANCELLED) {
                                            az.mtk.app.ui.components.SecondaryButton(
                                                text = "Ləğv et",
                                                onClick = { cancelTarget = charge },
                                                enabled = !state.busy,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionHeading(title = "Ödənişlər", subtitle = "Tədarükçüyə edilən ödənişlər tranzaksiyalar jurnalına xərc kimi düşür.")
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

    if (payOpen) {
        PayVendorModal(
            suggestedAmount = state.charges.orEmpty()
                .filter { it.status != ChargeStatus.PAID && it.status != ChargeStatus.CANCELLED }
                .sumOf { it.outstandingAmount },
            onDismiss = { payOpen = false },
            onPay = { amount, method, notes ->
                viewModel.pay(amount, method, notes, onDone = { payOpen = false }, onError = {})
            },
        )
    }
    payCharge?.let { charge ->
        PayVendorModal(
            suggestedAmount = charge.outstandingAmount,
            description = charge.description,
            onDismiss = { payCharge = null },
            onPay = { amount, method, notes ->
                viewModel.pay(amount, method, notes, onDone = { payCharge = null }, onError = {})
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
private fun PayVendorModal(
    suggestedAmount: Double,
    description: String? = null,
    onDismiss: () -> Unit,
    onPay: (Double, Int, String?) -> Unit,
) {
    var amount by remember { mutableStateOf(if (suggestedAmount > 0) String.format(java.util.Locale.US, "%.2f", suggestedAmount) else "") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Tədarükçüyə ödəniş",
        subtitle = if (description != null) {
            "$description · qalıq borc ${Format.money(suggestedAmount)}"
        } else {
            "Ümumi açıq borc ${Format.money(suggestedAmount)} — ödəniş açıq borclara ən köhnədən avtomatik tətbiq olunacaq."
        },
        onDismiss = onDismiss,
        confirmLabel = "Ödə",
        onConfirm = {
            val value = amount.replace(',', '.').toDoubleOrNull()
            if (value == null || value <= 0) {
                error = "Ödəniş məbləği müsbət olmalıdır."
                return@MtkModal
            }
            onPay(value, method, notes.trim().ifBlank { null })
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
        Spacer(Modifier.height(12.dp))
        az.mtk.app.ui.components.MtkTextField(value = notes, onValueChange = { notes = it }, label = "Qeyd")
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}



