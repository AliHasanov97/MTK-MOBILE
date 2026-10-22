package az.mtk.app.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApartmentDto
import az.mtk.app.data.api.GarageDto
import az.mtk.app.data.api.ReportMonthlyChargeDto
import az.mtk.app.data.api.ReportPropertyAnnualRowDto
import az.mtk.app.data.api.ReportPropertyType
import az.mtk.app.data.api.ReportExports
import az.mtk.app.data.api.ReportsApi
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.ExportActionRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

private val AZ_MONTHS_SHORT = listOf(
    "Yan", "Fev", "Mar", "Apr", "May", "İyn",
    "İyl", "Avq", "Sen", "Okt", "Noy", "Dek",
)

private val AZ_MONTHS_FULL = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
    "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
)

/** One grid row: property + its 12 month cells + current debt. */
data class PaymentChartRow(
    val propertyId: String,
    val label: String,
    val months: List<ReportMonthlyChargeDto>?,
    val currentDebt: Double,
)

data class PaymentChartState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<PaymentChartRow> = emptyList(),
)

class PaymentChartViewModel : ViewModel() {
    val currentYear: Int = LocalDate.now().year

    val year = MutableStateFlow(currentYear)
    val type = MutableStateFlow(ReportPropertyType.APARTMENT)
    val state = MutableStateFlow(PaymentChartState())
    private val propertyNames = MutableStateFlow<Map<String, String>>(emptyMap())

    val yearOptions: List<MtkOption> =
        (currentYear - 4..currentYear + 1).map { MtkOption(it.toString(), it.toString()) }

    fun selectYear(value: String) {
        val parsed = value.toIntOrNull() ?: return
        if (parsed == year.value) return
        year.value = parsed
        load()
    }

    fun selectType(value: String) {
        val parsed = when (value) {
            "garage" -> ReportPropertyType.GARAGE
            else -> ReportPropertyType.APARTMENT
        }
        if (parsed == type.value) return
        type.value = parsed
        load()
    }

    init {
        load()
        viewModelScope.launch { loadPropertyNames() }
    }

    fun load() {
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, error = null)
            runCatching {
                val report = AppGraph.service(ReportsApi::class.java)
                    .annualPaymentReport(year.value, type.value)
                (report.data ?: error("İllik hesabat üçün boş cavab gəldi"))
            }
                .onSuccess { report ->
                    state.value = state.value.copy(
                        loading = false,
                        error = null,
                        rows = report.properties.orEmpty()
                            .mapNotNull { row -> row.toChartRow(propertyNames.value) },
                    )
                }
                .onFailure { failure ->
                    state.value = state.value.copy(loading = false, error = failure.toUserMessage())
                }
        }
    }

    /** TS-dəki kimi mənzil/qaraj siyahısı bir dəfə yüklənir və adlar oradan götürülür. */
    private suspend fun loadPropertyNames() {
        coroutineScope {
            runCatching {
                val apartments = AppGraph.repository.apartments(page = 0, pageSize = 1000).items
                val garages = AppGraph.repository.garages(page = 0, pageSize = 1000).items
                val names = buildMap {
                    apartments.forEach { apt ->
                        put(apt.id, apartmentLabel(apt))
                    }
                    garages.forEach { garage ->
                        put(garage.id, garageLabel(garage))
                    }
                }
                names
            }
                .onSuccess { names ->
                    propertyNames.value = names
                    state.value = state.value.copy(
                        rows = state.value.rows.mapNotNull { it.copyLabel(names) },
                    )
                }
                .onFailure { /* adlar olmadan da ID-lərlə göstərmək olar */ }
        }
    }

    private fun ReportPropertyAnnualRowDto.toChartRow(names: Map<String, String>): PaymentChartRow? {
        val kindLabel = if (type.value == ReportPropertyType.GARAGE) "Qaraj" else "Mənzil"
        val label = names[propertyId] ?: "$kindLabel ${propertyId.take(8)}…"
        return PaymentChartRow(
            propertyId = propertyId,
            label = label,
            months = months,
            currentDebt = currentDebt ?: 0.0,
        )
    }

    private fun PaymentChartRow.copyLabel(names: Map<String, String>): PaymentChartRow? {
        val kindLabel = if (type.value == ReportPropertyType.GARAGE) "Qaraj" else "Mənzil"
        return copy(label = names[propertyId] ?: "$kindLabel ${propertyId.take(8)}…")
    }

    private fun apartmentLabel(apt: ApartmentDto): String = buildString {
        append("Mənzil ")
        append(apt.apartmentNumber ?: "—")
        apt.building?.name?.let { append(" · ").append(it) }
    }

    private fun garageLabel(garage: GarageDto): String =
        "Qaraj ${garage.garageNumber ?: "—"}"
}

private fun monthCellColor(status: Int?): Color = when (status) {
    2 -> MtkColors.PositiveBg
    1 -> MtkColors.WarningBg
    0 -> MtkColors.NegativeBg
    else -> MtkColors.Soft
}

private fun monthCellSymbol(status: Int?): String = when (status) {
    2 -> "+"
    1 -> "±"
    0 -> "−"
    else -> "·"
}

@Composable
fun PaymentChartScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: PaymentChartViewModel = mtkViewModel { PaymentChartViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val year by viewModel.year.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var search by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var exportMonth by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(LocalDate.now().monthValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "İllik ödəniş qrafiki",
                subtitle = "${state.rows.size} əmlak · $year",
                onBack = onBack,
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Axtar (mənzil, bina, qaraj…)",
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MtkSelect(
                    value = if (type == ReportPropertyType.GARAGE) "garage" else "apartment",
                    onValueChange = viewModel::selectType,
                    label = "Əmlak növü",
                    options = listOf(
                        MtkOption("apartment", "Mənzillər"),
                        MtkOption("garage", "Qarajlar"),
                    ),
                    modifier = Modifier.weight(1f),
                )
                MtkSelect(
                    value = year.toString(),
                    onValueChange = viewModel::selectYear,
                    label = "İl",
                    options = viewModel.yearOptions,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            // Legend — TS gridindəki hüceyrə mənaları.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pill("Ödənilib (+)", Tone.Success)
                Pill("Qismən (±)", Tone.Warning)
                Pill("Ödənilməyib (−)", Tone.Danger)
                Pill("Haqq yaradılmayıb (·)", Tone.Neutral)
            }
        }

        when {
            state.loading && state.rows.isEmpty() -> LoadingState("Hesabat yüklənir…")
            state.error != null && state.rows.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::load)
            state.rows.isEmpty() -> EmptyState("Əmlak tapılmadı")
            else -> {
                val term = search.trim().lowercase()
                val visible = if (term.isEmpty()) state.rows else state.rows.filter {
                    it.label.lowercase().contains(term)
                }
                if (visible.isEmpty()) {
                    EmptyState("Bu axtarışa uyğun mənzil və ya qaraj yoxdur")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            MtkCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Excel ixracı",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "$year ili — bütün aylar üzrə cədvəl",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MtkColors.Muted,
                                        )
                                    }
                                }
                                ExportActionRow(
                                    title = "İllik hesabat (Excel)",
                                    subtitle = "Seçilmiş əmlak növü üzrə",
                                    path = ReportExports.annualExcel(year, type),
                                    scope = scope,
                                )
                                ExportActionRow(
                                    title = "Aylıq hesabat (PDF)",
                                    subtitle = "Seçilmiş ay üçün",
                                    path = ReportExports.monthlyPdf(year, exportMonth),
                                    scope = scope,
                                )
                                MtkSelect(
                                    value = exportMonth.toString(),
                                    onValueChange = { exportMonth = it.toIntOrNull() ?: exportMonth },
                                    label = "PDF üçün ay",
                                    options = AZ_MONTHS_FULL.mapIndexed { index, name ->
                                        MtkOption((index + 1).toString(), name)
                                    },
                                )
                            }
                        }

                        items(visible, key = { it.propertyId }) { row ->
                            MtkCard(padding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        row.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (row.currentDebt > 0.005) "Borc: ${Format.money(row.currentDebt)}"
                                        else "Borc yoxdur",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (row.currentDebt > 0.005) MtkColors.Negative else MtkColors.Positive,
                                        textAlign = TextAlign.End,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                MonthGrid(row.months)
                            }
                        }

                        if (state.error != null) {
                            item { ErrorState(state.error!!, onRetry = viewModel::load) }
                        }
                        if (state.rows.size > 30) {
                            item {
                                Text(
                                    "Cəmi ${state.rows.size} əmlak göstərilir",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 12 aylıq ştrix-sütunlu mini qrafik: hər ay üçün ödənilmiş/ödənilməmiş payı olan sütun. */
@Composable
private fun MonthGrid(months: List<ReportMonthlyChargeDto>?) {
    Column {
        // Ayların başlıqları
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            AZ_MONTHS_SHORT.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedSoft,
                    fontSize = 8.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            months.orEmpty().take(12).forEachIndexed { index, cell ->
                Column(Modifier.weight(1f)) {
                    // Sütun: ümumi hündürlük normalizasiya olunur, mavi hissə = ödənilib, qırmızı = qalıq.
                    val amount = (cell.amount ?: 0.0).coerceAtLeast(0.0)
                    val paid = (cell.paidAmount ?: 0.0).coerceIn(0.0, amount.takeIf { it > 0 } ?: 0.0)
                    val maxRef = amount.takeIf { it > 0 } ?: 1.0
                    val barHeight = 46.dp
                    val filledFraction = (paid / maxRef).toFloat().coerceIn(0f, 1f)
                    val hasCharge = cell.status != null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(
                                color = if (hasCharge) MtkColors.NegativeBg else MtkColors.Soft,
                                shape = RoundedCornerShape(4.dp),
                            ),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (hasCharge) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight * filledFraction)
                                    .background(
                                        color = if (cell.status == 2) MtkColors.Positive else MtkColors.Warning,
                                        shape = RoundedCornerShape(4.dp),
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(monthCellColor(cell.status), CircleShape)
                            .border(0.5.dp, MtkColors.MutedUltraSoft, CircleShape),
                    )
                    if (cell.status != null && amount > 0.0) {
                        Text(
                            "${monthCellSymbol(cell.status)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MtkColors.Muted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    } else {
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MtkColors.MutedUltraSoft,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
            // 12-dən az ay gəlsə boş sütunlarla tamamlanınca düzlük qorunur.
            repeat((12 - months.orEmpty().take(12).size).coerceAtLeast(0)) {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Ay üzrə aylıq haqq və ödənilən məbləğ: yaşıl = tam ödənilib, sarı = qismən, boş = haqq yaradılmayıb",
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.MutedSoft,
        )
    }
}
