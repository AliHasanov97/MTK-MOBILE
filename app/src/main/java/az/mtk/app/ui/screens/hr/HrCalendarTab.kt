package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrCalendarDayBody
import az.mtk.app.data.api.HrCalendarDayDto
import az.mtk.app.data.api.HrCalendarMonthDto
import az.mtk.app.data.api.HrCalendarYearDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HrCalendarEditTarget(val day: HrCalendarDayDto?, val date: String?)

data class HrBulkRow(
    val name: String = "",
    val from: String = "",
    val to: String = "",
    val dayType: Int = 1,
)

class HrCalendarViewModel : ViewModel() {

    private val repo = HrApi.repository

    data class UiState(
        val year: Int = LocalDate.now().year,
        val schedule: Int = 1,
        val sheets: Map<Int, HrCalendarYearDto> = emptyMap(),
        val loading: Boolean = false,
        val error: String? = null,
        val notice: String? = null,
        val editing: HrCalendarEditTarget? = null,
        val formName: String = "",
        val formDate: String = "",
        val formDayType: Int = 1,
        val formReason: String = "",
        val formSchedule: Int? = null,
        val saving: Boolean = false,
        val formError: String? = null,
        val deleting: Boolean = false,
        val bulkOpen: Boolean = false,
        val bulkRows: List<HrBulkRow> = listOf(HrBulkRow()),
        val bulkSchedule: Int? = null,
        val bulkSaving: Boolean = false,
        val bulkError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        val year = _state.value.year
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val five = async { repo.calendarYear(year, 1) }
                val six = async { repo.calendarYear(year, 2) }
                mapOf(1 to five.await(), 2 to six.await())
            }.onSuccess { sheets ->
                _state.value = _state.value.copy(sheets = sheets, loading = false)
            }.onFailure { failure ->
                _state.value = _state.value.copy(error = failure.toUserMessage(), loading = false)
            }
        }
    }

    fun changeYear(delta: Int) {
        _state.value = _state.value.copy(year = _state.value.year + delta, sheets = emptyMap(), error = null)
        load()
    }

    fun setSchedule(schedule: Int) {
        _state.value = _state.value.copy(schedule = schedule)
    }

    fun openNew(date: String? = null) {
        _state.value = _state.value.copy(
            editing = HrCalendarEditTarget(null, date),
            formName = "",
            formDate = date ?: "${_state.value.year}-01-01",
            formDayType = 1,
            formReason = "",
            formSchedule = null,
            formError = null,
        )
    }

    fun openEdit(day: HrCalendarDayDto) {
        _state.value = _state.value.copy(
            editing = HrCalendarEditTarget(day, day.date.take(10)),
            formName = day.name,
            formDate = day.date.take(10),
            formDayType = day.dayType ?: 1,
            formReason = day.reason.orEmpty(),
            formSchedule = day.applicableWorkingDays,
            formError = null,
        )
    }

    fun closeEditor() {
        _state.value = _state.value.copy(editing = null, formError = null, saving = false)
    }

    fun setFormName(value: String) {
        _state.value = _state.value.copy(formName = value)
    }

    fun setFormDate(value: String) {
        _state.value = _state.value.copy(formDate = value)
    }

    fun setFormDayType(value: Int) {
        _state.value = _state.value.copy(formDayType = value)
    }

    fun setFormReason(value: String) {
        _state.value = _state.value.copy(formReason = value)
    }

    fun setFormSchedule(value: Int?) {
        _state.value = _state.value.copy(formSchedule = value)
    }

    fun saveDay() {
        val s = _state.value
        val target = s.editing ?: return
        if (s.formName.isBlank()) {
            _state.value = s.copy(formError = "Ad doldurulmalıdır.")
            return
        }
        if (s.formDate.isBlank()) {
            _state.value = s.copy(formError = "Tarix doldurulmalıdır.")
            return
        }
        _state.value = s.copy(saving = true, formError = null)
        val body = HrCalendarDayBody(
            name = s.formName.trim(),
            date = s.formDate.take(10),
            dayType = s.formDayType,
            reason = s.formReason.trim().ifBlank { null },
            applicableWorkingDays = s.formSchedule,
        )
        viewModelScope.launch {
            runCatching {
                if (target.day == null) repo.createCalendarDay(body)
                else repo.updateCalendarDay(target.day.id, body)
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    editing = null,
                    notice = "Təqvim günü yadda saxlanıldı.",
                )
                load()
            }.onFailure { failure ->
                _state.value = _state.value.copy(saving = false, formError = failure.toUserMessage())
            }
        }
    }

    fun requestDelete(day: HrCalendarDayDto) {
        _state.value = _state.value.copy(editing = HrCalendarEditTarget(day, day.date.take(10)))
    }

    fun confirmDelete() {
        val day = _state.value.editing?.day ?: return
        _state.value = _state.value.copy(deleting = true)
        viewModelScope.launch {
            runCatching { repo.deleteCalendarDay(day.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        deleting = false,
                        editing = null,
                        notice = "Təqvim günü silindi.",
                    )
                    load()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(deleting = false, formError = failure.toUserMessage())
                }
        }
    }

    fun openBulk() {
        _state.value = _state.value.copy(
            bulkOpen = true,
            bulkRows = listOf(HrBulkRow(from = "${_state.value.year}-01-01")),
            bulkSchedule = null,
            bulkError = null,
        )
    }

    fun closeBulk() {
        _state.value = _state.value.copy(bulkOpen = false, bulkError = null, bulkSaving = false)
    }

    fun updateBulkRow(index: Int, row: HrBulkRow) {
        val rows = _state.value.bulkRows.toMutableList()
        if (index !in rows.indices) return
        rows[index] = row
        _state.value = _state.value.copy(bulkRows = rows)
    }

    fun addBulkRow() {
        _state.value = _state.value.copy(
            bulkRows = _state.value.bulkRows + HrBulkRow(from = "${_state.value.year}-01-01"),
        )
    }

    fun removeBulkRow(index: Int) {
        val rows = _state.value.bulkRows.filterIndexed { i, _ -> i != index }
        _state.value = _state.value.copy(bulkRows = rows.ifEmpty { listOf(HrBulkRow()) })
    }

    fun setBulkSchedule(value: Int?) {
        _state.value = _state.value.copy(bulkSchedule = value)
    }

    fun saveBulk() {
        val s = _state.value
        val days = s.bulkRows.flatMap { row ->
            expandRange(row.from, row.to).map { date ->
                HrCalendarDayBody(
                    name = row.name.trim(),
                    date = date,
                    dayType = row.dayType,
                    reason = null,
                    applicableWorkingDays = s.bulkSchedule,
                )
            }
        }.filter { it.name.isNotBlank() }

        if (days.isEmpty()) {
            _state.value = s.copy(bulkError = "Ən azı bir tarix aralığı və ad daxil edin.")
            return
        }
        _state.value = s.copy(bulkSaving = true, bulkError = null)
        viewModelScope.launch {
            runCatching { repo.bulkCreateCalendarDays(days) }
                .onSuccess { result ->
                    val skipped = result.skippedDates.orEmpty().size
                    _state.value = _state.value.copy(
                        bulkSaving = false,
                        bulkOpen = false,
                        notice = buildString {
                            append("${result.createdCount ?: days.size} gün əlavə edildi.")
                            if (skipped > 0) append(" $skipped tarix keçildi.")
                        },
                    )
                    load()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(bulkSaving = false, bulkError = failure.toUserMessage())
                }
        }
    }

    fun clearNotice() {
        _state.value = _state.value.copy(notice = null, error = null)
    }

    private fun expandRange(from: String, to: String): List<String> {
        if (from.isBlank()) return emptyList()
        val start = runCatching { LocalDate.parse(from.take(10)) }.getOrNull() ?: return emptyList()
        val end = runCatching { LocalDate.parse((to.ifBlank { from }).take(10)) }.getOrNull() ?: start
        val out = mutableListOf<String>()
        var cursor = start
        while (!cursor.isAfter(end) && out.size < 400) {
            out += cursor.toString()
            cursor = cursor.plusDays(1)
        }
        return out
    }
}

@Composable
internal fun HrCalendarTab(viewModel: HrCalendarViewModel = mtkViewModel { HrCalendarViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val data = state.sheets[state.schedule]

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.changeYear(-1) }) {
                Icon(Icons.Default.ChevronLeft, "Əvvəlki il", tint = HrColors.Muted)
            }
            Text(
                text = state.year.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
            )
            IconButton(onClick = { viewModel.changeYear(1) }) {
                Icon(Icons.Default.ChevronRight, "Növbəti il", tint = HrColors.Muted)
            }
            Spacer(Modifier.weight(1f))
            HrSecondaryButton(text = "Toplu", onClick = viewModel::openBulk)
            Spacer(Modifier.size(8.dp))
            HrPrimaryButton(text = "+ Gün", onClick = { viewModel.openNew() })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1 to "5 günlük", 2 to "6 günlük").forEach { (value, label) ->
                SurfaceChip(
                    label = label,
                    selected = state.schedule == value,
                    onClick = { viewModel.setSchedule(value) },
                )
            }
        }

        state.notice?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearNotice() } }
        state.error?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearNotice() } }

        when {
            state.loading && data == null -> HrLoading()
            data == null -> HrEmptyState("Təqvim alınmadı", "İli dəyişin və ya yenidən cəhd edin.")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CalendarStat("İş günü", data.yearTotal?.workingDays?.toString() ?: "—", Modifier.weight(1f))
                            CalendarStat("Qeyri-iş", data.yearTotal?.nonWorkingDays?.toString() ?: "—", Modifier.weight(1f))
                            CalendarStat(
                                "Qeyd",
                                data.months.orEmpty().sumOf { it.days.orEmpty().size }.toString(),
                                Modifier.weight(1f),
                            )
                        }
                    }
                    items(data.months.orEmpty().size) { index ->
                        val month = data.months.orEmpty()[index]
                        MonthCard(
                            year = state.year,
                            month = month,
                            schedule = state.schedule,
                            onPick = { date, day -> if (day == null) viewModel.openNew(date) else viewModel.openEdit(day) },
                            onEdit = viewModel::openEdit,
                        )
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }
    }

    state.editing?.let { target ->
        CalendarDaySheet(
            target = target,
            state = state,
            onName = viewModel::setFormName,
            onDate = viewModel::setFormDate,
            onType = viewModel::setFormDayType,
            onReason = viewModel::setFormReason,
            onSchedule = viewModel::setFormSchedule,
            onSave = viewModel::saveDay,
            onDelete = viewModel::confirmDelete,
            onDismiss = viewModel::closeEditor,
        )
    }

    if (state.bulkOpen) {
        BulkCalendarSheet(
            state = state,
            onRow = viewModel::updateBulkRow,
            onAddRow = viewModel::addBulkRow,
            onRemoveRow = viewModel::removeBulkRow,
            onSchedule = viewModel::setBulkSchedule,
            onSave = viewModel::saveBulk,
            onDismiss = viewModel::closeBulk,
        )
    }
}

@Composable
private fun SurfaceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) HrColors.Terracotta else HrColors.Subtle,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else HrColors.Ink,
        )
    }
}

@Composable
private fun CalendarStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HrColors.Surface)
            .padding(10.dp),
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink)
        Text(label, fontSize = 10.sp, color = HrColors.MutedSoft)
    }
}

@Composable
private fun MonthCard(
    year: Int,
    month: HrCalendarMonthDto,
    schedule: Int,
    onPick: (String, HrCalendarDayDto?) -> Unit,
    onEdit: (HrCalendarDayDto) -> Unit,
) {
    val monthNumber = month.month ?: 1
    val byDate = month.days.orEmpty().associateBy { it.date.take(10) }
    val firstDay = runCatching { LocalDate.of(year, monthNumber, 1) }.getOrDefault(LocalDate.of(year, 1, 1))
    val offset = firstDay.dayOfWeek.value - 1
    val length = firstDay.lengthOfMonth()

    HrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.monthName ?: HrEnums.month(monthNumber),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${month.workingDays ?: 0} iş · ${month.nonWorkingDays ?: 0} qeyri-iş",
                fontSize = 10.sp,
                color = HrColors.MutedSoft,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("B.e", "Ç.a", "Çər", "C.a", "Cüm", "Şən", "Baz").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = HrColors.MutedSoft,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells: List<Int?> = List(offset) { null } + (1..length).toList()
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = "%04d-%02d-%02d".format(year, monthNumber, day)
                        val entry = byDate[date]
                        val weekday = runCatching { LocalDate.of(year, monthNumber, day).dayOfWeek.value }.getOrDefault(1)
                        val weekend = weekday == 7 || (weekday == 6 && schedule == 1)
                        val (fg, bg) = when (entry?.dayType) {
                            1 -> HrColors.Error to HrColors.ErrorSoft
                            2 -> HrColors.Olive to HrColors.OliveSoft
                            3 -> HrColors.Muted to HrColors.Subtle
                            else -> if (weekend) HrColors.MutedSoft to HrColors.Subtle else HrColors.Ink to Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(bg)
                                .clickable { onPick(date, entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = 11.sp,
                                color = fg,
                                fontWeight = if (entry != null) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        val entries = month.days.orEmpty()
        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = Format.date(entry.date).take(5),
                        fontSize = 10.sp,
                        color = HrColors.MutedSoft,
                        modifier = Modifier.size(width = 38.dp, height = 16.dp),
                    )
                    Text(
                        text = entry.name,
                        fontSize = 12.sp,
                        color = HrColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    HrTag(
                        text = HrEnums.label(HrEnums.CALENDAR_DAY_TYPES, entry.dayType),
                        foreground = when (entry.dayType) {
                            1 -> HrColors.Error
                            2 -> HrColors.Olive
                            else -> HrColors.Muted
                        },
                        background = when (entry.dayType) {
                            1 -> HrColors.ErrorSoft
                            2 -> HrColors.OliveSoft
                            else -> HrColors.Subtle
                        },
                    )
                    IconButton(onClick = { onEdit(entry) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Redaktə", tint = HrColors.Muted, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDaySheet(
    target: HrCalendarEditTarget,
    state: HrCalendarViewModel.UiState,
    onName: (String) -> Unit,
    onDate: (String) -> Unit,
    onType: (Int) -> Unit,
    onReason: (String) -> Unit,
    onSchedule: (Int?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle(if (target.day == null) "Yeni təqvim günü" else "Təqvim gününü redaktə et", onDismiss)

        HrSelectField(
            label = "Gün növü",
            value = state.formDayType.toString(),
            options = HrEnums.CALENDAR_DAY_TYPES.map { it.value.toString() to it.label },
            onSelect = { onType(it.toIntOrNull() ?: 1) },
        )
        Spacer(Modifier.height(10.dp))
        HrDateField("Tarix", state.formDate, onValueChange = onDate)
        Spacer(Modifier.height(10.dp))
        HrTextField("Ad", state.formName, placeholder = "Məs. Novruz bayramı", onValueChange = onName)
        Spacer(Modifier.height(10.dp))
        HrSelectField(
            label = "Hansı iş qrafikinə tətbiq olunur",
            value = (state.formSchedule ?: -1).toString(),
            options = listOf("-1" to "Hamıya", "1" to "Yalnız 5 günlük", "2" to "Yalnız 6 günlük"),
            onSelect = { onSchedule(it.toIntOrNull()?.takeIf { value -> value > 0 }) },
        )
        Spacer(Modifier.height(10.dp))
        HrTextField("Səbəb (ixtiyari)", state.formReason, onValueChange = onReason)

        state.formError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (target.day != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Sil", color = HrColors.Error, fontWeight = FontWeight.Bold)
                }
            }
            HrSecondaryButton(text = "Ləğv et", onClick = onDismiss, enabled = !state.saving)
            HrPrimaryButton(
                text = if (state.saving) "Saxlanılır…" else "Yadda saxla",
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }

    if (confirmDelete) {
        AppSheetDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Gün silinsin?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink) },
            text = {
                Text(
                    text = "${Format.date(target.day?.date)} — «${target.day?.name.orEmpty()}» silinəcək.",
                    fontSize = 13.sp,
                    color = HrColors.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Sil", color = HrColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Ləğv et", color = HrColors.Muted) }
            },
        )
    }
}

@Composable
private fun BulkCalendarSheet(
    state: HrCalendarViewModel.UiState,
    onRow: (Int, HrBulkRow) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    onSchedule: (Int?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle("Toplu əlavə", onDismiss)
        Text(
            text = "Tarix aralığı üçün ad və gün növü seçin — aralıqdaki bütün günlər əlavə olunur.",
            fontSize = 12.sp,
            color = HrColors.MutedSoft,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        state.bulkRows.forEachIndexed { index, row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sətir ${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HrColors.Muted)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onRemoveRow(index) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Sətri sil", tint = HrColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
                HrTextField("Ad", row.name) { onRow(index, row.copy(name = it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        HrDateField("Başlanğıc", row.from) { onRow(index, row.copy(from = it)) }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        HrDateField("Son", row.to) { onRow(index, row.copy(to = it)) }
                    }
                }
                HrSelectField(
                    label = "Gün növü",
                    value = row.dayType.toString(),
                    options = HrEnums.CALENDAR_DAY_TYPES.map { it.value.toString() to it.label },
                    onSelect = { onRow(index, row.copy(dayType = it.toIntOrNull() ?: 1)) },
                )
            }
        }

        HrSecondaryButton(text = "+ Sətir əlavə et", onClick = onAddRow)
        Spacer(Modifier.height(10.dp))
        HrSelectField(
            label = "İş qrafiki",
            value = (state.bulkSchedule ?: -1).toString(),
            options = listOf("-1" to "Hamıya", "1" to "Yalnız 5 günlük", "2" to "Yalnız 6 günlük"),
            onSelect = { onSchedule(it.toIntOrNull()?.takeIf { value -> value > 0 }) },
        )

        state.bulkError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton(text = "Ləğv et", onClick = onDismiss, enabled = !state.bulkSaving)
            HrPrimaryButton(
                text = if (state.bulkSaving) "Əlavə olunur…" else "Əlavə et",
                onClick = onSave,
                enabled = !state.bulkSaving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
