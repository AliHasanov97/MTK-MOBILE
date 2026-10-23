package az.mtk.app.ui.screens.hr

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrPayslipDto
import az.mtk.app.data.api.HrVacationPayDto
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.DocumentPreviewDialog
import az.mtk.app.ui.components.PreviewDocument
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentSubTab(val label: String) {
    PAYSLIP("Maaş ödənişləri"),
    VACATION("Məzuniyyət ödənişləri"),
}

data class HrPayTarget(val kind: PaymentSubTab, val payrollRunId: String?, val paymentId: String)

class EmployeePaymentsViewModel : ViewModel() {

    private val repo = HrApi.repository

    data class UiState(
        val subTab: PaymentSubTab = PaymentSubTab.PAYSLIP,
        val payslips: List<HrPayslipDto>? = null,
        val vacationPays: List<HrVacationPayDto>? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val payingId: String? = null,
        val payTarget: HrPayTarget? = null,
        val method: Int = 0,
        val methodSaving: Boolean = false,
        val methodError: String? = null,
        val actionMessage: String? = null,
        val actionError: String? = null,
        val preview: PreviewDocument? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load(employeeId: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                Pair(repo.payslips(employeeId), repo.vacationPayList(employeeId))
            }.onSuccess { (payslips, vacation) ->
                _state.value = _state.value.copy(
                    payslips = payslips.sortedByDescending { (it.year ?: 0) * 100 + (it.month ?: 0) },
                    vacationPays = vacation.sortedByDescending { it.createdAt.orEmpty() },
                    loading = false,
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(error = failure.toUserMessage(), loading = false)
            }
        }
    }

    fun selectTab(tab: PaymentSubTab) {
        _state.value = _state.value.copy(subTab = tab)
    }

    fun openPay(target: HrPayTarget) {
        _state.value = _state.value.copy(payTarget = target, method = 0, methodError = null)
    }

    fun closePay() {
        _state.value = _state.value.copy(payTarget = null, methodSaving = false, methodError = null)
    }

    fun setMethod(value: Int) {
        _state.value = _state.value.copy(method = value)
    }

    fun confirmPay(employeeId: String) {
        val s = _state.value
        val target = s.payTarget ?: return
        _state.value = s.copy(methodSaving = true, methodError = null)
        viewModelScope.launch {
            runCatching {
                when (target.kind) {
                    PaymentSubTab.PAYSLIP -> repo.payPayslip(
                        payrollRunId = target.payrollRunId.orEmpty(),
                        employeeId = employeeId,
                        paymentMethod = s.method,
                    )
                    PaymentSubTab.VACATION -> repo.payVacationPay(target.paymentId, s.method)
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    methodSaving = false,
                    payTarget = null,
                    actionMessage = "Ödəniş qeydə alındı.",
                )
                load(employeeId)
            }.onFailure { failure ->
                _state.value = _state.value.copy(methodSaving = false, methodError = failure.toUserMessage())
            }
        }
    }

    fun exportPayslip(context: Context, runId: String, employeeId: String) {
        viewModelScope.launch {
            runCatching {
                FileRepository.downloadToInternal(
                    context = context,
                    path = "api/payroll-runs/$runId/payslips/$employeeId/export",
                    fallbackName = "qebz.pdf",
                )
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    preview = PreviewDocument(
                        filePath = result.file.absolutePath,
                        fileName = result.fileName,
                        mimeHint = "application/pdf",
                        title = "Maaş qəbzi",
                    ),
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(actionError = failure.toUserMessage())
            }
        }
    }

    fun closePreview() {
        _state.value = _state.value.copy(preview = null)
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }
}

@Composable
internal fun EmployeePaymentsSection(
    employeeId: String,
    employeeName: String,
    viewModel: EmployeePaymentsViewModel = mtkViewModel { EmployeePaymentsViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(employeeId) { viewModel.load(employeeId) }

    Column(modifier = Modifier.fillMaxSize()) {
        HrPillRow(
            pills = PaymentSubTab.entries.map { tab ->
                HrPill(tab.label, state.subTab == tab) { viewModel.selectTab(tab) }
            },
        )
        Spacer(Modifier.height(6.dp))

        state.actionMessage?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearFeedback() } }
        state.actionError?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearFeedback() } }

        when {
            state.loading -> HrLoading()
            state.error != null -> HrErrorState(state.error!!) { viewModel.load(employeeId) }
            state.subTab == PaymentSubTab.PAYSLIP -> {
                val payslips = state.payslips.orEmpty()
                if (payslips.isEmpty()) {
                    HrEmptyState("Maaş ödənişi yoxdur", "$employeeName üçün hələ qəbz yaranmayıb.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 20.dp,
                            vertical = 2.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(payslips, key = { "${it.payrollRunId}-${it.year}-${it.month}" }) { payslip ->
                            PayslipCard(
                                payslip = payslip,
                                onExport = {
                                    val runId = payslip.payrollRunId ?: return@PayslipCard
                                    viewModel.exportPayslip(context, runId, employeeId)
                                },
                                onPay = {
                                    val runId = payslip.payrollRunId ?: return@PayslipCard
                                    viewModel.openPay(HrPayTarget(PaymentSubTab.PAYSLIP, runId, runId))
                                },
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
            else -> {
                val vacationPays = state.vacationPays.orEmpty()
                if (vacationPays.isEmpty()) {
                    HrEmptyState("Məzuniyyət ödənişi yoxdur", "$employeeName üçün hələ ödəniş yaranmayıb.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 20.dp,
                            vertical = 2.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(vacationPays, key = { it.id }) { pay ->
                            VacationPayCard(
                                pay = pay,
                                onPay = { viewModel.openPay(HrPayTarget(PaymentSubTab.VACATION, null, pay.id)) },
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }

    state.preview?.let { document ->
        DocumentPreviewDialog(document = document, onClose = viewModel::closePreview)
    }

    state.payTarget?.let { target ->
        AppSheet(onDismissRequest = viewModel::closePay, scrollable = true) {
            HrSheetTitle("Ödəniş üsulu", viewModel::closePay)
            Text(
                text = "Ödənişi hansı üsulla qeyd etmək istəyirsiniz?",
                fontSize = 12.sp,
                color = HrColors.MutedSoft,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            HrSelectField(
                label = "Üsul",
                value = state.method.toString(),
                options = HrEnums.PAYMENT_METHODS.map { it.value.toString() to it.label },
                onSelect = { viewModel.setMethod(it.toIntOrNull() ?: 0) },
            )
            state.methodError?.let {
                Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HrSecondaryButton("Ləğv et", viewModel::closePay, enabled = !state.methodSaving)
                HrPrimaryButton(
                    text = if (state.methodSaving) "Qeydə alınır…" else "Ödə",
                    onClick = { viewModel.confirmPay(employeeId) },
                    enabled = !state.methodSaving,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PayslipCard(
    payslip: HrPayslipDto,
    onExport: () -> Unit,
    onPay: () -> Unit,
) {
    val paid = payslip.paidAt != null
    HrCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${HrEnums.month(payslip.month)} ${payslip.year ?: ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                )
                Text(
                    text = hrPayrollStatus(payslip.payrollRunStatus),
                    fontSize = 10.sp,
                    color = HrColors.MutedSoft,
                )
            }
            Text(
                text = Format.money(payslip.finalNetSalary),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        HrKeyValueGrid(
            listOf(
                "Baza maaş" to Format.money(payslip.baseSalary),
                "Bonus" to Format.money(payslip.bonusAmount),
                "Düzəliş" to Format.money(payslip.adjustmentAmount),
                "Ödənilib" to if (paid) Format.date(payslip.paidAt) else "—",
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HrSecondaryButton("Qəbzə bax", onExport)
            if (!paid) HrPrimaryButton("Ödə", onPay)
        }
    }
}

@Composable
private fun VacationPayCard(pay: HrVacationPayDto, onPay: () -> Unit) {
    HrCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${Format.date(pay.vacationStartDate)} – ${Format.date(pay.vacationEndDate)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                )
                Text(
                    text = "${Format.number(pay.vacationDays)} gün · ${hrPaymentStatus(pay.status)}",
                    fontSize = 10.sp,
                    color = HrColors.MutedSoft,
                )
            }
            Text(
                text = Format.money(pay.totalAmount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HrColors.Ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        HrKeyValueGrid(
            listOf(
                "Orta aylıq əmək haqqı" to Format.money(pay.averageMonthlyWage),
                "Orta günlük" to Format.money(pay.averageDailyWage),
                "Son ödəniş tarixi" to Format.date(pay.dueDate),
                "Ödənilib" to if (pay.paidAt != null) Format.date(pay.paidAt) else "—",
            ),
        )
        if (pay.status == 1) {
            Spacer(Modifier.height(8.dp))
            HrPrimaryButton("Ödə", onPay)
        }
    }
}
