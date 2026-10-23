package az.mtk.app.data.api

import az.mtk.app.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Hesabatlar modulunun məlumat qatı.
 *
 * Saytdakı `OdenisQrafikiView` və `UmumiHesabatView` ilə eyni strategiya:
 * mənzil/qaraj siyahıları və tranzaksiya jurnalı bir dəfə geniş şəkildə
 * çəkilir, bütün filtrasiya/aqreqasiya müştəri tərəfində aparılır.
 */
data class ReportProperty(
    val id: String,
    val kind: Int,
    val label: String,
    val buildingId: String?,
    val buildingName: String?,
) {
    val key: String get() = "$kind:$id"
    val isApartment: Boolean get() = kind == ReportPropertyType.APARTMENT
}

data class ReportMonthCell(
    val month: Int,
    val amount: Double,
    val paidAmount: Double,
    val status: Int?,
) {
    val hasCharge: Boolean get() = status != null
    val remaining: Double get() = (amount - paidAmount).coerceAtLeast(0.0)
}

data class ReportScheduleRow(
    val property: ReportProperty,
    val months: List<ReportMonthCell>,
    val currentDebt: Double,
) {
    val hasDebt: Boolean get() = currentDebt > 0.005
}

object ReportsRepository {

    private val reports: ReportsApi by lazy { AppGraph.service(ReportsApi::class.java) }
    private val buildings: BuildingsApi by lazy { AppGraph.service(BuildingsApi::class.java) }

    /** Mənzillər + qarajlar — illik qrafikin sətirlərini adlandırmaq üçün. */
    suspend fun loadProperties(): List<ReportProperty> = coroutineScope {
        val apartmentsDeferred = async {
            buildings.searchApartments(
                SearchBody(
                    sortCriteria = SortCriteria("ApartmentNumber", Sort.ASCENDING),
                    page = 0,
                    pageSize = 1000,
                ),
            ).unwrap("Mənzillər")
        }
        val garagesDeferred = async {
            buildings.searchGarages(
                SearchBody(page = 0, pageSize = 1000),
            ).unwrap("Qarajlar")
        }

        val apartments = apartmentsDeferred.await().items.orEmpty().map { dto ->
            ReportProperty(
                id = dto.id,
                kind = ReportPropertyType.APARTMENT,
                label = "Mənzil ${dto.apartmentNumber ?: "—"}",
                buildingId = dto.building?.id,
                buildingName = dto.building?.name,
            )
        }
        val garages = garagesDeferred.await().items.orEmpty().map { dto ->
            ReportProperty(
                id = dto.id,
                kind = ReportPropertyType.GARAGE,
                label = "Qaraj ${dto.garageNumber ?: "—"}",
                buildingId = null,
                buildingName = null,
            )
        }
        apartments + garages
    }

    /** İllik hesabat sətirləri, `propertyId` üzrə indekslənmiş. */
    suspend fun annualRows(year: Int): Map<String, ReportPropertyAnnualRowDto> {
        val response = reports.annualPaymentReport(year).unwrap("İllik hesabat")
        return response.properties.orEmpty().associateBy { it.propertyId }
    }

    /** Ümumi hesabat üçün bütün tranzaksiya jurnalı (saytdakı 3000 limiti). */
    suspend fun loadTransactions(): List<ReportTransactionDto> =
        reports.transactions(
            SearchBody(
                sortCriteria = SortCriteria("TransactionDate", Sort.ASCENDING),
                page = 0,
                pageSize = 3000,
            ),
        ).unwrap("Tranzaksiyalar").transactions.orEmpty()

    suspend fun companyBalance(): ReportCompanyBalanceDto =
        reports.companyBalance().unwrap("Rəsmi balans")

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(
            message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi",
        )
}
