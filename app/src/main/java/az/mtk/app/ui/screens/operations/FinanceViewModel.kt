package az.mtk.app.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.BillingPeriod
import az.mtk.app.data.api.FinCreatePaymentRequest
import az.mtk.app.data.api.FinCreateRateRequest
import az.mtk.app.data.api.FinCreateTransactionRequest
import az.mtk.app.data.api.FinPaymentResponse
import az.mtk.app.data.api.FinRateResponse
import az.mtk.app.data.api.FinTransactionDirection
import az.mtk.app.data.api.FinTransactionResponse
import az.mtk.app.data.api.OpsOneTimeExpenseRequest
import az.mtk.app.data.api.OpsPayrollRunDto
import az.mtk.app.data.api.OpsVacationPayDto
import az.mtk.app.data.api.OperationsRepository
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.PartyRow
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Maliyyə əməliyyatları bölməsinin alt səhifələri (saytdakı hub ilə eyni). */
enum class FinanceSection(val label: String, val description: String) {
    PAYMENTS("Ödənişlərin daxil edilməsi", "Sakin ödənişlərini qeydə alın"),
    EXPENSES("Xərclərin daxil edilməsi", "Müqavilə xidməti və ya sərbəst xərc"),
    INCOMES("Əlavə gəlirlər", "Müqavilə ilə bağlı olmayan gəlirlər"),
    RATES("Tariflər", "Aylıq haqq generasiyasının əsası"),
    PURCHASES("Satınalmalar", "Mal alışları və qaimələr"),
    PAYROLL("Əmək haqqı", "Aylıq əmək haqqı hesablamaları"),
    VACATION("Məzuniyyət ödənişləri", "Yaxınlaşan və ödənilmiş məzuniyyət haqları"),
}

/** Birdəfəlik müqavilə xidməti — xərc formasında seçim üçün. */
data class OneTimeServiceOption(
    val contractId: String,
    val contractNumber: String,
    val serviceId: String,
    val serviceName: String,
    val unitPrice: Double,
)

/** Əmək haqqı ekranı ilk açılanda göstərilən dövr — keçən ay. */
private val DEFAULT_PAYROLL_PERIOD: YearMonth = YearMonth.now().minusMonths(1)

class FinanceViewModel : ViewModel() {

    private val repo = OperationsRepository
    private val defaultPeriod = DEFAULT_PAYROLL_PERIOD

    data class UiState(
        val section: FinanceSection? = null,

        // Ödənişlər
        val payments: List<FinPaymentResponse> = emptyList(),
        val paymentsLoading: Boolean = false,
        val paymentsError: String? = null,
        val ownerOptions: List<OwnerItemDto> = emptyList(),
        val ownersLoading: Boolean = false,
        val ownersError: String? = null,
        val selectedOwner: OwnerItemDto? = null,
        val selectedOwnerDetail: OwnerDetailDto? = null,
        val ownerDetailLoading: Boolean = false,

        // Tranzaksiyalar (xərc / gəlir)
        val transactions: List<FinTransactionResponse> = emptyList(),
        val transactionsLoading: Boolean = false,
        val transactionsError: String? = null,

        // Tariflər
        val rates: List<FinRateResponse> = emptyList(),
        val ratesLoading: Boolean = false,
        val ratesError: String? = null,

        // Əmək haqqı
        val payroll: OpsPayrollRunDto? = null,
        val payrollLoading: Boolean = false,
        val payrollError: String? = null,
        val payrollYear: Int = DEFAULT_PAYROLL_PERIOD.year,
        val payrollMonth: Int = DEFAULT_PAYROLL_PERIOD.monthValue,

        // Məzuniyyət
        val vacation: List<OpsVacationPayDto> = emptyList(),
        val vacationLoading: Boolean = false,
        val vacationError: String? = null,

        // Müqavilə xidməti xərci
        val expenseVendors: List<PartyRow> = emptyList(),
        val expenseVendorsLoading: Boolean = false,
        val expenseServices: List<OneTimeServiceOption> = emptyList(),
        val expenseServicesLoading: Boolean = false,

        // Ümumi
        val saving: Boolean = false,
        val formError: String? = null,
        val message: String? = null,
        val actionError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var ownerRequest = 0

    // ---- Bölmə naviqasiyası -------------------------------------------------

    fun openSection(section: FinanceSection) {
        _state.value = _state.value.copy(
            section = section,
            message = null,
            actionError = null,
            formError = null,
        )
        when (section) {
            FinanceSection.PAYMENTS -> if (_state.value.payments.isEmpty()) loadPayments()
            FinanceSection.EXPENSES -> {
                loadTransactions(FinTransactionDirection.EXPENSE)
                loadExpenseVendors()
            }
            FinanceSection.INCOMES -> loadTransactions(FinTransactionDirection.INCOME)
            FinanceSection.RATES -> if (_state.value.rates.isEmpty()) loadRates()
            FinanceSection.PURCHASES -> Unit
            FinanceSection.PAYROLL -> loadPayroll(_state.value.payrollYear, _state.value.payrollMonth)
            FinanceSection.VACATION -> if (_state.value.vacation.isEmpty()) loadVacation()
        }
    }

    fun closeSection() {
        ownerRequest++
        _state.value = _state.value.copy(
            section = null,
            selectedOwner = null,
            selectedOwnerDetail = null,
            formError = null,
            message = null,
            actionError = null,
        )
    }

    fun refresh() {
        when (_state.value.section) {
            FinanceSection.PAYMENTS -> loadPayments()
            FinanceSection.EXPENSES -> loadTransactions(FinTransactionDirection.EXPENSE)
            FinanceSection.INCOMES -> loadTransactions(FinTransactionDirection.INCOME)
            FinanceSection.RATES -> loadRates()
            FinanceSection.PURCHASES -> Unit
            FinanceSection.PAYROLL -> loadPayroll(_state.value.payrollYear, _state.value.payrollMonth)
            FinanceSection.VACATION -> loadVacation()
            null -> Unit
        }
    }

    // ---- Ödənişlər ----------------------------------------------------------

    fun loadPayments() {
        _state.value = _state.value.copy(paymentsLoading = true, paymentsError = null)
        viewModelScope.launch {
            try {
                val page = repo.financePayments(0, pageSize = 30)
                _state.value = _state.value.copy(payments = page.items, paymentsLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(paymentsLoading = false, paymentsError = t.toUserMessage())
            }
        }
    }

    fun searchOwners(query: String) {
        _state.value = _state.value.copy(ownersLoading = true, ownersError = null)
        viewModelScope.launch {
            try {
                val page = repo.owners(0, pageSize = 50, search = query.trim().ifBlank { null })
                _state.value = _state.value.copy(ownerOptions = page.items, ownersLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(ownersLoading = false, ownersError = t.toUserMessage())
            }
        }
    }

    fun selectOwner(owner: OwnerItemDto?) {
        if (owner == null) {
            ownerRequest++
            _state.value = _state.value.copy(selectedOwner = null, selectedOwnerDetail = null)
            return
        }
        val request = ++ownerRequest
        _state.value = _state.value.copy(
            selectedOwner = owner,
            selectedOwnerDetail = null,
            ownerDetailLoading = true,
        )
        viewModelScope.launch {
            try {
                val detail = repo.owner(owner.id)
                if (request != ownerRequest) return@launch
                _state.value = _state.value.copy(selectedOwnerDetail = detail, ownerDetailLoading = false)
            } catch (t: Throwable) {
                if (request != ownerRequest) return@launch
                _state.value = _state.value.copy(
                    ownerDetailLoading = false,
                    formError = t.toUserMessage(),
                )
            }
        }
    }

    fun resetOwner() {
        ownerRequest++
        _state.value = _state.value.copy(selectedOwner = null, selectedOwnerDetail = null, formError = null)
    }

    fun createPayment(
        amount: Double,
        method: Int,
        notes: String?,
        apartmentId: String?,
        garageId: String?,
        onDone: () -> Unit = {},
    ) {
        val ownerId = _state.value.selectedOwner?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null, message = null)
            try {
                repo.createFinancePayment(
                    FinCreatePaymentRequest(
                        ownerId = ownerId,
                        amount = amount,
                        paymentMethod = method,
                        notes = notes?.trim()?.ifBlank { null },
                        apartmentId = apartmentId,
                        garageId = garageId,
                    ),
                )
                _state.value = _state.value.copy(saving = false, message = "Ödəniş qeydə alındı")
                onDone()
                loadPayments()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    // ---- Tranzaksiyalar -----------------------------------------------------

    fun loadTransactions(direction: Int) {
        _state.value = _state.value.copy(transactionsLoading = true, transactionsError = null)
        viewModelScope.launch {
            try {
                val page = repo.transactions(0, pageSize = 40, direction = direction)
                _state.value = _state.value.copy(transactions = page.items, transactionsLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(transactionsLoading = false, transactionsError = t.toUserMessage())
            }
        }
    }

    fun createTransaction(
        direction: Int,
        category: String,
        amount: Double,
        description: String?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null, message = null)
            try {
                repo.createTransaction(
                    FinCreateTransactionRequest(
                        direction = direction,
                        category = category.trim(),
                        amount = amount,
                        description = description?.trim()?.ifBlank { null },
                    ),
                )
                _state.value = _state.value.copy(
                    saving = false,
                    message = "${category.trim()} — ${amount} ₼ qeydə alındı",
                )
                onDone()
                loadTransactions(direction)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    // ---- Müqavilə xidməti xərci --------------------------------------------

    fun loadExpenseVendors() {
        if (_state.value.expenseVendors.isNotEmpty() || _state.value.expenseVendorsLoading) return
        _state.value = _state.value.copy(expenseVendorsLoading = true)
        viewModelScope.launch {
            try {
                val page = repo.vendors(0, pageSize = 200)
                _state.value = _state.value.copy(
                    expenseVendors = page.items.filter { it.isActive },
                    expenseVendorsLoading = false,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    expenseVendorsLoading = false,
                    formError = t.toUserMessage(),
                )
            }
        }
    }

    fun loadExpenseServices(vendorId: String) {
        if (vendorId.isBlank()) {
            _state.value = _state.value.copy(expenseServices = emptyList())
            return
        }
        _state.value = _state.value.copy(expenseServicesLoading = true, expenseServices = emptyList())
        viewModelScope.launch {
            try {
                val options = coroutineScope {
                    val contracts = repo.contractsByVendor(vendorId)
                    contracts.map { item ->
                        async {
                            runCatching { repo.contract(item.id) }.getOrNull()
                        }
                    }.awaitAll().filterNotNull().flatMap { contract ->
                        contract.services.orEmpty()
                            .filter { it.isActive != false && it.billingPeriod == BillingPeriod.ONE_TIME }
                            .map { service ->
                                OneTimeServiceOption(
                                    contractId = contract.id,
                                    contractNumber = contract.number,
                                    serviceId = service.id,
                                    serviceName = service.name,
                                    unitPrice = service.unitPrice ?: 0.0,
                                )
                            }
                    }
                }
                _state.value = _state.value.copy(expenseServices = options, expenseServicesLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    expenseServicesLoading = false,
                    formError = t.toUserMessage(),
                )
            }
        }
    }

    fun createOneTimeExpense(
        contractId: String,
        serviceId: String,
        amount: Double,
        method: Int,
        notes: String?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null, message = null)
            try {
                repo.createOneTimeServiceExpense(
                    OpsOneTimeExpenseRequest(contractId, serviceId, amount, method, notes?.trim()?.ifBlank { null }),
                )
                _state.value = _state.value.copy(saving = false, message = "Xərc qeydə alındı və ödənildi")
                onDone()
                loadTransactions(FinTransactionDirection.EXPENSE)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    // ---- Tariflər -----------------------------------------------------------

    fun loadRates() {
        _state.value = _state.value.copy(ratesLoading = true, ratesError = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(rates = repo.currentRates(), ratesLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(ratesLoading = false, ratesError = t.toUserMessage())
            }
        }
    }

    fun createRate(
        rateType: Int,
        amount: Double,
        effectiveFrom: String,
        description: String?,
        garageType: Int?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null, message = null)
            try {
                repo.createRate(
                    FinCreateRateRequest(
                        rateType = rateType,
                        amount = amount,
                        effectiveFrom = effectiveFrom,
                        description = description?.trim()?.ifBlank { null },
                        garageType = garageType,
                    ),
                )
                _state.value = _state.value.copy(saving = false, message = "Tarif yaradıldı")
                onDone()
                loadRates()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, formError = t.toUserMessage())
            }
        }
    }

    // ---- Əmək haqqı ---------------------------------------------------------

    fun loadPayroll(year: Int, month: Int) {
        _state.value = _state.value.copy(
            payrollYear = year,
            payrollMonth = month,
            payrollLoading = true,
            payrollError = null,
        )
        viewModelScope.launch {
            try {
                val run = repo.payroll(year, month)
                _state.value = _state.value.copy(payroll = run, payrollLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(payrollLoading = false, payrollError = t.toUserMessage())
            }
        }
    }

    fun calculatePayroll() {
        val s = _state.value
        mutatePayroll { repo.calculatePayroll(s.payrollYear, s.payrollMonth) }
    }

    fun recalculatePayroll() {
        val id = _state.value.payroll?.id ?: return
        mutatePayroll { repo.recalculatePayroll(id) }
    }

    fun approvePayroll() {
        val id = _state.value.payroll?.id ?: return
        mutatePayroll { repo.approvePayroll(id) }
    }

    fun payAllPayslips(method: Int) {
        val id = _state.value.payroll?.id ?: return
        mutatePayroll { repo.payAllPayslips(id, method) }
    }

    fun payPayslip(employeeId: String, method: Int) {
        val id = _state.value.payroll?.id ?: return
        mutatePayroll { repo.payPayslip(id, employeeId, method) }
    }

    private fun mutatePayroll(block: suspend () -> OpsPayrollRunDto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, payrollError = null, message = null)
            try {
                val run = block()
                _state.value = _state.value.copy(payroll = run, saving = false, message = "Əmək haqqı yeniləndi")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, payrollError = t.toUserMessage())
            }
        }
    }

    // ---- Məzuniyyət ---------------------------------------------------------

    fun loadVacation() {
        _state.value = _state.value.copy(vacationLoading = true, vacationError = null)
        viewModelScope.launch {
            try {
                val page = repo.vacationPays(0, pageSize = 50)
                _state.value = _state.value.copy(vacation = page.items, vacationLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(vacationLoading = false, vacationError = t.toUserMessage())
            }
        }
    }

    fun payVacation(id: String, method: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, actionError = null, message = null)
            try {
                repo.payVacationPay(id, method)
                _state.value = _state.value.copy(saving = false, message = "Məzuniyyət ödənişi icra olundu")
                loadVacation()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, actionError = t.toUserMessage())
            }
        }
    }

    fun clearFormError() {
        _state.value = _state.value.copy(formError = null)
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, actionError = null)
    }
}
