package az.mtk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import az.mtk.app.ui.MtkApp
import az.mtk.app.ui.theme.MtkTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Track app background time for PIN lock
        lifecycle.addObserver(AppLockLifecycleObserver())

        setContent {
            MtkTheme {
                MtkApp()
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /** Keycloak redirects back to az.mtk.app:/oauth2redirect. */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != AppConfig.RedirectScheme) return

        lifecycleScope.launch {
            AppGraph.auth.completeLogin(data)
        }
    }
}

private class AppLockLifecycleObserver : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        // Record time when app goes to background
        AppGraph.lockManager.recordActivity()
    }
}
