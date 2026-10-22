package az.mtk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.BrandMark
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.StatTile
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.navigation.Routes
import az.mtk.app.ui.theme.MtkColors
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val buildings: Int? = null,
    val apartments: Int? = null,
    val owners: Int? = null,
    val garages: Int? = null,
    val employees: Int? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val repo = AppGraph.repository

            val buildings = async { runCatching { repo.buildings().size } }
            val apartments = async { runCatching { repo.apartments(page = 0, pageSize = 1).totalCount } }
            val owners = async { runCatching { repo.owners(page = 0, pageSize = 1).totalCount } }
            val garages = async { runCatching { repo.garages(page = 0, pageSize = 1).totalCount } }
            val employees = async { runCatching { repo.employees(page = 0, pageSize = 1).totalCount } }

            val next = DashboardState(
                buildings = buildings.await().getOrNull(),
                apartments = apartments.await().getOrNull(),
                owners = owners.await().getOrNull(),
                garages = garages.await().getOrNull(),
                employees = employees.await().getOrNull(),
                loading = false,
                error = null,
            )
            _state.value = next.copy(error = if (next.allNull()) "Məlumatları yükləmək mümkün olmadı." else null)
        }
    }

    private fun DashboardState.allNull() = listOf(buildings, apartments, owners, garages, employees).all { it == null }
}

private data class DashboardModule(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val roles: List<String>? = null,
)

private val modules = listOf(
    DashboardModule("Binalar", "Bina fondunun strukturu və məlumatları", Icons.Outlined.Apartment, Routes.Buildings, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
    DashboardModule("Mənzillər", "Mənzil qeydiyyatı, otaqlar və sahələr", Icons.Outlined.MeetingRoom, Routes.Apartments, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
    DashboardModule("Mülkiyyətçilər", "Sakinlər və əlaqə məlumatları", Icons.Outlined.People, Routes.Owners, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
    DashboardModule("Qarajlar", "Dayanacaq və qaraj yerləri", Icons.Outlined.DirectionsCar, Routes.Garages, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
    DashboardModule("Borclar və Hesablar", "Aylıq ödənişlər, borclar və tariflər", Icons.AutoMirrored.Outlined.ReceiptLong, Routes.Charges, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT)),
    DashboardModule("İnsan Resursları (HR)", "İşçi heyəti, vəzifələr və ərizələr", Icons.Outlined.Badge, Routes.Hr, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
)

@Composable
fun DashboardScreen(
    profile: UserProfile,
    onNavigate: (String) -> Unit,
    onOpenProfile: () -> Unit,
    showTopBar: Boolean = false,
    viewModel: DashboardViewModel = mtkViewModel { DashboardViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visibleModules = modules.filter { module ->
        module.roles == null || profile.hasAnyRole(*module.roles.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandMark(size = 38.dp, letterSize = 20)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
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
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MtkColors.Positive),
                            )
                        }
                        Text(
                            "İdarəetmə Paneli",
                            style = MaterialTheme.typography.labelSmall,
                            color = MtkColors.MutedSoft,
                        )
                    }

                    IconButton(onClick = viewModel::load) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Yenilə",
                            tint = MtkColors.Muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))
                    AvatarCircle(
                        initials = profile.initials,
                        size = 38.dp,
                        background = MtkColors.Green,
                        onClick = onOpenProfile,
                    )
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Hero Welcome Card (Dark Forest Emerald Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MtkColors.GreenDark, MtkColors.Green),
                        ),
                    )
                    .clickable(onClick = onOpenProfile)
                    .padding(20.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "XOŞ GƏLDİNİZ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MtkColors.Lime,
                            letterSpacing = 1.2.sp,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "CANLI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sistemdəki bütün əsas göstəricilər və idarəetmə vasitələri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )

                    if (profile.roles.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            profile.roles.take(3).forEach { role ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        Roles.label(role),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MtkColors.Lime,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stat Tiles (2x2 Grid with vector icons)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Binalar",
                    value = state.buildings?.toString() ?: "—",
                    caption = if (state.loading) "yüklənir…" else "Ümumi bina",
                    icon = Icons.Outlined.Apartment,
                    onClick = { onNavigate(Routes.Buildings) },
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Mənzillər",
                    value = state.apartments?.toString() ?: "—",
                    caption = if (state.loading) "yüklənir…" else "Qeydiyyatda",
                    icon = Icons.Outlined.MeetingRoom,
                    onClick = { onNavigate(Routes.Apartments) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Sahiblər",
                    value = state.owners?.toString() ?: "—",
                    caption = if (state.loading) "yüklənir…" else "Sakinlər",
                    icon = Icons.Outlined.People,
                    onClick = { onNavigate(Routes.Owners) },
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Qarajlar",
                    value = state.garages?.toString() ?: "—",
                    caption = if (state.loading) "yüklənir…" else "Dayanacaqlar",
                    icon = Icons.Outlined.DirectionsCar,
                    onClick = { onNavigate(Routes.Garages) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                MtkCard(
                    padding = 14.dp,
                    containerColor = MtkColors.NegativeBg.copy(alpha = 0.5f),
                    borderColor = MtkColors.Negative.copy(alpha = 0.2f),
                ) {
                    Text(state.error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Modules Section
            SectionHeading(
                title = "Əsas Bölmələr",
                subtitle = "İcazələrinizə uyğun aktiv modullar",
            )
            Spacer(Modifier.height(12.dp))

            MtkCard(
                padding = 6.dp,
                shape = RoundedCornerShape(18.dp),
            ) {
                visibleModules.forEachIndexed { index, module ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(module.route) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MtkColors.IconBoxBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                module.icon,
                                contentDescription = null,
                                tint = MtkColors.Green,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                module.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                module.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                                maxLines = 1,
                            )
                        }

                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MtkColors.MutedUltraSoft,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (index != visibleModules.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
