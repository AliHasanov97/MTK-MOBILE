package az.mtk.app.ui.screens.owners

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.data.api.OwnedApartmentDto
import az.mtk.app.data.api.OwnedGarageDto
import az.mtk.app.data.api.PaymentAllocationDto
import az.mtk.app.data.api.PaymentDto
import az.mtk.app.data.auth.AuthState
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.properties.ChargeCard
import az.mtk.app.ui.screens.properties.ChargeDetailDialog
import az.mtk.app.ui.screens.properties.DetailColors
import az.mtk.app.ui.screens.properties.ErrorState
import az.mtk.app.ui.screens.properties.FeedbackBanner
import az.mtk.app.ui.screens.properties.LoadingState
import az.mtk.app.ui.screens.properties.PayDialog
import az.mtk.app.ui.screens.properties.PropertyType
import az.mtk.app.ui.screens.properties.SectionCard
import az.mtk.app.ui.screens.properties.formatNumber
import az.mtk.app.ui.screens.properties.money
import az.mtk.app.ui.screens.properties.signedMoney
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ödəniş dialoqunun hədəfi — konkret mənzil/qaraj (və ya ümumi balans). */
private data class PayTarget(
    val title: String,
    val apartmentId: String?,
    val garageId: String?,
    val balance: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDetailScreen(
    ownerId: String,
    onBack: () -> Unit,
    onOpenProperty: (String, PropertyType) -> Unit,
    viewModel: OwnerDetailViewModel = mtkViewModel { OwnerDetailViewModel(ownerId) },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val authState by AppGraph.auth.state.collectAsStateWithLifecycle()
    val profile = (authState as? AuthState.Authenticated)?.profile

    // Saytla eyni icazə qaydası: ödənişi admin, bina meneceri və mühasib yaza bilər.
    val canPay = profile?.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT) == true

    var payTarget by remember { mutableStateOf<PayTarget?>(null) }

    LaunchedEffect(ownerId) {
        viewModel.load(ownerId)
    }

    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage != null) {
            delay(4000)
            viewModel.clearActionFeedback()
        }
    }

    Scaffold(
        containerColor = DetailColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sakin detalları",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailColors.Ink,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Geri",
                            tint = DetailColors.Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DetailColors.Background,
                ),
            )
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState()

            uiState.error != null -> ErrorState(uiState.error!!) { viewModel.load(ownerId) }

            // Eyni ViewModel başqa sakin üçün də istifadə olunur — köhnə sakinin
            // məlumatı bir anlıq da görünməsin deyə id yoxlanılır.
            uiState.owner != null && uiState.owner?.id == ownerId -> OwnerDetailContent(
                owner = uiState.owner!!,
                selectedTab = uiState.selectedTab,
                charges = uiState.charges,
                payments = uiState.payments,
                ownerBalance = uiState.ownerBalance,
                propertyBalances = uiState.propertyBalances,
                createdBy = uiState.createdBy,
                balanceCreatedBy = uiState.balanceCreatedBy,
                canPay = canPay,
                actionError = uiState.actionError,
                actionMessage = uiState.actionMessage,
                onTabSelected = viewModel::setTab,
                onDismissFeedback = viewModel::clearActionFeedback,
                onChargeClick = viewModel::openCharge,
                onPaymentClick = viewModel::openPayment,
                onOpenProperty = onOpenProperty,
                onPay = { target ->
                    viewModel.clearActionFeedback()
                    payTarget = target
                },
                modifier = Modifier.padding(padding),
            )

            else -> LoadingState()
        }
    }

    val target = payTarget
    if (target != null) {
        PayDialog(
            balance = target.balance,
            ownerName = uiState.owner?.fullName,
            saving = uiState.saving,
            error = uiState.actionError,
            onPay = { amount, notes ->
                viewModel.pay(amount, notes, target.apartmentId, target.garageId)
            },
            onDismiss = {
                viewModel.clearActionFeedback()
                payTarget = null
            },
            onPaid = { payTarget = null },
        )
    }

    val selectedCharge = uiState.selectedCharge
    if (selectedCharge != null) {
        ChargeDetailDialog(
            charge = selectedCharge,
            allocations = uiState.chargeAllocations,
            onDismiss = viewModel::closeCharge,
        )
    }

    val selectedPayment = uiState.selectedPayment
    if (selectedPayment != null) {
        PaymentDetailDialog(
            payment = selectedPayment,
            allocations = uiState.paymentAllocations,
            createdBy = uiState.paymentCreatedBy,
            allocationCreators = uiState.allocationCreators,
            onDismiss = viewModel::closePayment,
        )
    }
}

@Composable
private fun OwnerDetailContent(
    owner: OwnerDetailDto,
    selectedTab: OwnerDetailViewModel.Tab,
    charges: List<ChargeDto>,
    payments: List<PaymentDto>,
    ownerBalance: Double,
    propertyBalances: Map<String, Double>,
    createdBy: String?,
    balanceCreatedBy: String?,
    canPay: Boolean,
    actionError: String?,
    actionMessage: String?,
    onTabSelected: (OwnerDetailViewModel.Tab) -> Unit,
    onDismissFeedback: () -> Unit,
    onChargeClick: (ChargeDto) -> Unit,
    onPaymentClick: (PaymentDto) -> Unit,
    onOpenProperty: (String, PropertyType) -> Unit,
    onPay: (PayTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apartments = owner.apartments.orEmpty()
    val garages = owner.garages.orEmpty()
    val lastPayment = remember(payments) { payments.maxByOrNull { it.paymentDate.orEmpty() } }

    val propertyLabels = remember(owner) {
        buildMap {
            apartments.forEach {
                put(it.id, "${it.building?.name ?: "—"} · Mənzil ${it.apartmentNumber ?: "—"}")
            }
            garages.forEach {
                put(it.id, "Qaraj ${it.garageNumber ?: "—"}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        val feedback = actionMessage ?: actionError
        if (feedback != null) {
            FeedbackBanner(
                text = feedback,
                isError = actionMessage == null,
                onDismiss = onDismissFeedback,
            )
        }

        OwnerHeroCard(owner = owner, createdBy = createdBy)

        OwnerBalanceCard(
            balance = ownerBalance,
            balanceCreatedBy = balanceCreatedBy,
            lastPayment = lastPayment,
        )

        OwnerTabs(
            selected = selectedTab,
            apartments = apartments.size,
            garages = garages.size,
            charges = charges.size,
            payments = payments.size,
            onSelect = onTabSelected,
        )

        when (selectedTab) {
            OwnerDetailViewModel.Tab.APARTMENTS -> SectionCard(
                title = "Mənzillər",
                icon = Icons.Default.Apartment,
            ) {
                if (apartments.isEmpty()) {
                    EmptyTabText("Bu sakinin adına heç bir mənzil qeydə alınmayıb.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        apartments.forEach { apartment ->
                            val balance = propertyBalances[apartment.id] ?: 0.0
                            ApartmentRow(
                                apartment = apartment,
                                balance = balance,
                                canPay = canPay,
                                onClick = { onOpenProperty(apartment.id, PropertyType.APARTMENT) },
                                onPay = {
                                    onPay(
                                        PayTarget(
                                            title = "${apartment.building?.name ?: "—"} · Mənzil ${apartment.apartmentNumber ?: "—"}",
                                            apartmentId = apartment.id,
                                            garageId = null,
                                            balance = balance,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            OwnerDetailViewModel.Tab.GARAGES -> SectionCard(
                title = "Qarajlar",
                icon = Icons.Default.DirectionsCar,
            ) {
                if (garages.isEmpty()) {
                    EmptyTabText("Bu sakinin adına heç bir qaraj qeydə alınmayıb.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        garages.forEach { garage ->
                            val balance = propertyBalances[garage.id] ?: 0.0
                            GarageRow(
                                garage = garage,
                                balance = balance,
                                canPay = canPay,
                                onClick = { onOpenProperty(garage.id, PropertyType.GARAGE) },
                                onPay = {
                                    onPay(
                                        PayTarget(
                                            title = "Qaraj ${garage.garageNumber ?: "—"}",
                                            apartmentId = null,
                                            garageId = garage.id,
                                            balance = balance,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            OwnerDetailViewModel.Tab.CHARGES -> SectionCard(
                title = "Haqlar",
                icon = Icons.Default.AccountBalance,
            ) {
                if (charges.isEmpty()) {
                    EmptyTabText("Hələ heç bir haqq yaranmayıb.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        charges.sortedByDescending { it.createdAt.orEmpty() }.forEach { charge ->
                            ChargeCard(
                                charge = charge,
                                showProperty = true,
                                onClick = { onChargeClick(charge) },
                            )
                        }
                    }
                }
            }

            OwnerDetailViewModel.Tab.PAYMENTS -> SectionCard(
                title = "Ödənişlər",
                icon = Icons.Default.Payments,
            ) {
                if (payments.isEmpty()) {
                    EmptyTabText("Hələ heç bir ödəniş qeydə alınmayıb.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        payments.sortedByDescending { it.paymentDate.orEmpty() }.forEach { payment ->
                            val propertyLabel = when {
                                payment.apartmentId != null -> propertyLabels[payment.apartmentId]
                                payment.garageId != null -> propertyLabels[payment.garageId]
                                else -> null
                            }
                            PaymentRow(
                                payment = payment,
                                propertyLabel = propertyLabel,
                                onClick = { onPaymentClick(payment) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ------------------------------------------------------------------- hero

@Composable
private fun OwnerHeroCard(owner: OwnerDetailDto, createdBy: String?) {
    val active = owner.isActive != false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DetailColors.Container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = Format.initials(owner.fullName),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DetailColors.Terracotta,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = owner.fullName?.takeIf { it.isNotBlank() } ?: "Adsız sakin",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DetailColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = owner.email?.takeIf { it.isNotBlank() } ?: "—",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = owner.phoneNumber?.takeIf { it.isNotBlank() } ?: "—",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                SmallTag(
                    text = if (active) "Aktiv" else "Deaktiv",
                    color = if (active) DetailColors.Olive else DetailColors.MutedSoft,
                )
            }

            HorizontalDivider(color = DetailColors.Border)

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = DetailColors.MutedSoft,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = if (owner.userId != null) {
                        "Qeydiyyatlı istifadəçi"
                    } else {
                        "Passiv sahib (hesabı yoxdur)"
                    },
                    fontSize = 11.sp,
                    color = DetailColors.MutedSoft,
                )
            }

            if (!createdBy.isNullOrBlank()) {
                Text(
                    text = "Əməliyyatı icra etdi: $createdBy",
                    fontSize = 11.sp,
                    color = DetailColors.MutedSoft,
                )
            }
        }
    }
}

@Composable
private fun OwnerBalanceCard(
    balance: Double,
    balanceCreatedBy: String?,
    lastPayment: PaymentDto?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Ümumi balans", fontSize = 12.sp, color = DetailColors.Muted)
                Text(
                    text = signedMoney(balance),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor(balance),
                )
                if (!balanceCreatedBy.isNullOrBlank()) {
                    Text(
                        text = "Əməliyyatı icra etdi: $balanceCreatedBy",
                        fontSize = 9.sp,
                        color = DetailColors.MutedSoft,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(64.dp),
                color = DetailColors.Border,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Son ödəniş", fontSize = 12.sp, color = DetailColors.Muted)
                Text(
                    text = Format.dateTime(lastPayment?.paymentDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Ink,
                )
                if (lastPayment?.amount != null) {
                    Text(
                        text = money(lastPayment.amount),
                        fontSize = 11.sp,
                        color = DetailColors.MutedSoft,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------- tabs

@Composable
private fun OwnerTabs(
    selected: OwnerDetailViewModel.Tab,
    apartments: Int,
    garages: Int,
    charges: Int,
    payments: Int,
    onSelect: (OwnerDetailViewModel.Tab) -> Unit,
) {
    val tabs = listOf(
        Triple(OwnerDetailViewModel.Tab.APARTMENTS, "Mənzillər", apartments),
        Triple(OwnerDetailViewModel.Tab.GARAGES, "Qarajlar", garages),
        Triple(OwnerDetailViewModel.Tab.CHARGES, "Haqlar", charges),
        Triple(OwnerDetailViewModel.Tab.PAYMENTS, "Ödənişlər", payments),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { (tab, label, count) ->
            val isSelected = selected == tab

            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clickable { onSelect(tab) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) DetailColors.Terracotta else DetailColors.Subtle,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else DetailColors.Ink,
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color.White.copy(alpha = 0.3f) else DetailColors.Border,
                    ) {
                        Text(
                            text = count.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else DetailColors.Muted,
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- əmlaklar

@Composable
private fun ApartmentRow(
    apartment: OwnedApartmentDto,
    balance: Double,
    canPay: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${apartment.building?.name ?: "—"} · Mənzil ${apartment.apartmentNumber ?: "—"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${apartment.floor ?: 0}-ci mərtəbə · ${apartment.roomCount ?: 0} otaq · " +
                            "${formatNumber(apartment.areaSquareMeters ?: 0.0)} m²",
                        fontSize = 11.sp,
                        color = DetailColors.Muted,
                    )
                }

                OwnerStatusTag(apartment.status)
            }

            HorizontalDivider(color = DetailColors.Border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BalanceBlock(balance)

                if (canPay && balance < -0.005) {
                    PayButton(onClick = onPay)
                }
            }
        }
    }
}

@Composable
private fun GarageRow(
    garage: OwnedGarageDto,
    balance: Double,
    canPay: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Qaraj ${garage.garageNumber ?: "—"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Ink,
                    )
                    Text(
                        text = buildString {
                            append(Format.garageType(garage.type))
                            if (!garage.description.isNullOrBlank()) {
                                append(" · ")
                                append(garage.description)
                            }
                        },
                        fontSize = 11.sp,
                        color = DetailColors.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = DetailColors.Border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BalanceBlock(balance)

                if (canPay && balance < -0.005) {
                    PayButton(onClick = onPay)
                }
            }
        }
    }
}

@Composable
private fun BalanceBlock(balance: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Balans", fontSize = 10.sp, color = DetailColors.Muted)
        Text(
            text = signedMoney(balance),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = balanceColor(balance),
        )
    }
}

@Composable
private fun PayButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DetailColors.Terracotta),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailColors.Terracotta),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
    ) {
        Text("Ödə", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OwnerStatusTag(status: String?) {
    val (label, color) = when (status?.lowercase()) {
        "occupied" -> "Məskunlaşıb" to DetailColors.Olive
        "vacant" -> "Boş" to Color(0xFFB26A00)
        null, "" -> "—" to DetailColors.MutedSoft
        else -> status to DetailColors.MutedSoft
    }
    SmallTag(text = label, color = color)
}

// -------------------------------------------------------------- ödənişlər

@Composable
private fun PaymentRow(payment: PaymentDto, propertyLabel: String?, onClick: () -> Unit) {
    val completed = payment.status == 1
    val statusLabel = paymentStatusLabel(payment.status)
    val statusColor = paymentStatusColor(payment.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = money(payment.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) DetailColors.Ink else DetailColors.Muted,
                )
                SmallTag(text = statusLabel, color = statusColor)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    null,
                    tint = DetailColors.MutedSoft,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = Format.dateTime(payment.paymentDate),
                    fontSize = 11.sp,
                    color = DetailColors.Muted,
                )
                Text("·", fontSize = 11.sp, color = DetailColors.MutedSoft)
                Text(
                    text = paymentMethodLabel(payment.paymentMethod),
                    fontSize = 11.sp,
                    color = DetailColors.Muted,
                )
            }

            if (!propertyLabel.isNullOrBlank()) {
                Text(
                    text = propertyLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Terracotta,
                )
            }

            payment.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    fontSize = 11.sp,
                    color = DetailColors.MutedSoft,
                )
            }
        }
    }
}

/**
 * Ödəniş kartına toxunanda açılan sənəd — saytdakı `PaymentDetailModal`:
 * ödənişin rekvizitləri + bölgü (hansı haqları qarşıladığı) + qalan avans.
 */
@Composable
private fun PaymentDetailDialog(
    payment: PaymentDto,
    allocations: List<PaymentAllocationDto>?,
    createdBy: String?,
    allocationCreators: Map<String, String>,
    onDismiss: () -> Unit,
) {
    val completed = payment.status == 1
    val allocatedTotal = allocations?.sumOf { it.allocatedAmount ?: 0.0 } ?: 0.0
    val unallocated = (payment.amount ?: 0.0) - allocatedTotal

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }

    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ödəniş sənədi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DetailColors.Ink,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ödəniş sənədi ${payment.id.take(8).uppercase()}",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Terracotta,
                    )

                    // Saytdakı "Qəbzi yüklə (PDF)" — backend-in receipt endpointi.
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                downloading = true
                                try {
                                    val result = FileRepository.downloadToInternal(
                                        context = context,
                                        path = "api/payments/payments/${payment.id}/receipt",
                                        fallbackName = "qebz-${payment.id.take(8)}.pdf",
                                    )
                                    if (!FileRepository.openFile(context, result.file)) {
                                        Toast.makeText(
                                            context,
                                            "Fayl yükləndi: ${result.fileName}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.toUserMessage(), Toast.LENGTH_LONG).show()
                                } finally {
                                    downloading = false
                                }
                            }
                        },
                        enabled = !downloading,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DetailColors.Terracotta),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailColors.Terracotta),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (downloading) "Yüklənir…" else "Qəbzi yüklə (PDF)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                PaymentDetailRow("Tarix", Format.dateTime(payment.paymentDate))
                PaymentDetailRow("Məbləğ", money(payment.amount))
                PaymentDetailRow("Ödəniş üsulu", paymentMethodLabel(payment.paymentMethod))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status", fontSize = 12.sp, color = DetailColors.Muted)
                    SmallTag(
                        text = paymentStatusLabel(payment.status),
                        color = paymentStatusColor(payment.status),
                    )
                }

                payment.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    PaymentDetailRow("Qeyd", notes)
                }

                if (!createdBy.isNullOrBlank()) {
                    Text(
                        text = "Əməliyyatı icra etdi: $createdBy",
                        fontSize = 11.sp,
                        color = DetailColors.MutedSoft,
                    )
                }

                if (!completed) {
                    Text(
                        text = "Bu ödəniş hələ gözləmə statusundadır, tamamlanmayıb.",
                        fontSize = 11.sp,
                        color = DetailColors.MutedSoft,
                    )
                }

                HorizontalDivider(color = DetailColors.Border)

                Text(
                    text = "Bölgü — bu ödəniş haraya getdi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailColors.Ink,
                )

                when {
                    allocations == null -> Text(
                        text = "Bölgü məlumatı yüklənir…",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                    )

                    allocations.isEmpty() -> Text(
                        text = "Bu ödəniş hələ heç bir haqqa tətbiq olunmayıb — tam " +
                            "${money(payment.amount)} sahibin hesabında avans kimi saxlanılır.",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                    )

                    else -> {
                        allocations.forEach { row ->
                            PaymentAllocationRow(
                                row = row,
                                performedBy = allocationCreators[row.id],
                            )
                        }

                        if (unallocated > 0.005) {
                            Text(
                                text = "Qalan ${money(unallocated)} sahibin hesabında avans kimi " +
                                    "saxlanılır və növbəti haqqa avtomatik tətbiq olunacaq.",
                                fontSize = 12.sp,
                                color = DetailColors.Muted,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
            ) {
                Text("Bağla")
            }
        },
    )
}

@Composable
private fun PaymentAllocationRow(row: PaymentAllocationDto, performedBy: String?) {
    val remaining = row.remainingDebtAfterPayment ?: 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DetailColors.Subtle,
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            row.propertyLabel?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Terracotta,
                )
            }

            Text(
                text = row.description?.takeIf { it.isNotBlank() } ?: Format.period(row.period),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DetailColors.Ink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Məbləğ", fontSize = 9.sp, color = DetailColors.Muted)
                    Text(
                        text = money(row.chargeAmount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailColors.Ink,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text("Tətbiq", fontSize = 9.sp, color = DetailColors.Muted)
                    Text(
                        text = money(row.allocatedAmount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailColors.Olive,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text("Qalıq", fontSize = 9.sp, color = DetailColors.Muted)
                    Text(
                        text = money(remaining),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0.005) DetailColors.Error else DetailColors.Olive,
                    )
                }
            }

            if (!performedBy.isNullOrBlank()) {
                Text(
                    text = "İcra edən: $performedBy",
                    fontSize = 10.sp,
                    color = DetailColors.MutedSoft,
                )
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = DetailColors.Muted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DetailColors.Ink,
        )
    }
}

private fun paymentStatusLabel(status: Int?): String = when (status) {
    1 -> "Tamamlanıb"
    0 -> "Gözləyir"
    else -> "—"
}

private fun paymentStatusColor(status: Int?): Color = when (status) {
    1 -> DetailColors.Olive
    0 -> Color(0xFFB26A00)
    else -> DetailColors.MutedSoft
}

private fun paymentMethodLabel(method: Int?): String = when (method) {
    0 -> "Nağd"
    1 -> "Bank köçürməsi"
    2 -> "Kart"
    else -> "—"
}

// ----------------------------------------------------------------- helpers

@Composable
private fun SmallTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun EmptyTabText(text: String) {
    Text(text = text, fontSize = 13.sp, color = DetailColors.Muted)
}

private fun balanceColor(value: Double): Color = when {
    value < -0.005 -> DetailColors.Error
    value > 0.005 -> DetailColors.Olive
    else -> DetailColors.Ink
}
