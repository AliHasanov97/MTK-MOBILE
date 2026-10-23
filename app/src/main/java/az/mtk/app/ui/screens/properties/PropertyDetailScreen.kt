package az.mtk.app.ui.screens.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.OwnershipRecordDto
import az.mtk.app.data.api.PaymentDto
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

private object DetailColors {
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
    viewModel: PropertyDetailViewModel = mtkViewModel {
        PropertyDetailViewModel(propertyId, propertyType)
    },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showAssignOwner by remember { mutableStateOf(false) }
    var showTransferOwnership by remember { mutableStateOf(false) }

    LaunchedEffect(propertyId, propertyType) {
        println("🔄 LaunchedEffect triggered: propertyId=$propertyId, propertyType=$propertyType")
        viewModel.load(propertyId, propertyType)
    }

    Scaffold(
        containerColor = DetailColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Geri",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: QR Code */ }) {
                        Icon(Icons.Default.QrCode, null, tint = Color.White)
                    }
                    IconButton(onClick = { /* TODO: More menu */ }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState()
            uiState.error != null -> ErrorState(uiState.error!!) { viewModel.load() }
            uiState.property != null -> {
                println("🖼️ UI: Rendering property detail")
                println("🖼️ UI: ownershipHistory size = ${uiState.ownershipHistory.size}")
                println("🖼️ UI: charges size = ${uiState.charges.size}")
                println("🖼️ UI: payments size = ${uiState.payments.size}")
                PropertyDetailContent(
                    property = uiState.property!!,
                    ownershipHistory = uiState.ownershipHistory,
                    charges = uiState.charges,
                    payments = uiState.payments,
                    onAssignOwner = { showAssignOwner = true },
                    onTransferOwnership = { showTransferOwnership = true },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun PropertyDetailContent(
    property: PropertyDetail,
    ownershipHistory: List<OwnershipRecordDto>,
    charges: List<ChargeDto>,
    payments: List<PaymentDto>,
    onAssignOwner: () -> Unit,
    onTransferOwnership: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        // Hero image section
        HeroImageSection(property)

        // Content
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            // Stats cards
            StatsCards(property)

            // Finance stats (balans, son ödəniş, aylıq tərəf) - always show
            println("💳 Showing FinanceStatsSection: balance=${property.balance}, lastPayment=${property.lastPaymentAmount}, monthlyRate=${property.monthlyRate}")
            FinanceStatsSection(property, payments)

            // Utilities section
            if (property.utilities.isNotEmpty()) {
                UtilitiesSection(property.utilities)
            } else {
                Text(
                    text = "Utilities will be displayed here",
                    fontSize = 12.sp,
                    color = DetailColors.Muted,
                )
            }

            // Owner info with action buttons
            OwnerSection(
                property = property,
                onAssignOwner = onAssignOwner,
                onTransferOwnership = onTransferOwnership,
            )

            // Ownership history (apartments only)
            if (ownershipHistory.isNotEmpty()) {
                OwnershipHistorySection(ownershipHistory)
            }

            // TEST: Visible marker to confirm code is running
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Red),
            ) {
                Text(
                    "🔴 TEST: ChargesPaymentsSection is here. Charges: ${charges.size}, Payments: ${payments.size}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Charges and payments - always show (even if empty)
            println("📊 Showing ChargesPaymentsSection: charges=${charges.size}, payments=${payments.size}")
            ChargesPaymentsSection(charges, payments)

            // Other properties
            if (property.otherProperties.isNotEmpty()) {
                OtherPropertiesSection(property.otherProperties)
            }

            Spacer(Modifier.height(96.dp))
        }
    }

    // Bottom actions
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BottomActions(property)
    }
}

@Composable
private fun HeroImageSection(property: PropertyDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        // Building image
        if (property.imageUrl != null) {
            AsyncImage(
                model = property.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Placeholder gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(DetailColors.ForestDeep, DetailColors.Forest)
                        )
                    ),
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                        )
                    )
                ),
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status badge
            val statusText = when (property.status.lowercase()) {
                "occupied" -> "MƏSKUNLAŞİB"
                "vacant" -> "BOŞ"
                else -> property.status.uppercase()
            }
            val statusColor = when (property.status.lowercase()) {
                "occupied" -> DetailColors.Lime
                "vacant" -> Color(0xFFFFCC80)
                else -> Color.White
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Circle,
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(8.dp),
                    )
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = Color.White,
                    )
                }
            }

            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (property.type) {
                            PropertyItemType.APARTMENT -> Icons.Default.Apartment
                            PropertyItemType.GARAGE -> Icons.Default.DirectionsCar
                        },
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
                    text = "${property.buildingName} · ${property.title}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White,
                )
            }
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
                label = "Ümumi səhə",
                modifier = Modifier.weight(1f),
            )
        }
        if (property.direction != null) {
            StatCard(
                icon = Icons.Default.Explore,
                value = property.direction,
                label = "Günəşli baxış",
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
    onAssignOwner: () -> Unit,
    onTransferOwnership: () -> Unit,
) {
    SectionCard(
        title = "Sahib Məlumatı",
        icon = Icons.Default.Person,
        badge = if (property.ownerName != null) "SAKIN MÜLKİYYƏTÇİ" else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (property.ownerName == null) {
                // No owner - show assign button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Bu əmlakın hazırda sahibi yoxdur (boş)",
                        fontSize = 14.sp,
                        color = DetailColors.Muted,
                    )
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
            } else {
            // Owner name with avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
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
                            text = property.ownerName?.take(1) ?: "?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailColors.Terracotta,
                        )
                    }
                    Column {
                        Text(
                            text = property.ownerName ?: "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailColors.Ink,
                        )
                        Text(
                            text = "Rezident · Mənzil Sahibi",
                            fontSize = 12.sp,
                            color = DetailColors.Muted,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { /* TODO: Call */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DetailColors.Olive.copy(alpha = 0.12f)),
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            tint = DetailColors.Olive,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: Message */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DetailColors.Forest.copy(alpha = 0.12f)),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            null,
                            tint = DetailColors.Forest,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = DetailColors.Border)

            // Contact info
            ContactRow(Icons.Default.Phone, property.ownerPhone ?: "—", "Mobil")
            ContactRow(Icons.Default.Email, property.ownerEmail ?: "—", "Şəxsi")

            if (property.contractNumber != null) {
                ContactRow(
                    Icons.Default.Description,
                    property.contractNumber,
                    "Qeyd: ${property.contractDate ?: "—"}"
                )
            }

            // Transfer ownership button
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
private fun FinanceSection(property: PropertyDetail) {
    SectionCard(
        title = "Maliyyə Vəziyyəti",
        icon = Icons.Default.AccountBalance,
        badge = if (property.balance!! < 0) "BORC MÖVCUDDUR" else null,
        badgeColor = if (property.balance < 0) DetailColors.Error else null,
    ) {
        // Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (property.balance < 0)
                    DetailColors.Error.copy(alpha = 0.08f)
                else DetailColors.Olive.copy(alpha = 0.08f)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "CARİ BALANS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = DetailColors.Muted,
                    )
                    if (property.balance < 0) {
                        Surface(
                            shape = CircleShape,
                            color = DetailColors.Error.copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = DetailColors.Error,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "Ödəniş tələb olunur",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DetailColors.Error,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${if (property.balance >= 0) "+" else ""}${String.format("%.2f", property.balance)} ₼",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = if (property.balance < 0) DetailColors.Error else DetailColors.Olive,
                )

                if (property.lastPaymentDate != null && property.lastPaymentAmount != null) {
                    HorizontalDivider(color = DetailColors.Border)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Sonuncu ödəniş:",
                                fontSize = 12.sp,
                                color = DetailColors.Muted,
                            )
                            Text(
                                text = "+${String.format("%.2f", property.lastPaymentAmount)} ₼ (${property.lastPaymentDate})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DetailColors.Ink,
                            )
                        }
                    }
                }

                if (property.monthlyRate != null) {
                    Text(
                        text = "Aylıq komendant tarifi: ${String.format("%.2f", property.monthlyRate)} ₼/ay",
                        fontSize = 12.sp,
                        color = DetailColors.Muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Payment history button
        OutlinedButton(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DetailColors.Terracotta,
            ),
            border = BorderStroke(1.dp, DetailColors.Border),
        ) {
            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hesab Çıxarışı Al")
        }
    }
}

@Composable
private fun OtherPropertiesSection(properties: List<OtherProperty>) {
    SectionCard(
        title = "Sahibin Digər Əmlakı",
        icon = Icons.Default.HomeWork,
        badge = "${properties.size} əlaqəli əmlak",
    ) {
        properties.forEach { property ->
            OtherPropertyCard(property)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OtherPropertyCard(property: OtherProperty) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
private fun BottomActions(property: PropertyDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { /* TODO: Call */ },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DetailColors.Forest,
                contentColor = Color.White,
            ),
            border = BorderStroke(0.dp, Color.Transparent),
        ) {
            Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Zəng et", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { /* TODO: Send debt notice */ },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DetailColors.Terracotta,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.Notifications, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Borc Bildirişi", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = DetailColors.Terracotta)
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
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
