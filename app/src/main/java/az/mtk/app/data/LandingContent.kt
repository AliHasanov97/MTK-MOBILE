package az.mtk.app.data

/**
 * Module definition matching MTK-UI structure
 */
data class Module(
    val icon: String,
    val title: String,
    val text: String,
    val points: List<String>
)

/**
 * All 8 modules from MTK-UI
 */
val modules = listOf(
    Module(
        icon = "⌂",
        title = "Binalar və mənzillər",
        text = "Məhəllənin bütün fondu bir yerdə.",
        points = listOf(
            "Binalar, mənzillər, qarajlar",
            "Sahiblər və sahiblik tarixçəsi",
            "İzometrik bina sxemi"
        )
    ),
    Module(
        icon = "₼",
        title = "Müqavilə və borclar",
        text = "Tarifə əsasən avtomatik hesablama.",
        points = listOf(
            "Müqavilələr, müştərilər, tədarükçülər",
            "Hesablar, avans və qismən ödənişlər",
            "Sahib üzrə borc izləmə"
        )
    ),
    Module(
        icon = "⇄",
        title = "Tranzaksiyalar",
        text = "Hər gəlir və xərcin izlənə bilən qeydi.",
        points = listOf(
            "Ümumi mühasibat kitabı",
            "Xərc və gəlir daxiletmə formaları",
            "Qəbz və sənəd əlavələri"
        )
    ),
    Module(
        icon = "▩",
        title = "Hesabatlar",
        text = "Maliyyə mənzərəsi bir baxışda.",
        points = listOf(
            "Ödəniş qrafiki",
            "Ümumi maliyyə hesabatı",
            "Sahib üçün şəxsi hesabat"
        )
    ),
    Module(
        icon = "▦",
        title = "İnventar və anbar",
        text = "Material və avadanlıq uçotu.",
        points = listOf(
            "Anbar və materiallar",
            "Daxilolmalar (qaimələr)",
            "Çıxarışlar və qalıq"
        )
    ),
    Module(
        icon = "◈",
        title = "HR və əmək haqqı",
        text = "İşçi heyətinin tam idarəsi.",
        points = listOf(
            "İşçilər, ərizələr, əmrlər",
            "Tabel və istehsalat təqvimi",
            "Əmək haqqı və məzuniyyət ödənişləri"
        )
    ),
    Module(
        icon = "✳",
        title = "Müraciətlər",
        text = "Sakinlərlə açıq əlaqə.",
        points = listOf(
            "Müraciətin qeydiyyatı",
            "Statusun izlənməsi",
            "Məsul şəxsə yönləndirmə"
        )
    ),
    Module(
        icon = "◐",
        title = "İcazələr və audit",
        text = "Hər kəs yalnız öz səlahiyyətində.",
        points = listOf(
            "İstifadəçilər, rollar, qruplar",
            "Rol əsaslı giriş",
            "Audit qeydləri — kim nə etdi"
        )
    )
)

/**
 * Statistics data
 */
data class Stat(
    val value: String,
    val label: String
)

val stats = listOf(
    Stat("8", "inteqrə modul"),
    Stat("5", "istifadəçi rolu"),
    Stat("1", "vahid məlumat bazası"),
    Stat("100%", "əməliyyat tarixçəsi")
)

/**
 * Process steps
 */
data class Step(
    val icon: String,
    val title: String,
    val text: String
)

val steps = listOf(
    Step(
        icon = "⌂",
        title = "Qeydiyyat",
        text = "Bina, mənzil, sahib və müqavilə məlumatları bir dəfə daxil edilir."
    ),
    Step(
        icon = "₼",
        title = "Hesablama",
        text = "Tariflər əsasında aylıq hesablar və borclar avtomatik yaranır."
    ),
    Step(
        icon = "⇄",
        title = "Ödəniş və xərc",
        text = "Daxilolmalar, xərclər və maaşlar tranzaksiya kitabına düşür."
    ),
    Step(
        icon = "▩",
        title = "Hesabat",
        text = "Eyni məlumat əsasında qrafik və ümumi hesabatlar hazırlanır."
    )
)

/**
 * User roles
 */
data class Role(
    val name: String,
    val text: String
)

val roles = listOf(
    Role(
        name = "İdarəçi",
        text = "Bütün modullar, istifadəçi və rol idarəsi, audit və sistem ayarları."
    ),
    Role(
        name = "Komandant",
        text = "Binalar, inventar, müqavilələr, müraciətlər və HR."
    ),
    Role(
        name = "Mühasib",
        text = "Müqavilələr, borclar, maliyyə əməliyyatları, tranzaksiyalar və hesabatlar."
    ),
    Role(
        name = "Mənzil sahibi",
        text = "Öz borcu, tarifləri, tranzaksiyaları, hesabatı, müraciətləri və profili."
    )
)

/**
 * Before/After comparison
 */
object Compare {
    val before = listOf(
        "Excel cədvəlləri və kağız qeydlər",
        "Borc və ödənişlər müxtəlif faylda",
        "Kim nəyi dəyişdi — bilinmir",
        "Sakin məlumat üçün idarəyə zəng edir",
        "Hesabat əl ilə, gec hazırlanır"
    )

    val after = listOf(
        "Vahid sistem, ortaq məlumat bazası",
        "Hesablar, borclar və ödənişlər bir yerdə",
        "Hər əməliyyatın audit qeydi var",
        "Sakin borcunu və tarixçəsini özü görür",
        "Hesabatlar real vaxtda hazırdır"
    )
}

/**
 * Resident features
 */
data class ResidentFeature(
    val icon: String,
    val title: String,
    val text: String
)

val residentFeatures = listOf(
    ResidentFeature(
        icon = "₼",
        title = "Borcunuzu görün",
        text = "Cari borc, hesablar və ödəniş tarixçəsi şəxsi panelinizdədir."
    ),
    ResidentFeature(
        icon = "▩",
        title = "Hesabat və tariflər",
        text = "Tarifləri və şəxsi hesabatınızı istədiyiniz vaxt yoxlayın."
    ),
    ResidentFeature(
        icon = "✳",
        title = "Müraciət göndərin",
        text = "Problemi qeyd edin, statusunu izləyin, idarəyə zəng etməyə ehtiyac yoxdur."
    ),
    ResidentFeature(
        icon = "☺",
        title = "Profil",
        text = "Əlaqə məlumatlarınızı və mənzil/qaraj siyahınızı bir yerdə saxlayın."
    )
)

/**
 * Security features
 */
data class SecurityFeature(
    val icon: String,
    val title: String,
    val text: String
)

val securityFeatures = listOf(
    SecurityFeature(
        icon = "◐",
        title = "Rol əsaslı giriş",
        text = "Hər istifadəçi yalnız öz səlahiyyətinə aid bölmələri görür."
    ),
    SecurityFeature(
        icon = "✓",
        title = "Mərkəzləşmiş autentifikasiya",
        text = "Giriş Keycloak vasitəsilə, ayrıca şifrə saxlanmadan həyata keçirilir."
    ),
    SecurityFeature(
        icon = "≡",
        title = "Audit qeydləri",
        text = "Kim, nə vaxt, nəyi dəyişdi — administrator hər şeyi izləyə bilir."
    ),
    SecurityFeature(
        icon = "⇄",
        title = "İzlənə bilən maliyyə",
        text = "Hər gəlir və xərc tranzaksiya kitabında sənədlə birlikdə saxlanılır."
    )
)

/**
 * About content
 */
object About {
    val text = listOf(
        "Bu sistem Mənzil-Tikinti Kooperativinin gündəlik işini sadələşdirmək üçün hazırlanıb: binalar, mənzillər, kommunal hesablar, maliyyə, inventar və işçi heyəti ayrı-ayrı cədvəllərdə yox, bir yerdə idarə olunur.",
        "Məqsədimiz idarəetməni şəffaf etməkdir: idarəçi tam mənzərəni, mühasib dəqiq rəqəmləri, sakin isə öz borcunu və müraciətinin gedişatını görür."
    )

    val values = listOf(
        "Şəffaflıq",
        "Dəqiqlik",
        "Sadəlik",
        "Hesabatlılıq"
    )
}

/**
 * FAQ items
 */
data class FAQ(
    val question: String,
    val answer: String
)

val faqs = listOf(
    FAQ(
        question = "Sistemə necə giriş əldə edə bilərəm?",
        answer = "Giriş hesabları kooperativin idarəsi tərəfindən yaradılır. Hesabınız yoxdursa, idarəyə müraciət edin; yaradıldıqdan sonra \"Daxil ol\" düyməsi ilə daxil olun."
    ),
    FAQ(
        question = "Mənzil sahibi olaraq nəyi görürəm?",
        answer = "Öz borcunuzu, tarifləri, tranzaksiyalarınızı, hesabatınızı, müraciətlərinizi və profilinizi. Digər sakinlərin məlumatları sizə görünmür."
    ),
    FAQ(
        question = "Borc necə hesablanır?",
        answer = "Müqavilə və tariflər əsasında aylıq hesablar avtomatik yaranır. Qismən və avans ödənişlər də nəzərə alınır, qalıq borc hər hesab üzrə göstərilir."
    ),
    FAQ(
        question = "Məlumatlarım təhlükəsizdir?",
        answer = "Giriş rola görə məhdudlaşdırılıb, autentifikasiya mərkəzləşmiş şəkildə aparılır və bütün dəyişikliklər audit qeydlərində saxlanılır."
    ),
    FAQ(
        question = "Müraciətimin nə vəziyyətdə olduğunu necə bilərəm?",
        answer = "\"Müraciətlər\" bölməsində hər müraciətin statusu göstərilir. Yeni müraciəti də oradan göndərə bilərsiniz."
    ),
    FAQ(
        question = "Hesabatları yükləmək olar?",
        answer = "Bəli, ödəniş qrafiki və ümumi hesabat panelin \"Hesabatlar\" bölməsindədir. Əmək haqqı qəbzləri isə PDF formatında çıxarıla bilir."
    ),
    FAQ(
        question = "Mobil telefondan istifadə etmək olar?",
        answer = "Bəli, sistem telefon və planşet ekranlarına uyğunlaşdırılıb; cədvəllər kiçik ekranda kart görünüşünə keçir."
    )
)
