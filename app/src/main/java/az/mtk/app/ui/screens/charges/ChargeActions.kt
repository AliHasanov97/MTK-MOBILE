package az.mtk.app.ui.screens.charges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ChargeAllocationDto
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.ChargeGenerateBody
import az.mtk.app.data.api.ChargeStatus
import az.mtk.app.data.api.CreatePaymentRequestDto
import az.mtk.app.data.api.GenerateChargesResultDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.OwnerBalanceDto
import az.mtk.app.data.api.CompanyBalanceDto
import az.mtk.app.data.api.PaymentsApi
import az.mtk.app.data.api.PaymentMethods
import az.mtk.app.data.api.PropertyBalanceDto
import az.mtk.app.ui.components.ConfirmDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Color
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Action layer of the Borclar module — mirrors the write/allocations/balance
 * parts of MTK-UI app/lib/api/payments.ts and the modals of BorclarView.
 */
object ChargeActions {

    private fun api(): PaymentsApi = AppGraph.service(PaymentsApi::class.java)

    /** POST api/payments/charges/generate */
    suspend fun generateChargesForPeriod(period: String): GenerateChargesResultDto {
        val env = api().generateChargesForPeriod(ChargeGenerateBody(period))
        return env.data ?: throw az.mtk.app.data.api.ApiException(env.message ?: "Borclar yaradıla bilmədi")
    }

    /** POST api/payments/charges/{id}/cancel */
    suspend fun cancelCharge(chargeId: String) {
        api().cancelCharge(chargeId)
    }

    /** POST api/payments/payments */
    suspend fun createPayment(request: CreatePaymentRequestDto) {
        val env = api().createPayment(request)
        if (env.data == null && env.message != null) throw az.mtk.app.data.api.ApiException(env.message)
    }

    /** GET api/payments/charges/{id}/allocations */
    suspend fun chargeAllocations(chargeId: String): List<ChargeAllocationDto> =
        api().chargeAllocations(chargeId).data.orEmpty()

    /** GET api/payments/balances/owner/{id} */
    suspend fun ownerBalance(ownerId: String): OwnerBalanceDto = api().ownerBalance(ownerId).data
        ?: throw az.mtk.app.data.api.ApiException("Balans tapılmadı")

    /** GET api/payments/balances/property/{id} */
    suspend fun propertyBalance(propertyId: String): PropertyBalanceDto? = api().propertyBalance(propertyId).data

    /** GET api/payments/balances/company */
    suspend fun companyBalance(): CompanyBalanceDto = api().companyBalance().data
        ?: throw az.mtk.app.data.api.ApiException("Şirkət balansı tapılmadı")
}

private val paymentMethodOptions = listOf(
    MtkOption(PaymentMethods.CASH.toString(), "Nağd"),
    MtkOption(PaymentMethods.BANK_TRANSFER.toString(), "Bank köçürməsi"),
    MtkOption(PaymentMethods.CARD.toString(), "Kart"),
)

private val monthOptions = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
    "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
).mapIndexed { index, name -> MtkOption((index + 1).toString(), name) }

// ---- GenerateChargesModal equivalent ----------------------------------------

@Composable
fun GenerateChargesSheet(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val now = java.time.LocalDate.now()
    var year by remember { mutableStateOf(now.year.toString()) }
    var month by remember { mutableStateOf((now.monthValue).toString()) }
    var saving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<GenerateChargesResultDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val yearInt = year.toIntOrNull()
    val period = if (yearInt != null) "$yearInt-${month.padStart(2, '0')}" else ""
    val periodLabel = "${monthOptions.firstOrNull { it.value == month.toIntOrNull()?.toString() }?.label ?: "—"} $year"

    MtkModal(
        title = "Dövr üçün hesab yarat",
        subtitle = "Seçilmiş dövr üçün hələ borcu olmayan mənzil, qaraj və müqavilə xidmətlərinə borc yaradılır.",
        onDismiss = onDismiss,
        confirmEnabled = yearInt in 2000..2100,
        confirmLoading = saving,
        confirmLabel = if (result == null) "$periodLabel üçün yarat" else "Bağla",
        onConfirm = {
            if (result != null) {
                onDone(period)
            } else {
                scope.launch {
                    saving = true
                    error = null
                    runCatching { ChargeActions.generateChargesForPeriod(period) }
                        .onSuccess { result = it }
                        .onFailure { error = it.toUserMessage() }
                    saving = false
                }
            }
        },
    ) {
        Text(
            "Artıq borcu olanlara toxunulmur, ona görə əməliyyatı təkrar etmək zərərsizdir.",
            style = MaterialTheme.typography.bodySmall,
            color = MtkColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkSelect(
                value = month,
                onValueChange = { month = it },
                label = "Ay",
                options = monthOptions,
                modifier = Modifier.weight(1f),
                required = true,
            )
            MtkNumberField(
                value = year,
                onValueChange = { year = it },
                label = "İl",
                modifier = Modifier.weight(1f),
                required = true,
            )
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        result?.let { res ->
            val created = (res.residentChargesCreated ?: 0) + (res.vendorChargesCreated ?: 0)
            Spacer(Modifier.height(14.dp))
            MtkCard(containerColor = MtkColors.PositiveBg, borderColor = MtkColors.Positive.copy(alpha = 0.3f)) {
                Text(
                    "$periodLabel dövrü üçün $created hesab yaradıldı.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MtkColors.Positive,
                )
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Sakin hesabları", (res.residentChargesCreated ?: 0).toString())
            InfoRow("Tədarükçü hesabları", (res.vendorChargesCreated ?: 0).toString())
            val skipped = (res.apartmentsSkippedNoRate ?: 0) + (res.garagesSkippedNoRate ?: 0)
            if (skipped > 0) {
                Text(
                    "Tarif tapılmadığı üçün borc yazılmayanlar: ${res.apartmentsSkippedNoRate} mənzil, ${res.garagesSkippedNoRate} qaraj.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MtkColors.Negative,
                )
            }
        }
    }
}

// ---- PayButton / payment modal equivalent ------------------------------------

@Composable
fun PaymentSheet(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var owner by remember { mutableStateOf<OwnerItemDto?>(null) }
    var properties by remember { mutableStateOf<List<MtkOption>>(emptyList()) }
    var target by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethods.CASH.toString()) }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(owner?.id) {
        target = ""
        properties = emptyList()
        val id = owner?.id ?: return@LaunchedEffect
        runCatching { AppGraph.repository.owner(id) }
            .onSuccess { detail ->
                properties = buildList {
                    add(MtkOption("", "Ümumi (sahibin bütün borcları)"))
                    detail.apartments.orEmpty().forEach {
                        add(MtkOption("apartment:${it.id}", "Mənzil ${it.apartmentNumber ?: "—"} — ${it.building?.name ?: "—"}"))
                    }
                    detail.garages.orEmpty().forEach {
                        add(MtkOption("garage:${it.id}", "Qaraj ${it.garageNumber ?: "—"} (${Format.garageType(it.type)})"))
                    }
                }
            }
            .onFailure { error = it.toUserMessage() }
    }

    val numericAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0

    MtkModal(
        title = "Ödəniş qəbul et",
        subtitle = "Ödəniş avtomatik olaraq açıq borclara bölgülənir.",
        onDismiss = onDismiss,
        confirmLabel = "Qəbul et",
        confirmEnabled = owner != null && numericAmount > 0,
        confirmLoading = saving,
        onConfirm = {
            val selectedOwner = owner ?: return@MtkModal
            scope.launch {
                saving = true
                error = null
                val (kind, propertyId) = if (target.contains(":")) {
                    target.split(":", limit = 2).let { it[0] to it[1] }
                } else {
                    "" to ""
                }
                runCatching {
                    ChargeActions.createPayment(
                        CreatePaymentRequestDto(
                            ownerId = selectedOwner.id,
                            amount = numericAmount,
                            paymentMethod = method.toIntOrNull() ?: PaymentMethods.CASH,
                            notes = notes.trim().ifBlank { null },
                            apartmentId = if (kind == "apartment") propertyId else null,
                            garageId = if (kind == "garage") propertyId else null,
                        ),
                    )
                }
                    .onSuccess { onDone("Ödəniş qəbul edildi") }
                    .onFailure { error = it.toUserMessage() }
                saving = false
            }
        },
    ) {
        SearchableSelect(
            label = "Sahib",
            required = true,
            selectedLabel = owner?.fullName,
            searchPlaceholder = "Sahib axtar…",
            onLoad = { query -> AppGraph.repository.owners(0, 20, search = query).items },
            itemLabel = { it.fullName ?: it.id },
            onSelect = { owner = it },
            onClear = { owner = null },
        )
        if (owner != null) {
            Spacer(Modifier.height(12.dp))
            MtkSelect(
                value = target,
                onValueChange = { target = it },
                label = "Əmlak (istəyə görə)",
                options = properties,
                required = false,
            )
        }
        Spacer(Modifier.height(12.dp))
        MtkNumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Məbləğ (₼)",
            required = true,
        )
        Spacer(Modifier.height(12.dp))
        MtkSelect(
            value = method,
            onValueChange = { method = it },
            label = "Ödəniş üsulu",
            options = paymentMethodOptions,
            required = true,
        )
        Spacer(Modifier.height(12.dp))
        MtkTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Qeyd (istəyə görə)",
        )
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
    }
}

// ---- Charge detail (allocations + balances + receipt) -------------------------

data class ChargeDetailData(
    val allocations: List<ChargeAllocationDto> = emptyList(),
    val ownerBalance: OwnerBalanceDto? = null,
    val propertyBalance: PropertyBalanceDto? = null,
    val companyBalance: CompanyBalanceDto? = null,
)

@Composable
fun ChargeDetailSheet(
    charge: ChargeDto,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onOpenOwner: (String) -> Unit,
    onCancelled: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ChargeDetailData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCancel by remember { mutableStateOf(false) }

    LaunchedEffect(charge.id) {
        coroutineScope {
            detail = try {
                ChargeDetailData(
                    allocations = ChargeActions.chargeAllocations(charge.id),
                    ownerBalance = charge.ownerId?.let { runCatching { ChargeActions.ownerBalance(it) }.getOrNull() },
                    propertyBalance = (charge.apartmentId ?: charge.garageId)
                        ?.let { runCatching { ChargeActions.propertyBalance(it) }.getOrNull() },
                    companyBalance = runCatching { ChargeActions.companyBalance() }.getOrNull(),
                )
            } catch (t: Throwable) {
                error = t.toUserMessage()
                null
            }
        }
    }

    MtkModal(title = "Haqqın detalları", onDismiss = onDismiss) {
        val outstanding = ((charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)).coerceAtLeast(0.0)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Sahib balansı",
                value = Format.money(detail?.ownerBalance?.currentBalance),
                caption = "Borc: ${Format.money(detail?.ownerBalance?.totalDebt)}",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Əmlak balansı",
                value = Format.money(detail?.propertyBalance?.currentBalance),
                caption = "Borc: ${Format.money(detail?.propertyBalance?.totalDebt)}",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        StatTile(
            label = "Şirkət balansı",
            value = Format.money(detail?.companyBalance?.currentBalance),
            caption = "Gəlir: ${Format.money(detail?.companyBalance?.totalIncome)} · Xərc: ${Format.money(detail?.companyBalance?.totalExpense)}",
        )

        KeyValueHeader("Haqq məlumatı")
        InfoRow("Sakin", charge.partyName ?: "—")
        InfoRow("Əmlak", charge.propertyLabel ?: "—")
        InfoRow("Dövr", Format.period(charge.period))
        InfoRow("Məbləğ", Format.money(charge.amount))
        InfoRow("Ödənilib", Format.money(charge.paidAmount))
        InfoRow("Qalıq borc", Format.money(outstanding))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Status", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
            Pill(Format.chargeStatus(charge.status), chargeStatusTone(charge.status))
        }
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }

        KeyValueHeader("Ödəniş bölgüsü")
        when {
            detail == null -> LoadingState("Ödəniş məlumatları yüklənir…")
            detail!!.allocations.isEmpty() -> Text(
                "Bu haqqa hələ ödəniş tətbiq olunmayıb.",
                style = MaterialTheme.typography.bodySmall,
                color = MtkColors.Muted,
            )
            else -> detail!!.allocations.forEach { allocation ->
                MtkCard(padding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Format.dateTime(allocation.paymentDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Ödənişdən sonra qalıq: ${Format.money(allocation.remainingDebtAfterPayment)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MtkColors.MutedSoft,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                Format.money(allocation.allocatedAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Pill(
                                if (allocation.isFromAdvance == true) "Avansdan" else "Birbaşa ödəniş",
                                if (allocation.isFromAdvance == true) Tone.Warning else Tone.Success,
                            )
                        }
                    }
                    if (allocation.paymentId != null) {
                        Spacer(Modifier.height(6.dp))
                        ExportActionRow(
                            title = "Ödəniş qəbzi",
                            subtitle = "PDF qəbzi endir və aç",
                            path = "api/payments/payments/${allocation.paymentId}/receipt",
                            scope = scope,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        val cancellable = canManage &&
            charge.status != ChargeStatus.PAID &&
            charge.status != ChargeStatus.CANCELLED
        if (cancellable) {
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { showCancel = true },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MtkColors.Negative,
                    contentColor = Color.White,
                ),
            ) {
                Text("Haqqı ləğv et", fontWeight = FontWeight.Bold)
            }
        }
        charge.ownerId?.let { ownerId ->
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Sahibə bax", onClick = { onOpenOwner(ownerId) })
        }
    }

    if (showCancel) {
        CancelChargeDialog(
            charge = charge,
            onDismiss = { showCancel = false },
            onConfirmed = {
                showCancel = false
                onCancelled(charge.id)
            },
        )
    }
}

// ---- Cancel charge confirmation ------------------------------------------------

@Composable
fun CancelChargeDialog(
    charge: ChargeDto,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ConfirmDialog(
        title = "Haqqı ləğv et",
        message = buildString {
            append("${charge.partyName ?: "Sakin"} — ${Format.period(charge.period)} — ${Format.money(charge.amount)}.")
            append(" Bu haqqı ləğv etmək istəyirsiniz?")
            if (error != null) append("\n\nXəta: $error")
        },
        confirmLabel = if (busy) "Ləğv olunur…" else "Ləğv et",
        danger = true,
        onDismiss = { if (!busy) onDismiss() },
        onConfirm = {
            if (busy) return@ConfirmDialog
            scope.launch {
                busy = true
                error = null
                runCatching { ChargeActions.cancelCharge(charge.id) }
                    .onSuccess { onConfirmed() }
                    .onFailure { error = it.toUserMessage() }
                busy = false
            }
        },
    )
    error?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MtkColors.Negative,
        )
    }
}

fun chargeStatusTone(status: Int?): Tone = when (status) {
    ChargeStatus.UNPAID -> Tone.Danger
    ChargeStatus.PARTIALLY_PAID -> Tone.Warning
    ChargeStatus.PAID -> Tone.Success
    else -> Tone.Neutral
}
