package az.mtk.app.ui.screens.finance

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.FinCreateRateRequest
import az.mtk.app.data.api.FinGarageType
import az.mtk.app.data.api.FinRateResponse
import az.mtk.app.data.api.FinRateType
import az.mtk.app.data.api.FinanceRepository
import az.mtk.app.data.api.finDateOnlyToUtcIso
import az.mtk.app.data.api.finGarageTypeLabel
import az.mtk.app.data.api.finRateTypeLabel
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkDateField
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SectionHeading
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RatesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rates: List<FinRateResponse> = emptyList(),
)

class RatesViewModel : ViewModel() {
    private val _state = MutableStateFlow(RatesUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { FinanceRepository.currentRates() }
                .onSuccess { rates ->
                    _state.value = RatesUiState(loading = false, rates = rates.sortedByDescending { it.effectiveFrom.orEmpty() })
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.toUserMessage())
                }
        }
    }

    fun createRate(
        rateType: Int,
        garageType: Int?,
        amount: Double,
        effectiveFrom: LocalDate,
        description: String?,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                FinanceRepository.createRate(
                    FinCreateRateRequest(
                        rateType = rateType,
                        amount = amount,
                        effectiveFrom = finDateOnlyToUtcIso(effectiveFrom.toString()),
                        description = description,
                        garageType = garageType,
                    ),
                )
            }.onSuccess {
                load()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }

    fun updateRate(id: String, amount: Double, description: String?, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { FinanceRepository.updateRate(id, amount, description) }
                .onSuccess {
                    load()
                    onDone(null)
                }
                .onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun RatesScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: RatesViewModel = mtkViewModel { RatesViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinRateResponse?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Tariflər",
                subtitle = if (state.loading) "Yüklənir…" else "Hazırkı tariflər",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SectionHeading(
                title = "Hazırkı tariflər",
                subtitle = "Aylıq haqq generasiyası bu tariflərə əsaslanır.",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni tarif", onClick = { showCreate = true })
        }

        when {
            state.loading -> LoadingState("Tariflər yüklənir…")
            state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
            state.rates.isEmpty() -> EmptyState("Hələ heç bir tarif təyin edilməyib.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rates, key = { it.id }) { rate ->
                    val isFixedGarage = rate.rateType == FinRateType.FIXED_GARAGE
                    val unit = if (isFixedGarage) "qaraj" else "m²"
                    MtkCard(onClick = { editing = rate }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (isFixedGarage) "Qaraj — ${finGarageTypeLabel(rate.garageType)}" else "Mənzil — m² üzrə",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (!rate.description.isNullOrBlank()) {
                                    Text(
                                        rate.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MtkColors.Muted,
                                    )
                                }
                            }
                            Pill(
                                if (rate.effectiveTo == null) "Qüvvədədir" else "Gözləyir",
                                if (rate.effectiveTo == null) Tone.Success else Tone.Warning,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                "${Format.date(rate.effectiveFrom)} tarixindən tətbiq olunur",
                                style = MaterialTheme.typography.labelMedium,
                                color = MtkColors.MutedSoft,
                            )
                            Text(
                                "${Format.number(rate.amount)} ₼ / $unit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        RateCreateModal(
            onDismiss = { showCreate = false },
            onSubmit = { rateType, garageType, amount, effectiveFrom, description, onDone ->
                viewModel.createRate(rateType, garageType, amount, effectiveFrom, description, onDone)
            },
        )
    }

    editing?.let { rate ->
        RateEditModal(
            rate = rate,
            onDismiss = { editing = null },
            onSubmit = { amount, description, onDone ->
                viewModel.updateRate(rate.id, amount, description, onDone)
            },
        )
    }
}

@Composable
private fun RateCreateModal(
    onDismiss: () -> Unit,
    onSubmit: (rateType: Int, garageType: Int?, amount: Double, effectiveFrom: LocalDate, description: String?, onDone: (String?) -> Unit) -> Unit,
) {
    // Manual tariflər yalnız bir dəfəlik haqqların snapshot-dur — formada verilmir (MTK-UI kimi).
    var rateType by remember { mutableStateOf(FinRateType.PER_SQUARE_METER) }
    var garageType by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf("") }
    var effectiveFrom by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val numericAmount = amount.replace(',', '.').toDoubleOrNull()
    val canSubmit = numericAmount != null && numericAmount > 0

    MtkModal(
        title = "Yeni tarif",
        subtitle = "Mənzillər üçün m² üzrə, qarajlar üçün sabit tarif təyin edin.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Tarif yarat",
        confirmEnabled = canSubmit && !saving,
        onConfirm = {
            if (!canSubmit || saving) return@MtkModal
            saving = true
            error = null
            onSubmit(
                rateType,
                if (rateType == FinRateType.FIXED_GARAGE) garageType else null,
                numericAmount!!,
                effectiveFrom,
                description.trim().ifBlank { null },
            ) { err ->
                saving = false
                if (err == null) onDismiss() else error = err
            }
        },
    ) {
        MtkSelect(
            value = rateType.toString(),
            onValueChange = {
                rateType = it.toIntOrNull() ?: FinRateType.PER_SQUARE_METER
                garageType = null
            },
            label = "Tip",
            required = true,
            options = listOf(
                MtkOption(FinRateType.PER_SQUARE_METER.toString(), finRateTypeLabel(FinRateType.PER_SQUARE_METER)),
                MtkOption(FinRateType.FIXED_GARAGE.toString(), finRateTypeLabel(FinRateType.FIXED_GARAGE)),
            ),
        )
        if (rateType == FinRateType.FIXED_GARAGE) {
            Spacer(Modifier.height(10.dp))
            MtkSelect(
                value = garageType?.toString() ?: "",
                onValueChange = { garageType = it.toIntOrNull() },
                label = "Qaraj növü",
                options = listOf(
                    MtkOption("", "Bütün növlər (defolt)"),
                    MtkOption(FinGarageType.OPEN_PARKING.toString(), finGarageTypeLabel(FinGarageType.OPEN_PARKING)),
                    MtkOption(FinGarageType.COVERED_GARAGE.toString(), finGarageTypeLabel(FinGarageType.COVERED_GARAGE)),
                    MtkOption(FinGarageType.STORAGE.toString(), finGarageTypeLabel(FinGarageType.STORAGE)),
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        MtkNumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Məbləğ (₼)",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkDateField(
            value = effectiveFrom,
            onValueChange = { effectiveFrom = it },
            label = "Qüvvəyə minmə tarixi",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Təsvir",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RateEditModal(
    rate: FinRateResponse,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, description: String?, onDone: (String?) -> Unit) -> Unit,
) {
    var amount by remember { mutableStateOf(rate.amount?.let { az.mtk.app.util.Format.number(it) } ?: "") }
    var description by remember { mutableStateOf(rate.description.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val numericAmount = amount.replace(',', '.').replace(" ", "").toDoubleOrNull()
    val canSubmit = numericAmount != null && numericAmount > 0

    MtkModal(
        title = "Tarifi redaktə et",
        subtitle = "Yalnız məbləğ və təsvir dəyişdirilə bilər.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Yadda saxla",
        confirmEnabled = canSubmit && !saving,
        onConfirm = {
            if (!canSubmit || saving) return@MtkModal
            saving = true
            error = null
            onSubmit(numericAmount!!, description.trim().ifBlank { null }) { err ->
                saving = false
                if (err == null) onDismiss() else error = err
            }
        },
    ) {
        Text(
            finRateTypeLabel(rate.rateType),
            style = MaterialTheme.typography.bodySmall,
            color = MtkColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
        MtkNumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Məbləğ (₼)",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = description,
            onValueChange = { description = it },
            label = "Təsvir",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(4.dp))
    }
}
