package az.mtk.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.AdminApi
import az.mtk.app.data.api.AdmAuditRow
import az.mtk.app.data.api.AuditActions
import az.mtk.app.data.api.AuditModules
import az.mtk.app.data.api.Comparison
import az.mtk.app.data.api.Page
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuditViewModel : ViewModel() {
    private val api = AppGraph.service(AdminApi::class.java)

    val module = MutableStateFlow(AuditModules.IDENTITY)
    val action = MutableStateFlow<String?>(null)

    val pager = Pager<AdmAuditRow>(viewModelScope) { page, query ->
        val filters = buildList {
            // Backend Contains böyük/kiçik hərfə həssasdır; entity adları C# sinif adlarıdır.
            val entity = query.trim().takeIf { it.isNotEmpty() }
            if (entity != null) {
                add(QueryFilter("EntityType", Comparison.CONTAINS, entity.replaceFirstChar { it.uppercase() }))
            }
            action.value?.let { add(QueryFilter("Action", Comparison.EQUALS, it)) }
        }
        val body = SearchBody(
            filters = filters.ifEmpty { null },
            sortCriteria = SortCriteria("Timestamp", Sort.DESCENDING),
            page = page,
            pageSize = 20,
        )
        val env = when (module.value) {
            AuditModules.IDENTITY -> api.auditIdentity(body)
            AuditModules.BUILDINGS -> api.auditBuildings(body)
            AuditModules.PAYMENTS -> api.auditPayments(body)
            AuditModules.HR -> api.auditHr(body)
            AuditModules.WAREHOUSE -> api.auditWarehouse(body)
            else -> api.auditIdentity(body)
        }
        val payload = env.data ?: throw az.mtk.app.data.api.ApiException(
            env.message?.takeIf { it.isNotBlank() } ?: "Audit qeydləri üçün boş cavab gəldi",
        )
        Page(
            payload.auditLogs.orEmpty().map { log ->
                AdmAuditRow(
                    id = log.id,
                    entityType = log.entityType ?: "—",
                    entityId = log.entityId ?: "—",
                    action = log.action ?: "—",
                    oldValues = log.oldValues,
                    newValues = log.newValues,
                    userLabel = log.user?.name ?: log.userId?.takeIf { it.length >= 8 }?.let { "${it.take(8)}…" },
                    timestamp = log.timestamp,
                )
            },
            payload.totalCount,
            payload.page,
            payload.pageSize,
        )
    }

    fun selectModule(value: String) {
        if (value == module.value) return
        module.value = value
        pager.refresh()
    }

    fun selectAction(value: String?) {
        if (value == action.value) return
        action.value = value
        pager.refresh()
    }
}

/** oldValues/newValues JSON string-lərini açar-dəyər cütlərinə çevirir. */
private fun parseDiff(json: String?): List<Pair<String, String>> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        obj.entrySet().map { (key, value) -> key to value.toString().trim('"') }
    }.getOrDefault(emptyList())
}

@Composable
fun AuditScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: AuditViewModel = mtkViewModel { AuditViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    val module by viewModel.module.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Audit jurnalı",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} qeyd",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            MtkSelect(
                value = module,
                onValueChange = {
                    expandedId = null
                    viewModel.selectModule(it)
                },
                label = "Modul",
                options = AuditModules.ALL.map { MtkOption(it.first, it.second) },
            )
            Spacer(Modifier.height(10.dp))
            SearchField(
                value = state.query,
                onValueChange = {
                    expandedId = null
                    viewModel.pager.onQueryChange(it)
                },
                placeholder = "Entity növü (məs: Employee)…",
            )
            Spacer(Modifier.height(10.dp))
            MtkSelect(
                value = action,
                onValueChange = {
                    expandedId = null
                    viewModel.selectAction(it)
                },
                label = "Əməliyyat",
                options = listOf(MtkOption("", "Bütün əməliyyatlar")) +
                    AuditActions.ALL.map { MtkOption(it.first, it.second) },
            )
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Audit qeydləri yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Nəticə tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { log ->
                    val isOpen = expandedId == log.id
                    MtkCard(
                        modifier = Modifier.clickable { expandedId = if (isOpen) null else log.id },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        log.entityType,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.padding(start = 6.dp))
                                    Text(
                                        "(${log.entityId.take(8)}…)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MtkColors.MutedSoft,
                                    )
                                }
                                Text(
                                    log.userLabel ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    Format.dateTime(log.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Pill(AuditActions.label(log.action), when (log.action) {
                                    "Created" -> Tone.Success
                                    "Updated" -> Tone.Info
                                    "Deleted" -> Tone.Danger
                                    else -> Tone.Neutral
                                })
                                Spacer(Modifier.padding(start = 6.dp))
                                Icon(
                                    if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MtkColors.MutedSoft,
                                )
                            }
                        }

                        if (isOpen) {
                            Spacer(Modifier.height(10.dp))
                            val oldPairs = parseDiff(log.oldValues)
                            val newPairs = parseDiff(log.newValues)
                            if (oldPairs.isEmpty() && newPairs.isEmpty()) {
                                Text(
                                    "Dəyişiklik təfsilatı yoxdur.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                            } else {
                                if (oldPairs.isNotEmpty()) {
                                    Text(
                                        "ƏVVƏL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MtkColors.MutedSoft,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    oldPairs.forEach { (key, value) -> InfoRow(key, value) }
                                }
                                if (newPairs.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "SONRA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MtkColors.MutedSoft,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    newPairs.forEach { (key, value) -> InfoRow(key, value) }
                                }
                            }
                        }
                    }
                }

                if (state.items.size < state.total) {
                    item {
                        az.mtk.app.ui.components.SecondaryButton(
                            text = if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                            onClick = viewModel.pager::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = viewModel.pager::refresh) }
                }
            }
        }
    }
}
