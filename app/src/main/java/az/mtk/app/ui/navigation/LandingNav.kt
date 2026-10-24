package az.mtk.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import az.mtk.app.ui.theme.Civic

/**
 * Keeps the system status-bar icons light while a dark (forest) header is on
 * screen, and restores the previous style when it leaves the composition.
 */
@Composable
fun LandingDarkSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = (view.context as? android.app.Activity)
            ?.window
            ?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}

/**
 * Top app bar for landing pages — the same forest panel and terracotta action
 * the resident dashboard opens with, so the public site and the panel feel like
 * one product. The bar paints under the status bar, so the system icons go light.
 */
@Composable
fun LandingTopBar(
    companyName: String,
    onLoginClick: () -> Unit,
    userName: String? = null,
    modifier: Modifier = Modifier
) {
    LandingDarkSystemBars()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Civic.ForestDeep)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apartment,
                contentDescription = "MTK",
                tint = Civic.Lime,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = companyName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "İdarəetmə sistemi",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1
            )
        }

        if (userName != null) {
            // Signed-in visitor: the account replaces the login action.
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onLoginClick)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Civic.Lime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = userName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Civic.TerracottaVivid,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Daxil ol", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Bottom navigation for landing pages — a floating paper dock with a hairline
 * border, mirroring the quick-action dock inside the app.
 */
@Composable
fun LandingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Civic.Surface)
                .border(1.dp, Civic.Border, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LandingNavItem(
                icon = Icons.Default.Home,
                label = "Əsas",
                selected = currentRoute == LandingRoutes.HOME,
                onClick = { onNavigate(LandingRoutes.HOME) },
                modifier = Modifier.weight(1f)
            )
            LandingNavItem(
                icon = Icons.Default.MenuBook,
                label = "Təlimat",
                selected = currentRoute == LandingRoutes.INSTRUCTIONS,
                onClick = { onNavigate(LandingRoutes.INSTRUCTIONS) },
                modifier = Modifier.weight(1f)
            )
            LandingNavItem(
                icon = Icons.Default.Newspaper,
                label = "Xəbərlər",
                selected = currentRoute == LandingRoutes.NEWS,
                onClick = { onNavigate(LandingRoutes.NEWS) },
                modifier = Modifier.weight(1f)
            )
            LandingNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Haqqımızda",
                selected = currentRoute == LandingRoutes.ABOUT,
                onClick = { onNavigate(LandingRoutes.ABOUT) },
                modifier = Modifier.weight(1f)
            )
            LandingNavItem(
                icon = Icons.Default.Phone,
                label = "Əlaqə",
                selected = currentRoute == LandingRoutes.CONTACT,
                onClick = { onNavigate(LandingRoutes.CONTACT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LandingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Civic.Terracotta else Civic.MutedSoft

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Civic.Container else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Navigation routes for landing pages
 */
object LandingRoutes {
    const val HOME = "home"
    const val INSTRUCTIONS = "instructions"
    const val NEWS = "news"
    const val ABOUT = "about"
    const val CONTACT = "contact"
}
