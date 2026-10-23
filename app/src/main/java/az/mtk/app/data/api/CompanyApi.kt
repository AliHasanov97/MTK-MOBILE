package az.mtk.app.data.api

data class CompanyProfile(
    val name: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val workHours: String?
)

interface CompanyApi {
    @retrofit2.http.GET("api/payments/companies/public")
    suspend fun getPublicProfile(): Envelope<CompanyProfile>
}
