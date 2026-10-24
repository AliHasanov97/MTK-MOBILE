package az.mtk.app.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.mtk.app.AppGraph
import az.mtk.app.ui.screens.operations.OpsColors

@Composable
fun LockScreen(
    viewModel: LockViewModel = viewModel(),
    onUnlocked: () -> Unit,
    onKeycloakLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        viewModel.checkLockState()
    }

    // Track mode changes and call onUnlocked when needed
    val currentMode = state.mode
    LaunchedEffect(currentMode) {
        when (currentMode) {
            LockMode.Unlocked -> onUnlocked()
            LockMode.BiometricPrompt -> {
                if (activity != null) {
                    AppGraph.biometricHelper.authenticate(
                        activity = activity,
                        onSuccess = viewModel::onBiometricSuccess,
                        onError = { viewModel.onBiometricFallback() },
                        onFallback = viewModel::onBiometricFallback,
                    )
                } else {
                    viewModel.onBiometricFallback()
                }
            }
            else -> {}
        }
    }

    BackHandler(enabled = state.mode != LockMode.Unlocked && state.mode != LockMode.Checking) {
        if (state.mode == LockMode.PinSetup || state.mode == LockMode.PinConfirm) {
            viewModel.skipPinSetup()
        }
    }

    if (state.mode == LockMode.Unlocked || state.mode == LockMode.Checking) {
        return
    }

    Scaffold(
        containerColor = OpsColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = when (state.mode) {
                    LockMode.PinSetup -> "PIN kod təyin edin"
                    LockMode.PinConfirm -> "PIN-i təsdiqləyin"
                    else -> "PIN daxil edin"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Ink,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (state.mode) {
                    LockMode.PinSetup -> "4 rəqəmli təhlükəsizlik kodu"
                    LockMode.PinConfirm -> "Eyni PIN-i yenidən daxil edin"
                    else -> "Tətbiqə giriş üçün"
                },
                fontSize = 14.sp,
                color = OpsColors.Muted,
            )

            Spacer(Modifier.height(48.dp))

            PinDots(
                filledCount = state.pinDigits.length,
                error = state.error != null && state.lockoutSeconds == 0,
            )

            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.error!!,
                    fontSize = 13.sp,
                    color = OpsColors.Error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(48.dp))

            PinKeypad(
                onDigit = { if (state.lockoutSeconds == 0) viewModel.onDigitEntered(it) },
                onDelete = { if (state.lockoutSeconds == 0) viewModel.onDigitDeleted() },
                onBiometric = if (state.biometricAvailable && state.mode == LockMode.PinEntry) {
                    { viewModel.switchToBiometric() }
                } else null,
                enabled = state.lockoutSeconds == 0,
            )

            Spacer(Modifier.weight(1f))

            if (state.mode == LockMode.PinEntry) {
                TextButton(
                    onClick = {
                        viewModel.onForgotPin()
                        onKeycloakLogin()
                    },
                ) {
                    Text(
                        text = "PIN-i unutdum",
                        fontSize = 14.sp,
                        color = OpsColors.Terracotta,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PinSetupDialog(
    onSetup: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = OpsColors.Surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "PIN kod təyin edin?",
                fontWeight = FontWeight.SemiBold,
                color = OpsColors.Ink,
            )
        },
        text = {
            Text(
                text = "Növbəti girişlərdə daha sürətli daxil olmaq üçün 4 rəqəmli PIN təyin edə bilərsiniz.",
                color = OpsColors.Muted,
            )
        },
        confirmButton = {
            Button(
                onClick = onSetup,
                colors = ButtonDefaults.buttonColors(containerColor = OpsColors.Terracotta),
            ) {
                Text("Bəli, təyin et")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Xeyr, sonra", color = OpsColors.Muted)
            }
        },
    )
}

@Composable
private fun PinDots(filledCount: Int, error: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            error -> OpsColors.Error
                            index < filledCount -> OpsColors.Terracotta
                            else -> OpsColors.Border
                        }
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)?,
    enabled: Boolean,
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('B', '0', 'D'),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        'B' -> {
                            if (onBiometric != null) {
                                KeyButton(
                                    onClick = onBiometric,
                                    enabled = enabled,
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Biometrik",
                                        tint = OpsColors.Terracotta,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(72.dp))
                            }
                        }
                        'D' -> {
                            KeyButton(
                                onClick = onDelete,
                                enabled = enabled,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Sil",
                                    tint = OpsColors.Muted,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        else -> {
                            KeyButton(
                                onClick = { onDigit(key) },
                                enabled = enabled,
                            ) {
                                Text(
                                    text = key.toString(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (enabled) OpsColors.Ink else OpsColors.Muted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(OpsColors.Container)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
