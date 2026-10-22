package az.mtk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Modern design palette inspired by MTK-UI (app/globals.css)
 * with native mobile Polish, emerald green tones, and soft paper surfaces.
 */
object MtkColors {
    val Paper = Color(0xFFF7F8F4)
    val Ink = Color(0xFF14221E)
    val InkLight = Color(0xFF2C3E37)
    val Muted = Color(0xFF5E6D66)
    val MutedSoft = Color(0xFF88968F)
    val MutedUltraSoft = Color(0xFFB5C0B9)

    val Green = Color(0xFF345C4A)
    val GreenDark = Color(0xFF234435)
    val GreenLight = Color(0xFF45745E)
    val Lime = Color(0xFFC9E3A1)
    val LimeDark = Color(0xFF8FA967)

    val Line = Color(0xFFE4E8DF)
    val LineSoft = Color(0xFFEDF0E9)
    val White = Color(0xFFFFFFFF)
    val Soft = Color(0xFFF1F3ED)

    val Positive = Color(0xFF437A3B)
    val PositiveBg = Color(0xFFEAF5E4)
    val Negative = Color(0xFFC0392B)
    val NegativeBg = Color(0xFFFDECEB)
    val Warning = Color(0xFFC27803)
    val WarningBg = Color(0xFFFEF4E3)
    val Info = Color(0xFF2563EB)
    val InfoBg = Color(0xFFEEF4FF)

    val IconBoxBg = Color(0xFFEDF3E8)
    val IconBoxTint = Color(0xFF2F5443)
}

private val LightColors = lightColorScheme(
    primary = MtkColors.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F2E4),
    onPrimaryContainer = MtkColors.GreenDark,
    secondary = MtkColors.Lime,
    onSecondary = MtkColors.GreenDark,
    background = MtkColors.Paper,
    onBackground = MtkColors.Ink,
    surface = MtkColors.White,
    onSurface = MtkColors.Ink,
    surfaceVariant = MtkColors.Soft,
    onSurfaceVariant = MtkColors.Muted,
    outline = MtkColors.Line,
    outlineVariant = MtkColors.LineSoft,
    error = MtkColors.Negative,
)

private val DarkColors = darkColorScheme(
    primary = MtkColors.Lime,
    onPrimary = MtkColors.GreenDark,
    primaryContainer = Color(0xFF223E30),
    onPrimaryContainer = MtkColors.Lime,
    secondary = MtkColors.Lime,
    onSecondary = MtkColors.GreenDark,
    background = Color(0xFF101915),
    onBackground = Color(0xFFEDF3ED),
    surface = Color(0xFF16231E),
    onSurface = Color(0xFFEDF3ED),
    surfaceVariant = Color(0xFF1D2C25),
    onSurfaceVariant = Color(0xFFB0BFB7),
    outline = Color(0xFF293B32),
    outlineVariant = Color(0xFF223129),
    error = Color(0xFFE58787),
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
fun MtkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MtkTypography,
        content = content,
    )
}
