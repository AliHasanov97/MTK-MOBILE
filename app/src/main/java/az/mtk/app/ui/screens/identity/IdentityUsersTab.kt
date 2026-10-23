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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.UsrUserSummary
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsFormActions
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPagination
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTextField

@Composable
internal fun IdentityUsersTab(
    state: IdentityUsersState,
    rolesEditor: UserRolesEditorState,
    viewModel: IdentityViewModel,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UsrUserSummary?>(null) }
    var selectedUser by remember { mutableStateOf<UsrUserSummary?>(null) }
    var rolesUser by remember { mutableStateOf<UsrUserSummary?>(null) }
    var deletingUser by remember { mutableStateOf<UsrUserSummary?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.deletingId) {
        if (pendingDeleteId != null && state.deletingId == null) {
            deletingUser = null
            pendingDeleteId = null
        }
    }

    LaunchedEffect(state.formDone) {
        if (state.formDone) {
            showCreate = false
            editingUser = null
            viewModel.consumeUsersFormDone()
        }
    }

    LaunchedEffect(rolesEditor.done) {
        if (rolesEditor.done) {
            rolesUser = null
            viewModel.closeUserRoles()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            IdentitySummaryCard(
                title = "İstifadəçilər",
                subtitle = "Sistemə girişi olan şəxslər",
                count = state.totalCount,
                countLabel = "istifadəçi",
                extra = "Səhifə ${state.page + 1}/${state.pageCount}",
            )
        }

        item {
            IdentityEmptyActionButton("+ Yeni istifadəçi") { showCreate = true }
        }

        item {
            OpsSearchField(
                value = state.search,
                onValueChange = viewModel::setUserSearch,
                placeholder = "Ad, email və ya telefon üzrə…",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.actionError != null) {
            item {
                OpsFeedback(
                    message = state.actionError,
                    isError = true,
                    onDismiss = viewModel::clearUsersFeedback,
                )
            }
        }

        if (state.notice != null) {
            item {
                OpsFeedback(
                    message = state.notice,
                    isError = false,
                    onDismiss = viewModel::clearUsersFeedback,
                )
            }
        }

        when {
            state.loading && state.items.isEmpty() -> item { OpsLoading() }

            state.error != null && state.items.isEmpty() -> item {
                OpsError(state.error, onRetry = viewModel::refreshUsers)
            }

            state.items.isEmpty() -> item {
                OpsEmpty(
                    title = if (state.search.isNotBlank()) {
                        "İstifadəçi tapılmadı"
                    } else {
                        "Hələ istifadəçi yoxdur"
                    },
                    text = if (state.search.isNotBlank()) {
                        "Axtarış sözünü dəyişin."
                    } else {
                        "Yeni istifadəçi əlavə edin."
                    },
                )
            }

            else -> items(state.items, key = { it.id }) { user ->
                UserCard(user = user, onClick = { selectedUser = user })
            }
        }

        if (state.totalCount > state.pageSize) {
            item {
                OpsPagination(
                    page = state.page,
                    total = state.totalCount,
                    pageSize = state.pageSize,
                    onPrev = viewModel::prevUsersPage,
                    onNext = viewModel::nextUsersPage,
                )
            }
        }
    }

    if (showCreate) {
        UserFormSheet(
            mode = UserFormMode.CREATE,
            user = null,
            saving = state.formSaving,
            error = state.formError,
            onDismiss = { showCreate = false },
            onClearError = viewModel::clearUsersFormError,
            onSubmit = { email, firstName, lastName, phone, password ->
                viewModel.createUser(email, firstName, lastName, password, phone)
            },
        )
    }

    editingUser?.let { user ->
        UserFormSheet(
            mode = UserFormMode.EDIT,
            user = user,
            saving = state.formSaving,
            error = state.formError,
            onDismiss = { editingUser = null },
            onClearError = viewModel::clearUsersFormError,
            onSubmit = { _, firstName, lastName, phone, _ ->
                viewModel.updateUser(user.id, firstName, lastName, phone)
            },
        )
    }

    selectedUser?.let { user ->
        UserDetailSheet(
            user = user,
            onClose = { selectedUser = null },
            onRoles = {
                selectedUser = null
                rolesUser = user
                viewModel.openUserRoles(user.id)
            },
            onEdit = {
                selectedUser = null
                viewModel.clearUsersFormError()
                editingUser = user
            },
            onDelete = {
                selectedUser = null
                deletingUser = user
            },
        )
    }

    rolesUser?.let { user ->
        UserRolesSheet(
            user = user,
            editor = rolesEditor,
            onToggle = viewModel::toggleUserRole,
            onSave = viewModel::saveUserRoles,
            onDismiss = {
                rolesUser = null
                viewModel.closeUserRoles()
            },
        )
    }

    deletingUser?.let { user ->
        IdentityConfirmSheet(
            title = "İstifadəçini sil",
            message = "«${identityUserName(user.firstName, user.lastName, user.email)}» istifadəçisini silmək istədiyinizə əminsiniz?",
            confirmLabel = "Sil",
            busy = state.deletingId == user.id,
            onConfirm = {
                pendingDeleteId = user.id
                viewModel.deleteUser(user.id)
            },
            onDismiss = {
                deletingUser = null
                pendingDeleteId = null
            },
        )
    }
}

@Composable
private fun UserCard(user: UsrUserSummary, onClick: () -> Unit) {
    val name = identityUserName(user.firstName, user.lastName, user.email)

    OpsCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IdentityAvatar(name)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.email?.takeIf { it.isNotBlank() } ?: "—",
                    fontSize = 11.sp,
                    color = OpsColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!user.phoneNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = user.phoneNumber,
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                UserStatusChip(user.status)
                Spacer(Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OpsColors.MutedSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UserDetailSheet(
    user: UsrUserSummary,
    onClose: () -> Unit,
    onRoles: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = identityUserName(user.firstName, user.lastName, user.email)

    AppSheet(
        onDismissRequest = onClose,
        containerColor = OpsColors.Background,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IdentityAvatar(name)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                UserStatusChip(user.status)
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(4.dp))

        OpsInfoRow("Email", user.email?.takeIf { it.isNotBlank() } ?: "—")
        OpsInfoRow("Telefon", user.phoneNumber?.takeIf { it.isNotBlank() } ?: "—")

        Spacer(Modifier.height(14.dp))

        OpsPrimaryButton(
            text = "Rolları idarə et",
            onClick = onRoles,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Redaktə et",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            OpsSecondaryButton(
                text = "Sil",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                danger = true,
            )
        }

        Spacer(Modifier.height(8.dp))

        OpsSecondaryButton(
            text = "Bağla",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private enum class UserFormMode { CREATE, EDIT }

@Composable
private fun UserFormSheet(
    mode: UserFormMode,
    user: UsrUserSummary?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSubmit: (email: String, firstName: String, lastName: String, phone: String, password: String) -> Unit,
) {
    var email by remember { mutableStateOf(user?.email.orEmpty()) }
    var firstName by remember { mutableStateOf(user?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(user?.lastName.orEmpty()) }
    var phone by remember { mutableStateOf(user?.phoneNumber.orEmpty()) }
    var password by remember { mutableStateOf("") }

    val isCreate = mode == UserFormMode.CREATE
    val canSubmit = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        (!isCreate || (email.isNotBlank() && password.isNotBlank()))

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
        modifier = Modifier.imePadding(),
    ) {
        Text(
            text = if (isCreate) "Yeni istifadəçi" else "İstifadəçini redaktə et",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = if (isCreate) {
                "Sistemə giriş üçün hesab yaradın."
            } else {
                "Ad, soyad və əlaqə məlumatlarını yeniləyin."
            },
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(14.dp))

        if (isCreate) {
            OpsTextField(
                value = email,
                onValueChange = {
                    email = it
                    onClearError()
                },
                label = "Email",
                keyboardType = KeyboardType.Email,
                placeholder = "ad@example.com",
            )
            Spacer(Modifier.height(10.dp))
        } else {
            OpsInfoRow("Email", user?.email?.takeIf { it.isNotBlank() } ?: "—")
            Spacer(Modifier.height(10.dp))
        }

        OpsTextField(
            value = firstName,
            onValueChange = {
                firstName = it
                onClearError()
            },
            label = "Ad",
        )

        Spacer(Modifier.height(10.dp))

        OpsTextField(
            value = lastName,
            onValueChange = {
                lastName = it
                onClearError()
            },
            label = "Soyad",
        )

        Spacer(Modifier.height(10.dp))

        OpsTextField(
            value = phone,
            onValueChange = {
                phone = it
                onClearError()
            },
            label = "Telefon",
            keyboardType = KeyboardType.Phone,
            placeholder = "+994…",
        )

        if (isCreate) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onClearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Şifrə", fontSize = 12.sp) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OpsColors.Terracotta,
                    unfocusedBorderColor = OpsColors.Border,
                    focusedContainerColor = OpsColors.Surface,
                    unfocusedContainerColor = OpsColors.Surface,
                ),
            )
        }

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            IdentityInlineError(error)
        }

        Spacer(Modifier.height(14.dp))

        OpsFormActions(
            saving = saving,
            submitLabel = "Saxla",
            onCancel = onDismiss,
            onSubmit = { onSubmit(email, firstName, lastName, phone, password) },
            enabled = canSubmit,
        )
    }
}

@Composable
private fun UserRolesSheet(
    user: UsrUserSummary,
    editor: UserRolesEditorState,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = identityUserName(user.firstName, user.lastName, user.email)

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = "$name — rollar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "İstifadəçiyə birbaşa təyin edilmiş rollar.",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(12.dp))

        when {
            editor.loading -> OpsLoading()

            editor.allRoles.isEmpty() && editor.error != null -> IdentityInlineError(editor.error)

            editor.allRoles.isEmpty() -> Text(
                text = "Rol tapılmadı.",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )

            else -> {
                editor.allRoles.forEachIndexed { index, role ->
                    IdentitySelectableRow(
                        label = role.name,
                        supporting = role.description?.takeIf { it.isNotBlank() },
                        selected = role.name in editor.selectedNames,
                        onClick = { onToggle(role.name) },
                    )
                    if (index != editor.allRoles.lastIndex) {
                        HorizontalDivider(color = OpsColors.Border)
                    }
                }

                if (editor.error != null) {
                    Spacer(Modifier.height(10.dp))
                    IdentityInlineError(editor.error)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Ləğv et",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !editor.saving,
            )
            OpsPrimaryButton(
                text = if (editor.saving) "Saxlanılır…" else "Saxla",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !editor.loading && editor.error == null && !editor.saving,
            )
        }
    }
}
