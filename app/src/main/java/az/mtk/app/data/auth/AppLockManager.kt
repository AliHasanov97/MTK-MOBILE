package az.mtk.app.data.auth

import android.content.Context

/**
 * Manages app lock state and background timeout.
 * Locks the app after 2 minutes in background or on process restart.
 */
class AppLockManager(context: Context) {

    private val prefs = context.getSharedPreferences("mtk.lock", Context.MODE_PRIVATE)

    // Cold process start (app launch or reboot) always begins locked
    @Volatile
    private var isLockedInMemory = true

    fun recordActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    fun isLocked(): Boolean {
        return isLockedInMemory || prefs.getBoolean(KEY_IS_LOCKED, true)
    }

    fun shouldLock(): Boolean {
        if (isLocked()) return true

        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0)
        if (lastActivity == 0L) return true

        val elapsed = System.currentTimeMillis() - lastActivity
        if (elapsed > LOCK_TIMEOUT_MS) {
            lock()
            return true
        }

        return false
    }

    fun unlock() {
        isLockedInMemory = false
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    fun lock() {
        isLockedInMemory = true
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, true)
            .putLong(KEY_LAST_ACTIVITY, 0)
            .apply()
    }

    fun clear() {
        isLockedInMemory = true
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val LOCK_TIMEOUT_MS = 2 * 60 * 1000L // 2 minutes
    }
}
