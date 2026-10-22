package az.mtk.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.BrandMark
import az.mtk.app.ui.screens.DashboardScreen
import az.mtk.app.ui.screens.MoreScreen
import az.mtk.app.ui.screens.apartments.ApartmentDetailScreen
import az.mtk.app.ui.screens.apartments.ApartmentsScreen
import az.mtk.app.ui.screens.buildings.BuildingDetailScreen
import az.mtk.app.ui.screens.buildings.BuildingsScreen
import az.mtk.app.ui.screens.charges.ChargesScreen
import az.mtk.app.ui.screens.garages.GarageDetailScreen
import az.mtk.app.ui.screens.garages.GaragesScreen
import az.mtk.app.ui.screens.hr.EmployeeDetailScreen
import az.mtk.app.ui.screens.hr.HrScreen
import az.mtk.app.ui.screens.owners.OwnerDetailScreen
import az.mtk.app.ui.screens.owners.OwnersScreen
import az.mtk.app.ui.theme.MtkColors
import kotlinx.coroutines.launch

object Routes {
    const val Dashboard = "dashboard"
    const val Buildings = "buildings"
    const val Apartments = "apartments"
    const val Owners = "owners"
    const val Garages = "garages"
    const val Charges = "charges"
    const val Hr = "hr"
    const val More = "more"

    const val BuildingDetail = "building/{id}"
    const val ApartmentDetail = "apartment/{id}"
    const val OwnerDetail = "owner/{id}"
    const val GarageDetail = "garage/{id}"
    const val EmployeeDetail = "employee/{id}"

    fun building(id: String) = "building/$id"
    fun apartment(id: String) = "apartment/$id"
    fun owner(id: String) = "owner/$id"
    fun garage(id: String) = "garage/$id"
    fun employee(id: String) = "employee/$id"
}

private data class SubTab(val label: String, val route: String)

private data class NavModule(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val rootRoute: String,
    val roles: List<String>? = null,
    val tabs: List<SubTab>? = null,
)

/**
 * Mirror of MTK-UI NAV_MODULES (app/panel/nav.ts):
 * One sidebar row per module; sub-pages live in the secondary horizontal tab strip.
 */
private val NAV_MODULES = listOf(
    NavModule(
        id = "dashboard",
        label = "Ümumi baxış",
        icon = Icons.Outlined.Dashboard,
        rootRoute = Routes.Dashboard,
    ),
    NavModule(
        id = "buildings",
        label = "Binalar",
        icon = Icons.Outlined.Apartment,
        rootRoute = Routes.Buildings,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
        tabs = listOf(
            SubTab("Binalar", Routes.Buildings),
            SubTab("Mənzillər", Routes.Apartments),
            SubTab("Qarajlar", Routes.Garages),
            SubTab("Sahiblər", Routes.Owners),
        ),
    ),
    NavModule(
        id = "charges",
        label = "Borclar və Hesablar",
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        rootRoute = Routes.Charges,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT),
    ),
    NavModule(
        id = "hr",
        label = "İnsan Resursları (HR)",
        icon = Icons.Outlined.Badge,
        rootRoute = Routes.Hr,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    NavModule(
        id = "profile",
        label = "Mənim profilim",
        icon = Icons.Outlined.Person,
        rootRoute = Routes.More,
    ),
)

@Composable
fun AppNav(profile: UserProfile) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.Dashboard

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isDetailScreen = currentRoute.startsWith("building/") ||
        currentRoute.startsWith("apartment/") ||
        currentRoute.startsWith("owner/") ||
        currentRoute.startsWith("garage/") ||
        currentRoute.startsWith("employee/")

    val visibleModules = NAV_MODULES.filter { mod ->
        mod.roles == null || profile.hasAnyRole(*mod.roles.toTypedArray())
    }

    val activeModule = visibleModules.find { mod ->
        mod.rootRoute == currentRoute || mod.tabs?.any { it.route == currentRoute } == true
    } ?: visibleModules.first()

    val topBarTitle = when {
        currentRoute == Routes.Dashboard -> "Ümumi baxış"
        currentRoute == Routes.Buildings -> "Binalar"
        currentRoute == Routes.Apartments -> "Mənzillər"
        currentRoute == Routes.Garages -> "Qarajlar"
        currentRoute == Routes.Owners -> "Sahiblər"
        currentRoute == Routes.Charges -> "Borclar və Hesablar"
        currentRoute == Routes.Hr -> "İnsan Resursları (HR)"
        currentRoute == Routes.More -> "Mənim profilim"
        currentRoute.startsWith("building/") -> "Bina detalları"
        currentRoute.startsWith("apartment/") -> "Mənzil detalları"
        currentRoute.startsWith("owner/") -> "Sahib detalları"
        currentRoute.startsWith("garage/") -> "Qaraj detalları"
        currentRoute.startsWith("employee/") -> "İşçi detalları"
        else -> "MTK"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 0.dp,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
            ) {
                MtkSidebarContent(
                    profile = profile,
                    visibleModules = visibleModules,
                    activeModule = activeModule,
                    onSelectModule = { mod ->
                        scope.launch { drawerState.close() }
                        if (currentRoute != mod.rootRoute) {
                            nav.navigate(mod.rootRoute) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onClose = { scope.launch { drawerState.close() } },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        AppGraph.auth.logout()
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!isDetailScreen) {
                    PanelTopBar(
                        title = topBarTitle,
                        profile = profile,
                        activeModule = activeModule,
                        currentRoute = currentRoute,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateTab = { route ->
                            if (currentRoute != route) {
                                nav.navigate(route) {
                                    popUpTo(Routes.Buildings) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onOpenProfile = {
                            nav.navigate(Routes.More) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.Dashboard,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.Dashboard) {
                    DashboardScreen(
                        profile = profile,
                        onNavigate = { route -> nav.navigate(route) },
                        onOpenProfile = { nav.navigate(Routes.More) },
                        showTopBar = false,
                    )
                }

                composable(Routes.Buildings) {
                    BuildingsScreen(
                        onBack = null,
                        onOpenBuilding = { nav.navigate(Routes.building(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.BuildingDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    BuildingDetailScreen(
                        buildingId = id,
                        onBack = { nav.popBackStack() },
                        onOpenApartment = { nav.navigate(Routes.apartment(it)) },
                    )
                }

                composable(Routes.Apartments) {
                    ApartmentsScreen(
                        onBack = null,
                        onOpenApartment = { nav.navigate(Routes.apartment(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.ApartmentDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    ApartmentDetailScreen(
                        apartmentId = id,
                        onBack = { nav.popBackStack() },
                        onOpenOwner = { nav.navigate(Routes.owner(it)) },
                    )
                }

                composable(Routes.Owners) {
                    OwnersScreen(
                        onBack = null,
                        onOpenOwner = { nav.navigate(Routes.owner(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.OwnerDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    OwnerDetailScreen(
                        ownerId = id,
                        onBack = { nav.popBackStack() },
                        onOpenApartment = { nav.navigate(Routes.apartment(it)) },
                    )
                }

                composable(Routes.Garages) {
                    GaragesScreen(
                        onBack = { nav.popBackStack() },
                        onOpenGarage = { nav.navigate(Routes.garage(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.GarageDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    GarageDetailScreen(
                        garageId = id,
                        onBack = { nav.popBackStack() },
                        onOpenOwner = { nav.navigate(Routes.owner(it)) },
                    )
                }

                composable(Routes.Charges) {
                    ChargesScreen(
                        onBack = { nav.popBackStack() },
                        onOpenOwner = { nav.navigate(Routes.owner(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.Hr) {
                    HrScreen(
                        onBack = { nav.popBackStack() },
                        onOpenEmployee = { nav.navigate(Routes.employee(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.EmployeeDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    EmployeeDetailScreen(employeeId = id, onBack = { nav.popBackStack() })
                }

                composable(Routes.More) {
                    MoreScreen(
                        profile = profile,
                        onNavigate = { route -> nav.navigate(route) },
                        showTopBar = false,
                    )
                }
            }
        }
    }
}

/**
 * TopBar mirroring MTK-UI .panel-topbar and .panel-tabs:
 * Hamburger menu toggle + active title + user profile trigger + secondary tab strip.
 */
@Composable
private fun PanelTopBar(
    title: String,
    profile: UserProfile,
    activeModule: NavModule,
    currentRoute: String,
    onOpenDrawer: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menyu",
                        tint = MtkColors.Ink,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp, top = 1.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MtkColors.Positive),
                        )
                    }
                    Text(
                        "MTK İdarəetmə Paneli",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                    )
                }

                // Top right user profile trigger (MTK-UI .panel-user-trigger)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenProfile)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarCircle(
                        initials = profile.initials,
                        size = 34.dp,
                        background = MtkColors.Green,
                    )
                }
            }

            // Secondary Sub-Tabs Strip (MTK-UI .panel-tabs)
            if (activeModule.tabs != null) {
                val tabIndex = activeModule.tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                ScrollableTabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MtkColors.Green,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (tabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                color = MtkColors.Green,
                                height = 2.5.dp,
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    },
                ) {
                    activeModule.tabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        Tab(
                            selected = isSelected,
                            onClick = { onNavigateTab(tab.route) },
                            text = {
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MtkColors.Green else MtkColors.Muted,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Left Sidebar Drawer mirroring MTK-UI .panel-sidebar:
 * Top logo & brand + close button + nav items (.panel-nav-item) + logout at bottom (.panel-sidebar-logout).
 */
@Composable
private fun MtkSidebarContent(
    profile: UserProfile,
    visibleModules: List<NavModule>,
    activeModule: NavModule,
    onSelectModule: (NavModule) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp, horizontal = 14.dp),
    ) {
        // Sidebar Top (.panel-sidebar-top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 36.dp, letterSize = 19)
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "MTK",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp, top = 2.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MtkColors.Positive),
                        )
                    }
                    Text(
                        "VAHİD İDARƏETMƏ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Menyunu bağla",
                    tint = MtkColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // User info mini card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MtkColors.Soft,
            border = BorderStroke(1.dp, MtkColors.LineSoft),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarCircle(initials = profile.initials, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "@${profile.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        // Navigation links (.panel-nav)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            visibleModules.forEach { mod ->
                val isSelected = mod == activeModule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MtkColors.Green else Color.Transparent)
                        .clickable { onSelectModule(mod) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        mod.icon,
                        contentDescription = null,
                        tint = if (isSelected) MtkColors.Lime else MtkColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        mod.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MtkColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (mod.tabs != null) {
                        Text(
                            "${mod.tabs.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MtkColors.Lime else MtkColors.MutedSoft,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Logout at sidebar bottom (.panel-sidebar-logout)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onLogout),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFBF2F2),
            border = BorderStroke(1.dp, Color(0xFFF0D9D9)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = MtkColors.Negative,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Çıxış",
                    style = MaterialTheme.typography.labelLarge,
                    color = MtkColors.Negative,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
