package az.mtk.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.AppGraph
import az.mtk.app.data.api.LandingRepository
import az.mtk.app.data.auth.AuthState
import az.mtk.app.data.auth.Roles
import az.mtk.app.ui.navigation.LandingBottomNav
import az.mtk.app.ui.navigation.LandingRoutes
import az.mtk.app.ui.navigation.LandingTopBar
import az.mtk.app.ui.screens.dashboard.DashboardScreen
import az.mtk.app.ui.screens.inventory.InventoryItemScreen
import az.mtk.app.ui.screens.inventory.InventoryScreen
import az.mtk.app.ui.screens.appeals.AppealsScreen
import az.mtk.app.ui.screens.hr.EmployeeDetailScreen
import az.mtk.app.ui.screens.hr.HrScreen
import az.mtk.app.ui.screens.identity.IdentityScreen
import az.mtk.app.ui.screens.news.NewsScreen
import az.mtk.app.ui.screens.landing.*
import az.mtk.app.ui.screens.operations.OperationsScreen
import az.mtk.app.ui.screens.owners.OwnerDetailScreen
import az.mtk.app.ui.screens.properties.PropertiesScreen
import az.mtk.app.ui.screens.properties.PropertyDetailScreen
import az.mtk.app.ui.screens.properties.PropertyType
import az.mtk.app.ui.screens.reports.ReportsScreen
import az.mtk.app.ui.screens.transactions.TransactionsScreen
import az.mtk.app.ui.theme.Civic
import kotlinx.coroutines.launch

@Composable
fun MtkApp() {
    var currentRoute by remember { mutableStateOf(LandingRoutes.HOME) }
    var selectedGuideSlug by remember { mutableStateOf<String?>(null) }
    var selectedArticleId by remember { mutableStateOf<String?>(null) }
    // Landing page is the app's home: there is no separate login screen, the
    // "Daxil ol" actions open Keycloak directly and the dashboard appears once
    // the deep link comes back authenticated.
    var showDashboard by remember { mutableStateOf(false) }
    var awaitingLogin by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }
    var selectedPropertyType by remember { mutableStateOf<PropertyType?>(null) }
    // One-level history, so "Sahibin digər əmlakı" → geri qayıdanda əvvəlki
    // əmlakın səhifəsi açılır (siyahıya yox).
    var previousProperty by remember { mutableStateOf<Pair<String, PropertyType>?>(null) }
    // Sakin detalları: hansı sakinin açıldığı və (əgər əmlakdan keçilibsə)
    // geri qayıdanda bərpa olunacaq əmlak.
    var selectedOwnerId by remember { mutableStateOf<String?>(null) }
    var ownerReturnProperty by remember { mutableStateOf<Pair<String, PropertyType>?>(null) }
    // İnventar modulu: siyahı və material detalı.
    var showInventory by remember { mutableStateOf(false) }
    var selectedNomenclatureId by remember { mutableStateOf<String?>(null) }
    // Əməliyyatlar modulu: müqavilələr, tədarükçülər, müştərilər, borclar,
    // maliyyə əməliyyatları — hamısı bir mobil axında.
    var showOperations by remember { mutableStateOf(false) }
    var showTransactions by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    // Müraciətlər modulu: siyahı/detal ekranı öz içində saxlayır, burada isə
    // yalnız modulun açıq olması və "birbaşa yeni müraciət" bayrağı var.
    var showAppeals by remember { mutableStateOf(false) }
    var appealsCreateOnOpen by remember { mutableStateOf(false) }
    // İdentifikasiya modulu: istifadəçilər, rollar və qruplar.
    var showIdentity by remember { mutableStateOf(false) }
    // Xəbərlər modulu: elanlar, qaralamalar və arxiv.
    var showNews by remember { mutableStateOf(false) }
    // HR modulu: işçilər, ərizələr, əmrlər, tabel, təqvim və lüğətlər.
    var showHr by remember { mutableStateOf(false) }
    var selectedHrEmployeeId by remember { mutableStateOf<String?>(null) }
    var companyName by remember { mutableStateOf("MTK") }

    val repository = remember { LandingRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val auth = remember { AppGraph.auth }
    val authState by auth.state.collectAsStateWithLifecycle()
    val authenticated = authState as? AuthState.Authenticated

    // Opens Keycloak's hosted login page straight from the landing page.
    fun startKeycloakLogin() {
        awaitingLogin = true
        runCatching { context.startActivity(auth.startLogin()) }
            .onFailure {
                awaitingLogin = false
                scope.launch {
                    snackbarHostState.showSnackbar("Brauzer açıla bilmədi. Yenidən cəhd edin.")
                }
            }
    }

    // After the az.mtk.app:/oauth2redirect deep link the session becomes
    // authenticated — that is the moment the dashboard replaces the landing page.
    LaunchedEffect(authenticated) {
        if (awaitingLogin && authenticated != null) {
            awaitingLogin = false
            showDashboard = true
        }
    }

    // Keycloak errors (cancelled flow, state mismatch, expired session) surface
    // as a snackbar instead of a dedicated login screen.
    val authMessage = (authState as? AuthState.Unauthenticated)?.message
    LaunchedEffect(authMessage) {
        if (!authMessage.isNullOrBlank()) {
            awaitingLogin = false
            snackbarHostState.showSnackbar(authMessage)
        }
    }

    // System back from the dashboard returns to the site.
    BackHandler(enabled = showDashboard) {
        showDashboard = false
        selectedGuideSlug = null
        selectedArticleId = null
        currentRoute = LandingRoutes.HOME
    }

    // İnventar daxilində sistem "geri" düyməsi: əvvəlcə material detalı,
    // sonra modulun özü bağlanır.
    BackHandler(enabled = selectedNomenclatureId != null) {
        selectedNomenclatureId = null
    }
    BackHandler(enabled = showInventory && selectedNomenclatureId == null) {
        showInventory = false
    }

    // Əməliyyatlar modulu açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    BackHandler(enabled = showOperations) {
        showOperations = false
    }

    // Tranzaksiyalar jurnalı açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    BackHandler(enabled = showTransactions) {
        showTransactions = false
    }

    // Hesabatlar modulu açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    BackHandler(enabled = showReports) {
        showReports = false
    }

    // Müraciətlər modulu açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    // Modul daxilində detal açıqdırsa, onun öz BackHandler-i bu handler-dən
    // sonra qeydiyyatdan keçdiyi üçün əvvəlcə siyahıya qaytarır.
    BackHandler(enabled = showAppeals) {
        showAppeals = false
        appealsCreateOnOpen = false
    }

    // İdentifikasiya modulu açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    BackHandler(enabled = showIdentity) {
        showIdentity = false
    }

    // Xəbərlər modulu açıqdırsa sistem "geri" düyməsi dashboard-a qaytarır.
    BackHandler(enabled = showNews) {
        showNews = false
    }

    // HR modulu: əvvəlcə işçi kartı, sonra modulun özü bağlanır.
    BackHandler(enabled = selectedHrEmployeeId != null) {
        selectedHrEmployeeId = null
    }
    BackHandler(enabled = showHr && selectedHrEmployeeId == null) {
        showHr = false
    }

    // Load company name on startup
    LaunchedEffect(Unit) {
        scope.launch {
            companyName = repository.getCompanyName()
        }
    }

    Scaffold(
        topBar = {
            // Hide top bar when showing guide, article detail, or the dashboard
            if (selectedGuideSlug == null && selectedArticleId == null && !showDashboard) {
                LandingTopBar(
                    companyName = companyName,
                    onLoginClick = {
                        // Signed-in visitors go straight to the panel, everyone
                        // else to Keycloak — no intermediate login screen.
                        if (authenticated != null) showDashboard = true else startKeycloakLogin()
                    },
                    userName = authenticated?.profile?.displayName
                        ?.trim()
                        ?.split(" ")
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 84.dp)
            )
        }
    ) { paddingValues ->
        if (selectedPropertyId != null && selectedPropertyType != null && authenticated != null) {
            // Property detail screen - use key() to force recreation when ID changes
            key(selectedPropertyId, selectedPropertyType) {
                PropertyDetailScreen(
                    propertyId = selectedPropertyId!!,
                    propertyType = selectedPropertyType!!,
                    onBack = {
                        val previous = previousProperty
                        if (previous != null) {
                            selectedPropertyId = previous.first
                            selectedPropertyType = previous.second
                            previousProperty = null
                        } else {
                            // Sakin detallarından açılıbsa, geri həmin sakinə qayıdır.
                            selectedPropertyId = null
                            selectedPropertyType = null
                        }
                    },
                    onOpenProperty = { propertyId, propertyType ->
                        val currentId = selectedPropertyId
                        val currentType = selectedPropertyType
                        if (currentId != null && currentType != null) {
                            previousProperty = currentId to currentType
                        }
                        selectedPropertyId = propertyId
                        selectedPropertyType = propertyType
                    },
                    onOpenOwner = { ownerId ->
                        // Sahib kartından sakin detallarına keçid; geri qayıdanda
                        // həmin əmlak səhifəsi bərpa olunur.
                        val currentId = selectedPropertyId
                        val currentType = selectedPropertyType
                        ownerReturnProperty = if (currentId != null && currentType != null) {
                            currentId to currentType
                        } else {
                            null
                        }
                        previousProperty = null
                        selectedPropertyId = null
                        selectedPropertyType = null
                        selectedOwnerId = ownerId
                    },
                )
            }
        } else if (selectedOwnerId != null && authenticated != null) {
            // Sakin detalları (Əmlak Fondu → Sakinlər)
            key(selectedOwnerId) {
                OwnerDetailScreen(
                    ownerId = selectedOwnerId!!,
                    onBack = {
                        val returnProperty = ownerReturnProperty
                        if (returnProperty != null) {
                            // Əmlakdan gəlmişdiksə, geri həmin əmlaka qayıdırıq.
                            ownerReturnProperty = null
                            selectedOwnerId = null
                            selectedPropertyId = returnProperty.first
                            selectedPropertyType = returnProperty.second
                        } else {
                            selectedOwnerId = null
                        }
                    },
                    onOpenProperty = { propertyId, propertyType ->
                        // Sakin səhifəsi açıq qalır — geri qayıdanda ora qayıdırıq.
                        previousProperty = null
                        selectedPropertyId = propertyId
                        selectedPropertyType = propertyType
                    },
                )
            }
        } else if (selectedHrEmployeeId != null && authenticated != null) {
            // İşçi kartı (HR → İşçilər)
            key(selectedHrEmployeeId) {
                EmployeeDetailScreen(
                    employeeId = selectedHrEmployeeId!!,
                    onBack = { selectedHrEmployeeId = null },
                )
            }
        } else if (showHr && authenticated != null) {
            // HR modulu: İşçilər · Ərizələr · Əmrlər · Tabel · Təqvim · Lüğətlər
            HrScreen(
                onBack = { showHr = false },
                onOpenEmployee = { selectedHrEmployeeId = it },
            )
        } else if (selectedNomenclatureId != null && authenticated != null) {
            // Material detalı (İnventar → Materiallar)
            key(selectedNomenclatureId) {
                InventoryItemScreen(
                    nomenclatureId = selectedNomenclatureId!!,
                    onBack = { selectedNomenclatureId = null },
                )
            }
        } else if (showInventory && authenticated != null) {
            // İnventar modulu: Anbar · Materiallar · Daxilolmalar · Çıxarışlar
            InventoryScreen(
                onBack = { showInventory = false },
                onOpenMaterial = { selectedNomenclatureId = it },
                canManage = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER
                },
            )
        } else if (showOperations && authenticated != null) {
            // Əməliyyatlar modulu (müqavilələr, tədarükçülər, müştərilər,
            // borclar, maliyyə əməliyyatları).
            OperationsScreen(
                onBack = { showOperations = false },
                canManage = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER
                },
                canPay = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER || it == Roles.ACCOUNTANT
                },
                isAdmin = authenticated.profile.roles.contains(Roles.ADMIN),
            )
        } else if (showTransactions && authenticated != null) {
            // Tranzaksiyalar: avtomatik kassa jurnalı (gəlir/xərc qeydləri).
            TransactionsScreen(
                onBack = { showTransactions = false },
                canDownload = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER || it == Roles.ACCOUNTANT
                },
            )
        } else if (showReports && authenticated != null) {
            // Hesabatlar: illik ödəniş qrafiki + aylıq ümumi hesabat.
            ReportsScreen(
                onBack = { showReports = false },
                canDownload = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER || it == Roles.ACCOUNTANT
                },
            )
        } else if (showAppeals && authenticated != null) {
            // Müraciətlər: sakin öz müraciətlərini, işçi bütün müraciətləri görür.
            AppealsScreen(
                onBack = {
                    showAppeals = false
                    appealsCreateOnOpen = false
                },
                isStaff = authenticated.profile.roles.any {
                    it == Roles.ADMIN || it == Roles.BUILDING_MANAGER
                },
                startWithCreate = appealsCreateOnOpen,
                onStartCreateConsumed = { appealsCreateOnOpen = false },
            )
        } else if (showIdentity && authenticated != null) {
            // İdentifikasiya: istifadəçilər, rollar və qruplar bir mobil axında.
            IdentityScreen(
                onBack = { showIdentity = false },
            )
        } else if (showNews && authenticated != null) {
            // Xəbərlər: yayım, qaralama və arxivdə olan elanların idarəsi.
            NewsScreen(
                onBack = { showNews = false },
            )
        } else if (showProperties && authenticated != null) {
            // Properties (Əmlak Fondu) screen
            PropertiesScreen(
                onBack = { showProperties = false },
                onPropertyClick = { propertyId, propertyType ->
                    println("🔗 MtkApp: Opening property detail - ID=$propertyId, type=$propertyType")
                    previousProperty = null
                    selectedPropertyId = propertyId
                    selectedPropertyType = propertyType
                },
                onResidentClick = { residentId ->
                    println("👤 MtkApp: Opening resident detail - ID=$residentId")
                    ownerReturnProperty = null
                    selectedOwnerId = residentId
                },
            )
        } else if (showDashboard && authenticated != null) {
            // Full-screen: the dashboard draws its own status-bar background.
            DashboardScreen(
                profile = authenticated.profile,
                onBack = {
                    showDashboard = false
                    selectedGuideSlug = null
                    selectedArticleId = null
                    currentRoute = LandingRoutes.HOME
                },
                onLogout = {
                    scope.launch {
                        auth.logout()
                    }
                    showDashboard = false
                    selectedGuideSlug = null
                    selectedArticleId = null
                    currentRoute = LandingRoutes.HOME
                },
                onOpenContact = {
                    showDashboard = false
                    selectedGuideSlug = null
                    selectedArticleId = null
                    currentRoute = LandingRoutes.CONTACT
                },
                onOpenProperties = {
                    showProperties = true
                },
                onOpenInventory = {
                    showInventory = true
                },
                onOpenOperations = {
                    showOperations = true
                },
                onOpenTransactions = {
                    showTransactions = true
                },
                onOpenReports = {
                    showReports = true
                },
                onOpenAppeals = { create ->
                    appealsCreateOnOpen = create
                    showAppeals = true
                },
                onOpenIdentity = {
                    showIdentity = true
                },
                onOpenNews = {
                    showNews = true
                },
                onOpenHr = {
                    showHr = true
                },
            )
        } else {
            // Detail pages draw their own scaffold (and their own insets), so the
            // landing scaffold padding must not be applied twice.
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedArticleId != null) {
                    ArticleDetailScreen(
                        articleId = selectedArticleId!!,
                        repository = repository,
                        onBack = { selectedArticleId = null }
                    )
                } else if (selectedGuideSlug != null) {
                    // Show guide detail if selected
                    val guide = az.mtk.app.data.getGuide(selectedGuideSlug!!)
                    if (guide != null) {
                        GuideDetailScreen(
                            guide = guide,
                            onBack = { selectedGuideSlug = null }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Content
                        when (currentRoute) {
                            LandingRoutes.HOME -> {
                                HomeScreen(
                                    repository = repository,
                                    onLoginClick = { startKeycloakLogin() },
                                    onNavigateToNews = { currentRoute = LandingRoutes.NEWS },
                                    onArticleClick = { articleId -> selectedArticleId = articleId },
                                    onNavigateToContact = { currentRoute = LandingRoutes.CONTACT }
                                )
                            }
                            LandingRoutes.INSTRUCTIONS -> {
                                InstructionsScreen(
                                    onGuideClick = { slug -> selectedGuideSlug = slug }
                                )
                            }
                            LandingRoutes.NEWS -> {
                                NewsScreen(
                                    repository = repository,
                                    onArticleClick = { articleId -> selectedArticleId = articleId }
                                )
                            }
                            LandingRoutes.ABOUT -> {
                                AboutScreen()
                            }
                            LandingRoutes.CONTACT -> {
                                ContactScreen(repository = repository)
                            }
                        }
                    }

                    // Floating Navigation Bar (absolute positioned)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        LandingBottomNav(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                currentRoute = route
                                selectedGuideSlug = null // Clear guide when switching tabs
                                selectedArticleId = null // Clear article when switching tabs
                            }
                        )
                    }
                }

                // Brief overlay while the Keycloak code is exchanged after the
                // browser redirects back to the app.
                if (awaitingLogin && authState is AuthState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Civic.Background.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Civic.Terracotta, strokeWidth = 3.dp)
                            Text(
                                text = "Giriş yoxlanılır…",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Civic.Muted
                            )
                        }
                    }
                }
            }
        }
    }
}
