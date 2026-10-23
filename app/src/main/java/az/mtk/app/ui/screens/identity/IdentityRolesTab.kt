package az.mtk.app.ui.screens.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import az.mtk.app.data.api.UsrRole
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsFormActions
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsTextField

@Composable
internal fun IdentityRolesTab(
    state: IdentityRolesState,
    viewModel: IdentityViewModel,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }
    var deletingRole by remember { mutableStateOf<UsrRole?>(null) }
    var pendingDeleteName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.deletingName) {
        if (pendingDeleteName != null && state.deletingName == null) {
            deletingRole = null
            pendingDeleteName = null
        }
    }

    LaunchedEffect(state.formDone) {
        if (state.formDone) {
            showCreate = false
            viewModel.consumeRolesFormDone()
        }
    }

    val visible = state.visibleItems

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            IdentitySummaryCard(
                title = "Rollar",
                subtitle = "İstifadəçi və qrup icazələri",
                count = state.items.size,
                countLabel = "rol",
                extra = if (state.search.isNotBlank()) {
                    "Axtarış: ${visible.size} nəticə"
                } else {
                    null
                },
            )
        }

        item {
            IdentityEmptyActionButton("+ Yeni rol") { showCreate = true }
        }

        item {
            OpsSearchField(
                value = state.search,
                onValueChange = viewModel::setRoleSearch,
                placeholder = "Rol adı və ya təsviri üzrə…",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.actionError != null) {
            item {
                OpsFeedback(
                    message = state.actionError,
                    isError = true,
                    onDismiss = viewModel::clearRolesFeedback,
                )
            }
        }

        if (state.notice != null) {
            item {
                OpsFeedback(
                    message = state.notice,
                    isError = false,
                    onDismiss = viewModel::clearRolesFeedback,
                )
            }
        }

        when {
            state.loading && state.items.isEmpty() -> item { OpsLoading() }

            state.error != null && state.items.isEmpty() -> item {
                OpsError(state.error, onRetry = viewModel::refreshRoles)
            }

            visible.isEmpty() -> item {
                OpsEmpty(
                    title = if (state.search.isNotBlank()) "Rol tapılmadı" else "Hələ rol yoxdur",
                    text = if (state.search.isNotBlank()) {
                        "Axtarış sözünü dəyişin."
                    } else {
                        "Yeni rol yaradın."
                    },
                )
            }

            else -> items(visible, key = { it.name }) { role ->
                RoleCard(
                    role = role,
                    onDelete = { deletingRole = role },
                )
            }
        }
    }

    if (showCreate) {
        RoleFormSheet(
            saving = state.formSaving,
            error = state.formError,
            onDismiss = { showCreate = false },
            onClearError = viewModel::clearRolesFormError,
            onSubmit = viewModel::createRole,
        )
    }

    deletingRole?.let { role ->
        IdentityConfirmSheet(
            title = "Rolu sil",
            message = "«${role.name}» rolunu silmək istədiyinizə əminsiniz? Rol istifadəçilərdən və qruplardan çıxarılacaq.",
            confirmLabel = "Sil",
            busy = state.deletingName == role.name,
            onConfirm = {
                pendingDeleteName = role.name
                viewModel.deleteRole(role.name)
            },
            onDismiss = {
                deletingRole = null
                pendingDeleteName = null
            },
        )
    }
}

@Composable
private fun RoleCard(role: UsrRole, onDelete: () -> Unit) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = role.description?.takeIf { it.isNotBlank() } ?: "Təsvir yoxdur",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IdentityTagChip(roleTypeLabel(role.roleType))
                    RoleStatusChip(role.isActive)
                }
            }
            TextButton(onClick = onDelete) {
                Text(
                    text = "Sil",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Error,
                )
            }
        }
    }
}

@Composable
private fun RoleFormSheet(
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
        modifier = Modifier.imePadding(),
    ) {
        Text(
            text = "Yeni rol",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "Rolun adı sistemdə unikal olmalıdır.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(14.dp))

        OpsTextField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            label = "Ad",
            placeholder = "məs: auditor",
        )

        Spacer(Modifier.height(10.dp))

        OpsTextField(
            value = description,
            onValueChange = {
                description = it
                onClearError()
            },
            label = "Təsvir",
            placeholder = "Rolun vəzifəsi (istəyə bağlı)",
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            IdentityInlineError(error)
        }

        Spacer(Modifier.height(14.dp))

        OpsFormActions(
            saving = saving,
            submitLabel = "Saxla",
            onCancel = onDismiss,
            onSubmit = { onSubmit(name, description) },
            enabled = name.isNotBlank(),
        )
    }
}
