package az.mtk.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.AdminApi
import az.mtk.app.data.api.AdmCompanyResponse
import az.mtk.app.data.api.AdmUpsertCompanyRequest
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SecondaryButton
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompanyProfileViewModel : ViewModel() {
    private val api = AppGraph.service(AdminApi::class.java)

    data class UiState(
        val loading: Boolean = true,
        val company: AdmCompanyResponse? = null,
        /** "Company.NotFound" — profil hələ qurulmayıb, yaratma forması göstərilir. */
        val notFound: Boolean = false,
        val error: String? = null,
        val saving: Boolean = false,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { api.company() }
                .onSuccess { env ->
                    val company = env.data
                    _state.value = _state.value.copy(
                        loading = false,
                        company = company,
                        notFound = company == null,
                        error = if (company == null) (env.message ?: "Şirkət profili tapılmadı") else null,
                    )
                }
                .onFailure { failure ->
                    // Profil hələ qurulmayıbsa 404 gəlir — bu, yaratma forması üçün siqnaldır.
                    val isNotFound = failure is retrofit2.HttpException && failure.code() == 404
                    _state.value = _state.value.copy(
                        loading = false,
                        notFound = isNotFound,
                        error = if (isNotFound) null else failure.toUserMessage(),
                    )
                }
        }
    }

    fun save(
        name: String,
        director: String,
        address: String,
        email: String,
        phone: String,
        workHours: String,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            runCatching {
                api.upsertCompany(
                    AdmUpsertCompanyRequest(
                        name = name.trim(),
                        director = director.trim().takeIf { it.isNotEmpty() },
                        address = address.trim(),
                        email = email.trim().takeIf { it.isNotEmpty() },
                        phone = phone.trim().takeIf { it.isNotEmpty() },
                        workHours = workHours.trim().takeIf { it.isNotEmpty() },
                    ),
                )
            }.onSuccess { env ->
                val company = env.data
                _state.value = _state.value.copy(
                    saving = false,
                    company = company,
                    notFound = false,
                    saved = true,
                    error = null,
                )
                onDone(null)
            }.onFailure { failure ->
                _state.value = _state.value.copy(saving = false)
                onDone(failure.toUserMessage())
            }
        }
    }
}

@Composable
fun CompanyProfileScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: CompanyProfileViewModel = mtkViewModel { CompanyProfileViewModel() },
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var workHours by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val company = uiState.company
    val isNew = uiState.notFound

    fun fill(c: AdmCompanyResponse?) {
        name = c?.name ?: ""
        director = c?.director ?: ""
        address = c?.address ?: ""
        email = c?.email ?: ""
        phone = c?.phone ?: ""
        workHours = c?.workHours ?: ""
    }

    LaunchedEffect(company) { fill(company) }
    LaunchedEffect(Unit) { viewModel.load() }

    // Profil yoxdursa birbaşa yaratma formasıdır.
    val isEditing = editing || isNew

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Şirkət profili",
                subtitle = "Rekvizitlər qəbz və sənədlərdə göstərilir",
                onBack = onBack,
            )
        }

        when {
            uiState.loading -> LoadingState("Şirkət profili yüklənir…")
            uiState.error != null && company == null && !isNew ->
                Column(Modifier.padding(16.dp)) {
                    az.mtk.app.ui.components.ErrorState(uiState.error!!, onRetry = viewModel::load)
                }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (isNew) {
                    MtkCard(
                        containerColor = MtkColors.WarningBg,
                        borderColor = MtkColors.Warning.copy(alpha = 0.3f),
                    ) {
                        Text(
                            "Şirkət profili hələ qurulmayıb",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MtkColors.Warning,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Aşağıdakı məlumatları doldurub profili yaradın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MtkColors.Muted,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (isEditing) {
                    SectionHeading(
                        title = if (isNew) "Profili yarat" else "Profili redaktə et",
                        subtitle = "Bu rekvizitlər əmək haqqı qəbzi, ödəniş qəbzi və sənəd PDF-lərində göstərilir.",
                    )
                    Spacer(Modifier.height(12.dp))
                    MtkCard {
                        MtkTextField(name, { name = it }, "Şirkət adı", required = true)
                        Spacer(Modifier.height(12.dp))
                        MtkTextField(director, { director = it }, "Direktor")
                        Spacer(Modifier.height(12.dp))
                        MtkTextField(address, { address = it }, "Ünvan", required = true)
                        Spacer(Modifier.height(12.dp))
                        MtkTextField(email, { email = it }, "E-poçt")
                        Spacer(Modifier.height(12.dp))
                        MtkTextField(phone, { phone = it }, "Telefon")
                        Spacer(Modifier.height(12.dp))
                        MtkTextField(workHours, { workHours = it }, "İş saatları")

                        if (formError != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(formError!!, color = MtkColors.Negative, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(16.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                if (!isNew) {
                                    SecondaryButton(
                                        text = "Ləğv et",
                                        enabled = !uiState.saving,
                                        onClick = {
                                            fill(company)
                                            formError = null
                                            editing = false
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.weight(0.1f))
                            Column(Modifier.weight(1f)) {
                                PrimaryButton(
                                    text = if (uiState.saving) "Saxlanılır…" else if (isNew) "Profili yarat" else "Yadda saxla",
                                    enabled = name.isNotBlank() && address.isNotBlank() && !uiState.saving,
                                    loading = uiState.saving,
                                    onClick = {
                                        formError = null
                                        viewModel.save(name, director, address, email, phone, workHours) { message ->
                                            if (message == null) editing = false else formError = message
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else {
                    SectionHeading(
                        title = "Şirkət məlumatları",
                        subtitle = if (uiState.saved) "✓ Dəyişikliklər saxlanıldı" else null,
                        action = {
                            az.mtk.app.ui.components.SecondaryButton(
                                text = "Redaktə et",
                                onClick = { editing = true },
                                modifier = Modifier.fillMaxWidth(0.5f),
                            )
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    MtkCard {
                        InfoRow("Şirkət adı", company?.name ?: "—")
                        InfoRow("Direktor", company?.director ?: "—")
                        InfoRow("Ünvan", company?.address ?: "—")
                        InfoRow("E-poçt", company?.email ?: "—")
                        InfoRow("Telefon", company?.phone ?: "Əlavə olunacaq")
                        InfoRow("İş saatları", company?.workHours ?: "Əlavə olunacaq")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
