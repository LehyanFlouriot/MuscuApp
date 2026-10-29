package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WarmUp
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WarmUpTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    // A Tuesday.
    private val today = LocalDate.of(2026, 10, 27)
    private var ids = 0

    private val WorkoutRepository.training: Training get() = data.value!!.training

    private fun TestScope.repository(): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(FakeStorage(), backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    @Test
    fun theProgramComesWithTheWarmUpOfItsAdvice() = runTest {
        val warmUp = repository().training.program(HypertrophyProgram.ID)!!.warmUp
        // "5 min de mobilité, puis 2 séries légères de l'exercice d'emphase."
        assertEquals(WarmUp(enabled = true, mobilityMinutes = 5, lightSets = 2), warmUp)
        assertTrue(warmUp.isActive)
        assertFalse(WarmUp(enabled = true, mobilityMinutes = 0, lightSets = 0).isActive)
        assertFalse(WarmUp(enabled = false).isActive)
    }

    @Test
    fun changingTheWarmUpKeepsNoCopyOfTheProgramAndStaysInBounds() = runTest {
        val repo = repository()
        // Noted on Monday: changing the sessions would keep the program as it was for that day.
        repo.addSet("ex-pompes-deficit", today.minusDays(1), WorkSet(10))
        val programs = repo.training.programs.size

        repo.updateWarmUp(HypertrophyProgram.ID, WarmUp(enabled = true, mobilityMinutes = 99, lightSets = -1))

        assertEquals(WarmUp(enabled = true, mobilityMinutes = MAX_WARM_UP_MINUTES, lightSets = 0), repo.training.program(HypertrophyProgram.ID)!!.warmUp)
        assertEquals(programs, repo.training.programs.size)
    }

    @Test
    fun programsSavedBeforeTheWarmUpGetTheOneOfTheAdvice() {
        val old = """{"schemaVersion":4,"training":{"programs":[{"id":"p","name":"Ancien"}]}}"""
        val data = WorkoutRepository.sanitize(AppJson.decode(old))
        assertEquals(WarmUp(), data.training.program("p")!!.warmUp)
    }
}
