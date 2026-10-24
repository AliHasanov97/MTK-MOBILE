package az.mtk.app.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.BiometricAvailability
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsInfoRow
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.ui.screens.operations.OpsTopBar

@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit,
) {
    val pinManager = remember { AppGraph.pinManager }
    val biometricHelper = remember { AppGraph.biometricHelper }

    var isPinEnabled by remember { mutableStateOf(pinManager.isPinSet()) }
    var showDisablePinDialog by remember { mutableStateOf(false) }

    val biometricAvailable = remember {
        biometricHelper.canUseBiometric() == BiometricAvailability.Available
    }

    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            containerColor = OpsColors.Surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "PIN-i deaktiv et?",
                    fontWeight = FontWeight.SemiBold,
                    color = OpsColors.Ink,
                )
            },
            text = {
                Text(
                    text = "PIN deaktiv edildikdə tətbiqə giriş üçün yalnız Keycloak hesabınız istifadə olunacaq.",
                    color = OpsColors.Muted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pinManager.clearPin()
                        AppGraph.lockManager.clear()
                        isPinEnabled = false
                        showDisablePinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpsColors.Error),
                ) {
                    Text("Deaktiv et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinDialog = false }) {
                    Text("Ləğv et", color = OpsColors.Muted)
                }
            },
        )
    }

    Scaffold(
        containerColor = OpsColors.Background,
        topBar = {
            OpsTopBar(
                title = "Təhlükəsizlik",
                subtitle = "PIN kod · Biometrik",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // PIN Section
            OpsCard {
                SecurityHeroRow(
                    icon = Icons.Default.Pin,
                    title = "PIN Kod",
                    subtitle = if (isPinEnabled) "4 rəqəmli PIN təyin edilib" else "PIN təyin edilməyib",
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isPinEnabled) {
                        OpsSecondaryButton(
                            text = "Dəyişdir",
                            onClick = onChangePin,
                            modifier = Modifier.weight(1f),
                        )
                        OpsSecondaryButton(
                            text = "Deaktiv et",
                            onClick = { showDisablePinDialog = true },
                            modifier = Modifier.weight(1f),
                            danger = true,
                        )
                    } else {
                        OpsPrimaryButton(
                            text = "PIN təyin et",
                            onClick = onSetupPin,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Biometric Section
            if (biometricAvailable) {
                OpsCard {
                    SecurityHeroRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometrik",
                        subtitle = if (isPinEnabled) {
                            "Barmaq izi / Üz tanıma aktiv"
                        } else {
                            "Əvvəlcə PIN təyin edin"
                        },
                    )

                    if (isPinEnabled) {
                        Spacer(Modifier.height(8.dp))
                        OpsInfoRow("Status", "PIN ilə birlikdə istifadə olunur")
                    }
                }
            }

            // Auto Lock Section
            OpsCard {
                SecurityHeroRow(
                    icon = Icons.Default.Timer,
                    title = "Avtomatik Kilid",
                    subtitle = if (isPinEnabled) "Aktiv" else "PIN tələb olunur",
                )

                if (isPinEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OpsInfoRow("Kilid müddəti", "2 dəqiqə fondan sonra")
                    OpsInfoRow("Səhv cəhd limiti", "5 cəhd → 30 san gözləmə")
                }
            }
        }
    }
}

@Composable
private fun SecurityHeroRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = OpsColors.Terracotta,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OpsColors.Muted,
            )
        }
    }
}
