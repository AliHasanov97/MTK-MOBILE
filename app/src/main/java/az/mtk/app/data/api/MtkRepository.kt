package az.mtk.app.data.api

data class Page<T>(
    val items: List<T>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
)

object ChargeStatus {
    const val UNPAID = 0
    const val PARTIALLY_PAID = 1
    const val PAID = 2
    const val CANCELLED = 3
}

class MtkRepository(private val api: MtkService) {

    suspend fun buildings(): List<BuildingDto> = api.buildings()

    suspend fun building(id: String): BuildingDto = api.building(id)

    suspend fun apartment(id: String): ApartmentDto = api.apartment(id)

    suspend fun apartmentsByBuilding(id: String): List<ApartmentDto> = api.apartmentsByBuilding(id)

    suspend fun ownershipHistory(id: String): List<OwnershipRecordDto> = api.ownershipHistory(id)

    suspend fun apartments(page: Int, pageSize: Int = PAGE_SIZE, search: String? = null): Page<ApartmentDto> {
        val env = api.apartments(SearchBody(searchTerm = search.clean(), page = page, pageSize = pageSize))
        val payload = env.unwrap("Mənzillər")
        return Page(payload.items.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun owners(page: Int, pageSize: Int = PAGE_SIZE, search: String? = null): Page<OwnerItemDto> {
        val env = api.owners(SearchBody(searchTerm = search.clean(), page = page, pageSize = pageSize))
        val payload = env.unwrap("Sahiblər")
        return Page(payload.items.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun owner(id: String): OwnerDetailDto = api.owner(id)

    suspend fun garages(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        search: String? = null,
        type: String? = null,
    ): Page<GarageDto> {
        val filters = buildList {
            if (type != null) add(QueryFilter("Type", Comparison.EQUALS, type))
        }
        val env = api.garages(
            SearchBody(
                filters = filters.ifEmpty { null },
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Qarajlar")
        return Page(payload.items.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun garage(id: String): GarageDto =
        api.garage(id).data ?: throw ApiException("Qaraj tapılmadı")

    /**
     * [debtOnly] = true keeps open charges (not paid and not cancelled), false
     * keeps only settled ones — the same filters MTK-UI's Borclar view applies.
     */
    suspend fun charges(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        search: String? = null,
        debtOnly: Boolean? = null,
    ): Page<ChargeDto> {
        val filters = when (debtOnly) {
            true -> listOf(
                QueryFilter("Status", Comparison.NOT_EQUALS, ChargeStatus.PAID),
                QueryFilter("Status", Comparison.NOT_EQUALS, ChargeStatus.CANCELLED),
            )
            false -> listOf(QueryFilter("Status", Comparison.EQUALS, ChargeStatus.PAID))
            null -> emptyList()
        }
        val env = api.charges(
            SearchBody(
                filters = filters.ifEmpty { null },
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Borclar")
        return Page(payload.charges.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun employees(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        search: String? = null,
        active: Boolean? = null,
    ): Page<EmployeeDto> {
        val filters = buildList {
            if (active != null) add(QueryFilter("IsActive", Comparison.EQUALS, if (active) 1 else 2))
        }
        val env = api.employees(
            SearchBody(
                filters = filters.ifEmpty { null },
                sortCriteria = SortCriteria("Surname", Sort.ASCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("İşçilər")
        return Page(payload.data.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    suspend fun employee(id: String): EmployeeDetailDto =
        api.employee(id).data ?: throw ApiException("İşçi tapılmadı")

    suspend fun applications(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        search: String? = null,
    ): Page<ApplicationDto> {
        val env = api.applications(
            SearchBody(
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = search.clean(),
                page = page,
                pageSize = pageSize,
            ),
        )
        val payload = env.unwrap("Ərizələr")
        return Page(payload.data.orEmpty(), payload.totalCount ?: 0, payload.page ?: page, payload.pageSize ?: pageSize)
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")

    companion object {
        const val PAGE_SIZE = 20
    }
}
