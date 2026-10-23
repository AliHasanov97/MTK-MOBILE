package az.mtk.app.ui.screens.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.IdentityRepository
import az.mtk.app.data.api.UsrGroup
import az.mtk.app.data.api.UsrGroupMember
import az.mtk.app.data.api.UsrGroupRole
import az.mtk.app.data.api.UsrRole
import az.mtk.app.data.api.UsrUserSummary
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class IdentityTab(val label: String) {
    USERS("İstifadəçilər"),
    ROLES("Rollar"),
    GROUPS("Qruplar"),
}

data class IdentityUsersState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val search: String = "",
    val items: List<UsrUserSummary> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val pageSize: Int = 10,
    val formSaving: Boolean = false,
    val formError: String? = null,
    val formDone: Boolean = false,
    val deletingId: String? = null,
    val actionError: String? = null,
    val notice: String? = null,
) {
    val pageCount: Int get() = maxOf(1, (totalCount + pageSize - 1) / pageSize)
}

data class UserRolesEditorState(
    val userId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val allRoles: List<UsrRole> = emptyList(),
    val originalNames: Set<String> = emptySet(),
    val selectedNames: Set<String> = emptySet(),
    val saving: Boolean = false,
    val done: Boolean = false,
)

data class IdentityRolesState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val search: String = "",
    val items: List<UsrRole> = emptyList(),
    val formSaving: Boolean = false,
    val formError: String? = null,
    val formDone: Boolean = false,
    val deletingName: String? = null,
    val actionError: String? = null,
    val notice: String? = null,
) {
    val visibleItems: List<UsrRole>
        get() {
            val term = search.trim().lowercase()
            if (term.isEmpty()) return items
            return items.filter {
                it.name.lowercase().contains(term) ||
                    it.description.orEmpty().lowercase().contains(term)
            }
        }
}

data class IdentityGroupsState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val search: String = "",
    val items: List<UsrGroup> = emptyList(),
    val formSaving: Boolean = false,
    val formError: String? = null,
    val formDone: Boolean = false,
    val deletingId: String? = null,
    val actionError: String? = null,
    val notice: String? = null,
) {
    val visibleItems: List<UsrGroup>
        get() {
            val term = search.trim().lowercase()
            if (term.isEmpty()) return items
            return items.filter {
                it.name.lowercase().contains(term) ||
                    it.description.orEmpty().lowercase().contains(term)
            }
        }
}

data class GroupDetailState(
    val group: UsrGroup? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val roles: List<UsrGroupRole> = emptyList(),
    val members: List<UsrGroupMember> = emptyList(),
    val availableRoleNames: List<String> = emptyList(),
    val allUsers: List<UsrUserSummary> = emptyList(),
    val busy: Boolean = false,
    val actionError: String? = null,
    val notice: String? = null,
)

data class IdentityUiState(
    val users: IdentityUsersState = IdentityUsersState(),
    val userRoles: UserRolesEditorState = UserRolesEditorState(),
    val roles: IdentityRolesState = IdentityRolesState(),
    val groups: IdentityGroupsState = IdentityGroupsState(),
    val groupDetail: GroupDetailState = GroupDetailState(),
)

/**
 * İdentifikasiya modulunun vəziyyəti: istifadəçilər, rollar, qruplar.
 *
 * İstifadəçi siyahısı serverdə səhifələnir (10 qeyd); rollar və qruplar
 * az sayda olduğu üçün tam çəkilir və axtarış müştəri tərəfində aparılır.
 */
class IdentityViewModel : ViewModel() {

    private val _state = MutableStateFlow(IdentityUiState())
    val state = _state.asStateFlow()

    private var usersRequest = 0
    private var userSearchJob: Job? = null
    private var rolesRequest = 0
    private var groupsRequest = 0
    private var groupDetailRequest = 0

    // ---- İstifadəçilər -------------------------------------------------------

    fun ensureUsers() {
        val users = _state.value.users
        if (!users.loaded && !users.loading) loadUsers()
    }

    fun refreshUsers() = loadUsers()

    fun setUserSearch(value: String) {
        _state.value = _state.value.copy(
            users = _state.value.users.copy(search = value, page = 0),
        )
        userSearchJob?.cancel()
        userSearchJob = viewModelScope.launch {
            delay(350)
            loadUsers()
        }
    }

    fun nextUsersPage() {
        val users = _state.value.users
        if (users.loading || users.page + 1 >= users.pageCount) return
        _state.value = _state.value.copy(users = users.copy(page = users.page + 1))
        loadUsers()
    }

    fun prevUsersPage() {
        val users = _state.value.users
        if (users.loading || users.page <= 0) return
        _state.value = _state.value.copy(users = users.copy(page = users.page - 1))
        loadUsers()
    }

    private fun loadUsers(silent: Boolean = false) {
        val request = ++usersRequest
        val snapshot = _state.value.users
        _state.value = _state.value.copy(
            users = snapshot.copy(
                loading = !silent || snapshot.items.isEmpty(),
                error = null,
            ),
        )

        viewModelScope.launch {
            try {
                val result = IdentityRepository.searchUsers(
                    searchTerm = snapshot.search,
                    page = snapshot.page + 1,
                    pageSize = snapshot.pageSize,
                )
                if (request != usersRequest) return@launch
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        items = result.users.orEmpty(),
                        totalCount = result.totalCount,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != usersRequest) return@launch
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        loading = false,
                        loaded = true,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun createUser(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String,
    ) {
        val users = _state.value.users
        if (users.formSaving) return

        _state.value = _state.value.copy(
            users = users.copy(formSaving = true, formError = null, formDone = false),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.createUser(
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    password = password,
                    phoneNumber = phoneNumber,
                )
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        formSaving = false,
                        formError = null,
                        formDone = true,
                        page = 0,
                        notice = "İstifadəçi yaradıldı.",
                    ),
                )
                loadUsers(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        formSaving = false,
                        formError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun updateUser(
        id: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
    ) {
        val users = _state.value.users
        if (users.formSaving) return

        _state.value = _state.value.copy(
            users = users.copy(formSaving = true, formError = null, formDone = false),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.updateUser(id, firstName, lastName, phoneNumber)
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        formSaving = false,
                        formError = null,
                        formDone = true,
                        notice = "İstifadəçi yeniləndi.",
                    ),
                )
                loadUsers(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        formSaving = false,
                        formError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun deleteUser(id: String) {
        val users = _state.value.users
        if (users.deletingId != null) return

        _state.value = _state.value.copy(
            users = users.copy(deletingId = id, actionError = null, notice = null),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.deleteUser(id)
                val current = _state.value.users
                val nextPage = if (current.items.size <= 1 && current.page > 0) {
                    current.page - 1
                } else {
                    current.page
                }
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        deletingId = null,
                        page = nextPage,
                        notice = "İstifadəçi silindi.",
                    ),
                )
                loadUsers(silent = true)
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    users = _state.value.users.copy(
                        deletingId = null,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun consumeUsersFormDone() {
        _state.value = _state.value.copy(
            users = _state.value.users.copy(formDone = false, formError = null),
        )
    }

    fun clearUsersFormError() {
        _state.value = _state.value.copy(
            users = _state.value.users.copy(formError = null),
        )
    }

    fun clearUsersFeedback() {
        _state.value = _state.value.copy(
            users = _state.value.users.copy(actionError = null, notice = null),
        )
    }

    // ---- İstifadəçinin rolları ----------------------------------------------

    fun openUserRoles(userId: String) {
        _state.value = _state.value.copy(
            userRoles = UserRolesEditorState(userId = userId, loading = true),
        )

        viewModelScope.launch {
            try {
                val allDeferred = async { IdentityRepository.listRoles() }
                val directDeferred = async { IdentityRepository.userRoles(userId) }
                val allRoles = allDeferred.await()
                val names = directDeferred.await().directRoles.orEmpty()
                    .map { it.name }
                    .toSet()

                if (_state.value.userRoles.userId != userId) return@launch
                _state.value = _state.value.copy(
                    userRoles = UserRolesEditorState(
                        userId = userId,
                        loading = false,
                        allRoles = allRoles,
                        originalNames = names,
                        selectedNames = names,
                    ),
                )
            } catch (failure: Throwable) {
                if (_state.value.userRoles.userId != userId) return@launch
                _state.value = _state.value.copy(
                    userRoles = _state.value.userRoles.copy(
                        loading = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun toggleUserRole(name: String) {
        val editor = _state.value.userRoles
        if (editor.saving) return
        val next = if (name in editor.selectedNames) {
            editor.selectedNames - name
        } else {
            editor.selectedNames + name
        }
        _state.value = _state.value.copy(
            userRoles = editor.copy(selectedNames = next),
        )
    }

    fun saveUserRoles() {
        val editor = _state.value.userRoles
        val userId = editor.userId ?: return
        if (editor.saving) return

        val toAdd = editor.selectedNames - editor.originalNames
        val toRemove = editor.originalNames - editor.selectedNames

        _state.value = _state.value.copy(
            userRoles = editor.copy(saving = true, error = null),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.assignRolesToUser(userId, toAdd.toList())
                IdentityRepository.removeRolesFromUser(userId, toRemove.toList())
                _state.value = _state.value.copy(
                    userRoles = _state.value.userRoles.copy(saving = false, done = true),
                    users = _state.value.users.copy(notice = "Rollar yeniləndi."),
                )
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    userRoles = _state.value.userRoles.copy(
                        saving = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun closeUserRoles() {
        _state.value = _state.value.copy(userRoles = UserRolesEditorState())
    }

    // ---- Rollar --------------------------------------------------------------

    fun ensureRoles() {
        val roles = _state.value.roles
        if (!roles.loaded && !roles.loading) loadRoles()
    }

    fun refreshRoles() = loadRoles()

    fun setRoleSearch(value: String) {
        _state.value = _state.value.copy(
            roles = _state.value.roles.copy(search = value),
        )
    }

    private fun loadRoles() {
        val request = ++rolesRequest
        _state.value = _state.value.copy(
            roles = _state.value.roles.copy(loading = true, error = null),
        )

        viewModelScope.launch {
            try {
                val items = IdentityRepository.listRoles()
                if (request != rolesRequest) return@launch
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        items = items,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != rolesRequest) return@launch
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        loading = false,
                        loaded = true,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun createRole(name: String, description: String) {
        val roles = _state.value.roles
        if (roles.formSaving) return

        _state.value = _state.value.copy(
            roles = roles.copy(formSaving = true, formError = null, formDone = false),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.createRole(name, description)
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        formSaving = false,
                        formError = null,
                        formDone = true,
                        notice = "Rol yaradıldı.",
                    ),
                )
                loadRoles()
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        formSaving = false,
                        formError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun deleteRole(name: String) {
        val roles = _state.value.roles
        if (roles.deletingName != null) return

        _state.value = _state.value.copy(
            roles = roles.copy(deletingName = name, actionError = null, notice = null),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.deleteRole(name)
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        deletingName = null,
                        notice = "Rol silindi.",
                    ),
                )
                loadRoles()
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    roles = _state.value.roles.copy(
                        deletingName = null,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun consumeRolesFormDone() {
        _state.value = _state.value.copy(
            roles = _state.value.roles.copy(formDone = false, formError = null),
        )
    }

    fun clearRolesFormError() {
        _state.value = _state.value.copy(
            roles = _state.value.roles.copy(formError = null),
        )
    }

    fun clearRolesFeedback() {
        _state.value = _state.value.copy(
            roles = _state.value.roles.copy(actionError = null, notice = null),
        )
    }

    // ---- Qruplar -------------------------------------------------------------

    fun ensureGroups() {
        val groups = _state.value.groups
        if (!groups.loaded && !groups.loading) loadGroups()
    }

    fun refreshGroups() = loadGroups()

    fun setGroupSearch(value: String) {
        _state.value = _state.value.copy(
            groups = _state.value.groups.copy(search = value),
        )
    }

    private fun loadGroups() {
        val request = ++groupsRequest
        _state.value = _state.value.copy(
            groups = _state.value.groups.copy(loading = true, error = null),
        )

        viewModelScope.launch {
            try {
                val items = IdentityRepository.listGroups()
                if (request != groupsRequest) return@launch
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        loading = false,
                        loaded = true,
                        error = null,
                        items = items,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != groupsRequest) return@launch
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        loading = false,
                        loaded = true,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun createGroup(name: String, description: String) {
        val groups = _state.value.groups
        if (groups.formSaving) return

        _state.value = _state.value.copy(
            groups = groups.copy(formSaving = true, formError = null, formDone = false),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.createGroup(name, description)
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        formSaving = false,
                        formError = null,
                        formDone = true,
                        notice = "Qrup yaradıldı.",
                    ),
                )
                loadGroups()
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        formSaving = false,
                        formError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun deleteGroup(id: String) {
        val groups = _state.value.groups
        if (groups.deletingId != null) return

        _state.value = _state.value.copy(
            groups = groups.copy(deletingId = id, actionError = null, notice = null),
        )

        viewModelScope.launch {
            try {
                IdentityRepository.deleteGroup(id)
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        deletingId = null,
                        notice = "Qrup silindi.",
                    ),
                )
                loadGroups()
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(
                    groups = _state.value.groups.copy(
                        deletingId = null,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun consumeGroupsFormDone() {
        _state.value = _state.value.copy(
            groups = _state.value.groups.copy(formDone = false, formError = null),
        )
    }

    fun clearGroupsFormError() {
        _state.value = _state.value.copy(
            groups = _state.value.groups.copy(formError = null),
        )
    }

    fun clearGroupsFeedback() {
        _state.value = _state.value.copy(
            groups = _state.value.groups.copy(actionError = null, notice = null),
        )
    }

    // ---- Qrup detalı ---------------------------------------------------------

    fun openGroup(group: UsrGroup) {
        val request = ++groupDetailRequest
        _state.value = _state.value.copy(
            groupDetail = GroupDetailState(group = group, loading = true),
        )

        viewModelScope.launch {
            try {
                val bundle = IdentityRepository.groupBundle(group.id)
                if (request != groupDetailRequest) return@launch
                _state.value = _state.value.copy(
                    groupDetail = _state.value.groupDetail.copy(
                        loading = false,
                        error = null,
                        roles = bundle.roles,
                        members = bundle.members,
                        availableRoleNames = bundle.availableRoleNames,
                        allUsers = bundle.users,
                    ),
                )
            } catch (failure: Throwable) {
                if (request != groupDetailRequest) return@launch
                _state.value = _state.value.copy(
                    groupDetail = _state.value.groupDetail.copy(
                        loading = false,
                        error = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun refreshGroupDetail() {
        val group = _state.value.groupDetail.group ?: return
        openGroup(group)
    }

    fun addGroupRole(name: String) {
        val group = _state.value.groupDetail.group ?: return
        if (name.isBlank() || _state.value.groupDetail.busy) return
        runGroupAction(group.id, "Rol qrupa əlavə edildi.") {
            IdentityRepository.assignRolesToGroup(group.id, listOf(name))
        }
    }

    fun removeGroupRole(name: String) {
        val group = _state.value.groupDetail.group ?: return
        if (_state.value.groupDetail.busy) return
        runGroupAction(group.id, "Rol qrupdan çıxarıldı.") {
            IdentityRepository.removeRolesFromGroup(group.id, listOf(name))
        }
    }

    fun addGroupMember(userId: String) {
        val group = _state.value.groupDetail.group ?: return
        if (userId.isBlank() || _state.value.groupDetail.busy) return
        runGroupAction(group.id, "İstifadəçi qrupa əlavə edildi.") {
            IdentityRepository.addUserToGroup(group.id, userId)
        }
    }

    fun removeGroupMember(userId: String) {
        val group = _state.value.groupDetail.group ?: return
        if (_state.value.groupDetail.busy) return
        runGroupAction(group.id, "İstifadəçi qrupdan çıxarıldı.") {
            IdentityRepository.removeUserFromGroup(group.id, userId)
        }
    }

    private fun runGroupAction(
        groupId: String,
        successMessage: String,
        action: suspend () -> Unit,
    ) {
        _state.value = _state.value.copy(
            groupDetail = _state.value.groupDetail.copy(
                busy = true,
                actionError = null,
                notice = null,
            ),
        )

        viewModelScope.launch {
            try {
                action()
                val bundle = IdentityRepository.groupBundle(groupId)
                if (_state.value.groupDetail.group?.id != groupId) return@launch
                _state.value = _state.value.copy(
                    groupDetail = _state.value.groupDetail.copy(
                        roles = bundle.roles,
                        members = bundle.members,
                        availableRoleNames = bundle.availableRoleNames,
                        allUsers = bundle.users,
                        busy = false,
                        actionError = null,
                        notice = successMessage,
                    ),
                )
            } catch (failure: Throwable) {
                if (_state.value.groupDetail.group?.id != groupId) return@launch
                _state.value = _state.value.copy(
                    groupDetail = _state.value.groupDetail.copy(
                        busy = false,
                        actionError = failure.toUserMessage(),
                    ),
                )
            }
        }
    }

    fun clearGroupDetailFeedback() {
        _state.value = _state.value.copy(
            groupDetail = _state.value.groupDetail.copy(
                actionError = null,
                notice = null,
            ),
        )
    }

    fun closeGroup() {
        groupDetailRequest++
        _state.value = _state.value.copy(groupDetail = GroupDetailState())
    }
}
