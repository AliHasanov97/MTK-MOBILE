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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.AppConfig
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.Roles
import az.mtk.app.data.auth.UserProfile
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.navigation.Routes
import az.mtk.app.ui.theme.MtkColors

private data class MoreLink(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val roles: List<String>? = null,
)

private val links = listOf(
    MoreLink("Qarajlar və Dayanacaqlar", "Qaraj yerlərinin uçotu", Icons.Outlined.DirectionsCar, Routes.Garages),
    MoreLink("Borclar və Ödənişlər", "Sakin hesabları və borc hesabatları", Icons.AutoMirrored.Outlined.ReceiptLong, Routes.Charges),
    MoreLink("Müqavilələr", "Xidmət müqavilələrinin idarəsi", Icons.Outlined.Description, Routes.Contracts),
    MoreLink("Tədarükçülər", "Tədarükçü kataloqu və balansları", Icons.Outlined.LocalShipping, Routes.Vendors),
    MoreLink("Müştərilər", "Müştəri qeydiyyatı və balansları", Icons.Outlined.Groups, Routes.Customers),
    MoreLink("Maliyyə əməliyyatları", "Xərclər, ödənişlər, gəlirlər, tariflər", Icons.Outlined.Payments, Routes.Expenses),
    MoreLink("Tranzaksiyalar", "Bütün maliyyə hərəkətləri", Icons.Outlined.SwapHoriz, Routes.Transactions),
    MoreLink("Hesabatlar", "Ödəniş qrafiki və ümumi hesabat", Icons.Outlined.BarChart, Routes.PaymentChart),
    MoreLink("Müraciətlər", "Sakin müraciətlərinin izlənməsi", Icons.Outlined.Forum, Routes.Appeals),
    MoreLink("İnventar", "Anbar, materiallar, daxilolmalar", Icons.Outlined.Inventory2, Routes.Warehouse),
    MoreLink("İnsan Resursları (HR)", "İşçilər, əmrlər, tabel, təqvim", Icons.Outlined.Badge, Routes.Hr),
    MoreLink("İstifadəçilər", "Hesablar, rollar və qruplar", Icons.Outlined.ManageAccounts, Routes.Users, listOf(Roles.ADMIN)),
    MoreLink("Xəbərlər", "Yayımlanan xəbərlərin idarəsi", Icons.Outlined.Article, Routes.Articles, listOf(Roles.ADMIN, Roles.BUILDING_MANAGER)),
    MoreLink("Şirkət məlumatları", "Təşkilat profilinin saxlanması", Icons.Outlined.Business, Routes.CompanyProfile, listOf(Roles.ADMIN)),
    MoreLink("Audit qeydləri", "Sistemdəki dəyişiklik jurnalı", Icons.Outlined.History, Routes.Audit, listOf(Roles.ADMIN)),
)

@Composable
fun MoreScreen(
    profile: UserProfile,
    onNavigate: (String) -> Unit,
    showTopBar: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(title = "Daha çox", subtitle = "Profil və əlavə bölmələr")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Profile Card
            MtkCard(padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCircle(initials = profile.initials, size = 54.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "@${profile.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.MutedSoft,
                        )
                        if (profile.email != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                    }
                }

                if (profile.roles.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.roles.take(4).forEach { role ->
                            Pill(Roles.label(role), Tone.Brand)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = { AppGraph.auth.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MtkColors.Negative,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MtkColors.Negative.copy(alpha = 0.35f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Hesabdan çıxış",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Extra Modules Section
            SectionHeading(
                title = "Əlavə Modullar",
                subtitle = "Digər idarəetmə bölmələri",
            )
            Spacer(Modifier.height(10.dp))

            MtkCard(padding = 4.dp) {
                val visibleLinks = links.filter { link ->
                    link.roles == null || profile.hasAnyRole(*link.roles.toTypedArray())
                }
                visibleLinks.forEachIndexed { index, link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(link.route) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MtkColors.IconBoxBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                link.icon,
                                contentDescription = null,
                                tint = MtkColors.Green,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                link.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                link.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MtkColors.Muted,
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MtkColors.MutedUltraSoft,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (index != visibleLinks.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // System Info Section
            SectionHeading(title = "Sistem və Bağlantı")
            Spacer(Modifier.height(10.dp))

            MtkCard(padding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Mobil Tətbiq Versiyası", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                    Text("1.0.0 (Release)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Backend API", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                    Text(
                        AppConfig.ApiBaseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Autentifikasiya", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                    Text("Keycloak OIDC · PKCE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
