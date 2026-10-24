package az.mtk.app.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.auth.BiometricAvailability
import az.mtk.app.data.auth.PinVerifyResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockUiState(
    val mode: LockMode = LockMode.Checking,
    val pinDigits: String = "",
    val error: String? = null,
    val attemptsRemaining: Int = 5,
    val lockoutSeconds: Int = 0,
    val biometricAvailable: Boolean = false,
)

enum class LockMode {
    Checking,
    PinSetup,
    PinConfirm,
    PinEntry,
    BiometricPrompt,
    Unlocked,
}

class LockViewModel : ViewModel() {

    private val pinManager = AppGraph.pinManager
    private val lockManager = AppGraph.lockManager
    private val biometricHelper = AppGraph.biometricHelper
    private val auth = AppGraph.auth

    private val _state = MutableStateFlow(LockUiState())
    val state = _state.asStateFlow()

    private var setupPin: String? = null
    private var isInSetupMode = false

    fun checkLockState() {
        // Don't override if we're in PIN setup mode
        if (isInSetupMode) return

        _state.value = _state.value.copy(mode = LockMode.Checking, pinDigits = "", error = null)

        viewModelScope.launch {
            if (!auth.hasValidSession()) {
                _state.value = _state.value.copy(mode = LockMode.Unlocked)
                return@launch
            }

            if (!pinManager.isPinSet()) {
                lockManager.unlock()
                _state.value = _state.value.copy(mode = LockMode.Unlocked)
                return@launch
            }

            if (!lockManager.shouldLock()) {
                _state.value = _state.value.copy(mode = LockMode.Unlocked)
                return@launch
            }

            val lockoutRemaining = pinManager.getLockoutRemaining()
            if (lockoutRemaining > 0) {
                startLockoutTimer(lockoutRemaining)
                return@launch
            }

            val biometricAvailable = biometricHelper.canUseBiometric() == BiometricAvailability.Available
            _state.value = _state.value.copy(
                mode = if (biometricAvailable) LockMode.BiometricPrompt else LockMode.PinEntry,
                biometricAvailable = biometricAvailable,
                attemptsRemaining = pinManager.getRemainingAttempts(),
            )
        }
    }

    fun startPinSetup() {
        isInSetupMode = true
        setupPin = null
        _state.value = _state.value.copy(
            mode = LockMode.PinSetup,
            pinDigits = "",
            error = null,
        )
    }

    fun startPinChange() {
        // For changing PIN, we clear the old one and start fresh setup
        pinManager.clearPin()
        startPinSetup()
    }

    fun onDigitEntered(digit: Char) {
        val current = _state.value.pinDigits
        if (current.length >= 4) return

        val newPin = current + digit
        _state.value = _state.value.copy(pinDigits = newPin, error = null)

        if (newPin.length == 4) {
            when (_state.value.mode) {
                LockMode.PinSetup -> onSetupPinEntered(newPin)
                LockMode.PinConfirm -> onConfirmPinEntered(newPin)
                LockMode.PinEntry -> onPinEntered(newPin)
                else -> {}
            }
        }
    }

    fun onDigitDeleted() {
        val current = _state.value.pinDigits
        if (current.isNotEmpty()) {
            _state.value = _state.value.copy(pinDigits = current.dropLast(1), error = null)
        }
    }

    private fun onSetupPinEntered(pin: String) {
        setupPin = pin
        _state.value = _state.value.copy(
            mode = LockMode.PinConfirm,
            pinDigits = "",
            error = null,
        )
    }

    private fun onConfirmPinEntered(pin: String) {
        if (pin == setupPin) {
            pinManager.setPin(pin)
            setupPin = null
            isInSetupMode = false
            lockManager.unlock()
            _state.value = _state.value.copy(mode = LockMode.Unlocked)
        } else {
            _state.value = _state.value.copy(
                pinDigits = "",
                error = "PIN-lər uyğun gəlmir. Yenidən cəhd edin.",
            )
            viewModelScope.launch {
                delay(1500)
                setupPin = null
                _state.value = _state.value.copy(mode = LockMode.PinSetup, error = null, pinDigits = "")
            }
        }
    }

    private fun onPinEntered(pin: String) {
        viewModelScope.launch {
            when (val result = pinManager.verifyPin(pin)) {
                is PinVerifyResult.Success -> {
                    // PIN düzgündür - dərhal aç, token refresh arxa planda baş verəcək
                    lockManager.unlock()
                    _state.value = _state.value.copy(mode = LockMode.Unlocked)
                    // Token refresh-i arxa planda et
                    auth.refreshTokenIfNeeded()
                }
                is PinVerifyResult.Failed -> {
                    _state.value = _state.value.copy(
                        pinDigits = "",
                        error = "Səhv PIN. ${result.attemptsRemaining} cəhd qalıb.",
                        attemptsRemaining = result.attemptsRemaining,
                    )
                }
                is PinVerifyResult.LockedOut -> {
                    startLockoutTimer(result.remainingSeconds)
                }
                is PinVerifyResult.NoPinSet -> {
                    _state.value = _state.value.copy(mode = LockMode.Unlocked)
                }
            }
        }
    }

    private fun startLockoutTimer(seconds: Int) {
        _state.value = _state.value.copy(
            mode = LockMode.PinEntry,
            pinDigits = "",
            lockoutSeconds = seconds,
            error = "$seconds saniyə gözləyin.",
        )

        viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.value = _state.value.copy(
                    lockoutSeconds = remaining,
                    error = if (remaining > 0) "$remaining saniyə gözləyin." else null,
                )
            }
            _state.value = _state.value.copy(
                attemptsRemaining = pinManager.getRemainingAttempts(),
            )
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            // Biometrik uğurludur - dərhal aç
            lockManager.unlock()
            _state.value = _state.value.copy(mode = LockMode.Unlocked)
            // Token refresh-i arxa planda et
            auth.refreshTokenIfNeeded()
        }
    }

    fun onBiometricFallback() {
        _state.value = _state.value.copy(mode = LockMode.PinEntry)
    }

    fun onForgotPin() {
        viewModelScope.launch {
            pinManager.clearPin()
            lockManager.clear()
            auth.logout()
        }
    }

    fun switchToBiometric() {
        if (_state.value.biometricAvailable) {
            _state.value = _state.value.copy(mode = LockMode.BiometricPrompt)
        }
    }

    fun skipPinSetup() {
        isInSetupMode = false
        lockManager.unlock()
        _state.value = _state.value.copy(mode = LockMode.Unlocked)
    }
}
