package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import az.mtk.app.ui.components.ConfirmDialog
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarState(
    val sheets: Map<Int, HrCalendarYearDto> = emptyMap(), // key: workingDays (1/2)
    val loading: Boolean = true,
    val error: String? = null,
)

class HrCalendarViewModel : ViewModel() {
    private val _year = MutableStateFlow(LocalDate.now().year)
    val year = _year.asStateFlow()

    private val _schedule = MutableStateFlow(1) // 1 = 5 günlük, 2 = 6 günlük
    val schedule = _schedule.asStateFlow()

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _reloadKey = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _reloadKey.collect { if (it > 0) load() }
        }
        load()
    }

    fun setYear(value: Int) {
        _year.value = value
        load()
    }

    fun setSchedule(value: Int) {
        _schedule.value = value
    }

    fun reload() {
        _reloadKey.value++
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                HrApi.repository.calendarYear(_year.value, 1) to HrApi.repository.calendarYear(_year.value, 2)
            }
                .onSuccess { (five, six) ->
                    _state.value = CalendarState(
                        sheets = mapOf(1 to five, 2 to six),
                        loading = false,
                    )
                }
                .onFailure { _state.value = CalendarState(loading = false, error = it.toUserMessage()) }
        }
    }
}

private val CALENDAR_TYPE_INFO = mapOf(
    1 to Triple("Bayram", "Bayram və ya hüzn günü — qeyri-iş günü", Color(0xFFFDE3E3)),
    2 to Triple("İş günü", "İstirahət gününün əvəzinə işlənən gün", Color(0xFFE1F2D3)),
    3 to Triple("Qeyri-iş günü", "Əlavə istirahət günü", Color(0xFFE6E9E4)),
)

private val WEEKDAYS_AZ = listOf("B.e", "Ç.a", "Çər", "C.a", "Cüm", "Şən", "Baz")

/** data: "2026-03-01" / ISO date — "yyyy-MM-dd" hissəsi. */
private fun dayKey(date: String): String = date.take(10)

@Composable
fun HrCalendarScreen(
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false,
    viewModel: HrCalendarViewModel = mtkViewModel { HrCalendarViewModel() },
) {
    val year by viewModel.year.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var editTarget by remember { mutableStateOf<HrCalendarDayDto?>(null) }
    var editDate by remember { mutableStateOf<String?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showBulk by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HrCalendarDayDto?>(null) }

    val data = state.sheets[schedule]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(title = "Təqvim", subtitle = "$year ili", onBack = onBack)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { viewModel.setYear(year - 1) }) { Text("‹") }
            Text(
                year.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = { viewModel.setYear(year + 1) }) { Text("›") }
            Spacer(Modifier.width(8.dp))
            MtkSelect(
                value = schedule.toString(),
                onValueChange = { viewModel.setSchedule(it.toInt()) },
                label = "İş həftəsi",
                options = listOf(MtkOption("1", "5 günlük"), MtkOption("2", "6 günlük")),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(text = "Toplu əlavə et", onClick = { showBulk = true }, modifier = Modifier.weight(1f))
            PrimaryButton(text = "+ Gün əlavə et", onClick = { editTarget = null; editDate = null; showEdit = true }, modifier = Modifier.weight(1f))
        }

        when {
            state.loading -> LoadingState("Təqvim yüklənir…")
            state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
            data == null -> LoadingState()
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                data.yearTotal?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${it.workingDays ?: 0} iş günü", style = MaterialTheme.typography.bodySmall, color = MtkColors.Green, fontWeight = FontWeight.SemiBold)
                        Text("${it.nonWorkingDays ?: 0} qeyri-iş günü", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                state.sheets[1]?.yearTotal?.let { five ->
                    val six = state.sheets[2]?.yearTotal
                    Pill(
                        "İş günü: 5 günlük ${five.workingDays ?: 0} / 6 günlük ${six?.workingDays ?: 0}",
                        Tone.Info,
                        showDot = false,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                data.months.orEmpty().forEach { monthData ->
                    MonthCard(
                        year = year,
                        schedule = schedule,
                        month = monthData,
                        onPick = { date, day ->
                            editTarget = day
                            editDate = date
                            showEdit = true
                        },
                        onDelete = { deleteTarget = it },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showEdit) {
        CalendarDayModal(
            year = year,
            day = editTarget,
            initialDate = editDate,
            defaultSchedule = if (editTarget == null) schedule.toString() else "",
            onDismiss = { showEdit = false },
            onDelete = { day ->
                showEdit = false
                deleteTarget = day
            },
            onSaved = {
                showEdit = false
                viewModel.reload()
            },
        )
    }

    if (showBulk) {
        BulkCalendarModal(
            year = year,
            onDismiss = { showBulk = false },
            onSaved = {
                showBulk = false
                viewModel.reload()
            },
        )
    }

    deleteTarget?.let { day ->
        ConfirmDialog(
            title = "Günü sil",
            message = "${Format.date(day.date)} — \"${day.name}\" silinsin?",
            confirmLabel = "Sil",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val target = day
                deleteTarget = null
                viewModel.viewModelScope.launch {
                    runCatching { HrApi.repository.deleteCalendarDay(target.id) }
                        .onSuccess { viewModel.reload() }
                        .onFailure { /* səssiz: reload xətanı göstərəcək */ }
                }
            },
        )
    }
}

@Composable
private fun MonthCard(
    year: Int,
    schedule: Int,
    month: HrCalendarMonthDto,
    onPick: (date: String, day: HrCalendarDayDto?) -> Unit,
    onDelete: (HrCalendarDayDto) -> Unit,
) {
    val monthNumber = month.month ?: return
    val daysInMonth = remember(year, monthNumber) { LocalDate.of(year, monthNumber, 1).lengthOfMonth() }
    val offset = remember(year, monthNumber) { (LocalDate.of(year, monthNumber, 1).dayOfWeek.value + 5) % 7 } // B.e = 0
    val byDate = remember(month) { month.days.orEmpty().associateBy { dayKey(it.date) } }

    MtkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(month.monthName ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "${month.workingDays ?: 0} iş günü · ${month.nonWorkingDays ?: 0} qeyri-iş",
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
            )
        }
        Spacer(Modifier.height(8.dp))

        // Gün şəbəkəsi (bazar ertəsindən başlayır)
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS_AZ.forEachIndexed { i, w ->
                Text(
                    w,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (i >= 5) MtkColors.Warning else MtkColors.MutedSoft,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells: List<Int?> = List(offset) { null } + (1..daysInMonth).toList()
        val rows = cells.chunked(7)
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp),
                    ) {
                        if (day != null) {
                            val key = String.format("%04d-%02d-%02d", year, monthNumber, day)
                            val entry = byDate[key]
                            val weekday = (offset + day - 1) % 7 // 0=B.e … 6=Baz
                            val weekend = weekday == 6 || (weekday == 5 && schedule == 1)
                            val bg = when {
                                entry != null -> CALENDAR_TYPE_INFO[entry.dayType]?.third ?: Color(0xFFE6E9E4)
                                weekend -> Color(0xFFFDF1E7)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { onPick(key, entry) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (month.days.isNullOrEmpty()) {
            Text("Qeyd yoxdur", style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedUltraSoft)
        } else {
            month.days.orEmpty().forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Format.date(day.date).take(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MtkColors.MutedSoft,
                        modifier = Modifier.width(44.dp),
                    )
                    Text(
                        day.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        CALENDAR_TYPE_INFO[day.dayType]?.first ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MtkColors.Muted,
                    )
                    if (day.applicableWorkingDays != null) {
                        Spacer(Modifier.width(6.dp))
                        Pill("${day.applicableWorkingDays} gün", Tone.Neutral, showDot = false)
                    }
                    TextButton(onClick = { onPick(dayKey(day.date), day) }, modifier = Modifier.width(0.dp).weight(0.4f)) {
                        Text("✎", fontSize = 13.sp)
                    }
                    TextButton(onClick = { onDelete(day) }, modifier = Modifier.width(0.dp).weight(0.4f)) {
                        Text("✕", fontSize = 13.sp, color = MtkColors.Negative)
                    }
                }
            }
        }
    }
}

// ---- Tək gün modalı -----------------------------------------------------------

@Composable
private fun CalendarDayModal(
    year: Int,
    day: HrCalendarDayDto?,
    initialDate: String?,
    defaultSchedule: String,
    onDismiss: () -> Unit,
    onDelete: (HrCalendarDayDto) -> Unit,
    onSaved: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf(day?.name.orEmpty()) }
    var date by remember { mutableStateOf(initialDate ?: day?.date?.take(10) ?: "$year-01-01") }
    var dayType by remember { mutableStateOf(day?.dayType ?: 1) }
    var reason by remember { mutableStateOf(day?.reason.orEmpty()) }
    var schedule by remember { mutableStateOf(day?.applicableWorkingDays?.toString() ?: defaultSchedule) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MtkModal(
        title = if (day != null) "Təqvim gününü redaktə et" else "Yeni təqvim günü",
        onDismiss = onDismiss,
        confirmLabel = "Yadda saxla",
        confirmEnabled = !saving,
        confirmLoading = saving,
        onConfirm = {
            if (name.isBlank()) { error = "Ad doldurulmalıdır."; return@MtkModal }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    val body = HrCalendarDayBody(
                        name = name.trim(),
                        date = date,
                        dayType = dayType,
                        reason = reason.trim().takeIf { it.isNotEmpty() },
                        applicableWorkingDays = schedule.toIntOrNull(),
                    )
                    if (day != null) HrApi.repository.updateCalendarDay(day.id, body)
                    else HrApi.repository.createCalendarDay(body)
                }
                    .onSuccess { onSaved() }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        Text("Gün növü", style = MaterialTheme.typography.labelSmall, color = MtkColors.Muted)
        Spacer(Modifier.height(6.dp))
        CALENDAR_TYPE_INFO.forEach { (type, info) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (dayType == type) info.third else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (dayType == type) MtkColors.Green else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { dayType = type }
                    .padding(10.dp),
            ) {
                Column {
                    Text(info.first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(info.second, style = MaterialTheme.typography.labelSmall, color = MtkColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        MtkDateField(
            value = runCatching { LocalDate.parse(date) }.getOrNull(),
            onValueChange = { date = it.toString() },
            label = "Tarix",
            required = true,
        )
        MtkTextField(name, { name = it }, "Ad", required = true, placeholder = "Məs. Novruz bayramı")
        MtkSelect(
            value = schedule,
            onValueChange = { schedule = it },
            label = "Hansı iş qrafikinə tətbiq olunur",
            options = listOf(
                MtkOption("", "Hamıya"),
                MtkOption("1", "Yalnız 5 günlük"),
                MtkOption("2", "Yalnız 6 günlük"),
            ),
        )
        MtkTextField(reason, { reason = it }, "Səbəb (ixtiyari)")
        if (day != null) {
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Sil", onClick = { onDelete(day) })
        }
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---- Toplu əlavə modalı ---------------------------------------------------------

private data class BulkRow(
    val name: String = "",
    val from: LocalDate,
    val to: LocalDate? = null,
    val dayType: Int = 1,
)

@Composable
private fun BulkCalendarModal(
    year: Int,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var rows by remember { mutableStateOf(listOf(BulkRow(from = LocalDate.of(year, 1, 1)))) }
    var schedule by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val totalDays = rows.sumOf { row ->
        val end = row.to ?: row.from
        java.time.temporal.ChronoUnit.DAYS.between(row.from, end).toInt() + 1
    }

    MtkModal(
        title = "Təqvim günlərini toplu əlavə et",
        onDismiss = onDismiss,
        confirmLabel = "Əlavə et ($totalDays gün)",
        confirmEnabled = !saving && totalDays > 0,
        confirmLoading = saving,
        onConfirm = {
            if (rows.any { it.to != null && it.to!! < it.from }) {
                error = "Bitmə tarixi başlanğıc tarixindən əvvəl ola bilməz"
                return@MtkModal
            }
            saving = true
            error = null
            scope.launch {
                runCatching {
                    val days = rows.flatMap { row ->
                        val end = row.to ?: row.from
                        (0..java.time.temporal.ChronoUnit.DAYS.between(row.from, end)).map { offset ->
                            HrCalendarDayBody(
                                name = row.name.trim(),
                                date = row.from.plusDays(offset).toString(),
                                dayType = row.dayType,
                                reason = null,
                                applicableWorkingDays = schedule.toIntOrNull(),
                            )
                        }
                    }
                    HrApi.repository.bulkCreateCalendarDays(days)
                }
                    .onSuccess { result ->
                        val skipped = result.skippedDates.orEmpty().size
                        onSaved("${result.createdCount ?: 0} gün əlavə edildi${if (skipped > 0) ", $skipped gün artıq mövcud olduğu üçün atlandı" else ""}.")
                    }
                    .onFailure { error = it.toUserMessage(); saving = false }
            }
        },
    ) {
        MtkSelect(
            value = schedule,
            onValueChange = { schedule = it },
            label = "Hansı iş qrafikinə tətbiq olunur",
            options = listOf(
                MtkOption("", "Hamıya"),
                MtkOption("1", "Yalnız 5 günlük"),
                MtkOption("2", "Yalnız 6 günlük"),
            ),
        )
        Text("Artıq mövcud tarixlər atlanır.", style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft)

        rows.forEachIndexed { index, row ->
            Column(Modifier.padding(vertical = 6.dp)) {
                MtkTextField(
                    row.name,
                    { value -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(name = value) else r } },
                    "Ad",
                    required = true,
                    placeholder = "Məs. Novruz bayramı",
                )
                MtkDateField(
                    value = row.from,
                    onValueChange = { value -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(from = value) else r } },
                    label = "Başlanğıc",
                    required = true,
                )
                MtkDateField(
                    value = row.to,
                    onValueChange = { value -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(to = value) else r } },
                    label = "Bitmə (boşdursa 1 gün)",
                )
                MtkSelect(
                    value = row.dayType.toString(),
                    onValueChange = { value ->
                        rows = rows.mapIndexed { i, r -> if (i == index) r.copy(dayType = value.toIntOrNull() ?: 1) else r }
                    },
                    label = "Növ",
                    options = CALENDAR_TYPE_INFO.map { (type, info) -> MtkOption(type.toString(), info.first) },
                )
                if (rows.size > 1) {
                    SecondaryButton(
                        text = "Sətri sil",
                        onClick = { rows = rows.filterIndexed { i, _ -> i != index } },
                    )
                }
            }
        }
        SecondaryButton(
            text = "+ Sətir əlavə et",
            onClick = { rows = rows + BulkRow(from = LocalDate.of(year, 1, 1)) },
        )
        error?.let {
            Text(it, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
