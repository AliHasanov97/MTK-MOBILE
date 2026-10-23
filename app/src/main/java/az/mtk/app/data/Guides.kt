package az.mtk.app.data

data class GuideSection(
    val title: String,
    val steps: List<String>
)

data class Guide(
    val slug: String,
    val icon: String,
    val title: String,
    val summary: String,
    val panelHref: String,
    val roles: List<String>,
    val sections: List<GuideSection>,
    val tips: List<String>,
    val related: List<String>
)

val guides = listOf(
    Guide(
        slug = "binalar",
        icon = "⌂",
        title = "Binalar, mənzillər və sahiblər",
        summary = "Məhəllənin fondunu qeydiyyata alın: binalar, mənzillər, qarajlar və onların sahibləri.",
        panelHref = "/panel/binalar/bina-siyahisi",
        roles = listOf("İdarəçi", "Komandant"),
        sections = listOf(
            GuideSection(
                title = "Yeni bina əlavə etmək",
                steps = listOf(
                    "Panelin yan menyusundan \"Binalar\" bölməsini açın, \"Binalar\" tabında olun.",
                    "\"+ Yeni bina\" düyməsini basın.",
                    "Bina məlumatlarını doldurub yaradın.",
                    "Yaranmış binanı siyahıdan seçib mənzillərini \"Mənzillər\" tabından idarə edin."
                )
            ),
            GuideSection(
                title = "Sahib əlavə etmək",
                steps = listOf(
                    "\"Sahiblər\" tabına keçin.",
                    "\"+ Yeni sahib\" düyməsini basın və məlumatları doldurun.",
                    "Sahibin səhifəsindən onun mənzillərini, qarajlarını və borcunu izləyin."
                )
            ),
            GuideSection(
                title = "Qaraj əlavə etmək",
                steps = listOf(
                    "\"Qarajlar\" tabına keçin.",
                    "\"+ Yeni qaraj\" düyməsini basın.",
                    "Qarajı sahibə bağlayın."
                )
            )
        ),
        tips = listOf(
            "Əvvəlcə bina, sonra mənzil, sonra sahib əlavə edin — hesablar bu ardıcıllıqla qurulur.",
            "Sahiblik tarixçəsi saxlanılır: mənzil əl dəyişəndə köhnə qeyd silinmir."
        ),
        related = listOf("muqavileler", "borclar")
    ),
    Guide(
        slug = "inventar",
        icon = "▦",
        title = "İnventar və anbar",
        summary = "Material və avadanlıqların uçotu: anbar, materiallar, daxilolmalar və çıxarışlar.",
        panelHref = "/panel/inventar/anbar",
        roles = listOf("İdarəçi", "Komandant"),
        sections = listOf(
            GuideSection(
                title = "Material əlavə etmək",
                steps = listOf(
                    "\"İnventar\" bölməsində \"Materiallar\" tabını açın.",
                    "\"+ Yeni material\" düyməsini basın.",
                    "Ad və ölçü vahidini daxil edib yaradın."
                )
            ),
            GuideSection(
                title = "Anbardan çıxarış",
                steps = listOf(
                    "\"Çıxarışlar\" tabını açın.",
                    "\"+ Yeni çıxarış\" düyməsini basın.",
                    "Material və miqdarı seçib təsdiq edin."
                )
            ),
            GuideSection(
                title = "Daxilolma",
                steps = listOf(
                    "\"Daxilolmalar\" tabında alınmış materialların qəbulunu izləyin.",
                    "Daxilolmalar maliyyə bölməsindəki \"Salınmalar\" ilə əlaqəlidir."
                )
            )
        ),
        tips = listOf("Anbar qalığı daxilolma və çıxarışlardan avtomatik hesablanır."),
        related = listOf("maliyye")
    ),
    Guide(
        slug = "muqavileler",
        icon = "₼",
        title = "Müqavilələr, müştərilər və tədarükçülər",
        summary = "Xidmət müqavilələrini və tərəfdaşları idarə edin.",
        panelHref = "/panel/muqavileler",
        roles = listOf("İdarəçi", "Komandant", "Mühasib"),
        sections = listOf(
            GuideSection(
                title = "Müqavilə yaratmaq",
                steps = listOf(
                    "\"Əməliyyatlar\" bölməsində \"Müqavilələr\" tabını açın.",
                    "\"+ Yeni müqavilə\" düyməsini basın.",
                    "Müştərini seçin, xidmətləri və dövrü daxil edin.",
                    "Müqavilənin səhifəsində imzalanmış sənədləri əlavə edin."
                )
            ),
            GuideSection(
                title = "Müştəri və tədarükçü",
                steps = listOf(
                    "\"Müştərilər\" tabında \"+ Yeni müştəri\" ilə müştəri əlavə edin.",
                    "\"Tədarükçülər\" tabında \"+ Yeni tədarükçü\" ilə tədarükçü əlavə edin.",
                    "Hər birinin səhifəsində müqavilə və hesab tarixçəsi görünür."
                )
            )
        ),
        tips = listOf("Mənzil sahibləri öz müqavilələrini və tədarükçüləri \"Müqavilələr\" bölməsində görə bilərlər."),
        related = listOf("borclar", "maliyye")
    ),
    Guide(
        slug = "borclar",
        icon = "◎",
        title = "Hesablar və borclar",
        summary = "Aylıq hesabların yaranması, borcun izlənməsi və ödənişlərin bölüşdürülməsi.",
        panelHref = "/panel/borclar",
        roles = listOf("İdarəçi", "Komandant", "Mühasib"),
        sections = listOf(
            GuideSection(
                title = "Aylıq hesabları yaratmaq",
                steps = listOf(
                    "\"Borclar\" tabını açın.",
                    "Hesablaşma dövrünü seçin.",
                    "\"Hesabla\" düyməsini basın — hesablar tariflər əsasında yaranır.",
                    "Nəticəni siyahıda yoxlayın."
                )
            ),
            GuideSection(
                title = "Borca baxmaq",
                steps = listOf(
                    "Hər hesab ayrıca kartda göstərilir: məbləğ, ödənilən hissə, qalıq.",
                    "Sahibə görə filtrləyin və ya axtarın."
                )
            )
        ),
        tips = listOf("Qismən və avans ödənişlər nəzərə alınır; qalıq borc hər hesab üzrə ayrıca göstərilir."),
        related = listOf("maliyye", "tranzaksiyalar")
    ),
    Guide(
        slug = "maliyye",
        icon = "₼",
        title = "Maliyyə əməliyyatları",
        summary = "Ödənişlər, xərclər, əlavə gəlirlər, salınmalar, əmək haqqı və tariflər.",
        panelHref = "/panel/maliyye-emeliyyatlari",
        roles = listOf("İdarəçi", "Komandant", "Mühasib"),
        sections = listOf(
            GuideSection(
                title = "Ödəniş qeydə almaq",
                steps = listOf(
                    "\"Maliyyə əməliyyatları\" səhifəsində \"Ödənişlər\" bölməsini açın.",
                    "Sahibi və hesabı seçin, məbləği daxil edin.",
                    "Yadda saxlayın — ödəniş tranzaksiya kitabına düşür."
                )
            ),
            GuideSection(
                title = "Xərc və əlavə gəlir",
                steps = listOf(
                    "\"Xərclər\" bölməsində xərci qeyd edin, lazım olsa qəbz əlavə edin.",
                    "\"Əlavə gəlirlər\" bölməsində digər gəlirləri qeyd edin."
                )
            ),
            GuideSection(
                title = "Tariflər",
                steps = listOf(
                    "\"+ Yeni tarif\" ilə xidmət tarifini yaradın.",
                    "Tarif dəyişəndə növbəti hesablama yeni tarifə əsasən aparılır."
                )
            )
        ),
        tips = listOf("Hər əməliyyat izlənə bilən qeyd kimi saxlanılır; səhv varsa, silmək əvəzinə düzəliş əməliyyatı edin."),
        related = listOf("tranzaksiyalar", "hesabatlar", "hr")
    ),
    Guide(
        slug = "tranzaksiyalar",
        icon = "⇄",
        title = "Tranzaksiyalar",
        summary = "Bütün gəlir və xərclərin ümumi mühasibat kitabı.",
        panelHref = "/panel/tranzaksiyalar",
        roles = listOf("İdarəçi", "Komandant", "Mühasib", "Mənzil sahibi"),
        sections = listOf(
            GuideSection(
                title = "Tranzaksiyalara baxmaq",
                steps = listOf(
                    "Yan menyudan \"Tranzaksiyalar\" bölməsini açın.",
                    "Tarix, növ və ya məbləğə görə sütun filtrlərindən istifadə edin.",
                    "Sətrə basıb əlavə edilmiş sənəd və qəbzlərə baxın."
                )
            )
        ),
        tips = listOf("Mənzil sahibi yalnız özünə aid tranzaksiyaları görür."),
        related = listOf("maliyye", "hesabatlar")
    ),
    Guide(
        slug = "hesabatlar",
        icon = "▩",
        title = "Hesabatlar",
        summary = "Ödəniş qrafiki və ümumi maliyyə hesabatı.",
        panelHref = "/panel/hesabatlar",
        roles = listOf("İdarəçi", "Komandant", "Mühasib", "Mənzil sahibi"),
        sections = listOf(
            GuideSection(
                title = "Ödəniş qrafiki",
                steps = listOf(
                    "\"Hesabatlar\" bölməsində \"Ödəniş qrafiki\" tabını açın.",
                    "İli seçin; hər mənzil üçün aylar üzrə ödəniş vəziyyəti rənglə göstərilir.",
                    "Xanaya basıb ətraflı məlumata baxın."
                )
            ),
            GuideSection(
                title = "Ümumi hesabat",
                steps = listOf(
                    "\"Ümumi hesabat\" tabını açın.",
                    "Dövr seçin, gəlir-xərc və balansa baxın.",
                    "Lazım olsa Excel-ə ixrac edin."
                )
            )
        ),
        tips = listOf("Mənzil sahibi öz hesabatını görür."),
        related = listOf("tranzaksiyalar", "borclar")
    ),
    Guide(
        slug = "muraciyetler",
        icon = "✳",
        title = "Müraciətlər",
        summary = "Sakin müraciətlərinin qeydiyyatı və izlənməsi.",
        panelHref = "/panel/muracietler",
        roles = listOf("İdarəçi", "Komandant", "Mənzil sahibi"),
        sections = listOf(
            GuideSection(
                title = "Sakin üçün: müraciət göndərmək",
                steps = listOf(
                    "Yan menyudan \"Müraciətlər\" bölməsini açın.",
                    "Yeni müraciət yaradın, mövzu və təsvirini yazın.",
                    "Statusunu həmin səhifədən izləyin."
                )
            ),
            GuideSection(
                title = "İdarə üçün: müraciətə baxmaq",
                steps = listOf(
                    "Müraciətlər siyahısını açın.",
                    "Müraciəti seçib statusunu yeniləyin.",
                    "Məsul şəxsi təyin edin."
                )
            )
        ),
        tips = listOf("Problemi dəqiq təsvir edin: bina, mənzil və nə baş verdiyi."),
        related = listOf("binalar")
    ),
    Guide(
        slug = "hr",
        icon = "◈",
        title = "HR: işçilər, ərizələr, əmrlər, tabel",
        summary = "İşçi heyətinin idarəsi, kadr sənədləri və iş vaxtı uçotu.",
        panelHref = "/panel/hr/isciler",
        roles = listOf("İdarəçi", "Komandant"),
        sections = listOf(
            GuideSection(
                title = "İşçi əlavə etmək",
                steps = listOf(
                    "\"HR\" bölməsində \"İşçilər\" tabını açın.",
                    "\"+ Yeni işçi\" düyməsini basın.",
                    "Şəxsi, əmək və maaş məlumatlarını doldurun.",
                    "İşçinin səhifəsində sənədlər və ödənişlər tabları var."
                )
            ),
            GuideSection(
                title = "Tabel və təqvim",
                steps = listOf(
                    "\"Tabel\" tabında işçilərin davamiyyətini qeyd edin.",
                    "\"Təqvim\" tabında istehsalat təqvimini yoxlayın."
                )
            )
        ),
        tips = listOf("Əmək haqqı və məzuniyyət ödənişləri \"Maliyyə əməliyyatları\" bölməsindədir."),
        related = listOf("maliyye")
    ),
    Guide(
        slug = "profil",
        icon = "☺",
        title = "Mənim profilim (sakinlər üçün)",
        summary = "Mənzil sahibi üçün şəxsi bölmə: borc, tariflər, hesabat və müraciətlər.",
        panelHref = "/panel/profil",
        roles = listOf("Mənzil sahibi"),
        sections = listOf(
            GuideSection(
                title = "İlk giriş",
                steps = listOf(
                    "Saytda \"Daxil ol\" düyməsini basın və hesabınızla daxil olun.",
                    "\"Ümumi baxış\" səhifəsində cari vəziyyətinizə baxın.",
                    "\"Mənim profilim\" bölməsində məlumatlarınızı və mənzil/qaraj siyahınızı yoxlayın."
                )
            ),
            GuideSection(
                title = "Borc və ödənişlər",
                steps = listOf(
                    "Borcunuzu ümumi baxışda görürsünüz.",
                    "\"Tranzaksiyalar\" bölməsində ödəniş tarixçənizə baxın.",
                    "\"Hesabatlar\" bölməsində şəxsi hesabatınıza baxın."
                )
            ),
            GuideSection(
                title = "Müraciət",
                steps = listOf(
                    "\"Müraciətlər\" bölməsində yeni müraciət göndərin və statusunu izləyin."
                )
            )
        ),
        tips = listOf("Giriş hesabınız yoxdursa, kooperativin idarəsinə müraciət edin."),
        related = listOf("muraciyetler", "borclar")
    )
)

fun getGuide(slug: String): Guide? {
    return guides.find { it.slug == slug }
}
