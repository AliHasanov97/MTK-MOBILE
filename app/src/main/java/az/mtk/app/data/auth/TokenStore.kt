package az.mtk.app.data.auth

import android.content.Context
import com.google.gson.Gson
import java.io.Serializable

data class Session(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresAt: Long,
    val profile: UserProfile,
) : Serializable

/**
 * Minimal, private storage for the OIDC session. Tokens are app-private
 * (MODE_PRIVATE); Android's backup is disabled for this app in the manifest, so
 * they never leave the device through a backup round-trip.
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("mtk.session", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): Session? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { gson.fromJson(raw, Session::class.java) }.getOrNull()
    }

    fun save(session: Session) {
        prefs.edit().putString(KEY_SESSION, gson.toJson(session)).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SESSION = "session"
    }
}
