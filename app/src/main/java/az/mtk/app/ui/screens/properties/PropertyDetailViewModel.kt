package az.mtk.app.ui.screens.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.*
import az.mtk.app.util.toUserMessage
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PropertyDetailViewModel(
    initialPropertyId: String,
    initialPropertyType: PropertyType,
) : ViewModel() {

    private var currentPropertyId = initialPropertyId
    private var currentPropertyType = initialPropertyType

    init {
        println("📋 PropertyDetailViewModel created: ID=$currentPropertyId, type=$currentPropertyType")
    }

    private val buildingsApi = AppGraph.service(BuildingsApi::class.java)
    private val paymentsApi = AppGraph.service(PaymentsApi::class.java)

    data class UiState(
        val loading: Boolean = true,
        val property: PropertyDetail? = null,
        val error: String? = null,
        val ownershipHistory: List<OwnershipRecordDto> = emptyList(),
        val charges: List<ChargeDto> = emptyList(),
        val payments: List<PaymentDto> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun load(propertyId: String = currentPropertyId, propertyType: PropertyType = currentPropertyType) {
        currentPropertyId = propertyId
        currentPropertyType = propertyType

        viewModelScope.launch {
            _state.value = UiState(loading = true)

            try {
                println("🔄 Loading property: ID=$currentPropertyId, type=$currentPropertyType")
                val property = when (currentPropertyType) {
                    PropertyType.APARTMENT -> loadApartmentDetail(currentPropertyId)
                    PropertyType.GARAGE -> loadGarageDetail(currentPropertyId)
                }

                // Load additional data in parallel
                val ownershipHistory = if (currentPropertyType == PropertyType.APARTMENT) {
                    try {
                        buildingsApi.getOwnershipHistory(currentPropertyId)
                    } catch (e: Exception) {
                        println("⚠️ Failed to load ownership history: ${e.message}")
                        emptyList()
                    }
                } else emptyList()

                // Get ownerId from property's cached owner data to avoid duplicate API call
                val ownerId = property.ownerId

                val (charges, payments) = if (ownerId != null) {
                    try {
                        println("📞 API Call: getChargesByOwner($ownerId)")
                        val chargesPage = paymentsApi.getChargesByOwner(ownerId).data
                        println("✅ Got ${chargesPage?.charges?.size ?: 0} charges")

                        println("📞 API Call: getPaymentsByProperty($currentPropertyId)")
                        val paymentslist = paymentsApi.getPaymentsByProperty(currentPropertyId).data
                        println("✅ Got ${paymentslist?.size ?: 0} payments")

                        Pair(chargesPage?.charges.orEmpty(), paymentslist.orEmpty())
                    } catch (e: Exception) {
                        println("⚠️ Failed to load charges/payments: ${e.message}")
                        e.printStackTrace()
                        Pair(emptyList(), emptyList())
                    }
                } else {
                    println("⚠️ No ownerId - skipping charges/payments")
                    Pair(emptyList(), emptyList())
                }

                _state.value = _state.value.copy(
                    loading = false,
                    property = property,
                    error = null,
                    ownershipHistory = ownershipHistory,
                    charges = charges,
                    payments = payments,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toUserMessage(),
                )
            }
        }
    }

    private suspend fun loadApartmentDetail(apartmentId: String): PropertyDetail {
        // Get apartment details
        println("📞 API Call: getApartmentById($apartmentId)")
        val apartment = buildingsApi.getApartmentById(apartmentId)
        println("✅ API Response: apartment.id=${apartment.id}, apartmentNumber=${apartment.apartmentNumber}")

        // Get building details
        val building = apartment.building?.id?.let { buildingId ->
            try {
                buildingsApi.getBuildingById(buildingId)
            } catch (e: Exception) {
                null
            }
        }

        // Get owner details
        val owner = apartment.currentOwner?.id?.let { ownerId ->
            try {
                buildingsApi.getOwnerById(ownerId)
            } catch (e: Exception) {
                null
            }
        }

        // Get balance info
        val balance = apartment.currentOwner?.id?.let { ownerId ->
            try {
                paymentsApi.ownerBalance(ownerId).data
            } catch (e: Exception) {
                null
            }
        }

        // Get last payment and monthly rate
        val lastPaymentAmount: Double? = apartment.currentOwner?.id?.let { ownerId ->
            try {
                val paymentsPage = paymentsApi.getPaymentsByOwner(ownerId).data
                    ?: emptyList()
                if (paymentsPage.isNotEmpty()) {
                    paymentsPage.maxByOrNull { it.paymentDate.orEmpty() }?.amount
                } else null
            } catch (e: Exception) {
                null
            }
        }
        val lastPaymentDate: String? = apartment.currentOwner?.id?.let { ownerId ->
            try {
                val paymentsPage = paymentsApi.getPaymentsByOwner(ownerId).data
                    ?: emptyList()
                if (paymentsPage.isNotEmpty()) {
                    paymentsPage.maxByOrNull { it.paymentDate.orEmpty() }?.paymentDate
                        ?.let { parsePaymentDate(it) }
                } else null
            } catch (e: Exception) {
                null
            }
        }
        val monthlyRate: Double? = apartment.currentOwner?.id?.let { ownerId ->
            try {
                val chargesPage = paymentsApi.getChargesPage(ownerId).data
                val charges = chargesPage?.charges?.orEmpty() ?: emptyList()
                charges.find { it.rateAmount != null }?.rateAmount ?: charges.firstOrNull()?.amount
            } catch (e: Exception) {
                null
            }
        }

        // Get utilities for apartment
        val utilities: List<Utility> = emptyList() // TODO: implement API call to fetch utilities

        // Get owner's other properties
        val otherProperties = owner?.let {
            val apartments = it.apartments.orEmpty().filter { apt -> apt.id != apartmentId }
            val garages = it.garages.orEmpty()

            apartments.map { apt ->
                OtherProperty(
                    id = apt.id,
                    type = PropertyItemType.APARTMENT,
                    title = "${apt.building?.name ?: "—"} · Mənzil ${apt.apartmentNumber ?: "—"}",
                    subtitle = "${apt.floor ?: 0}-ci mərtəbə · ${apt.roomCount ?: 0} otaq",
                )
            } + garages.map { garage ->
                OtherProperty(
                    id = garage.id,
                    type = PropertyItemType.GARAGE,
                    title = "Qaraj ${garage.garageNumber ?: "—"}",
                    subtitle = garage.type ?: "—",
                )
            }
        }.orEmpty()

        return PropertyDetail(
            id = apartment.id,
            type = PropertyItemType.APARTMENT,
            title = "Mənzil ${apartment.apartmentNumber ?: "—"}",
            buildingName = apartment.building?.name ?: "—",
            buildingAddress = building?.fullAddress ?: "—",
            floor = apartment.floor,
            roomCount = apartment.roomCount,
            area = apartment.areaSquareMeters,
            direction = null,
            status = apartment.status ?: "unknown",
            ownerId = apartment.currentOwner?.id,
            ownerName = owner?.fullName,
            ownerPhone = owner?.phoneNumber,
            ownerEmail = owner?.email,
            contractNumber = null,
            contractDate = null,
            balance = balance?.currentBalance,
            lastPaymentAmount = lastPaymentAmount,
            lastPaymentDate = lastPaymentDate,
            monthlyRate = monthlyRate,
            otherProperties = otherProperties,
            utilities = utilities,
            imageUrl = null,
        )
    }

    private fun parsePaymentDate(dateStr: String): String {
        return try {
            val iso = dateStr.substring(0..9) + "T" + dateStr.substring(10..18)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(iso) ?: return dateStr
            )
        } catch (e: Exception) {
            dateStr
        }
    }

    private suspend fun loadGarageDetail(garageId: String): PropertyDetail {
        // Get garage details
        println("📞 API Call: getGarageById($garageId)")
        val garage = buildingsApi.getGarageById(garageId).data
            ?: throw Exception("Qaraj tapılmadı")
        println("✅ API Response: garage.id=${garage.id}, garageNumber=${garage.garageNumber}")

        // Get owner details
        val owner = garage.owner?.id?.let { ownerId ->
            try {
                buildingsApi.getOwnerById(ownerId)
            } catch (e: Exception) {
                null
            }
        }

        // Get balance info
        val balance = garage.owner?.id?.let { ownerId ->
            try {
                paymentsApi.ownerBalance(ownerId).data
            } catch (e: Exception) {
                null
            }
        }

        // Get last payment and monthly rate
        val lastPaymentAmount: Double? = garage.owner?.id?.let { ownerId ->
            try {
                val paymentsPage = paymentsApi.getPaymentsByOwner(ownerId).data
                    ?: emptyList()
                if (paymentsPage.isNotEmpty()) {
                    paymentsPage.maxByOrNull { it.paymentDate.orEmpty() }?.amount
                } else null
            } catch (e: Exception) {
                null
            }
        }
        val lastPaymentDate: String? = garage.owner?.id?.let { ownerId ->
            try {
                val paymentsPage = paymentsApi.getPaymentsByOwner(ownerId).data
                    ?: emptyList()
                if (paymentsPage.isNotEmpty()) {
                    paymentsPage.maxByOrNull { it.paymentDate.orEmpty() }?.paymentDate
                        ?.let { parsePaymentDate(it) }
                } else null
            } catch (e: Exception) {
                null
            }
        }
        val monthlyRate: Double? = garage.owner?.id?.let { ownerId ->
            try {
                val chargesPage = paymentsApi.getChargesPage(ownerId).data
                val charges = chargesPage?.charges?.orEmpty() ?: emptyList()
                charges.find { it.rateAmount != null }?.rateAmount ?: charges.firstOrNull()?.amount
            } catch (e: Exception) {
                null
            }
        }

        val utilities: List<Utility> = emptyList()

        // Get owner's other properties
        val otherProperties = owner?.let {
            val apartments = it.apartments.orEmpty()
            val garages = it.garages.orEmpty().filter { g -> g.id != garageId }

            apartments.map { apt ->
                OtherProperty(
                    id = apt.id,
                    type = PropertyItemType.APARTMENT,
                    title = "${apt.building?.name ?: "—"} · Mənzil ${apt.apartmentNumber ?: "—"}",
                    subtitle = "${apt.floor ?: 0}-ci mərtəbə · ${apt.roomCount ?: 0} otaq",
                )
            } + garages.map { g ->
                OtherProperty(
                    id = g.id,
                    type = PropertyItemType.GARAGE,
                    title = "Qaraj ${g.garageNumber ?: "—"}",
                    subtitle = g.type ?: "—",
                )
            }
        }.orEmpty()

        return PropertyDetail(
            id = garage.id,
            type = PropertyItemType.GARAGE,
            title = "Qaraj ${garage.garageNumber ?: "—"}",
            buildingName = "—",
            buildingAddress = garage.description ?: "Yeraltı dayanacaq",
            floor = null,
            roomCount = null,
            area = null, // Not in API
            direction = null,
            status = if (garage.owner != null) "occupied" else "vacant",
            ownerId = garage.owner?.id,
            ownerName = owner?.fullName,
            ownerPhone = owner?.phoneNumber,
            ownerEmail = owner?.email,
            contractNumber = null,
            contractDate = null,
            balance = balance?.currentBalance,
            lastPaymentAmount = lastPaymentAmount,
            lastPaymentDate = lastPaymentDate,
            monthlyRate = monthlyRate,
            otherProperties = otherProperties,
            utilities = utilities,
            imageUrl = null,
        )
    }
}

// Data Models

data class PropertyDetail(
    val id: String,
    val type: PropertyItemType,
    val title: String,
    val buildingName: String,
    val buildingAddress: String,
    val floor: Int?,
    val roomCount: Int?,
    val area: Double?,
    val direction: String?,
    val status: String,
    val ownerId: String?,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerEmail: String?,
    val contractNumber: String?,
    val contractDate: String?,
    val balance: Double?,
    val lastPaymentAmount: Double?,
    val lastPaymentDate: String?,
    val monthlyRate: Double?,
    val otherProperties: List<OtherProperty>,
    val utilities: List<Utility>,
    val imageUrl: String?,
)

data class Utility(
    val name: String,
    val hasActive: Boolean,
)

data class OtherProperty(
    val id: String,
    val type: PropertyItemType,
    val title: String,
    val subtitle: String,
)
