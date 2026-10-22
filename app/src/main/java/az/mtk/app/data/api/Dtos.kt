package az.mtk.app.data.api

// DTOs mirror MTK API responses exactly as MTK-UI consumes them in its
// lib/api TypeScript modules. Field names match the camelCase JSON.

data class Envelope<T>(
    val success: Boolean?,
    val statusCode: Int?,
    val message: String?,
    val data: T?,
)

data class NamedRef(
    val id: String?,
    val name: String?,
)

data class BuildingDto(
    val id: String,
    val name: String,
    val fullAddress: String?,
    val status: String?,
    val description: String?,
)

data class ApartmentDto(
    val id: String,
    val building: NamedRef?,
    val apartmentNumber: String?,
    val floor: Int?,
    val areaSquareMeters: Double?,
    val roomCount: Int?,
    val status: String?,
    val currentOwner: NamedRef?,
)

data class SearchPage<T>(
    val items: List<T>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class OwnerItemDto(
    val id: String,
    val userId: String?,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val isActive: Boolean?,
)

data class OwnedApartmentDto(
    val id: String,
    val building: NamedRef?,
    val apartmentNumber: String?,
    val floor: Int?,
    val areaSquareMeters: Double?,
    val roomCount: Int?,
    val status: String?,
)

data class OwnedGarageDto(
    val id: String,
    val garageNumber: String?,
    val type: String?,
    val description: String?,
)

data class OwnerDetailDto(
    val id: String,
    val userId: String?,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val isActive: Boolean?,
    val apartments: List<OwnedApartmentDto>?,
    val garages: List<OwnedGarageDto>?,
)

data class GarageDto(
    val id: String,
    val garageNumber: String?,
    val type: String?,
    val description: String?,
    val owner: NamedRef?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class OwnershipRecordDto(
    val id: String,
    val previousOwnerId: String?,
    val previousOwnerName: String?,
    val newOwnerId: String?,
    val newOwnerName: String?,
    val transferDate: String?,
)

data class ChargeDto(
    val id: String,
    val ownerId: String?,
    val apartmentId: String?,
    val garageId: String?,
    val period: String?,
    val amount: Double?,
    val paidAmount: Double?,
    val status: Int?,
    val createdAt: String?,
    val issuedOn: String?,
    val description: String?,
    val areaSquareMeters: Double?,
    val rateAmount: Double?,
    val rateType: Int?,
    val partyName: String?,
    val propertyLabel: String?,
)

data class ChargesPage(
    val charges: List<ChargeDto>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
)

data class EmployeeDto(
    val id: String,
    val registerNumber: Int?,
    val name: String?,
    val surname: String?,
    val fathersName: String?,
    val gender: Int?,
    val birthDate: String?,
    val age: Int?,
    val finCode: String?,
    val phoneNumber: String?,
    val email: String?,
    val education: Int?,
    val maritalStatus: Int?,
    val startWorkDate: String?,
    val job: NamedRef?,
    val isActive: Int?,
    val employmentType: Int?,
    val createdAt: String?,
)

data class WorkExperienceDto(
    val years: Int?,
    val months: Int?,
    val days: Int?,
)

data class EmployeeDetailDto(
    val id: String,
    val registerNumber: Int?,
    val name: String?,
    val surname: String?,
    val fathersName: String?,
    val nationality: String?,
    val gender: Int?,
    val birthDate: String?,
    val finCode: String?,
    val idCardNumber: String?,
    val socialSecurityNumber: String?,
    val contractNumber: String?,
    val salaryBankName: String?,
    val employeeBankAccountNumber: String?,
    val baseSalary: Double?,
    val maritalStatus: Int?,
    val numberOfChildren: Int?,
    val education: Int?,
    val phoneNumber: String?,
    val homePhoneNumber: String?,
    val email: String?,
    val registeredAddress: String?,
    val currentAddress: String?,
    val workingDays: Int?,
    val vacationDays: Int?,
    val totalWorkExperience: WorkExperienceDto?,
    val organizationWorkExperience: WorkExperienceDto?,
    val startWorkDate: String?,
    val job: NamedRef?,
    val isActive: Int?,
    val employmentType: Int?,
    val createdAt: String?,
)

data class ApplicationDto(
    val id: String,
    val applicationNumber: Int?,
    val type: Int?,
    val employee: NamedRef?,
    val jobApplicant: NamedRef?,
    val createdBy: NamedRef?,
    val order: NamedRef?,
    val status: String?,
    val createdAt: String?,
)

data class HrPage<T>(
    val data: List<T>?,
    val totalCount: Int?,
    val page: Int?,
    val pageSize: Int?,
    val pageCount: Int?,
)

/** Legacy endpoints return the bare object (no envelope). */
data class GarageEnvelope(
    val data: GarageDto?,
    val message: String?,
)
