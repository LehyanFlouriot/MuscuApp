package com.lehyanflouriot.muscuapp.timer

import android.content.Context
import androidx.core.content.edit
import com.lehyanflouriot.muscuapp.core.data.AppJson
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rest timer shared by the whole app. The state is persisted, an exact alarm rings at the end
 * (even when the app is closed or the screen is off) and a notification shows the countdown.
 */
class RestTimer(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notifications = RestNotifications(appContext)
    private val alarms = RestAlarms(appContext)

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _idleDuration = MutableStateFlow(
        prefs.getLong(KEY_IDLE_DURATION, DEFAULT_DURATION_MS).coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
    )

    /** Duration offered when no rest is running: the last one used. */
    val idleDuration: StateFlow<Long> = _idleDuration.asStateFlow()

    init {
        reconcile()
    }

    fun now(): Long = clock()

    @Synchronized
    fun start(durationMs: Long, label: String? = null) {
        val duration = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        setIdleDuration(duration)
        update(TimerLogic.start(clock(), duration, label))
    }

    @Synchronized
    fun pause() = update(TimerLogic.pause(_state.value, clock()))

    @Synchronized
    fun resume() = update(TimerLogic.resume(_state.value, clock()))

    @Synchronized
    fun toggle() {
        when (_state.value) {
            is TimerState.Running -> pause()
            is TimerState.Paused -> resume()
            else -> start(_idleDuration.value)
        }
    }

    @Synchronized
    fun adjust(deltaMs: Long) {
        val current = _state.value
        val next = TimerLogic.adjust(current, clock(), deltaMs)
        if (next != current) update(next, alert = next is TimerState.Finished)
    }

    @Synchronized
    fun stop() = update(TimerState.Idle)

    /** Starts again the last rest (same duration and label). */
    @Synchronized
    fun restart() {
        val current = _state.value
        val total = TimerLogic.total(current).takeIf { it > 0 } ?: _idleDuration.value
        start(total, TimerLogic.label(current))
    }

    /** Called when the alarm goes off. */
    @Synchronized
    fun onAlarm() = finishIfDue(ALARM_TOLERANCE_MS)

    /** Called by the UI when the countdown reaches zero while the app is visible. */
    @Synchronized
    fun finishIfDue(toleranceMs: Long = 0) {
        val current = _state.value
        if (current is TimerState.Running && clock() >= current.endAt - toleranceMs) {
            update(TimerLogic.finish(current), alert = true)
        }
    }

    fun setIdleDuration(durationMs: Long) {
        val value = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        _idleDuration.value = value
        prefs.edit { putLong(KEY_IDLE_DURATION, value) }
    }

    private fun update(next: TimerState, alert: Boolean = false) {
        _state.value = next
        prefs.edit { putString(KEY_STATE, AppJson.json.encodeToString(TimerState.serializer(), next)) }
        when (next) {
            is TimerState.Running -> {
                alarms.schedule(next.endAt)
                notifications.showRunning(next)
            }
            is TimerState.Paused -> {
                alarms.cancel()
                notifications.showPaused(next)
            }
            is TimerState.Finished -> {
                alarms.cancel()
                notifications.cancelOngoing()
                if (alert) notifications.showFinished(next)
            }
            TimerState.Idle -> {
                alarms.cancel()
                notifications.cancelAll()
            }
        }
    }

    private fun readState(): TimerState =
        prefs.getString(KEY_STATE, null)
            ?.let { runCatching { AppJson.json.decodeFromString(TimerState.serializer(), it) }.getOrNull() }
            ?: TimerState.Idle

    /** Puts things back in order after the process was restarted. */
    private fun reconcile() {
        val current = _state.value
        if (current is TimerState.Running) {
            if (clock() - current.endAt > MISSED_ALARM_MS) {
                // The alarm was lost (reboot, app force-stopped): end the rest quietly.
                update(TimerLogic.finish(current))
            } else {
                alarms.schedule(current.endAt)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "rest_timer"
        private const val KEY_STATE = "state"
        private const val KEY_IDLE_DURATION = "idle_duration"
        private const val ALARM_TOLERANCE_MS = 1_000L
        private const val MISSED_ALARM_MS = 60_000L

        const val DEFAULT_DURATION_MS = 90_000L
        const val MIN_DURATION_MS = 5_000L
        const val MAX_DURATION_MS = 60 * 60_000L

        /** How long a finished rest stays on screen before the timer looks idle again. */
        const val FINISHED_DISPLAY_MS = 10 * 60_000L
    }
}
