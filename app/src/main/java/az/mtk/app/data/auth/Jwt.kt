package az.mtk.app.data.auth

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Base64 as JvmBase64

/** Reads the (unverified) payload of a JWT; the signature is validated by the backend. */
object Jwt {
    fun profileFrom(accessToken: String): UserProfile {
        val payload = payload(accessToken) ?: return UserProfile("İstifadəçi", "user", null, emptyList())

        val name = payload.get("name")?.asString
            ?: listOfNotNull(
                payload.get("given_name")?.asString,
                payload.get("family_name")?.asString,
            ).joinToString(" ").trim()

        val username = payload.get("preferred_username")?.asString
            ?: payload.get("sub")?.asString
            ?: "user"

        // Keycloak also injects technical roles (offline_access, uma_authorization,
        // default-roles-mtk) — only the business roles are meaningful in the UI.
        val knownRoles = setOf("admin", "accountant", "owner", "building-manager", "employee")
        val roles = payload.getAsJsonObject("realm_access")
            ?.getAsJsonArray("roles")
            ?.mapNotNull { it.asString }
            ?.filter { it in knownRoles }
            ?: emptyList()

        return UserProfile(
            displayName = name.ifBlank { username },
            username = username,
            email = payload.get("email")?.asString,
            roles = roles,
        )
    }

    fun expSeconds(token: String): Long? =
        payload(token)?.get("exp")?.asLong

    private fun payload(token: String): JsonObject? {
        val parts = token.split('.')
        if (parts.size < 2) return null
        return runCatching {
            val bytes = JvmBase64.getUrlDecoder().decode(parts[1])
            JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        }.getOrNull()
    }
}
