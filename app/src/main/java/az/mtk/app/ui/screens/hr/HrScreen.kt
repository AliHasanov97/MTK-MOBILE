package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApplicationDto
import az.mtk.app.data.api.EmployeeDto
import az.mtk.app.data.api.HrApi
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
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
import java.time.LocalDate

class HrViewModel : ViewModel() {
    private val _tab = MutableStateFlow(0)
    val tab = _tab.asStateFlow()

    val employees = Pager<EmployeeDto>(viewModelScope) { page, query ->
        AppGraph.repository.employees(page, search = query)
    }

    val applications = Pager<ApplicationDto>(viewModelScope) { page, query ->
        AppGraph.repository.applications(page, search = query)
    }

    fun selectTab(index: Int) {
        _tab.value = index
    }
}

@Composable
fun HrScreen(
    onBack: (() -> Unit)?,
    onOpenEmployee: (String) -> Unit,
    showTopBar: Boolean = false,
    viewModel: HrViewModel = mtkViewModel { HrViewModel() },
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val employees by viewModel.employees.state.collectAsStateWithLifecycle()
    val applications by viewModel.applications.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showCreateEmployee by remember { mutableStateOf(false) }
    var showCreateApplication by remember { mutableStateOf(false) }
    var convertTarget by remember { mutableStateOf<ApplicationDto?>(null) }
    var deleteTarget by remember { mutableStateOf<ApplicationDto?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "HR",
                subtitle = if (tab == 0) "${employees.total} işçi" else "${applications.total} ərizə",
                onBack = onBack,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HrTab("İşçilər", tab == 0, Modifier.weight(1f)) { viewModel.selectTab(0) }
            HrTab("Ərizələr", tab == 1, Modifier.weight(1f)) { viewModel.selectTab(1) }
        }

        if (tab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                PrimaryButton(
                    text = "+ Yeni işçi",
                    onClick = { showCreateEmployee = true },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                PrimaryButton(
                    text = "+ Yeni ərizə",
                    onClick = { showCreateApplication = true },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SearchField(
                    value = employees.query,
                    onValueChange = viewModel.employees::onQueryChange,
                    placeholder = "Ad, FIN və ya vəzifə",
                )
            }
            Spacer(Modifier.height(10.dp))

            when {
                employees.loading && employees.items.isEmpty() -> LoadingState("İşçilər yüklənir…")
                employees.error != null && employees.items.isEmpty() ->
                    ErrorState(employees.error!!, onRetry = viewModel.employees::refresh)
                employees.items.isEmpty() -> EmptyState("İşçi tapılmadı")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(employees.items, key = { it.id }) { employee ->
                        MtkCard(onClick = { onOpenEmployee(employee.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarCircle(
                                    Format.initials(listOfNotNull(employee.name, employee.surname).joinToString(" ")),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        Format.person(employee.name, employee.surname, employee.fathersName),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        employee.job?.name ?: "Vəzifə göstərilməyib",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Pill(Format.employmentType(employee.employmentType), Tone.Neutral)
                                        Pill(
                                            Format.isActiveLabel(employee.isActive),
                                            if (employee.isActive == 1) Tone.Success else Tone.Neutral,
                                        )
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

                    if (employees.items.size < employees.total) {
                        item {
                            OutlinedButton(
                                onClick = viewModel.employees::loadMore,
                                enabled = !employees.loadingMore,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) {
                                Text(
                                    if (employees.loadingMore) "Yüklənir…" else "Daha çox (${employees.items.size}/${employees.total})",
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    if (employees.error != null) {
                        item { ErrorState(employees.error!!, onRetry = viewModel.employees::refresh) }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                PrimaryButton(
                    text = "+ Yeni ərizə",
                    onClick = { showCreateApplication = true },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SearchField(
                    value = applications.query,
                    onValueChange = viewModel.applications::onQueryChange,
                    placeholder = "Ərizə növü və ya işçi",
                )
            }
            Spacer(Modifier.height(10.dp))

            when {
                applications.loading && applications.items.isEmpty() -> LoadingState("Ərizələr yüklənir…")
                applications.error != null && applications.items.isEmpty() ->
                    ErrorState(applications.error!!, onRetry = viewModel.applications::refresh)
                applications.items.isEmpty() -> EmptyState("Ərizə tapılmadı")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(applications.items, key = { it.id }) { application ->
                        MtkCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        Format.applicationType(application.type),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        application.employee?.name
                                            ?: application.jobApplicant?.name
                                            ?: "Namizəd göstərilməyib",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "#${application.applicationNumber ?: "—"} · ${Format.date(application.createdAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MtkColors.MutedSoft,
                                    )
                                }
                                if (application.status != null) {
                                    Pill(
                                        if (application.status == "ConvertedToOrder") "Əmrə çevrilib"
                                        else if (application.status == "PendingApproval") "Gözləmədə"
                                        else application.status!!,
                                        if (application.status == "ConvertedToOrder") Tone.Success else Tone.Warning,
                                    )
                                }
                            }
                            if (application.status == "PendingApproval" && application.type != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = { convertTarget = application },
                                    ) {
                                        Text(
                                            if (application.type == 1) "İşə qəbul əmrinə çevir" else "Əmrə çevir",
                                            color = MtkColors.Green,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    TextButton(onClick = { deleteTarget = application }) {
                                        Text("Sil", color = MtkColors.Negative, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (applications.items.size < applications.total) {
                        item {
                            OutlinedButton(
                                onClick = viewModel.applications::loadMore,
                                enabled = !applications.loadingMore,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) {
                                Text(
                                    if (applications.loadingMore) "Yüklənir…" else "Daha çox (${applications.items.size}/${applications.total})",
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    if (applications.error != null) {
                        item { ErrorState(applications.error!!, onRetry = viewModel.applications::refresh) }
                    }
                }
            }
        }
    }

    // ---- Modallar ----
    if (showCreateEmployee) {
        HrEmployeeCreateModal(
            onDismiss = { showCreateEmployee = false },
            onCreated = {
                showCreateEmployee = false
                viewModel.employees.refresh()
            },
        )
    }

    if (showCreateApplication) {
        HrCreateApplicationModal(
            onDismiss = { showCreateApplication = false },
            onCreated = {
                showCreateApplication = false
                viewModel.applications.refresh()
            },
        )
    }

    convertTarget?.let { application ->
        HrConvertApplicationModal(
            type = application.type ?: 1,
            applicationId = application.id,
            applicantName = application.employee?.name ?: application.jobApplicant?.name,
            onDismiss = { convertTarget = null },
            onConverted = {
                convertTarget = null
                viewModel.applications.refresh()
            },
        )
    }

    deleteTarget?.let { application ->
        ConfirmDialog(
            title = "Ərizəni sil",
            message = "№${application.applicationNumber ?: "—"} ${Format.applicationType(application.type)} ərizəsi silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val type = application.type ?: 1
                val id = application.id
                deleteTarget = null
                scope.launch {
                    runCatching { HrApi.repository.deleteApplication(type, id) }
                        .onSuccess { viewModel.applications.refresh() }
                        .onFailure { actionError = it.toUserMessage() }
                }
            },
        )
    }

    actionError?.let { message ->
        ConfirmDialog(
            title = "Xəta",
            message = message,
            confirmLabel = "Bağla",
            onDismiss = { actionError = null },
            onConfirm = { actionError = null },
        )
    }
}

@Composable
private fun HrTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MtkColors.Green else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) MtkColors.Green else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MtkColors.Muted,
        )
    }
}
