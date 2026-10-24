package az.mtk.app.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure PIN management with PBKDF2 hashing and lockout protection.
 */
class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "mtk.pin",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isPinSet(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN 4 rəqəm olmalıdır" }

        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun verifyPin(pin: String): PinVerifyResult {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        val now = System.currentTimeMillis()

        if (lockoutUntil > now) {
            val remainingSeconds = ((lockoutUntil - now) / 1000).toInt()
            return PinVerifyResult.LockedOut(remainingSeconds)
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return PinVerifyResult.NoPinSet
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return PinVerifyResult.NoPinSet

        val inputHash = hashPin(pin, salt)

        return if (inputHash == storedHash) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply()
            PinVerifyResult.Success
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, attempts)
                    .putLong(KEY_LOCKOUT_UNTIL, now + LOCKOUT_DURATION_MS)
                    .apply()
                PinVerifyResult.LockedOut(LOCKOUT_DURATION_MS.toInt() / 1000)
            } else {
                prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
                PinVerifyResult.Failed(attemptsRemaining = MAX_FAILED_ATTEMPTS - attempts)
            }
        }
    }

    fun clearPin() {
        prefs.edit().clear().apply()
    }

    fun getRemainingAttempts(): Int {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        return (MAX_FAILED_ATTEMPTS - attempts).coerceAtLeast(0)
    }

    fun getLockoutRemaining(): Int {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            ((lockoutUntil - now) / 1000).toInt()
        } else {
            0
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"

        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
        private const val PBKDF2_ITERATIONS = 100_000
    }
}

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data object NoPinSet : PinVerifyResult
    data class Failed(val attemptsRemaining: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
}
