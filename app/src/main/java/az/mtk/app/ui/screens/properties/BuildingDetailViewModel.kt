package az.mtk.app.ui.screens.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.ApartmentDto
import az.mtk.app.data.api.ApartmentTypeDto
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.BuildingDto
import az.mtk.app.data.api.CreateApartmentRequest
import az.mtk.app.data.api.CreateApartmentTypeRequest
import az.mtk.app.data.api.MtkService
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One building's page: info, apartment list, apartment types and the write
 * actions the panel offers in "Bina siyahısı" (create type, create apartment).
 */
class BuildingDetailViewModel(private val buildingId: String) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val building: BuildingDto? = null,
        val apartments: List<ApartmentDto> = emptyList(),
        val types: List<ApartmentTypeDto> = emptyList(),
        val error: String? = null,
        val saving: Boolean = false,
        val formError: String? = null,
        val message: String? = null,
    )

    private val buildingsApi = AppGraph.service(BuildingsApi::class.java)
    private val service = AppGraph.service(MtkService::class.java)

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val building = service.building(buildingId)
                val apartments = service.apartmentsByBuilding(buildingId)
                val types = buildingsApi.apartmentTypes(buildingId)
                building to (apartments to types)
            }.onSuccess { (building, lists) ->
                _state.value = _state.value.copy(
                    loading = false,
                    building = building,
                    apartments = lists.first
                        .sortedWith(compareBy({ it.floor ?: 0 }, { it.apartmentNumber.orEmpty() })),
                    types = lists.second,
                    error = null,
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.toUserMessage(),
                )
            }
        }
    }

    fun createType(roomCount: Int, areaSquareMeters: Double, onSaved: () -> Unit) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            runCatching {
                buildingsApi.createApartmentType(
                    buildingId,
                    CreateApartmentTypeRequest(
                        roomCount = roomCount,
                        areaSquareMeters = areaSquareMeters,
                    ),
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    formError = null,
                    message = "Mənzil tipi əlavə edildi.",
                )
                onSaved()
                reloadTypes()
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    formError = it.toUserMessage(),
                )
            }
        }
    }

    fun createApartment(
        apartmentNumber: String,
        floor: Int,
        apartmentTypeId: String,
        onSaved: () -> Unit,
    ) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            runCatching {
                buildingsApi.createApartment(
                    CreateApartmentRequest(
                        buildingId = buildingId,
                        apartmentNumber = apartmentNumber,
                        floor = floor,
                        apartmentTypeId = apartmentTypeId,
                    ),
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    formError = null,
                    message = "Mənzil yaradıldı.",
                )
                onSaved()
                load()
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    formError = it.toUserMessage(),
                )
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(formError = null, message = null)
    }

    private fun reloadTypes() {
        viewModelScope.launch {
            runCatching { buildingsApi.apartmentTypes(buildingId) }
                .onSuccess { types ->
                    _state.value = _state.value.copy(types = types)
                }
        }
    }
}
