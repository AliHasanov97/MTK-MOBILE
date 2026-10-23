package az.mtk.app.ui.screens.hr

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.ApplicationDto
import az.mtk.app.data.api.HrApi
import az.mtk.app.data.api.HrApplicationKind
import az.mtk.app.data.api.HrEmployeeListItemDto
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrFileApi
import az.mtk.app.data.api.HrFileAttachmentDto
import az.mtk.app.data.api.HrJobDto
import az.mtk.app.data.api.HrOrderItemDto
import az.mtk.app.data.api.HrOrderKind
import az.mtk.app.data.api.HrOption
import az.mtk.app.data.api.HrRepository
import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.components.DocumentPreviewDialog
import az.mtk.app.ui.components.PreviewDocument
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HrDocumentKind(val label: String) {
    APPLICATION("Ərizə"),
    ORDER("Əmr"),
}

data class HrDocumentTarget(val kind: HrDocumentKind, val type: Int, val id: String)

/* ------------------------------ Sahə konfiqurasiyası ------------------------------ */

internal enum class HrFieldKind { TEXT, NUMBER, DATE, SELECT, EMPLOYEE, JOB }

internal data class HrFieldSpec(
    val key: String,
    val label: String,
    val kind: HrFieldKind,
    val required: Boolean = false,
    val options: List<HrOption> = emptyList(),
    val hint: String? = null,
    val min: Double? = null,
    val max: Double? = null,
)

internal val HR_APPLICATION_FIELDS: Map<Int, List<HrFieldSpec>> = mapOf(
    1 to listOf(
        HrFieldSpec("surname", "Soyad", HrFieldKind.TEXT, true),
        HrFieldSpec("name", "Ad", HrFieldKind.TEXT, true),
        HrFieldSpec("fathersName", "Ata adı", HrFieldKind.TEXT, true),
        HrFieldSpec("gender", "Cins", HrFieldKind.SELECT, true, HrEnums.GENDERS),
        HrFieldSpec("telephone", "Telefon", HrFieldKind.TEXT, true),
        HrFieldSpec("homeTelephoneNumber", "Ev telefonu", HrFieldKind.TEXT),
        HrFieldSpec("address", "Ünvan", HrFieldKind.TEXT, true),
        HrFieldSpec("jobId", "Vəzifə", HrFieldKind.JOB, true),
        HrFieldSpec("startDate", "İşə başlama tarixi", HrFieldKind.DATE, true),
    ),
    2 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("newJobId", "Yeni vəzifə", HrFieldKind.JOB, true),
        HrFieldSpec("setDate", "Dəyişiklik tarixi", HrFieldKind.DATE, true),
    ),
    3 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("requestedDays", "Kompensasiya ediləcək gün sayı", HrFieldKind.NUMBER, true, min = 1.0),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    4 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    5 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, true),
        HrFieldSpec(
            "endDate",
            "Bitmə tarixi",
            HrFieldKind.DATE,
            hint = "Bitmə tarixi və ya gün sayından yalnız birini doldurun. Bayram günləri avtomatik çıxılır.",
        ),
        HrFieldSpec("requestedDays", "Gün sayı", HrFieldKind.NUMBER, min = 1.0),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
    6 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("reason", "Səbəb", HrFieldKind.TEXT),
    ),
    7 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("newEmploymentType", "Yeni iş rejimi", HrFieldKind.SELECT, true, HrEnums.EMPLOYMENT_TYPES),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, true),
    ),
    8 to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("returnDate", "İşə qayıtma tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("notes", "Qeyd", HrFieldKind.TEXT),
    ),
)

internal val HR_DIRECT_ORDER_KINDS: List<Pair<String, String>> = listOf(
    "Bonus" to "Mükafat",
    "UnexcusedAbsence" to "İcazəsiz qeyb",
    "Warning" to "Xəbərdarlıq / töhmət",
    "WorkOnNonWorkday" to "İstirahət / bayram günü işi",
)

internal val HR_DIRECT_ORDER_FIELDS: Map<String, List<HrFieldSpec>> = mapOf(
    "Bonus" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("bonusQuantity", "Mükafat məbləği (₼)", HrFieldKind.NUMBER, true, min = 1.0),
        HrFieldSpec("salaryMonth", "Maaş ayı", HrFieldKind.SELECT, true, HrEnums.MONTHS.mapIndexed { i, m -> HrOption(i + 1, m) }),
        HrFieldSpec("salaryYear", "Maaş ili", HrFieldKind.NUMBER, true, min = 2000.0, max = 2100.0),
    ),
    "UnexcusedAbsence" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("setDate", "Qeyb tarixi", HrFieldKind.DATE, true),
    ),
    "Warning" to listOf(
        HrFieldSpec("employeeId", "İşçi", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("orderExecutionSupervisorId", "Əmrin icrasına nəzarət edən", HrFieldKind.EMPLOYEE, true),
        HrFieldSpec("disciplinaryType", "Növ", HrFieldKind.SELECT, true, HrEnums.DISCIPLINARY_TYPES),
        HrFieldSpec("setDate", "Tarix", HrFieldKind.DATE, true),
    ),
    "WorkOnNonWorkday" to listOf(
        HrFieldSpec("startDate", "Başlanğıc tarixi", HrFieldKind.DATE, true),
        HrFieldSpec("endDate", "Bitmə tarixi", HrFieldKind.DATE, hint = "Boş buraxsanız yalnız başlanğıc günü nəzərdə tutulur."),
    ),
)

/* ------------------------------ ViewModel ------------------------------ */

class HrDocumentsViewModel : ViewModel() {

    private val repo: HrRepository = HrApi.repository

    data class UiState(
        val kind: HrDocumentKind = HrDocumentKind.APPLICATION,
        val employeeId: String? = null,
        val items: List<HrDocumentRow>? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val page: Int = 1,
        val pageCount: Int = 1,
        val totalCount: Int = 0,
        val typeFilter: Int? = null,
        val employees: List<HrEmployeeListItemDto> = emptyList(),
        val jobs: List<HrJobDto> = emptyList(),
        val createOpen: Boolean = false,
        val createKind: Int = 5,
        val createDirectKind: String = "Bonus",
        val values: Map<String, String> = emptyMap(),
        val createSaving: Boolean = false,
        val createError: String? = null,
        val fixedEmployeeId: String? = null,
        val detail: HrDocumentTarget? = null,
        val detailData: JsonObject? = null,
        val detailLoading: Boolean = false,
        val detailError: String? = null,
        val detailBusy: String? = null,
        val detailNotice: String? = null,
        val convertOpen: Boolean = false,
        val convertCases: List<az.mtk.app.data.api.HrLaborCodeCaseDto>? = null,
        val convertStart: String = "",
        val convertEnd: String = "",
        val convertCaseId: String = "",
        val convertSaving: Boolean = false,
        val convertError: String? = null,
        val attachments: List<HrFileAttachmentDto>? = null,
        val attachmentError: String? = null,
        val attachmentUploading: Boolean = false,
        val attachmentBusyId: String? = null,
        val actionMessage: String? = null,
        val actionError: String? = null,
        val preview: PreviewDocument? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun setScope(kind: HrDocumentKind, employeeId: String?) {
        val current = _state.value
        if (current.kind == kind && current.employeeId == employeeId && current.items != null) return
        _state.value = current.copy(
            kind = kind,
            employeeId = employeeId,
            items = null,
            page = 1,
            typeFilter = null,
            error = null,
        )
        load()
        loadPickers()
    }

    fun load() {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                when (s.kind) {
                    HrDocumentKind.APPLICATION -> repo.applications(
                        page = s.page - 1,
                        pageSize = 10,
                        type = s.typeFilter,
                        employeeId = s.employeeId,
                    )
                        .let { page ->
                            page.items.map {
                                HrDocumentRow(
                                    id = it.id,
                                    number = it.applicationNumber,
                                    type = it.type,
                                    personName = it.employee?.name ?: it.jobApplicant?.name,
                                    createdBy = it.createdBy?.name,
                                    createdAt = it.createdAt,
                                    status = it.status,
                                )
                            } to page.totalCount
                        }
                    HrDocumentKind.ORDER -> repo.orders(
                        page = s.page - 1,
                        pageSize = 10,
                        types = s.typeFilter?.let { listOf(it) },
                        employeeId = s.employeeId,
                    )
                        .let { page ->
                            page.items.map {
                                HrDocumentRow(
                                    id = it.id,
                                    number = it.orderNumber,
                                    type = it.type,
                                    personName = it.employee?.name ?: it.jobApplicant?.name,
                                    createdBy = it.createdBy?.name,
                                    createdAt = it.createdAt,
                                    status = null,
                                )
                            } to page.totalCount
                        }
                }
            }.onSuccess { (rows, total) ->
                _state.value = _state.value.copy(
                    items = rows,
                    totalCount = total,
                    pageCount = ((total + 9) / 10).coerceAtLeast(1),
                    loading = false,
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(error = failure.toUserMessage(), loading = false)
            }
        }
    }

    private fun loadPickers() {
        if (_state.value.employees.isEmpty()) {
            viewModelScope.launch {
                runCatching { repo.employeeList(page = 0, pageSize = 300) }
                    .onSuccess { page -> _state.value = _state.value.copy(employees = page.items) }
            }
        }
        if (_state.value.jobs.isEmpty()) {
            viewModelScope.launch {
                runCatching { repo.jobs(page = 0, pageSize = 300) }
                    .onSuccess { page -> _state.value = _state.value.copy(jobs = page.items) }
            }
        }
    }

    fun goToPage(page: Int) {
        _state.value = _state.value.copy(page = page.coerceIn(1, _state.value.pageCount.coerceAtLeast(1)))
        load()
    }

    fun setTypeFilter(type: Int?) {
        _state.value = _state.value.copy(typeFilter = type, page = 1)
        load()
    }

    fun openCreate(fixedEmployeeId: String? = null) {
        val s = _state.value
        val employee = fixedEmployeeId ?: s.employeeId
        _state.value = s.copy(
            createOpen = true,
            createError = null,
            values = employee?.let { mapOf("employeeId" to it) }.orEmpty(),
            fixedEmployeeId = employee,
            createKind = if (s.kind == HrDocumentKind.APPLICATION) 5 else s.createKind,
            createDirectKind = if (s.kind == HrDocumentKind.ORDER) "Bonus" else s.createDirectKind,
        )
        loadPickers()
    }

    fun closeCreate() {
        _state.value = _state.value.copy(createOpen = false, createError = null, createSaving = false)
    }

    fun setCreateKind(value: Int) {
        _state.value = _state.value.copy(
            createKind = value,
            values = _state.value.fixedEmployeeId?.let { mapOf("employeeId" to it) }.orEmpty(),
            createError = null,
        )
    }

    fun setCreateDirectKind(value: String) {
        _state.value = _state.value.copy(
            createDirectKind = value,
            values = _state.value.fixedEmployeeId?.let { mapOf("employeeId" to it) }.orEmpty(),
            createError = null,
        )
    }

    fun setField(key: String, value: String) {
        _state.value = _state.value.copy(values = _state.value.values + (key to value), createError = null)
    }

    fun submitCreate() {
        val s = _state.value
        val specs = currentSpecs(s).filter { it.key != "employeeId" || s.fixedEmployeeId == null }
        val missing = specs.firstOrNull { it.required && s.values[it.key].orEmpty().isBlank() }
        if (missing != null) {
            _state.value = s.copy(createError = "«${missing.label}» sahəsi doldurulmalıdır.")
            return
        }
        _state.value = s.copy(createSaving = true, createError = null)
        viewModelScope.launch {
            runCatching {
                when (s.kind) {
                    HrDocumentKind.APPLICATION -> {
                        val body = buildBody(specs, s.values)
                        if (s.fixedEmployeeId != null) body.addProperty("employeeId", s.fixedEmployeeId)
                        if (s.createKind == 5 || s.createKind == 3) {
                            body.addProperty("vacationBalanceId", "00000000-0000-0000-0000-000000000000")
                        }
                        repo.createApplication(s.createKind, body)
                    }
                    HrDocumentKind.ORDER -> {
                        val body = buildBody(specs, s.values)
                        if (s.fixedEmployeeId != null) body.addProperty("employeeId", s.fixedEmployeeId)
                        repo.createDirectOrder(s.createDirectKind, body)
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    createSaving = false,
                    createOpen = false,
                    actionMessage = if (s.kind == HrDocumentKind.APPLICATION) "Ərizə yaradıldı." else "Əmr yaradıldı.",
                )
                load()
            }.onFailure { failure ->
                _state.value = _state.value.copy(createSaving = false, createError = failure.toUserMessage())
            }
        }
    }

    private fun currentSpecs(s: UiState): List<HrFieldSpec> =
        if (s.kind == HrDocumentKind.APPLICATION) {
            HR_APPLICATION_FIELDS[s.createKind].orEmpty()
        } else {
            HR_DIRECT_ORDER_FIELDS[s.createDirectKind].orEmpty()
        }

    private fun buildBody(specs: List<HrFieldSpec>, values: Map<String, String>): JsonObject =
        JsonObject().apply {
            specs.forEach { spec ->
                val raw = values[spec.key].orEmpty().trim()
                if (raw.isEmpty()) return@forEach
                when (spec.kind) {
                    HrFieldKind.NUMBER -> raw.toDoubleOrNull()?.let { number ->
                        if (number % 1.0 == 0.0) addProperty(spec.key, number.toInt()) else addProperty(spec.key, number)
                    }
                    HrFieldKind.SELECT -> raw.toIntOrNull()?.let { addProperty(spec.key, it) }
                    HrFieldKind.DATE -> addProperty(spec.key, isoDate(raw))
                    HrFieldKind.EMPLOYEE, HrFieldKind.JOB, HrFieldKind.TEXT -> addProperty(spec.key, raw)
                }
            }
        }

    /* ------------------------------ Sənəd detalı ------------------------------ */

    fun openDetail(target: HrDocumentTarget) {
        _state.value = _state.value.copy(
            detail = target,
            detailData = null,
            detailError = null,
            detailLoading = true,
            detailNotice = null,
            attachments = null,
            attachmentError = null,
        )
        viewModelScope.launch {
            runCatching {
                when (target.kind) {
                    HrDocumentKind.APPLICATION -> repo.applicationDetail(target.type, target.id)
                    HrDocumentKind.ORDER -> repo.orderDetail(target.type, target.id)
                }
            }.onSuccess { data ->
                _state.value = _state.value.copy(detailData = data, detailLoading = false)
            }.onFailure { failure ->
                _state.value = _state.value.copy(detailError = failure.toUserMessage(), detailLoading = false)
            }
        }
        loadAttachments()
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detail = null, detailData = null, detailError = null, attachments = null)
    }

    fun convertCurrent() {
        val target = _state.value.detail ?: return
        if (target.kind != HrDocumentKind.APPLICATION) return
        if (target.type == 1) {
            _state.value = _state.value.copy(
                convertOpen = true,
                convertCases = null,
                convertStart = "",
                convertEnd = "",
                convertCaseId = "",
                convertError = null,
            )
            viewModelScope.launch {
                runCatching { repo.laborCodeCases() }
                    .onSuccess { cases -> _state.value = _state.value.copy(convertCases = cases) }
                    .onFailure { failure -> _state.value = _state.value.copy(convertError = failure.toUserMessage()) }
            }
            return
        }
        _state.value = _state.value.copy(detailBusy = "convert", detailError = null)
        viewModelScope.launch {
            runCatching { repo.convertApplication(target.type, target.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        detailBusy = null,
                        detailNotice = "Ərizə əmrə çevrildi.",
                    )
                    load()
                    refreshDetail()
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(detailBusy = null, detailError = failure.toUserMessage())
                }
        }
    }

    fun closeConvert() {
        _state.value = _state.value.copy(convertOpen = false, convertError = null, convertSaving = false)
    }

    fun setConvertField(start: String? = null, end: String? = null, caseId: String? = null) {
        _state.value = _state.value.copy(
            convertStart = start ?: _state.value.convertStart,
            convertEnd = end ?: _state.value.convertEnd,
            convertCaseId = caseId ?: _state.value.convertCaseId,
            convertError = null,
        )
    }

    fun submitConvert() {
        val target = _state.value.detail ?: return
        val s = _state.value
        if (s.convertStart.isBlank() || s.convertEnd.isBlank()) {
            _state.value = s.copy(convertError = "Müqavilənin başlanğıc və bitmə tarixi doldurulmalıdır.")
            return
        }
        if (s.convertCaseId.isBlank()) {
            _state.value = s.copy(convertError = "Müqavilənin əsası seçilməlidir.")
            return
        }
        _state.value = s.copy(convertSaving = true, convertError = null)
        viewModelScope.launch {
            runCatching {
                repo.convertApplication(
                    target.type,
                    target.id,
                    JsonObject().apply {
                        addProperty("startDate", isoDate(s.convertStart))
                        addProperty("endDate", isoDate(s.convertEnd))
                        addProperty("laborCodeCaseId", s.convertCaseId)
                    },
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    convertSaving = false,
                    convertOpen = false,
                    detailNotice = "Ərizə əmrə çevrildi.",
                )
                load()
                refreshDetail()
            }.onFailure { failure ->
                _state.value = _state.value.copy(convertSaving = false, convertError = failure.toUserMessage())
            }
        }
    }

    fun deleteCurrent() {
        val target = _state.value.detail ?: return
        _state.value = _state.value.copy(detailBusy = "delete", detailError = null)
        viewModelScope.launch {
            runCatching {
                when (target.kind) {
                    HrDocumentKind.APPLICATION -> repo.deleteApplication(target.type, target.id)
                    HrDocumentKind.ORDER -> repo.deleteOrder(target.type, target.id)
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    detailBusy = null,
                    detail = null,
                    detailData = null,
                    actionMessage = "Sənəd silindi.",
                )
                load()
            }.onFailure { failure ->
                _state.value = _state.value.copy(detailBusy = null, detailError = failure.toUserMessage())
            }
        }
    }

    private fun refreshDetail() {
        val target = _state.value.detail ?: return
        viewModelScope.launch {
            runCatching {
                when (target.kind) {
                    HrDocumentKind.APPLICATION -> repo.applicationDetail(target.type, target.id)
                    HrDocumentKind.ORDER -> repo.orderDetail(target.type, target.id)
                }
            }.onSuccess { data -> _state.value = _state.value.copy(detailData = data) }
        }
    }

    fun downloadPdf(context: Context) {
        val target = _state.value.detail ?: return
        val route = when (target.kind) {
            HrDocumentKind.APPLICATION -> HrApplicationKind.route(target.type)
            HrDocumentKind.ORDER -> HrOrderKind.route(target.type)
        }
        _state.value = _state.value.copy(detailBusy = "pdf", detailError = null)
        viewModelScope.launch {
            runCatching {
                FileRepository.downloadToInternal(
                    context = context,
                    path = "api/hr/$route/${target.id}/export-pdf",
                    fallbackName = "sened-${target.id.take(8)}.pdf",
                )
            }.onSuccess { result ->
                val label = when (target.kind) {
                    HrDocumentKind.APPLICATION -> HrApplicationKind.label(target.type)
                    HrDocumentKind.ORDER -> HrOrderKind.label(target.type)
                }
                _state.value = _state.value.copy(
                    preview = PreviewDocument(
                        filePath = result.file.absolutePath,
                        fileName = result.fileName,
                        mimeHint = "application/pdf",
                        title = label,
                    ),
                )
            }.onFailure { failure ->
                _state.value = _state.value.copy(detailError = failure.toUserMessage())
            }
            _state.value = _state.value.copy(detailBusy = null)
        }
    }

    fun closePreview() {
        _state.value = _state.value.copy(preview = null)
    }

    /* ------------------------------ Fayl əlavələri ------------------------------ */

    private fun fkColumn(): String? {
        val target = _state.value.detail ?: return null
        return when (target.kind) {
            HrDocumentKind.APPLICATION -> HR_APPLICATION_ATTACHMENT_FK[target.type]
            HrDocumentKind.ORDER -> HR_ORDER_ATTACHMENT_FK[target.type]
        }
    }

    fun loadAttachments() {
        val target = _state.value.detail ?: return
        val column = fkColumn() ?: return
        viewModelScope.launch {
            runCatching { repo.attachments(column, target.id) }
                .onSuccess { list -> _state.value = _state.value.copy(attachments = list, attachmentError = null) }
                .onFailure { failure -> _state.value = _state.value.copy(attachmentError = failure.toUserMessage()) }
        }
    }

    fun uploadAttachment(context: Context, uri: Uri) {
        val target = _state.value.detail ?: return
        val column = fkColumn() ?: return
        _state.value = _state.value.copy(attachmentUploading = true, attachmentError = null)
        viewModelScope.launch {
            runCatching { HrFileApi.upload(context, uri, column, target.id) }
                .onSuccess { loadAttachments() }
                .onFailure { failure ->
                    _state.value = _state.value.copy(attachmentError = failure.toUserMessage())
                }
            _state.value = _state.value.copy(attachmentUploading = false)
        }
    }

    fun downloadAttachment(context: Context, file: HrFileAttachmentDto) {
        _state.value = _state.value.copy(attachmentBusyId = file.id, attachmentError = null)
        viewModelScope.launch {
            runCatching { HrFileApi.download(context, file.id, file.fileName) }
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        preview = PreviewDocument(
                            filePath = result.file.absolutePath,
                            fileName = result.fileName,
                            mimeHint = file.mimeType,
                            title = file.fileName,
                        ),
                    )
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(attachmentError = failure.toUserMessage())
                }
            _state.value = _state.value.copy(attachmentBusyId = null)
        }
    }

    fun deleteAttachment(id: String) {
        _state.value = _state.value.copy(attachmentBusyId = id, attachmentError = null)
        viewModelScope.launch {
            runCatching { repo.deleteAttachment(id) }
                .onSuccess { loadAttachments() }
                .onFailure { failure ->
                    _state.value = _state.value.copy(attachmentError = failure.toUserMessage())
                }
            _state.value = _state.value.copy(attachmentBusyId = null)
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(actionMessage = null, actionError = null)
    }
}

data class HrDocumentRow(
    val id: String,
    val number: Int?,
    val type: Int?,
    val personName: String?,
    val createdBy: String?,
    val createdAt: String?,
    val status: String?,
)

/* ------------------------------ UI ------------------------------ */

@Composable
internal fun HrDocumentsTab(
    kind: HrDocumentKind,
    employeeId: String? = null,
    viewModel: HrDocumentsViewModel = hrDocumentsViewModel(kind, employeeId),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(kind, employeeId) { viewModel.setScope(kind, employeeId) }

    val typeOptions = when (kind) {
        HrDocumentKind.APPLICATION -> HrApplicationKind.LABELS.entries.sortedBy { it.key }
            .map { it.key.toString() to it.value }
        HrDocumentKind.ORDER -> HrOrderKind.LABELS.entries.sortedBy { it.key }
            .map { it.key.toString() to it.value }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HrSelectField(
                    label = "Növ",
                    value = state.typeFilter?.toString().orEmpty(),
                    options = listOf("" to "Hamısı") + typeOptions,
                    onSelect = { viewModel.setTypeFilter(it.toIntOrNull()) },
                )
            }
            HrPrimaryButton(
                text = if (kind == HrDocumentKind.APPLICATION) "+ Yeni ərizə" else "+ Yeni əmr",
                onClick = { viewModel.openCreate() },
            )
        }

        state.actionMessage?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearFeedback() } }
        state.actionError?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearFeedback() } }

        when {
            state.loading && state.items == null -> HrLoading()
            state.error != null && state.items == null -> HrErrorState(state.error!!) { viewModel.load() }
            state.items.orEmpty().isEmpty() -> HrEmptyState(
                title = "Sənəd yoxdur",
                text = if (kind == HrDocumentKind.APPLICATION) {
                    "“+ Yeni ərizə” ilə ilk müraciəti yaradın."
                } else {
                    "“+ Yeni əmr” ilə əmrsiz verilən əmrləri yaradın."
                },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items.orEmpty(), key = { it.id }) { row ->
                    DocumentCard(
                        row = row,
                        kind = kind,
                        onClick = {
                            val type = row.type ?: return@DocumentCard
                            viewModel.openDetail(HrDocumentTarget(kind, type, row.id))
                        },
                    )
                }
                item {
                    HrPaginationRow(
                        page = state.page,
                        pageCount = state.pageCount,
                        totalCount = state.totalCount,
                        onPage = viewModel::goToPage,
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    if (state.createOpen) {
        HrDocumentCreateSheet(
            state = state,
            kind = kind,
            onKind = viewModel::setCreateKind,
            onDirectKind = viewModel::setCreateDirectKind,
            onField = viewModel::setField,
            onSubmit = viewModel::submitCreate,
            onDismiss = viewModel::closeCreate,
        )
    }

    state.detail?.takeIf { !state.convertOpen }?.let { target ->
        val context = LocalContext.current
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                viewModel.uploadAttachment(context, uri)
            }
        }

        AppSheet(onDismissRequest = viewModel::closeDetail, scrollable = true) {
            HrSheetTitle(
                title = buildString {
                    append(target.kind.label)
                    state.detailData?.get("applicationNumber")?.intOrNull()?.let { append(" №$it") }
                    state.detailData?.get("orderNumber")?.intOrNull()?.let { append(" №$it") }
                    append(" · ")
                    append(if (target.kind == HrDocumentKind.APPLICATION) {
                        HrApplicationKind.label(target.type)
                    } else {
                        HrOrderKind.label(target.type)
                    })
                },
                onClose = viewModel::closeDetail,
            )

            state.detailNotice?.let {
                HrFeedbackBanner(it, isError = false) { }
            }
            state.detailError?.let { HrFeedbackBanner(it, isError = true) { } }

            val status = state.detailData?.get("status")
                ?.takeIf { it.isJsonPrimitive }?.asString

            if (state.detailLoading && state.detailData == null) {
                HrLoading()
            } else {
                val data = state.detailData
                if (data != null) {
                    if (target.kind == HrDocumentKind.APPLICATION && status != null) {
                        HrTag(
                            text = hrStatusLabel(status),
                            foreground = if (status == "PendingApproval") HrColors.Amber else HrColors.Olive,
                            background = if (status == "PendingApproval") HrColors.AmberSoft else HrColors.OliveSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        documentFields(data).forEach { (label, value) ->
                            HrInfoRow(label, value)
                        }
                    }
                } else if (state.detailError == null) {
                    HrLoading()
                }

                Spacer(Modifier.height(8.dp))
                HrDivider()
                HrSectionHeader(
                    title = "İmzalanmış sənəd",
                    actionLabel = if (state.attachmentUploading) "Yüklənir…" else "+ Fayl yüklə",
                    actionEnabled = !state.attachmentUploading,
                    onAction = { picker.launch(arrayOf("application/pdf", "image/*")) },
                )
                Text(
                    text = "Sənədi yükləyin, imzalayın və skan edilmiş nüsxəsini buraya əlavə edin.",
                    fontSize = 11.sp,
                    color = HrColors.MutedSoft,
                )
                state.attachmentError?.let {
                    Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 6.dp))
                }
                val files = state.attachments
                when {
                    files == null -> HrLoading()
                    files.isEmpty() -> Text(
                        "Hələ imzalanmış fayl yoxdur.",
                        fontSize = 12.sp,
                        color = HrColors.MutedSoft,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    else -> files.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.downloadAttachment(context, file) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.AttachFile, null, tint = HrColors.MutedSoft, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.fileName,
                                    fontSize = 12.sp,
                                    color = HrColors.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(Format.dateTime(file.createdAt), fontSize = 10.sp, color = HrColors.MutedSoft)
                            }
                            IconButton(
                                onClick = { viewModel.downloadAttachment(context, file) },
                                enabled = state.attachmentBusyId == null,
                            ) {
                                Icon(Icons.Default.Visibility, "Sənədə bax", tint = HrColors.Muted, modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deleteAttachment(file.id) },
                                enabled = state.attachmentBusyId == null,
                            ) {
                                Icon(Icons.Default.Delete, "Sil", tint = HrColors.Error, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HrDivider()
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val canDelete = when (target.kind) {
                        HrDocumentKind.APPLICATION -> status == "PendingApproval"
                        HrDocumentKind.ORDER -> target.type in HR_DELETABLE_ORDERS
                    }
                    if (canDelete) {
                        HrSecondaryButton(
                            text = if (state.detailBusy == "delete") "Silinir…" else "Sil",
                            onClick = viewModel::deleteCurrent,
                            enabled = state.detailBusy == null,
                        )
                    }
                    if (target.kind == HrDocumentKind.APPLICATION && status == "PendingApproval") {
                        HrSecondaryButton(
                            text = if (state.detailBusy == "convert") "Çevrilir…" else "Əmrə çevir",
                            onClick = viewModel::convertCurrent,
                            enabled = state.detailBusy == null,
                        )
                    }
                    HrPrimaryButton(
                        text = if (state.detailBusy == "pdf") "Hazırlanır…" else "Sənədi aç",
                        onClick = { viewModel.downloadPdf(context) },
                        enabled = state.detailBusy == null,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    state.preview?.let { document ->
        DocumentPreviewDialog(document = document, onClose = viewModel::closePreview)
    }

    if (state.convertOpen) {
        AppSheet(onDismissRequest = viewModel::closeConvert, scrollable = true) {
            HrSheetTitle("İşə qəbul əmri", viewModel::closeConvert)
            Text(
                text = "Vakansiyaya müraciət əmrə çevrilir — müqavilənin müddəti və əsası göstərilməlidir.",
                fontSize = 11.sp,
                color = HrColors.MutedSoft,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HrDateField("Müqavilənin başlanğıcı", state.convertStart) {
                viewModel.setConvertField(start = it)
            }
            Spacer(Modifier.height(8.dp))
            HrDateField("Müqavilənin bitməsi", state.convertEnd) {
                viewModel.setConvertField(end = it)
            }
            Spacer(Modifier.height(8.dp))
            val cases = state.convertCases
            if (cases == null) {
                HrLoading()
            } else {
                val articles = cases.filter { it.parentId == null }.associateBy { it.id }
                val options = cases.filter { it.parentId != null }.map { item ->
                    item.id to listOfNotNull(
                        articles[item.parentId]?.code?.let { code -> "maddə $code" },
                        "${item.code}) ${item.name}",
                    ).joinToString(" · ")
                }
                HrSelectField(
                    label = "Müddətli müqavilənin əsası *",
                    value = state.convertCaseId,
                    options = listOf("" to "Seçin…") + options,
                    onSelect = { viewModel.setConvertField(caseId = it) },
                )
            }
            state.convertError?.let {
                Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HrSecondaryButton("Ləğv et", viewModel::closeConvert, enabled = !state.convertSaving)
                HrPrimaryButton(
                    text = if (state.convertSaving) "Çevrilir…" else "Əmrə çevir",
                    onClick = viewModel::submitConvert,
                    enabled = !state.convertSaving,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun hrDocumentsViewModel(kind: HrDocumentKind, employeeId: String?): HrDocumentsViewModel =
    androidx.lifecycle.viewmodel.compose.viewModel(
        key = "hr-docs-${employeeId ?: "all"}-${kind.name}",
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer { HrDocumentsViewModel() }
        },
    )

/** İşçi kartındaki "Ərizə və əmrlər" bölməsi — yalnız həmin işçinin sənədləri. */
@Composable
internal fun HrEmployeeDocumentsTab(employeeId: String) {
    var kind by remember { mutableStateOf(HrDocumentKind.APPLICATION) }
    Column(modifier = Modifier.fillMaxSize()) {
        HrPillRow(
            pills = listOf(
                HrPill("Ərizələr", kind == HrDocumentKind.APPLICATION) { kind = HrDocumentKind.APPLICATION },
                HrPill("Əmrlər", kind == HrDocumentKind.ORDER) { kind = HrDocumentKind.ORDER },
            ),
        )
        Spacer(Modifier.height(6.dp))
        HrDocumentsTab(kind = kind, employeeId = employeeId)
    }
}

private fun JsonElement?.intOrNull(): Int? = this
    ?.takeIf { it.isJsonPrimitive }
    ?.let { runCatching { it.asInt }.getOrNull() }

@Composable
private fun DocumentCard(row: HrDocumentRow, kind: HrDocumentKind, onClick: () -> Unit) {
    val typeLabel = row.type?.let {
        if (kind == HrDocumentKind.APPLICATION) HrApplicationKind.label(it) else HrOrderKind.label(it)
    } ?: "—"
    HrCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (kind == HrDocumentKind.APPLICATION) "Ərizə" else "Əmr"} №${row.number ?: "—"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                )
                Text(typeLabel, fontSize = 11.sp, color = HrColors.MutedSoft)
            }
            if (row.status != null) {
                val pending = row.status == "PendingApproval"
                HrTag(
                    text = hrStatusLabel(row.status),
                    foreground = if (pending) HrColors.Amber else HrColors.Olive,
                    background = if (pending) HrColors.AmberSoft else HrColors.OliveSoft,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = row.personName?.takeIf { it.isNotBlank() } ?: "Bütün işçilər",
            fontSize = 13.sp,
            color = HrColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${Format.dateTime(row.createdAt)} · ${row.createdBy ?: "—"}",
            fontSize = 10.sp,
            color = HrColors.MutedSoft,
        )
    }
}

@Composable
private fun HrDocumentCreateSheet(
    state: HrDocumentsViewModel.UiState,
    kind: HrDocumentKind,
    onKind: (Int) -> Unit,
    onDirectKind: (String) -> Unit,
    onField: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val specs = if (kind == HrDocumentKind.APPLICATION) {
        HR_APPLICATION_FIELDS[state.createKind].orEmpty()
    } else {
        HR_DIRECT_ORDER_FIELDS[state.createDirectKind].orEmpty()
    }.filter { state.fixedEmployeeId == null || it.key != "employeeId" }

    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle(if (kind == HrDocumentKind.APPLICATION) "Yeni ərizə" else "Yeni əmr", onDismiss)

        if (kind == HrDocumentKind.APPLICATION) {
            HrSelectField(
                label = "Ərizə növü",
                value = state.createKind.toString(),
                options = HrApplicationKind.LABELS.entries.sortedBy { it.key }.map { it.key.toString() to it.value },
                onSelect = { onKind(it.toIntOrNull() ?: 5) },
            )
        } else {
            Text(
                text = "Yalnız ərizəsiz verilən əmrlər burada yaradılır. İşə qəbul, məzuniyyət, vəzifə və iş rejimi dəyişikliyi əmrləri müvafiq ərizənin təsdiqi ilə avtomatik yaranır.",
                fontSize = 11.sp,
                color = HrColors.MutedSoft,
            )
            Spacer(Modifier.height(8.dp))
            HrSelectField(
                label = "Əmr növü",
                value = state.createDirectKind,
                options = HR_DIRECT_ORDER_KINDS,
                onSelect = onDirectKind,
            )
        }

        Spacer(Modifier.height(10.dp))

        specs.forEach { spec ->
            when (spec.kind) {
                HrFieldKind.EMPLOYEE -> HrSelectField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    options = listOf("" to "Seçin…") + state.employees.map {
                        it.id to (Format.person(it.name, it.surname, it.fathersName))
                    },
                    onSelect = { onField(spec.key, it) },
                )
                HrFieldKind.JOB -> HrSelectField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    options = listOf("" to "Seçin…") + state.jobs.map { it.id to it.name },
                    onSelect = { onField(spec.key, it) },
                )
                HrFieldKind.SELECT -> HrSelectField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    options = listOf("" to "Seçin…") + spec.options.map { it.value.toString() to it.label },
                    onSelect = { onField(spec.key, it) },
                )
                HrFieldKind.DATE -> HrDateField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    onValueChange = { onField(spec.key, it) },
                )
                HrFieldKind.NUMBER -> HrTextField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    onValueChange = { onField(spec.key, it.filter { char -> char.isDigit() || char == '.' || char == '-' }) },
                    keyboardType = KeyboardType.Number,
                )
                HrFieldKind.TEXT -> HrTextField(
                    label = if (spec.required) "${spec.label} *" else spec.label,
                    value = state.values[spec.key].orEmpty(),
                    onValueChange = { onField(spec.key, it) },
                )
            }
            spec.hint?.let {
                Text(it, fontSize = 10.sp, color = HrColors.MutedSoft, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        state.createError?.let {
            Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(bottom = 6.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton(text = "Ləğv et", onClick = onDismiss, enabled = !state.createSaving)
            HrPrimaryButton(
                text = if (state.createSaving) "Saxlanılır…" else "Yarat",
                onClick = onSubmit,
                enabled = !state.createSaving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

/* ------------------------------ Detail helpers ------------------------------ */

internal val HR_DELETABLE_ORDERS = setOf(1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 13, 14)

internal val HR_APPLICATION_ATTACHMENT_FK: Map<Int, String> = mapOf(
    1 to "JobApplicationId",
    2 to "ApplicationForChangeOfPositionId",
    3 to "VacationCompensationApplicationId",
    4 to "UnpaidLeaveApplicationId",
    5 to "VacationApplicationId",
    6 to "EducationLeaveApplicationId",
    7 to "EmploymentStatusChangeApplicationId",
    8 to "VacationReturnApplicationId",
)

internal val HR_ORDER_ATTACHMENT_FK: Map<Int, String> = mapOf(
    1 to "EmploymentOrderId",
    2 to "OrderForChangeOfPositionId",
    3 to "UnexcusedAbsenceId",
    4 to "WarningId",
    5 to "CompensationOrderId",
    6 to "UnpaidLeaveOrderId",
    7 to "VacationOrderId",
    8 to "EducationLeaveOrderId",
    9 to "WarningId",
    10 to "WarningId",
    11 to "BonusOrderId",
    13 to "WorkOnNonWorkdayOrderId",
    14 to "EmploymentStatusChangeOrderId",
    15 to "VacationReturnOrderId",
)

internal val HR_DOC_FIELD_LABELS: Map<String, String> = mapOf(
    "employee" to "İşçi",
    "currentJob" to "Cari vəzifə",
    "newJob" to "Yeni vəzifə",
    "job" to "Vəzifə",
    "jobApplication" to "Vakansiyaya müraciət",
    "laborCodeCase" to "Müqavilənin əsası",
    "orderExecutionSupervisor" to "Əmrin icrasına nəzarət edən",
    "startDate" to "Başlanğıc tarixi",
    "endDate" to "Bitmə tarixi",
    "setDate" to "Tarix",
    "returnDate" to "İşə qayıtma tarixi",
    "returnToWorkDate" to "İşə çıxma tarixi",
    "unusedDaysReturned" to "Balansa qaytarılan gün",
    "requestedDays" to "Gün sayı",
    "totalRequestedDays" to "Gün sayı",
    "vacationDays" to "Məzuniyyət günləri",
    "compensatedDays" to "Kompensasiya günləri",
    "workYearStart" to "İş ilinin başlanğıcı",
    "workYearEnd" to "İş ilinin sonu",
    "notes" to "Qeyd",
    "reason" to "Səbəb",
    "currentEmploymentType" to "Cari iş rejimi",
    "newEmploymentType" to "Yeni iş rejimi",
    "bonusQuantity" to "Mükafat məbləği",
    "salaryMonth" to "Maaş ayı",
    "salaryYear" to "Maaş ili",
    "district" to "Rayon / məhkəmə",
    "judgementNo" to "Qərar №",
    "judgementDate" to "Qərar tarixi",
    "percentageSalary" to "Tutulma faizi",
    "stateFee" to "Dövlət rüsumu",
    "creditor" to "Alıcı (kreditor)",
    "debt" to "Borc",
    "totalPaid" to "Ödənilmiş məbləğ",
    "remainingDebt" to "Qalan borc",
    "disciplinaryType" to "İntizam növü",
    "name" to "Ad",
    "surname" to "Soyad",
    "fathersName" to "Ata adı",
    "gender" to "Cins",
    "telephone" to "Telefon",
    "homeTelephoneNumber" to "Ev telefonu",
    "address" to "Ünvan",
    "createdBy" to "Yaradan",
    "createdAt" to "Yaradılma tarixi",
    "updatedAt" to "Son dəyişiklik",
)

private val HR_HIDDEN_KEYS = setOf(
    "id", "applicationNumber", "orderNumber", "status", "createdAt", "updatedAt", "createdBy",
)

internal fun documentFields(data: JsonObject): List<Pair<String, String>> =
    data.entrySet()
        .filter { (key, value) ->
            key !in HR_HIDDEN_KEYS &&
                key in HR_DOC_FIELD_LABELS &&
                value != null &&
                !value.isJsonNull &&
                !(value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString.isBlank())
        }
        .map { (key, value) -> HR_DOC_FIELD_LABELS.getValue(key) to formatDocumentValue(key, value) }

private fun formatDocumentValue(key: String, value: JsonElement): String {
    if (value.isJsonObject) {
        val obj = value.asJsonObject
        obj.get("name")?.takeIf { it.isJsonPrimitive }?.let { return it.asString }
        return obj.toString()
    }
    if (value.isJsonPrimitive) {
        val primitive = value.asJsonPrimitive
        if (primitive.isBoolean) return if (primitive.asBoolean) "Bəli" else "Xeyr"
        if (primitive.isNumber) {
            val number = primitive.asDouble
            return when (key) {
                "bonusQuantity", "stateFee", "debt", "totalPaid", "remainingDebt" -> Format.money(number)
                "percentageSalary" -> "${Format.number(number)}%"
                "salaryMonth" -> HrEnums.month(number.toInt())
                "disciplinaryType" -> HrEnums.label(HrEnums.DISCIPLINARY_TYPES, number.toInt())
                "requestedDays", "totalRequestedDays", "vacationDays", "compensatedDays" -> "${Format.number(number)} gün"
                else -> if (number % 1.0 == 0.0) number.toInt().toString() else Format.number(number)
            }
        }
        val text = primitive.asString
        return when {
            Regex("^\\d{4}-\\d{2}-\\d{2}T").containsMatchIn(text) ->
                if (key == "createdAt" || key == "updatedAt") Format.dateTime(text) else Format.date(text)
            Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text) -> Format.date(text)
            else -> when (text) {
                "Male" -> "Kişi"
                "Female" -> "Qadın"
                "FullTime" -> "Tam ştat"
                "HalfTime" -> "Yarım ştat"
                else -> text
            }
        }
    }
    if (value.isJsonArray) {
        val array: JsonArray = value.asJsonArray
        return array.mapNotNull { element ->
            if (element.isJsonObject) element.asJsonObject.get("name")?.takeIf { it.isJsonPrimitive }?.asString
            else element.takeIf { it.isJsonPrimitive }?.asString
        }.joinToString(", ")
    }
    return value.toString()
}
