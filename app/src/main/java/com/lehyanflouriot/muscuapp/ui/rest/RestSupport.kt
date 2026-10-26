package com.lehyanflouriot.muscuapp.ui.rest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import kotlinx.coroutines.delay

/** A rest that ended long ago is displayed as if the timer was idle. */
fun TimerState.displayed(now: Long): TimerState =
    if (this is TimerState.Finished && now - finishedAt > RestTimer.FINISHED_DISPLAY_MS) TimerState.Idle else this

/** Current time, refreshed as often as the timer display needs it. */
@Composable
fun rememberTimerNow(state: TimerState, clock: () -> Long): Long {
    val now by produceState(initialValue = clock(), state) {
        while (true) {
            value = clock()
            when (state) {
                is TimerState.Running -> delay(200)
                is TimerState.Finished -> delay(1_000)
                else -> break
            }
        }
    }
    return now
}

/** Keeps the screen on while [active], so the countdown stays visible. */
@Composable
fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}

fun notificationsEnabled(context: Context): Boolean {
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/** Whether notifications are allowed, refreshed when coming back from the system settings. */
@Composable
fun rememberNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(notificationsEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = notificationsEnabled(context) }
    return enabled
}

fun openNotificationSettings(context: Context, channelId: String? = null) {
    val intent = if (channelId != null) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private object PermissionRequests {
    /** The notification permission is asked at most once per launch of the app. */
    var notificationsAsked = false
}

/**
 * Returns a function that starts a rest. The first time, it also asks for the permission to
 * show notifications (needed to be alerted when the screen is off).
 */
@Composable
fun rememberRestStarter(timer: RestTimer): (seconds: Int, label: String?) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(timer, launcher, context) {
        { seconds, label ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificationsEnabled(context) &&
                !PermissionRequests.notificationsAsked
            ) {
                PermissionRequests.notificationsAsked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            timer.start(seconds * 1_000L, label)
        }
    }
}

/** Asks for the notification permission, or opens the settings when it can no longer be asked. */
@Composable
fun rememberNotificationEnabler(): () -> Unit {
    val context = LocalContext.current
    val launchedAt = remember { longArrayOf(0L) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // An instant refusal means Android no longer shows the question: go to the settings instead.
        if (!granted && SystemClock.elapsedRealtime() - launchedAt[0] < 500) openNotificationSettings(context)
    }
    return remember(launcher, context) {
        {
            val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (permissionMissing) {
                launchedAt[0] = SystemClock.elapsedRealtime()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        }
    }
}
