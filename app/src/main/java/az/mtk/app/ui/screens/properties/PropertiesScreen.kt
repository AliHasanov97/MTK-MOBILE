package az.mtk.app.ui.screens.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format

private object PropertyColors {
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
    val Olive = Color(0xFF6F8559)
    val Error = Color(0xFFBA1A1A)
}

/**
 * Unified Properties (Əmlak Fondu) screen - combines apartments and garages
 * in one searchable, filterable list view following the Civic Hearth design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    onBack: () -> Unit,
    onPropertyClick: (propertyId: String, propertyType: PropertyType) -> Unit,
    onResidentClick: (residentId: String) -> Unit = {},
    viewModel: PropertiesViewModel = mtkViewModel { PropertiesViewModel() },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val properties = uiState.properties
    val stats = uiState.stats

    if (showFilterDialog) {
        FilterDialog(
            currentFilters = uiState.filters,
            buildings = uiState.availableBuildings,
            floors = uiState.availableFloors,
            rooms = uiState.availableRooms,
            onDismiss = { showFilterDialog = false },
            onApply = { filters ->
                viewModel.setFilters(filters)
                showFilterDialog = false
            },
        )
    }

    Scaffold(
        containerColor = PropertyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Əmlak Fondu",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PropertyColors.Ink,
                        )
                        Text(
                            text = "Bina, mənzil və qarajların vahid reyestri",
                            fontSize = 12.sp,
                            color = PropertyColors.Muted,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Geri",
                            tint = PropertyColors.Ink,
                        )
                    }
                },
                actions = {
                    // Filtr yalnız əmlak siyahılarına aiddir — Sakinlər tabında gizlədilir.
                    if (uiState.selectedTab != PropertyTabType.RESIDENTS) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PropertyColors.Container)
                                .clickable { showFilterDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = PropertyColors.Terracotta,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PropertyColors.Background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = {
                    Text(
                        if (uiState.selectedTab == PropertyTabType.RESIDENTS) {
                            "Axtar (ad, e-poçt, telefon…)"
                        } else {
                            "Axtar (bina, mənzil, sahib, qaraj…)"
                        },
                        color = PropertyColors.MutedSoft,
                        fontSize = 14.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = PropertyColors.MutedSoft,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PropertyColors.Terracotta,
                    unfocusedBorderColor = PropertyColors.Border,
                    focusedContainerColor = PropertyColors.Surface,
                    unfocusedContainerColor = PropertyColors.Surface,
                ),
                singleLine = true,
            )

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PropertyTab.entries.forEach { tab ->
                    val tabType = when (tab) {
                        PropertyTab.APARTMENTS -> PropertyTabType.APARTMENTS
                        PropertyTab.GARAGES -> PropertyTabType.GARAGES
                        PropertyTab.RESIDENTS -> PropertyTabType.RESIDENTS
                    }
                    val selected = uiState.selectedTab == tabType
                    val count = when (tab) {
                        PropertyTab.APARTMENTS -> stats.totalApartments
                        PropertyTab.GARAGES -> stats.totalGarages
                        PropertyTab.RESIDENTS -> uiState.residentsTotal
                    }

                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { viewModel.setTab(tabType) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) PropertyColors.Terracotta else PropertyColors.Subtle,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else PropertyColors.Ink,
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (selected) Color.White.copy(alpha = 0.3f)
                                else PropertyColors.Border,
                            ) {
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else PropertyColors.Muted,
                                )
                            }
                        }
                    }
                }
            }

            // Stats row — yalnız əmlak tablarında (Sakinlər tabında əmlak statistikası yersizdir).
            if (uiState.selectedTab != PropertyTabType.RESIDENTS) {
                StatsRow(stats)
            }

            when {
                uiState.selectedTab == PropertyTabType.RESIDENTS -> ResidentsContent(
                    residents = uiState.residents,
                    query = uiState.searchQuery,
                    loading = uiState.residentsLoading,
                    error = uiState.residentsError,
                    onRetry = { viewModel.loadResidents(force = true) },
                    onResidentClick = onResidentClick,
                )

                uiState.loading -> PropertiesLoadingState()
                uiState.error != null -> PropertiesErrorState(uiState.error ?: "") { viewModel.load() }
                properties.isEmpty() -> EmptyState()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = properties,
                            key = { property -> property.id }
                        ) { property ->
                            PropertyCard(
                                property = property,
                                onClick = {
                                    val propType = when (property.type) {
                                        PropertyItemType.APARTMENT -> PropertyType.APARTMENT
                                        PropertyItemType.GARAGE -> PropertyType.GARAGE
                                    }
                                    println("🏠 PropertyCard clicked: ID=${property.id}, type=$propType, title=${property.title}")
                                    onPropertyClick(property.id, propType)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- sakinlər

@Composable
private fun ResidentsContent(
    residents: List<OwnerItemDto>,
    query: String,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onResidentClick: (String) -> Unit,
) {
    val filtered = remember(residents, query) {
        val term = query.trim().lowercase()
        if (term.isEmpty()) {
            residents
        } else {
            residents.filter { resident ->
                resident.fullName?.lowercase()?.contains(term) == true ||
                    resident.email?.lowercase()?.contains(term) == true ||
                    resident.phoneNumber?.contains(term) == true
            }
        }
    }

    when {
        loading && residents.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = PropertyColors.Terracotta)
        }

        error != null && residents.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(error, fontSize = 14.sp, color = PropertyColors.Muted)
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PropertyColors.Terracotta),
                ) {
                    Text("Yenidən cəhd et")
                }
            }
        }

        filtered.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (query.isBlank()) "Hələ sakin qeydə alınmayıb." else "Nəticə tapılmadı.",
                fontSize = 14.sp,
                color = PropertyColors.Muted,
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = filtered, key = { it.id }) { resident ->
                ResidentCard(resident) { onResidentClick(resident.id) }
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = PropertyColors.Terracotta,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidentCard(resident: OwnerItemDto, onClick: () -> Unit) {
    val name = resident.fullName?.takeIf { it.isNotBlank() } ?: "Adsız sakin"
    val active = resident.isActive != false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PropertyColors.Surface),
        border = BorderStroke(1.dp, PropertyColors.Border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PropertyColors.Container),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = Format.initials(name),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PropertyColors.Terracotta,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PropertyColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = resident.email?.takeIf { it.isNotBlank() } ?: "—",
                    fontSize = 12.sp,
                    color = PropertyColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(resident.phoneNumber?.takeIf { it.isNotBlank() } ?: "—")
                        append(" · ")
                        append(if (resident.userId != null) "Qeydiyyatlı istifadəçi" else "Passiv sahib")
                    },
                    fontSize = 11.sp,
                    color = PropertyColors.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val accent = if (active) PropertyColors.Olive else PropertyColors.MutedSoft
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = if (active) "Aktiv" else "Deaktiv",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = PropertyColors.MutedSoft.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(stats: PropertyStatsData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PropertyColors.Subtle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(
            value = stats.totalApartments.toString(),
            label = "Mənzil",
        )
        StatItem(
            value = stats.totalGarages.toString(),
            label = "Qaraj Yeri",
        )
        StatItem(
            value = "${stats.occupancyRate}%",
            label = "Maskunlaşma",
        )
        StatItem(
            value = stats.overdue.toString(),
            label = "Gecikma",
            color = PropertyColors.Error,
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color = PropertyColors.Ink,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = PropertyColors.Muted,
        )
    }
}

@Composable
private fun PropertyCard(
    property: PropertyItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PropertyColors.Surface),
        border = BorderStroke(1.dp, PropertyColors.Border),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (property.type) {
                                PropertyItemType.APARTMENT -> PropertyColors.Terracotta.copy(alpha = 0.12f)
                                PropertyItemType.GARAGE -> PropertyColors.Olive.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (property.type) {
                            PropertyItemType.APARTMENT -> Icons.Default.Apartment
                            PropertyItemType.GARAGE -> Icons.Default.DirectionsCar
                        },
                        contentDescription = null,
                        tint = when (property.type) {
                            PropertyItemType.APARTMENT -> PropertyColors.Terracotta
                            PropertyItemType.GARAGE -> PropertyColors.Olive
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Content
                Column(Modifier.weight(1f)) {
                    Text(
                        text = property.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PropertyColors.Ink,
                    )
                    Text(
                        text = property.subtitle,
                        fontSize = 13.sp,
                        color = PropertyColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Arrow
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = PropertyColors.MutedSoft.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Badges row
            if (property.badges.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    property.badges.forEach { badge ->
                        PropertyBadge(badge)
                    }
                }
            }

            // Owner info
            if (property.ownerName != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = PropertyColors.Border)
                Spacer(Modifier.height(12.dp))

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
                            Icons.Default.Person,
                            null,
                            tint = PropertyColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = property.ownerName,
                            fontSize = 13.sp,
                            color = PropertyColors.Ink,
                            fontWeight = FontWeight.Medium,
                        )
                        if (property.ownerPhone != null) {
                            Text(
                                text = property.ownerPhone,
                                fontSize = 12.sp,
                                color = PropertyColors.Muted,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PropertyColors.Terracotta,
                        ),
                        border = BorderStroke(1.dp, PropertyColors.Border),
                    ) {
                        Text("Detallar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyBadge(badge: PropertyBadgeItem) {
    val (textColor, backgroundColor) = when (badge.type) {
        PropertyBadgeType.SUCCESS -> PropertyColors.Olive to PropertyColors.Olive.copy(alpha = 0.12f)
        PropertyBadgeType.WARNING -> PropertyColors.TerracottaVivid to PropertyColors.Container
        PropertyBadgeType.ERROR -> PropertyColors.Error to PropertyColors.Error.copy(alpha = 0.12f)
        PropertyBadgeType.NEUTRAL -> PropertyColors.Muted to PropertyColors.Subtle
    }

    Surface(
        shape = CircleShape,
        color = backgroundColor,
    ) {
        Text(
            text = badge.text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun PropertiesLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                color = PropertyColors.Terracotta,
                strokeWidth = 3.dp,
            )
            Text(
                text = "Yüklənir…",
                fontSize = 14.sp,
                color = PropertyColors.Muted,
            )
        }
    }
}

@Composable
private fun PropertiesErrorState(error: String, onRetry: () -> Unit) {
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
                tint = PropertyColors.Error,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Xəta baş verdi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PropertyColors.Ink,
            )
            Text(
                text = error,
                fontSize = 13.sp,
                color = PropertyColors.Muted,
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PropertyColors.Terracotta,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Yenidən cəhd et")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.SearchOff,
                null,
                tint = PropertyColors.MutedSoft,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Əmlak tapılmadı",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PropertyColors.Muted,
            )
            Text(
                text = "Axtarış parametrlərini dəyişdirin",
                fontSize = 13.sp,
                color = PropertyColors.MutedSoft,
            )
        }
    }
}

// ---- Data models ----

enum class PropertyTab(val label: String) {
    APARTMENTS("Mənzillər"),
    GARAGES("Qarajlar"),
    RESIDENTS("Sakinlər"),
}

enum class PropertyType {
    APARTMENT,
    GARAGE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    currentFilters: PropertyFilters,
    buildings: List<FilterOption>,
    floors: List<Int>,
    rooms: List<Int>,
    onDismiss: () -> Unit,
    onApply: (PropertyFilters) -> Unit,
) {
    var selectedBuildings by remember { mutableStateOf(currentFilters.buildingIds.toSet()) }
    var selectedFloors by remember { mutableStateOf(currentFilters.floors.toSet()) }
    var selectedRooms by remember { mutableStateOf(currentFilters.roomCounts.toSet()) }
    var selectedStatus by remember { mutableStateOf(currentFilters.status) }
    var hasOwnerOnly by remember { mutableStateOf(currentFilters.hasOwnerOnly) }
    val scrollState = rememberScrollState()

    AppSheetDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        PropertyFilters(
                            buildingIds = selectedBuildings.toList(),
                            floors = selectedFloors.toList(),
                            roomCounts = selectedRooms.toList(),
                            status = selectedStatus,
                            hasOwnerOnly = hasOwnerOnly,
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PropertyColors.Terracotta,
                ),
            ) {
                Text("Tətbiq et")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = PropertyColors.Muted)
            }
        },
        title = {
            Text(
                "Filterlə",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PropertyColors.Ink,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Building filter
                if (buildings.isNotEmpty()) {
                    Text(
                        "Bina",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PropertyColors.Ink,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        buildings.chunked(2).forEach { rowBuildings ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                rowBuildings.forEach { building ->
                                    FilterChip(
                                        selected = building.id in selectedBuildings,
                                        onClick = {
                                            selectedBuildings = if (building.id in selectedBuildings) {
                                                selectedBuildings - building.id
                                            } else {
                                                selectedBuildings + building.id
                                            }
                                        },
                                        label = { Text(building.name, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                // Fill remaining space if odd number
                                repeat(2 - rowBuildings.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = PropertyColors.Border)
                }

                // Floor filter
                if (floors.isNotEmpty()) {
                    Text(
                        "Mərtəbə",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PropertyColors.Ink,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        floors.chunked(3).forEach { rowFloors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                rowFloors.forEach { floor ->
                                    FilterChip(
                                        selected = floor in selectedFloors,
                                        onClick = {
                                            selectedFloors = if (floor in selectedFloors) {
                                                selectedFloors - floor
                                            } else {
                                                selectedFloors + floor
                                            }
                                        },
                                        label = { Text("$floor-ci", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                // Fill remaining space
                                repeat(3 - rowFloors.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = PropertyColors.Border)
                }

                // Room filter
                if (rooms.isNotEmpty()) {
                    Text(
                        "Otaq sayı",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PropertyColors.Ink,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rooms.chunked(3).forEach { rowRooms ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                rowRooms.forEach { room ->
                                    FilterChip(
                                        selected = room in selectedRooms,
                                        onClick = {
                                            selectedRooms = if (room in selectedRooms) {
                                                selectedRooms - room
                                            } else {
                                                selectedRooms + room
                                            }
                                        },
                                        label = { Text("$room otaq", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                // Fill remaining space
                                repeat(3 - rowRooms.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = PropertyColors.Border)
                }

                // Status filter
                Text(
                    "Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PropertyColors.Ink,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedStatus == "occupied",
                        onClick = {
                            selectedStatus = if (selectedStatus == "occupied") null else "occupied"
                        },
                        label = { Text("Məskunlaşıb", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PropertyColors.Olive.copy(alpha = 0.2f),
                            selectedLabelColor = PropertyColors.Olive,
                        ),
                    )
                    FilterChip(
                        selected = selectedStatus == "vacant",
                        onClick = {
                            selectedStatus = if (selectedStatus == "vacant") null else "vacant"
                        },
                        label = { Text("Boş", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PropertyColors.Terracotta.copy(alpha = 0.2f),
                            selectedLabelColor = PropertyColors.Terracotta,
                        ),
                    )
                }

                HorizontalDivider(color = PropertyColors.Border)

                // Owner filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Yalnız sahibli",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PropertyColors.Ink,
                    )
                    Switch(
                        checked = hasOwnerOnly,
                        onCheckedChange = { hasOwnerOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PropertyColors.Terracotta,
                            checkedTrackColor = PropertyColors.Terracotta.copy(alpha = 0.5f),
                        ),
                    )
                }

                HorizontalDivider(color = PropertyColors.Border)

                // Reset button
                TextButton(
                    onClick = {
                        selectedBuildings = emptySet()
                        selectedFloors = emptySet()
                        selectedRooms = emptySet()
                        selectedStatus = null
                        hasOwnerOnly = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Clear,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Filterləri sıfırla")
                }
            }
        },
        containerColor = PropertyColors.Surface,
        shape = RoundedCornerShape(20.dp),
    )
}
