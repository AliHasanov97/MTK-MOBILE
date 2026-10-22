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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApiException
import az.mtk.app.data.api.UsrRegisterUserRequest
import az.mtk.app.data.api.UsrRole
import az.mtk.app.data.api.UsrUpdateUserRequest
import az.mtk.app.data.api.UsrUserRoles
import az.mtk.app.data.api.UsrUserSummary
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsersViewModel : ViewModel() {
    private val api = AppGraph.service(IdentityApi::class.java)

    val pager = Pager<UsrUserSummary>(viewModelScope) { page, query ->
        val result = api.searchUsers(
            az.mtk.app.data.api.UsrSearchUsersRequest(
                searchTerm = query.trim().takeIf { it.isNotEmpty() },
                pageNumber = page,
                pageSize = 20,
            ),
        )
        az.mtk.app.data.api.Page(result.users.orEmpty(), result.totalCount, result.pageNumber, result.pageSize)
    }

    fun register(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        password: String,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                api.registerUser(
                    UsrRegisterUserRequest(
                        email = email.trim(),
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        password = password,
                        phoneNumber = phone.trim().takeIf { it.isNotEmpty() },
                    ),
                )
            }.onSuccess {
                pager.refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun update(
        id: String,
        firstName: String,
        lastName: String,
        phone: String,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                api.updateUser(
                    id,
                    UsrUpdateUserRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        phoneNumber = phone.trim().takeIf { it.isNotEmpty() },
                    ),
                )
            }.onSuccess {
                pager.refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun delete(id: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.deleteUser(id) }
                .onSuccess { pager.refresh(); onDone(null) }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun loadRoles(userId: String, onDone: (List<UsrRole>, Set<String>, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val roles = api.listRoles()
                val userRoles: UsrUserRoles = api.userRoles(userId)
                roles to (userRoles.directRoles.orEmpty().map { it.name }.toSet())
            }.onSuccess { (roles, selected) -> onDone(roles, selected, null) }
                .onFailure { onDone(emptyList(), emptySet(), it.toUserMessage()) }
        }
    }

    fun saveRoles(
        userId: String,
        original: Set<String>,
        selected: Set<String>,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                val toAdd = selected - original
                val toRemove = original - selected
                if (toAdd.isNotEmpty()) api.assignRolesToUser(userId, az.mtk.app.data.api.UsrRoleNamesBody(toAdd.toList()))
                if (toRemove.isNotEmpty()) api.removeRolesFromUser(userId, az.mtk.app.data.api.UsrRoleNamesBody(toRemove.toList()))
            }.onSuccess { onDone(null) }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun UsersScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: UsersViewModel = mtkViewModel { UsersViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UsrUserSummary?>(null) }
    var rolesFor by remember { mutableStateOf<UsrUserSummary?>(null) }
    var deleting by remember { mutableStateOf<UsrUserSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "İstifadəçilər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} istifadəçi",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Axtar (ad, email, telefon)…",
            )
            Spacer(Modifier.height(10.dp))
            az.mtk.app.ui.components.SecondaryButton(
                text = "+ Yeni istifadəçi",
                onClick = { showCreate = true },
            )
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("İstifadəçilər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Nəticə tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { user ->
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim().ifBlank { "—" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    user.email ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!user.phoneNumber.isNullOrBlank()) {
                                    Text(
                                        user.phoneNumber,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MtkColors.MutedSoft,
                                    )
                                }
                            }
                            Pill(user.status ?: "—", Tone.Info)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { rolesFor = user }, content = { Text("Rollar", fontSize = 12.sp) })
                            OutlinedButton(onClick = { editing = user }, content = { Text("Redaktə", fontSize = 12.sp) })
                            OutlinedButton(onClick = { deleting = user }, content = {
                                Text("Sil", fontSize = 12.sp, color = MtkColors.Negative)
                            })
                        }
                    }
                }

                if (state.items.size < state.total) {
                    item {
                        OutlinedButton(
                            onClick = viewModel.pager::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = viewModel.pager::refresh) }
                }
            }
        }
    }

    if (showCreate) {
        UserFormModal(
            title = "Yeni istifadəçi",
            initialEmail = "",
            initialFirstName = "",
            initialLastName = "",
            initialPhone = "",
            showCredentials = true,
            onDismiss = { showCreate = false },
            onSave = { email, first, last, phone, password, onDone ->
                viewModel.register(email, first, last, phone, password, onDone)
            },
        )
    }

    editing?.let { user ->
        UserFormModal(
            title = "İstifadəçini redaktə et",
            initialEmail = user.email ?: "",
            initialFirstName = user.firstName ?: "",
            initialLastName = user.lastName ?: "",
            initialPhone = user.phoneNumber ?: "",
            showCredentials = false,
            onDismiss = { editing = null },
            onSave = { _, first, last, phone, _, onDone ->
                viewModel.update(user.id, first, last, phone, onDone)
            },
        )
    }

    rolesFor?.let { user ->
        UserRolesModal(
            user = user,
            onDismiss = { rolesFor = null },
            onLoad = { onDone -> viewModel.loadRoles(user.id, onDone) },
            onSave = { original, selected, onDone ->
                viewModel.saveRoles(user.id, original, selected, onDone)
            },
        )
    }

    deleting?.let { user ->
        ConfirmDialog(
            title = "İstifadəçini sil",
            message = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()} istifadəçisini silmək istədiyinizə əminsiniz?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.delete(user.id) { deleting = null }
            },
        )
    }
}

@Composable
private fun UserFormModal(
    title: String,
    initialEmail: String,
    initialFirstName: String,
    initialLastName: String,
    initialPhone: String,
    showCredentials: Boolean,
    onDismiss: () -> Unit,
    onSave: (email: String, firstName: String, lastName: String, phone: String, password: String, onDone: (String?) -> Unit) -> Unit,
) {
    var email by remember { mutableStateOf(initialEmail) }
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val valid = firstName.isNotBlank() && lastName.isNotBlank() &&
        (!showCredentials || (email.isNotBlank() && password.isNotBlank()))

    MtkModal(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Saxla",
        confirmEnabled = valid && !saving,
        onConfirm = {
            saving = true
            error = null
            onSave(email, firstName, lastName, phone, password) { message ->
                saving = false
                if (message == null) onDismiss() else error = message
            }
        },
    ) {
        if (showCredentials) {
            MtkTextField(email, { email = it }, "Email", required = true)
            Spacer(Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MtkTextField(firstName, { firstName = it }, "Ad", modifier = Modifier.weight(1f), required = true)
            MtkTextField(lastName, { lastName = it }, "Soyad", modifier = Modifier.weight(1f), required = true)
        }
        Spacer(Modifier.height(12.dp))
        MtkTextField(phone, { phone = it }, "Telefon")
        if (showCredentials) {
            Spacer(Modifier.height(12.dp))
            MtkTextField(password, { password = it }, "Şifrə", required = true)
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UserRolesModal(
    user: UsrUserSummary,
    onDismiss: () -> Unit,
    onLoad: (onDone: (List<UsrRole>, Set<String>, String?) -> Unit) -> Unit,
    onSave: (original: Set<String>, selected: Set<String>, onDone: (String?) -> Unit) -> Unit,
) {
    var roles by remember { mutableStateOf<List<UsrRole>?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var original by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(user.id) {
        onLoad { loadedRoles, loadedSelected, message ->
            roles = loadedRoles
            selected = loadedSelected
            original = loadedSelected
            error = message
        }
    }

    MtkModal(
        title = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()} — rollar",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Saxla",
        confirmEnabled = !saving,
        onConfirm = {
            saving = true
            onSave(original, selected) { message ->
                saving = false
                if (message == null) onDismiss() else error = message
            }
        },
    ) {
        val loaded = roles
        if (loaded == null && error == null) {
            LoadingState("Rollar yüklənir…")
        } else if (error != null) {
            Text(error!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
        } else {
            loaded?.forEach { role ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = role.name in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + role.name else selected - role.name
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MtkColors.Green),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(role.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
