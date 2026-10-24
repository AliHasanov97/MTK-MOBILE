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
import az.mtk.app.AppGraph
import az.mtk.app.data.api.MtkService
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.SearchBody
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import kotlinx.coroutines.delay

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
    onBuildingClick: (buildingId: String) -> Unit = {},
    canManage: Boolean = false,
    viewModel: PropertiesViewModel = mtkViewModel { PropertiesViewModel() },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCreateBuilding by remember { mutableStateOf(false) }
    var showCreateGarage by remember { mutableStateOf(false) }
    var showCreateOwner by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var apartmentSort by remember { mutableStateOf(ApartmentSort.BUILDING) }
    var sortAscending by remember { mutableStateOf(true) }
    var residentSort by remember { mutableStateOf(ResidentSort.NAME) }
    var garageSort by remember { mutableStateOf(GarageSort.NUMBER) }

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

    if (showCreateBuilding) {
        CreateBuildingSheet(
            onDismiss = { showCreateBuilding = false },
            onSaved = {
                showCreateBuilding = false
                viewModel.refresh()
            },
        )
    }

    if (showCreateGarage) {
        CreateGarageSheet(
            onDismiss = { showCreateGarage = false },
            onSaved = {
                showCreateGarage = false
                viewModel.refresh()
            },
        )
    }

    if (showCreateOwner) {
        CreateOwnerSheet(
            onDismiss = { showCreateOwner = false },
            onSaved = {
                showCreateOwner = false
                viewModel.refresh()
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
                    // Filtr yalnız əmlak siyahılarına aiddir — Binalar və
                    // Sakinlər tablarında gizlədilir.
                    val showFilters = uiState.selectedTab == PropertyTabType.APARTMENTS ||
                        uiState.selectedTab == PropertyTabType.GARAGES
                    if (showFilters) {
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

                    // Sıralama — panelin cədvəl başlıqlarındakı sıralama ilə eyni.
                    if (showFilters || uiState.selectedTab == PropertyTabType.RESIDENTS) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PropertyColors.Container)
                                    .clickable { sortMenuOpen = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    "Sırala",
                                    tint = PropertyColors.Terracotta,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            SortMenu(
                                expanded = sortMenuOpen,
                                selectedTab = uiState.selectedTab,
                                apartmentSort = apartmentSort,
                                residentSort = residentSort,
                                garageSort = garageSort,
                                ascending = sortAscending,
                                onDismiss = { sortMenuOpen = false },
                                onApartmentSort = { apartmentSort = it; sortMenuOpen = false },
                                onResidentSort = { residentSort = it; sortMenuOpen = false },
                                onGarageSort = { garageSort = it; sortMenuOpen = false },
                                onToggleDirection = { sortAscending = !sortAscending; sortMenuOpen = false },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    val addLabel = when (uiState.selectedTab) {
                        PropertyTabType.BUILDINGS -> "Bina"
                        PropertyTabType.GARAGES -> "Qaraj"
                        PropertyTabType.RESIDENTS -> "Sahib"
                        else -> null
                    }
                    if (canManage && addLabel != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PropertyColors.Terracotta)
                                .clickable {
                                    when (uiState.selectedTab) {
                                        PropertyTabType.BUILDINGS -> showCreateBuilding = true
                                        PropertyTabType.GARAGES -> showCreateGarage = true
                                        PropertyTabType.RESIDENTS -> showCreateOwner = true
                                        else -> Unit
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "Yeni $addLabel",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
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
            val onBuildingsTab = uiState.selectedTab == PropertyTabType.BUILDINGS
            OutlinedTextField(
                value = if (onBuildingsTab) uiState.buildingsQuery else uiState.searchQuery,
                onValueChange = {
                    if (onBuildingsTab) viewModel.setBuildingsQuery(it) else viewModel.setSearchQuery(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = {
                    Text(
                        text = when (uiState.selectedTab) {
                            PropertyTabType.BUILDINGS -> "Axtar (bina adı, ünvan…)"
                            PropertyTabType.RESIDENTS -> "Axtar (ad, e-poçt, telefon…)"
                            PropertyTabType.GARAGES -> "Axtar (nömrə, növ, sahibi…)"
                            PropertyTabType.APARTMENTS -> "Axtar (bina, mənzil, sahib…)"
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
                        PropertyTab.BUILDINGS -> PropertyTabType.BUILDINGS
                        PropertyTab.APARTMENTS -> PropertyTabType.APARTMENTS
                        PropertyTab.GARAGES -> PropertyTabType.GARAGES
                        PropertyTab.RESIDENTS -> PropertyTabType.RESIDENTS
                    }
                    val selected = uiState.selectedTab == tabType
                    val count = when (tab) {
                        PropertyTab.BUILDINGS -> stats.totalBuildings
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

            // Stats row — yalnız mənzil/qaraj tablarında.
            if (uiState.selectedTab == PropertyTabType.APARTMENTS ||
                uiState.selectedTab == PropertyTabType.GARAGES
            ) {
                StatsRow(stats)
            }

            when {
                uiState.selectedTab == PropertyTabType.BUILDINGS -> BuildingsContent(
                    buildings = uiState.buildings,
                    apartmentCounts = uiState.apartmentCounts,
                    query = uiState.buildingsQuery,
                    loading = uiState.buildingsLoading,
                    error = uiState.buildingsError,
                    onRetry = { viewModel.load() },
                    onBuildingClick = onBuildingClick,
                )

                uiState.selectedTab == PropertyTabType.RESIDENTS -> ResidentsContent(
                    residents = uiState.residents,
                    query = uiState.searchQuery,
                    loading = uiState.residentsLoading,
                    error = uiState.residentsError,
                    sort = residentSort,
                    ascending = sortAscending,
                    onRetry = { viewModel.loadResidents(force = true) },
                    onResidentClick = onResidentClick,
                )

                uiState.loading -> PropertiesLoadingState()
                uiState.error != null -> PropertiesErrorState(uiState.error ?: "") { viewModel.load() }
                properties.isEmpty() -> EmptyState()
                else -> {
                    val sortedProperties = remember(
                        properties,
                        uiState.selectedTab,
                        apartmentSort,
                        garageSort,
                        sortAscending,
                    ) {
                        val comparator = when (uiState.selectedTab) {
                            PropertyTabType.GARAGES -> when (garageSort) {
                                GarageSort.NUMBER -> Comparator<PropertyItem> { a, b ->
                                    (a.number?.toIntOrNull() ?: Int.MAX_VALUE)
                                        .compareTo(b.number?.toIntOrNull() ?: Int.MAX_VALUE)
                                }
                                GarageSort.TYPE -> Comparator<PropertyItem> { a, b ->
                                    a.subtitle.compareTo(b.subtitle, ignoreCase = true)
                                }
                            }
                            else -> when (apartmentSort) {
                                ApartmentSort.BUILDING -> Comparator<PropertyItem> { a, b ->
                                    a.buildingName.orEmpty().compareTo(b.buildingName.orEmpty(), ignoreCase = true)
                                }
                                ApartmentSort.NUMBER -> Comparator<PropertyItem> { a, b ->
                                    (a.number?.toIntOrNull() ?: Int.MAX_VALUE)
                                        .compareTo(b.number?.toIntOrNull() ?: Int.MAX_VALUE)
                                }
                                ApartmentSort.FLOOR -> Comparator<PropertyItem> { a, b ->
                                    (a.floor ?: Int.MAX_VALUE).compareTo(b.floor ?: Int.MAX_VALUE)
                                }
                                ApartmentSort.ROOMS -> Comparator<PropertyItem> { a, b ->
                                    (a.roomCount ?: Int.MAX_VALUE).compareTo(b.roomCount ?: Int.MAX_VALUE)
                                }
                                ApartmentSort.AREA -> Comparator<PropertyItem> { a, b ->
                                    (a.area ?: Double.MAX_VALUE).compareTo(b.area ?: Double.MAX_VALUE)
                                }
                                ApartmentSort.STATUS -> Comparator<PropertyItem> { a, b ->
                                    a.status.orEmpty().compareTo(b.status.orEmpty(), ignoreCase = true)
                                }
                            }
                        }
                        properties.sortedWith(if (sortAscending) comparator else comparator.reversed())
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = sortedProperties,
                            key = { property -> property.id }
                        ) { property ->
                            PropertyCard(
                                property = property,
                                onClick = {
                                    val propType = when (property.type) {
                                        PropertyItemType.APARTMENT -> PropertyType.APARTMENT
                                        PropertyItemType.GARAGE -> PropertyType.GARAGE
                                    }
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

// ------------------------------------------------------------- sıralama menyusu

@Composable
private fun SortMenu(
    expanded: Boolean,
    selectedTab: PropertyTabType,
    apartmentSort: ApartmentSort,
    residentSort: ResidentSort,
    garageSort: GarageSort,
    ascending: Boolean,
    onDismiss: () -> Unit,
    onApartmentSort: (ApartmentSort) -> Unit,
    onResidentSort: (ResidentSort) -> Unit,
    onGarageSort: (GarageSort) -> Unit,
    onToggleDirection: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        when (selectedTab) {
            PropertyTabType.APARTMENTS -> ApartmentSort.entries.forEach { sort ->
                SortMenuItem(sort.label, sort == apartmentSort) { onApartmentSort(sort) }
            }

            PropertyTabType.GARAGES -> GarageSort.entries.forEach { sort ->
                SortMenuItem(sort.label, sort == garageSort) { onGarageSort(sort) }
            }

            PropertyTabType.RESIDENTS -> ResidentSort.entries.forEach { sort ->
                SortMenuItem(sort.label, sort == residentSort) { onResidentSort(sort) }
            }

            else -> Unit
        }

        HorizontalDivider(color = PropertyColors.Border)
        DropdownMenuItem(
            text = {
                Text(
                    text = if (ascending) "Azalan sıra" else "Artan sıra",
                    fontSize = 13.sp,
                    color = PropertyColors.Ink,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.SwapVert,
                    null,
                    tint = PropertyColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onToggleDirection,
        )
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = PropertyColors.Ink,
            )
        },
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = PropertyColors.Terracotta,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = onClick,
    )
}

// ------------------------------------------------------------- sakinlər

@Composable
private fun ResidentsContent(
    residents: List<OwnerItemDto>,
    query: String,
    loading: Boolean,
    error: String?,
    sort: ResidentSort,
    ascending: Boolean,
    onRetry: () -> Unit,
    onResidentClick: (String) -> Unit,
) {
    val filtered = remember(residents, query, sort, ascending) {
        val term = query.trim().lowercase()
        val matching = if (term.isEmpty()) {
            residents
        } else {
            residents.filter { resident ->
                resident.fullName?.lowercase()?.contains(term) == true ||
                    resident.email?.lowercase()?.contains(term) == true ||
                    resident.phoneNumber?.contains(term) == true
            }
        }
        val comparator = when (sort) {
            ResidentSort.NAME -> Comparator<OwnerItemDto> { a, b ->
                a.fullName.orEmpty().compareTo(b.fullName.orEmpty(), ignoreCase = true)
            }
            ResidentSort.EMAIL -> Comparator<OwnerItemDto> { a, b ->
                a.email.orEmpty().compareTo(b.email.orEmpty(), ignoreCase = true)
            }
        }
        matching.sortedWith(if (ascending) comparator else comparator.reversed())
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
    BUILDINGS("Binalar"),
    APARTMENTS("Mənzillər"),
    GARAGES("Qarajlar"),
    RESIDENTS("Sakinlər"),
}

enum class ApartmentSort(val label: String) {
    BUILDING("Bina"),
    NUMBER("Nömrə"),
    FLOOR("Mərtəbə"),
    ROOMS("Otaq"),
    AREA("Sahə"),
    STATUS("Status"),
}

enum class GarageSort(val label: String) {
    NUMBER("Nömrə"),
    TYPE("Növ"),
}

enum class ResidentSort(val label: String) {
    NAME("Ad Soyad"),
    EMAIL("E-poçt"),
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
    var selectedOwner by remember { mutableStateOf<OwnerItemDto?>(null) }
    var noOwner by remember { mutableStateOf(currentFilters.noOwner) }
    var minArea by remember {
        mutableStateOf(if (currentFilters.minArea > 0.0) currentFilters.minArea.toString() else "")
    }
    var maxArea by remember {
        mutableStateOf(if (currentFilters.maxArea < 1000.0) currentFilters.maxArea.toString() else "")
    }
    var ownerQuery by remember { mutableStateOf("") }
    var ownerResults by remember { mutableStateOf<List<OwnerItemDto>>(emptyList()) }
    var ownerLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(ownerQuery) {
        if (ownerQuery.isBlank()) {
            ownerResults = emptyList()
            ownerLoading = false
            return@LaunchedEffect
        }
        delay(350)
        ownerLoading = true
        ownerResults = runCatching {
            AppGraph.service(MtkService::class.java)
                .owners(SearchBody(searchTerm = ownerQuery.trim(), pageSize = 8))
                .data?.items.orEmpty()
        }.getOrDefault(emptyList())
        ownerLoading = false
    }

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
                            minArea = minArea.toDoubleOrNull() ?: 0.0,
                            maxArea = maxArea.toDoubleOrNull() ?: 1000.0,
                            status = selectedStatus,
                            hasOwnerOnly = hasOwnerOnly,
                            ownerIds = listOfNotNull(selectedOwner?.id),
                            noOwner = noOwner,
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

                // Sahib üzrə filtr — panelin "Sahibi" sütunu.
                Text(
                    "Sahibi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PropertyColors.Ink,
                )
                OwnerPicker(
                    selected = selectedOwner,
                    owners = ownerResults,
                    query = ownerQuery,
                    loading = ownerLoading,
                    onQueryChange = { ownerQuery = it },
                    onSelect = { selectedOwner = it },
                )
                FilterChip(
                    selected = noOwner,
                    onClick = { noOwner = !noOwner },
                    label = { Text("Yalnız sahibsiz", fontSize = 12.sp) },
                )

                HorizontalDivider(color = PropertyColors.Border)

                // Sahə aralığı — panelin "Sahə (m²)" sütunu.
                Text(
                    "Sahə (m²)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PropertyColors.Ink,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minArea,
                        onValueChange = { minArea = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Min", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = maxArea,
                        onValueChange = { maxArea = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Maks", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        modifier = Modifier.weight(1f),
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
                        selectedOwner = null
                        ownerQuery = ""
                        noOwner = false
                        minArea = ""
                        maxArea = ""
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
