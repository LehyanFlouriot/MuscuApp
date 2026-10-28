package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.data.HypertrophyProgram
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RemindersTest {
    // A Wednesday, with the session of the quadriceps.
    private val wednesday = LocalDate.of(2026, 10, 28)
    private val data = AppData(
        exercises = HypertrophyProgram.exercises(),
        training = Training(
            programs = listOf(HypertrophyProgram.create()),
            schedule = listOf(PlanPeriod(HypertrophyProgram.ID, wednesday.minusDays(2))),
            seeded = true,
        ),
    )

    @Test
    fun aReminderGoesOffAtItsNextTime() {
        val six = LocalTime.of(18, 0)
        assertEquals(wednesday.atTime(six), nextReminder(wednesday.atTime(7, 15), six))
        // Once the time is past, or right at it, the next one is tomorrow.
        assertEquals(wednesday.plusDays(1).atTime(six), nextReminder(wednesday.atTime(18, 0), six))
        assertEquals(wednesday.plusDays(1).atTime(six), nextReminder(wednesday.atTime(23, 59), six))
        assertEquals(wednesday.plusDays(1).atTime(0, 0), nextReminder(wednesday.atTime(12, 0), LocalTime.MIDNIGHT))
    }

    @Test
    fun theSessionIsRemindedWhileExercisesAreLeft() {
        assertEquals(6, sessionToRemind(data, wednesday)!!.items.size)
        // Thursday is a rest day, and there is no program before Monday.
        assertNull(sessionToRemind(data, wednesday.plusDays(1)))
        assertNull(sessionToRemind(data, wednesday.minusWeeks(1)))

        val session = buildSession(data, wednesday)!!
        val someDone = data.copy(entries = listOf(WorkoutEntry("a", session.items[0].exercise.id, wednesday, listOf(WorkSet(10)))))
        assertNotNull(sessionToRemind(someDone, wednesday))
        val allDone = data.copy(
            entries = session.items.mapIndexed { index, item -> WorkoutEntry("e$index", item.exercise.id, wednesday, listOf(WorkSet(10))) },
        )
        assertNull(sessionToRemind(allDone, wednesday))
    }

    @Test
    fun theWeighInIsRemindedUntilTheWeightIsNoted() {
        assertTrue(weighInToRemind(data, wednesday))
        val weighed = data.copy(bodyWeights = listOf(BodyWeightEntry(wednesday, 70.4)))
        assertFalse(weighInToRemind(weighed, wednesday))
        assertTrue(weighInToRemind(weighed, wednesday.plusDays(1)))
    }
}
