package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.ADVISED_WEEKLY_SETS
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.share
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.core.sync.isPristine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NoEquipmentProgramTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    // Saturday 31 October 2026.
    private val today = LocalDate.of(2026, 10, 31)
    private val monday = LocalDate.of(2026, 10, 26)
    private val nextMonday = LocalDate.of(2026, 11, 2)
    private var ids = 0

    private fun TestScope.repository(storage: FakeStorage = FakeStorage()): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(storage, backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    /** What 1.13 saved: the first program, followed since the week before, and a session done on Wednesday. */
    private fun firstProgramData(programs: List<TrainingProgram> = emptyList(), schedule: List<PlanPeriod> = emptyList()) =
        AppData.initial().copy(
            exercises = HypertrophyProgram.exercises(),
            entries = listOf(WorkoutEntry("e", "ex-squat-bulgare", monday.plusDays(2), listOf(WorkSet(12)))),
            training = Training(
                programs = listOf(HypertrophyProgram.create()) + programs,
                schedule = listOf(PlanPeriod(HypertrophyProgram.ID, monday.minusWeeks(1))) + schedule,
                seeded = true,
            ),
        )

    @Test
    fun theSessionsNeedNoEquipmentAndFollowTheDaysOfTheMealPlan() {
        val program = NoEquipmentProgram.create()
        val exercises = NoEquipmentProgram.exercises().associateBy { it.id }
        assertEquals(DefaultMealPlan.create().days.map { it.training }, program.days.map { !it.isRest })
        program.days.filterNot { it.isRest }.forEach { day ->
            assertEquals(day.title, 1, day.exercises.count { it.emphasis })
            assertTrue(day.title, day.exercises.all { it.exerciseId in exercises })
        }
        assertTrue(exercises.values.none { it.usesWeight })
        // The exercises it shares with the first program are the same ones, so that their history goes on.
        val first = HypertrophyProgram.exercises().associateBy { it.id }
        val shared = exercises.keys.filter { it in first }
        assertEquals(listOf("ex-extensions-triceps-sol", "ex-squat-bulgare", "ex-mollets-unilateraux"), shared)
        shared.forEach { assertEquals(first[it], exercises[it]) }
    }

    @Test
    fun everyMuscleGetsTheAdvisedSetsOfAWeek() {
        val exercises = NoEquipmentProgram.exercises().associateBy { it.id }
        val lines = NoEquipmentProgram.create().days.flatMap { it.exercises }
        MuscleGroup.muscles.forEach { muscle ->
            val sets = lines.sumOf { it.target.sets * exercises.getValue(it.exerciseId).share(muscle) }
            assertTrue("$muscle: $sets", sets in ADVISED_WEEKLY_SETS)
        }
    }

    @Test
    fun newDataStartsWithTheProgramWithoutEquipment() = runTest {
        val data = repository().data.value!!
        assertEquals(listOf(NoEquipmentProgram.create(createdAt = 1000L)), data.training.programs)
        assertEquals(NoEquipmentProgram.exercises(createdAt = 1000L), data.exercises)
        assertEquals(listOf(PlanPeriod(NoEquipmentProgram.ID, monday)), data.training.schedule)
        assertEquals("Fessiers & ischios", buildSession(data, today)!!.day.title)
        assertTrue(data.isPristine())
    }

    @Test
    fun theFirstProgramHandsOverOnTheComingMonday() = runTest {
        val storage = FakeStorage(AppJson.encode(firstProgramData()))
        val repo = repository(storage)
        val data = repo.data.value!!
        assertEquals(listOf(HypertrophyProgram.ID, NoEquipmentProgram.ID), data.training.programs.map { it.id })
        assertEquals(
            listOf(PlanPeriod(HypertrophyProgram.ID, monday.minusWeeks(1)), PlanPeriod(NoEquipmentProgram.ID, nextMonday)),
            data.training.schedule,
        )
        // This week stays as it was; the next one is the new program.
        assertEquals("Quadriceps", buildSession(data, monday.plusDays(2))!!.day.title)
        assertEquals("Jambes", buildSession(data, nextMonday.plusDays(2))!!.day.title)
        // The exercises both programs have keep their history.
        assertEquals(1, data.exercises.count { it.name == "Squat bulgare" })
        assertEquals(24 + 15, data.exercises.size)

        // Added once: taken out, it does not come back.
        assertTrue(repo.deleteTrainingProgram(NoEquipmentProgram.ID))
        runCurrent()
        val reloaded = repository(FakeStorage(storage.content)).data.value!!
        assertNull(reloaded.training.program(NoEquipmentProgram.ID))
        assertEquals(listOf(PlanPeriod(HypertrophyProgram.ID, monday.minusWeeks(1))), reloaded.training.schedule)
    }

    @Test
    fun aProgramOfTheUserIsKeptAndTheNewOneWaitsAmongThePrograms() = runTest {
        val mine = TrainingProgram(id = "mine", name = "Mon programme")
        val saved = firstProgramData(programs = listOf(mine), schedule = listOf(PlanPeriod("mine", monday)))
        val data = repository(FakeStorage(AppJson.encode(saved))).data.value!!
        assertEquals(listOf(HypertrophyProgram.ID, "mine", NoEquipmentProgram.ID), data.training.programs.map { it.id })
        assertEquals(listOf(HypertrophyProgram.ID, "mine"), data.training.schedule.map { it.planId })
        assertEquals(listOf(NoEquipmentProgram.ID), data.training.addedPrograms)
    }
}
