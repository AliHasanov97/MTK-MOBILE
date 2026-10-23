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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.FinCreateTransactionRequest
import az.mtk.app.data.api.FinGarageType
import az.mtk.app.data.api.FinPaymentResponse
import az.mtk.app.data.api.FinRateResponse
import az.mtk.app.data.api.FinRateType
import az.mtk.app.data.api.FinTransactionDirection
import az.mtk.app.data.api.FinTransactionResponse
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvPurchaseResponse
import az.mtk.app.data.api.OpsPayrollRunDto
import az.mtk.app.data.api.OpsPayslipDto
import az.mtk.app.data.api.OpsVacationPayDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.PaymentMethod
import az.mtk.app.data.api.PayrollStatus
import az.mtk.app.data.api.VacationPayStatus
import az.mtk.app.util.Format
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

private val EXPENSE_CATEGORIES = listOf(
    "Azərişıq (elektrik)",
    "Sukanal (su)",
    "Azəriqaz (qaz)",
    "İstilik",
    "Zibil daşınması",
    "Təmizlik xidməti",
    "Mühafizə / təhlükəsizlik",
    "Lift xidməti",
    "Cari təmir",
    "Əsaslı təmir",
    "Həyətin abadlaşdırılması",
    "Bank xidmət haqqı",
    "Sığorta",
    "Hüquqi / mühasibat xidməti",
    "Ofis xərcləri",
    "Digər",
)

private val INCOME_CATEGORIES = listOf(
    "Ümumi sahələrin icarəsi",
    "Avtodayanacaq (qonaq) haqqı",
    "Reklam / antena yerləşdirilməsi",
    "Cərimə",
    "Bank faizi",
    "Sponsorluq / ianə",
    "Satılan materialın dəyəri",
    "Digər",
)

private fun rateTypeLabel(type: Int?): String = when (type) {
    FinRateType.PER_SQUARE_METER -> "m² üzrə"
    FinRateType.FIXED_GARAGE -> "Qaraj (sabit)"
    FinRateType.MANUAL -> "Əl ilə"
    else -> "—"
}

private fun garageTypeLabel(type: Int?): String = when (type) {
    FinGarageType.OPEN_PARKING -> "Açıq dayanacaq"
    FinGarageType.COVERED_GARAGE -> "Örtülü qaraj"
    FinGarageType.STORAGE -> "Anbar yeri"
    else -> "—"
}

private fun sectionIcon(section: FinanceSection): ImageVector = when (section) {
    FinanceSection.PAYMENTS -> Icons.Default.Payments
    FinanceSection.EXPENSES -> Icons.Default.ReceiptLong
    FinanceSection.INCOMES -> Icons.Default.TrendingUp
    FinanceSection.RATES -> Icons.Default.PriceChange
    FinanceSection.PURCHASES -> Icons.Default.ShoppingCart
    FinanceSection.PAYROLL -> Icons.Default.AccountBalanceWallet
    FinanceSection.VACATION -> Icons.Default.BeachAccess
}

private fun sectionTint(section: FinanceSection): Pair<Color, Color> = when (section) {
    FinanceSection.PAYMENTS -> OpsColors.Olive to OpsColors.OliveSoft
    FinanceSection.EXPENSES -> OpsColors.Error to OpsColors.ErrorSoft
    FinanceSection.INCOMES -> OpsColors.Olive to OpsColors.OliveSoft
    FinanceSection.RATES -> OpsColors.Blue to OpsColors.BlueSoft
    FinanceSection.PURCHASES -> OpsColors.Amber to OpsColors.AmberSoft
    FinanceSection.PAYROLL -> OpsColors.Terracotta to OpsColors.Container
    FinanceSection.VACATION -> OpsColors.Blue to OpsColors.BlueSoft
}

/* ====================================================================== */
/* Hub                                                                    */
/* ====================================================================== */

@Composable
internal fun FinanceHubTab(viewModel: FinanceViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 4.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text(
                    "Maliyyə əməliyyatları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ödəniş, xərc, gəlir, tarif, satınalma, əmək haqqı və məzuniyyət " +
                        "əməliyyatları bir yerdə. Hər bölmə ayrıca mobil ekran kimi açılır.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
            }
        }
        items(FinanceSection.entries, key = { it.name }) { section ->
            val (fg, bg) = sectionTint(section)
            OpsCard(onClick = { viewModel.openSection(section) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(bg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(sectionIcon(section), null, tint = fg)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            section.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpsColors.Ink,
                        )
                        Text(
                            section.description,
                            fontSize = 11.sp,
                            color = OpsColors.MutedSoft,
                        )
                    }
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
}

/* ====================================================================== */
/* Bölmə ekranı                                                           */
/* ====================================================================== */

@Composable
internal fun FinanceSectionScreen(
    section: FinanceSection,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    canManage: Boolean,
    canPay: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(section) {
        if (viewModel.state.value.section != section) viewModel.openSection(section)
    }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = section.label,
                subtitle = section.description,
                onBack = onBack,
                onRefresh = viewModel::refresh,
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

            when (section) {
                FinanceSection.PAYMENTS -> PaymentsSection(state, viewModel, canPay)
                FinanceSection.EXPENSES -> ExpensesSection(state, viewModel, canManage)
                FinanceSection.INCOMES -> IncomesSection(state, viewModel, canManage)
                FinanceSection.RATES -> RatesSection(state, viewModel, canManage)
                FinanceSection.PURCHASES -> PurchasesSection(
                    canManage = canManage,
                    canDownload = canPay || canManage,
                )
                FinanceSection.PAYROLL -> PayrollSection(state, viewModel, canManage)
                FinanceSection.VACATION -> VacationSection(state, viewModel, canManage)
            }
        }
    }
}

/* ====================================================================== */
/* Ödənişlər                                                              */
/* ====================================================================== */

@Composable
private fun PaymentsSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canPay: Boolean,
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text(
                    "Sakin ödənişi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    "Ödəniş avtomatik olaraq sakinin açıq borclarına bölünür. " +
                        "Artıq məbləğ avans kimi saxlanılır.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
                if (canPay) {
                    Spacer(Modifier.height(10.dp))
                    OpsPrimaryButton("+ Yeni ödəniş", { showForm = true })
                }
            }
        }

        item { OpsSectionTitle("Son ödənişlər") }

        when {
            state.paymentsLoading && state.payments.isEmpty() -> item { OpsLoading() }
            state.paymentsError != null && state.payments.isEmpty() ->
                item { OpsError(state.paymentsError!!) { viewModel.loadPayments() } }
            state.payments.isEmpty() -> item {
                OpsCard {
                    Text("Ödəniş qeydə alınmayıb.", fontSize = 12.sp, color = OpsColors.MutedSoft)
                }
            }
            else -> items(state.payments, key = { it.id }) { payment ->
                FinancePaymentCard(payment)
            }
        }
    }

    if (showForm) {
        PaymentEntrySheet(state, viewModel) {
            showForm = false
            viewModel.clearFormError()
            viewModel.resetOwner()
        }
    }
}

@Composable
private fun FinancePaymentCard(payment: FinPaymentResponse) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    payment.partyName ?: "Sakin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    listOfNotNull(
                        payment.propertyLabel?.takeIf { it.isNotBlank() },
                        Format.date(payment.paymentDate),
                        payment.paymentMethod?.let(PaymentMethod::label),
                    ).joinToString(" · "),
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                if (!payment.notes.isNullOrBlank()) {
                    Text(payment.notes, fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.money(payment.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Olive,
                )
                OpsChip(
                    text = if (payment.status == 1) "Tamamlanıb" else "Gözləyir",
                    foreground = if (payment.status == 1) OpsColors.Olive else OpsColors.Amber,
                    background = if (payment.status == 1) OpsColors.OliveSoft else OpsColors.AmberSoft,
                )
            }
        }
    }
}

@Composable
private fun PaymentEntrySheet(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var property by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(350)
        viewModel.searchOwners(query)
    }

    val selected = state.selectedOwner
    val detail = state.selectedOwnerDetail

    OpsFormSheet(title = "Ödənişin daxil edilməsi", onDismiss = onDismiss) {
        if (selected == null) {
            OpsTextField(
                query,
                { query = it },
                "Sakin axtar",
                placeholder = "Ad, telefon və ya e-poçt",
            )
            Spacer(Modifier.height(6.dp))
            when {
                state.ownersLoading -> Text("Axtarılır…", fontSize = 11.sp, color = OpsColors.MutedSoft)
                state.ownersError != null -> Text(state.ownersError, fontSize = 11.sp, color = OpsColors.Error)
                state.ownerOptions.isEmpty() -> Text(
                    "Sakin tapılmadı.",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.ownerOptions.take(8).forEach { owner ->
                        OwnerPickRow(owner) { viewModel.selectOwner(owner) }
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

            if (state.ownerDetailLoading) {
                Spacer(Modifier.height(8.dp))
                Text("Əmlaklar yüklənir…", fontSize = 11.sp, color = OpsColors.MutedSoft)
            } else if (detail != null) {
                val propertyOptions = buildList {
                    add("" to "Ümumi (əmlaksız)")
                    detail.apartments.orEmpty().forEach { apartment ->
                        add(
                            "apartment:${apartment.id}" to buildString {
                                append("Mənzil ")
                                append(apartment.apartmentNumber ?: "—")
                                apartment.building?.name?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                            },
                        )
                    }
                    detail.garages.orEmpty().forEach { garage ->
                        add(
                            "garage:${garage.id}" to buildString {
                                append("Qaraj ")
                                append(garage.garageNumber ?: "—")
                                garage.type?.takeIf { it.isNotBlank() }?.let { append(" · ${Format.garageType(it)}") }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OpsSelectField(
                    label = "Ödənişin hədəfi",
                    options = propertyOptions,
                    selectedId = property,
                    onSelect = { property = it },
                )
            }
        }

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

        (localError ?: state.formError)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Ödənişi qeyd et",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                when {
                    selected == null -> localError = "Sakin seçilməlidir."
                    value == null || value <= 0 -> localError = "Məbləğ 0-dan böyük olmalıdır."
                    else -> {
                        localError = null
                        viewModel.createPayment(
                            amount = value,
                            method = method,
                            notes = notes,
                            apartmentId = property.takeIf { it.startsWith("apartment:") }
                                ?.removePrefix("apartment:"),
                            garageId = property.takeIf { it.startsWith("garage:") }
                                ?.removePrefix("garage:"),
                            onDone = onDismiss,
                        )
                    }
                }
            },
            enabled = selected != null && amount.isNotBlank(),
        )
    }
}

@Composable
private fun OwnerPickRow(owner: OwnerItemDto, onClick: () -> Unit) {
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

/* ====================================================================== */
/* Xərclər                                                                */
/* ====================================================================== */

@Composable
private fun ExpensesSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canManage: Boolean,
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text("Xərcin daxil edilməsi", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Text(
                    "Müqavilə xidməti üzrə birdəfəlik xərc və ya müqaviləsi olmayan sərbəst xərc.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
                if (canManage) {
                    Spacer(Modifier.height(10.dp))
                    OpsPrimaryButton("+ Yeni xərc", { showForm = true })
                }
            }
        }
        item { OpsSectionTitle("Son xərclər") }

        when {
            state.transactionsLoading && state.transactions.isEmpty() -> item { OpsLoading() }
            state.transactionsError != null && state.transactions.isEmpty() ->
                item { OpsError(state.transactionsError!!) { viewModel.loadTransactions(FinTransactionDirection.EXPENSE) } }
            state.transactions.isEmpty() -> item {
                OpsCard { Text("Xərc qeydə alınmayıb.", fontSize = 12.sp, color = OpsColors.MutedSoft) }
            }
            else -> items(state.transactions, key = { it.id }) { tx ->
                TransactionCard(tx, isIncome = false)
            }
        }
    }

    if (showForm) {
        ExpenseEntrySheet(state, viewModel) {
            showForm = false
            viewModel.clearFormError()
        }
    }
}

@Composable
private fun ExpenseEntrySheet(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(0) }
    var vendorId by remember { mutableStateOf("") }
    var serviceKey by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val selectedService = state.expenseServices.firstOrNull {
        "${it.contractId}|${it.serviceId}" == serviceKey
    }

    OpsFormSheet(title = "Xərcin daxil edilməsi", onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("Müqavilə xidməti", mode == 0, Modifier.weight(1f)) { mode = 0 }
            ModeChip("Sərbəst xərc", mode == 1, Modifier.weight(1f)) { mode = 1 }
        }
        Spacer(Modifier.height(12.dp))

        if (mode == 0) {
            OpsSelectField(
                label = "Tədarükçü",
                options = state.expenseVendors.map { it.id to it.name },
                selectedId = vendorId.ifBlank { null },
                onSelect = { id ->
                    vendorId = id
                    serviceKey = ""
                    amount = ""
                    viewModel.loadExpenseServices(id)
                },
                placeholder = if (state.expenseVendorsLoading) "Yüklənir…" else "Seçin…",
            )
            Spacer(Modifier.height(10.dp))
            if (state.expenseServicesLoading) {
                Text("Xidmətlər yüklənir…", fontSize = 11.sp, color = OpsColors.MutedSoft)
            } else {
                OpsSelectField(
                    label = "Birdəfəlik xidmət",
                    options = state.expenseServices.map {
                        "${it.contractId}|${it.serviceId}" to "${it.serviceName} · ${it.contractNumber}"
                    },
                    selectedId = serviceKey.ifBlank { null },
                    onSelect = { key ->
                        serviceKey = key
                        val option = state.expenseServices.firstOrNull {
                            "${it.contractId}|${it.serviceId}" == key
                        }
                        if (option != null && option.unitPrice > 0) {
                            amount = option.unitPrice.toString()
                        }
                    },
                    placeholder = if (vendorId.isBlank()) "Əvvəlcə tədarükçü seçin" else "Seçin…",
                    enabled = vendorId.isNotBlank(),
                )
            }
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
        } else {
            OpsSelectField(
                label = "Kateqoriya",
                options = EXPENSE_CATEGORIES.map { it to it },
                selectedId = category,
                onSelect = { category = it },
            )
            Spacer(Modifier.height(10.dp))
            OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(10.dp))
            OpsTextField(notes, { notes = it }, "Təsvir / qeyd", singleLine = false)
        }

        (localError ?: state.formError)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Qeydə al",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                when {
                    value == null || value <= 0 -> localError = "Məbləğ 0-dan böyük olmalıdır."
                    mode == 0 && selectedService == null -> localError = "Xidmət seçilməlidir."
                    mode == 0 -> {
                        localError = null
                        val service = selectedService!!
                        viewModel.createOneTimeExpense(
                            contractId = service.contractId,
                            serviceId = service.serviceId,
                            amount = value,
                            method = method,
                            notes = notes,
                            onDone = onDismiss,
                        )
                    }
                    else -> {
                        localError = null
                        viewModel.createTransaction(
                            direction = FinTransactionDirection.EXPENSE,
                            category = category,
                            amount = value,
                            description = notes,
                            onDone = onDismiss,
                        )
                    }
                }
            },
            enabled = amount.isNotBlank(),
        )
    }
}

/* ====================================================================== */
/* Əlavə gəlirlər                                                         */
/* ====================================================================== */

@Composable
private fun IncomesSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canManage: Boolean,
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text("Əlavə gəlir", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Text(
                    "Sakin ödənişlərindən kənar gəlirlər: icarə, reklam, cərimə, bank faizi və s.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
                if (canManage) {
                    Spacer(Modifier.height(10.dp))
                    OpsPrimaryButton("+ Yeni gəlir", { showForm = true })
                }
            }
        }
        item { OpsSectionTitle("Son gəlirlər") }

        when {
            state.transactionsLoading && state.transactions.isEmpty() -> item { OpsLoading() }
            state.transactionsError != null && state.transactions.isEmpty() ->
                item { OpsError(state.transactionsError!!) { viewModel.loadTransactions(FinTransactionDirection.INCOME) } }
            state.transactions.isEmpty() -> item {
                OpsCard { Text("Əlavə gəlir qeydə alınmayıb.", fontSize = 12.sp, color = OpsColors.MutedSoft) }
            }
            else -> items(state.transactions, key = { it.id }) { tx ->
                TransactionCard(tx, isIncome = true)
            }
        }
    }

    if (showForm) {
        IncomeEntrySheet(state, viewModel) {
            showForm = false
            viewModel.clearFormError()
        }
    }
}

@Composable
private fun IncomeEntrySheet(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
) {
    var category by remember { mutableStateOf(INCOME_CATEGORIES.first()) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(title = "Əlavə gəlirin daxil edilməsi", onDismiss = onDismiss) {
        OpsSelectField(
            label = "Kateqoriya",
            options = INCOME_CATEGORIES.map { it to it },
            selectedId = category,
            onSelect = { category = it },
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(10.dp))
        OpsTextField(description, { description = it }, "Təsvir", singleLine = false)
        (localError ?: state.formError)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Qeydə al",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0) {
                    localError = "Məbləğ 0-dan böyük olmalıdır."
                } else {
                    localError = null
                    viewModel.createTransaction(
                        direction = FinTransactionDirection.INCOME,
                        category = category,
                        amount = value,
                        description = description,
                        onDone = onDismiss,
                    )
                }
            },
            enabled = amount.isNotBlank(),
        )
    }
}

@Composable
private fun TransactionCard(tx: FinTransactionResponse, isIncome: Boolean) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.category ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    Format.date(tx.transactionDate ?: tx.createdAt),
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                if (!tx.description.isNullOrBlank()) {
                    Text(tx.description, fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
            Text(
                text = (if (isIncome) "+" else "−") + Format.money(tx.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) OpsColors.Olive else OpsColors.Error,
            )
        }
    }
}

/* ====================================================================== */
/* Tariflər                                                               */
/* ====================================================================== */

@Composable
private fun RatesSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canManage: Boolean,
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text("Cari tariflər", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Text(
                    "Aylıq borc generasiyası bu tariflərə əsaslanır. Yeni tarif qüvvəyə mindiyi " +
                        "tarixdən etibarən tətbiq olunur.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
                if (canManage) {
                    Spacer(Modifier.height(10.dp))
                    OpsPrimaryButton("+ Yeni tarif", { showForm = true })
                }
            }
        }

        when {
            state.ratesLoading && state.rates.isEmpty() -> item { OpsLoading() }
            state.ratesError != null && state.rates.isEmpty() ->
                item { OpsError(state.ratesError!!) { viewModel.loadRates() } }
            state.rates.isEmpty() -> item {
                OpsCard { Text("Aktiv tarif yoxdur.", fontSize = 12.sp, color = OpsColors.MutedSoft) }
            }
            else -> items(state.rates, key = { it.id }) { rate -> RateCard(rate) }
        }
    }

    if (showForm) {
        RateSheet(state, viewModel) {
            showForm = false
            viewModel.clearFormError()
        }
    }
}

@Composable
private fun RateCard(rate: FinRateResponse) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rateTypeLabel(rate.rateType),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                if (rate.rateType == FinRateType.FIXED_GARAGE) {
                    Text(
                        garageTypeLabel(rate.garageType),
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                }
                Text(
                    "Qüvvədə: ${Format.date(rate.effectiveFrom)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                if (!rate.description.isNullOrBlank()) {
                    Text(rate.description, fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
            Text(
                Format.money(rate.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Terracotta,
            )
        }
    }
}

@Composable
private fun RateSheet(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
) {
    var rateType by remember { mutableStateOf(FinRateType.PER_SQUARE_METER) }
    var garageType by remember { mutableStateOf(FinGarageType.OPEN_PARKING) }
    var amount by remember { mutableStateOf("") }
    var effectiveFrom by remember { mutableStateOf(LocalDate.now().toString()) }
    var description by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    OpsFormSheet(title = "Yeni tarif", onDismiss = onDismiss) {
        OpsSelectField(
            label = "Tarif növü",
            options = listOf(
                FinRateType.PER_SQUARE_METER to "m² üzrə",
                FinRateType.FIXED_GARAGE to "Qaraj (sabit)",
            ).map { it.first.toString() to it.second },
            selectedId = rateType.toString(),
            onSelect = { rateType = it.toInt() },
        )
        if (rateType == FinRateType.FIXED_GARAGE) {
            Spacer(Modifier.height(10.dp))
            OpsSelectField(
                label = "Qaraj növü",
                options = listOf(
                    FinGarageType.OPEN_PARKING to "Açıq dayanacaq",
                    FinGarageType.COVERED_GARAGE to "Örtülü qaraj",
                    FinGarageType.STORAGE to "Anbar yeri",
                ).map { it.first.toString() to it.second },
                selectedId = garageType.toString(),
                onSelect = { garageType = it.toInt() },
            )
        }
        Spacer(Modifier.height(10.dp))
        OpsTextField(amount, { amount = it }, "Məbləğ (₼)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(10.dp))
        OpsDateField("Qüvvəyə minmə tarixi", effectiveFrom, { effectiveFrom = it })
        Spacer(Modifier.height(10.dp))
        OpsTextField(description, { description = it }, "Qeyd", singleLine = false)
        (localError ?: state.formError)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OpsColors.Error)
        }
        Spacer(Modifier.height(14.dp))
        OpsFormActions(
            saving = state.saving,
            submitLabel = "Yarat",
            onCancel = onDismiss,
            onSubmit = {
                val value = amount.trim().replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0) {
                    localError = "Məbləğ 0-dan böyük olmalıdır."
                } else {
                    localError = null
                    viewModel.createRate(
                        rateType = rateType,
                        amount = value,
                        effectiveFrom = effectiveFrom,
                        description = description,
                        garageType = if (rateType == FinRateType.FIXED_GARAGE) garageType else null,
                        onDone = onDismiss,
                    )
                }
            },
            enabled = amount.isNotBlank() && effectiveFrom.isNotBlank(),
        )
    }
}

/* ====================================================================== */
/* Əmək haqqı                                                             */
/* ====================================================================== */

private sealed interface PayrollAction {
    data object PayAll : PayrollAction
    data class PayOne(val employeeId: String) : PayrollAction
}

@Composable
private fun PayrollSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canManage: Boolean,
) {
    var methodAction by remember { mutableStateOf<PayrollAction?>(null) }
    val run = state.payroll

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text("Dövr", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OpsSelectField(
                        label = "İl",
                        options = (LocalDate.now().year - 2..LocalDate.now().year + 1)
                            .map { it.toString() to it.toString() },
                        selectedId = state.payrollYear.toString(),
                        onSelect = { viewModel.loadPayroll(it.toInt(), state.payrollMonth) },
                        modifier = Modifier.weight(1f),
                    )
                    OpsSelectField(
                        label = "Ay",
                        options = (1..12).map {
                            it.toString() to Format.period("2000-${it.toString().padStart(2, '0')}")
                                .substringBefore(" 2000")
                        },
                        selectedId = state.payrollMonth.toString(),
                        onSelect = { viewModel.loadPayroll(state.payrollYear, it.toInt()) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        when {
            state.payrollLoading -> item { OpsLoading() }
            state.payrollError != null && run == null ->
                item { OpsError(state.payrollError!!) { viewModel.loadPayroll(state.payrollYear, state.payrollMonth) } }
            run == null -> item {
                OpsCard {
                    Text(
                        "${Format.period("${state.payrollYear}-${state.payrollMonth.toString().padStart(2, '0')}")} " +
                            "üçün hesablama yoxdur.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OpsColors.Ink,
                    )
                    Text(
                        "Hesablama işçilərin cari əmək haqqı parametrləri əsasında aparılır.",
                        fontSize = 12.sp,
                        color = OpsColors.MutedSoft,
                    )
                    if (canManage) {
                        Spacer(Modifier.height(10.dp))
                        OpsPrimaryButton(
                            text = if (state.saving) "Hesablanır…" else "Hesabla",
                            onClick = viewModel::calculatePayroll,
                            enabled = !state.saving,
                        )
                    }
                }
            }
            else -> {
                item {
                    PayrollRunCard(
                        run = run,
                        state = state,
                        canManage = canManage,
                        viewModel = viewModel,
                        onPayAll = { methodAction = PayrollAction.PayAll },
                    )
                }
                item { OpsSectionTitle("Əmək haqqı cədvəli") }
                val payslips = run.payslips.orEmpty()
                if (payslips.isEmpty()) {
                    item {
                        OpsCard {
                            Text("İşçi üzrə sətir yoxdur.", fontSize = 12.sp, color = OpsColors.MutedSoft)
                        }
                    }
                } else {
                    items(payslips, key = { it.employeeId ?: it.employeeFullName ?: "x" }) { slip ->
                        PayslipCard(
                            slip = slip,
                            canPay = canManage && run.status == PayrollStatus.APPROVED && slip.paidAt == null,
                            busy = state.saving,
                            onPay = { methodAction = PayrollAction.PayOne(slip.employeeId.orEmpty()) },
                        )
                    }
                }
            }
        }
    }

    methodAction?.let { action ->
        MethodPickerDialog(
            title = if (action is PayrollAction.PayAll) "Bütün əmək haqqını ödə" else "Əmək haqqını ödə",
            onDismiss = { methodAction = null },
            onPick = { method ->
                methodAction = null
                when (action) {
                    PayrollAction.PayAll -> viewModel.payAllPayslips(method)
                    is PayrollAction.PayOne -> viewModel.payPayslip(action.employeeId, method)
                }
            },
        )
    }
}

@Composable
private fun PayrollRunCard(
    run: OpsPayrollRunDto,
    state: FinanceViewModel.UiState,
    canManage: Boolean,
    viewModel: FinanceViewModel,
    onPayAll: () -> Unit,
) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Format.period("${run.year}-${(run.month ?: 1).toString().padStart(2, '0')}"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text("Əmək haqqı hesablaması", fontSize = 11.sp, color = OpsColors.MutedSoft)
            }
            PayrollStatusChip(run.status)
        }
        Spacer(Modifier.height(8.dp))
        OpsInfoRow("Brüt cəm", Format.money(run.totalGross))
        OpsInfoRow("Net cəm", Format.money(run.totalNet), OpsColors.Olive)
        OpsInfoRow("İşəgötürən xərci", Format.money(run.totalEmployerCost))
        OpsInfoRow("İşçi sayı", (run.payslips?.size ?: 0).toString())

        if (canManage) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (run.status) {
                    PayrollStatus.DRAFT, PayrollStatus.CALCULATED -> {
                        OpsSecondaryButton(
                            text = "Yenidən hesabla",
                            onClick = viewModel::recalculatePayroll,
                            enabled = !state.saving,
                        )
                        OpsPrimaryButton(
                            text = "Təsdiqlə",
                            onClick = viewModel::approvePayroll,
                            enabled = !state.saving,
                        )
                    }
                    PayrollStatus.APPROVED -> {
                        OpsPrimaryButton(
                            text = "Hamısını ödə",
                            onClick = onPayAll,
                            enabled = !state.saving,
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun PayrollStatusChip(status: Int?) {
    val (fg, bg) = when (status) {
        PayrollStatus.DRAFT -> OpsColors.MutedSoft to OpsColors.Subtle
        PayrollStatus.CALCULATED -> OpsColors.Blue to OpsColors.BlueSoft
        PayrollStatus.APPROVED -> OpsColors.Amber to OpsColors.AmberSoft
        PayrollStatus.PAID -> OpsColors.Olive to OpsColors.OliveSoft
        else -> OpsColors.MutedSoft to OpsColors.Subtle
    }
    OpsChip(PayrollStatus.label(status), fg, bg)
}

@Composable
private fun PayslipCard(
    slip: OpsPayslipDto,
    canPay: Boolean,
    busy: Boolean,
    onPay: () -> Unit,
) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    slip.employeeFullName ?: "İşçi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    "Baza ${Format.money(slip.baseSalary)} · bonus ${Format.money(slip.bonusAmount)}",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                Text(
                    "İşəgötürən xərci ${Format.money(slip.totalEmployerCost)}",
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.money(slip.finalNetSalary),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Olive,
                )
                if (slip.paidAt != null) {
                    OpsChip("Ödənilib", OpsColors.Olive, OpsColors.OliveSoft)
                }
            }
        }
        if (canPay) {
            Spacer(Modifier.height(8.dp))
            OpsPrimaryButton("Ödə", onPay, enabled = !busy)
        }
    }
}

/* ====================================================================== */
/* Məzuniyyət                                                             */
/* ====================================================================== */

@Composable
private fun VacationSection(
    state: FinanceViewModel.UiState,
    viewModel: FinanceViewModel,
    canManage: Boolean,
) {
    var payTarget by remember { mutableStateOf<OpsVacationPayDto?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OpsCard {
                Text("Məzuniyyət ödənişləri", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
                Text(
                    "Əmək məzuniyyəti üzrə hesablanmış ödənişlər. Ödəniş tarixi yaxınlaşanlar " +
                        "siyahının yuxarısındadır.",
                    fontSize = 12.sp,
                    color = OpsColors.Muted,
                )
            }
        }

        when {
            state.vacationLoading && state.vacation.isEmpty() -> item { OpsLoading() }
            state.vacationError != null && state.vacation.isEmpty() ->
                item { OpsError(state.vacationError!!) { viewModel.loadVacation() } }
            state.vacation.isEmpty() -> item {
                OpsCard { Text("Məzuniyyət ödənişi yoxdur.", fontSize = 12.sp, color = OpsColors.MutedSoft) }
            }
            else -> items(state.vacation, key = { it.id ?: it.vacationOrderId ?: "x" }) { pay ->
                VacationCard(pay, canPay = canManage && pay.status == VacationPayStatus.PENDING) {
                    payTarget = pay
                }
            }
        }
    }

    payTarget?.let { pay ->
        MethodPickerDialog(
            title = "Məzuniyyət ödənişi",
            onDismiss = { payTarget = null },
            onPick = { method ->
                payTarget = null
                viewModel.payVacation(pay.id.orEmpty(), method)
            },
        )
    }
}

@Composable
private fun VacationCard(pay: OpsVacationPayDto, canPay: Boolean, onPay: () -> Unit) {
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pay.employeeFullName ?: "İşçi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Text(
                    "${Format.date(pay.vacationStartDate)} — ${Format.date(pay.vacationEndDate)} · " +
                        "${Format.number(pay.vacationDays)} gün",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
                Text(
                    "Ödəniş tarixi: ${Format.date(pay.dueDate)}",
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.money(pay.totalAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                OpsChip(
                    text = VacationPayStatus.label(pay.status),
                    foreground = if (pay.status == VacationPayStatus.PAID) OpsColors.Olive else OpsColors.Amber,
                    background = if (pay.status == VacationPayStatus.PAID) OpsColors.OliveSoft else OpsColors.AmberSoft,
                )
            }
        }
        if (canPay) {
            Spacer(Modifier.height(8.dp))
            OpsPrimaryButton("Ödə", onPay)
        }
    }
}

/* ====================================================================== */
/* Ortaq kiçik komponentlər                                               */
/* ====================================================================== */

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) OpsColors.Terracotta else OpsColors.Subtle,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else OpsColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MethodPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    PaymentMethod.CASH to "Nağd",
                    PaymentMethod.BANK_TRANSFER to "Bank köçürməsi",
                ).forEach { (id, label) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) },
                        shape = RoundedCornerShape(10.dp),
                        color = OpsColors.Subtle,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpsColors.Ink,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = OpsColors.Muted)
            }
        },
        containerColor = OpsColors.Surface,
    )
}
