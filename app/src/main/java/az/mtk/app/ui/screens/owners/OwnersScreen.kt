package az.mtk.app.ui.screens.owners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.CreatePassiveOwnerRequest
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.buildings.canWriteBuildings
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OwnersViewModel : ViewModel() {
    val pager = Pager<OwnerItemDto>(viewModelScope) { page, query ->
        AppGraph.repository.owners(page, search = query)
    }

    fun createPassiveOwner(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        notes: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                AppGraph.service(BuildingsApi::class.java).createPassiveOwner(
                    CreatePassiveOwnerRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        phoneNumber = phoneNumber.trim(),
                        email = email.trim(),
                        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
                .onSuccess {
                    pager.refresh()
                    onDone()
                }
                .onFailure { onError(it.toUserMessage()) }
        }
    }
}

@Composable
fun OwnersScreen(
    onBack: (() -> Unit)?,
    onOpenOwner: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: OwnersViewModel = mtkViewModel { OwnersViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    val canWrite = remember { canWriteBuildings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Sahiblər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} sahib",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Ad, e-poçt və ya telefon",
            )
            if (canWrite) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "Passive sahib yarat", onClick = { showCreate = true })
            }
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Sahiblər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Sahib tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { owner ->
                    MtkCard(onClick = { onOpenOwner(owner.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(Format.initials(owner.fullName))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        owner.fullName ?: "—",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (owner.isActive == false) {
                                        Spacer(Modifier.width(6.dp))
                                        Pill("Qeyri-aktiv", Tone.Neutral)
                                    }
                                }
                                if (!owner.email.isNullOrBlank()) {
                                    Text(
                                        owner.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!owner.phoneNumber.isNullOrBlank()) {
                                    Text(owner.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MtkColors.MutedSoft)
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MtkColors.MutedUltraSoft,
                                modifier = Modifier.size(20.dp),
                            )
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
        CreatePassiveOwnerModal(
            onDismiss = { showCreate = false },
            onCreate = { firstName, lastName, phone, email, notes, onDone, onError ->
                viewModel.createPassiveOwner(firstName, lastName, phone, email, notes, onDone, onError)
            },
        )
    }
}

/** "Passive sahib yarat" modalı — CreatePassiveOwnerRequest sahələri üzrə. */
@Composable
private fun CreatePassiveOwnerModal(
    onDismiss: () -> Unit,
    onCreate: (
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        notes: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = "Passive sahib yarat",
        subtitle = "İstifadəçi hesabı olmayan sahibin qeydiyyatı",
        onDismiss = onDismiss,
        confirmLabel = "Yarat",
        confirmEnabled = firstName.isNotBlank() && lastName.isNotBlank() &&
            phone.isNotBlank() && email.isNotBlank() && !submitting,
        confirmLoading = submitting,
        onConfirm = {
            submitting = true
            error = null
            onCreate(
                firstName,
                lastName,
                phone,
                email,
                notes,
                { onDismiss() },
                {
                    submitting = false
                    error = it
                },
            )
        },
    ) {
        MtkTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = "Ad",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = "Soyad",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Telefon",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-poçt",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Qeyd",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(12.dp))
    }
}
