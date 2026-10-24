package az.mtk.app.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsError
import az.mtk.app.ui.screens.operations.OpsFeedback
import az.mtk.app.ui.screens.operations.OpsFormSheet
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsLoading
import az.mtk.app.ui.screens.operations.OpsPill
import az.mtk.app.ui.screens.operations.OpsPillRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTextField
import az.mtk.app.ui.screens.operations.OpsTopBar

/**
 * İdarəetmə modulu — yalnız admin üçün.
 *
 * Saytdakı `/panel/idareetme` (şirkət profili) və `/panel/audit` (audit
 * qeydləri) səhifələri mobil axında iki keçidli bir ekranda birləşdirilib.
 */
@Composable
fun ManagementScreen(
    onBack: () -> Unit,
    initialTab: ManagementTab = ManagementTab.COMPANY,
    canManage: Boolean = true,
    viewModel: ManagementViewModel = mtkViewModel { ManagementViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tabName by rememberSaveable(initialTab) { mutableStateOf(initialTab.name) }
    val tab = runCatching { ManagementTab.valueOf(tabName) }.getOrDefault(initialTab)

    LaunchedEffect(Unit) { viewModel.loadCompany() }
    LaunchedEffect(tab) {
        if (tab == ManagementTab.AUDIT) viewModel.ensureAudit()
    }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            Column(modifier = Modifier.background(OpsColors.Background)) {
                OpsTopBar(
                    title = "İdarəetmə",
                    subtitle = "Şirkət profili · Audit qeydləri",
                    onBack = onBack,
                    onRefresh = {
                        when (tab) {
                            ManagementTab.COMPANY -> viewModel.loadCompany(force = true)
                            ManagementTab.AUDIT -> viewModel.refreshAudit()
                        }
                    },
                )
                OpsPillRow(
                    pills = ManagementTab.entries.map { item ->
                        OpsPill(
                            label = item.label,
                            selected = item == tab,
                            onClick = { tabName = item.name },
                        )
                    },
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        },
    ) { padding ->
        when (tab) {
            ManagementTab.COMPANY -> CompanyProfileTab(
                state = state.company,
                canManage = canManage,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )

            ManagementTab.AUDIT -> AuditLogsTab(
                state = state.audit,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )
        }

        // Redaktə/yaratma forması aşağıdan yuxarı açılan paneldə göstərilir.
        if (tab == ManagementTab.COMPANY && canManage && state.company.editing) {
            CompanyEditorSheet(
                state = state.company,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun CompanyProfileTab(
    state: CompanyProfileState,
    canManage: Boolean,
    viewModel: ManagementViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompanyHeroCard()

        when {
            state.loading && !state.loaded -> OpsLoading()

            state.error != null -> OpsError(state.error) {
                viewModel.loadCompany(force = true)
            }

            else -> {
                if (state.notFound) CompanyNotFoundNotice(canManage)

                if (state.saved) {
                    OpsFeedback("Dəyişikliklər saxlanıldı", isError = false) {
                        viewModel.clearCompanyFeedback()
                    }
                }

                CompanyFormCard(
                    state = state,
                    canManage = canManage,
                    viewModel = viewModel,
                )

                CompanyPreviewCard(
                    name = state.name,
                    address = state.address,
                    director = state.director,
                )
            }
        }
    }
}

@Composable
private fun CompanyHeroCard() {
    OpsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OpsColors.Container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = null,
                    tint = OpsColors.Terracotta,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Şirkət profili",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Bu rekvizitlər əmək haqqı qəbzi, ödəniş qəbzi və əmr " +
                        "PDF-lərində, həmçinin saytın başlığında və altlığında göstərilir.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = OpsColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun CompanyNotFoundNotice(canManage: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = OpsColors.AmberSoft,
        border = BorderStroke(1.dp, OpsColors.Amber.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Şirkət profili hələ qurulmayıb",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Amber,
            )
            Text(
                text = if (canManage) {
                    "Aşağıdakı məlumatları doldurub profili yaradın."
                } else {
                    "Profili qurmaq üçün admin ilə əlaqə saxlayın."
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = OpsColors.Muted,
            )
        }
    }
}

@Composable
private fun CompanyFormCard(
    state: CompanyProfileState,
    canManage: Boolean,
    viewModel: ManagementViewModel,
) {
    OpsCard {
        if (!state.notFound) {
            OpsInfoRow("Şirkət adı", state.name.ifBlank { "—" })
            OpsInfoRow("Direktor", state.director.ifBlank { "—" })
            OpsInfoRow("Ünvan", state.address.ifBlank { "—" })
            OpsInfoRow("E-poçt", state.email.ifBlank { "—" })
            OpsInfoRow("Telefon", state.phone.ifBlank { "Əlavə olunacaq" })
            OpsInfoRow("İş saatları", state.workHours.ifBlank { "Əlavə olunacaq" })
        }

        if (canManage) {
            Spacer(Modifier.height(14.dp))
            OpsPrimaryButton(
                text = if (state.notFound) "Profili yarat" else "Redaktə et",
                onClick = viewModel::startCompanyEdit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Şirkət profili redaktəsi aşağıdan yuxarı açılan paneldə aparılır — səhifənin
 * özü yerində qalır və istifadəçi formanı bağlayanda eyni yerə qayıdır.
 */
@Composable
private fun CompanyEditorSheet(
    state: CompanyProfileState,
    viewModel: ManagementViewModel,
) {
    OpsFormSheet(
        title = if (state.notFound) "Şirkət profilini yarat" else "Şirkət profilini redaktə et",
        onDismiss = {
            if (!state.saving) viewModel.cancelCompanyEdit()
        },
    ) {
        OpsTextField(
            value = state.name,
            onValueChange = viewModel::updateCompanyName,
            label = "Şirkət adı",
            placeholder = "Məs: Vahid MTK",
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.director,
            onValueChange = viewModel::updateCompanyDirector,
            label = "Direktor",
            placeholder = "Ad Soyad Ata adı",
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.address,
            onValueChange = viewModel::updateCompanyAddress,
            label = "Ünvan",
            placeholder = "Küçə, bina",
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.email,
            onValueChange = viewModel::updateCompanyEmail,
            label = "E-poçt",
            keyboardType = KeyboardType.Email,
            placeholder = "info@mtk.az",
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.phone,
            onValueChange = viewModel::updateCompanyPhone,
            label = "Telefon",
            keyboardType = KeyboardType.Phone,
            placeholder = "Əlavə olunacaq",
        )
        Spacer(Modifier.height(10.dp))
        OpsTextField(
            value = state.workHours,
            onValueChange = viewModel::updateCompanyWorkHours,
            label = "İş saatları",
            placeholder = "Məs: 09:00–18:00",
        )

        state.formError?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = OpsColors.Error,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OpsSecondaryButton(
                text = "Ləğv et",
                onClick = viewModel::cancelCompanyEdit,
                modifier = Modifier.weight(1f),
                enabled = !state.saving,
            )
            OpsPrimaryButton(
                text = when {
                    state.saving -> "Saxlanılır…"
                    state.notFound -> "Profili yarat"
                    else -> "Yadda saxla"
                },
                onClick = viewModel::saveCompany,
                modifier = Modifier.weight(1f),
                enabled = !state.saving,
            )
        }
    }
}

@Composable
private fun CompanyPreviewCard(
    name: String,
    address: String,
    director: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = OpsColors.Surface,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SƏNƏDDƏ BELƏ GÖRÜNƏCƏK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OpsColors.MutedSoft,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = name.trim().ifBlank { "Şirkət adı" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = address.trim().ifBlank { "Ünvan" },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = OpsColors.Muted,
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = OpsColors.Border)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = "Direktor: ",
                    fontSize = 12.sp,
                    color = OpsColors.MutedSoft,
                )
                Text(
                    text = director.trim().ifBlank { "—" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Ink,
                )
            }
        }
    }
}
