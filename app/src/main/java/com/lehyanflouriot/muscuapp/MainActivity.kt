package com.lehyanflouriot.muscuapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.core.model.ThemeMode
import com.lehyanflouriot.muscuapp.ui.MuscuApp
import com.lehyanflouriot.muscuapp.ui.OpenRequest
import com.lehyanflouriot.muscuapp.ui.OpenTarget
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import com.lehyanflouriot.muscuapp.ui.theme.isDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    /** The last screen a notification or the widget asked to show. */
    private val openRequests = MutableStateFlow<OpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        val container = appContainer

        setContent {
            val data by container.repository.data.collectAsStateWithLifecycle()
            val settings = data?.settings
            val darkTheme = (settings?.themeMode ?: ThemeMode.SYSTEM).isDark(isSystemInDarkTheme())
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            MuscuTheme(darkTheme = darkTheme, dynamicColor = settings?.dynamicColor == true) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    data?.let { loaded ->
                        MuscuApp(data = loaded, container = container, openRequests = openRequests)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.sync.onForeground()
    }

    override fun onStop() {
        appContainer.sync.onBackground()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val target = when (intent?.action) {
            ACTION_OPEN_REST -> OpenTarget.REST
            ACTION_OPEN_SESSION -> OpenTarget.SESSION
            ACTION_OPEN_WEIGHT -> OpenTarget.WEIGHT
            else -> return
        }
        openRequests.update { OpenRequest(target, (it?.count ?: 0) + 1) }
    }

    companion object {
        const val ACTION_OPEN_REST = "com.lehyanflouriot.muscuapp.action.OPEN_REST"
        const val ACTION_OPEN_SESSION = "com.lehyanflouriot.muscuapp.action.OPEN_SESSION"
        const val ACTION_OPEN_WEIGHT = "com.lehyanflouriot.muscuapp.action.OPEN_WEIGHT"
    }
}
