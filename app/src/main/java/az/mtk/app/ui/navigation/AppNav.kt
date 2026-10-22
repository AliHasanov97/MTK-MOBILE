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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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
import az.mtk.app.ui.screens.admin.AuditScreen
import az.mtk.app.ui.screens.admin.CompanyProfileScreen
import az.mtk.app.ui.screens.apartments.ApartmentDetailScreen
import az.mtk.app.ui.screens.apartments.ApartmentsScreen
import az.mtk.app.ui.screens.appeals.AppealDetailScreen
import az.mtk.app.ui.screens.appeals.AppealsScreen
import az.mtk.app.ui.screens.articles.ArticlesAdminScreen
import az.mtk.app.ui.screens.buildings.BuildingDetailScreen
import az.mtk.app.ui.screens.buildings.BuildingsScreen
import az.mtk.app.ui.screens.charges.ChargesScreen
import az.mtk.app.ui.screens.contracts.ContractDetailScreen
import az.mtk.app.ui.screens.contracts.ContractsScreen
import az.mtk.app.ui.screens.customers.CustomerDetailScreen
import az.mtk.app.ui.screens.customers.CustomersScreen
import az.mtk.app.ui.screens.finance.ExpensesScreen
import az.mtk.app.ui.screens.finance.ExtraIncomeScreen
import az.mtk.app.ui.screens.finance.PaymentsScreen
import az.mtk.app.ui.screens.finance.RatesScreen
import az.mtk.app.ui.screens.finance.TransactionsScreen
import az.mtk.app.ui.screens.garages.GarageDetailScreen
import az.mtk.app.ui.screens.garages.GaragesScreen
import az.mtk.app.ui.screens.hr.DictionariesScreen
import az.mtk.app.ui.screens.hr.EmployeeDetailScreen
import az.mtk.app.ui.screens.hr.HrCalendarScreen
import az.mtk.app.ui.screens.hr.HrScreen
import az.mtk.app.ui.screens.hr.OrdersScreen
import az.mtk.app.ui.screens.hr.TimesheetScreen
import az.mtk.app.ui.screens.identity.GroupsScreen
import az.mtk.app.ui.screens.identity.RolesScreen
import az.mtk.app.ui.screens.identity.UsersScreen
import az.mtk.app.ui.screens.inventory.IssuesScreen
import az.mtk.app.ui.screens.inventory.MaterialDetailScreen
import az.mtk.app.ui.screens.inventory.MaterialsScreen
import az.mtk.app.ui.screens.inventory.ReceiptsScreen
import az.mtk.app.ui.screens.inventory.WarehouseScreen
import az.mtk.app.ui.screens.owners.OwnerDetailScreen
import az.mtk.app.ui.screens.owners.OwnersScreen
import az.mtk.app.ui.screens.reports.GeneralReportScreen
import az.mtk.app.ui.screens.reports.PaymentChartScreen
import az.mtk.app.ui.screens.vendors.VendorDetailScreen
import az.mtk.app.ui.screens.vendors.VendorsScreen
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

    // Əməliyyatlar
    const val Contracts = "contracts"
    const val Vendors = "vendors"
    const val Customers = "customers"

    // Maliyyə əməliyyatları
    const val Expenses = "expenses"
    const val FinancePayments = "finance-payments"
    const val ExtraIncome = "extra-income"
    const val Rates = "rates"
    const val Transactions = "transactions"

    // Hesabatlar
    const val PaymentChart = "payment-chart"
    const val GeneralReport = "general-report"

    // Müraciətlər
    const val Appeals = "appeals"

    // İnventar
    const val Warehouse = "warehouse"
    const val Materials = "materials"
    const val Receipts = "receipts"
    const val Issues = "issues"

    // İdentifikasiya
    const val Users = "users"
    const val RolesList = "roles"
    const val Groups = "groups"

    // Xəbərlər / İdarəetmə
    const val Articles = "articles"
    const val CompanyProfile = "company-profile"
    const val Audit = "audit"

    // HR alt səhifələri
    const val Orders = "orders"
    const val Timesheet = "timesheet"
    const val HrCalendar = "hr-calendar"
    const val Dictionaries = "dictionaries"

    const val BuildingDetail = "building/{id}"
    const val ApartmentDetail = "apartment/{id}"
    const val OwnerDetail = "owner/{id}"
    const val GarageDetail = "garage/{id}"
    const val EmployeeDetail = "employee/{id}"
    const val ContractDetail = "contract/{id}"
    const val VendorDetail = "vendor/{id}"
    const val CustomerDetail = "customer/{id}"
    const val AppealDetail = "appeal/{id}"
    const val MaterialDetail = "material/{id}"

    fun building(id: String) = "building/$id"
    fun apartment(id: String) = "apartment/$id"
    fun owner(id: String) = "owner/$id"
    fun garage(id: String) = "garage/$id"
    fun employee(id: String) = "employee/$id"
    fun contract(id: String) = "contract/$id"
    fun vendor(id: String) = "vendor/$id"
    fun customer(id: String) = "customer/$id"
    fun appeal(id: String) = "appeal/$id"
    fun material(id: String) = "material/$id"
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
        id = "inventar",
        label = "İnventar",
        icon = Icons.Outlined.Inventory2,
        rootRoute = Routes.Warehouse,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
        tabs = listOf(
            SubTab("Anbar", Routes.Warehouse),
            SubTab("Materiallar", Routes.Materials),
            SubTab("Daxilolmalar", Routes.Receipts),
            SubTab("Çıxarışlar", Routes.Issues),
        ),
    ),
    NavModule(
        id = "emeliyyatlar",
        label = "Əməliyyatlar",
        icon = Icons.Outlined.Description,
        rootRoute = Routes.Contracts,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT),
        tabs = listOf(
            SubTab("Müqavilələr", Routes.Contracts),
            SubTab("Tədarükçülər", Routes.Vendors),
            SubTab("Müştərilər", Routes.Customers),
            SubTab("Borclar", Routes.Charges),
        ),
    ),
    NavModule(
        id = "maliyye",
        label = "Maliyyə əməliyyatları",
        icon = Icons.Outlined.Payments,
        rootRoute = Routes.Expenses,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT),
        tabs = listOf(
            SubTab("Xərclər", Routes.Expenses),
            SubTab("Ödənişlər", Routes.FinancePayments),
            SubTab("Əlavə gəlirlər", Routes.ExtraIncome),
            SubTab("Tariflər", Routes.Rates),
        ),
    ),
    NavModule(
        id = "muqavileler-owner",
        label = "Müqavilələr",
        icon = Icons.Outlined.Description,
        rootRoute = Routes.Contracts,
        roles = listOf(Roles.OWNER),
        tabs = listOf(
            SubTab("Müqavilələr", Routes.Contracts),
            SubTab("Tədarükçülər", Routes.Vendors),
        ),
    ),
    NavModule(
        id = "tarifler-owner",
        label = "Tariflər",
        icon = Icons.Outlined.Paid,
        rootRoute = Routes.Rates,
        roles = listOf(Roles.OWNER),
    ),
    NavModule(
        id = "tranzaksiyalar",
        label = "Tranzaksiyalar",
        icon = Icons.Outlined.SwapHoriz,
        rootRoute = Routes.Transactions,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT, Roles.OWNER),
    ),
    NavModule(
        id = "hesabatlar",
        label = "Hesabatlar",
        icon = Icons.Outlined.BarChart,
        rootRoute = Routes.PaymentChart,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT, Roles.OWNER),
        tabs = listOf(
            SubTab("Ödəniş qrafiki", Routes.PaymentChart),
            SubTab("Ümumi hesabat", Routes.GeneralReport),
        ),
    ),
    NavModule(
        id = "muracietler",
        label = "Müraciətlər",
        icon = Icons.Outlined.Forum,
        rootRoute = Routes.Appeals,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.OWNER),
    ),
    NavModule(
        id = "hr",
        label = "İnsan Resursları (HR)",
        icon = Icons.Outlined.Badge,
        rootRoute = Routes.Hr,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
        tabs = listOf(
            SubTab("İşçilər", Routes.Hr),
            SubTab("Əmrlər", Routes.Orders),
            SubTab("Tabel", Routes.Timesheet),
            SubTab("Təqvim", Routes.HrCalendar),
            SubTab("Lüğətlər", Routes.Dictionaries),
        ),
    ),
    NavModule(
        id = "identifikasiya",
        label = "İdentifikasiya",
        icon = Icons.Outlined.ManageAccounts,
        rootRoute = Routes.Users,
        roles = listOf(Roles.ADMIN),
        tabs = listOf(
            SubTab("İstifadəçilər", Routes.Users),
            SubTab("Rollar", Routes.RolesList),
            SubTab("Qruplar", Routes.Groups),
        ),
    ),
    NavModule(
        id = "xeberler",
        label = "Xəbərlər",
        icon = Icons.Outlined.Article,
        rootRoute = Routes.Articles,
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    NavModule(
        id = "idareetme",
        label = "İdarəetmə",
        icon = Icons.Outlined.Settings,
        rootRoute = Routes.CompanyProfile,
        roles = listOf(Roles.ADMIN),
        tabs = listOf(
            SubTab("Şirkət məlumatları", Routes.CompanyProfile),
            SubTab("Audit qeydləri", Routes.Audit),
        ),
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
        currentRoute.startsWith("employee/") ||
        currentRoute.startsWith("contract/") ||
        currentRoute.startsWith("vendor/") ||
        currentRoute.startsWith("customer/") ||
        currentRoute.startsWith("appeal/") ||
        currentRoute.startsWith("material/")

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
        currentRoute.startsWith("contract/") -> "Müqavilə detalları"
        currentRoute.startsWith("vendor/") -> "Tədarükçü detalları"
        currentRoute.startsWith("customer/") -> "Müştəri detalları"
        currentRoute.startsWith("appeal/") -> "Müraciət detalları"
        currentRoute.startsWith("material/") -> "Material detalları"
        currentRoute == Routes.Orders -> "Əmrlər"
        currentRoute == Routes.Timesheet -> "Tabel"
        currentRoute == Routes.HrCalendar -> "Təqvim"
        currentRoute == Routes.Dictionaries -> "Lüğətlər"
        currentRoute == Routes.Contracts -> "Əməliyyatlar"
        currentRoute == Routes.Vendors -> "Tədarükçülər"
        currentRoute == Routes.Customers -> "Müştərilər"
        currentRoute == Routes.Expenses -> "Xərclər"
        currentRoute == Routes.FinancePayments -> "Ödənişlər"
        currentRoute == Routes.ExtraIncome -> "Əlavə gəlirlər"
        currentRoute == Routes.Rates -> "Tariflər"
        currentRoute == Routes.Transactions -> "Tranzaksiyalar"
        currentRoute == Routes.PaymentChart -> "Ödəniş qrafiki"
        currentRoute == Routes.GeneralReport -> "Ümumi hesabat"
        currentRoute == Routes.Appeals -> "Müraciətlər"
        currentRoute == Routes.Warehouse -> "Anbar"
        currentRoute == Routes.Materials -> "Materiallar"
        currentRoute == Routes.Receipts -> "Daxilolmalar"
        currentRoute == Routes.Issues -> "Çıxarışlar"
        currentRoute == Routes.Users -> "İstifadəçilər"
        currentRoute == Routes.RolesList -> "Rollar"
        currentRoute == Routes.Groups -> "Qruplar"
        currentRoute == Routes.Articles -> "Xəbərlər"
        currentRoute == Routes.CompanyProfile -> "Şirkət məlumatları"
        currentRoute == Routes.Audit -> "Audit qeydləri"
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

                // ---- Əməliyyatlar ----
                composable(Routes.Contracts) {
                    ContractsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenDetail = { nav.navigate(Routes.contract(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.ContractDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    ContractDetailScreen(
                        contractId = id,
                        onBack = { nav.popBackStack() },
                        onDeleted = { nav.popBackStack() },
                        profile = profile,
                    )
                }

                composable(Routes.Vendors) {
                    VendorsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenDetail = { nav.navigate(Routes.vendor(it)) },
                        showTopBar = false,
                        profile = profile,
                    )
                }

                composable(Routes.VendorDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    VendorDetailScreen(
                        vendorId = id,
                        onBack = { nav.popBackStack() },
                        profile = profile,
                    )
                }

                composable(Routes.Customers) {
                    CustomersScreen(
                        onBack = { nav.popBackStack() },
                        onOpenDetail = { nav.navigate(Routes.customer(it)) },
                        showTopBar = false,
                        profile = profile,
                    )
                }

                composable(Routes.CustomerDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    CustomerDetailScreen(
                        customerId = id,
                        onBack = { nav.popBackStack() },
                        onOpenContract = { nav.navigate(Routes.contract(it)) },
                        profile = profile,
                    )
                }

                // ---- Maliyyə əməliyyatları ----
                composable(Routes.Expenses) {
                    ExpensesScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.FinancePayments) {
                    PaymentsScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.ExtraIncome) {
                    ExtraIncomeScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Rates) {
                    RatesScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Transactions) {
                    TransactionsScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                // ---- Hesabatlar ----
                composable(Routes.PaymentChart) {
                    PaymentChartScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.GeneralReport) {
                    GeneralReportScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                // ---- Müraciətlər ----
                composable(Routes.Appeals) {
                    AppealsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenAppeal = { nav.navigate(Routes.appeal(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.AppealDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    AppealDetailScreen(
                        appealId = id,
                        onBack = { nav.popBackStack() },
                        showTopBar = true,
                    )
                }

                // ---- İnventar ----
                composable(Routes.Warehouse) {
                    WarehouseScreen(
                        onBack = { nav.popBackStack() },
                        onOpenMaterial = { nav.navigate(Routes.material(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.Materials) {
                    MaterialsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenMaterial = { nav.navigate(Routes.material(it)) },
                        showTopBar = false,
                    )
                }

                composable(Routes.MaterialDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    MaterialDetailScreen(
                        nomenclatureId = id,
                        onBack = { nav.popBackStack() },
                    )
                }

                composable(Routes.Receipts) {
                    ReceiptsScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Issues) {
                    IssuesScreen(
                        onBack = { nav.popBackStack() },
                        onOpenMaterial = { nav.navigate(Routes.material(it)) },
                        showTopBar = false,
                    )
                }

                // ---- HR alt səhifələri ----
                composable(Routes.Orders) {
                    OrdersScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Timesheet) {
                    TimesheetScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.HrCalendar) {
                    HrCalendarScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Dictionaries) {
                    DictionariesScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                // ---- İdentifikasiya ----
                composable(Routes.Users) {
                    UsersScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.RolesList) {
                    RolesScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Groups) {
                    GroupsScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                // ---- Xəbərlər / İdarəetmə ----
                composable(Routes.Articles) {
                    ArticlesAdminScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.CompanyProfile) {
                    CompanyProfileScreen(onBack = { nav.popBackStack() }, showTopBar = false)
                }

                composable(Routes.Audit) {
                    AuditScreen(onBack = { nav.popBackStack() }, showTopBar = false)
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
