package dev.involvex.kiln.core.designsystem

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel

/**
 * Nav3 [androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator]
 * owners often miss Hilt’s ViewModel map and fall through to a no-arg constructor.
 * Scoping to the Activity uses Hilt’s ActivityRetainedComponent reliably.
 */
@Composable
inline fun <reified VM : ViewModel> kilnHiltViewModel(): VM {
    val activity = LocalActivity.current as ComponentActivity
    return hiltViewModel(viewModelStoreOwner = activity)
}
