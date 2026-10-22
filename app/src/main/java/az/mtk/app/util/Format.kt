package az.mtk.app.util

import az.mtk.app.data.api.WorkExperienceDto
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import retrofit2.HttpException

object Format {
    private val moneyFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    })
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val months = listOf(
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
        "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr",
    )

    fun money(value: Double?): String = value?.let { "${moneyFormat.format(it)} ₼" } ?: "—"

    fun number(value: Double?): String = value?.let { moneyFormat.format(it) } ?: "—"

    fun area(value: Double?): String = value?.let { "${moneyFormat.format(it)} m²" } ?: "—"

    fun date(iso: String?): String {
        val raw = iso?.takeIf { it.isNotBlank() } ?: return "—"
        return runCatching { OffsetDateTime.parse(raw).toLocalDate() }
            .recoverCatching { LocalDateTime.parse(raw).toLocalDate() }
            .recoverCatching { LocalDate.parse(raw.take(10)) }
            .getOrNull()
            ?.format(dateFormat)
            ?: raw.take(10)
    }

    fun dateTime(iso: String?): String {
        val raw = iso?.takeIf { it.isNotBlank() } ?: return "—"
        return runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }
            .recoverCatching { LocalDateTime.parse(raw) }
            .getOrNull()
            ?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            ?: date(raw)
    }

    /** "2026-03" -> "Mart 2026" */
    fun period(value: String?): String {
        val raw = value?.takeIf { it.isNotBlank() } ?: return "—"
        val parts = raw.split("-")
        if (parts.size < 2) return raw
        val month = parts[1].toIntOrNull()
        return if (month != null && month in 1..12) "${months[month - 1]} ${parts[0]}" else raw
    }

    fun garageType(value: String?): String = when (value) {
        "OpenParking" -> "Açıq dayanacaq"
        "CoveredGarage" -> "Örtülü qaraj"
        "Storage" -> "Anbar yeri"
        else -> value ?: "—"
    }

    fun chargeStatus(value: Int?): String = when (value) {
        0 -> "Ödənilməyib"
        1 -> "Qismən ödənilib"
        2 -> "Ödənilib"
        3 -> "Ləğv edilib"
        else -> "—"
    }

    fun applicationType(value: Int?): String = when (value) {
        1 -> "Vakansiyaya müraciət"
        2 -> "Vəzifə dəyişikliyi"
        3 -> "Məzuniyyət əvəzi"
        4 -> "Ödənişsiz məzuniyyət"
        5 -> "Əmək məzuniyyəti"
        6 -> "Təhsil məzuniyyəti"
        7 -> "İş rejimi dəyişikliyi"
        8 -> "Məzuniyyətdən geri çağırma"
        else -> "Müraciət"
    }

    fun employmentType(value: Int?): String = when (value) {
        1 -> "Tam ştat"
        2 -> "Yarım ştat"
        else -> "—"
    }

    fun gender(value: Int?): String = when (value) {
        1 -> "Kişi"
        2 -> "Qadın"
        else -> "—"
    }

    fun maritalStatus(value: Int?): String = when (value) {
        0 -> "Subay"
        1 -> "Evli"
        else -> "—"
    }

    fun isActiveLabel(value: Int?): String = if (value == 1) "Aktiv" else "Qeyri-aktiv"

    fun person(name: String?, surname: String?, fathersName: String?): String =
        listOfNotNull(surname, name, fathersName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "—" }

    fun initials(fullName: String?): String = fullName.orEmpty()
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    fun experience(value: WorkExperienceDto?): String {
        if (value == null) return "—"
        val parts = buildList {
            value.years?.takeIf { it > 0 }?.let { add("$it il") }
            value.months?.takeIf { it > 0 }?.let { add("$it ay") }
        }
        return parts.joinToString(" ").ifBlank { "—" }
    }

    fun yesNo(value: Boolean?): String = if (value == true) "Bəli" else "Xeyr"
}

/** Maps transport/API failures to text the user can act on. */
fun Throwable.toUserMessage(): String = when (this) {
    is az.mtk.app.data.api.ApiException -> message ?: "Server xətası"
    is HttpException -> when (code()) {
        401 -> "Giriş icazəsi bitdi. Yenidən daxil olun."
        403 -> "Bu məlumat üçün icazəniz yoxdur."
        404 -> "Məlumat tapılmadı."
        else -> "Server xətası (${code()})."
    }
    is IOException -> "Serverə qoşulmaq mümkün olmadı. İnternet bağlantısını yoxlayın."
    else -> message?.takeIf { it.isNotBlank() } ?: "Gözlənilməz xəta."
}
