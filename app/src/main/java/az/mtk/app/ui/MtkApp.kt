package az.mtk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.AuthState
import az.mtk.app.ui.components.BrandMark
import az.mtk.app.ui.navigation.AppNav
import az.mtk.app.ui.screens.LoginScreen
import az.mtk.app.ui.theme.MtkColors

@Composable
fun MtkApp() {
    val authState by AppGraph.auth.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = authState) {
            AuthState.Loading -> SplashScreen()
            is AuthState.Unauthenticated -> LoginScreen(message = state.message)
            is AuthState.Authenticated -> AppNav(profile = state.profile)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Soft background radial glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MtkColors.Lime.copy(alpha = 0.2f), androidx.compose.ui.graphics.Color.Transparent),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(size = 72.dp, letterSize = 36)
            Spacer(Modifier.height(20.dp))
            Text(
                "MTK",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Vahid İdarəetmə Sistemi",
                style = MaterialTheme.typography.labelMedium,
                color = MtkColors.MutedSoft,
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MtkColors.Green,
            )
        }
    }
}
