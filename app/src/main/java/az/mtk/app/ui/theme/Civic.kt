package az.mtk.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "Civic Hearth" — the design language the signed-in APK already speaks:
 * a warm paper canvas, hairline borders instead of shadows, deep-forest brand
 * surfaces and terracotta actions. The landing pages share the same tokens so
 * the public site and the panel read as one product.
 */
object Civic {
    // Canvas
    val Background = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF4F4EE)
    val Container = Color(0xFFF8EBE6)

    // Hairlines
    val Border = Color(0xFFE2E0D8)

    // Ink
    val Ink = Color(0xFF201A18)
    val Muted = Color(0xFF594139)
    val MutedSoft = Color(0xFF8D7168)

    // Signals
    val Terracotta = Color(0xFFD9531E)
    val TerracottaVivid = Color(0xFFE85D04)
    val Forest = Color(0xFF284033)
    val ForestDeep = Color(0xFF1B2D23)
    val ForestTop = Color(0xFF1E3629)
    val Olive = Color(0xFF6F8559)
    val Lime = Color(0xFFD2EBB7)

    // Feedback
    val Error = Color(0xFFBA1A1A)
    val ErrorSoft = Color(0xFFFBEDEB)
    val ErrorText = Color(0xFF93000A)

    // Reusable surfaces
    val HeroGradient = Brush.verticalGradient(listOf(ForestDeep, Forest, ForestTop))
}
