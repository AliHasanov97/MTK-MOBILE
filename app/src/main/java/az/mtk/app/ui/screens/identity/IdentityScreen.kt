package az.mtk.app.ui.screens.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsTopBar

/**
 * İdentifikasiya modulu — istifadəçilər, rollar və qruplar.
 *
 * Saytdakı üç ayrı səhifə bir mobil axında birləşdirilib: sabit başlıq,
 * üçbölməli keçid və hər bölmə üçün kart siyahısı. Bütün əməliyyatlar
 * aşağıdan açılan panellərdə aparılır.
 */
@Composable
fun IdentityScreen(
    onBack: () -> Unit,
    viewModel: IdentityViewModel = mtkViewModel { IdentityViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(IdentityTab.USERS.name) }
    val tab = runCatching { IdentityTab.valueOf(tabName) }.getOrDefault(IdentityTab.USERS)

    LaunchedEffect(tab) {
        when (tab) {
            IdentityTab.USERS -> viewModel.ensureUsers()
            IdentityTab.ROLES -> viewModel.ensureRoles()
            IdentityTab.GROUPS -> viewModel.ensureGroups()
        }
    }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            Column(modifier = Modifier.background(OpsColors.Background)) {
                OpsTopBar(
                    title = "İdentifikasiya",
                    subtitle = "İstifadəçilər · Rollar · Qruplar",
                    onBack = onBack,
                    onRefresh = {
                        when (tab) {
                            IdentityTab.USERS -> viewModel.refreshUsers()
                            IdentityTab.ROLES -> viewModel.refreshRoles()
                            IdentityTab.GROUPS -> viewModel.refreshGroups()
                        }
                    },
                )
                IdentitySegmented(
                    selected = tab,
                    onSelect = { tabName = it.name },
                )
            }
        },
    ) { padding ->
        when (tab) {
            IdentityTab.USERS -> IdentityUsersTab(
                state = state.users,
                rolesEditor = state.userRoles,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )

            IdentityTab.ROLES -> IdentityRolesTab(
                state = state.roles,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )

            IdentityTab.GROUPS -> IdentityGroupsTab(
                state = state.groups,
                detail = state.groupDetail,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
