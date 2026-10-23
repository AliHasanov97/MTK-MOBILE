package az.mtk.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.AppGraph
import az.mtk.app.data.api.LandingRepository
import az.mtk.app.data.auth.AuthState
import az.mtk.app.ui.navigation.LandingBottomNav
import az.mtk.app.ui.navigation.LandingRoutes
import az.mtk.app.ui.navigation.LandingTopBar
import az.mtk.app.ui.screens.dashboard.DashboardScreen
import az.mtk.app.ui.screens.landing.*
import kotlinx.coroutines.launch

@Composable
fun MtkApp() {
    var currentRoute by remember { mutableStateOf(LandingRoutes.HOME) }
    var selectedGuideSlug by remember { mutableStateOf<String?>(null) }
    var selectedArticleId by remember { mutableStateOf<String?>(null) }
    var showLogin by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf("MTK") }

    val repository = remember { LandingRepository() }
    val scope = rememberCoroutineScope()

    val auth = remember { AppGraph.auth }
    val authState by auth.state.collectAsStateWithLifecycle()
    val authenticated = authState as? AuthState.Authenticated

    // System back from the login screen or the dashboard returns to the site.
    BackHandler(enabled = showLogin) {
        if (authenticated == null) auth.cancelLogin()
        showLogin = false
    }

    // Load company name on startup
    LaunchedEffect(Unit) {
        scope.launch {
            companyName = repository.getCompanyName()
        }
    }

    Scaffold(
        topBar = {
            // Hide top bar when showing guide, article detail, or login
            if (selectedGuideSlug == null && selectedArticleId == null && !showLogin) {
                LandingTopBar(
                    companyName = companyName,
                    onLoginClick = { showLogin = true },
                    userName = authenticated?.profile?.displayName
                        ?.trim()
                        ?.split(" ")
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                )
            }
        }
    ) { paddingValues ->
        if (showLogin && authenticated != null) {
            // Full-screen: the dashboard draws its own status-bar background.
            DashboardScreen(
                profile = authenticated.profile,
                onBack = { showLogin = false },
                onLogout = {
                    auth.logout()
                    showLogin = false
                },
                onOpenContact = {
                    showLogin = false
                    currentRoute = LandingRoutes.CONTACT
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Login form while the user is not signed in yet
                if (showLogin) {
                    LoginScreen(
                        onBack = {
                            auth.cancelLogin()
                            showLogin = false
                        },
                        message = (authState as? AuthState.Unauthenticated)?.message,
                        loading = authState is AuthState.Loading,
                    )
                } else if (selectedArticleId != null) {
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
                    // Content
                    when (currentRoute) {
                        LandingRoutes.HOME -> {
                            HomeScreen(
                                repository = repository,
                                onNavigateToLogin = { showLogin = true },
                                onNavigateToNews = { currentRoute = LandingRoutes.NEWS },
                                onArticleClick = { articleId -> selectedArticleId = articleId }
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

                // Floating Navigation Bar (absolute positioned) - hide when login is shown
                if (!showLogin) {
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
            }
        }
    }
}
