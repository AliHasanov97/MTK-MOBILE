package az.mtk.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Small helper so screens can build their ViewModel without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> mtkViewModel(
    key: String? = null,
    crossinline create: () -> VM,
): VM = viewModel(key = key, factory = viewModelFactory { initializer { create() } })
