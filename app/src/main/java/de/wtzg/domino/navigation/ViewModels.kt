package de.wtzg.domino.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.wtzg.domino.AppContainer
import de.wtzg.domino.DominoApplication

/** Gets a ViewModel built from the app's [AppContainer] (manual dependency injection). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    crossinline create: (AppContainer) -> VM,
): VM = viewModel(
    viewModelStoreOwner = owner,
    key = key,
    factory = viewModelFactory {
        initializer { create((this[APPLICATION_KEY] as DominoApplication).container) }
    },
)
