package az.mtk.app.ui.screens.properties

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.ApartmentDto
import az.mtk.app.data.api.ApartmentTypeDto
import az.mtk.app.data.api.BuildingDto
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.Civic

/**
 * Bina detalları — MTK-UI "Bina siyahısı" səhifəsinin mobil qarşılığı:
 * bina məlumatı, mərtəbə sxemi, mənzil tipləri və binadakı mənzillər.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailScreen(
    buildingId: String,
    onBack: () -> Unit,
    onOpenProperty: (String, PropertyType) -> Unit,
    canManage: Boolean = false,
    viewModel: BuildingDetailViewModel = mtkViewModel(key = "building-$buildingId") {
        BuildingDetailViewModel(buildingId)
    },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var showScheme by remember { mutableStateOf(false) }
    var showTypes by remember { mutableStateOf(false) }
    var showCreateApartment by remember { mutableStateOf(false) }

    LaunchedEffect(buildingId) {
        viewModel.load()
    }

    // "Mənzil yaradıldı." kimi geri bildiriş özü itir.
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearFeedback()
        }
    }

    BackHandler(enabled = showScheme) {
        showScheme = false
    }

    if (showScheme) {
        BuildingSchemeScreen(
            building = uiState.building,
            apartments = uiState.apartments,
            types = uiState.types,
            onBack = { showScheme = false },
            onOpenProperty = { id ->
                showScheme = false
                onOpenProperty(id, PropertyType.APARTMENT)
            },
        )
    } else {
        Scaffold(
            containerColor = Civic.Background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Bina detalları",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Civic.Ink,
                            )
                            uiState.building?.let { building ->
                                Text(
                                    text = building.name,
                                    fontSize = 12.sp,
                                    color = Civic.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Geri",
                                tint = Civic.Ink,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Civic.Background),
                )
            },
        ) { padding ->
            when {
                uiState.loading && uiState.building == null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Civic.Terracotta)
                }

                uiState.error != null && uiState.building == null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(uiState.error ?: "", fontSize = 14.sp, color = Civic.Muted)
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
                        ) { Text("Yenidən cəhd et") }
                    }
                }

                uiState.building != null -> BuildingContent(
                    building = uiState.building!!,
                    apartments = uiState.apartments,
                    message = uiState.message,
                    canManage = canManage,
                    onOpenScheme = { showScheme = true },
                    onOpenTypes = { showTypes = true },
                    onCreateApartment = { showCreateApartment = true },
                    onOpenProperty = onOpenProperty,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    if (showTypes) {
        ApartmentTypesSheet(
            types = uiState.types,
            saving = uiState.saving,
            error = uiState.formError,
            onDismiss = {
                viewModel.clearFeedback()
                showTypes = false
            },
            onAdd = { roomCount, area ->
                viewModel.createType(roomCount, area) {}
            },
        )
    }

    if (showCreateApartment) {
        CreateApartmentSheet(
            types = uiState.types,
            saving = uiState.saving,
            error = uiState.formError,
            onDismiss = {
                viewModel.clearFeedback()
                showCreateApartment = false
            },
            onCreate = { number, floor, typeId ->
                viewModel.createApartment(number, floor, typeId) {
                    showCreateApartment = false
                }
            },
        )
    }
}

@Composable
private fun BuildingContent(
    building: BuildingDto,
    apartments: List<ApartmentDto>,
    message: String?,
    canManage: Boolean,
    onOpenScheme: () -> Unit,
    onOpenTypes: () -> Unit,
    onCreateApartment: () -> Unit,
    onOpenProperty: (String, PropertyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val occupied = apartments.count { it.currentOwner != null }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Civic.Background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Civic.Surface),
                border = BorderStroke(1.dp, Civic.Border),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = building.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Civic.Ink,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(building.status)
                    }
                    Text(
                        text = building.fullAddress?.takeIf { it.isNotBlank() } ?: "Ünvan göstərilməyib",
                        fontSize = 13.sp,
                        color = Civic.Muted,
                    )
                    building.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = Civic.MutedSoft,
                        )
                    }

                    HorizontalDivider(color = Civic.Border)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatBlock(apartments.size.toString(), "Mənzil")
                        StatBlock(occupied.toString(), "Məskun")
                        StatBlock((apartments.size - occupied).toString(), "Boş")
                    }
                }
            }
        }

        if (canManage) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BuildingAction(
                        icon = Icons.Default.GridView,
                        label = "Sxem",
                        onClick = onOpenScheme,
                        modifier = Modifier.weight(1f),
                    )
                    BuildingAction(
                        icon = Icons.Default.Category,
                        label = "Tiplər",
                        onClick = onOpenTypes,
                        modifier = Modifier.weight(1f),
                    )
                    BuildingAction(
                        icon = Icons.Default.Add,
                        label = "Mənzil",
                        onClick = onCreateApartment,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            item {
                BuildingAction(
                    icon = Icons.Default.GridView,
                    label = "Mərtəbə sxemi",
                    onClick = onOpenScheme,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (message != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Civic.Olive.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = Civic.Olive,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mənzillər",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Civic.Ink,
                )
                Text(
                    text = "${apartments.size} qeyd",
                    fontSize = 12.sp,
                    color = Civic.MutedSoft,
                )
            }
        }

        if (apartments.isEmpty()) {
            item {
                Text(
                    text = "Bu binada hələ mənzil qeydə alınmayıb.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontSize = 13.sp,
                    color = Civic.Muted,
                )
            }
        } else {
            items(items = apartments, key = { it.id }) { apartment ->
                ApartmentRow(apartment) {
                    onOpenProperty(apartment.id, PropertyType.APARTMENT)
                }
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Civic.Ink,
        )
        Text(text = label, fontSize = 11.sp, color = Civic.Muted)
    }
}

@Composable
private fun BuildingAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Civic.Container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Civic.Terracotta, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Civic.Terracotta,
            )
        }
    }
}

@Composable
private fun ApartmentRow(apartment: ApartmentDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Civic.Terracotta.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = apartment.apartmentNumber ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Civic.Terracotta,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Mənzil ${apartment.apartmentNumber ?: "—"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Civic.Ink,
                )
                Text(
                    text = buildString {
                        append("${apartment.floor ?: "—"}-ci mərtəbə")
                        apartment.roomCount?.let { append(" · $it otaq") }
                        apartment.areaSquareMeters?.let { append(" · ${it.toInt()} m²") }
                    },
                    fontSize = 11.sp,
                    color = Civic.Muted,
                )
                Text(
                    text = apartment.currentOwner?.name ?: "Sahibsiz",
                    fontSize = 11.sp,
                    color = if (apartment.currentOwner == null) Civic.MutedSoft else Civic.Olive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StatusPill(apartment.status)
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Civic.MutedSoft.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StatusPill(status: String?) {
    val normalized = status?.lowercase().orEmpty()
    val (color, label) = when (normalized) {
        "occupied" -> Civic.Olive to "Məskunlaşıb"
        "vacant" -> Civic.Terracotta to "Boş"
        "" -> Civic.MutedSoft to "—"
        else -> Civic.Muted to status!!
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ---------------------------------------------------------------- sxem

/**
 * Bina sxemi — panelin BinaSxemiModal-ının mobil versiyası. Tam ekran:
 * yuxarıda bina xülasəsi və status legandası, altında mərtəbə-mərtəbə
 * "fasad" (hər mərtəbə öz mənzil pəncərələri ilə), ən altda giriş.
 * Pəncərəyə toxunanda aşağıda mənzil kartı açılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildingSchemeScreen(
    building: BuildingDto?,
    apartments: List<ApartmentDto>,
    types: List<ApartmentTypeDto>,
    onBack: () -> Unit,
    onOpenProperty: (String) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = apartments.firstOrNull { it.id == selectedId }

    val byFloor = remember(apartments) {
        apartments
            .groupBy { it.floor ?: 0 }
            .toSortedMap(compareByDescending { it })
    }
    val highestFloor = remember(byFloor) {
        (byFloor.keys.maxOrNull() ?: 1).coerceAtLeast(1)
    }
    val floorsWithUnits = remember(byFloor) {
        byFloor.values.count { it.isNotEmpty() }
    }
    val emptyFloors = remember(byFloor, highestFloor) {
        (1..highestFloor).filter { byFloor[it].isNullOrEmpty() }
    }
    val statusGroups = remember(apartments) {
        apartments
            .groupBy { schemeStatusLabel(it.status) }
            .toSortedMap(compareBy { it })
    }

    Scaffold(
        containerColor = Civic.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bina sxemi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Civic.Ink,
                        )
                        Text(
                            text = building?.name.orEmpty(),
                            fontSize = 12.sp,
                            color = Civic.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Geri",
                            tint = Civic.Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Civic.Background),
            )
        },
        bottomBar = {
            selected?.let { apartment ->
                SchemeInspector(
                    apartment = apartment,
                    onClose = { selectedId = null },
                    onOpen = { onOpenProperty(apartment.id) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SchemeSummaryCard(
                    building = building,
                    apartmentCount = apartments.size,
                    floorCount = floorsWithUnits,
                    typeCount = types.size,
                )
            }

            if (apartments.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Civic.Surface,
                        border = BorderStroke(1.dp, Civic.Border),
                    ) {
                        Text(
                            text = "Bu binada hələ heç bir mənzil qeydə alınmayıb.",
                            modifier = Modifier.padding(20.dp),
                            fontSize = 13.sp,
                            color = Civic.Muted,
                        )
                    }
                }
            } else {
                item {
                    SchemeLegend(statusGroups)
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp, top = 4.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Civic.ForestDeep, Civic.Forest)
                                )
                            ),
                    )
                }

                items(
                    items = (highestFloor downTo 1).toList(),
                    key = { floor -> "floor-$floor" },
                ) { floor ->
                    SchemeFloorRow(
                        floor = floor,
                        units = byFloor[floor].orEmpty(),
                        selectedId = selectedId,
                        onSelect = { selectedId = it },
                    )
                }

                item {
                    SchemePlinth(apartmentCount = apartments.size)
                }

                if (emptyFloors.isNotEmpty()) {
                    item {
                        Text(
                            text = "${emptyFloors.size} mərtəbədə mənzil qeydiyyatı yoxdur " +
                                "(${emptyFloors.joinToString(", ")}) — sxemdə boş sətir kimi saxlanılıb.",
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Civic.MutedSoft,
                        )
                    }
                }

                item {
                    Text(
                        text = "Məlumat üçün mənzil pəncərəsinə toxunun; " +
                            "aşağıdakı kartdan mənzil səhifəsinə keçə bilərsiniz.",
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Civic.MutedSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeSummaryCard(
    building: BuildingDto?,
    apartmentCount: Int,
    floorCount: Int,
    typeCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Civic.Surface,
        border = BorderStroke(1.dp, Civic.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = building?.name ?: "Bina",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
            )
            Text(
                text = building?.fullAddress?.takeIf { it.isNotBlank() } ?: "Ünvan göstərilməyib",
                fontSize = 12.sp,
                color = Civic.Muted,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Civic.Border,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SchemeStat(apartmentCount.toString(), "Mənzil")
                SchemeStat(floorCount.toString(), "Mərtəbə")
                SchemeStat(typeCount.toString(), "Tip")
            }
        }
    }
}

@Composable
private fun SchemeStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Civic.Ink,
        )
        Text(text = label, fontSize = 11.sp, color = Civic.Muted)
    }
}

@Composable
private fun SchemeLegend(statusGroups: Map<String, List<ApartmentDto>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        statusGroups.forEach { (label, units) ->
            val accent = schemeAccent(units.first().status)
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                    Text(
                        text = units.size.toString(),
                        fontSize = 11.sp,
                        color = Civic.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeFloorRow(
    floor: Int,
    units: List<ApartmentDto>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Mərtəbə nişanı + bina "onurğası"nı göstərən qısa xətt.
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (units.isEmpty()) Civic.Subtle else Civic.Forest,
            ) {
                Text(
                    text = floor.toString(),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (units.isEmpty()) Civic.MutedSoft else Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Civic.Border),
            )
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = Civic.Surface,
            border = BorderStroke(1.dp, Civic.Border),
        ) {
            if (units.isEmpty()) {
                Text(
                    text = "Mənzil qeydiyyatı yoxdur",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    fontSize = 11.sp,
                    color = Civic.MutedSoft,
                )
            } else {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    units.chunked(2).forEach { rowUnits ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowUnits.forEach { unit ->
                                SchemeUnitCard(
                                    apartment = unit,
                                    selected = unit.id == selectedId,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelect(unit.id) },
                                )
                            }
                            repeat(2 - rowUnits.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeUnitCard(
    apartment: ApartmentDto,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = schemeAccent(apartment.status)

    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = if (selected) 0.20f else 0.07f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = accent.copy(alpha = if (selected) 0.9f else 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "№ ${apartment.apartmentNumber ?: "—"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
            Text(
                text = buildString {
                    append("${apartment.roomCount ?: "—"} otaq")
                    apartment.areaSquareMeters?.let { append(" · ${it.toInt()} m²") }
                },
                fontSize = 10.sp,
                color = Civic.Muted,
                maxLines = 1,
            )
            Text(
                text = apartment.currentOwner?.name ?: "Sahibsiz",
                fontSize = 10.sp,
                color = if (apartment.currentOwner == null) Civic.MutedSoft else accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SchemePlinth(apartmentCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(Civic.ForestDeep.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(Civic.ForestDeep),
        )
        Text(
            text = "Giriş",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Civic.Forest,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$apartmentCount mənzil",
            fontSize = 10.sp,
            color = Civic.MutedSoft,
        )
    }
}

@Composable
private fun SchemeInspector(
    apartment: ApartmentDto,
    onClose: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = schemeAccent(apartment.status)

    Surface(
        color = Civic.Surface,
        shadowElevation = 12.dp,
    ) {
        Column {
            HorizontalDivider(color = Civic.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = apartment.apartmentNumber ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Mənzil ${apartment.apartmentNumber ?: "—"} · " +
                            "${apartment.floor ?: "—"}-ci mərtəbə",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Civic.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append("${apartment.roomCount ?: "—"} otaq")
                            apartment.areaSquareMeters?.let { append(" · ${it.toInt()} m²") }
                            append(" · ${schemeStatusLabel(apartment.status)}")
                        },
                        fontSize = 11.sp,
                        color = Civic.Muted,
                        maxLines = 1,
                    )
                    Text(
                        text = apartment.currentOwner?.name ?: "Sahibsiz",
                        fontSize = 11.sp,
                        color = if (apartment.currentOwner == null) Civic.MutedSoft else accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        "Bağla",
                        tint = Civic.MutedSoft,
                    )
                }
            }

            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) {
                Text("Mənzil səhifəsinə keç")
            }
        }
    }
}

private fun schemeAccent(status: String?): Color = when (status?.trim()?.lowercase()) {
    "occupied", "məskun", "məskunlaşıb", "sakin" -> Civic.Olive
    "vacant", "boş" -> Civic.Terracotta
    null, "" -> Civic.MutedSoft
    else -> Civic.Forest
}

private fun schemeStatusLabel(status: String?): String {
    val trimmed = status?.trim().orEmpty()
    return when (trimmed.lowercase()) {
        "occupied" -> "Məskun"
        "vacant" -> "Boş"
        "" -> "Status yoxdur"
        else -> trimmed
    }
}

// ---------------------------------------------------------------- mənzil tipləri

@Composable
private fun ApartmentTypesSheet(
    types: List<ApartmentTypeDto>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAdd: (Int, Double) -> Unit,
) {
    var roomCount by remember { mutableStateOf("1") }
    var area by remember { mutableStateOf("") }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = "Mənzil tipləri",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (types.isEmpty()) {
                    Text(
                        "Hələ heç bir tip əlavə edilməyib.",
                        fontSize = 13.sp,
                        color = Civic.Muted,
                    )
                } else {
                    types.forEach { type ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Civic.Subtle,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${type.roomCount ?: "—"} otaq",
                                    fontSize = 13.sp,
                                    color = Civic.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${type.areaSquareMeters?.toInt() ?: "—"} m²",
                                    fontSize = 13.sp,
                                    color = Civic.Muted,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Civic.Border)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomCount,
                        onValueChange = { roomCount = it.filter(Char::isDigit) },
                        label = { Text("Otaq sayı", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = area,
                        onValueChange = { value ->
                            area = value.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Sahə (m²)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                error?.let { Text(it, fontSize = 12.sp, color = Civic.Error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rooms = roomCount.toIntOrNull()
                    val squareMeters = area.toDoubleOrNull()
                    if (rooms != null && squareMeters != null) {
                        onAdd(rooms, squareMeters)
                        area = ""
                    }
                },
                enabled = roomCount.toIntOrNull() != null &&
                    (area.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text(if (saving) "Saxlanılır…" else "Tip əlavə et") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Bağla", color = Civic.Muted)
            }
        },
        containerColor = Civic.Surface,
    )
}

// ---------------------------------------------------------------- yeni mənzil

@Composable
private fun CreateApartmentSheet(
    types: List<ApartmentTypeDto>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, Int, String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("1") }
    var selectedTypeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(types) {
        if (selectedTypeId == null) selectedTypeId = types.firstOrNull()?.id
    }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = "Yeni mənzil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Mənzil nömrəsi", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it.filter(Char::isDigit) },
                    label = { Text("Mərtəbə", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Mənzil tipi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Civic.Muted,
                )

                if (types.isEmpty()) {
                    Text(
                        "Bu binada tip yoxdur — əvvəlcə \"Tiplər\" bölməsindən tip əlavə edin.",
                        fontSize = 12.sp,
                        color = Civic.MutedSoft,
                    )
                } else {
                    types.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { type ->
                                FilterChip(
                                    selected = selectedTypeId == type.id,
                                    onClick = { selectedTypeId = type.id },
                                    label = {
                                        Text(
                                            "${type.roomCount} otaq · ${type.areaSquareMeters?.toInt()} m²",
                                            fontSize = 11.sp,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                error?.let { Text(it, fontSize = 12.sp, color = Civic.Error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val floorNumber = floor.toIntOrNull()
                    val typeId = selectedTypeId
                    if (number.isNotBlank() && floorNumber != null && typeId != null) {
                        onCreate(number.trim(), floorNumber, typeId)
                    }
                },
                enabled = number.isNotBlank() &&
                    floor.toIntOrNull() != null &&
                    selectedTypeId != null &&
                    !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text(if (saving) "Saxlanılır…" else "Yarat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = Civic.Muted)
            }
        },
        containerColor = Civic.Surface,
    )
}
