package az.mtk.app.ui.screens.operations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.ContractListItemDto
import az.mtk.app.data.api.CustomerType
import az.mtk.app.data.api.PartyChargeRow
import az.mtk.app.data.api.PartyPaymentRow
import az.mtk.app.data.api.PartyRow
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.data.api.VendorType
import az.mtk.app.util.Format
import java.time.LocalDate

private fun partyTypeLabel(kind: PartyKind, type: Int): String =
    if (kind == PartyKind.VENDOR) VendorType.label(type) else CustomerType.label(type)

/* ====================================================================== */
/* Siyahı                                                                 */
/* ====================================================================== */

@Composable
internal fun PartiesTab(
    state: PartiesViewModel.UiState,
    viewModel: PartiesViewModel,
    onOpenParty: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OpsSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = if (state.kind == PartyKind.VENDOR) {
                "Tədarükçü adı, VÖEN və ya telefon…"
            } else {
                "Müştəri adı, VÖEN və ya telefon…"
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                text = "${state.total} ${if (state.kind == PartyKind.VENDOR) "tədarükçü" else "müştəri"}",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
            if (state.canManage) {
                OpsPrimaryButton(
                    text = if (state.kind == PartyKind.VENDOR) "+ Tədarükçü" else "+ Müştəri",
                    onClick = { showCreate = true },
                )
            }
        }

        when {
            state.loading && state.list.isEmpty() -> OpsLoading()
            state.error != null && state.list.isEmpty() -> OpsError(state.error!!) { viewModel.load() }
            state.list.isEmpty() -> OpsEmpty(
                title = if (state.kind == PartyKind.VENDOR) "Tədarükçü tapılmadı" else "Müştəri tapılmadı",
                text = "Axtarışı dəyişin və ya yeni tərəf əlavə edin.",
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
                items(state.list, key = { it.id }) { party ->
                    PartyCard(state.kind, party) { onOpenParty(party.id) }
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
        PartyFormSheet(
            kind = state.kind,
            party = null,
            saving = state.saving,
            error = state.formError,
            onDismiss = {
                showCreate = false
                viewModel.clearFormError()
            },
            onSave = { name, type, voen, director, email, phone, address, note ->
                viewModel.saveParty(
                    id = null,
                    name = name,
                    type = type,
                    voen = voen,
                    director = director,
                    email = email,
                    phone = phone,
                    address = address,
                    note = note,
                    onSaved = { showCreate = false },
                )
            },
        )
    }
}

@Composable
private fun PartyCard(kind: PartyKind, party: PartyRow, onClick: () -> Unit) {
    OpsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(OpsColors.Container, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = Format.initials(party.name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Terracotta,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = party.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = partyTypeLabel(kind, party.type),
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                val meta = listOfNotNull(
                    party.voen?.takeIf { it.isNotBlank() }?.let { "VÖEN $it" },
                    party.phone?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 11.sp,
                        color = OpsColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                OpsChip(
                    text = if (party.isActive) "Aktiv" else "Deaktiv",
                    foreground = if (party.isActive) OpsColors.Olive else OpsColors.MutedSoft,
                    background = if (party.isActive) OpsColors.OliveSoft else OpsColors.Subtle,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = OpsColors.MutedSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/* ====================================================================== */
/* Detal                                                                  */
/* ====================================================================== */

@Composable
internal fun PartyDetailScreen(
    partyId: String,
    viewModel: PartiesViewModel,
    onBack: () -> Unit,
    onOpenContract: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(partyId) { viewModel.openParty(partyId) }

    var tab by remember { mutableStateOf(PartyDetailTab.CHARGES) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showChargeForm by remember { mutableStateOf(false) }
    var showPaymentForm by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<PartyChargeRow?>(null) }
    var cancelTarget by remember { mutableStateOf<PartyChargeRow?>(null) }

    val detail = state.detail
    val kind = state.kind

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = detail?.name ?: kind.singular,
                subtitle = detail?.let { partyTypeLabel(kind, it.type) } ?: "Yüklənir…",
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
                state.detailLoading && detail == null -> OpsLoading()
                state.detailError != null && detail == null ->
                    OpsError(state.detailError!!) { viewModel.refreshDetail() }
                detail == null -> OpsLoading()
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
                    item {
                        PartyHero(
                            kind = kind,
                            party = detail,
                            balance = state.balance,
                            canManage = state.canManage,
                            busy = state.busy,
                            onEdit = { showEdit = true },
                            onToggleStatus = { viewModel.setStatus(!detail.isActive) },
                            onDelete = { showDelete = true },
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.canPay) {
                                OpsPrimaryButton(
                                    text = if (kind == PartyKind.CUSTOMER) "Ödəniş qəbul et" else "Ödəniş et",
                                    onClick = { showPaymentForm = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.busy,
                                )
                            }
                            if (state.canManage && kind == PartyKind.CUSTOMER) {
                                OpsSecondaryButton(
                                    text = "Borc yaz",
                                    onClick = { showChargeForm = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.busy,
                                )
                            }
                        }
                    }

                    item {
                        val tabs = if (kind == PartyKind.CUSTOMER) {
                            listOf(
                                PartyDetailTab.CHARGES to "Borclar",
                                PartyDetailTab.PAYMENTS to "Ödənişlər",
                                PartyDetailTab.CONTRACTS to "Müqavilələr",
                            )
                        } else {
                            listOf(
                                PartyDetailTab.CHARGES to "Borclar",
                                PartyDetailTab.PAYMENTS to "Ödənişlər",
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tabs.forEach { (value, label) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { tab = value },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (tab == value) OpsColors.Terracotta else OpsColors.Subtle,
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        fontSize = 12.sp,
                                        fontWeight = if (tab == value) FontWeight.Bold else FontWeight.Medium,
                                        color = if (tab == value) androidx.compose.ui.graphics.Color.White else OpsColors.Ink,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    when (tab) {
                        PartyDetailTab.CHARGES -> {
                            if (state.relatedLoading && state.charges.isEmpty()) {
                                item { OpsLoading() }
                            } else if (state.charges.isEmpty()) {
                                item {
                                    OpsCard {
                                        Text(
                                            "Bu tərəf üzrə borc yoxdur.",
                                            fontSize = 12.sp,
                                            color = OpsColors.MutedSoft,
                                        )
                                    }
                                }
                            } else {
                                items(state.charges, key = { it.id }) { charge ->
                                    PartyChargeCard(
                                        charge = charge,
                                        kind = kind,
                                        canPay = state.canPay,
                                        canManage = state.canManage,
                                        busy = state.busy,
                                        onPay = { payTarget = charge },
                                        onCancel = { cancelTarget = charge },
                                    )
                                }
                            }
                        }

                        PartyDetailTab.PAYMENTS -> {
                            if (state.relatedLoading && state.payments.isEmpty()) {
                                item { OpsLoading() }
                            } else if (state.payments.isEmpty()) {
                                item {
                                    OpsCard {
                                        Text(
                                            "Ödəniş qeydə alınmayıb.",
                                            fontSize = 12.sp,
                                            color = OpsColors.MutedSoft,
                                        )
                                    }
                                }
                            } else {
                                items(state.payments, key = { it.id }) { payment ->
                                    PartyPaymentCard(payment)
                                }
                            }
                        }

                        PartyDetailTab.CONTRACTS -> {
                            if (state.contracts.isEmpty()) {
                                item {
                                    OpsCard {
                                        Text(
                                            "Bu müştəri ilə bağlanmış müqavilə yoxdur.",
                                            fontSize = 12.sp,
                                            color = OpsColors.MutedSoft,
                                        )
                                    }
                                }
                            } else {
                                items(state.contracts, key = { it.id }) { contract ->
                                    PartyContractCard(contract) { onOpenContract(contract.id) }
                                }
                            }
                        }
                    }

                    if (state.relatedError != null) {
                        item {
                            Text(state.relatedError!!, fontSize = 11.sp, color = OpsColors.Error)
                        }
                    }
                }
            }
        }
    }

    if (showEdit && detail != null) {
        PartyFormSheet(
            kind = kind,
            party = detail,
            saving = state.saving,
            error = state.formError,
            onDismiss = {
                showEdit = false
                viewModel.clearFormError()
            },
            onSave = { name, type, voen, director, email, phone, address, note ->
                viewModel.saveParty(
                    id = detail.id,
                    name = name,
                    type = type,
                    voen = voen,
                    director = director,
                    email = email,
                    phone = phone,
                    address = address,
                    note = note,
                    onSaved = { showEdit = false },
                )
            },
        )
    }

    if (showChargeForm) {
        CustomerChargeSheet(
            saving = state.busy,
            error = state.formError,
            onDismiss = {
                showChargeForm = false
                viewModel.clearFormError()
            },
            onSave = { description, amount, dueDate ->
                viewModel.createCharge(description, amount, dueDate) { showChargeForm = false }
            },
        )
    }

    if (showPaymentForm) {
        OpsPaymentSheet(
            title = if (kind == PartyKind.CUSTOMER) "Müştəridən ödəniş" else "Tədarükçüyə ödəniş",
            hint = if (kind == PartyKind.CUSTOMER) {
                "Ödəniş müştərinin açıq borclarına avtomatik bölünəcək."
            } else {
                "Ödəniş tədarükçünün açıq borclarına avtomatik bölünəcək."
            },
            saving = state.busy,
            error = state.formError,
            onDismiss = {
                showPaymentForm = false
                viewModel.clearFormError()
            },
            onPay = { amount, method, notes ->
                if (kind == PartyKind.CUSTOMER) {
                    viewModel.receivePayment(amount, method, notes) { showPaymentForm = false }
                } else {
                    viewModel.payVendor(amount, method, notes) { showPaymentForm = false }
                }
            },
        )
    }

    payTarget?.let { charge ->
        OpsPaymentSheet(
            title = if (kind == PartyKind.CUSTOMER) "Müştəridən ödəniş" else "Tədarükçüyə ödəniş",
            hint = "${charge.description ?: "Borc"} · qalıq ${Format.money(charge.outstandingAmount)}",
            initialAmount = charge.outstandingAmount.toString(),
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

    if (showDelete && detail != null) {
        ConfirmDialog(
            title = "\"${detail.name}\" silinsin?",
            text = "Bu əməliyyat geri qaytarılmır.",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.delete { onBack() }
            },
        )
    }
}

private enum class PartyDetailTab { CHARGES, PAYMENTS, CONTRACTS }

@Composable
private fun PartyHero(
    kind: PartyKind,
    party: PartyRow,
    balance: az.mtk.app.data.api.CustomerBalanceDto?,
    canManage: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
) {
    OpsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(OpsColors.Container, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = Format.initials(party.name),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Terracotta,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = party.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    text = partyTypeLabel(kind, party.type),
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = if (party.isActive) "Aktiv" else "Deaktiv",
                foreground = if (party.isActive) OpsColors.Olive else OpsColors.MutedSoft,
                background = if (party.isActive) OpsColors.OliveSoft else OpsColors.Subtle,
            )
        }

        if (kind == PartyKind.CUSTOMER && balance != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BalanceTile("Qalıq borc", Format.money(balance.outstandingDebt), OpsColors.Error, Modifier.weight(1f))
                BalanceTile("Avans", Format.money(balance.advance), OpsColors.Olive, Modifier.weight(1f))
                BalanceTile("Ödənilib", Format.money(balance.totalPaid), OpsColors.Blue, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(6.dp))
        if (!party.voen.isNullOrBlank()) OpsInfoRow("VÖEN", party.voen)
        if (!party.director.isNullOrBlank()) OpsInfoRow("Rəhbər", party.director)
        if (!party.phone.isNullOrBlank()) OpsInfoRow("Telefon", party.phone)
        if (!party.email.isNullOrBlank()) OpsInfoRow("E-poçt", party.email)
        if (!party.address.isNullOrBlank()) OpsInfoRow("Ünvan", party.address)
        if (!party.note.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(party.note, fontSize = 12.sp, color = OpsColors.Muted)
        }

        if (canManage) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpsSecondaryButton("Redaktə et", onEdit, enabled = !busy)
                OpsSecondaryButton(
                    text = if (party.isActive) "Deaktiv et" else "Aktiv et",
                    onClick = onToggleStatus,
                    enabled = !busy,
                )
                OpsSecondaryButton("Sil", onDelete, enabled = !busy, danger = true)
            }
        }
    }
}

@Composable
private fun BalanceTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = OpsColors.Subtle,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 10.sp, color = OpsColors.MutedSoft)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun PartyChargeCard(
    charge: PartyChargeRow,
    kind: PartyKind,
    canPay: Boolean,
    canManage: Boolean,
    busy: Boolean,
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
                        text = if (kind == PartyKind.CUSTOMER) "Ödəniş al" else "Ödə",
                        onClick = onPay,
                        enabled = !busy,
                    )
                }
                if (canManage && charge.status != 2 && charge.status != 3) {
                    OpsSecondaryButton("Ləğv et", onCancel, enabled = !busy, danger = true)
                }
            }
        }
    }
}

@Composable
private fun PartyPaymentCard(payment: PartyPaymentRow) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Format.money(payment.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Olive,
                )
                Text(
                    text = "${Format.date(payment.paymentDate)} · ${PaymentMethod.label(payment.paymentMethod)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                if (!payment.notes.isNullOrBlank()) {
                    Text(payment.notes, fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
            OpsChip("Ödənilib", OpsColors.Olive, OpsColors.OliveSoft)
        }
    }
}

@Composable
private fun PartyContractCard(contract: ContractListItemDto, onClick: () -> Unit) {
    OpsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contract.number, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Text(
                    "${Format.date(contract.startDate)} — ${Format.date(contract.endDate)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            ContractStatusChip(contract.status, contract.isExpired == true)
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = OpsColors.MutedSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/* ====================================================================== */
/* Formalar                                                               */
/* ====================================================================== */

@Composable
private fun PartyFormSheet(
    kind: PartyKind,
    party: PartyRow?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, Int, String?, String?, String?, String?, String?, String?) -> Unit,
) {
    var name by remember(party) { mutableStateOf(party?.name ?: "") }
    var type by remember(party) { mutableStateOf(party?.type ?: 0) }
    var voen by remember(party) { mutableStateOf(party?.voen ?: "") }
    var director by remember(party) { mutableStateOf(party?.director ?: "") }
    var email by remember(party) { mutableStateOf(party?.email ?: "") }
    var phone by remember(party) { mutableStateOf(party?.phone ?: "") }
    var address by remember(party) { mutableStateOf(party?.address ?: "") }
    var note by remember(party) { mutableStateOf(party?.note ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(
        title = if (party == null) "Yeni ${kind.singular.lowercase()}" else "${kind.singular} redaktəsi",
        onDismiss = onDismiss,
    ) {
        OpsTextField(name, { name = it }, "Ad / təşkilat adı")
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = "Tərəf növü",
            options = listOf(
                VendorType.LEGAL_ENTITY to "Hüquqi şəxs",
                VendorType.INDIVIDUAL to "Fiziki şəxs",
                VendorType.SOLE_PROPRIETOR to "Fərdi sahibkar",
            ).map { it.first.toString() to it.second },
            selectedId = type.toString(),
            onSelect = { type = it.toInt() },
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(voen, { voen = it }, "VÖEN")
        Spacer(Modifier.height(10.dp))
        OpsTextField(director, { director = it }, "Rəhbər / əlaqə şəxsi")
        Spacer(Modifier.height(10.dp))
        OpsTextField(phone, { phone = it }, "Telefon", keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(10.dp))
        OpsTextField(email, { email = it }, "E-poçt", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        OpsTextField(address, { address = it }, "Ünvan", singleLine = false)
        Spacer(Modifier.height(10.dp))
        OpsTextField(note, { note = it }, "Qeyd", singleLine = false)
        (localError ?: error)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = if (party == null) "Yarat" else "Saxla",
            onCancel = onDismiss,
            onSubmit = {
                if (name.isBlank()) {
                    localError = "Ad tələb olunur."
                } else {
                    localError = null
                    onSave(name, type, voen, director, email, phone, address, note)
                }
            },
            enabled = name.isNotBlank(),
        )
    }
}

@Composable
private fun CustomerChargeSheet(
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String?) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(10).toString()) }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(title = "Müştəriyə borc yaz", onDismiss = onDismiss) {
        OpsTextField(description, { description = it }, "Təsvir", placeholder = "Məs. əlavə xidmət haqqı")
        Spacer(Modifier.height(10.dp))
        OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(10.dp))
        OpsDateField("Son ödəniş tarixi", dueDate, { dueDate = it })
        (localError ?: error)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = saving,
            submitLabel = "Borc yaz",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                if (description.isBlank()) {
                    localError = "Təsvir tələb olunur."
                } else if (value == null || value <= 0) {
                    localError = "Məbləğ 0-dan böyük olmalıdır."
                } else {
                    localError = null
                    onSave(description, value, dueDate.takeIf { it.isNotBlank() })
                }
            },
            enabled = description.isNotBlank() && amount.isNotBlank(),
        )
    }
}

@Composable
internal fun OpsPaymentSheet(
    title: String,
    hint: String,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPay: (Double, Int, String?) -> Unit,
    initialAmount: String = "",
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(title = title, onDismiss = onDismiss) {
        Text(hint, fontSize = 12.sp, color = OpsColors.Muted)
        Spacer(Modifier.height(10.dp))
        OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)
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
                    localError = "Məbləğ müsbət olmalıdır."
                } else {
                    localError = null
                    onPay(value, method, notes)
                }
            },
            enabled = amount.isNotBlank(),
        )
    }
}
