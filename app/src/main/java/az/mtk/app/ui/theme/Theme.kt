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
 * Material theme built on the shared "Civic Hearth" palette ([Civic]) that the
 * dashboard, the panel modules and the landing pages all use: paper canvas,
 * forest brand surfaces, terracotta actions.
 */
private val LightColors = lightColorScheme(
    primary = Civic.Terracotta,
    onPrimary = Color.White,
    primaryContainer = Civic.Container,
    onPrimaryContainer = Civic.Terracotta,
    secondary = Civic.Forest,
    onSecondary = Color.White,
    secondaryContainer = Civic.Subtle,
    onSecondaryContainer = Civic.Forest,
    tertiary = Civic.Olive,
    onTertiary = Color.White,
    tertiaryContainer = Civic.Subtle,
    onTertiaryContainer = Civic.Olive,
    background = Civic.Background,
    onBackground = Civic.Ink,
    surface = Civic.Surface,
    onSurface = Civic.Ink,
    surfaceVariant = Civic.Subtle,
    onSurfaceVariant = Civic.Muted,
    outline = Civic.Border,
    outlineVariant = Civic.Border,
    error = Civic.Error,
    onError = Color.White,
    errorContainer = Civic.ErrorSoft,
    onErrorContainer = Civic.ErrorText,
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
