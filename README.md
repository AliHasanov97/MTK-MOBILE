# MTK Android (native)

MTK idarəetmə sisteminin **native Android** tətbiqi. UI dizaynı `MTK-UI` (Next.js) layihəsindən götürülüb,
lakin tətbiq web view deyil — Kotlin + Jetpack Compose ilə yazılıb və məlumatları birbaşa
`https://mtk-api.vahid.az/` REST endpoint-lərindən oxuyur.

```
app/src/main/java/az/mtk/app/
  AppConfig.kt              API / Keycloak ünvanları (buradan dəyişilir)
  AppGraph.kt               sadə əl ilə DI (AuthRepository, Retrofit, MtkRepository)
  data/auth/                Keycloak PKCE girişi, token saxlanması, JWT rolları
  data/api/                 Retrofit servisi, DTO-lar, səhifələmə, 401-də avtomatik refresh
  ui/theme/                 MTK rəng palitrası (yaşıl #345C4A, kağız #F7F8F4, laym #C9E3A1)
  ui/components/            kart, pill, axtarış, siyahı, pagination komponentləri
  ui/screens/               Login, Dashboard, Binalar, Mənzillər, Sahiblər, Qarajlar, Borclar, HR
  ui/navigation/AppNav.kt   alt tab-lar və detal ekranları
```

## Ekranlar (MVP)

| Bölmə | Nə edir | Endpoint |
|---|---|---|
| Giriş | Keycloak (Authorization Code + PKCE), sessiya avtomatik yenilənir | `auth.vahid.az` |
| Ümumi baxış | Bina / mənzil / sahib / qaraj sayları, rol əsaslı modul kartları | bir neçə `search` |
| Binalar | Bina siyahısı və bina daxilində mənzillər | `api/buildings/buildings`, `.../apartments/building/{id}` |
| Mənzillər | Axtarış, səhifələmə, mənzil kartı, sahibi, mülkiyyət tarixçəsi | `api/buildings/apartments/search` |
| Sahiblər | Axtarış, sahəb kartı, onun mənzil və qarajları | `api/buildings/owners/search`, `/owners/{id}` |
| Qarajlar | Növ üzrə filtr, sahib, qaraj kartı | `api/buildings/garages/search`, `/garages/{id}` |
| Borclar | Qalıq borc / ödənilib filtri, məbləğ və status | `api/payments/charges/search` |
| HR | İşçilər və ərizələr tabları, işçi kartı | `api/hr/employees/search`, `api/hr/applications/search` |

Hazırda bütün ekranlar **oxuma** rejimindədir (yaratma/redaktə/silmə əməliyyatları MVP-yə daxil deyil).
Fayl/PDF endirmə, push bildiriş və offline rejim də yoxdur.

## Vacib: Keycloak ayarı

Tətbiq öz *custom scheme* redirect URI-si ilə qayıdır. Keycloak-da `mtk-web` client-inə
(Admin → Clients → `mtk-web` → Settings) **dəqiq** bu dəyəri əlavə edin:

```
Valid redirect URIs:        az.mtk.app:/oauth2redirect
Valid post logout redirect: az.mtk.app:/oauth2redirect   (istəyə bağlı)
```

Diqqət: `az.mtk.app://oauth2redirect` (iki slash) **yox**, `az.mtk.app:/oauth2redirect` (bir slash).
Client public (PKCE) olaraq qalmalıdır; "Client authentication" açıq olmamalıdır.

Yoxlama: aşağıdakı sorğu `Invalid parameter: redirect_uri` qaytarmamalıdır (login səhifəsi açılmalıdır):

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://auth.vahid.az/realms/mtk/protocol/openid-connect/auth?client_id=mtk-web&redirect_uri=az.mtk.app%3A%2Foauth2redirect&response_type=code&scope=openid%20profile%20email&state=x&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256"
```

## Konfiqurasiya

Bütün ünvanlar bir yerdə — `app/src/main/java/az/mtk/app/AppConfig.kt`:

| Sahə | Dəyər |
|---|---|
| `ApiBaseUrl` | `https://mtk-api.vahid.az/` |
| `OidcIssuer` | `https://auth.vahid.az/realms/mtk` |
| `ClientId` | `mtk-web` |
| `RedirectUri` | `az.mtk.app:/oauth2redirect` |

`RedirectScheme` dəyişsəniz, `AndroidManifest.xml`-dəki intent filter-in `android:scheme`-i də
eyni olmalıdır.

## Build

```bash
# Android Studio JBR (JDK 21) ilə — JDK 25 AGP 9 üçün tövsiyə olunmur
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"

./gradlew :app:assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk (imzalı)
```

APK quraşdırma:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## İmzalama açarı

`keystore/mtk-release.jks` və `keystore.properties` **git-ə düşmür** (`.gitignore`).
Bu açarı itirməyin — yeni versiyalar yalnız eyni açarla imzalandıqda mövcud quraşdırmanın
üstünə yenilənə bilər. Açarı və parolu təhlükəsiz yerdə saxlayın.

## Yoxlanılıb

- `:app:assembleDebug` və `:app:assembleRelease` uğurlu; release APK `apksigner` ilə imzalı görünür.
- Emulyatorda (Medium Phone, API 37) tətbiq açılır, login ekranı düzgün render olunur, crash yoxdur.
- `az.mtk.app:/oauth2redirect` deep link-i tətbiq tərəfindən tutulur; səhv cavabda istifadəçiyə
  aydın xəta göstərilir (crash yoxdur).
- API endpoint-ləri yoxlanılıb: `api/buildings/buildings` → 401, `api/.../search` → 401/405
  (yəni yol düzdür, yalnız token tələb olunur).
- Keycloak: `mtk-web` client və `mtk` realm mövcuddur; redirect URI hələ əlavə edilmədiyi üçün
  `Invalid parameter: redirect_uri` qaytarır — yuxarıdaki ayarı etdikdən sonra giriş işləyəcək.

## Növbəti addımlar

1. Keycloak-da redirect URI-ni əlavə et, real hesabla girişi yoxla.
2. Read-only ekranlardan sonra yaratma/redaktə əməliyyatlarını əlavə et.
3. Bildirişlər (FCM), PDF/fayl endirmə, offline keş.
