package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrInstitutionDto
import az.mtk.app.data.api.HrJobDto
import az.mtk.app.data.api.HrLaborCodeCaseDto
import az.mtk.app.data.api.Page
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Lüğət bölmələri: 0 = Vəzifələr, 1 = Təhsil müəssisələri, 2 = Əmək məcəlləsi halları. */
class DictionariesViewModel : ViewModel() {
    val tab = MutableStateFlow(0)

    val jobs = Pager<HrJobDto>(viewModelScope) { page, query ->
        HrApi.repository.jobs(page, search = query)
    }

    val institutions = Pager<HrInstitutionDto>(viewModelScope) { page, query ->
        HrApi.repository.institutions(page, search = query)
    }

    fun selectTab(index: Int) {
        tab.value = index
    }
}

@Composable
fun DictionariesScreen(
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false,
    viewModel: DictionariesViewModel = mtkViewModel { DictionariesViewModel() },
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.state.collectAsStateWithLifecycle()
    val institutions by viewModel.institutions.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editingJob by remember { mutableStateOf<HrJobDto?>(null) }
    var showNewJob by remember { mutableStateOf(false) }
    var editingInst by remember { mutableStateOf<HrInstitutionDto?>(null) }
    var showNewInst by remember { mutableStateOf(false) }
    var deleteJobTarget by remember { mutableStateOf<HrJobDto?>(null) }
    var deleteInstTarget by remember { mutableStateOf<HrInstitutionDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(title = "Lüğətlər", subtitle = "Vəzifələr, təhsil ocaqları, əmək məcəlləsi", onBack = onBack)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HrTabChip("Vəzifələr", tab == 0, Modifier.weight(1f)) { viewModel.selectTab(0) }
            HrTabChip("Təhsil müəssisələri", tab == 1, Modifier.weight(1f)) { viewModel.selectTab(1) }
            HrTabChip("Əmək məcəlləsi", tab == 2, Modifier.weight(1f)) { viewModel.selectTab(2) }
        }

        when (tab) {
            0 -> {
                DictionaryToolbar(
                    query = jobs.query,
                    onQuery = viewModel.jobs::onQueryChange,
                    placeholder = "Vəzifə axtar…",
                    newLabel = "+ Yeni vəzifə",
                    onNew = { showNewJob = true },
                )
                DictionaryList(
                    state = jobs,
                    onRetry = viewModel.jobs::refresh,
                    onLoadMore = viewModel.jobs::loadMore,
                ) { job ->
                    ListRow(
                        title = job.name,
                        leading = { AvatarCircle(job.name.trim().firstOrNull()?.uppercase() ?: "?") },
                        trailing = {
                            Row {
                                TextButton(onClick = { editingJob = job }) { Text("Redaktə", fontSize = 12.sp) }
                                TextButton(onClick = { deleteJobTarget = job }) {
                                    Text("Sil", fontSize = 12.sp, color = MtkColors.Negative)
                                }
                            }
                        },
                    )
                }
            }
            1 -> {
                DictionaryToolbar(
                    query = institutions.query,
                    onQuery = viewModel.institutions::onQueryChange,
                    placeholder = "Təhsil ocağı axtar…",
                    newLabel = "+ Yeni təhsil ocağı",
                    onNew = { showNewInst = true },
                )
                DictionaryList(
                    state = institutions,
                    onRetry = viewModel.institutions::refresh,
                    onLoadMore = viewModel.institutions::loadMore,
                ) { item ->
                    ListRow(
                        title = item.name,
                        subtitle = HrOptions.label(HrOptions.institutionTypes, item.type),
                        leading = {
                            AvatarCircle(
                                item.name.trim().firstOrNull()?.uppercase() ?: "?",
                                background = MtkColors.Info,
                            )
                        },
                        trailing = {
                            Row {
                                TextButton(onClick = { editingInst = item }) { Text("Redaktə", fontSize = 12.sp) }
                                TextButton(onClick = { deleteInstTarget = item }) {
                                    Text("Sil", fontSize = 12.sp, color = MtkColors.Negative)
                                }
                            }
                        },
                    )
                }
            }
            else -> LaborCodeCasesTab()
        }
    }

    // ---- Vəzifə modalları ----
    if (showNewJob || editingJob != null) {
        DictionaryNameModal(
            title = if (editingJob != null) "Vəzifəni redaktə et" else "Yeni vəzifə",
            nameLabel = "Vəzifənin adı",
            initialName = editingJob?.name.orEmpty(),
            onDismiss = { showNewJob = false; editingJob = null },
            onSave = { name ->
                runCatching {
                    val existing = editingJob
                    if (existing != null) HrApi.repository.updateJob(existing.id, name)
                    else HrApi.repository.createJob(name)
                }
            },
            onSaved = {
                showNewJob = false
                editingJob = null
                viewModel.jobs.refresh()
            },
        )
    }

    // ---- Təhsil ocağı modalları ----
    if (showNewInst || editingInst != null) {
        InstitutionModal(
            editing = editingInst,
            onDismiss = { showNewInst = false; editingInst = null },
            onSaved = {
                showNewInst = false
                editingInst = null
                viewModel.institutions.refresh()
            },
        )
    }

    deleteJobTarget?.let { job ->
        ConfirmDialog(
            title = "Vəzifəni sil",
            message = "\"${job.name}\" vəzifəsi silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteJobTarget = null },
            onConfirm = {
                val target = job
                deleteJobTarget = null
                scope.launch {
                    runCatching { HrApi.repository.deleteJob(target.id) }
                        .onSuccess { viewModel.jobs.refresh() }
                        .onFailure { viewModel.jobs.refresh() }
                }
            },
        )
    }

    deleteInstTarget?.let { item ->
        ConfirmDialog(
            title = "Təhsil ocağını sil",
            message = "\"${item.name}\" silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteInstTarget = null },
            onConfirm = {
                val target = item
                deleteInstTarget = null
                scope.launch {
                    runCatching { HrApi.repository.deleteInstitution(target.id) }
                        .onSuccess { viewModel.institutions.refresh() }
                        .onFailure { viewModel.institutions.refresh() }
                }
            },
        )
    }
}

@Composable
private fun DictionaryToolbar(
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    newLabel: String,
    onNew: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SearchField(value = query, onValueChange = onQuery, placeholder = placeholder)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(text = newLabel, onClick = onNew)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun <T> DictionaryList(
    state: az.mtk.app.ui.components.PagedState<T>,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    when {
        state.loading && state.items.isEmpty() -> LoadingState("Yüklənir…")
        state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = onRetry)
        state.items.isEmpty() -> EmptyState("Nəticə tapılmadı")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.items) { item -> MtkCard(padding = 6.dp) { row(item) } }
            if (state.items.size < state.total) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !state.loadingMore,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Text(
                            if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            if (state.error != null) {
                item { ErrorState(state.error!!, onRetry = onRetry) }
            }
        }
    }
}

@Composable
private fun DictionaryNameModal(
    title: String,
    nameLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = title,
        onDismiss = onDismiss,
        confirmEnabled = !saving && name.isNotBlank(),
        confirmLoading = saving,
        onConfirm = {
            saving = true
            error = null
            scope.launch {
                runCatching { onSave(name.trim()) }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkTextField(name, { name = it }, nameLabel, required = true)
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun InstitutionModal(
    editing: HrInstitutionDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember { mutableStateOf((editing?.type ?: 1).toString()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = if (editing != null) "Təhsil ocağını redaktə et" else "Yeni təhsil ocağı",
        onDismiss = onDismiss,
        confirmEnabled = !saving && name.isNotBlank(),
        confirmLoading = saving,
        onConfirm = {
            saving = true
            error = null
            scope.launch {
                runCatching {
                    if (editing != null) HrApi.repository.updateInstitution(editing.id, name.trim(), type.toIntOrNull() ?: 1)
                    else HrApi.repository.createInstitution(name.trim(), type.toIntOrNull() ?: 1)
                }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkTextField(name, { name = it }, "Təhsil ocağının adı", required = true)
        MtkSelect(type, { type = it }, "Növ", HrOptions.institutionTypes, required = true)
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun LaborCodeCasesTab() {
    var cases by remember { mutableStateOf<List<HrLaborCodeCaseDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { HrApi.repository.laborCodeCases() }
            .onSuccess { cases = it }
            .onFailure { error = it.toUserMessage() }
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Əmək Məcəlləsi halları — müddətli müqavilə əsasları kimi sənədlərdə seçilir (yalnız oxuma).",
            style = MaterialTheme.typography.bodySmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    when {
        error != null -> Column(Modifier.padding(16.dp)) { ErrorState(error!!, onRetry = { cases = null; error = null }) }
        cases == null -> LoadingState("Maddələr yüklənir…")
        cases!!.isEmpty() -> EmptyState("Maddə tapılmadı")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val grouped = cases!!.groupBy { it.parentName ?: "—" }
            grouped.forEach { (article, list) ->
                item {
                    Text(
                        article,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                }
                items(list) { case ->
                    MtkCard(padding = 10.dp) {
                        ListRow(
                            title = "${case.code} ${case.name}",
                            leading = { Pill(if (case.isActive == true) "Aktiv" else "Qeyri-aktiv", Tone.Neutral, showDot = false) },
                        )
                    }
                }
            }
        }
    }
}
