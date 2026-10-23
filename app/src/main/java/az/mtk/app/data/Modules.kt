package az.mtk.app.data

import az.mtk.app.data.auth.Roles

/**
 * Module catalogue mirrored from MTK-UI's `app/panel/nav.ts` — one entry per
 * sidebar row, carrying the same role gate and the sub-pages shown as a hint.
 */
data class PanelModule(
    val key: String,
    val label: String,
    val hint: String,
    val roles: List<String>,
)

val panelModules: List<PanelModule> = listOf(
    PanelModule(
        key = "buildings",
        label = "Binalar",
        hint = "Binalar · Mənzillər · Qarajlar · Sahiblər",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    PanelModule(
        key = "inventory",
        label = "İnventar",
        hint = "Anbar · Materiallar · Daxilolmalar · Çıxarışlar",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    PanelModule(
        key = "operations",
        label = "Əməliyyatlar",
        hint = "Müqavilələr · Tədarükçülər · Müştərilər · Borclar",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT),
    ),
    PanelModule(
        key = "contracts",
        label = "Müqavilələr",
        hint = "Müqavilələr · Tədarükçülər",
        roles = listOf(Roles.OWNER),
    ),
    PanelModule(
        key = "rates",
        label = "Tariflər",
        hint = "Kommunal xidmət tarifləri",
        roles = listOf(Roles.OWNER),
    ),
    PanelModule(
        key = "transactions",
        label = "Tranzaksiyalar",
        hint = "Mədaxil · Məxaric · Kassa jurnalı",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT, Roles.OWNER),
    ),
    PanelModule(
        key = "reports",
        label = "Hesabatlar",
        hint = "Ödəniş qrafiki · Ümumi hesabat",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.ACCOUNTANT, Roles.OWNER),
    ),
    PanelModule(
        key = "appeals",
        label = "Müraciətlər",
        hint = "Sakin sorğuları · İcrada olanlar",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER, Roles.OWNER),
    ),
    PanelModule(
        key = "hr",
        label = "HR",
        hint = "İşçilər · Ərizələr · Əmrlər · Tabel · Təqvim",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    PanelModule(
        key = "identity",
        label = "İdentifikasiya",
        hint = "İstifadəçilər · Rollar · Qruplar",
        roles = listOf(Roles.ADMIN),
    ),
    PanelModule(
        key = "news",
        label = "Xəbərlər",
        hint = "Elanlar · Bildiriş göndər",
        roles = listOf(Roles.ADMIN, Roles.BUILDING_MANAGER),
    ),
    PanelModule(
        key = "profile",
        label = "Mənim profilim",
        hint = "Mənzil/qaraj və maliyyə məlumatlarınız",
        roles = listOf(Roles.OWNER),
    ),
    PanelModule(
        key = "admin",
        label = "İdarəetmə",
        hint = "Sistem idarəetməsi · Audit qeydləri",
        roles = listOf(Roles.ADMIN),
    ),
)

/** Same rule as the web panel: a module without roles is visible to everyone. */
fun modulesForRoles(roles: List<String>): List<PanelModule> =
    panelModules.filter { module ->
        module.roles.isEmpty() || module.roles.any { it in roles }
    }
