package az.mtk.app.ui.screens.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.AdmAuditRow
import az.mtk.app.data.api.AdmUpsertCompanyRequest
import az.mtk.app.data.api.AdminRepository
import az.mtk.app.data.api.AuditModules
import az.mtk.app.data.api.Comparison
import az.mtk.app.data.api.QueryFilter
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.Sort
import az.mtk.app.data.api.SortCriteria
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class ManagementTab(val label: String) {
    COMPANY("Şirkət profili"),
    AUDIT("Audit qeydləri"),
}

data class CompanyProfileState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    /** Profil hələ yaradılmayıb — backend `Company.NotFound` qaytarır. */
    val notFound: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val director: String = "",
    val address: String = "",
    val email: String = "info@mtk.az",
    val phone: String = "",
    val workHours: String = "",
    val editing: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val formError: String? = null,
)

data class AuditLogState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val module: String = AuditModules.IDENTITY,
    val entityType: String = "",
    val action: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val rows: List<AdmAuditRow> = emptyList(),
    val expandedId: String? = null,
) {
    val pageCount: Int get() = maxOf(1, (totalCount + pageSize - 1) / pageSize)
}

data class ManagementUiState(
    val company: CompanyProfileState = CompanyProfileState(),
    val audit: AuditLogState = AuditLogState(),
)

/**
 * İdarəetmə modulu: şirkət rekvizitləri və bütün modulların audit jurnalı.
 *
 * Audit filtrləri serverdə tətbiq olunur; mətn sahələri yazılarkən sorğu
 * 400 ms gecikmə ilə göndərilir ki, hər hərf üçün ayrıca request getməsin.
 */
class ManagementViewModel(
    private val repository: AdminRepository = AdminRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ManagementUiState())
    val state = _state.asStateFlow()

    private var companySnapshot = CompanySnapshot()
    private var auditJob: Job? = null

    // ---------------------------------------------------------------- company

    fun loadCompany(force: Boolean = false) {
        if (_state.value.company.loading && !force) return
        viewModelScope.launch {
            _state.update { it.copy(company = it.company.copy(loading = true, error = null)) }
            runCatching { repository.companyOrNull() }
                .onSuccess { company ->
                    if (company == null) {
                        companySnapshot = CompanySnapshot()
                        _state.update {
                            it.copy(
                                company = CompanyProfileState(
                                    loading = false,
                                    loaded = true,
                                    notFound = true,
                                ),
                            )
                        }
                    } else {
                        val snapshot = CompanySnapshot(
                            name = company.name,
                            director = company.director.orEmpty(),
                            address = company.address,
                            email = company.email?.takeIf { email -> email.isNotBlank() }
                                ?: "info@mtk.az",
                            phone = company.phone.orEmpty(),
                            workHours = company.workHours.orEmpty(),
                        )
                        companySnapshot = snapshot
                        _state.update {
                            it.copy(
                                company = CompanyProfileState(
                                    loading = false,
                                    loaded = true,
                                    notFound = false,
                                    name = snapshot.name,
                                    director = snapshot.director,
                                    address = snapshot.address,
                                    email = snapshot.email,
                                    phone = snapshot.phone,
                                    workHours = snapshot.workHours,
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            company = it.company.copy(
                                loading = false,
                                loaded = true,
                                error = error.toUserMessage(),
                            ),
                        )
                    }
                }
        }
    }

    fun startCompanyEdit() {
        _state.update {
            it.copy(company = it.company.copy(editing = true, saved = false, formError = null))
        }
    }

    fun cancelCompanyEdit() {
        val snapshot = companySnapshot
        _state.update {
            it.copy(
                company = it.company.copy(
                    name = snapshot.name,
                    director = snapshot.director,
                    address = snapshot.address,
                    email = snapshot.email,
                    phone = snapshot.phone,
                    workHours = snapshot.workHours,
                    editing = false,
                    saved = false,
                    formError = null,
                ),
            )
        }
    }

    fun updateCompanyName(value: String) = updateCompany { it.copy(name = value) }

    fun updateCompanyDirector(value: String) = updateCompany { it.copy(director = value) }

    fun updateCompanyAddress(value: String) = updateCompany { it.copy(address = value) }

    fun updateCompanyEmail(value: String) = updateCompany { it.copy(email = value) }

    fun updateCompanyPhone(value: String) = updateCompany { it.copy(phone = value) }

    fun updateCompanyWorkHours(value: String) = updateCompany { it.copy(workHours = value) }

    private fun updateCompany(transform: (CompanyProfileState) -> CompanyProfileState) {
        _state.update {
            it.copy(company = transform(it.company).copy(saved = false, formError = null))
        }
    }

    fun saveCompany() {
        val company = _state.value.company
        val name = company.name.trim()
        val address = company.address.trim()
        val validationError = when {
            name.isEmpty() -> "Şirkət adı boş ola bilməz."
            address.isEmpty() -> "Ünvan boş ola bilməz."
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(company = it.company.copy(formError = validationError)) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(company = it.company.copy(saving = true, saved = false, formError = null))
            }
            runCatching {
                repository.upsertCompany(
                    AdmUpsertCompanyRequest(
                        name = name,
                        director = company.director.trim().takeIf { it.isNotEmpty() },
                        address = address,
                        email = company.email.trim().takeIf { it.isNotEmpty() } ?: "info@mtk.az",
                        phone = company.phone.trim().takeIf { it.isNotEmpty() },
                        workHours = company.workHours.trim().takeIf { it.isNotEmpty() },
                    ),
                )
            }.onSuccess { updated ->
                val snapshot = CompanySnapshot(
                    name = updated.name,
                    director = updated.director.orEmpty(),
                    address = updated.address,
                    email = updated.email?.takeIf { email -> email.isNotBlank() } ?: "info@mtk.az",
                    phone = updated.phone.orEmpty(),
                    workHours = updated.workHours.orEmpty(),
                )
                companySnapshot = snapshot
                _state.update {
                    it.copy(
                        company = CompanyProfileState(
                            loading = false,
                            loaded = true,
                            notFound = false,
                            name = snapshot.name,
                            director = snapshot.director,
                            address = snapshot.address,
                            email = snapshot.email,
                            phone = snapshot.phone,
                            workHours = snapshot.workHours,
                            editing = false,
                            saving = false,
                            saved = true,
                        ),
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(company = it.company.copy(saving = false, formError = error.toUserMessage()))
                }
            }
        }
    }

    fun clearCompanyFeedback() {
        _state.update { it.copy(company = it.company.copy(saved = false, formError = null)) }
    }

    // ------------------------------------------------------------------ audit

    fun ensureAudit() {
        val audit = _state.value.audit
        if (!audit.loaded && !audit.loading) loadAudit()
    }

    fun refreshAudit() = loadAudit()

    fun selectAuditModule(module: String) {
        _state.update {
            it.copy(
                audit = it.audit.copy(
                    module = module,
                    page = 0,
                    expandedId = null,
                    loaded = false,
                ),
            )
        }
        loadAudit()
    }

    fun setAuditEntityType(value: String) {
        _state.update {
            it.copy(audit = it.audit.copy(entityType = value, page = 0, expandedId = null))
        }
        scheduleAuditLoad(delayMs = 400)
    }

    fun setAuditAction(value: String) {
        _state.update {
            it.copy(audit = it.audit.copy(action = value, page = 0, expandedId = null))
        }
        loadAudit()
    }

    fun setAuditDateFrom(value: String) {
        _state.update {
            it.copy(audit = it.audit.copy(dateFrom = value, page = 0, expandedId = null))
        }
        scheduleAuditLoad(delayMs = 400)
    }

    fun setAuditDateTo(value: String) {
        _state.update {
            it.copy(audit = it.audit.copy(dateTo = value, page = 0, expandedId = null))
        }
        scheduleAuditLoad(delayMs = 400)
    }

    fun clearAuditFilters() {
        _state.update {
            it.copy(
                audit = it.audit.copy(
                    entityType = "",
                    action = "",
                    dateFrom = "",
                    dateTo = "",
                    page = 0,
                    expandedId = null,
                    loaded = false,
                ),
            )
        }
        loadAudit()
    }

    fun previousAuditPage() {
        if (_state.value.audit.page <= 0) return
        _state.update {
            it.copy(audit = it.audit.copy(page = it.audit.page - 1, expandedId = null))
        }
        loadAudit()
    }

    fun nextAuditPage() {
        val audit = _state.value.audit
        if (audit.page + 1 >= audit.pageCount) return
        _state.update {
            it.copy(audit = it.audit.copy(page = it.audit.page + 1, expandedId = null))
        }
        loadAudit()
    }

    fun toggleAuditRow(id: String) {
        _state.update {
            it.copy(audit = it.audit.copy(expandedId = if (it.audit.expandedId == id) null else id))
        }
    }

    private fun loadAudit() = scheduleAuditLoad(delayMs = 0)

    private fun scheduleAuditLoad(delayMs: Long) {
        auditJob?.cancel()
        auditJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            loadAuditNow()
        }
    }

    private suspend fun loadAuditNow() {
        val audit = _state.value.audit
        _state.update { it.copy(audit = it.audit.copy(loading = true, error = null)) }

        val filters = buildList {
            val entity = audit.entityType.trim()
            if (entity.isNotEmpty()) {
                add(
                    QueryFilter(
                        columnName = "EntityType",
                        comparison = Comparison.CONTAINS,
                        value = entity.replaceFirstChar { it.uppercaseChar() },
                    ),
                )
            }
            if (audit.action.isNotEmpty()) {
                add(QueryFilter("Action", Comparison.EQUALS, audit.action))
            }
            isoStartOfDay(audit.dateFrom)?.let {
                add(QueryFilter("Timestamp", Comparison.GREATER_THAN_OR_EQUAL, it))
            }
            isoEndOfDay(audit.dateTo)?.let {
                add(QueryFilter("Timestamp", Comparison.LESS_THAN_OR_EQUAL, it))
            }
        }

        val body = SearchBody(
            filters = filters.ifEmpty { null },
            sortCriteria = SortCriteria("Timestamp", Sort.DESCENDING),
            page = audit.page,
            pageSize = audit.pageSize,
        )

        runCatching { repository.searchAudit(audit.module, body) }
            .onSuccess { result ->
                val rows = result.auditLogs.orEmpty().map { log ->
                    AdmAuditRow(
                        id = log.id,
                        entityType = log.entityType ?: "—",
                        entityId = log.entityId ?: "—",
                        action = log.action ?: "—",
                        oldValues = log.oldValues,
                        newValues = log.newValues,
                        userLabel = log.user?.name
                            ?: log.userId?.take(8)?.let { "$it…" },
                        timestamp = log.timestamp,
                    )
                }
                _state.update {
                    it.copy(
                        audit = it.audit.copy(
                            loading = false,
                            loaded = true,
                            error = null,
                            rows = rows,
                            totalCount = result.totalCount,
                            pageSize = result.pageSize.takeIf { size -> size > 0 }
                                ?: it.audit.pageSize,
                            expandedId = it.audit.expandedId
                                ?.takeIf { id -> rows.any { row -> row.id == id } },
                        ),
                    )
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        audit = it.audit.copy(
                            loading = false,
                            loaded = true,
                            error = error.toUserMessage(),
                            rows = emptyList(),
                            totalCount = 0,
                            expandedId = null,
                        ),
                    )
                }
            }
    }

    private fun isoStartOfDay(value: String): String? = runCatching {
        LocalDate.parse(value)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toString()
    }.getOrNull()

    private fun isoEndOfDay(value: String): String? = runCatching {
        LocalDate.parse(value)
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toString()
    }.getOrNull()

    private data class CompanySnapshot(
        val name: String = "",
        val director: String = "",
        val address: String = "",
        val email: String = "info@mtk.az",
        val phone: String = "",
        val workHours: String = "",
    )
}
