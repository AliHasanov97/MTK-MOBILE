package az.mtk.app.data.api

import az.mtk.app.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

/** Qrup detalı üçün bir dəfəyə çəkilən məlumat dəsti. */
data class IdentityGroupBundle(
    val roles: List<UsrGroupRole>,
    val members: List<UsrGroupMember>,
    val availableRoleNames: List<String>,
    val users: List<UsrUserSummary>,
)

/**
 * İdentifikasiya modulunun məlumat qatı — `identity.ts` ilə eyni endpoint-lər.
 * Bu endpoint-lər zərfsiz cavab qaytarır, ona görə `Envelope` açılmır.
 */
object IdentityRepository {

    private val api: IdentityApi by lazy { AppGraph.service(IdentityApi::class.java) }

    // ---- İstifadəçilər -------------------------------------------------------

    suspend fun searchUsers(
        searchTerm: String?,
        page: Int,
        pageSize: Int,
    ): UsrSearchUsersResult = api.searchUsers(
        UsrSearchUsersRequest(
            searchTerm = searchTerm?.trim()?.takeIf { it.isNotEmpty() },
            pageNumber = page,
            pageSize = pageSize,
        ),
    )

    suspend fun createUser(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String?,
    ): String {
        val response = api.registerUser(
            UsrRegisterUserRequest(
                email = email.trim(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                password = password,
                phoneNumber = phoneNumber?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return response.userId ?: throw ApiException("İstifadəçi yaradıla bilmədi.")
    }

    suspend fun updateUser(
        id: String,
        firstName: String,
        lastName: String,
        phoneNumber: String?,
    ) {
        api.updateUser(
            id,
            UsrUpdateUserRequest(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phoneNumber = phoneNumber?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun deleteUser(id: String) {
        api.deleteUser(id)
    }

    suspend fun userRoles(userId: String): UsrUserRoles =
        api.userRoles(userId, includeInheritedRoles = false)

    suspend fun assignRolesToUser(userId: String, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        api.assignRolesToUser(userId, UsrRoleNamesBody(roleNames))
    }

    suspend fun removeRolesFromUser(userId: String, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        api.removeRolesFromUser(userId, UsrRoleNamesBody(roleNames))
    }

    suspend fun listRoles(): List<UsrRole> = api.listRoles()

    // ---- Rollar --------------------------------------------------------------

    suspend fun createRole(name: String, description: String?) {
        api.createRole(
            UsrRoleWriteRequest(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun deleteRole(name: String) {
        api.deleteRole(encodePath(name))
    }

    // ---- Qruplar -------------------------------------------------------------

    suspend fun listGroups(): List<UsrGroup> = api.listGroups()

    suspend fun createGroup(name: String, description: String?) {
        api.createGroup(
            UsrGroupWriteRequest(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                parentGroupId = null,
            ),
        )
    }

    suspend fun deleteGroup(id: String) {
        api.deleteGroup(id)
    }

    suspend fun groupBundle(groupId: String): IdentityGroupBundle = coroutineScope {
        val roles = async { api.groupRoles(groupId) }
        val members = async { api.groupUsers(groupId) }
        val roleNames = async { api.listAvailableRoleNames() }
        val users = async { api.listUsers() }

        IdentityGroupBundle(
            roles = roles.await(),
            members = members.await(),
            availableRoleNames = roleNames.await(),
            users = users.await(),
        )
    }

    suspend fun assignRolesToGroup(groupId: String, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        api.assignRolesToGroup(UsrGroupRolesBody(groupId, roleNames))
    }

    suspend fun removeRolesFromGroup(groupId: String, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        api.removeRolesFromGroup(groupId, UsrRoleNamesBody(roleNames))
    }

    suspend fun addUserToGroup(groupId: String, userId: String) {
        api.addUserToGroup(groupId, userId)
    }

    suspend fun removeUserFromGroup(groupId: String, userId: String) {
        api.removeUserFromGroup(groupId, userId)
    }

    /**
     * Rol adları URL yolunda `encoded = true` gözlənilir; boşluq və ya
     * xüsusi simvol olarsa backend marşrutu pozulmasın deyə burada kodlanır.
     */
    private fun encodePath(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
