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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import kotlinx.coroutines.delay

/** A rest that ended long ago is displayed as if the timer was idle. */
fun TimerState.displayed(now: Long): TimerState =
    if (this is TimerState.Finished && now - finishedAt > RestTimer.FINISHED_DISPLAY_MS) TimerState.Idle else this

/** Time between two refreshes of a running timer. */
const val TIMER_TICK_MS = 200

/** Current time, refreshed as often as the timer display needs it. */
@Composable
fun rememberTimerNow(state: TimerState, clock: () -> Long): Long {
    // Read again as soon as the timer changes: a rest just started or resumed must not be shown
    // with the time of the last tick, which can be minutes old.
    val changedAt = remember(state) { clock() }
    val ticked by produceState(initialValue = changedAt, state) {
        while (true) {
            value = clock()
            when (state) {
                is TimerState.Running -> delay(TIMER_TICK_MS.toLong())
                is TimerState.Finished -> delay(1_000)
                else -> break
            }
        }
    }
    return maxOf(ticked, changedAt)
}

private class DrawnProgress {
    var value = Float.NaN
    var gap: Animatable<Float, AnimationVector1D>? = null
}

/**
 * Share of the rest still to do, as the ring and the bar draw it. It flows at every frame rather
 * than at each tick of [now], and when the timer is changed (time added, rest stopped or started
 * again) it glides from what was drawn to its new value instead of jumping. Read it while drawing.
 */
@Composable
fun rememberRestProgress(state: TimerState, now: Long): () -> Float {
    val drawn = remember { DrawnProgress() }
    // Time elapsed since the last tick, so that the progress keeps moving until the next one.
    val sinceTick = remember(state, now) { Animatable(0f) }
    LaunchedEffect(sinceTick) {
        if (state is TimerState.Running) {
            sinceTick.animateTo(TIMER_TICK_MS.toFloat(), tween(TIMER_TICK_MS, easing = LinearEasing))
        }
    }
    // Set while composing, so that the first frame after a change already starts from what was drawn.
    val gap = remember(state) {
        val start = if (drawn.value.isNaN()) 0f else drawn.value - TimerLogic.fractionRemaining(state, now)
        Animatable(start) to (drawn.gap?.velocity ?: 0f)
    }
    LaunchedEffect(gap) {
        val (animatable, velocity) = gap
        drawn.gap = animatable
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
            initialVelocity = velocity,
        )
    }
    return remember(state, now) {
        {
            val time = now + sinceTick.value.toLong()
            val value = (TimerLogic.fractionRemaining(state, time) + gap.first.value).coerceIn(0f, 1f)
            drawn.value = value
            value
        }
    }
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
