package az.mtk.app.data.api

// Mirrors MTK-UI app/lib/api/auditLogs.ts + company parts of payments.ts
// (audit across modules and company profile).

// ---------- Audit ----------

object AuditModules {
    const val IDENTITY = "identity"
    const val BUILDINGS = "buildings"
    const val PAYMENTS = "payments"
    const val HR = "hr"
    const val WAREHOUSE = "warehouse"

    val ALL = listOf(
        IDENTITY to "İdentifikasiya",
        BUILDINGS to "Binalar",
        PAYMENTS to "Maliyyə və ödənişlər",
        HR to "HR",
        WAREHOUSE to "İnventar (anbar)",
    )
}

object AuditActions {
    const val CREATED = "Created"
    const val UPDATED = "Updated"
    const val DELETED = "Deleted"

    val ALL = listOf(
        CREATED to "Yaradıldı",
        UPDATED to "Dəyişdirildi",
        DELETED to "Silindi",
    )

    fun label(action: String?): String = when (action) {
        CREATED -> "Yaradıldı"
        UPDATED -> "Dəyişdirildi"
        DELETED -> "Silindi"
        else -> action ?: "—"
    }
}

/** Raw audit log row as the backend writes it (Identity sends userId, others send user). */
data class AdmRawAuditLog(
    val id: String,
    val entityType: String?,
    val entityId: String?,
    val action: String?,
    val oldValues: String?,
    val newValues: String?,
    val userId: String?,
    val user: AdmAuditUser?,
    val timestamp: String?,
)

data class AdmAuditUser(
    val id: String?,
    val name: String?,
)

data class AdmAuditSearchResult(
    val auditLogs: List<AdmRawAuditLog>?,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
)

/** Normalized row shown in the UI. */
data class AdmAuditRow(
    val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val oldValues: String?,
    val newValues: String?,
    val userLabel: String?,
    val timestamp: String?,
)

// ---------- Company profile ----------

data class AdmCompanyResponse(
    val id: String,
    val name: String,
    val director: String?,
    val address: String,
    val email: String?,
    val phone: String?,
    val workHours: String?,
)

data class AdmUpsertCompanyRequest(
    val name: String,
    val director: String? = null,
    val address: String,
    val email: String? = null,
    val phone: String? = null,
    val workHours: String? = null,
)

interface AdminApi {

    // ---- Audit (each module exposes its own auditlogs endpoint) ----
    @retrofit2.http.POST("api/identity/auditlogs/search")
    suspend fun auditIdentity(@retrofit2.http.Body body: SearchBody): Envelope<AdmAuditSearchResult>

    @retrofit2.http.POST("api/buildings/auditlogs/search")
    suspend fun auditBuildings(@retrofit2.http.Body body: SearchBody): Envelope<AdmAuditSearchResult>

    @retrofit2.http.POST("api/payments/auditlogs/search")
    suspend fun auditPayments(@retrofit2.http.Body body: SearchBody): Envelope<AdmAuditSearchResult>

    @retrofit2.http.POST("api/hr/auditlogs/search")
    suspend fun auditHr(@retrofit2.http.Body body: SearchBody): Envelope<AdmAuditSearchResult>

    @retrofit2.http.POST("api/warehouse/auditlogs/search")
    suspend fun auditWarehouse(@retrofit2.http.Body body: SearchBody): Envelope<AdmAuditSearchResult>

    // ---- Company profile ----
    @retrofit2.http.GET("api/payments/companies")
    suspend fun company(): Envelope<AdmCompanyResponse>

    @retrofit2.http.PUT("api/payments/companies")
    suspend fun upsertCompany(@retrofit2.http.Body body: AdmUpsertCompanyRequest): Envelope<AdmCompanyResponse>
}
