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
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.data.api.UsrRole
import az.mtk.app.data.api.UsrRoleWriteRequest
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RolesViewModel : ViewModel() {
    private val api = AppGraph.service(IdentityApi::class.java)

    private val _roles = MutableStateFlow<List<UsrRole>>(emptyList())
    val roles = _roles.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { api.listRoles() }
                .onSuccess {
                    _roles.value = it
                    _error.value = null
                }
                .onFailure { _error.value = it.toUserMessage() }
            _loading.value = false
        }
    }

    fun create(name: String, description: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.createRole(UsrRoleWriteRequest(name = name.trim(), description = description.trim().takeIf { it.isNotEmpty() }))
            }.onSuccess {
                refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun update(oldName: String, name: String, description: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.updateRole(oldName, UsrRoleWriteRequest(name = name.trim(), description = description.trim().takeIf { it.isNotEmpty() }))
            }.onSuccess {
                refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun delete(roleName: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { api.deleteRole(roleName) }
                .onSuccess { refresh(); onDone(null) }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun RolesScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: RolesViewModel = mtkViewModel { RolesViewModel() },
) {
    val roles by viewModel.roles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UsrRole?>(null) }
    var deleting by remember { mutableStateOf<UsrRole?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    val visible = roles.filter { it.name.contains(filter.trim(), ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Rollar",
                subtitle = if (loading) "Yüklənir…" else "${roles.size} rol",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = "Rol axtar…",
            )
            Spacer(Modifier.height(10.dp))
            az.mtk.app.ui.components.SecondaryButton(
                text = "+ Yeni rol",
                onClick = { showCreate = true },
            )
        }

        when {
            loading && roles.isEmpty() -> LoadingState("Rollar yüklənir…")
            error != null && roles.isEmpty() -> ErrorState(error!!, onRetry = viewModel::refresh)
            visible.isEmpty() -> EmptyState("Rol tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visible, key = { it.id }) { role ->
                    MtkCard {
                        ListRow(
                            title = role.name,
                            subtitle = role.description,
                            meta = "Növ: ${role.roleType}",
                            trailing = { Pill(if (role.isActive) "Aktiv" else "Deaktiv", if (role.isActive) Tone.Success else Tone.Neutral) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = role }, content = { Text("Redaktə", fontSize = 12.sp) })
                            OutlinedButton(onClick = { deleting = role }, content = {
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
        RoleFormModal(
            title = "Yeni rol",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreate = false },
            onSave = { name, description, onDone -> viewModel.create(name, description, onDone) },
        )
    }

    editing?.let { role ->
        RoleFormModal(
            title = "Rolu redaktə et",
            initialName = role.name,
            initialDescription = role.description ?: "",
            onDismiss = { editing = null },
            onSave = { name, description, onDone -> viewModel.update(role.name, name, description, onDone) },
        )
    }

    deleting?.let { role ->
        ConfirmDialog(
            title = "Rolu sil",
            message = "${role.name} rolunu silmək istədiyinizə əminsiniz?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleting = null },
            onConfirm = { viewModel.delete(role.name) { deleting = null } },
        )
    }
}

@Composable
private fun RoleFormModal(
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
