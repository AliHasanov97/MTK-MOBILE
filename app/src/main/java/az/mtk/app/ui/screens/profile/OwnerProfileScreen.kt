package az.mtk.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
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
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ChargeStatus
import az.mtk.app.data.api.ChargesPage
import az.mtk.app.data.api.Envelope
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.data.api.OprOwner
import az.mtk.app.data.api.OprOwnerBalanceResponse
import az.mtk.app.data.api.OprPaymentResponse
import az.mtk.app.data.api.OprPaymentsPage
import az.mtk.app.data.api.OwnerProfileApi
import az.mtk.app.data.api.UsrChangeMyInfoRequest
import az.mtk.app.data.api.UsrCurrentUser
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerProfileViewModel : ViewModel() {
    private val identityApi = AppGraph.service(IdentityApi::class.java)
    private val api = AppGraph.service(OwnerProfileApi::class.java)

    data class UiState(
        val loading: Boolean = true,
        val owner: OprOwner? = null,
        val balance: OprOwnerBalanceResponse? = null,
        val charges: List<az.mtk.app.data.api.ChargeDto> = emptyList(),
        val payments: List<OprPaymentResponse> = emptyList(),
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState()
            runCatching {
                val me: UsrCurrentUser = identityApi.currentUser()
                val owner = api.ownerByUserId(me.id)
                Triple(owner, api.ownerBalance(owner.id), owner.id)
            }.onSuccess { (owner, balanceEnv, ownerId) ->
                _state.value = _state.value.copy(
                    loading = false,
                    owner = owner,
                    balance = balanceEnv.data,
                )
                runCatching { api.chargesByOwner(ownerId) }.onSuccess { charges ->
                    _state.value = _state.value.copy(charges = charges.chargesList)
                }
                runCatching { api.paymentsByOwner(ownerId) }.onSuccess { payments ->
                    _state.value = _state.value.copy(payments = payments.paymentsList)
                }
            }.onFailure { failure ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = if (failure is retrofit2.HttpException && failure.code() == 404) {
                        "Sizin adınıza bağlı sahib qeydi tapılmadı. Zəhmət olmasa idarəçi ilə əlaqə saxlayın."
                    } else {
                        failure.toUserMessage()
                    },
                )
            }
        }
    }

    fun changeInfo(phone: String, password: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                identityApi.changeMyInfo(
                    UsrChangeMyInfoRequest(
                        password = password.takeIf { it.isNotBlank() },
                        phoneNumber = phone.trim().takeIf { it.isNotBlank() },
                    ),
                )
            }.onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

/** Envelope payloads without the generic unwrap helper (kept local to this module). */
private val Envelope<ChargesPage>.chargesList: List<az.mtk.app.data.api.ChargeDto>
    get() = data?.charges.orEmpty()

private val Envelope<OprPaymentsPage>.paymentsList: List<OprPaymentResponse>
    get() = data?.payments.orEmpty()

private fun signedBalance(value: Double?): String {
    if (value == null) return "—"
    return if (value > 0) "+${Format.money(value)}" else Format.money(value)
}

private fun statusTone(status: Int?): Tone = when (status) {
    ChargeStatus.UNPAID -> Tone.Danger
    ChargeStatus.PARTIALLY_PAID -> Tone.Warning
    ChargeStatus.PAID -> Tone.Success
    else -> Tone.Neutral
}

private val ownerTabs = listOf(
    "Mənzillər" to 0,
    "Qarajlar" to 1,
    "Ödənişlər" to 2,
)

@Composable
fun OwnerProfileScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: OwnerProfileViewModel = mtkViewModel { OwnerProfileViewModel() },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var showChangeInfo by remember { mutableStateOf(false) }

    val owner = uiState.owner

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Mənim profilim",
                subtitle = owner?.fullName ?: if (uiState.loading) "Yüklənir…" else "Sahib profili",
                onBack = onBack,
            )
        }

        when {
            uiState.loading -> LoadingState("Profil yüklənir…")
            uiState.error != null -> Column(Modifier.padding(16.dp)) {
                ErrorState(uiState.error!!, onRetry = viewModel::load)
            }
            owner == null -> EmptyState("Məlumat tapılmadı")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Hero card
                MtkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(initials = Format.initials(owner.fullName), size = 54.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                owner.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                owner.email ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                            Text(
                                owner.phoneNumber ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                        Pill(
                            if (owner.isActive) "Aktiv" else "Deaktiv",
                            if (owner.isActive) Tone.Success else Tone.Neutral,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(text = "Məlumatları dəyiş", onClick = { showChangeInfo = true })
                }

                Spacer(Modifier.height(14.dp))

                // Balance tiles
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        StatTile(
                            label = "Ümumi borc",
                            value = Format.money(uiState.balance?.totalDebt),
                            icon = Icons.Outlined.Home,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        StatTile(
                            label = "Ödənilib",
                            value = Format.money(uiState.balance?.totalPaid),
                            icon = Icons.Outlined.DirectionsCar,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                StatTile(
                    label = "Cari balans",
                    value = signedBalance(uiState.balance?.currentBalance),
                    caption = "Müsbət — avans, mənfi — borc",
                )

                Spacer(Modifier.height(18.dp))

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ownerTabs.forEach { (label, index) ->
                        FilterChip(
                            selected = tab == index,
                            onClick = { tab = index },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MtkColors.Green,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (tab) {
                    0 -> ApartmentsTab(owner)
                    1 -> GaragesTab(owner)
                    else -> PaymentsTab(uiState)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showChangeInfo) {
        ChangeInfoModal(
            currentPhone = owner?.phoneNumber ?: "",
            onDismiss = { showChangeInfo = false },
            onSave = { phone, password, onDone ->
                viewModel.changeInfo(phone, password) { message ->
                    if (message == null) {
                        viewModel.load()
                    }
                    onDone(message)
                }
            },
        )
    }
}

@Composable
private fun ApartmentsTab(owner: OprOwner) {
    val apartments = owner.apartments.orEmpty()
    SectionCard(title = "Mənzillərim", emptyText = "Adınıza heç bir mənzil qeydə alınmayıb.", hasContent = apartments.isNotEmpty()) {
        apartments.forEach { apartment ->
            ListRow(
                title = "${apartment.building?.name ?: "—"} · ${apartment.apartmentNumber ?: "—"}",
                subtitle = listOfNotNull(
                    apartment.floor?.let { "$it mərtəbə" },
                    apartment.roomCount?.let { "$it otaq" },
                    apartment.areaSquareMeters?.let { Format.area(it) },
                ).joinToString(" · ").ifBlank { null },
                trailing = { Pill(apartment.status ?: "—", Tone.Info) },
            )
        }
    }
}

@Composable
private fun GaragesTab(owner: OprOwner) {
    val garages = owner.garages.orEmpty()
    SectionCard(title = "Qarajlarım", emptyText = "Adınıza heç bir qaraj qeydə alınmayıb.", hasContent = garages.isNotEmpty()) {
        garages.forEach { garage ->
            ListRow(
                title = garage.garageNumber ?: "—",
                subtitle = garage.description,
                trailing = { Pill(Format.garageType(garage.type), Tone.Info) },
            )
        }
    }
}

@Composable
private fun PaymentsTab(uiState: OwnerProfileViewModel.UiState) {
    SectionCard(
        title = "Ödənişlər",
        emptyText = "Ödəniş qeydi tapılmadı.",
        hasContent = uiState.charges.isNotEmpty() || uiState.payments.isNotEmpty(),
    ) {
        uiState.charges.forEach { charge ->
            ListRow(
                title = "${Format.period(charge.period)} — ${Format.money(charge.amount)}",
                subtitle = charge.description ?: charge.propertyLabel,
                trailing = { Pill(Format.chargeStatus(charge.status), statusTone(charge.status)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        uiState.payments.forEach { payment ->
            ListRow(
                title = Format.money(payment.amount),
                subtitle = listOfNotNull(
                    Format.date(payment.paymentDate),
                    payment.propertyLabel,
                    payment.notes?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { null },
                meta = payment.partyName,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    emptyText: String,
    hasContent: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    MtkCard {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        if (!hasContent) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
        } else {
            content()
        }
    }
}

@Composable
private fun ChangeInfoModal(
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (phone: String, password: String, onDone: (String?) -> Unit) -> Unit,
) {
    var phone by remember { mutableStateOf(currentPhone) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    MtkModal(
        title = "Məlumatları dəyiş",
        subtitle = "Telefon nömrənizi yeniləyə və şifrə təyin edə bilərsiniz.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Saxla",
        confirmEnabled = !saving,
        onConfirm = {
            saving = true
            onSave(phone, password) { message ->
                saving = false
                if (message == null) onDismiss() else error = message
            }
        },
    ) {
        MtkTextField(phone, { phone = it }, "Telefon")
        Spacer(Modifier.height(12.dp))
        MtkTextField(password, { password = it }, "Yeni şifrə (dəyişməməsi üçün boş saxlayın)")
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}
