package az.mtk.app.data.api

import az.mtk.app.AppGraph
import retrofit2.HttpException

/**
 * Admin-only data source for the İdarəetmə module.
 *
 * Mirrors MTK-UI's `app/lib/api/auditLogs.ts` and the company-profile part of
 * `app/lib/api/payments.ts`: one endpoint per module for audit rows, plus a
 * single company profile shared by all PDFs and the public site.
 */
class AdminRepository(
    private val api: AdminApi = AppGraph.service(AdminApi::class.java),
) {
    /**
     * The backend returns 404 with `Company.NotFound` until an admin creates the
     * profile. That is not an error for the UI: it switches to the create form.
     */
    suspend fun companyOrNull(): AdmCompanyResponse? {
        return try {
            val envelope = api.company()
            envelope.data ?: run {
                val message = envelope.message.orEmpty()
                if (message.contains("Company.NotFound", ignoreCase = true)) {
                    null
                } else {
                    throw ApiException(message.ifBlank { "Şirkət profili oxunmadı" })
                }
            }
        } catch (error: HttpException) {
            if (error.code() == 404) null else throw error
        }
    }

    suspend fun upsertCompany(body: AdmUpsertCompanyRequest): AdmCompanyResponse =
        api.upsertCompany(body).unwrap("Şirkət profili")

    suspend fun searchAudit(module: String, body: SearchBody): AdmAuditSearchResult {
        val envelope = when (module) {
            AuditModules.IDENTITY -> api.auditIdentity(body)
            AuditModules.BUILDINGS -> api.auditBuildings(body)
            AuditModules.PAYMENTS -> api.auditPayments(body)
            AuditModules.HR -> api.auditHr(body)
            AuditModules.WAREHOUSE -> api.auditWarehouse(body)
            else -> throw ApiException("Naməlum audit modulu: $module")
        }
        return envelope.unwrap("Audit qeydləri")
    }

    private fun <T> Envelope<T>.unwrap(what: String): T =
        data ?: throw ApiException(message?.takeIf { it.isNotBlank() } ?: "$what üçün boş cavab gəldi")
}
