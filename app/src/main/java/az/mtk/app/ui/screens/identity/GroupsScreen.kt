package az.mtk.app.ui.screens.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.data.api.UsrGroup
import az.mtk.app.data.api.UsrGroupMember
import az.mtk.app.data.api.UsrGroupRole
import az.mtk.app.data.api.UsrGroupRolesBody
import az.mtk.app.data.api.UsrGroupWriteRequest
import az.mtk.app.data.api.UsrRoleNamesBody
import az.mtk.app.data.api.UsrUserSummary
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel : ViewModel() {
    private val api = AppGraph.service(IdentityApi::class.java)

    private val _groups = MutableStateFlow<List<UsrGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { api.listGroups() }
                .onSuccess { _groups.value = it; _error.value = null }
                .onFailure { _error.value = it.toUserMessage() }
            _loading.value = false
        }
    }

    fun create(name: String, description: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.createGroup(UsrGroupWriteRequest(name = name.trim(), description = description.trim().takeIf { it.isNotEmpty() }))
            }.onSuccess { refresh(); onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun update(id: String, name: String, description: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.updateGroup(id, UsrGroupWriteRequest(name = name.trim(), description = description.trim().takeIf { it.isNotEmpty() }))
            }.onSuccess { refresh(); onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun delete(id: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.deleteGroup(id) }
                .onSuccess { refresh(); onDone(null) }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun loadDetail(groupId: String, onDone: (List<UsrGroupRole>, List<UsrGroupMember>, List<String>, List<UsrUserSummary>, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val roles = api.groupRoles(groupId)
                val members = api.groupUsers(groupId)
                val roleNames = runCatching { api.listAvailableRoleNames() }.getOrDefault(emptyList())
                val users = runCatching { api.listUsers() }.getOrDefault(emptyList())
                GroupDetail(roles, members, roleNames, users)
            }.onSuccess { onDone(it.roles, it.members, it.roleNames, it.users, null) }
                .onFailure { onDone(emptyList(), emptyList(), emptyList(), emptyList(), it.toUserMessage()) }
        }
    }

    private data class GroupDetail(
        val roles: List<UsrGroupRole>,
        val members: List<UsrGroupMember>,
        val roleNames: List<String>,
        val users: List<UsrUserSummary>,
    )

    fun addRole(groupId: String, roleName: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.assignRolesToGroup(UsrGroupRolesBody(groupId, listOf(roleName))) }
                .onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun removeRole(groupId: String, roleName: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.removeRolesFromGroup(groupId, UsrRoleNamesBody(listOf(roleName))) }
                .onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun addUser(groupId: String, userId: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.addUserToGroup(groupId, userId) }
                .onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun removeUser(groupId: String, userId: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.removeUserFromGroup(groupId, userId) }
                .onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun GroupsScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: GroupsViewModel = mtkViewModel { GroupsViewModel() },
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UsrGroup?>(null) }
    var detail by remember { mutableStateOf<UsrGroup?>(null) }
    var deleting by remember { mutableStateOf<UsrGroup?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Qruplar",
                subtitle = if (loading) "Yüklənir…" else "${groups.size} qrup",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            az.mtk.app.ui.components.SecondaryButton(
                text = "+ Yeni qrup",
                onClick = { showCreate = true },
            )
        }

        when {
            loading && groups.isEmpty() -> LoadingState("Qruplar yüklənir…")
            error != null && groups.isEmpty() -> ErrorState(error!!, onRetry = viewModel::refresh)
            groups.isEmpty() -> EmptyState("Qrup tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    MtkCard {
                        ListRow(
                            title = group.name,
                            subtitle = group.description ?: "—",
                            onClick = { detail = group },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { detail = group }, content = { Text("Ətraflı", fontSize = 12.sp) })
                            OutlinedButton(onClick = { editing = group }, content = { Text("Redaktə", fontSize = 12.sp) })
                            OutlinedButton(onClick = { deleting = group }, content = {
                                Text("Sil", fontSize = 12.sp, color = MtkColors.Negative)
                            })
                        }
                    }
                }
                if (error != null) {
                    item { ErrorState(error!!, onRetry = viewModel::refresh) }
                }
            }
        }
    }

    if (showCreate) {
        GroupFormModal(
            title = "Yeni qrup",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreate = false },
            onSave = { name, description, onDone -> viewModel.create(name, description, onDone) },
        )
    }

    editing?.let { group ->
        GroupFormModal(
            title = "Qrupu redaktə et",
            initialName = group.name,
            initialDescription = group.description ?: "",
            onDismiss = { editing = null },
            onSave = { name, description, onDone -> viewModel.update(group.id, name, description, onDone) },
        )
    }

    detail?.let { group ->
        GroupDetailModal(
            group = group,
            onDismiss = { detail = null },
            onLoadDetail = { onDone -> viewModel.loadDetail(group.id, onDone) },
            onAddRole = { roleName, onDone -> viewModel.addRole(group.id, roleName, onDone) },
            onRemoveRole = { roleName, onDone -> viewModel.removeRole(group.id, roleName, onDone) },
            onAddUser = { userId, onDone -> viewModel.addUser(group.id, userId, onDone) },
            onRemoveUser = { userId, onDone -> viewModel.removeUser(group.id, userId, onDone) },
        )
    }

    deleting?.let { group ->
        ConfirmDialog(
            title = "Qrupu sil",
            message = "${group.name} qrupunu silmək istədiyinizə əminsiniz?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleting = null },
            onConfirm = { viewModel.delete(group.id) { deleting = null } },
        )
    }
}

@Composable
private fun GroupFormModal(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, onDone: (String?) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    MtkModal(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Saxla",
        confirmEnabled = name.isNotBlank() && !saving,
        onConfirm = {
            saving = true
            onSave(name, description) { message ->
                saving = false
                if (message == null) onDismiss() else error = message
            }
        },
    ) {
        MtkTextField(name, { name = it }, "Ad", required = true)
        Spacer(Modifier.height(12.dp))
        MtkTextField(description, { description = it }, "Təsvir")
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GroupDetailModal(
    group: UsrGroup,
    onDismiss: () -> Unit,
    onLoadDetail: (onDone: (List<UsrGroupRole>, List<UsrGroupMember>, List<String>, List<UsrUserSummary>, String?) -> Unit) -> Unit,
    onAddRole: (roleName: String, onDone: (String?) -> Unit) -> Unit,
    onRemoveRole: (roleName: String, onDone: (String?) -> Unit) -> Unit,
    onAddUser: (userId: String, onDone: (String?) -> Unit) -> Unit,
    onRemoveUser: (userId: String, onDone: (String?) -> Unit) -> Unit,
) {
    var roles by remember { mutableStateOf<List<UsrGroupRole>?>(null) }
    var members by remember { mutableStateOf<List<UsrGroupMember>?>(null) }
    var availableRoleNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<UsrUserSummary>>(emptyList()) }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<UsrUserSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        onLoadDetail { r, m, rn, u, e ->
            roles = r
            members = m
            availableRoleNames = rn
            allUsers = u
            error = e
        }
    }

    androidx.compose.runtime.LaunchedEffect(group.id) { reload() }

    MtkModal(
        title = group.name,
        subtitle = "Rollar və üzvlərin idarəsi",
        onDismiss = onDismiss,
    ) {
        if (error != null) {
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        SectionHeading(title = "Rollar", subtitle = null)
        Spacer(Modifier.height(6.dp))

        val roleList = roles
        if (roleList == null) {
            LoadingState("Yüklənir…")
        } else if (roleList.isEmpty()) {
            Text("Rol təyin edilməyib.", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
        } else {
            roleList.forEach { role ->
                ListRow(
                    title = role.name,
                    trailing = {
                        OutlinedButton(onClick = {
                            onRemoveRole(role.name) { message ->
                                if (message != null) error = message else reload()
                            }
                        }) { Text("Sil", fontSize = 12.sp, color = MtkColors.Negative) }
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        MtkSelect(
            value = selectedRole,
            onValueChange = { selectedRole = it },
            label = "Rol əlavə et",
            options = availableRoleNames
                .filter { name -> roles?.none { it.name == name } ?: true }
                .map { MtkOption(it, it) },
        )
        Spacer(Modifier.height(8.dp))
        az.mtk.app.ui.components.SecondaryButton(
            text = "Rolu əlavə et",
            enabled = selectedRole != null,
            onClick = {
                val role = selectedRole ?: return@SecondaryButton
                onAddRole(role) { message ->
                    if (message != null) error = message else { selectedRole = null; reload() }
                }
            },
        )

        Spacer(Modifier.height(18.dp))
        SectionHeading(title = "Üzvlər", subtitle = null)
        Spacer(Modifier.height(6.dp))

        val memberList = members
        if (memberList == null) {
            LoadingState("Yüklənir…")
        } else if (memberList.isEmpty()) {
            Text("Üzv yoxdur.", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
        } else {
            memberList.forEach { member ->
                ListRow(
                    title = "${member.firstName.orEmpty()} ${member.lastName.orEmpty()}".trim().ifBlank { member.username ?: "—" },
                    subtitle = member.email,
                    trailing = {
                        OutlinedButton(onClick = {
                            onRemoveUser(member.id) { message ->
                                if (message != null) error = message else reload()
                            }
                        }) { Text("Çıxar", fontSize = 12.sp, color = MtkColors.Negative) }
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SearchableSelect(
            label = "İstifadəçi əlavə et",
            selectedLabel = selectedUser?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() },
            searchPlaceholder = "İstifadəçi axtar…",
            onLoad = { query ->
                allUsers.filter { user ->
                    query.isBlank() ||
                        "${user.firstName.orEmpty()} ${user.lastName.orEmpty()} ${user.email.orEmpty()}"
                            .contains(query.trim(), ignoreCase = true)
                }
            },
            itemLabel = { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() },
            onSelect = { selectedUser = it },
            onClear = { selectedUser = null },
        )
        Spacer(Modifier.height(8.dp))
        az.mtk.app.ui.components.SecondaryButton(
            text = "Üzv əlavə et",
            enabled = selectedUser != null,
            onClick = {
                val user = selectedUser ?: return@SecondaryButton
                onAddUser(user.id) { message ->
                    if (message != null) error = message else { selectedUser = null; reload() }
                }
            },
        )
    }
}
