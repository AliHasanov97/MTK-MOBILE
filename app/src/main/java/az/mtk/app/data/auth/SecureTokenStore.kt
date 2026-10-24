package az.mtk.app.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

/**
 * Android Keystore backed encrypted token storage.
 * Uses AES256-GCM encryption for both keys and values.
 */
class SecureTokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "mtk.secure.session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
