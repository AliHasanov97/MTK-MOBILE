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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import az.mtk.app.data.api.UsrGroup
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsFormActions
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSearchField
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsSelectField
import az.mtk.app.ui.screens.operations.OpsTextField

@Composable
internal fun IdentityGroupsTab(
    state: IdentityGroupsState,
    detail: GroupDetailState,
    viewModel: IdentityViewModel,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }
    var openedGroup by remember { mutableStateOf<UsrGroup?>(null) }
    var deletingGroup by remember { mutableStateOf<UsrGroup?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.deletingId) {
        if (pendingDeleteId != null && state.deletingId == null) {
            deletingGroup = null
            pendingDeleteId = null
        }
    }

    LaunchedEffect(state.formDone) {
        if (state.formDone) {
            showCreate = false
            viewModel.consumeGroupsFormDone()
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
                title = "Qruplar",
                subtitle = "Rol və istifadəçi qrupları",
                count = state.items.size,
                countLabel = "qrup",
                extra = if (state.search.isNotBlank()) {
                    "Axtarış: ${visible.size} nəticə"
                } else {
                    null
                },
            )
        }

        item {
            IdentityEmptyActionButton("+ Yeni qrup") { showCreate = true }
        }

        item {
            OpsSearchField(
                value = state.search,
                onValueChange = viewModel::setGroupSearch,
                placeholder = "Qrup adı və ya təsviri üzrə…",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.actionError != null) {
            item {
                OpsFeedback(
                    message = state.actionError,
                    isError = true,
                    onDismiss = viewModel::clearGroupsFeedback,
                )
            }
        }

        if (state.notice != null) {
            item {
                OpsFeedback(
                    message = state.notice,
                    isError = false,
                    onDismiss = viewModel::clearGroupsFeedback,
                )
            }
        }

        when {
            state.loading && state.items.isEmpty() -> item { OpsLoading() }

            state.error != null && state.items.isEmpty() -> item {
                OpsError(state.error, onRetry = viewModel::refreshGroups)
            }

            visible.isEmpty() -> item {
                OpsEmpty(
                    title = if (state.search.isNotBlank()) "Qrup tapılmadı" else "Hələ qrup yoxdur",
                    text = if (state.search.isNotBlank()) {
                        "Axtarış sözünü dəyişin."
                    } else {
                        "Yeni qrup yaradın."
                    },
                )
            }

            else -> items(visible, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    onClick = {
                        openedGroup = group
                        viewModel.openGroup(group)
                    },
                )
            }
        }
    }

    if (showCreate) {
        GroupFormSheet(
            saving = state.formSaving,
            error = state.formError,
            onDismiss = { showCreate = false },
            onClearError = viewModel::clearGroupsFormError,
            onSubmit = viewModel::createGroup,
        )
    }

    openedGroup?.let { group ->
        GroupDetailSheet(
            group = detail.group ?: group,
            detail = detail,
            onDismiss = {
                openedGroup = null
                viewModel.closeGroup()
            },
            onRefresh = viewModel::refreshGroupDetail,
            onAddRole = viewModel::addGroupRole,
            onRemoveRole = viewModel::removeGroupRole,
            onAddMember = viewModel::addGroupMember,
            onRemoveMember = viewModel::removeGroupMember,
            onClearFeedback = viewModel::clearGroupDetailFeedback,
            onDelete = {
                openedGroup = null
                viewModel.closeGroup()
                deletingGroup = group
            },
        )
    }

    deletingGroup?.let { group ->
        IdentityConfirmSheet(
            title = "Qrupu sil",
            message = "«${group.name}» qrupunu silmək istədiyinizə əminsiniz?",
            confirmLabel = "Sil",
            busy = state.deletingId == group.id,
            onConfirm = {
                pendingDeleteId = group.id
                viewModel.deleteGroup(group.id)
            },
            onDismiss = {
                deletingGroup = null
                pendingDeleteId = null
            },
        )
    }
}

@Composable
private fun GroupCard(group: UsrGroup, onClick: () -> Unit) {
    OpsCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.description?.takeIf { it.isNotBlank() } ?: "Təsvir yoxdur",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (group.parentId != null) {
                    Spacer(Modifier.height(6.dp))
                    IdentityTagChip("Alt qrup")
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OpsColors.MutedSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GroupFormSheet(
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
            text = "Yeni qrup",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = "Qruplar istifadəçiləri və rolları birləşdirir.",
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
            placeholder = "məs: mühasibat",
        )

        Spacer(Modifier.height(10.dp))

        OpsTextField(
            value = description,
            onValueChange = {
                description = it
                onClearError()
            },
            label = "Təsvir",
            placeholder = "Qrupun təyinatı (istəyə bağlı)",
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

@Composable
private fun GroupDetailSheet(
    group: UsrGroup,
    detail: GroupDetailState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onAddRole: (String) -> Unit,
    onRemoveRole: (String) -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onClearFeedback: () -> Unit,
    onDelete: () -> Unit,
) {
    val availableRoles = remember(detail.roles, detail.availableRoleNames) {
        detail.availableRoleNames
            .filter { name -> detail.roles.none { it.name == name } }
            .sorted()
    }
    val availableMembers = remember(detail.members, detail.allUsers) {
        detail.allUsers.filter { user -> detail.members.none { it.id == user.id } }
    }

    var roleToAdd by remember(group.id, detail.roles) { mutableStateOf<String?>(null) }
    var memberToAdd by remember(group.id, detail.members) { mutableStateOf<String?>(null) }

    AppSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = group.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = group.description?.takeIf { it.isNotBlank() } ?: "Təsvir yoxdur",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        if (detail.actionError != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    IdentityInlineError(detail.actionError)
                }
                TextButton(onClick = onClearFeedback) {
                    Text("Bağla", fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
        }

        if (detail.notice != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = detail.notice,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = OpsColors.Olive,
                )
                TextButton(onClick = onClearFeedback) {
                    Text("Bağla", fontSize = 11.sp, color = OpsColors.Muted)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            detail.loading && detail.roles.isEmpty() && detail.members.isEmpty() -> OpsLoading()

            detail.error != null && detail.roles.isEmpty() && detail.members.isEmpty() -> {
                IdentityInlineError(detail.error)
                Spacer(Modifier.height(8.dp))
                OpsSecondaryButton(
                    text = "Yenidən cəhd et",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                GroupSectionHeader(title = "Rollar", count = detail.roles.size)
                Spacer(Modifier.height(4.dp))

                if (detail.roles.isEmpty()) {
                    Text(
                        text = "Rol təyin edilməyib.",
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                } else {
                    detail.roles.forEachIndexed { index, role ->
                        GroupManagedRow(
                            title = role.name,
                            supporting = role.description?.takeIf { it.isNotBlank() }
                                ?: roleTypeLabel(role.roleType),
                            actionLabel = "Çıxar",
                            enabled = !detail.busy,
                            onAction = { onRemoveRole(role.name) },
                        )
                        if (index != detail.roles.lastIndex) {
                            HorizontalDivider(color = OpsColors.Border)
                        }
                    }
                }

                if (availableRoles.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    OpsSelectField(
                        label = "Rol əlavə et",
                        options = availableRoles.map { it to it },
                        selectedId = roleToAdd,
                        onSelect = { roleToAdd = it },
                        placeholder = "Rol seçin…",
                        enabled = !detail.busy,
                    )
                    Spacer(Modifier.height(8.dp))
                    OpsSecondaryButton(
                        text = if (detail.busy) "Gözləyin…" else "Rolu əlavə et",
                        onClick = {
                            roleToAdd?.let(onAddRole)
                            roleToAdd = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = roleToAdd != null && !detail.busy,
                    )
                }

                Spacer(Modifier.height(18.dp))
                GroupSectionHeader(title = "Üzvlər", count = detail.members.size)
                Spacer(Modifier.height(4.dp))

                if (detail.members.isEmpty()) {
                    Text(
                        text = "Üzv yoxdur.",
                        fontSize = 11.sp,
                        color = OpsColors.MutedSoft,
                    )
                } else {
                    detail.members.forEachIndexed { index, member ->
                        GroupManagedRow(
                            title = identityUserName(member.firstName, member.lastName, member.email),
                            supporting = member.email?.takeIf { it.isNotBlank() }
                                ?: member.username?.takeIf { it.isNotBlank() },
                            actionLabel = "Çıxar",
                            enabled = !detail.busy,
                            onAction = { onRemoveMember(member.id) },
                        )
                        if (index != detail.members.lastIndex) {
                            HorizontalDivider(color = OpsColors.Border)
                        }
                    }
                }

                if (availableMembers.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    OpsSelectField(
                        label = "Üzv əlavə et",
                        options = availableMembers.map { user ->
                            user.id to identityUserName(user.firstName, user.lastName, user.email)
                        },
                        selectedId = memberToAdd,
                        onSelect = { memberToAdd = it },
                        placeholder = "İstifadəçi seçin…",
                        enabled = !detail.busy,
                    )
                    Spacer(Modifier.height(8.dp))
                    OpsSecondaryButton(
                        text = if (detail.busy) "Gözləyin…" else "Üzvü əlavə et",
                        onClick = {
                            memberToAdd?.let(onAddMember)
                            memberToAdd = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = memberToAdd != null && !detail.busy,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = OpsColors.Border)
        Spacer(Modifier.height(12.dp))

        OpsPrimaryButton(
            text = "Bağla",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        OpsSecondaryButton(
            text = "Qrupu sil",
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            enabled = !detail.busy,
            danger = true,
        )
    }
}

@Composable
private fun GroupSectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        OpsChip(
            text = count.toString(),
            foreground = OpsColors.Terracotta,
            background = OpsColors.Container,
        )
    }
}

@Composable
private fun GroupManagedRow(
    title: String,
    supporting: String?,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OpsColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(onClick = onAction, enabled = enabled) {
            Text(
                text = actionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Error,
            )
        }
    }
}
