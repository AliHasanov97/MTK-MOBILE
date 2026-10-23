package az.mtk.app.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.InvEnums
import az.mtk.app.data.api.InvNomenclatureResponse
import az.mtk.app.data.api.InvPurchaseHistoryItem
import az.mtk.app.data.api.InvTransactionDto
import az.mtk.app.data.api.InventoryRepo
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Material (nomenklatura) detalı — saytdakı `NomenclatureDetailView`-ın
 * mobil qarşılığı: ümumi məlumat, anbar qalığı, son alış qiyməti, alış
 * tarixçəsi və bu material üzrə çıxarışlar.
 */
class InventoryItemViewModel : ViewModel() {

    private val repo = InventoryRepo.repository

    data class UiState(
        val currentId: String? = null,
        val loading: Boolean = true,
        val error: String? = null,
        val nomenclature: InvNomenclatureResponse? = null,
        val stockQuantity: Double = 0.0,
        val history: List<InvPurchaseHistoryItem> = emptyList(),
        val issues: List<InvTransactionDto> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    /**
     * `nomenclatureId` hər dəfə açıq şəkildə ötürülür: ViewModel activity-yə
     * bağlı olduğu üçün eyni instans başqa material üçün də istifadə olunur.
     */
    fun load(nomenclatureId: String) {
        _state.value = UiState(currentId = nomenclatureId, loading = true)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val nomenclature = async { repo.nomenclature(nomenclatureId) }
                    val history = async { repo.purchaseHistory(nomenclatureId) }
                    val issues = async {
                        repo.transactions(
                            transactionType = InvEnums.TRANSACTION_ISSUE,
                            nomenclatureId = nomenclatureId,
                        )
                    }
                    val stock = async { repo.stockOf(nomenclatureId) }

                    Loaded(
                        nomenclature = nomenclature.await(),
                        history = history.await(),
                        issues = issues.await(),
                        stockQuantity = stock.await()?.quantityOnHand ?: 0.0,
                    )
                }
            }.onSuccess { loaded ->
                _state.value = UiState(
                    currentId = nomenclatureId,
                    loading = false,
                    nomenclature = loaded.nomenclature,
                    stockQuantity = loaded.stockQuantity,
                    history = loaded.history,
                    issues = loaded.issues,
                )
            }.onFailure { failure ->
                _state.value = UiState(
                    currentId = nomenclatureId,
                    loading = false,
                    error = failure.toUserMessage(),
                )
            }
        }
    }

    private data class Loaded(
        val nomenclature: InvNomenclatureResponse,
        val history: List<InvPurchaseHistoryItem>,
        val issues: List<InvTransactionDto>,
        val stockQuantity: Double,
    )
}
