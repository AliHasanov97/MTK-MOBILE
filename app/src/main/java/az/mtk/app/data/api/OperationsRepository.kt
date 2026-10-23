package az.mtk.app.data.api

import az.mtk.app.AppGraph

/**
 * Əməliyyatlar modulu üçün vahid giriş nöqtəsi. Mövcud Retrofit
 * interfeyslərini (Contracts/Vendors/Customers/Finance/Payments/Inventory)
 * bir yerdə birləşdirir və UI-nın rahat işlədiyi sadə modellərə çevirir.
 */

/** Müqavilənin tərəfi — siyahı filtrində istifadə olunur. */
enum class ContractParty(val label: String, val column: String?) {
    ALL("Hamısı", null),
    VENDOR("Tədarükçü", "vendorId"),
    CUSTOMER("Müştəri", "customerId"),
}

/** Tədarükçü və müştəri üçün ortaq siyahı modeli. */
data class PartyRow(
    val id: String,
    val name: String,
    val type: Int,
    val voen: String? = null,
    val director: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
)

/** Tədarükçü/müştəri borcu — iki fərqli DTO-nun ortaq forması. */
data class PartyChargeRow(
    val id: String,
    val partyId: String,
    val contractId: String?,
    val period: String?,
    val description: String?,
    val amount: Double,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val status: Int,
    val chargeDate: String?,
    val dueDate: String?,
    val isOverdue: Boolean,
)

/** Tədarükçü/müştəri ödənişi. */
data class PartyPaymentRow(
    val id: String,
    val amount: Double,
    val paymentMethod: Int,
    val paymentDate: String?,
    val status: Int?,
    val notes: String?,
)

object OperationsRepository {

    private val contractsApi: ContractsApi by lazy { AppGraph.service(ContractsApi::class.java) }
    private val vendorsApi: VendorsApi by lazy { AppGraph.service(VendorsApi::class.java) }
    private val vendorChargesApi: VendorChargesApi by lazy { AppGraph.service(VendorChargesApi::class.java) }
    private val customersApi: CustomersApi by lazy { AppGraph.service(CustomersApi::class.java) }
    private val customerChargesApi: CustomerChargesApi by lazy { AppGraph.service(CustomerChargesApi::class.java) }
    private val paymentsApi: PaymentsApi by lazy { AppGraph.service(PaymentsApi::class.java) }
    private val coreApi: MtkService by lazy { AppGraph.service(MtkService::class.java) }
    private val operationsApi: OperationsService by lazy { AppGraph.service(OperationsService::class.java) }

    // ---- Müqavilələr --------------------------------------------------------

    suspend fun contracts(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
        party: ContractParty = ContractParty.ALL,
    ): Page<ContractListItemDto> {
        val filters = party.column?.let {
            listOf(QueryFilter(it, Comparison.IS_NOT_NULL, null))
        }
        val env = contractsApi.search(
            SearchBody(
                filters = filters,
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Müqavilələr")
        return Page(
            payload.contracts.orEmpty(),
            payload.totalCount,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun contract(id: String): ContractDto = contractsApi.get(id).unwrap("Müqavilə")

    suspend fun createContract(request: ContractCreateRequest): String =
        contractsApi.create(request).unwrap("Müqavilə").trim('"')

    suspend fun updateContract(id: String, request: ContractUpdateRequest) {
        contractsApi.update(id, request)
    }

    suspend fun addContractService(contractId: String, request: ContractServiceRequest): String =
        contractsApi.addService(contractId, request).unwrap("Xidmət").trim('"')

    suspend fun updateContractService(contractId: String, serviceId: String, request: ContractServiceRequest) {
        contractsApi.updateService(contractId, serviceId, request)
    }

    suspend fun removeContractService(contractId: String, serviceId: String) {
        contractsApi.removeService(contractId, serviceId)
    }

    suspend fun setContractServiceStatus(contractId: String, serviceId: String, active: Boolean) {
        contractsApi.setServiceStatus(contractId, serviceId, ContractServiceStatusRequest(active))
    }

    suspend fun activateContract(id: String) {
        contractsApi.activate(id)
    }

    suspend fun suspendContract(id: String, note: String?) {
        contractsApi.suspend(id, ContractSuspendRequest(note))
    }

    suspend fun terminateContract(id: String, terminatedOn: String, note: String?) {
        contractsApi.terminate(id, ContractTerminateRequest(terminatedOn, note))
    }

    suspend fun deleteContract(id: String) {
        contractsApi.delete(id)
    }

    // ---- Tədarükçülər -------------------------------------------------------

    suspend fun vendors(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<PartyRow> {
        val env = vendorsApi.search(
            SearchBody(
                sortCriteria = SortCriteria("Name", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Tədarükçülər")
        return Page(
            payload.vendors.orEmpty().map { it.toRow() },
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun vendor(id: String): PartyRow {
        val dto = vendorsApi.get(id).unwrap("Tədarükçü")
        return PartyRow(
            id = dto.id,
            name = dto.name,
            type = dto.vendorType,
            voen = dto.voen,
            director = dto.director,
            email = dto.email,
            phone = dto.phone,
            address = dto.address,
            note = dto.note,
            isActive = dto.isActive != false,
            createdAt = dto.createdAt,
        )
    }

    suspend fun createVendor(request: VendorSaveRequest): String =
        vendorsApi.create(request).unwrap("Tədarükçü").trim('"')

    suspend fun updateVendor(id: String, request: VendorSaveRequest) {
        vendorsApi.update(id, request)
    }

    suspend fun setVendorStatus(id: String, active: Boolean) {
        vendorsApi.setStatus(id, VendorStatusRequest(active))
    }

    suspend fun deleteVendor(id: String) {
        vendorsApi.delete(id)
    }

    suspend fun vendorCharges(vendorId: String): List<PartyChargeRow> =
        vendorChargesApi.byVendor(vendorId).unwrap("Tədarükçü borcları").map { it.toRow() }

    suspend fun vendorPayments(vendorId: String): List<PartyPaymentRow> =
        vendorChargesApi.paymentsByVendor(vendorId).unwrap("Tədarükçü ödənişləri").map { it.toRow() }

    suspend fun createVendorPayment(vendorId: String, amount: Double, method: Int, notes: String?): String =
        vendorChargesApi.createPayment(VendorPaymentCreateRequest(vendorId, amount, method, notes))
            .unwrap("Ödəniş").trim('"')

    suspend fun cancelVendorCharge(id: String) {
        vendorChargesApi.cancel(id, mapOf("note" to "Ləğv edildi"))
    }

    /** Müqavilə üzrə tədarükçü borcları. */
    suspend fun vendorChargesByContract(contractId: String): List<PartyChargeRow> {
        val env = vendorChargesApi.search(
            SearchBody(
                filters = listOf(QueryFilter("contractId", Comparison.EQUALS, contractId)),
                pageSize = 100,
            ),
        )
        return env.unwrap("Müqavilə borcları").items.orEmpty().map { it.toRow() }
    }

    // ---- Müştərilər ---------------------------------------------------------

    suspend fun customers(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<PartyRow> {
        val env = customersApi.search(
            SearchBody(
                sortCriteria = SortCriteria("Name", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Müştərilər")
        return Page(
            payload.customers.orEmpty().map { it.toRow() },
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun customer(id: String): PartyRow {
        val dto = customersApi.get(id).unwrap("Müştəri")
        return PartyRow(
            id = dto.id,
            name = dto.name,
            type = dto.customerType,
            voen = dto.voen,
            director = dto.director,
            email = dto.email,
            phone = dto.phone,
            address = dto.address,
            note = dto.note,
            isActive = dto.isActive != false,
            createdAt = dto.createdAt,
        )
    }

    suspend fun customerBalance(id: String): CustomerBalanceDto =
        customersApi.balance(id).unwrap("Müştəri balansı")

    suspend fun createCustomer(request: CustomerSaveRequest): String =
        customersApi.create(request).unwrap("Müştəri").trim('"')

    suspend fun updateCustomer(id: String, request: CustomerSaveRequest) {
        customersApi.update(id, request)
    }

    suspend fun setCustomerStatus(id: String, active: Boolean) {
        customersApi.setStatus(id, CustomerStatusRequest(active))
    }

    suspend fun deleteCustomer(id: String) {
        customersApi.delete(id)
    }

    suspend fun customerCharges(customerId: String): List<PartyChargeRow> =
        customerChargesApi.byCustomer(customerId).unwrap("Müştəri borcları").map { it.toRow() }

    suspend fun customerPayments(customerId: String): List<PartyPaymentRow> =
        customerChargesApi.paymentsByCustomer(customerId).unwrap("Müştəri ödənişləri").map { it.toRow() }

    suspend fun createCustomerPayment(customerId: String, amount: Double, method: Int, notes: String?): String =
        customerChargesApi.createPayment(CustomerPaymentCreateRequest(customerId, amount, method, notes))
            .unwrap("Ödəniş").trim('"')

    suspend fun createCustomerCharge(customerId: String, description: String, amount: Double, dueDate: String?): String =
        customerChargesApi.createCharge(CustomerChargeCreateRequest(customerId, description, amount, dueDate))
            .unwrap("Borc").trim('"')

    suspend fun cancelCustomerCharge(id: String) {
        customerChargesApi.cancelCharge(id, mapOf("note" to "Ləğv edildi"))
    }

    /** Müqavilə üzrə müştəri borcları. */
    suspend fun customerChargesByContract(contractId: String): List<PartyChargeRow> {
        val env = customerChargesApi.search(
            SearchBody(
                filters = listOf(QueryFilter("contractId", Comparison.EQUALS, contractId)),
                pageSize = 100,
            ),
        )
        return env.unwrap("Müqavilə borcları").items.orEmpty().map { it.toRow() }
    }

    /** Tədarükçünün aktiv müqavilələri (birdəfəlik xidmət xərci üçün). */
    suspend fun contractsByVendor(vendorId: String): List<ContractListItemDto> {
        val env = contractsApi.search(
            SearchBody(
                filters = listOf(
                    QueryFilter("vendorId", Comparison.EQUALS, vendorId),
                    QueryFilter("status", Comparison.EQUALS, ContractStatus.ACTIVE),
                ),
                pageSize = 100,
            ),
        )
        return env.unwrap("Müqavilələr").contracts.orEmpty()
    }

    /** Müştəri ilə bağlanmış müqavilələr (müştəri detalı üçün). */
    suspend fun contractsByCustomer(customerId: String): List<ContractListItemDto> {
        val env = contractsApi.search(
            SearchBody(
                filters = listOf(QueryFilter("customerId", Comparison.EQUALS, customerId)),
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                pageSize = 100,
            ),
        )
        return env.unwrap("Müqavilələr").contracts.orEmpty()
    }

    // ---- Borclar (sakin haqları) -------------------------------------------

    suspend fun charges(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
        scope: DebtScope,
    ): Page<ChargeDto> {
        val filters = when (scope) {
            DebtScope.OPEN -> listOf(
                QueryFilter("Status", Comparison.NOT_EQUALS, ChargeStatus.PAID),
                QueryFilter("Status", Comparison.NOT_EQUALS, ChargeStatus.CANCELLED),
            )
            DebtScope.PAID -> listOf(QueryFilter("Status", Comparison.EQUALS, ChargeStatus.PAID))
            DebtScope.ALL -> emptyList()
        }
        val env = coreApi.charges(
            SearchBody(
                filters = filters.ifEmpty { null },
                sortCriteria = SortCriteria("OwnerId", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Borclar")
        return Page(
            payload.charges.orEmpty(),
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun chargeAllocations(chargeId: String): List<ChargeAllocationDto> =
        paymentsApi.chargeAllocations(chargeId).unwrap("Bölgü")

    suspend fun generateCharges(period: String): GenerateChargesResultDto =
        paymentsApi.generateChargesForPeriod(ChargeGenerateBody(period)).unwrap("Borc generasiyası")

    suspend fun createOwnerCharge(request: CreateChargeRequestDto): String =
        paymentsApi.createCharge(request).unwrap("Borc").trim('"')

    suspend fun cancelOwnerCharge(id: String) {
        paymentsApi.cancelCharge(id)
    }

    suspend fun createOwnerPayment(request: CreatePaymentRequestDto): String =
        paymentsApi.createPayment(request).unwrap("Ödəniş").trim('"')

    /** Tranzaksiya jurnalındakı ödəniş sənədi. */
    suspend fun payment(id: String): PaymentDto = paymentsApi.payment(id).unwrap("Ödəniş")

    suspend fun paymentAllocations(id: String): List<PaymentAllocationDto> =
        paymentsApi.paymentAllocations(id).unwrap("Ödəniş bölgüsü")

    // ---- Maliyyə əməliyyatları ---------------------------------------------

    suspend fun financePayments(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<FinPaymentResponse> = FinanceRepository.payments(page, pageSize, search)

    suspend fun createFinancePayment(request: FinCreatePaymentRequest): String =
        FinanceRepository.createPayment(request)

    suspend fun currentRates(): List<FinRateResponse> = FinanceRepository.currentRates()

    suspend fun createRate(request: FinCreateRateRequest): String = FinanceRepository.createRate(request)

    suspend fun updateRate(id: String, amount: Double, description: String?) =
        FinanceRepository.updateRate(id, amount, description)

    suspend fun transactions(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
        direction: Int? = null,
    ): Page<FinTransactionResponse> = FinanceRepository.transactions(page, pageSize, search, direction)

    suspend fun transactionsSummary(): FinTransactionsSummary = FinanceRepository.transactionsSummary()

    suspend fun createTransaction(request: FinCreateTransactionRequest): String =
        FinanceRepository.createTransaction(request)

    suspend fun owners(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<OwnerItemDto> = FinanceRepository.owners(page, pageSize, search)

    suspend fun owner(id: String): OwnerDetailDto = FinanceRepository.owner(id)

    suspend fun purchases(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<InvPurchaseResponse> = InventoryRepo.repository.purchasePage(page, pageSize, search.clean())

    /** Tranzaksiya jurnalındakı satınalma qaiməsi. */
    suspend fun purchase(id: String): InvPurchaseResponse = InventoryRepo.repository.purchase(id)

    suspend fun payroll(year: Int, month: Int): OpsPayrollRunDto? =
        try {
            operationsApi.payrollByPeriod(year, month).data
        } catch (e: retrofit2.HttpException) {
            // Dövr üçün hesablama hələ yaradılmayıb — 404 normal haldır.
            if (e.code() == 404) null else throw e
        }

    suspend fun calculatePayroll(year: Int, month: Int): OpsPayrollRunDto =
        operationsApi.calculatePayroll(OpsPayrollPeriodRequest(year, month)).unwrap("Əmək haqqı")

    suspend fun recalculatePayroll(id: String): OpsPayrollRunDto =
        operationsApi.recalculatePayroll(id).unwrap("Əmək haqqı")

    suspend fun approvePayroll(id: String): OpsPayrollRunDto =
        operationsApi.approvePayroll(id).unwrap("Əmək haqqı")

    suspend fun payAllPayslips(id: String, method: Int): OpsPayrollRunDto =
        operationsApi.payAllPayslips(id, OpsPayMethodRequest(method)).unwrap("Əmək haqqı")

    suspend fun payPayslip(id: String, employeeId: String, method: Int): OpsPayrollRunDto =
        operationsApi.payPayslip(id, employeeId, OpsPayMethodRequest(method)).unwrap("Əmək haqqı")

    suspend fun upcomingVacationPays(): List<OpsVacationPayDto> =
        operationsApi.upcomingVacationPays().unwrap("Məzuniyyət ödənişləri")

    suspend fun vacationPays(
        page: Int,
        pageSize: Int = MtkRepository.PAGE_SIZE,
        search: String? = null,
    ): Page<OpsVacationPayDto> {
        val env = operationsApi.searchVacationPays(
            SearchBody(
                sortCriteria = SortCriteria("DueDate", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Məzuniyyət ödənişləri")
        return Page(
            payload.vacationPays.orEmpty(),
            payload.totalCount ?: 0,
            payload.page ?: page,
            payload.pageSize ?: pageSize,
        )
    }

    suspend fun payVacationPay(id: String, method: Int): OpsVacationPayDto =
        operationsApi.payVacationPay(id, OpsPayMethodRequest(method)).unwrap("Məzuniyyət ödənişi")

    suspend fun createOneTimeServiceExpense(request: OpsOneTimeExpenseRequest): String =
        operationsApi.oneTimeServiceExpense(request).unwrap("Xərc").trim('"')

    // ---- Köməkçilər ---------------------------------------------------------

    private fun VendorDto.toRow() = PartyRow(
        id = id,
        name = name,
        type = vendorType,
        voen = voen,
        director = director,
        email = email,
        phone = phone,
        address = address,
        isActive = isActive != false,
        createdAt = createdAt,
    )

    private fun CustomerDto.toRow() = PartyRow(
        id = id,
        name = name,
        type = customerType,
        voen = voen,
        director = director,
        email = email,
        phone = phone,
        address = address,
        isActive = isActive != false,
        createdAt = createdAt,
    )

    private fun VendorChargeDto.toRow() = PartyChargeRow(
        id = id,
        partyId = vendorId,
        contractId = contractId,
        period = period,
        description = description,
        amount = amount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        status = status,
        chargeDate = chargeDate,
        dueDate = dueDate,
        isOverdue = isOverdue == true,
    )

    private fun CustomerChargeDto.toRow() = PartyChargeRow(
        id = id,
        partyId = customerId,
        contractId = contractId,
        period = period,
        description = description,
        amount = amount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        status = status,
        chargeDate = chargeDate,
        dueDate = dueDate,
        isOverdue = isOverdue == true,
    )

    private fun PartyPaymentDto.toRow() = PartyPaymentRow(
        id = id,
        amount = amount,
        paymentMethod = paymentMethod,
        paymentDate = paymentDate,
        status = status,
        notes = notes,
    )

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")
}

/** Borclar siyahısının seqmenti. */
enum class DebtScope(val label: String) {
    OPEN("Açıq borclar"),
    PAID("Ödənilib"),
    ALL("Hamısı"),
}
