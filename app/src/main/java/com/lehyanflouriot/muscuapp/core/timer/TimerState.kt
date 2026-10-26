package com.lehyanflouriot.muscuapp.core.timer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** State of the rest timer. Times are wall-clock milliseconds, durations are milliseconds. */
@Serializable
sealed interface TimerState {
    @Serializable
    @SerialName("idle")
    data object Idle : TimerState

    @Serializable
    @SerialName("running")
    data class Running(val endAt: Long, val total: Long, val label: String? = null) : TimerState

    @Serializable
    @SerialName("paused")
    data class Paused(val remaining: Long, val total: Long, val label: String? = null) : TimerState

    @Serializable
    @SerialName("finished")
    data class Finished(val finishedAt: Long, val total: Long, val label: String? = null) : TimerState
}

/** Pure transitions of the rest timer, kept free of Android so they can be unit tested. */
object TimerLogic {
    fun start(now: Long, duration: Long, label: String? = null): TimerState.Running =
        TimerState.Running(endAt = now + duration, total = duration, label = label)

    fun remaining(state: TimerState, now: Long): Long = when (state) {
        is TimerState.Running -> (state.endAt - now).coerceAtLeast(0)
        is TimerState.Paused -> state.remaining
        else -> 0
    }

    /** Share of the rest still to do, from 1 (just started) to 0 (done). */
    fun fractionRemaining(state: TimerState, now: Long): Float = when (state) {
        is TimerState.Running, is TimerState.Paused -> {
            val total = total(state)
            if (total <= 0) 0f else (remaining(state, now).toFloat() / total).coerceIn(0f, 1f)
        }
        TimerState.Idle -> 1f
        is TimerState.Finished -> 0f
    }

    fun total(state: TimerState): Long = when (state) {
        is TimerState.Running -> state.total
        is TimerState.Paused -> state.total
        is TimerState.Finished -> state.total
        TimerState.Idle -> 0
    }

    fun label(state: TimerState): String? = when (state) {
        is TimerState.Running -> state.label
        is TimerState.Paused -> state.label
        is TimerState.Finished -> state.label
        TimerState.Idle -> null
    }

    fun pause(state: TimerState, now: Long): TimerState =
        if (state is TimerState.Running) {
            TimerState.Paused(remaining = (state.endAt - now).coerceAtLeast(0), total = state.total, label = state.label)
        } else {
            state
        }

    fun resume(state: TimerState, now: Long): TimerState =
        if (state is TimerState.Paused) {
            TimerState.Running(endAt = now + state.remaining, total = state.total, label = state.label)
        } else {
            state
        }

    /** Adds (or removes, when negative) time. Removing more than what is left ends the rest. */
    fun adjust(state: TimerState, now: Long, delta: Long): TimerState = when (state) {
        is TimerState.Running -> {
            val endAt = state.endAt + delta
            if (endAt <= now) {
                TimerState.Finished(finishedAt = now, total = state.total, label = state.label)
            } else {
                state.copy(endAt = endAt, total = (state.total + delta).coerceAtLeast(endAt - now))
            }
        }
        is TimerState.Paused -> {
            val remaining = state.remaining + delta
            if (remaining <= 0) {
                TimerState.Finished(finishedAt = now, total = state.total, label = state.label)
            } else {
                state.copy(remaining = remaining, total = (state.total + delta).coerceAtLeast(remaining))
            }
        }
        else -> state
    }

    fun isDue(state: TimerState, now: Long): Boolean = state is TimerState.Running && now >= state.endAt

    /** Ends a running timer at its planned end time (even when the alarm arrives a bit late). */
    fun finish(state: TimerState): TimerState =
        if (state is TimerState.Running) {
            TimerState.Finished(finishedAt = state.endAt, total = state.total, label = state.label)
        } else {
            state
        }

    /** Time elapsed since the end of the rest. */
    fun overtime(state: TimerState, now: Long): Long =
        if (state is TimerState.Finished) (now - state.finishedAt).coerceAtLeast(0) else 0
}
