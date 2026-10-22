package az.mtk.app.data.api

/**
 * Body of the server-side search endpoints (buildings/owners/garages/payments/HR).
 * Pages are zero-based, exactly like MTK-UI sends them.
 */
data class SearchBody(
    val filters: List<QueryFilter>? = null,
    val sortCriteria: SortCriteria? = null,
    val searchTerm: String? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)

data class QueryFilter(
    val columnName: String,
    val comparison: Int,
    val value: Any?,
)

data class SortCriteria(
    val columnName: String,
    val direction: Int,
)

object Comparison {
    const val EQUALS = 0
    const val NOT_EQUALS = 1
    const val CONTAINS = 2
    const val STARTS_WITH = 3
    const val ENDS_WITH = 4
    const val GREATER_THAN = 5
    const val GREATER_THAN_OR_EQUAL = 6
    const val LESS_THAN = 7
    const val LESS_THAN_OR_EQUAL = 8
    const val IS_NULL = 9
    const val IS_NOT_NULL = 10
    const val IN = 11
    const val NOT_IN = 12
}

object Sort {
    const val ASCENDING = 0
    const val DESCENDING = 1
}
