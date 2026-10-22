package az.mtk.app.data.api

// Mirrors MTK-UI app/lib/api/identity.ts exactly (az.mtk.app identity module).

// ---------- Users ----------

data class UsrUserSummary(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val status: String?,
)

data class UsrUserDetail(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val status: String?,
    val identityId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class UsrRoleRef(
    val id: String,
    val name: String,
    val description: String?,
    val roleType: String,
)

data class UsrGroupRef(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
)

data class UsrUserRoles(
    val userId: String,
    val directRoles: List<UsrRoleRef>?,
    val inheritedRoles: List<UsrRoleRef>?,
)

data class UsrUserGroups(
    val userId: String,
    val groups: List<UsrGroupRef>?,
)

data class UsrUserRolesAndGroups(
    val userId: String,
    val directRoles: List<UsrRoleRef>?,
    val inheritedRoles: List<UsrRoleRef>?,
    val groups: List<UsrGroupRef>?,
)

data class UsrSearchUsersRequest(
    val searchTerm: String? = null,
    val pageNumber: Int? = null,
    val pageSize: Int? = null,
    val sortBy: String? = null,
    val sortDirection: String? = null,
)

data class UsrSearchUsersResult(
    val users: List<UsrUserSummary>?,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
)

data class UsrRegisterUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val phoneNumber: String? = null,
    val roleNames: List<String>? = null,
)

data class UsrUpdateUserRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null,
)

data class UsrRegisterUserResponse(val userId: String?)

data class UsrCurrentUser(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val role: String?,
)

data class UsrRoleNamesBody(val roleNames: List<String>)

// ---------- Roles ----------

data class UsrRole(
    val id: String,
    val name: String,
    val description: String?,
    val roleType: String,
    val isActive: Boolean,
)

data class UsrSearchRolesRequest(
    val searchTerm: String? = null,
    val pageNumber: Int? = null,
    val pageSize: Int? = null,
    val sortBy: String? = null,
    val sortDirection: String? = null,
)

data class UsrSearchRolesResult(
    val roles: List<UsrRole>?,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
)

data class UsrRoleWriteRequest(
    val name: String,
    val description: String? = null,
)

data class UsrRoleIdResponse(val roleId: String?)

// ---------- Groups ----------

data class UsrGroup(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
)

data class UsrSearchGroupsRequest(
    val searchTerm: String? = null,
    val pageNumber: Int? = null,
    val pageSize: Int? = null,
    val sortBy: String? = null,
    val sortDirection: String? = null,
)

data class UsrSearchGroupsResult(
    val groups: List<UsrGroup>?,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
)

data class UsrGroupWriteRequest(
    val name: String,
    val description: String? = null,
    val parentGroupId: String? = null,
)

data class UsrGroupIdResponse(val groupId: String?)

data class UsrGroupMember(
    val id: String,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
)

data class UsrGroupRole(
    val id: String,
    val name: String,
    val description: String?,
    val roleType: String,
)

// ---------- Profile (self-service) ----------

data class UsrChangeMyInfoRequest(
    val password: String? = null,
    val phoneNumber: String? = null,
)

interface IdentityApi {

    // ---- Users ----
    @retrofit2.http.GET("api/identity/users")
    suspend fun listUsers(): List<UsrUserSummary>

    @retrofit2.http.GET("api/identity/users/me")
    suspend fun currentUser(): UsrCurrentUser

    @retrofit2.http.POST("api/identity/users/search")
    suspend fun searchUsers(@retrofit2.http.Body body: UsrSearchUsersRequest): UsrSearchUsersResult

    @retrofit2.http.GET("api/identity/users/{id}")
    suspend fun user(@retrofit2.http.Path("id") id: String): UsrUserDetail

    @retrofit2.http.POST("api/identity/users/register")
    suspend fun registerUser(@retrofit2.http.Body body: UsrRegisterUserRequest): UsrRegisterUserResponse

    @retrofit2.http.PATCH("api/identity/users/{id}")
    suspend fun updateUser(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: UsrUpdateUserRequest,
    )

    @retrofit2.http.DELETE("api/identity/users/{id}")
    suspend fun deleteUser(@retrofit2.http.Path("id") id: String)

    @retrofit2.http.GET("api/identity/users/{userId}/roles")
    suspend fun userRoles(
        @retrofit2.http.Path("userId") userId: String,
        @retrofit2.http.Query("includeInheritedRoles") includeInheritedRoles: Boolean = false,
    ): UsrUserRoles

    @retrofit2.http.POST("api/identity/users/{userId}/roles")
    suspend fun assignRolesToUser(
        @retrofit2.http.Path("userId") userId: String,
        @retrofit2.http.Body body: UsrRoleNamesBody,
    )

    @retrofit2.http.DELETE("api/identity/users/{userId}/roles")
    suspend fun removeRolesFromUser(
        @retrofit2.http.Path("userId") userId: String,
        @retrofit2.http.Body body: UsrRoleNamesBody,
    )

    @retrofit2.http.GET("api/identity/users/{userId}/groups")
    suspend fun userGroups(@retrofit2.http.Path("userId") userId: String): UsrUserGroups

    @retrofit2.http.GET("api/identity/users/{userId}/roles-and-groups")
    suspend fun userRolesAndGroups(@retrofit2.http.Path("userId") userId: String): UsrUserRolesAndGroups

    // ---- Roles ----
    @retrofit2.http.GET("api/identity/roles")
    suspend fun listRoles(): List<UsrRole>

    @retrofit2.http.POST("api/identity/roles/search")
    suspend fun searchRoles(@retrofit2.http.Body body: UsrSearchRolesRequest): UsrSearchRolesResult

    @retrofit2.http.POST("api/identity/roles")
    suspend fun createRole(@retrofit2.http.Body body: UsrRoleWriteRequest): UsrRoleIdResponse

    @retrofit2.http.PATCH("api/identity/roles/{oldRoleName}")
    suspend fun updateRole(
        @retrofit2.http.Path(value = "oldRoleName", encoded = true) oldRoleName: String,
        @retrofit2.http.Body body: UsrRoleWriteRequest,
    )

    @retrofit2.http.DELETE("api/identity/roles/{roleName}")
    suspend fun deleteRole(
        @retrofit2.http.Path(value = "roleName", encoded = true) roleName: String,
    )

    // ---- Groups ----
    @retrofit2.http.GET("api/identity/groups")
    suspend fun listGroups(): List<UsrGroup>

    @retrofit2.http.POST("api/identity/groups/search")
    suspend fun searchGroups(@retrofit2.http.Body body: UsrSearchGroupsRequest): UsrSearchGroupsResult

    @retrofit2.http.GET("api/identity/groups/{id}")
    suspend fun group(@retrofit2.http.Path("id") id: String): UsrGroup

    @retrofit2.http.POST("api/identity/groups")
    suspend fun createGroup(@retrofit2.http.Body body: UsrGroupWriteRequest): UsrGroupIdResponse

    @retrofit2.http.PATCH("api/identity/groups/{id}")
    suspend fun updateGroup(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: UsrGroupWriteRequest,
    )

    @retrofit2.http.DELETE("api/identity/groups/{id}")
    suspend fun deleteGroup(@retrofit2.http.Path("id") id: String)

    @retrofit2.http.GET("api/identity/groups/rolenames")
    suspend fun listAvailableRoleNames(): List<String>

    @retrofit2.http.GET("api/identity/groups/{groupId}/roles")
    suspend fun groupRoles(@retrofit2.http.Path("groupId") groupId: String): List<UsrGroupRole>

    @retrofit2.http.POST("api/identity/groups/roles")
    suspend fun assignRolesToGroup(@retrofit2.http.Body body: UsrGroupRolesBody)

    @retrofit2.http.DELETE("api/identity/groups/{groupId}/roles")
    suspend fun removeRolesFromGroup(
        @retrofit2.http.Path("groupId") groupId: String,
        @retrofit2.http.Body body: UsrRoleNamesBody,
    )

    @retrofit2.http.POST("api/identity/groups/{groupId}/users/{userId}")
    suspend fun addUserToGroup(
        @retrofit2.http.Path("groupId") groupId: String,
        @retrofit2.http.Path("userId") userId: String,
    )

    @retrofit2.http.DELETE("api/identity/groups/{groupId}/users/{userId}")
    suspend fun removeUserFromGroup(
        @retrofit2.http.Path("groupId") groupId: String,
        @retrofit2.http.Path("userId") userId: String,
    )

    @retrofit2.http.GET("api/identity/groups/{groupId}/users")
    suspend fun groupUsers(@retrofit2.http.Path("groupId") groupId: String): List<UsrGroupMember>

    // ---- Profile (self-service) ----
    @retrofit2.http.PATCH("api/identity/profile/change-info")
    suspend fun changeMyInfo(@retrofit2.http.Body body: UsrChangeMyInfoRequest)
}

data class UsrGroupRolesBody(
    val groupId: String,
    val roleNames: List<String>,
)
