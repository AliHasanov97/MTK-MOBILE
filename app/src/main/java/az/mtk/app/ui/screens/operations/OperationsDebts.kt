package az.mtk.app.ui.screens.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.DebtScope
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.util.Format as Fmt
import java.time.YearMonth

private data class OwnerGroup(
    val ownerId: String,
    val ownerName: String,
    val charges: List<ChargeDto>,
    val outstanding: Double,
)

@Composable
internal fun DebtsTab(
    state: DebtsViewModel.UiState,
    viewModel: DebtsViewModel,
) {
    var showGenerate by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var payGroup by remember { mutableStateOf<OwnerGroup?>(null) }
    var payCharge by remember { mutableStateOf<ChargeDto?>(null) }
    var cancelTarget by remember { mutableStateOf<ChargeDto?>(null) }

    val groups = remember(state.charges) {
        state.charges
            .groupBy { it.ownerId ?: "—" }
            .map { (ownerId, charges) ->
                OwnerGroup(
                    ownerId = ownerId,
                    ownerName = charges.firstOrNull()?.partyName?.takeIf { it.isNotBlank() } ?: "Sakin",
                    charges = charges,
                    outstanding = charges
                        .filter { it.status == 0 || it.status == 1 }
                        .sumOf { (it.amount ?: 0.0) - (it.paidAmount ?: 0.0) },
                )
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OpsSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Sakin, əmlak və ya təsvir…",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        OpsPillRow(
            pills = DebtScope.entries.map { scope ->
                OpsPill(
                    label = scope.label,
                    selected = state.scope == scope,
                    onClick = { viewModel.setScope(scope) },
                )
            },
        )
        Spacer(Modifier.height(8.dp))

        state.message?.let { OpsFeedback(it, isError = false) { viewModel.clearFeedback() } }
        state.actionError?.let { OpsFeedback(it, isError = true) { viewModel.clearFeedback() } }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.total} borc",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isAdmin) {
                    OpsSecondaryButton(
                        text = "Dövr yarat",
                        onClick = { showGenerate = true },
                    )
                }
                if (state.canManage) {
                    OpsPrimaryButton(
                        text = "+ Borc",
                        onClick = { showAdd = true },
                    )
                }
            }
        }

        when {
            state.loading && state.charges.isEmpty() -> OpsLoading()
            state.error != null && state.charges.isEmpty() ->
                OpsError(state.error!!) { viewModel.load() }
            groups.isEmpty() -> OpsEmpty(
                title = "Borc tapılmadı",
                text = if (state.scope == DebtScope.OPEN) {
                    "Açıq borc yoxdur — bütün haqlar ödənilib."
                } else {
                    "Filtrləri dəyişin və ya yeni borc yaradın."
                },
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
                items(groups, key = { it.ownerId }) { group ->
                    OwnerGroupCard(
                        group = group,
                        state = state,
                        viewModel = viewModel,
                        canPay = state.canPay,
                        canManage = state.canManage,
                        onPayGroup = { payGroup = group },
                        onPayCharge = { payCharge = it },
                        onCancel = { cancelTarget = it },
                    )
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

    if (showGenerate) {
        GenerateChargesSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                showGenerate = false
                viewModel.closeGenerate()
            },
        )
    }

    if (showAdd) {
        AddChargeSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                showAdd = false
                viewModel.resetChargeForm()
            },
        )
    }

    payGroup?.let { group ->
        OpsPaymentSheet(
            title = "Ödəniş qəbul et",
            hint = "${group.ownerName} · açıq borc ${Fmt.money(group.outstanding)}",
            saving = state.busy,
            error = state.formError,
            onDismiss = {
                payGroup = null
                viewModel.clearFormError()
            },
            onPay = { amount, method, notes ->
                viewModel.pay(
                    ownerId = group.ownerId,
                    amount = amount,
                    method = method,
                    notes = notes,
                    apartmentId = null,
                    garageId = null,
                    onDone = { payGroup = null },
                )
            },
        )
    }

    payCharge?.let { charge ->
        OpsPaymentSheet(
            title = "Ödəniş qəbul et",
            hint = "${charge.description ?: "Borc"} · qalıq ${
                Fmt.money((charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0))
            }",
            initialAmount = ((charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)).toString(),
            saving = state.busy,
            error = state.formError,
            onDismiss = {
                payCharge = null
                viewModel.clearFormError()
            },
            onPay = { amount, method, notes ->
                viewModel.pay(
                    ownerId = charge.ownerId.orEmpty(),
                    amount = amount,
                    method = method,
                    notes = notes,
                    apartmentId = charge.apartmentId,
                    garageId = charge.garageId,
                    onDone = { payCharge = null },
                )
            },
        )
    }

    cancelTarget?.let { charge ->
        ConfirmDialog(
            title = "Borc ləğv edilsin?",
            text = "${charge.description ?: "Borc"} — ${Fmt.money(charge.amount)}. Ləğv edilmiş borc açıq borc sayılmır.",
            confirmLabel = "Ləğv et",
            danger = true,
            onDismiss = { cancelTarget = null },
            onConfirm = {
                cancelTarget = null
                viewModel.cancel(charge.id)
            },
        )
    }
}

@Composable
private fun OwnerGroupCard(
    group: OwnerGroup,
    state: DebtsViewModel.UiState,
    viewModel: DebtsViewModel,
    canPay: Boolean,
    canManage: Boolean,
    onPayGroup: () -> Unit,
    onPayCharge: (ChargeDto) -> Unit,
    onCancel: (ChargeDto) -> Unit,
) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.ownerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${group.charges.size} haqq · açıq ${Fmt.money(group.outstanding)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            if (canPay && group.outstanding > 0.005) {
                OpsPrimaryButton("Ödə", onPayGroup, enabled = !state.busy)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(8.dp))

        group.charges.forEachIndexed { index, charge ->
            if (index > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = OpsColors.Border)
                Spacer(Modifier.height(10.dp))
            }
            DebtChargeRow(
                charge = charge,
                allocations = state.allocations[charge.id],
                allocationsLoading = state.allocationsLoadingId == charge.id,
                canPay = canPay,
                canManage = canManage,
                busy = state.busy,
                onToggleAllocations = { viewModel.toggleAllocations(charge.id) },
                onPay = { onPayCharge(charge) },
                onCancel = { onCancel(charge) },
            )
        }
    }
}

@Composable
private fun DebtChargeRow(
    charge: ChargeDto,
    allocations: List<az.mtk.app.data.api.ChargeAllocationDto>?,
    allocationsLoading: Boolean,
    canPay: Boolean,
    canManage: Boolean,
    busy: Boolean,
    onToggleAllocations: () -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit,
) {
    val amount = charge.amount ?: 0.0
    val paid = charge.paidAmount ?: 0.0
    val outstanding = (amount - paid).coerceAtLeast(0.0)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = charge.description ?: "Borc",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = OpsColors.Ink,
                )
                Text(
                    text = listOfNotNull(
                        charge.propertyLabel?.takeIf { it.isNotBlank() },
                        charge.period?.takeIf { it.isNotBlank() }?.let { Fmt.period(it) },
                    ).joinToString(" · ").ifBlank { "—" },
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChargeStatusChip(charge.status)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Məbləğ ${Fmt.money(amount)}", fontSize = 11.sp, color = OpsColors.Muted)
            Text("Ödənilib ${Fmt.money(paid)}", fontSize = 11.sp, color = OpsColors.Olive)
            Text(
                "Qalıq ${Fmt.money(outstanding)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (outstanding > 0.005) OpsColors.Error else OpsColors.Olive,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onToggleAllocations) {
                Text(
                    text = if (allocations != null) "Bölgünü gizlət" else "Bölgü",
                    fontSize = 11.sp,
                    color = OpsColors.Terracotta,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.weight(1f))
            if (canPay && (charge.status == 0 || charge.status == 1) && outstanding > 0.005) {
                OpsPrimaryButton("Ödə", onPay, enabled = !busy)
            }
            if (canManage && (charge.status == 0 || charge.status == 1)) {
                OpsSecondaryButton("Ləğv et", onCancel, enabled = !busy, danger = true)
            }
        }

        when {
            allocationsLoading -> {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bölgü yüklənir…", fontSize = 11.sp, color = OpsColors.MutedSoft)
                }
            }
            allocations != null -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = OpsColors.Subtle,
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (allocations.isEmpty()) {
                            Text(
                                "Bu borca hələ ödəniş bölünməyib.",
                                fontSize = 11.sp,
                                color = OpsColors.MutedSoft,
                            )
                        } else {
                            allocations.forEach { allocation ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            Fmt.money(allocation.allocatedAmount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpsColors.Ink,
                                        )
                                        Text(
                                            buildString {
                                                append(Fmt.date(allocation.paymentDate))
                                                if (allocation.isFromAdvance == true) append(" · avansdan")
                                            },
                                            fontSize = 10.sp,
                                            color = OpsColors.MutedSoft,
                                        )
                                    }
                                    Text(
                                        "qalıq ${Fmt.money(allocation.remainingDebtAfterPayment)}",
                                        fontSize = 10.sp,
                                        color = OpsColors.Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateChargesSheet(
    state: DebtsViewModel.UiState,
    viewModel: DebtsViewModel,
    onDismiss: () -> Unit,
) {
    val default = remember { YearMonth.now().minusMonths(1) }
    var year by remember { mutableStateOf(default.year) }
    var month by remember { mutableStateOf(default.monthValue) }

    OpsFormSheet(title = "Dövr üçün borcları yarat", onDismiss = onDismiss) {
        Text(
            "Seçilmiş dövr üçün tariflərə əsasən sakin və tədarükçü borcları yaradılır. " +
                "Artıq yaradılmış dövr üçün təkrar yaratmaq tövsiyə olunmur.",
            fontSize = 12.sp,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSelectField(
                label = "İl",
                options = (default.year - 2..default.year + 1).map { it.toString() to it.toString() },
                selectedId = year.toString(),
                onSelect = { year = it.toInt() },
                modifier = Modifier.weight(1f),
            )
            OpsSelectField(
                label = "Ay",
                options = (1..12).map { it.toString() to Fmt.period("2000-${it.toString().padStart(2, '0')}").substringBefore(" 2000") },
                selectedId = month.toString(),
                onSelect = { month = it.toInt() },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = OpsColors.Container,
        ) {
            Text(
                text = "Dövr: ${Fmt.period("$year-${month.toString().padStart(2, '0')}")}",
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Terracotta,
            )
        }
        state.generateError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        state.generateResult?.let { result ->
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.OliveSoft,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Nəticə", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpsColors.Olive)
                    Text("Sakin borcları: ${result.residentChargesCreated ?: 0}", fontSize = 11.sp, color = OpsColors.Ink)
                    Text("Tədarükçü borcları: ${result.vendorChargesCreated ?: 0}", fontSize = 11.sp, color = OpsColors.Ink)
                    if ((result.apartmentsSkippedNoRate ?: 0) > 0) {
                        Text(
                            "Tarifsiz mənzillər: ${result.apartmentsSkippedNoRate}",
                            fontSize = 11.sp,
                            color = OpsColors.Amber,
                        )
                    }
                    if ((result.garagesSkippedNoRate ?: 0) > 0) {
                        Text(
                            "Tarifsiz qarajlar: ${result.garagesSkippedNoRate}",
                            fontSize = 11.sp,
                            color = OpsColors.Amber,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.generating,
            submitLabel = "Yarat",
            onCancel = onDismiss,
            onSubmit = {
                viewModel.generate("$year-${month.toString().padStart(2, '0')}")
            },
            enabled = state.generateResult == null,
        )
    }
}

@Composable
private fun AddChargeSheet(
    state: DebtsViewModel.UiState,
    viewModel: DebtsViewModel,
    onDismiss: () -> Unit,
) {
    var ownerQuery by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var property by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ownerQuery) {
        kotlinx.coroutines.delay(350)
        viewModel.loadOwners(ownerQuery)
    }

    val selected = state.selectedOwner
    val detail: OwnerDetailDto? = state.selectedOwnerDetail

    // Sahib dəyişəndə əvvəlki əmlak seçimi qalmamalıdır — əks halda başqa
    // sakinin mənzili göndərilir və backend "əmlak bu sahibə aid deyil" qaytarır.
    LaunchedEffect(selected?.id) {
        property = ""
        localError = null
        viewModel.clearFormError()
    }

    val propertyOptions = buildList {
        add("" to "Ümumi (konkret mənzil/qaraja bağlı deyil)")
        detail?.apartments.orEmpty().forEach { apartment ->
            val label = buildString {
                append("Mənzil ")
                append(apartment.apartmentNumber ?: "—")
                apartment.building?.name?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            add("apartment:${apartment.id}" to label)
        }
        detail?.garages.orEmpty().forEach { garage ->
            val label = buildString {
                append("Qaraj ")
                append(garage.garageNumber ?: "—")
                garage.type?.takeIf { it.isNotBlank() }?.let { append(" · ${Fmt.garageType(it)}") }
            }
            add("garage:${garage.id}" to label)
        }
    }

    OpsFormSheet(title = "Sakinə borc yaz", onDismiss = onDismiss) {
        if (selected == null) {
            OpsTextField(
                ownerQuery,
                { ownerQuery = it },
                "Sakin axtar",
                placeholder = "Ad, telefon və ya e-poçt",
            )
            Spacer(Modifier.height(6.dp))
            when {
                state.ownerOptionsLoading -> Text(
                    "Axtarılır…",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                state.ownerOptionsError != null -> Text(
                    state.ownerOptionsError,
                    fontSize = 11.sp,
                    color = OpsColors.Error,
                )
                state.ownerOptions.isEmpty() -> Text(
                    "Sakin tapılmadı.",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.ownerOptions.take(8).forEach { owner ->
                        OwnerOptionRow(owner) { viewModel.selectOwner(owner) }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = OpsColors.Container,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selected.fullName ?: selected.id,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpsColors.Ink,
                        )
                        Text(
                            selected.phoneNumber ?: selected.email ?: "",
                            fontSize = 11.sp,
                            color = OpsColors.MutedSoft,
                        )
                    }
                    TextButton(onClick = { viewModel.selectOwner(null) }) {
                        Text("Dəyiş", fontSize = 11.sp, color = OpsColors.Terracotta)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (state.ownerDetailLoading) {
                Text("Əmlaklar yüklənir…", fontSize = 11.sp, color = OpsColors.MutedSoft)
            } else {
                OpsSelectField(
                    label = "Əmlak",
                    options = propertyOptions,
                    selectedId = property,
                    onSelect = { property = it },
                    placeholder = "Əmlak seçin…",
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        OpsTextField(description, { description = it }, "Təsvir", placeholder = "Məs. cari təmir haqqı")
        Spacer(Modifier.height(10.dp))
        OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)

        (localError ?: state.formError)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Borc yaz",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                val ownerId = selected?.id
                when {
                    ownerId == null -> localError = "Sakin seçilməlidir."
                    description.isBlank() -> localError = "Təsvir tələb olunur."
                    value == null || value <= 0 -> localError = "Məbləğ 0-dan böyük olmalıdır."
                    else -> {
                        localError = null
                        viewModel.addCharge(
                            ownerId = ownerId,
                            apartmentId = property.takeIf { it.startsWith("apartment:") }
                                ?.removePrefix("apartment:"),
                            garageId = property.takeIf { it.startsWith("garage:") }
                                ?.removePrefix("garage:"),
                            amount = value,
                            description = description,
                            period = null,
                            onDone = {
                                onDismiss()
                            },
                        )
                    }
                }
            },
            enabled = selected != null && description.isNotBlank() && amount.isNotBlank(),
        )
    }
}

@Composable
private fun OwnerOptionRow(owner: OwnerItemDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = OpsColors.Subtle,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                owner.fullName ?: owner.id,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OpsColors.Ink,
            )
            val meta = listOfNotNull(owner.phoneNumber, owner.email).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, color = OpsColors.MutedSoft)
            }
        }
    }
}
