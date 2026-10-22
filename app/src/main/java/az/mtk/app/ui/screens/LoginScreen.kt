package az.mtk.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.AppGraph
import az.mtk.app.ui.components.BrandMark
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.theme.MtkColors

@Composable
fun LoginScreen(message: String?) {
    val context = LocalContext.current
    var launchError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Soft ambient glow at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MtkColors.Lime.copy(alpha = 0.25f), MaterialTheme.colorScheme.background),
                        radius = 800f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            // Header Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 42.dp, letterSize = 22)
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "MTK",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp,
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
                        "VAHİD İDARƏETMƏ PLATFORMASI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Eyebrow
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MtkColors.IconBoxBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    "MƏNZİL-TİKİNTİ KOOPERATİVİ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.Green,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Main Heading
            Text(
                buildAnnotatedString {
                    append("Binalar, sakinlər və maliyyə ")
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = MtkColors.Green,
                        ),
                    ) {
                        append("bir paneldə")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.displaySmall,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            )

            Spacer(Modifier.height(14.dp))
            Text(
                "Mənzillər, qarajlar, borclar və işçi heyəti üçün müasir idarəetmə həlli. " +
                    "Təşkilat hesabınız vasitəsilə dərhal daxil olun.",
                style = MaterialTheme.typography.bodyMedium,
                color = MtkColors.Muted,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(28.dp))

            // Feature Highlights
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LoginFeatureCard(
                    icon = Icons.Outlined.Apartment,
                    title = "Binalar və Mənzillər",
                    description = "Bütün binalar, mərtəbə və otaq planlarının vahid reyestri",
                )
                LoginFeatureCard(
                    icon = Icons.Outlined.People,
                    title = "Mülkiyyətçilər və Sakinlər",
                    description = "Sakin bazası, qarajlar və müqavilələrin idarə edilməsi",
                )
                LoginFeatureCard(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Maliyyə və Hesablaşmalar",
                    description = "Kommunal ödənişlər, borclar və detallı çıxarışlar",
                )
            }

            Spacer(Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = {
                    launchError = null
                    runCatching {
                        context.startActivity(AppGraph.auth.startLogin())
                    }.onFailure {
                        launchError = "Brauzer açıla bilmədi. Cihazda bir brauzer quraşdırıldığını yoxlayın."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MtkColors.Green,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Keycloak ilə daxil ol",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            val errorText = launchError ?: message
            if (errorText != null) {
                Spacer(Modifier.height(16.dp))
                MtkCard(
                    padding = 12.dp,
                    containerColor = MtkColors.NegativeBg,
                    borderColor = MtkColors.Negative.copy(alpha = 0.3f),
                ) {
                    Text(
                        errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MtkColors.Negative,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MtkColors.Positive),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Təhlükəsiz giriş · Keycloak SSO · PKCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MtkColors.MutedSoft,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                "© ${java.time.Year.now()} MTK · Bütün hüquqlar qorunur",
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun LoginFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
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
                    icon,
                    contentDescription = null,
                    tint = MtkColors.Green,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MtkColors.Muted,
                )
            }
        }
    }
}
