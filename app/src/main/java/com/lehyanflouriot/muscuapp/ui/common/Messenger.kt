package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shows short messages ("Séance enregistrée", "Annuler"...) at the bottom of the screen. */
fun interface Messenger {
    fun show(message: String, actionLabel: String?, onAction: (() -> Unit)?)
}

fun Messenger.show(message: String) = show(message, null, null)

/** Messages outlive the screen or sheet that sent them: they run in the app's scope. */
class SnackbarMessenger(
    private val scope: CoroutineScope,
    private val hostState: SnackbarHostState,
) : Messenger {
    override fun show(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalMessenger = staticCompositionLocalOf<Messenger> { Messenger { _, _, _ -> } }
