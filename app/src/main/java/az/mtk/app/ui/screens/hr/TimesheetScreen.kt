package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import az.mtk.app.data.api.HrEmployeeTimesheetDto
import az.mtk.app.data.api.HrTimesheetDto
import az.mtk.app.data.api.HrTimesheetDayDto
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TimesheetState(
    val sheet: HrTimesheetDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class TimesheetViewModel : ViewModel() {
    val now = LocalDate.now()

    private val _year = MutableStateFlow(now.year)
    val year = _year.asStateFlow()

    private val _month = MutableStateFlow(now.monthValue)
    val month = _month.asStateFlow()

    private val _state = MutableStateFlow(TimesheetState())
    val state: StateFlow<TimesheetState> = _state.asStateFlow()

    init {
        load()
    }

    fun setYear(value: Int) {
        if (value !in 2000..2100) return
        _year.value = value
        load()
    }

    fun setMonth(value: Int) {
        _month.value = value
        load()
    }

    fun shift(delta: Int) {
        val date = LocalDate.of(_year.value, _month.value, 1).plusMonths(delta.toLong())
        _year.value = date.year
        _month.value = date.monthValue
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { HrApi.repository.timesheet(_year.value, _month.value) }
                .onSuccess { _state.value = TimesheetState(sheet = it, loading = false) }
                .onFailure { _state.value = TimesheetState(loading = false, error = it.toUserMessage()) }
        }
    }
}

/** Tabel gün kodlarına uyğun rənglər (DavamiyyetView tb-* sinifləri). */
private fun codeColor(code: String?): Color = when (code) {
    "İ" -> Color(0xFFE1F2D3)
    "Q" -> Color(0xFFE6E9E4)
    "B", "H" -> Color(0xFFFDE3E3)
    "M" -> Color(0xFFDDF0FA)
    "TM" -> Color(0xFFE7E3F7)
    "ÖM" -> Color(0xFFFBF1DC)
    "Y" -> Color(0xFFFCE7EF)
    "X" -> Color(0xFFE3F4F4)
    "E" -> Color(0xFFEFF1E0)
    "İG" -> Color(0xFFFAE3E3)
    "-" -> Color(0xFFF5F5F3)
    else -> Color(0xFFDDF0FA)
}

private fun codeTextColor(code: String?): Color = when (code) {
    "İ" -> Color(0xFF4B7A2A)
    "B", "H", "İG" -> Color(0xFFB0453A)
    else -> MtkColors.Muted
}

private val WEEKDAY_SHORT = listOf("B", "B.e", "Ç.a", "Çər", "C.a", "Cüm", "Şən")

@Composable
fun TimesheetScreen(
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false,
    viewModel: TimesheetViewModel = mtkViewModel { TimesheetViewModel() },
) {
    val year by viewModel.year.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(title = "Tabel", subtitle = "${HrOptions.months[month - 1].label} $year", onBack = onBack)
        }

        // Dövr seçimi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { viewModel.shift(-1) }) { Text("‹") }
            MtkSelect(
                value = month.toString(),
                onValueChange = { viewModel.setMonth(it.toInt()) },
                label = "Ay",
                options = HrOptions.months.map { MtkOption(it.value, it.label) },
                modifier = Modifier.weight(1f),
            )
            MtkSelect(
                value = year.toString(),
                onValueChange = { viewModel.setYear(it.toIntOrNull() ?: year) },
                label = "İl",
                options = (LocalDate.now().year + 1 downTo 2000).map { MtkOption(it.toString(), it.toString()) },
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.shift(1) }) { Text("›") }
            TextButton(onClick = {
                viewModel.setYear(LocalDate.now().year)
                viewModel.setMonth(LocalDate.now().monthValue)
            }) { Text("Bu ay") }
        }

        when {
            state.loading -> LoadingState("Tabel yüklənir…")
            state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
            state.sheet == null -> LoadingState()
            else -> {
                val sheet = state.sheet!!
                val weekendCount = (1..(sheet.daysInMonth ?: 0)).count {
                    val wd = LocalDate.of(year, month, it).dayOfWeek.value
                    wd == 6 || wd == 7
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Stat("İşçi", sheet.employees?.size ?: 0)
                    Stat("Təqvim günü", sheet.daysInMonth ?: 0)
                    Stat("Şənbə/Bazar", weekendCount)
                    Stat("Norma", sheet.employees?.firstOrNull()?.normWorkingDays ?: 0)
                }
                Spacer(Modifier.height(4.dp))

                MtkCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    padding = 4.dp,
                ) {
                    ExportActionRow(
                        title = "Excel-ə yüklə",
                        subtitle = "Tabel cədvəli (Excel)",
                        path = "api/hr/timesheets/export?year=$year&month=$month",
                        scope = scope,
                    )
                }
                Spacer(Modifier.height(8.dp))

                // İşçi sətirləri — üfüqi sürüşən gün qreydi
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    DayHeaderRow(sheet.daysInMonth ?: 0, year, month)
                    if (sheet.employees.isNullOrEmpty()) {
                        MtkCard { Text("Bu ay üçün işçi yoxdur.", color = MtkColors.Muted) }
                    }
                    sheet.employees.orEmpty().forEach { row ->
                        EmployeeTimesheetRow(row)
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: Int) {
    Column(Modifier.weight(1f)) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft)
    }
}

private fun dayInfo(year: Int, month: Int, day: Int): Triple<Int, Boolean, LocalDate> {
    val date = LocalDate.of(year, month, day)
    val wd = date.dayOfWeek.value
    return Triple(wd % 7, wd == 6 || wd == 7, date)
}

@Composable
private fun DayHeaderRow(daysInMonth: Int, year: Int, month: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(Modifier.width(120.dp)) { Text("İşçi", style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft) }
        (1..daysInMonth).forEach { day ->
            val (wd, weekend, _) = dayInfo(year, month, day)
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (weekend) MtkColors.Warning else MtkColors.Muted,
                )
                Text(
                    WEEKDAY_SHORT[wd],
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (weekend) MtkColors.Warning else MtkColors.MutedSoft,
                )
            }
        }
    }
}

@Composable
private fun EmployeeTimesheetRow(row: HrEmployeeTimesheetDto) {
    val byDay = row.days.orEmpty().associateBy { it.day }
    MtkCard(padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(120.dp)) {
                Text(
                    row.employee?.name ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    row.position?.name ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedSoft,
                    maxLines = 1,
                )
                Text(
                    "Tabel №${row.registerNumber ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedUltraSoft,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            byDay.keys.filterNotNull().sorted().forEach { day ->
                val cell = byDay[day] ?: HrTimesheetDayDto(day = day)
                DayCell(cell)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Norma: ${row.normWorkingDays ?: "—"}/${row.normWorkedHours ?: "—"} · " +
                "Faktiki: ${row.actualWorkingDays ?: "—"}/${row.actualWorkedHours ?: "—"} · " +
                "Məzuniyyət: ${(row.annualLeaveDays ?: 0) + (row.educationLeaveDays ?: 0) + (row.unpaidLeaveDays ?: 0) + (row.socialLeaveDays ?: 0)} gün",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
        )
    }
}

@Composable
private fun DayCell(cell: HrTimesheetDayDto) {
    val code = cell.dayCode ?: "-"
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(codeColor(code)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                code,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = codeTextColor(code),
            )
            if (code == "İ" && cell.workedHours != null) {
                Text(
                    Format.number(cell.workedHours),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = codeTextColor(code),
                )
            }
        }
    }
}

/** StatTile-ə uyğun kiçik göstərici (daxili istifadə üçün dairə). */
@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}
