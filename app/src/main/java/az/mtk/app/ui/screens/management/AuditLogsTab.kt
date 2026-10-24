package az.mtk.app.ui.screens.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.AdmAuditRow
import az.mtk.app.data.api.AuditActions
import az.mtk.app.data.api.AuditModules
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsDateField
import az.mtk.app.ui.screens.operations.OpsEmpty
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPagination
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsSelectField
import az.mtk.app.ui.screens.operations.OpsTextField
import az.mtk.app.util.Format
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

private val NOISE_FIELDS = setOf("UpdatedAt", "SearchVector")

@Composable
internal fun AuditLogsTab(
    state: AuditLogState,
    viewModel: ManagementViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { AuditFilterCard(state, viewModel) }

        if (state.loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = OpsColors.Terracotta,
                    trackColor = OpsColors.Border,
                )
            }
        }

        when {
            state.error != null -> item {
                OpsError(state.error) { viewModel.refreshAudit() }
            }

            state.rows.isEmpty() && !state.loading -> item {
                OpsEmpty(
                    title = "Nəticə tapılmadı",
                    text = "Filtrləri dəyişdirib yenidən yoxlayın.",
                )
            }

            else -> {
                items(state.rows, key = { row -> row.id }) { row ->
                    AuditRowCard(
                        row = row,
                        expanded = state.expandedId == row.id,
                        onToggle = { viewModel.toggleAuditRow(row.id) },
                    )
                }
                item {
                    OpsPagination(
                        page = state.page,
                        total = state.totalCount,
                        pageSize = state.pageSize,
                        onPrev = viewModel::previousAuditPage,
                        onNext = viewModel::nextAuditPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditFilterCard(
    state: AuditLogState,
    viewModel: ManagementViewModel,
) {
    OpsCard {
        OpsSelectField(
            label = "Modul",
            options = AuditModules.ALL,
            selectedId = state.module,
            onSelect = viewModel::selectAuditModule,
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.entityType,
            onValueChange = viewModel::setAuditEntityType,
            label = "Obyekt növü",
            placeholder = "məs: Employee",
        )
        Spacer(Modifier.height(10.dp))
        OpsSelectField(
            label = "Əməliyyat",
            options = listOf("" to "Bütün əməliyyatlar") + AuditActions.ALL,
            selectedId = state.action,
            onSelect = viewModel::setAuditAction,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OpsDateField(
                label = "Başlanğıc tarix",
                value = state.dateFrom,
                onValueChange = viewModel::setAuditDateFrom,
                modifier = Modifier.weight(1f),
            )
            OpsDateField(
                label = "Son tarix",
                value = state.dateTo,
                onValueChange = viewModel::setAuditDateTo,
                modifier = Modifier.weight(1f),
            )
        }

        val hasFilters = state.entityType.isNotBlank() || state.action.isNotBlank() ||
            state.dateFrom.isNotBlank() || state.dateTo.isNotBlank()
        if (hasFilters) {
            Spacer(Modifier.height(10.dp))
            OpsSecondaryButton(
                text = "Filtrləri təmizlə",
                onClick = viewModel::clearAuditFilters,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuditRowCard(
    row: AdmAuditRow,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    OpsCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuditActionChip(row.action)
            Spacer(Modifier.weight(1f))
            Text(
                text = Format.dateTime(row.timestamp),
                fontSize = 11.sp,
                color = OpsColors.MutedSoft,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = row.entityType,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Text(
            text = if (row.entityId.length > 12) "${row.entityId.take(8)}…" else row.entityId,
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )

        Spacer(Modifier.height(7.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = OpsColors.MutedSoft,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = row.userLabel ?: "—",
                fontSize = 12.sp,
                color = OpsColors.Muted,
            )
        }

        Spacer(Modifier.height(9.dp))
        HorizontalDivider(thickness = 1.dp, color = OpsColors.Border)
        Spacer(Modifier.height(9.dp))

        if (expanded) {
            AuditDiff(row.oldValues, row.newValues)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bağla",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Terracotta,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = OpsColors.MutedSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Detallara bax",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Terracotta,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OpsColors.MutedSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AuditActionChip(action: String) {
    val (foreground, background) = when (action) {
        AuditActions.CREATED -> OpsColors.Olive to OpsColors.OliveSoft
        AuditActions.UPDATED -> OpsColors.Blue to OpsColors.BlueSoft
        AuditActions.DELETED -> OpsColors.Error to OpsColors.ErrorSoft
        else -> OpsColors.MutedSoft to OpsColors.Subtle
    }
    OpsChip(
        text = AuditActions.label(action),
        foreground = foreground,
        background = background,
    )
}

/**
 * Backend hər dəyişiklikdə bütün sahələrin köhnə və yeni dəyərini JSON kimi
 * yazır. Burada yalnız həqiqətən dəyişən və məlumat daşıyan sahələr göstərilir.
 */
@Composable
private fun AuditDiff(oldValues: String?, newValues: String?) {
    val before = parseJsonObject(oldValues)
    val after = parseJsonObject(newValues)

    // JSON oxunmadısa, ham mətni göstərmək məlumatı itirməkdən yaxşıdır.
    if ((!oldValues.isNullOrBlank() && before == null) ||
        (!newValues.isNullOrBlank() && after == null)
    ) {
        if (!oldValues.isNullOrBlank()) {
            RawJsonBlock(oldValues)
        }
        if (!newValues.isNullOrBlank()) {
            if (!oldValues.isNullOrBlank()) Spacer(Modifier.height(6.dp))
            RawJsonBlock(newValues)
        }
        return
    }

    if (before == null && after == null) {
        Text(
            text = "Əlavə məlumat yoxdur.",
            fontSize = 12.sp,
            color = OpsColors.MutedSoft,
        )
        return
    }

    // Yeni qeyd və ya silinmə: yalnız bir tərəf var.
    if (before == null || after == null) {
        val single = after ?: before!!
        Text(
            text = if (after != null) "Yeni qeydin dəyərləri:" else "Silinmiş qeydin dəyərləri:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(4.dp))
        val entries = single.entrySet().filter { entry ->
            entry.key !in NOISE_FIELDS && !isBlankValue(entry.value)
        }
        if (entries.isEmpty()) {
            Text(
                text = "Göstəriləcək sahə yoxdur.",
                fontSize = 12.sp,
                color = OpsColors.MutedSoft,
            )
        } else {
            entries.forEach { entry ->
                DiffValueRow(field = entry.key, value = displayValue(entry.value))
            }
        }
        return
    }

    val keys = before.keySet() + after.keySet()
    val changed = keys.filter { key -> before.get(key) != after.get(key) }
    val meaningful = changed.filter { key -> key !in NOISE_FIELDS }
    val rows = if (meaningful.isNotEmpty()) meaningful else changed

    if (rows.isEmpty()) {
        Text(
            text = "Dəyişən sahə yoxdur.",
            fontSize = 12.sp,
            color = OpsColors.MutedSoft,
        )
        return
    }

    Text(
        text = "Dəyişən sahələr (${rows.size}):",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = OpsColors.Ink,
    )
    Spacer(Modifier.height(4.dp))
    rows.forEach { field ->
        DiffChangeRow(
            field = field,
            before = before.get(field),
            after = after.get(field),
        )
    }
}

@Composable
private fun RawJsonBlock(raw: String) {
    Text(
        text = raw,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = OpsColors.Muted,
    )
}

@Composable
private fun DiffValueRow(field: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = field,
            modifier = Modifier.weight(0.9f),
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = OpsColors.Ink,
        )
    }
}

@Composable
private fun DiffChangeRow(field: String, before: JsonElement?, after: JsonElement?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = field,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Köhnə", fontSize = 10.sp, color = OpsColors.MutedSoft)
                Text(
                    text = displayValue(before),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = OpsColors.Error,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Yeni", fontSize = 10.sp, color = OpsColors.MutedSoft)
                Text(
                    text = displayValue(after),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = OpsColors.Olive,
                )
            }
        }
    }
}

private fun parseJsonObject(raw: String?): JsonObject? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val element = JsonParser.parseString(raw)
        if (element.isJsonObject) element.asJsonObject else null
    }.getOrNull()
}

private fun isBlankValue(value: JsonElement?): Boolean {
    if (value == null || value.isJsonNull) return true
    if (value.isJsonPrimitive) {
        val primitive = value.asJsonPrimitive
        if (primitive.isString) return primitive.asString.isEmpty()
    }
    return false
}

private fun displayValue(value: JsonElement?): String {
    if (value == null || value.isJsonNull) return "—"
    if (value.isJsonPrimitive) {
        val primitive = value.asJsonPrimitive
        return when {
            primitive.isBoolean -> if (primitive.asBoolean) "Bəli" else "Xeyr"
            primitive.isString -> primitive.asString.ifEmpty { "—" }
            else -> primitive.asString
        }
    }
    return value.toString()
}
