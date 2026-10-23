package az.mtk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Palette mirrored from MTK-UI's public site (app/globals.css :root): deep
 * green, lime accent, warm paper and quiet ink — the same tones the web
 * landing pages and the panel use, so both surfaces read as one product.
 */
object MtkColors {
    // Primary colors — site green
    val Primary = Color(0xFF345C4A)
    val PrimaryDark = Color(0xFF263E34)
    val PrimaryLight = Color(0xFF64804E)
    val Lime = Color(0xFFC9E3A1)

    // Background colors
    val Background = Color(0xFFF7F8F4)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEF1E9)

    // Text colors
    val TextPrimary = Color(0xFF162522)
    val TextSecondary = Color(0xFF64716C)
    val TextTertiary = Color(0xFF838D86)

    // Border and divider
    val Border = Color(0xFFE5E9E1)
    val Divider = Color(0xFFF0F2EC)

    // Status colors — muted, like the panel's balance tags
    val Success = Color(0xFF4C7A38)
    val SuccessBg = Color(0xFFEEF6E6)
    val Error = Color(0xFFA24A4A)
    val ErrorBg = Color(0xFFFBF2F2)
    val Warning = Color(0xFF8A6A2E)
    val WarningBg = Color(0xFFFBF4E6)
    val Info = Color(0xFF526354)
    val InfoBg = Color(0xFFEEF1E9)

    // Icon and accent colors
    val IconPrimary = Color(0xFF345C4A)
    val IconSecondary = Color(0xFF64716C)
    val AccentOrange = Color(0xFF64804E)
    val AccentGreen = Color(0xFF8EAF67)
    val AccentBlue = Color(0xFF526354)
}

private val LightColors = lightColorScheme(
    primary = MtkColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MtkColors.SurfaceVariant,
    onPrimaryContainer = MtkColors.Primary,
    secondary = MtkColors.PrimaryLight,
    onSecondary = Color.White,
    background = MtkColors.Background,
    onBackground = MtkColors.TextPrimary,
    surface = MtkColors.Surface,
    onSurface = MtkColors.TextPrimary,
    surfaceVariant = MtkColors.SurfaceVariant,
    onSurfaceVariant = MtkColors.TextSecondary,
    outline = MtkColors.Border,
    outlineVariant = MtkColors.Divider,
    error = MtkColors.Error,
)

private val MtkTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.6.sp),
)

@Composable
fun MtkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MtkTypography,
        content = content,
    )
}
