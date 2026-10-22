package az.mtk.app.data.auth

import java.io.Serializable

data class UserProfile(
    val displayName: String,
    val username: String,
    val email: String?,
    val roles: List<String>,
) : Serializable {

    val initials: String
        get() = displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { username.take(1).uppercase() }

    fun hasAnyRole(vararg allowed: String): Boolean = allowed.any { it in roles }
}

/** Roles seeded in Keycloak (mirrors MTK-UI app/lib/auth/roleConstants.ts). */
object Roles {
    const val ADMIN = "admin"
    const val ACCOUNTANT = "accountant"
    const val OWNER = "owner"
    const val BUILDING_MANAGER = "building-manager"
    const val EMPLOYEE = "employee"

    private val labels = mapOf(
        ADMIN to "İdarəçi",
        ACCOUNTANT to "Mühasib",
        OWNER to "Mənzil sahibi",
        BUILDING_MANAGER to "Komandant",
        EMPLOYEE to "İşçi",
    )

    fun label(role: String): String = labels[role] ?: role
}

sealed interface AuthState {
    data object Loading : AuthState
    data class Unauthenticated(val message: String? = null) : AuthState
    data class Authenticated(val profile: UserProfile) : AuthState
}
