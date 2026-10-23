package az.mtk.app.ui.screens.hr

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrEmployeeTimesheetDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrTimesheetDayDto
import az.mtk.app.data.api.HrTimesheetDto
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HrTimesheetViewModel : ViewModel() {

    private val repo = HrApi.repository
    private val today = LocalDate.now()

    data class UiState(
        val year: Int = LocalDate.now().year,
        val month: Int = LocalDate.now().monthValue,
        val sheet: HrTimesheetDto? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val exporting: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.timesheet(s.year, s.month) }
                .onSuccess { sheet -> _state.value = _state.value.copy(sheet = sheet, loading = false) }
                .onFailure { failure -> _state.value = _state.value.copy(error = failure.toUserMessage(), loading = false) }
        }
    }

    fun previousMonth() = shift(-1)
    fun nextMonth() = shift(1)

    fun thisMonth() {
        _state.value = _state.value.copy(year = today.year, month = today.monthValue, sheet = null)
        load()
    }

    fun setMonth(month: Int) {
        if (month !in 1..12) return
        _state.value = _state.value.copy(month = month, sheet = null)
        load()
    }

    fun setYear(year: Int) {
        if (year !in 2000..2100) return
        _state.value = _state.value.copy(year = year, sheet = null)
        load()
    }

    private fun shift(delta: Int) {
        val base = LocalDate.of(_state.value.year, _state.value.month, 1).plusMonths(delta.toLong())
        _state.value = _state.value.copy(year = base.year, month = base.monthValue, sheet = null)
        load()
    }

    fun setExporting(value: Boolean) {
        _state.value = _state.value.copy(exporting = value)
    }

    fun showMessage(value: String?) {
        _state.value = _state.value.copy(message = value)
    }

    fun exportPath(): String = "api/hr/timesheets/export?year=${_state.value.year}&month=${_state.value.month}"
}

@Composable
internal fun HrTimesheetTab(viewModel: HrTimesheetViewModel = mtkViewModel { HrTimesheetViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.Default.ChevronLeft, "Əvvəlki ay", tint = HrColors.Muted)
            }
            Text(
                text = "${HrEnums.month(state.month)} ${state.year}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::thisMonth) {
                Text("Bu ay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HrColors.Terracotta)
            }
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.Default.ChevronRight, "Növbəti ay", tint = HrColors.Muted)
            }
        }

        state.message?.let { HrFeedbackBanner(it, isError = false) { viewModel.showMessage(null) } }
        state.error?.let { HrFeedbackBanner(it, isError = true) { viewModel.load() } }

        when {
            state.loading && state.sheet == null -> HrLoading()
            state.sheet == null && state.error == null -> HrLoading()
            else -> {
                val sheet = state.sheet
                if (sheet == null) {
                    HrEmptyState("Tabel alınmadı", "Ay seçimini dəyişin və ya yenidən cəhd edin.")
                } else {
                    val employees = sheet.employees.orEmpty()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimesheetStat("İşçi", employees.size.toString(), Modifier.weight(1f))
                                TimesheetStat("Təqvim günü", (sheet.daysInMonth ?: 0).toString(), Modifier.weight(1f))
                                TimesheetStat(
                                    "İş günü",
                                    employees.firstOrNull()?.normWorkingDays?.toString() ?: "—",
                                    Modifier.weight(1f),
                                )
                            }
                        }
                        item {
                            HrPrimaryButton(
                                text = if (state.exporting) "Hazırlanır…" else "Excel-ə yüklə",
                                onClick = {
                                    if (state.exporting) return@HrPrimaryButton
                                    viewModel.setExporting(true)
                                    scope.launch {
                                        runCatching {
                                            FileRepository.downloadToInternal(
                                                context = context,
                                                path = viewModel.exportPath(),
                                                fallbackName = "tabel-${state.year}-${state.month}.xlsx",
                                            )
                                        }.onSuccess { result ->
                                            val opened = FileRepository.registerAndOpen(context, result)
                                            Toast.makeText(
                                                context,
                                                if (opened) "Fayl açıldı: ${result.fileName}" else "Yükləndi: ${result.fileName}",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }.onFailure { failure ->
                                            viewModel.showMessage(failure.toUserMessage())
                                        }
                                        viewModel.setExporting(false)
                                    }
                                },
                                enabled = !state.exporting,
                            )
                        }
                        items(employees, key = { it.employee?.id ?: it.registerNumber.toString() }) { employee ->
                            TimesheetEmployeeCard(employee)
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimesheetStat(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun TimesheetEmployeeCard(employee: HrEmployeeTimesheetDto) {
    HrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.employee?.name ?: "—",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(employee.position?.name ?: "Vəzifə yoxdur")
                        employee.registerNumber?.let { append(" · №$it") }
                    },
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                )
            }
            Text(
                text = "${employee.actualWorkingDays ?: 0} gün",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrColors.Olive,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            employee.days.orEmpty().forEach { day -> TimesheetDayChip(day) }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Faktiki: ${Format.number(employee.actualWorkedHours)} saat",
                fontSize = 11.sp,
                color = HrColors.Muted,
            )
            Text(
                text = "Norma: ${Format.number(employee.normWorkedHours)} saat",
                fontSize = 11.sp,
                color = HrColors.MutedSoft,
            )
        }
        if ((employee.annualLeaveDays ?: 0) + (employee.socialLeaveDays ?: 0) +
            (employee.educationLeaveDays ?: 0) + (employee.unpaidLeaveDays ?: 0) +
            (employee.sickLeaveDays ?: 0) + (employee.businessTripDays ?: 0) > 0
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    fun part(label: String, value: Int?) {
                        if ((value ?: 0) > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("$label $value")
                        }
                    }
                    part("Məzuniyyət", employee.annualLeaveDays)
                    part("Sosial", employee.socialLeaveDays)
                    part("Təhsil", employee.educationLeaveDays)
                    part("Ödənişsiz", employee.unpaidLeaveDays)
                    part("Xəstəlik", employee.sickLeaveDays)
                    part("Ezamiyyət", employee.businessTripDays)
                },
                fontSize = 10.sp,
                color = HrColors.Blue,
            )
        }
    }
}

@Composable
private fun TimesheetDayChip(day: HrTimesheetDayDto) {
    val code = day.dayCode?.takeIf { it.isNotBlank() } ?: "—"
    val (foreground, background) = when (code) {
        "İ" -> HrColors.Olive to HrColors.OliveSoft
        "Q", "-" -> HrColors.MutedSoft to HrColors.Subtle
        "B", "H" -> HrColors.Amber to HrColors.AmberSoft
        "M", "TM", "ÖM", "Y" -> HrColors.Blue to HrColors.BlueSoft
        "X" -> HrColors.Error to HrColors.ErrorSoft
        else -> HrColors.Muted to HrColors.Subtle
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.day?.toString() ?: "—",
            fontSize = 9.sp,
            color = foreground.copy(alpha = 0.75f),
        )
        Text(code, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = foreground)
        if ((day.workedHours ?: 0.0) > 0) {
            Text(
                text = "${day.workedHours}",
                fontSize = 8.sp,
                color = foreground.copy(alpha = 0.8f),
            )
        }
    }
}
