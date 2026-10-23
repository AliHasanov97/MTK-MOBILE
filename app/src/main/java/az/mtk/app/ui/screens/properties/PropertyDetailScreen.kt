package az.mtk.app.ui.screens.properties

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ChargeAllocationDto
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.OwnershipRecordDto
import az.mtk.app.data.api.PaymentDto
import az.mtk.app.data.auth.AuthState
import az.mtk.app.data.auth.Roles
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.util.Format
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

internal object DetailColors {
    val Background = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF4F4EE)
    val Container = Color(0xFFF8EBE6)
    val Border = Color(0xFFE2E0D8)
    val Ink = Color(0xFF201A18)
    val Muted = Color(0xFF594139)
    val MutedSoft = Color(0xFF8D7168)
    val Terracotta = Color(0xFFD9531E)
    val TerracottaVivid = Color(0xFFE85D04)
    val Forest = Color(0xFF284033)
    val ForestDeep = Color(0xFF1B2D23)
    val Olive = Color(0xFF6F8559)
    val Lime = Color(0xFFD2EBB7)
    val Error = Color(0xFFBA1A1A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: String,
    propertyType: PropertyType,
    onBack: () -> Unit,
    onOpenProperty: (String, PropertyType) -> Unit = { _, _ -> },
    onOpenOwner: (String) -> Unit = {},
    viewModel: PropertyDetailViewModel = mtkViewModel {
        PropertyDetailViewModel(propertyId, propertyType)
    },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val authState by AppGraph.auth.state.collectAsStateWithLifecycle()
    val profile = (authState as? AuthState.Authenticated)?.profile

    // Same gates as the web page: assign/transfer is admin + building-manager,
    // registering a payment also allows the accountant.
    val canManage = profile?.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER) == true
    val canPay = profile?.hasAnyRole(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT) == true

    var showAssignOwner by remember { mutableStateOf(false) }
    var showTransferOwnership by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }

    LaunchedEffect(propertyId, propertyType) {
        viewModel.load(propertyId, propertyType)
    }

    // Action feedback ("Sahib təyin edildi." etc.) clears itself.
    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage != null) {
            delay(4000)
            viewModel.clearActionFeedback()
        }
    }

    Scaffold(
        containerColor = DetailColors.Background,
        topBar = {
            val pageTitle = when (propertyType) {
                PropertyType.APARTMENT -> "Mənzil detalları"
                PropertyType.GARAGE -> "Qaraj detalları"
            }

            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
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
            uiState.error != null -> ErrorState(uiState.error!!) { viewModel.load() }
            uiState.property != null -> PropertyDetailContent(
                property = uiState.property!!,
                ownershipHistory = uiState.ownershipHistory,
                transferCreators = uiState.transferCreators,
                charges = uiState.charges,
                payments = uiState.payments,
                actionError = uiState.actionError,
                actionMessage = uiState.actionMessage,
                canManage = canManage,
                canPay = canPay,
                onDismissFeedback = viewModel::clearActionFeedback,
                onAssignOwner = {
                    viewModel.clearActionFeedback()
                    viewModel.clearOwnerSelectionSearch()
                    showAssignOwner = true
                },
                onTransferOwnership = {
                    viewModel.clearActionFeedback()
                    viewModel.clearOwnerSelectionSearch()
                    viewModel.checkDebt()
                    showTransferOwnership = true
                },
                onPay = {
                    viewModel.clearActionFeedback()
                    showPay = true
                },
                onChargeClick = viewModel::openCharge,
                onOpenOtherProperty = onOpenProperty,
                onOpenOwner = onOpenOwner,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showAssignOwner) {
        AssignOwnerDialog(
            owners = uiState.ownerResults,
            query = uiState.ownerQuery,
            loading = uiState.ownersLoading,
            saving = uiState.saving,
            error = uiState.actionError,
            onQueryChange = viewModel::searchOwners,
            onAssign = { ownerId -> viewModel.assignOwner(ownerId) },
            onDismiss = {
                viewModel.clearActionFeedback()
                viewModel.clearOwnerSelectionSearch()
                showAssignOwner = false
            },
            onAssigned = { showAssignOwner = false },
        )
    }

    if (showTransferOwnership) {
        TransferOwnershipDialog(
            propertyTitle = uiState.property?.title.orEmpty(),
            owners = uiState.ownerResults,
            query = uiState.ownerQuery,
            loading = uiState.ownersLoading,
            saving = uiState.saving,
            debtChecking = uiState.debtChecking,
            outstandingDebt = uiState.outstandingDebt,
            debtError = uiState.debtError,
            error = uiState.actionError,
            onQueryChange = viewModel::searchOwners,
            onRetryDebt = viewModel::checkDebt,
            onTransfer = { ownerId -> viewModel.transferOwnership(ownerId) },
            onDismiss = {
                viewModel.clearActionFeedback()
                viewModel.clearOwnerSelectionSearch()
                showTransferOwnership = false
            },
            onTransferred = { showTransferOwnership = false },
        )
    }

    if (showPay) {
        PayDialog(
            balance = uiState.property?.balance ?: 0.0,
            ownerName = uiState.property?.ownerName,
            saving = uiState.saving,
            error = uiState.actionError,
            onPay = { amount, notes -> viewModel.pay(amount, notes) },
            onDismiss = {
                viewModel.clearActionFeedback()
                showPay = false
            },
            onPaid = { showPay = false },
        )
    }

    uiState.selectedCharge?.let { charge ->
        ChargeDetailDialog(
            charge = charge,
            allocations = uiState.chargeAllocations,
            onDismiss = viewModel::closeCharge,
        )
    }
}

@Composable
private fun PropertyDetailContent(
    property: PropertyDetail,
    ownershipHistory: List<OwnershipRecordDto>,
    transferCreators: Map<String, String>,
    charges: List<ChargeDto>,
    payments: List<PaymentDto>,
    actionError: String?,
    actionMessage: String?,
    canManage: Boolean,
    canPay: Boolean,
    onDismissFeedback: () -> Unit,
    onAssignOwner: () -> Unit,
    onTransferOwnership: () -> Unit,
    onPay: () -> Unit,
    onChargeClick: (ChargeDto) -> Unit,
    onOpenOtherProperty: (String, PropertyType) -> Unit,
    onOpenOwner: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        HeroImageSection(property)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            if (actionMessage != null || actionError != null) {
                FeedbackBanner(
                    text = actionError ?: actionMessage.orEmpty(),
                    isError = actionError != null,
                    onDismiss = onDismissFeedback,
                )
            }

            StatsCards(property)

            // Balans / Son ödəniş — the hero stats of the web apartment page.
            FinanceStatsSection(
                property = property,
                payments = payments,
                canPay = canPay,
                onPay = onPay,
            )

            if (property.utilities.isNotEmpty()) {
                UtilitiesSection(property.utilities)
            }

            OwnerSection(
                property = property,
                canManage = canManage,
                onAssignOwner = onAssignOwner,
                onTransferOwnership = onTransferOwnership,
                onOpenOwner = onOpenOwner,
            )

            if (property.type == PropertyItemType.APARTMENT) {
                OwnershipHistorySection(
                    history = ownershipHistory,
                    creators = transferCreators,
                )
            }

            ChargesPaymentsSection(
                charges = charges,
                onChargeClick = onChargeClick,
            )

            if (property.otherProperties.isNotEmpty()) {
                OtherPropertiesSection(
                    properties = property.otherProperties,
                    onClick = onOpenOtherProperty,
                )
            }

            Spacer(Modifier.height(96.dp))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BottomActions(property, canPay, onPay)
    }
}

@Composable
internal fun FeedbackBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val accent = if (isError) DetailColors.Error else DetailColors.Olive
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = DetailColors.Ink,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Bağla", tint = DetailColors.Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HeroImageSection(property: PropertyDetail) {
    if (property.imageUrl == null) {
        HeroHeaderCard(property)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        AsyncImage(
            model = property.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.65f),
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            StatusPill(property, onDark = true)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        typeIcon(property),
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = property.buildingAddress,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                Text(
                    text = property.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val meta = propertyMeta(property)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                if (!property.createdBy.isNullOrBlank()) {
                    Text(
                        text = "Əməliyyatı icra etdi: ${property.createdBy}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * Şəkli olmayan əmlaklar üçün hero: böyük qaranlıq blok yerinə saytın üslubunda
 * ağ kart — başlıq, ünvan, status və meta bir yerdə, kompakt.
 */
@Composable
private fun HeroHeaderCard(property: PropertyDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DetailColors.Terracotta.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        typeIcon(property),
                        null,
                        tint = DetailColors.Terracotta,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = property.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DetailColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = property.buildingAddress,
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                StatusPill(property, onDark = false)
            }

            val meta = propertyMeta(property)
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    fontSize = 12.sp,
                    color = DetailColors.Muted,
                )
            }
            if (!property.createdBy.isNullOrBlank()) {
                Text(
                    text = "Əməliyyatı icra etdi: ${property.createdBy}",
                    fontSize = 11.sp,
                    color = DetailColors.MutedSoft,
                )
            }
        }
    }
}

private fun typeIcon(property: PropertyDetail) = when (property.type) {
    PropertyItemType.APARTMENT -> Icons.Default.Apartment
    PropertyItemType.GARAGE -> Icons.Default.DirectionsCar
}

private fun propertyMeta(property: PropertyDetail): String = buildList {
    property.floor?.let { add("$it-ci mərtəbə") }
    property.roomCount?.let { add("$it otaqlı") }
    property.area?.let { add("${formatNumber(it)} m²") }
}.joinToString(" · ")

@Composable
private fun StatusPill(property: PropertyDetail, onDark: Boolean) {
    val status = property.status.lowercase()
    val label = when (status) {
        "occupied" -> if (property.type == PropertyItemType.GARAGE) "SAHİBİ VAR" else "MƏSKUNLAŞIB"
        "vacant" -> "BOŞ"
        else -> property.status.uppercase()
    }
    val accent = when (status) {
        "occupied" -> if (onDark) DetailColors.Lime else DetailColors.Olive
        "vacant" -> if (onDark) Color(0xFFFFCC80) else Color(0xFFB26A00)
        else -> if (onDark) Color.White else DetailColors.MutedSoft
    }

    Surface(
        shape = CircleShape,
        color = if (onDark) Color.White.copy(alpha = 0.15f) else accent.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Circle,
                null,
                tint = accent,
                modifier = Modifier.size(8.dp),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = if (onDark) Color.White else accent,
            )
        }
    }
}

@Composable
private fun StatsCards(property: PropertyDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (property.roomCount != null) {
            StatCard(
                icon = Icons.Default.Bed,
                value = "${property.roomCount} Otaq",
                label = "Planlaşma",
                modifier = Modifier.weight(1f),
            )
        }
        if (property.area != null) {
            StatCard(
                icon = Icons.Default.SquareFoot,
                value = "${property.area.toInt()} m²",
                label = "Ümumi sahə",
                modifier = Modifier.weight(1f),
            )
        }
        if (property.floor != null) {
            StatCard(
                icon = Icons.Default.Layers,
                value = "${property.floor}-ci",
                label = "Mərtəbə",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                icon,
                null,
                tint = DetailColors.Terracotta,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DetailColors.Ink,
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = DetailColors.MutedSoft,
            )
        }
    }
}

@Composable
private fun UtilitiesSection(utilities: List<Utility>) {
    SectionCard(
        title = "Kommunal Sayğaclar",
        icon = Icons.Default.Speed,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            utilities.forEach { utility ->
                UtilityChip(
                    name = utility.name,
                    hasActive = utility.hasActive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UtilityChip(
    name: String,
    hasActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (hasActive) DetailColors.Olive.copy(alpha = 0.12f)
        else DetailColors.Subtle,
        border = BorderStroke(
            1.dp,
            if (hasActive) DetailColors.Olive else DetailColors.Border
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Circle,
                null,
                tint = if (hasActive) DetailColors.Olive else DetailColors.MutedSoft,
                modifier = Modifier.size(8.dp),
            )
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasActive) DetailColors.Olive else DetailColors.Muted,
            )
        }
    }
}

@Composable
private fun OwnerSection(
    property: PropertyDetail,
    canManage: Boolean,
    onAssignOwner: () -> Unit,
    onTransferOwnership: () -> Unit,
    onOpenOwner: (String) -> Unit,
) {
    SectionCard(
        title = "Sahibi",
        icon = Icons.Default.Person,
        badge = if (property.ownerName != null) "MÜLKİYYƏTÇİ" else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (property.ownerName == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Bu əmlakın hazırda sahibi yoxdur (boş).",
                        fontSize = 14.sp,
                        color = DetailColors.Muted,
                    )
                    if (canManage) {
                        Button(
                            onClick = onAssignOwner,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DetailColors.Terracotta,
                            ),
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sahib təyin et")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DetailColors.Terracotta.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = property.ownerName.take(1),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DetailColors.Terracotta,
                            )
                        }
                        Column {
                            Text(
                                text = property.ownerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DetailColors.Ink,
                            )
                            Text(
                                text = "Mənzil sahibi",
                                fontSize = 12.sp,
                                color = DetailColors.Muted,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                val phone = property.ownerPhone
                                if (!phone.isNullOrBlank()) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DetailColors.Olive.copy(alpha = 0.12f)),
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                "Zəng et",
                                tint = DetailColors.Olive,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                val email = property.ownerEmail
                                if (!email.isNullOrBlank()) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DetailColors.Forest.copy(alpha = 0.12f)),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                "E-poçt göndər",
                                tint = DetailColors.Forest,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(color = DetailColors.Border)

                ContactRow(Icons.Default.Phone, property.ownerPhone ?: "—", "Mobil")
                ContactRow(Icons.Default.Email, property.ownerEmail ?: "—", "Şəxsi")

                if (property.contractNumber != null) {
                    ContactRow(
                        Icons.Default.Description,
                        property.contractNumber,
                        "Qeyd: ${property.contractDate ?: "—"}"
                    )
                }

                // Sakinin bütün əmlakları, haqları və ödənişləri bir səhifədə.
                val ownerId = property.ownerId
                if (ownerId != null) {
                    OutlinedButton(
                        onClick = { onOpenOwner(ownerId) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DetailColors.Forest),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DetailColors.Forest,
                        ),
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sakin detalları")
                    }
                }

                if (canManage && property.type == PropertyItemType.APARTMENT) {
                    HorizontalDivider(color = DetailColors.Border)

                    OutlinedButton(
                        onClick = onTransferOwnership,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DetailColors.Terracotta),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DetailColors.Terracotta,
                        ),
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mülkiyyəti köçür")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, value: String, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                null,
                tint = DetailColors.Muted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = DetailColors.Ink,
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = DetailColors.MutedSoft,
        )
    }
}

@Composable
internal fun SectionCard(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        null,
                        tint = DetailColors.Terracotta,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailColors.Ink,
                    )
                }

                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = (badgeColor ?: DetailColors.Terracotta).copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = badgeColor ?: DetailColors.Terracotta,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun OtherPropertiesSection(
    properties: List<OtherProperty>,
    onClick: (String, PropertyType) -> Unit,
) {
    SectionCard(
        title = "Sahibin Digər Əmlakı",
        icon = Icons.Default.HomeWork,
        badge = "${properties.size} əlaqəli əmlak",
    ) {
        properties.forEach { property ->
            OtherPropertyCard(property) {
                val type = when (property.type) {
                    PropertyItemType.APARTMENT -> PropertyType.APARTMENT
                    PropertyItemType.GARAGE -> PropertyType.GARAGE
                }
                onClick(property.id, type)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OtherPropertyCard(property: OtherProperty, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = DetailColors.Subtle,
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when (property.type) {
                    PropertyItemType.APARTMENT -> Icons.Default.Apartment
                    PropertyItemType.GARAGE -> Icons.Default.DirectionsCar
                },
                null,
                tint = DetailColors.Terracotta,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = property.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Ink,
                )
                Text(
                    text = property.subtitle,
                    fontSize = 12.sp,
                    color = DetailColors.Muted,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = DetailColors.MutedSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BottomActions(property: PropertyDetail, canPay: Boolean, onPay: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = {
                val phone = property.ownerPhone
                if (!phone.isNullOrBlank()) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }
                }
            },
            enabled = !property.ownerPhone.isNullOrBlank(),
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DetailColors.Forest,
                contentColor = Color.White,
                disabledContainerColor = DetailColors.Forest.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            border = BorderStroke(0.dp, Color.Transparent),
        ) {
            Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Zəng et", fontWeight = FontWeight.Bold)
        }

        if (canPay && property.ownerId != null) {
            Button(
                onClick = onPay,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailColors.Terracotta,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Default.Payments, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ödəniş qeyd et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------------------------------------------------------ dialogs

@Composable
private fun AssignOwnerDialog(
    owners: List<OwnerItemDto>,
    query: String,
    loading: Boolean,
    saving: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit,
    onAssigned: () -> Unit,
) {
    var selected by remember { mutableStateOf<OwnerItemDto?>(null) }
    var submitted by remember { mutableStateOf(false) }

    // Closes only after our own submit finished without an error.
    LaunchedEffect(saving) {
        if (submitted && !saving && error == null) onAssigned()
    }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { DialogTitle("Sahib təyin et") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OwnerPicker(
                    selected = selected,
                    owners = owners,
                    query = query,
                    loading = loading,
                    onQueryChange = onQueryChange,
                    onSelect = { selected = it },
                )
                if (error != null) FormError(error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selected?.id?.let { ownerId ->
                        submitted = true
                        onAssign(ownerId)
                    }
                },
                enabled = selected != null && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
            ) {
                Text(if (saving) "Saxlanılır…" else "Təyin et")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = DetailColors.Muted)
            }
        },
    )
}

@Composable
private fun TransferOwnershipDialog(
    propertyTitle: String,
    owners: List<OwnerItemDto>,
    query: String,
    loading: Boolean,
    saving: Boolean,
    debtChecking: Boolean,
    outstandingDebt: Double?,
    debtError: String?,
    error: String?,
    onQueryChange: (String) -> Unit,
    onRetryDebt: () -> Unit,
    onTransfer: (String) -> Unit,
    onDismiss: () -> Unit,
    onTransferred: () -> Unit,
) {
    var selected by remember { mutableStateOf<OwnerItemDto?>(null) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(saving) {
        if (submitted && !saving && error == null) onTransferred()
    }

    val blockedByDebt = (outstandingDebt ?: 0.0) > 0.005

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { DialogTitle("Mülkiyyəti köçür") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (propertyTitle.isNotBlank()) {
                    Text(propertyTitle, fontSize = 13.sp, color = DetailColors.Muted)
                }

                when {
                    debtChecking -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = DetailColors.Terracotta,
                        )
                        Text("Borc yoxlanılır…", fontSize = 13.sp, color = DetailColors.Muted)
                    }

                    debtError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormError("Borc yoxlanıla bilmədi: $debtError")
                        TextButton(onClick = onRetryDebt) { Text("Yenidən cəhd et") }
                    }

                    blockedByDebt -> FormError(
                        "Bu əmlak üzrə ${money(outstandingDebt)} qalıq borc var. " +
                            "Əvvəlcə borc ödənilməlidir — borcu olan mənzil köçürülə bilməz."
                    )

                    else -> {
                        OwnerPicker(
                            selected = selected,
                            owners = owners,
                            query = query,
                            loading = loading,
                            label = "Yeni sahib",
                            onQueryChange = onQueryChange,
                            onSelect = { selected = it },
                        )
                        if (error != null) FormError(error)
                    }
                }
            }
        },
        confirmButton = {
            if (blockedByDebt || debtError != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
                ) { Text("Bağla") }
            } else {
                Button(
                    onClick = {
                        selected?.id?.let { ownerId ->
                            submitted = true
                            onTransfer(ownerId)
                        }
                    },
                    enabled = selected != null && !saving && !debtChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
                ) {
                    Text(if (saving) "Saxlanılır…" else "Köçür")
                }
            }
        },
        dismissButton = {
            if (!blockedByDebt && debtError == null) {
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text("Ləğv et", color = DetailColors.Muted)
                }
            }
        },
    )
}

@Composable
internal fun PayDialog(
    balance: Double,
    ownerName: String?,
    saving: Boolean,
    error: String?,
    onPay: (Double, String?) -> Unit,
    onDismiss: () -> Unit,
    onPaid: () -> Unit,
) {
    val debt = if (balance < -0.005) -balance else 0.0
    var amount by remember { mutableStateOf(if (debt > 0) String.format(java.util.Locale.US, "%.2f", debt) else "") }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(saving) {
        if (submitted && !saving && error == null) onPaid()
    }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { DialogTitle("Ödəniş qeyd et") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ownerName != null) {
                    Text("Sahib: $ownerName", fontSize = 13.sp, color = DetailColors.Muted)
                }
                if (debt > 0) {
                    Text(
                        "Qalıq borc: ${money(debt)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Error,
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localError = null
                    },
                    label = { Text("Məbləğ (₼)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Qeyd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = localError ?: error
                if (message != null) FormError(message)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.replace(',', '.').toDoubleOrNull()
                    when {
                        parsed == null || parsed <= 0.0 ->
                            localError = "Məbləğ 0-dan böyük olmalıdır."

                        debt > 0 && parsed > debt + 0.005 ->
                            localError = "Əmlak üzrə ödəniş qalıq borcdan böyük ola bilməz " +
                                "(borc: ${money(debt)})."

                        else -> {
                            submitted = true
                            onPay(parsed, notes)
                        }
                    }
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
            ) {
                Text(if (saving) "Saxlanılır…" else "Qeyd et")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = DetailColors.Muted)
            }
        },
    )
}

@Composable
internal fun ChargeDetailDialog(
    charge: ChargeDto,
    allocations: List<ChargeAllocationDto>?,
    onDismiss: () -> Unit,
) {
    val remaining = (charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)

    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle("Haqq təfərrüatı") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailValueRow("Tarix", Format.dateTime(charge.createdAt))
                DetailValueRow("Məbləğ", money(charge.amount))
                DetailValueRow("Ödənilib", money(charge.paidAmount))
                DetailValueRow("Qalıq borc", money(remaining))
                DetailValueRow("Status", Format.chargeStatus(charge.status))
                Text(
                    text = chargeDescription(charge),
                    fontSize = 12.sp,
                    color = DetailColors.Muted,
                )

                HorizontalDivider(color = DetailColors.Border)

                Text(
                    "Bölgü — bu haqqı hansı ödənişlər qarşılayıb",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailColors.Ink,
                )

                when {
                    allocations == null -> Text(
                        "Bölgü məlumatı yüklənir…",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                    )

                    allocations.isEmpty() -> Text(
                        if (remaining > 0.005) {
                            "Bu haqqa hələ heç bir ödəniş tətbiq olunmayıb. Qalıq borc: ${money(remaining)}"
                        } else {
                            "Bu haqqa hələ heç bir ödəniş tətbiq olunmayıb."
                        },
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                    )

                    else -> allocations.forEach { row ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = DetailColors.Subtle,
                            border = BorderStroke(1.dp, DetailColors.Border),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    Format.dateTime(row.paymentDate),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DetailColors.Ink,
                                )
                                Text(
                                    "Tətbiq: ${money(row.allocatedAmount)} · Qalıq: ${money(row.remainingDebtAfterPayment)}",
                                    fontSize = 11.sp,
                                    color = DetailColors.Muted,
                                )
                                if (row.isFromAdvance == true) {
                                    Text("Avansdan ödənilib", fontSize = 10.sp, color = DetailColors.Olive)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DetailColors.Terracotta),
            ) { Text("Bağla") }
        },
    )
}

@Composable
private fun OwnerPicker(
    selected: OwnerItemDto?,
    owners: List<OwnerItemDto>,
    query: String,
    loading: Boolean,
    label: String = "Sahib",
    onQueryChange: (String) -> Unit,
    onSelect: (OwnerItemDto?) -> Unit,
) {
    if (selected != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = DetailColors.Subtle,
            border = BorderStroke(1.dp, DetailColors.Border),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        selected.fullName ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Ink,
                    )
                    Text(
                        selected.email ?: selected.phoneNumber ?: "",
                        fontSize = 11.sp,
                        color = DetailColors.Muted,
                    )
                }
                TextButton(onClick = { onSelect(null) }) {
                    Text("Dəyiş", fontSize = 12.sp)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(label) },
            placeholder = { Text("Ad, email və ya telefonla axtar…", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            loading -> Text("Axtarılır…", fontSize = 11.sp, color = DetailColors.MutedSoft)
            query.isNotBlank() && owners.isEmpty() ->
                Text("Nəticə yoxdur", fontSize = 11.sp, color = DetailColors.MutedSoft)
            else -> owners.take(5).forEach { owner ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(owner) },
                    shape = RoundedCornerShape(8.dp),
                    color = DetailColors.Surface,
                    border = BorderStroke(1.dp, DetailColors.Border),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            owner.fullName ?: "—",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DetailColors.Ink,
                        )
                        Text(
                            owner.email ?: owner.phoneNumber ?: "",
                            fontSize = 11.sp,
                            color = DetailColors.Muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = DetailColors.Ink,
    )
}

@Composable
private fun FormError(text: String) {
    Text(text = text, fontSize = 12.sp, color = DetailColors.Error)
}

@Composable
private fun DetailValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = DetailColors.Muted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DetailColors.Ink)
    }
}

// ------------------------------------------------------------------- states

@Composable
internal fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = DetailColors.Terracotta)
    }
}

@Composable
internal fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                null,
                tint = DetailColors.Error,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Xəta baş verdi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DetailColors.Ink,
            )
            Text(
                text = error,
                fontSize = 13.sp,
                color = DetailColors.Muted,
            )
            Button(onClick = onRetry) {
                Text("Yenidən cəhd et")
            }
        }
    }
}
