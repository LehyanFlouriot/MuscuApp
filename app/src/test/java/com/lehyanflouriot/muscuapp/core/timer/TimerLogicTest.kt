package com.lehyanflouriot.muscuapp.core.timer

import com.lehyanflouriot.muscuapp.core.data.AppJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {
    private val now = 1_000_000L

    @Test
    fun runningTimerCountsDown() {
        val running = TimerLogic.start(now, 90_000, "Squat")
        assertEquals(90_000, TimerLogic.remaining(running, now))
        assertEquals(30_000, TimerLogic.remaining(running, now + 60_000))
        assertEquals(0, TimerLogic.remaining(running, now + 100_000))
        assertEquals(0.5f, TimerLogic.fractionRemaining(running, now + 45_000), 1e-6f)
        assertFalse(TimerLogic.isDue(running, now + 89_999))
        assertTrue(TimerLogic.isDue(running, now + 90_000))
        assertEquals("Squat", TimerLogic.label(running))
    }

    @Test
    fun pauseAndResumeKeepTheRemainingTime() {
        val running = TimerLogic.start(now, 60_000)
        val paused = TimerLogic.pause(running, now + 20_000)
        assertEquals(TimerState.Paused(remaining = 40_000, total = 60_000), paused)
        assertEquals(40_000, TimerLogic.remaining(paused, now + 500_000))
        val resumed = TimerLogic.resume(paused, now + 100_000)
        assertEquals(TimerState.Running(endAt = now + 140_000, total = 60_000), resumed)
        // Pausing something that does not run changes nothing.
        assertEquals(paused, TimerLogic.pause(paused, now))
        assertEquals(TimerState.Idle, TimerLogic.resume(TimerState.Idle, now))
    }

    @Test
    fun addingTimeExtendsTheTotal() {
        val running = TimerLogic.start(now, 60_000)
        val longer = TimerLogic.adjust(running, now + 50_000, 15_000) as TimerState.Running
        assertEquals(now + 75_000, longer.endAt)
        assertEquals(75_000, longer.total)

        val shorter = TimerLogic.adjust(running, now + 10_000, -15_000) as TimerState.Running
        assertEquals(now + 45_000, shorter.endAt)
        assertEquals(45_000, shorter.total)
    }

    @Test
    fun removingMoreThanWhatIsLeftFinishesTheRest() {
        val running = TimerLogic.start(now, 60_000)
        val finished = TimerLogic.adjust(running, now + 50_000, -15_000)
        assertEquals(TimerState.Finished(finishedAt = now + 50_000, total = 60_000), finished)

        val paused = TimerState.Paused(remaining = 5_000, total = 60_000)
        assertTrue(TimerLogic.adjust(paused, now, -15_000) is TimerState.Finished)
        assertEquals(TimerState.Paused(remaining = 20_000, total = 75_000), TimerLogic.adjust(paused, now, 15_000))
    }

    @Test
    fun finishingUsesThePlannedEndTime() {
        val running = TimerLogic.start(now, 60_000, "Dips")
        val finished = TimerLogic.finish(running)
        assertEquals(TimerState.Finished(finishedAt = now + 60_000, total = 60_000, label = "Dips"), finished)
        assertEquals(5_000, TimerLogic.overtime(finished, now + 65_000))
        assertEquals(0f, TimerLogic.fractionRemaining(finished, now), 0f)
        assertEquals(finished, TimerLogic.finish(finished))
    }

    @Test
    fun statesSurviveSerialization() {
        val states = listOf(
            TimerState.Idle,
            TimerLogic.start(now, 90_000, "Squat"),
            TimerState.Paused(remaining = 1_000, total = 2_000),
            TimerState.Finished(finishedAt = now, total = 3_000, label = null),
        )
        for (state in states) {
            val text = AppJson.json.encodeToString(TimerState.serializer(), state)
            assertEquals(state, AppJson.json.decodeFromString(TimerState.serializer(), text))
        }
    }
}
