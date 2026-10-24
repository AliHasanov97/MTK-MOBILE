package az.mtk.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import az.mtk.app.AppConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Keycloak login via Authorization Code + PKCE (public client), the native
 * counterpart of MTK-UI's browser flow (app/lib/auth/client.ts).
 *
 * The flow: build an authorize URL with an S256 code challenge → open it in a
 * Custom Tab → Keycloak redirects back to az.mtk.app:/oauth2redirect → exchange
 * the code at the token endpoint → store the session and keep it fresh.
 */
class AuthRepository(private val context: Context) {

    private val store = TokenStore(context)
    private val pending = context.getSharedPreferences("mtk.oidc", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = ReentrantLock()

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        val session = store.load()
        if (session == null) {
            _state.value = AuthState.Unauthenticated()
            return
        }
        if (isFresh(session)) {
            _state.value = AuthState.Authenticated(session.profile)
            return
        }
        scope.launch {
            val refreshed = runCatching { refreshBlocking(session) }.getOrNull()
            _state.value = if (refreshed != null) {
                AuthState.Authenticated(refreshed.profile)
            } else {
                store.clear()
                AuthState.Unauthenticated("Sessiyanın vaxtı bitdi. Yenidən daxil olun.")
            }
        }
    }

    /** Intent that opens the Keycloak login page in a Custom Tab. */
    fun startLogin(): Intent {
        val verifier = randomUrlSafe(64)
        val state = randomUrlSafe(24)
        pending.edit().putString(KEY_VERIFIER, verifier).putString(KEY_STATE, state).apply()

        val authorizeUri = Uri.parse(AppConfig.AuthorizationEndpoint).buildUpon()
            .appendQueryParameter("client_id", AppConfig.ClientId)
            .appendQueryParameter("redirect_uri", AppConfig.RedirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", AppConfig.Scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        val tabsIntent = runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .intent
        }.getOrElse {
            Intent(Intent.ACTION_VIEW)
        }
        tabsIntent.data = authorizeUri
        return tabsIntent
    }

    /** Handles the az.mtk.app:/oauth2redirect deep link. */
    suspend fun completeLogin(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _state.value = AuthState.Loading

            val error = uri.getQueryParameter("error")
            if (error != null) {
                val description = uri.getQueryParameter("error_description") ?: error
                error(description)
            }

            val expectedState = pending.getString(KEY_STATE, null)
            val verifier = pending.getString(KEY_VERIFIER, null)
            val returnedState = uri.getQueryParameter("state")
            if (verifier == null || expectedState == null) {
                error("Giriş sorğusu tapılmadı. Yenidən cəhd edin.")
            }
            if (returnedState != expectedState) {
                error("Giriş sorğusunun state dəyəri uyğun gəlmir. Yenidən cəhd edin.")
            }
            val code = uri.getQueryParameter("code") ?: error("Keycloak kodu qaytarmadı.")

            val form = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", AppConfig.RedirectUri)
                .add("client_id", AppConfig.ClientId)
                .add("code_verifier", verifier)
                .build()

            val session = exchange(form, previousRefreshToken = null)
            pending.edit().clear().apply()
            if (session == null) error("Token cavabı oxuna bilmədi.")
            store.save(session)
            _state.value = AuthState.Authenticated(session.profile)
        }.onFailure { failure ->
            _state.value = AuthState.Unauthenticated(failure.message)
        }
    }

    /** Cancels a login the user abandoned in the browser. */
    fun cancelLogin(message: String? = null) {
        pending.edit().clear().apply()
        if (_state.value is AuthState.Loading) {
            _state.value = AuthState.Unauthenticated(message)
        }
    }

    /**
     * Token for outgoing API calls, refreshed synchronously when it is about to
     * expire. Always called off the main thread (OkHttp interceptor).
     */
    fun currentAccessToken(): String? {
        val session = store.load() ?: return null
        if (isFresh(session)) return session.accessToken

        lock.withLock {
            val latest = store.load() ?: return null
            if (isFresh(latest)) return latest.accessToken
            return try {
                refreshBlocking(latest)?.accessToken
            } catch (e: Exception) {
                store.clear()
                _state.value = AuthState.Unauthenticated("Sessiyanın vaxtı bitdi. Yenidən daxil olun.")
                null
            }
        }
    }

    /** Forces a refresh after a 401, even if the access token has not expired yet. */
    fun forceRefresh(): String? = lock.withLock {
        val session = store.load() ?: return null
        try {
            refreshBlocking(session)?.accessToken
        } catch (e: Exception) {
            store.clear()
            _state.value = AuthState.Unauthenticated("Sessiyanın vaxtı bitdi. Yenidən daxil olun.")
            null
        }
    }

    /**
     * Clears the local session and ends the Keycloak SSO session.
     *
     * Yalnız lokal tokenləri silmək kifayət etmir: brauzerdə Keycloak-un SSO
     * cookie-si qalır və növbəti "daxil ol" sorğusu istifadəçini parol
     * soruşmadan yenidən içəri buraxır. Keycloak-ın logout endpointi refresh
     * tokeni back-channel logout kimi qəbul edir (204) və serverdəki sessiyanı
     * ləğv edir — bundan sonra brauzer cookie-si etibarsız olur.
     */
    suspend fun logout() {
        val session = store.load()
        store.clear()
        pending.edit().clear().apply()
        _state.value = AuthState.Unauthenticated()

        withContext(Dispatchers.IO) {
            runCatching {
                val refreshToken = session?.refreshToken
                val idToken = session?.idToken
                val request = when {
                    refreshToken != null -> {
                        val form = FormBody.Builder()
                            .add("client_id", AppConfig.ClientId)
                            .add("refresh_token", refreshToken)
                            .build()
                        Request.Builder()
                            .url(AppConfig.EndSessionEndpoint)
                            .post(form)
                            .build()
                    }

                    idToken != null -> {
                        val url = Uri.parse(AppConfig.EndSessionEndpoint).buildUpon()
                            .appendQueryParameter("client_id", AppConfig.ClientId)
                            .appendQueryParameter("id_token_hint", idToken)
                            .build()
                            .toString()
                        Request.Builder().url(url).get().build()
                    }

                    else -> return@runCatching
                }
                client.newCall(request).execute().use { /* nəticə vacib deyil */ }
            }
        }
    }

    private fun refreshBlocking(session: Session): Session? {
        val refreshToken = session.refreshToken ?: return null
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", AppConfig.ClientId)
            .build()
        // Keycloak rotates refresh tokens, so the rotated pair must be persisted; otherwise
        // the next refresh replays an already-consumed token and fails.
        val refreshed = exchange(form, previousRefreshToken = refreshToken)
        if (refreshed != null) store.save(refreshed)
        return refreshed
    }

    private fun exchange(form: FormBody, previousRefreshToken: String?): Session? {
        val request = Request.Builder()
            .url(AppConfig.TokenEndpoint)
            .post(form)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching {
                    val json = JsonParser.parseString(text).asJsonObject
                    json.get("error_description")?.asString ?: json.get("error")?.asString
                }.getOrNull()
                error(error ?: "Keycloak xətası (${response.code})")
            }

            val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
                ?: return null

            val accessToken = json.stringOrNull("access_token") ?: return null
            val expiresIn = json.get("expires_in")?.asLong ?: 300L
            val profile = Jwt.profileFrom(accessToken)
            return Session(
                accessToken = accessToken,
                refreshToken = json.stringOrNull("refresh_token") ?: previousRefreshToken,
                idToken = json.stringOrNull("id_token"),
                expiresAt = System.currentTimeMillis() + expiresIn * 1000,
                profile = profile,
            )
        }
    }

    private fun isFresh(session: Session) = session.expiresAt - 30_000 > System.currentTimeMillis()

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun randomUrlSafe(bytes: Int): String {
        val buffer = ByteArray(bytes)
        SecureRandom().nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    private companion object {
        const val KEY_VERIFIER = "code_verifier"
        const val KEY_STATE = "state"
    }
}
