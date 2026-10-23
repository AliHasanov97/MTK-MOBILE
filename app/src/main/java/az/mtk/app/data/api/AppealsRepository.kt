package az.mtk.app.data.api

import az.mtk.app.AppGraph
import retrofit2.HttpException

/** «Yeni müraciət» formasında seçilə bilən obyekt (mənzil və ya qaraj). */
data class AppealPropertyOption(
    val id: String,
    val label: String,
    val isGarage: Boolean,
)

/**
 * Müraciətlər modulunun məlumat qatı.
 *
 * Saytdakı `appeals.ts` ilə eyni endpoint-lər: siyahı serverdə səhifələnir,
 * filtr/sort `SearchBody` ilə göndərilir. Enum-lar backend-də rəqəm ordinalı
 * kimi gəlir (1-dən başlayır).
 */
object AppealsRepository {

    private val api: AppealsApi by lazy { AppGraph.service(AppealsApi::class.java) }
    private val identity: IdentityApi by lazy { AppGraph.service(IdentityApi::class.java) }
    private val owners: OwnerProfileApi by lazy { AppGraph.service(OwnerProfileApi::class.java) }

    suspend fun search(
        status: Int?,
        category: Int?,
        priority: Int?,
        searchTerm: String?,
        page: Int,
        pageSize: Int,
    ): AppealsSearchPage {
        val filters = buildList {
            status?.let { add(QueryFilter("Status", Comparison.EQUALS, it)) }
            category?.let { add(QueryFilter("Category", Comparison.EQUALS, it)) }
            priority?.let { add(QueryFilter("Priority", Comparison.EQUALS, it)) }
        }

        return api.searchAppeals(
            SearchBody(
                filters = filters.takeIf { it.isNotEmpty() },
                sortCriteria = SortCriteria("CreatedAt", Sort.DESCENDING),
                searchTerm = searchTerm?.trim()?.takeIf { it.isNotEmpty() },
                page = page,
                pageSize = pageSize,
            ),
        ).unwrap("Müraciətlər")
    }

    suspend fun detail(id: String): AppealDetailDto =
        api.appeal(id).unwrap("Müraciət")

    /** Backend yeni ID-ni zərfsiz JSON sətri kimi qaytarır. */
    suspend fun create(
        title: String,
        description: String,
        category: Int,
        apartmentId: String?,
        garageId: String?,
    ): String {
        val id = api.createAppeal(
            CreateAppealRequestDto(
                title = title,
                description = description,
                category = category,
                apartmentId = apartmentId,
                garageId = garageId,
            ),
        ).trim().trim('"')

        if (id.isBlank()) throw ApiException("Server yeni müraciətin ID-sini qaytarmadı.")
        return id
    }

    suspend fun addComment(id: String, text: String) {
        api.addComment(id, AddAppealCommentRequestDto(text)).unwrap("Şərh")
    }

    suspend fun updateStatus(id: String, status: Int) {
        api.updateAppealStatus(id, UpdateAppealStatusRequestDto(status)).unwrap("Status")
    }

    /**
     * Sakinin öz mənzil/qarajları. İşçi hesablarında Owner qeydi olmur
     * (404) — bu halda boş siyahı qaytarılır və forma obyektsiz göndərilir.
     */
    suspend fun myProperties(): List<AppealPropertyOption> = try {
        val me = identity.currentUser()
        val owner = owners.ownerByUserId(me.id)
        buildList {
            owner.apartments.orEmpty().forEach { apartment ->
                val building = apartment.building?.name?.takeIf { it.isNotBlank() }
                val label = buildString {
                    append("Mənzil ").append(apartment.apartmentNumber ?: "—")
                    if (building != null) append(" · ").append(building)
                }
                add(AppealPropertyOption(id = apartment.id, label = label, isGarage = false))
            }
            owner.garages.orEmpty().forEach { garage ->
                add(
                    AppealPropertyOption(
                        id = garage.id,
                        label = "Qaraj ${garage.garageNumber ?: "—"}",
                        isGarage = true,
                    ),
                )
            }
        }
    } catch (failure: HttpException) {
        if (failure.code() == 404 || failure.code() == 403) emptyList() else throw failure
    }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(
            message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi",
        )
}
