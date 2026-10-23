package az.mtk.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.theme.MtkColors

/**
 * Top app bar for landing pages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingTopBar(
    companyName: String,
    onLoginClick: () -> Unit,
    userName: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "MTK Logo",
                    tint = MtkColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signed-in user: show the account instead of the login button
                if (userName != null) {
                    OutlinedButton(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MtkColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MtkColors.Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }
                } else {
                    // Login button
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MtkColors.Primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Daxil ol", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Bottom navigation for landing pages - Minimalist transparent floating style
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandingNavItem(
                    icon = Icons.Default.Home,
                    label = "Əsas",
                    selected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                LandingNavItem(
                    icon = Icons.Default.Book,
                    label = "Təlimat",
                    selected = currentRoute == "instructions",
                    onClick = { onNavigate("instructions") }
                )
                LandingNavItem(
                    icon = Icons.Default.Newspaper,
                    label = "Xəbərlər",
                    selected = currentRoute == "news",
                    onClick = { onNavigate("news") }
                )
                LandingNavItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Haqqımızda",
                    selected = currentRoute == "about",
                    onClick = { onNavigate("about") }
                )
                LandingNavItem(
                    icon = Icons.Default.Phone,
                    label = "Əlaqə",
                    selected = currentRoute == "contact",
                    onClick = { onNavigate("contact") }
                )
            }
        }
    }
}

@Composable
private fun LandingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) MtkColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MtkColors.Primary else MtkColors.TextTertiary,
            modifier = Modifier.size(if (selected) 26.dp else 24.dp)
        )
        Text(
            text = label,
            fontSize = if (selected) 10.sp else 9.sp,
            color = if (selected) MtkColors.Primary else MtkColors.TextTertiary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
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
