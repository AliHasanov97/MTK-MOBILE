# MTK-UI → Android APK: funksional paritet planı

Məqsəd: `MTK-UI` (Next.js) panelində olan **bütün** funksionallığı native Android tətbiqinə
(`az.mtk.app`, Kotlin + Jetpack Compose) tətbiq etmək. Bu sənəd araşdırma nəticəsini,
fərq analizini və mərhələli icra planını əks etdirir.

- UI mənbəyi: `MTK-UI/app/panel/nav.ts` (modul siyahısı), `MTK-UI/app/panel/**` (səhifələr),
  `MTK-UI/app/lib/api/**` (~20 API modulu, ~200+ funksiya), `MTK-UI/app/lib/auth/**` (Keycloak + rollar).
- APK mənbəsi: `app/src/main/java/az/mtk/app/**` — hazırda **read-only MVP** (8 ekran).

---

## 1. Mövcud vəziyyət

### 1.1 APK-da hazırda olan (oxu rejimi)

| Ekran | Endpoint-lər |
|---|---|
| Login (Keycloak PKCE, token refresh) | `auth.vahid.az` |
| Ümumi baxış (statistika + rol kartları) | bir neçə `search` |
| Binalar + Bina detayı | `api/buildings/buildings`, `.../apartments/building/{id}` |
| Mənzillər + Mənzil detayı (sahib, mülkiyyət tarixçəsi) | `.../apartments/search`, `.../ownership-history` |
| Sahiblər + Sahib detayı | `.../owners/search`, `.../owners/{id}` |
| Qarajlar + Qaraj detayı | `.../garages/search`, `.../garages/{id}` |
| Borclar (ödənilib/ödənilməyib filtri) | `api/payments/charges/search` |
| HR (İşçilər + Ərizələr tabları, işçi detayı) | `api/hr/employees/search`, `api/hr/applications/search` |
| Profil / Çıxış (MoreScreen) | — |

Alt struktur hazır: `Pager` (debounced server-side səhifələmə), `SearchField`, `Pill`, `MtkCard`,
`Modal`-a bənzər komponent yoxdur, `AuthInterceptor`/`TokenAuthenticator` (401-də refresh),
`Envelope<T>` cavab şablonu, rol əsaslı nav (`NAV_MODULES`), rəng palitrası MTK-UI ilə eyni.

### 1.2 UI-da olan modullar (`app/panel/nav.ts`)

| # | Modul | Rollar | Tab-lar |
|---|---|---|---|
| 1 | Ümumi baxış | hamısı | — |
| 2 | Binalar | admin, building-manager | Binalar, Mənzillər, Qarajlar, Sahiblər |
| 3 | İnventar | admin, building-manager | Anbar, Materiallar, Daxilolmalar, Çıxarışlar |
| 4 | Əməliyyatlar | admin, building-manager, accountant | Müqavilələr, Tədarükçülər, Müştərilər, Borclar, Maliyyə əməliyyatları |
| 5 | Müqavilələr (sahib) | owner | Müqavilələr, Tədarükçülər |
| 6 | Tariflər | owner | — |
| 7 | Tranzaksiyalar | admin, bm, accountant, owner | — |
| 8 | Hesabatlar | admin, bm, accountant, owner | Ödəniş qrafiki, Ümumi hesabat |
| 9 | Müraciətlər | admin, building-manager, owner | — |
| 10 | HR | admin, building-manager | İşçilər, Ərizələr, Əmrlər, Tabel, Təqvim, Lüğətlər |
| 11 | İdentifikasiya | admin | İstifadəçilər, Rollar, Qruplar |
| 12 | Xəbərlər | admin, building-manager | — |
| 13 | Mənim profilim | owner | — (mənzil/qaraj/haqqlar/ödənişlər tabları) |
| 14 | İdarəetmə | admin | Sistem idarəetməsi, Müraciətlər, Audit qeydləri |

Əlavə sub-səhifələr: `maliyye-emeliyyatlari/{xercler,odenisler,elave-gelirler,tariflar,emek-haqqi,vacation-pays,salinmalar}`,
`hesabatlar/umumi`, `binalar/{bina-siyahisi,qarajlar,sahibler,menzil/[id],qaraj/[id]}`,
`musteriler/[id]`, `tedarukculer/[id]`, `muqavileler/[id]`, `muracietler/[id]`,
`inventar/materiallar/[id]`, `hr/isciler/[id]`, `audit`, `profil`.

Landing/ictimai səhifələr (`/`, `/haqqimizda`, `/elaqe`, `/faq`, `/telimat`, `/xeberler`,
`/mexfilik`, `/sistem`) APK-ya aid deyil — onlar marketinq saytıdır; **planın xaricində**.

### 1.3 API modulları (UI `app/lib/api/`)

`buildings`, `owners`, `garages`, `payments` (charges/payments/transactions/rates/company/
payroll/salary/vacation/reports — ən böyük modul), `contracts`, `vendors`, `customers`,
`customerCharges`, `vendorCharges`, `purchases`, `inventory`, `hr`, `appeals`, `identity`,
`articles`, `auditLogs`, `fileAttachments`, `company`.

APK-ın `MtkService`-ində hazırda cəmi **14 endpoint** var (yuxarıdakı 1.1 cədvəli).

---

## 2. Fərq analizi (gap)

### 2.1 Tamamilə yoxdur (APK-da heç bir ekran/endpoint yoxdur)

| Modul | UI funksionalı | API modulu |
|---|---|---|
| İnventar | Anbar (stock, low-stock), Materiallar (CRUD + detal + tarixçə), Daxilolmalar (purchases: yaratma/qəbul/ləğv/PDF), Çıxarışlar (stock transactions, issue) | `inventory.ts`, `purchases-client.ts` |
| Əməliyyatlar → Müqavilələr | siyahı, detal, xidmət CRUD, activate/suspend/terminate, PDF export | `contracts.ts` |
| Əməliyyatlar → Tədarükçülər | CRUD + status + balance + vendor charges | `vendors.ts`, `vendorCharges.ts` |
| Əməliyyatlar → Müştərilər | CRUD + balans + charge/payment yaratma | `customers.ts`, `customerCharges.ts` |
| Maliyyə əməliyyatları | Xərclər, Ödənişlər, Əlavə gəlirlər, Emek haqqı (payroll run: calculate/approve/pay/PDF), Məzuniyyət haqları, Silinmələr (salary payments) | `payments.ts` (böyük hissə) |
| Tariflər | cari tariflər, yaratma/redaktə | `payments.ts` (rates) |
| Tranzaksiyalar | xülasə + siyahı + yaratma | `payments.ts` (transactions) |
| Hesabatlar | Ödəniş qrafiki (illik), Ümumi hesabat, illik PDF/Excel, aylıq PDF export | `payments.ts` (reports) |
| Müraciətlər | yaratma, status, assign, şərh, kateqoriya/prioritet | `appeals.ts` |
| HR → Əmrlər | orders CRUD + PDF export | `hr.ts` |
| HR → Tabel (timesheet) | aylıq tabel + export | `hr.ts` |
| HR → Təqvim | calendar days CRUD + bulk | `hr.ts` |
| HR → Lüğətlər | Vəzifələr, Təhsil müəssisələri, Əmək məcəlləsi halları (CRUD) | `hr.ts` |
| İdentifikasiya | İstifadəçilər (register/update/delete, rollar), Rollar CRUD, Qruplar (CRUD + rol üzvlər) | `identity.ts` |
| Xəbərlər | articles CRUD + publish + fayl yükləmə/silmə | `articles.ts` |
| İdarəetmə | sistem/məlumat (company), audit qeydləri (modul üzrə axtarış) | `company.ts`, `auditLogs.ts`, `payments.ts`, `buildings.ts` |
| Profil (owner) | sahibə bağlı mənzil/qaraj/haqqlar/ödənişlər tabları, balans, `changeMyInfo` | `owners.ts`, `payments.ts`, `identity.ts` |

### 2.2 APK-da var, amma tam deyil (oxu → tam funksional)

| Sahə | Hazırda | UI-da əlavə |
|---|---|---|
| Binalar | yalnız siyahı + detal | bina yaratma, mənzil tipi yaratma, mənzil yaratma, sahib təyini/transferi |
| Qarajlar | yalnız siyahı + detal | CRUD, sahib təyini/çıxarılması, silmə |
| Sahiblər | yalnız siyahı + detal | passive sahib yaratma, sahibi istifadəçi ilə əlaqələndirmə |
| Borclar | yalnız siyahı + 2 filtr | dövr üzrə charge generasiyası, charge yaratma/ləğv, payment yaratma, allocation detalle, balanslar (owner/property/company), receipt export |
| HR İşçilər | yalnız siyahı + detal | işçi CRUD, iş/tehsil tarixçəsi, cədvəl (work schedule), məzuniyyət balansı, fayl əlavələri, PDF |
| HR Ərizələr | yalnız siyahı | yaratma, silmə, əmrə konversiya, status |
| Profil | yalnız kart + çıxış | owner-scoped tablar, balans, məlumat dəyişmə (`profile/change-info`) |
| Ümumi baxış | 5 say | UI-dakı göstəricilərlə genişləndirmə |

### 2.3 Platforma fərqləri (APK-da həll olunmalı ümumi məsələlər)

1. **Yazma əməliyyatları** — `MtkService`-də hazırda yalnız GET + 2 `search` POST var;
   `POST/PUT/PATCH/DELETE` endpoint-ləri, `Envelope` xəta mesajlarının UI kimi göstərilməsi.
2. **Fayl əməliyyatları** — UI-da `apiFetchFile` (PDF/Excel endirmə) və `apiUploadFile`
   (multipart) var; APK-da **yoxdur**. Həll: `DownloadManager` + `ContentResolver`/`MultipartBody`,
   PDF-i `FileProvider` + xarici viewer ilə açmaq.
3. **Filtr/sıralama** — UI-da `ColumnFilter` (sütun üzrə filtr + sort) və `MobileFilterBar` var;
   APK-da yalnız `SearchField` + 2-3 hazır chip. Həll: modul-tri-paper `FilterBar`/`SortSheet`.
4. **Formlar/modallar** — UI-da `Modal`, `SearchableSelect`, `DateInput`, `ConfirmDialog`,
   `BBCodeEditor` var. APK-da `Modal`/`ConfirmDialog`/`DateInput`/`SearchableSelect` komponentləri
   ümumi `ui/components`-ə əlavə olunmalı (hər ekran üçün ayrıca yazılmamalıdır).
5. **Rol qorunması** — UI-da hər səhifədə `RequireRole`; APK-da yalnız nav səviyyəsi var.
   Hər detail/write əməliyyatında da rol yoxlaması (403 dostu mesaj) lazımdır.
6. **Serializasiya** — UI `camelCase` + öz tipləri; APK Gson DTO. Bəzi endpoint-lər
   `Envelope<T>`, bəzləri birbaşa array qaytarır (UI-dakı kimi əks etdirmək lazımdır).

---

## 3. Arxitektura hazırlığı (Faz 0 — hamı üçün ortaq)

Yeni modullar yazmazdan əvvəl ümumi infrastruktur:

1. **API qatını bölmək**: tək `MtkService` böyüyəcək → `data/api/service/` altinda
   `BuildingsService`, `PaymentsService`, `HrService`, `InventoryService`, `IdentityService`,
   `AppealsService`, `CommunicationsService` (retrofit interfeysləri) + `MtkRepository`-ni
   modul-repository-lərə ayırmаq (yoxsa fayl idarəolunmaz olacaq).
2. **Yazma əməliyyatları üçün dəstək**: `Envelope<Unit>`/`MessageOnly` DTO, 403/422 xəta
   mesajlarının `toUserMessage()` ilə çevrilməsi (artıq `util/Format.kt`-də var, genişləndirmək).
3. **Yeni orta komponentlər** (`ui/components/`):
   - `MtkModal` (bottom-sheet/form modal, UI `Modal.tsx` ekvivalenti)
   - `ConfirmDialog` (UI `ConfirmDialog.tsx`)
   - `DateInput` (təqvim picker, UI `DateInput.tsx`)
   - `SearchableSelect` (UI `SearchableSelect.tsx` — API axtarışı ilə dropdown)
   - `FilterBar` + `SortSheet` (UI `ColumnFilter`/`MobileFilterBar` ekvivalenti)
   - `FileRow` (endirmə/yükləmə/silmə), `EmptyState` artıq var
4. **Fayl endirmə/yükləmə servisi**: `FileRepository` — `DownloadManager` (arxa fonda PDF/Excel),
   `MultipartBody` (upload), `FileProvider` ilə açma.
5. **Nav genişləndirmə**: `AppNav.kt`-dəki `NAV_MODULES` siyahısı UI `nav.ts` ilə **eyni sıra,
   eyni ad, eyni rol, eyni tab sırası** olacaq şəkildə yenilənəcək; hər modul üçün route
   + tab route-ları əlavə olunacaq.
6. **Test infrastrukturu**: hazırda test yoxdur. Ən azı:
   - DTO/Gson deserialize testləri (nümunə JSON ilə — UI `lib/api` tipləri ilə uyğunluq)
   - Repository filter/paging testləri (mock `MtkService`)
   - Hər faz sonunda `./gradlew :app:assembleDebug` + emulator smoke.

Qəbul meyarı (Faz 0): mövcud 8 ekran dəyişmədən işləyir, yeni komponentlər sample ekranda
yoxlanılır, build keçir.

---

## 4. Mərhələli icra planı

Hər faz: (a) API/DTO, (b) ekran(lar), (c) nav, (d) rollar, (e) yoxlama.
Prioritet sırası biznes dəyərinə görədir.

### Faz 1 — Binalar modulunu tam CRUD-a çatdırmaq (nümunə model kimi)
- Endpoint: `POST api/buildings/buildings`, `POST .../apartment-types`,
  `POST api/buildings/apartments`, `POST .../assign-owner`, `POST .../transfer-ownership`.
- Ekran: Binalar siyahısına "+ bina yaratma" modalı; bina detayında mənzil əlavəsi;
  mənzil detayında sahib təyini/transferi; qaraj CRUD (`garages` PUT/DELETE/assign/remove);
  sahib yaratma (`owners/passive`, `link-user`).
- Rollar: admin, building-manager.
- Yoxlama: yaratma → siyahıda görünür; 403 (accountant) dostu xəta göstərir.

### Faz 2 — Borclar və ödənişlər (maliyyənin təməli)
- Endpoint: `generateChargesForPeriod`, `createCharge`, `cancelCharge`, `createPayment`,
  `getChargeAllocations`, `getPaymentAllocations`, `getOwnerBalance`, `getPropertyBalance`,
  `getCompanyBalance`, `exportPaymentReceipt` (fayl endirmə ilk dəfə burada işə düşür).
- Ekran: Borclar ekranına "Dövr üçün charge yaratma" (GenerateChargesModal) və
  "Ödəniş qəbulu" (CreatePayment) modalı; charge/payment detal sheet (allocations);
  balans kartları.
- Rollar: admin, building-manager, accountant.
- Yoxlama: real API-də test charge/payment (test mühitində), receipt endirilir və açılır.

### Faz 3 — Əməliyyatlar modulu (Müqavilələr, Tədarükçülər, Müştərilər)
- `contracts.ts` tam CRUD + xidmətlər + status keçidləri + PDF export.
- `vendors.ts` / `customers.ts` CRUD + status + balans.
- `vendorCharges.ts` / `customerCharges.ts` (charge/payment/ləğv).
- Ekranlar: siyahı (Pager + FilterBar) + detal (tab-lı) + form modalları.
- Rollar: admin, building-manager, accountant (+ owner: müqavilələr/ tədarükçülər read).

### Faz 4 — Maliyyə əməliyyatları + Tariflər + Tranzaksiyalar
- Xərclər, Ödənişlər, Əlavə gəlirlər (əsas `createTransaction`/`createPayment` formaları).
- Tariflər (`getCurrentRates`, `createRate`, `updateRate`) — owner üçün də görünür.
- Tranzaksiyalar: `getTransactionsSummary` (kartlar) + `searchTransactions` (siyahı) + yaratma.
- Rollar: admin, bm, accountant (tariflər + owner).

### Faz 5 — Hesabatlar
- Ödəniş qrafiki (illik, hər mənzil/qaraj × ay — ödənilib/ödənilməyib) — UI `hesabatlar/OdenisQrafikiView`.
- Ümumi hesabat (`hesabatlar/umumi`).
- Export: `exportAnnualPaymentReport` (Excel), `exportMonthlyReportPdf`.
- Faz 0-dakı `FileRepository` + hədəf qovluq seçimi (SAF) burada tam test olunur.

### Faz 6 — Müraciətlər
- `appeals.ts`: create/search/detail/status/assign/comment + label helper-lər (Azerbaijan dilində).
- Ekran: siyahı (status filtrləri), detal (status dəyişmə, assign, şərh yazmaq), yaratma modalı.
- Rollar: admin, building-manager, owner (owner öz müraciətlərini görür — UI-dakı kimi).

### Faz 7 — HR genişləndirməsi
- Əmrlər (orders CRUD + PDF), Tabel (timesheet + export), Təqvim (calendardays CRUD + bulk),
  Lüğətlər (jobs/educationalinstitutions/laborcodecases CRUD).
- İşçi CRUD + detal tabları: iş tarixçəsi, təhsil tarixçəsi, work schedule, məzuniyyət balansı.
- Ərizələr: yaratma/silmə/əmrə konversiya; fayl əlavələri (upload/download).
- Rollar: admin, building-manager.

### Faz 8 — İnventar
- Anbar (stocks, low-stock), Materiallar (nomenclature CRUD + detal + purchase history),
  Daxilolmalar (purchases: yaratma/redaktə/qəbul/ləğv/PDF), Çıxarışlar (transactions + issue).
- Rollar: admin, building-manager.

### Faz 9 — İdentifikasiya (admin)
- İstifadəçilər: search, register, update, delete, rol təyinı/çıxarılması, qruplar.
- Rollar CRUD, Qruplar CRUD + üzv/həmkar əlavə etmə.
- Yalnız `admin` — APK-da nav-a yalnız admin üçün.

### Faz 10 — Xəbərlər
- Articles CRUD + publish + fayl yükləmə/silmə (`api/communications/articles`).
- BBCode yox, sadə mətn/redaktor (APK-da `BBCodeEditor` lazım deyil — düz mətn + önizləmə kifayət edir; qərar implementasiya zamanı).

### Faz 11 — Profil (owner) + İdarəetmə + Audit
- Profil (owner): `getOwnerByUserId` → tablar (mənzillər, qarajlar, haqqlar, ödənişlər),
  balans, `profile/change-info` (ad/telefon redaktəsi).
- İdarəetmə (admin): sistem/company məlumatları (`getCompany`/`upsertCompany`),
  audit qeydləri (modul seçimi + axtarış + filtr) — UI `auditLogs.ts`-dəki modul siyahısı ilə.
- APK-nın hazırda `MoreScreen`-dəki profil kartı bu modulla əvəz/genişlənəcək.

### Faz 12 — Sonlandırma
- Bütün ekranların rol matrisi ilə yoxlanması (hər rol → yalnız öz modulları).
- UX: pull-to-refresh, retry, loading skeleton-ları, `Sistem və Bağlantı` kartında versiya.
- (İstəyə bağlı, plana daxil deyil) offline keş, FCM push, dark mode — UI-da da yoxdur.

---

## 5. Həcm təxmini

| Qrup | Ekran/view | Endpoint (təxmini) |
|---|---|---|
| Hazır (MVP) | 8 | 14 |
| Faz 1–2 (Binalar CRUD + Borclar/Ödəniş) | +6 | +20 |
| Faz 3–5 (Əməliyyatlar, Maliyyə, Hesabatlar) | +14 | +55 |
| Faz 6–8 (Müraciətlər, HR, İnventar) | +14 | +50 |
| Faz 9–11 (Identifikasiya, Xəbərlər, Profil/İdarəetmə) | +10 | +45 |
| **Cəmi** | **~50** | **~185** |

---

## 6. Risklər

1. **API kontraktı** — APK DTO-ları UI tipləri ilə əl ilə sinxronlaşdırılır; backend
   `Envelope` vs birbaşa array fərqləri var. Hər faz üçün UI `lib/api/*.ts` mənbəsini
   mənbə kimi istifadə et və nümunə JSON ilə deserialize testi yaz.
2. **Rollar/icazə** — `RequireRole` UI-da səhifəni gizlədir; APK-da həm nav, həm hər write
   endpoint-in 403 mesajı düzgün göstərilməlidir (yoxsa istifadəçi "boş ekran" görür).
3. **Həcm** — ~50 ekran; böyük bir `AppNav.kt`/fayl olmaması üçün hər modul öz paketində
   (`ui/screens/<modul>/`) və öz repository-si ilə.
4. **Fayl qoruması** — Android 13+ `POST_NOTIFICATIONS`/scoped storage; `DownloadManager` +
   `FileProvider` ilə həll olunur, SAF ilə "haraya saxla" seçimi.
5. **Keycloak** — artıq işləyir (PKCE + refresh); yeni scope lazım deyil, amma bəzi
   endpoint-lər 403 qaytara bilər — Faz 9-dan əvvəl admin rollarını yoxla.

---

## 7. Yoxlama qaydası (hər faz üçün)

1. `./gradlew :app:assembleDebug` — build keçir.
2. Yeni endpoint-lər real backend-də token ilə curl/WebView yoxlanır (401/403/405 deyil, 200).
3. UI ilə vizual müqayisə: eyni ad, eyni tab sırası, eyni rol filtri (`nav.ts` ilə).
4. Ən azı 1 emulator smoke: siyahı yüklənir, axtarış işləyir, write əməliyyatı göründü,
   xəta halında UI mesajı çıxır (crash yoxdur).
5. DTO deserialize testi əlavə olunur.
