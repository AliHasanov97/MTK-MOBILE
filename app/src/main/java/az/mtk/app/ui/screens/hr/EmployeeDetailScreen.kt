package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.EmployeeDetailDto
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.InfoRow
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
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

data class EmployeeDetailState(
    val employee: EmployeeDetailDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class EmployeeDetailViewModel(private val employeeId: String) : ViewModel() {
    private val _state = MutableStateFlow(EmployeeDetailState())
    val state: StateFlow<EmployeeDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { AppGraph.repository.employee(employeeId) }
                .onSuccess { _state.value = EmployeeDetailState(employee = it, loading = false) }
                .onFailure { _state.value = EmployeeDetailState(loading = false, error = it.toUserMessage()) }
        }
    }
}

@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onBack: () -> Unit,
    viewModel: EmployeeDetailViewModel = mtkViewModel { EmployeeDetailViewModel(employeeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.employee?.let { Format.person(it.name, it.surname, it.fathersName) } ?: "İşçi",
            subtitle = state.employee?.job?.name,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> {
                    val employee = state.employee ?: return@Column
                    MtkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(
                                Format.initials(listOfNotNull(employee.name, employee.surname).joinToString(" ")),
                                size = 52.dp,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Format.person(employee.name, employee.surname, employee.fathersName),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    employee.job?.name ?: "Vəzifə göstərilməyib",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                )
                                Text(
                                    "Qeydiyyat № ${employee.registerNumber ?: "—"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Pill(
                                Format.isActiveLabel(employee.isActive),
                                if (employee.isActive == 1) Tone.Success else Tone.Neutral,
                            )
                        }
                    }

                    KeyValueHeader("İş məlumatları")
                    MtkCard {
                        InfoRow("Vəzifə", employee.job?.name ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("İş rejimi", Format.employmentType(employee.employmentType))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("İşə başlama", Format.date(employee.startWorkDate))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Əmək haqqı", Format.money(employee.baseSalary))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Məzuniyyət günü", employee.vacationDays?.toString() ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("İş stajı", Format.experience(employee.organizationWorkExperience))
                    }

                    KeyValueHeader("Əlaqə")
                    MtkCard {
                        InfoRow("Telefon", employee.phoneNumber ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("E-poçt", employee.email ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Faktiki ünvan", employee.currentAddress ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Qeydiyyat ünvanı", employee.registeredAddress ?: "—")
                    }

                    KeyValueHeader("Şəxsi məlumatlar")
                    MtkCard {
                        InfoRow("Doğum tarixi", Format.date(employee.birthDate))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Cins", Format.gender(employee.gender))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("FİN", employee.finCode ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Vəsiqə №", employee.idCardNumber ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Ailə vəziyyəti", Format.maritalStatus(employee.maritalStatus))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        InfoRow("Uşaq sayı", employee.numberOfChildren?.toString() ?: "—")
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
