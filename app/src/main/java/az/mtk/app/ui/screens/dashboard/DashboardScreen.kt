package az.mtk.app.ui.screens.dashboard

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppConfig
import az.mtk.app.AppGraph
import az.mtk.app.data.PanelModule
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.ChargeStatus
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.data.api.OprOwner
import az.mtk.app.data.api.OprOwnerBalanceResponse
import az.mtk.app.data.api.OprPaymentResponse
import az.mtk.app.data.api.OwnerProfileApi
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.data.modulesForRoles
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * "Civic Hearth" design tokens (stitch_mtk_mobile_app_design/DESIGN.md):
 * warm off-white canvas, hairline slate borders, terracotta signals and the
 * deep forest hero panel that opens the resident dashboard.
 */
private object Civic {
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
    val ForestTop = Color(0xFF1E3629)
    val Olive = Color(0xFF6F8559)
    val Lime = Color(0xFFD2EBB7)
    val Error = Color(0xFFBA1A1A)
    val ErrorText = Color(0xFF93000A)
}

class DashboardViewModel : ViewModel() {
    private val identityApi = AppGraph.service(IdentityApi::class.java)
    private val ownerApi = AppGraph.service(OwnerProfileApi::class.java)

    data class UiState(
        val loading: Boolean = true,
        val owner: OprOwner? = null,
        val balance: OprOwnerBalanceResponse? = null,
        val charges: List<ChargeDto> = emptyList(),
        val payments: List<OprPaymentResponse> = emptyList(),
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState()
            val owner = runCatching {
                val me = identityApi.currentUser()
                ownerApi.ownerByUserId(me.id)
            }.getOrElse { failure ->
                // Staff accounts (komandant, mühasib, …) simply have no owner record:
                // that is not an error, only the resident sections are hidden.
                _state.value = if (failure is HttpException && failure.code() == 404) {
                    UiState(loading = false, owner = null)
                } else {
                    UiState(loading = false, error = failure.toUserMessage())
                }
                return@launch
            }

            _state.value = _state.value.copy(loading = false, owner = owner)
            _state.value = _state.value.copy(
                balance = runCatching { ownerApi.ownerBalance(owner.id).data }.getOrNull(),
            )
            _state.value = _state.value.copy(
                charges = runCatching { ownerApi.chargesByOwner(owner.id).data.orEmpty() }
                    .getOrNull()
                    .orEmpty(),
            )
            _state.value = _state.value.copy(
                payments = runCatching { ownerApi.paymentsByOwner(owner.id).data.orEmpty() }
                    .getOrNull()
                    .orEmpty(),
            )
        }
    }
}

private enum class DockItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val roles: List<String> = emptyList(),
) {
    HOME("home", "Ana Səhifə", Icons.Default.Home),

    // Owner (sakin) əməliyyatları
    MAKE_PAYMENT("make_payment", "Ödəniş et", Icons.Default.Payments, listOf(Roles.OWNER)),
    CREATE_APPEAL("create_appeal", "Müraciət", Icons.Default.SupportAgent, listOf(Roles.OWNER)),
    VIEW_PROFILE("view_profile", "Profil", Icons.Default.Person, listOf(Roles.OWNER)),

    // Accountant (mühasib) əməliyyatları
    RECEIVE_PAYMENT("receive_payment", "Ödəniş qəbul", Icons.Default.Paid, listOf(Roles.ACCOUNTANT)),
    RECORD_EXPENSE("record_expense", "Xərc qeyd et", Icons.AutoMirrored.Filled.ReceiptLong, listOf(Roles.ACCOUNTANT)),
    VIEW_REPORTS("view_reports", "Hesabat", Icons.Default.Analytics, listOf(Roles.ACCOUNTANT)),

    // Building Manager (komandant) əməliyyatları
    MANAGE_APPEALS("manage_appeals", "Müraciətlər", Icons.Default.SupportAgent, listOf(Roles.BUILDING_MANAGER)),
    MANAGE_STAFF("manage_staff", "İşçilər", Icons.Default.Badge, listOf(Roles.BUILDING_MANAGER)),
    MANAGE_BUILDINGS("manage_buildings", "Binalar", Icons.Default.Apartment, listOf(Roles.BUILDING_MANAGER)),

    // Admin əməliyyatları
    MANAGE_USERS("manage_users", "İstifadəçilər", Icons.Default.ManageAccounts, listOf(Roles.ADMIN)),
    VIEW_AUDIT("view_audit", "Audit", Icons.Default.VerifiedUser, listOf(Roles.ADMIN)),
    SYSTEM_SETTINGS("system_settings", "Sistem", Icons.Default.Settings, listOf(Roles.ADMIN)),

    CONTACT("contact", "Əlaqə", Icons.Default.Phone),
}

/** Signed-in overview in the Civic Hearth language, filled with our panel data. */
@Composable
fun DashboardScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenProperties: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenOperations: () -> Unit = {},
    onOpenTransactions: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenAppeals: (Boolean) -> Unit = {},
    onOpenIdentity: () -> Unit = {},
    onOpenNews: () -> Unit = {},
    viewModel: DashboardViewModel = mtkViewModel { DashboardViewModel() },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    var containerTop by remember { mutableStateOf(0f) }
    val modulesAnchor = remember { mutableStateOf(0f) }
    val paymentsAnchor = remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { viewModel.load() }

    // The forest hero sits under the clock/battery, so the system icons go light.
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    // Content offsets are stable while scrolling: the scroll value grows by the
    // same amount the node travels up the window.
    val anchor: (MutableState<Float>) -> Modifier = { target ->
        Modifier.onGloballyPositioned { coordinates ->
            target.value = scrollState.value + coordinates.positionInWindow().y - containerTop
        }
    }

    // Filter dock items by user's roles: always HOME + 3 role-specific actions + CONTACT
    val dockItems = remember(profile.roles) {
        buildList {
            add(DockItem.HOME)

            // Add role-specific actions (maximum 3)
            val roleActions = DockItem.entries.filter { item ->
                item != DockItem.HOME && item != DockItem.CONTACT &&
                item.roles.isNotEmpty() && item.roles.any { it in profile.roles }
            }.take(3)

            addAll(roleActions)
            add(DockItem.CONTACT)
        }
    }

    val activeDock = dockItems.firstOrNull() ?: DockItem.HOME

    Scaffold(
        containerColor = Civic.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sabit başlıq: səhifə aşağı sürüşdürülsə də yuxarıda qalır.
                DashboardTopBar(
                    owner = uiState.owner,
                    onBack = onBack,
                    onLogout = onLogout,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { containerTop = it.positionInWindow().y }
                        .verticalScroll(scrollState),
                ) {
                    HeroPanel(
                        profile = profile,
                        owner = uiState.owner,
                        balance = uiState.balance,
                        onOpenAccount = { openAccountConsole(context) },
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Spacer(Modifier.height(6.dp))

                        SectionHead(
                            title = "İdarəetmə Modulları",
                            subtitle = "Rolunuza uyğun aktiv sistem bölmələri",
                            onRefresh = viewModel::load,
                            modifier = anchor(modulesAnchor),
                        )
                        ModuleGrid(
                            modules = modulesForRoles(profile.roles),
                            onModuleClick = { module ->
                                when (module.key) {
                                    "buildings" -> onOpenProperties()
                                    "inventory" -> onOpenInventory()
                                    "operations" -> onOpenOperations()
                                    "transactions" -> onOpenTransactions()
                                    "reports" -> onOpenReports()
                                    "appeals" -> onOpenAppeals(false)
                                    "identity" -> onOpenIdentity()
                                    "news" -> onOpenNews()
                                    else -> scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "«${module.label}» modulu mobil tətbiqdə hələ hazırlanmayıb."
                                        )
                                    }
                                }
                            },
                        )

                        Spacer(Modifier.height(10.dp))

                        when {
                            uiState.loading -> LoadingBlock()
                            uiState.error != null -> ErrorCard(uiState.error!!, onRetry = viewModel::load)
                            else -> {
                                // Resident sections only exist for accounts that own property;
                                // staff see just the module grid and the account actions.
                                val owner = uiState.owner
                                val hasProperties = owner != null &&
                                    (owner.apartments.orEmpty().isNotEmpty() ||
                                        owner.garages.orEmpty().isNotEmpty())

                                if (hasProperties) {
                                    SectionHead(
                                        title = "Mənim əmlakım",
                                        subtitle = "Mənzil və qarajlarınız",
                                    )
                                    PropertyGrid(owner)
                                    Spacer(Modifier.height(10.dp))
                                }

                                if (uiState.charges.isNotEmpty()) {
                                    SectionHead(
                                        title = "Son hesablamalar",
                                        subtitle = "Ən son ${minOf(uiState.charges.size, 5)} qeyd",
                                        modifier = anchor(paymentsAnchor),
                                    )
                                    ChargesCard(uiState.charges)
                                    Spacer(Modifier.height(10.dp))
                                }

                                if (uiState.payments.isNotEmpty()) {
                                    SectionHead(
                                        title = "Son ödənişlər",
                                        subtitle = "Ən son ${minOf(uiState.payments.size, 5)} qeyd",
                                    )
                                    PaymentsCard(uiState.payments)
                                }
                            }
                        }

                        Spacer(Modifier.height(96.dp).navigationBarsPadding())
                    }
                }
            }

            DashboardDock(
                items = dockItems,
                active = activeDock,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                onSelect = { item ->
                    when (item.key) {
                        "home" -> scope.launch { scrollState.animateScrollTo(0) }
                        "contact" -> onOpenContact()
                        "manage_buildings" -> onOpenProperties()
                        "view_reports" -> onOpenReports()
                        "manage_appeals" -> onOpenAppeals(false)
                        "create_appeal" -> onOpenAppeals(true)
                        else -> {
                            // Show "not implemented yet" message for other actions
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "«${item.label}» əməliyyatı mobil tətbiqdə hələ hazırlanmayıb."
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

/* ---------- Fixed header ---------- */

@Composable
private fun DashboardTopBar(
    owner: OprOwner?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Civic.ForestDeep)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Civic.TerracottaVivid,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "SAKİN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = Color.White,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Civic.Lime),
            )
            Text(
                text = if (owner == null || owner.isActive) "Aktiv" else "Deaktiv",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        HeroIconButton(Icons.AutoMirrored.Filled.ExitToApp, "Çıxış", onLogout)
    }
}

/* ---------- Hero ---------- */

@Composable
private fun HeroPanel(
    profile: UserProfile,
    owner: OprOwner?,
    balance: OprOwnerBalanceResponse?,
    onOpenAccount: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Civic.ForestDeep, Civic.Forest, Civic.ForestTop)
                )
            )
            .padding(bottom = 22.dp),
    ) {
        Column(Modifier.padding(top = 8.dp)) {
            // Avatar, name, roles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = Format.initials(owner?.fullName ?: profile.displayName),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 3.dp, y = 3.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Civic.Lime)
                            .border(2.dp, Civic.Forest, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Civic.Forest,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = owner?.fullName ?: profile.displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.roles.take(2).forEachIndexed { index, role ->
                            Surface(
                                shape = CircleShape,
                                color = if (index == 0) {
                                    Civic.TerracottaVivid
                                } else {
                                    Color.White.copy(alpha = 0.2f)
                                },
                            ) {
                                Text(
                                    text = Roles.label(role)
                                        .uppercase(java.util.Locale.forLanguageTag("az")),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // Residency block only makes sense for accounts that own property;
            // staff (komandant, mühasib…) see just their modules below the header.
            if (owner != null) {
                val address = buildList {
                    owner.apartments.orEmpty().forEach { apartment ->
                        add("${apartment.building?.name ?: "Bina"} · mənzil ${apartment.apartmentNumber ?: "—"}")
                    }
                    owner.garages.orEmpty().forEach { garage ->
                        add("Qaraj ${garage.garageNumber ?: "—"}")
                    }
                }.joinToString("  ·  ")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Civic.Lime,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = address.ifBlank {
                            owner.email ?: profile.email ?: "Ünvan qeydə alınmayıb"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                ) {
                    HeroStat(
                        value = owner.apartments.orEmpty().size.toString(),
                        label = "Mənzil",
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.15f)),
                    )
                    HeroStat(
                        value = owner.garages.orEmpty().size.toString(),
                        label = "Qaraj",
                        highlight = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.15f)),
                    )
                    HeroStat(
                        value = signedBalance(balance?.currentBalance),
                        label = "Cari balans",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Account console (password, phone, sessions) lives in Keycloak.
            Button(
                onClick = onOpenAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Civic.TerracottaVivid,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hesab mərkəzi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) Civic.Lime else Color.White.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(17.dp),
        )
    }
}

/* ---------- Bottom quick actions ---------- */

@Composable
private fun DashboardDock(
    items: List<DockItem>,
    active: DockItem,
    onSelect: (DockItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Civic.Surface)
            .border(1.dp, Civic.Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item == active
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Civic.Container else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) Civic.Terracotta else Civic.MutedSoft,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = item.label,
                    fontSize = 9.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Civic.Terracotta else Civic.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ---------- Sections ---------- */

@Composable
private fun SectionHead(
    title: String,
    subtitle: String,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = Civic.Ink,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Civic.Muted,
            )
        }
        if (onRefresh != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Civic.Container)
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yenilə",
                    tint = Civic.Muted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

private data class PropertyItem(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val description: String,
)

@Composable
private fun PropertyGrid(owner: OprOwner?) {
    val items = buildList {
        owner?.apartments.orEmpty().forEach { apartment ->
            add(
                PropertyItem(
                    icon = Icons.Default.Apartment,
                    accent = Civic.Terracotta,
                    title = apartment.building?.name ?: "Bina",
                    description = listOfNotNull(
                        "Mənzil ${apartment.apartmentNumber ?: "—"}",
                        apartment.floor?.let { "$it mərtəbə" },
                        apartment.roomCount?.let { "$it otaq" },
                        apartment.areaSquareMeters?.let { Format.area(it) },
                    ).joinToString(" · "),
                ),
            )
        }
        owner?.garages.orEmpty().forEach { garage ->
            add(
                PropertyItem(
                    icon = Icons.Default.DirectionsCar,
                    accent = Civic.Olive,
                    title = "Qaraj ${garage.garageNumber ?: "—"}",
                    description = listOfNotNull(
                        Format.garageType(garage.type),
                        garage.description,
                    ).joinToString(" · ").ifBlank { "Qaraj" },
                ),
            )
        }
    }

    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    PropertyTile(item = item, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PropertyTile(item: PropertyItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.description,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Civic.MutedSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModuleGrid(
    modules: List<PanelModule>,
    onModuleClick: (PanelModule) -> Unit,
) {
    if (modules.isEmpty()) {
        EmptyCard("Rolunuza uyğun modul təyin edilməyib.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modules.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { module ->
                    ModuleTile(
                        module = module,
                        onClick = { onModuleClick(module) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModuleTile(
    module: PanelModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = moduleAccent(module.key)

    Card(
        modifier = modifier.defaultMinSize(minHeight = 118.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = moduleIcon(module.key),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Civic.MutedSoft.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = module.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = module.hint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Civic.MutedSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun moduleAccent(key: String): Color = when (key) {
    "operations", "transactions", "reports", "rates" -> Civic.Terracotta
    "buildings", "inventory", "appeals", "identity", "news", "profile" -> Civic.Olive
    else -> Civic.Muted
}

private fun moduleIcon(key: String): ImageVector = when (key) {
    "buildings" -> Icons.Default.Apartment
    "inventory" -> Icons.Default.Inventory2
    "operations" -> Icons.Default.Storefront
    "contracts" -> Icons.Default.Description
    "rates" -> Icons.Default.Paid
    "transactions" -> Icons.Default.SwapHoriz
    "reports" -> Icons.Default.Analytics
    "appeals" -> Icons.Default.SupportAgent
    "hr" -> Icons.Default.Badge
    "identity" -> Icons.Default.ManageAccounts
    "news" -> Icons.Default.Campaign
    "profile" -> Icons.Default.Person
    else -> Icons.Default.Settings
}

@Composable
private fun ChargesCard(charges: List<ChargeDto>) {
    if (charges.isEmpty()) return
    ListCard {
        charges.take(5).forEachIndexed { index, charge ->
            if (index > 0) RowRule()
            ListRow(
                title = "${Format.period(charge.period)} · ${Format.money(charge.amount)}",
                subtitle = charge.description ?: charge.propertyLabel,
                trailing = {
                    StatusChip(
                        text = Format.chargeStatus(charge.status),
                        color = chargeColor(charge.status),
                    )
                },
            )
        }
    }
}

@Composable
private fun PaymentsCard(payments: List<OprPaymentResponse>) {
    if (payments.isEmpty()) return
    ListCard {
        payments.take(5).forEachIndexed { index, payment ->
            if (index > 0) RowRule()
            ListRow(
                title = Format.money(payment.amount),
                subtitle = listOfNotNull(
                    Format.date(payment.paymentDate),
                    payment.propertyLabel,
                    payment.notes?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { null },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = Civic.Olive,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ListCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), content = content)
    }
}

@Composable
private fun ListRow(
    title: String,
    subtitle: String?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Civic.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Civic.MutedSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun RowRule() {
    HorizontalDivider(thickness = 1.dp, color = Civic.Border)
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Civic.MutedSoft,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Civic.Terracotta, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text(text = "Yüklənir…", fontSize = 12.sp, color = Civic.Muted)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Civic.ErrorText,
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Civic.Terracotta,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Yenidən cəhd et", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ---------- Helpers ---------- */

private fun signedBalance(value: Double?): String {
    if (value == null) return "—"
    return if (value > 0.005) "+${Format.money(value)}" else Format.money(value)
}

private fun chargeColor(status: Int?): Color = when (status) {
    ChargeStatus.UNPAID -> Civic.Terracotta
    ChargeStatus.PARTIALLY_PAID -> Civic.Terracotta
    ChargeStatus.PAID -> Civic.Olive
    else -> Civic.MutedSoft
}

private fun openAccountConsole(context: android.content.Context) {
    val intent = runCatching {
        CustomTabsIntent.Builder().setShowTitle(true).build().intent
    }.getOrElse {
        android.content.Intent(android.content.Intent.ACTION_VIEW)
    }
    intent.data = Uri.parse(AppConfig.AccountEndpoint)
    runCatching { context.startActivity(intent) }
}
