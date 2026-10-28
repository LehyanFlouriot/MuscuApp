package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingRepositoryTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    private val today = LocalDate.of(2026, 10, 27)
    private var ids = 0

    private fun TestScope.repository(storage: FakeStorage): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(storage, backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    @Test
    fun theProgramAndItsExercisesAreAddedOnce() = runTest {
        val storage = FakeStorage()
        val data = repository(storage).data.value!!
        assertEquals(HypertrophyProgram.exercises(createdAt = 1000L), data.exercises)
        assertEquals(HypertrophyProgram.create(), data.training.program)
        assertTrue(data.training.seeded)

        // A deleted exercise does not come back, and leaves the program.
        val repo = repository(FakeStorage(storage.content))
        repo.deleteExercise("ex-rollout-serviette")
        val reloaded = repository(FakeStorage(AppJson.encode(repo.data.value!!))).data.value!!
        assertEquals(23, reloaded.exercises.size)
        val wednesday = reloaded.training.program!!.days[2]
        assertEquals(5, wednesday.exercises.size)
        assertFalse(wednesday.exercises.any { it.exerciseId == "ex-rollout-serviette" })
    }

    @Test
    fun anExerciseAlreadyThereKeepsItsHistory() = runTest {
        val mine = Exercise("mine", "squat Bulgare ", MuscleGroup.OTHER, archived = true)
        val old = AppData.initial().copy(
            exercises = listOf(mine, Exercise("other", "Développé couché", MuscleGroup.CHEST)),
            entries = listOf(WorkoutEntry("e", "mine", today.minusDays(1), listOf(WorkSet(12)))),
        )
        val data = repository(FakeStorage(AppJson.encode(old))).data.value!!
        // No second "Squat bulgare": the one of the user is used, and shown again.
        assertEquals(25, data.exercises.size)
        assertEquals(1, data.exercises.count { ExerciseCatalog.normalize(it.name) == "squat bulgare" })
        assertFalse(data.exercise("mine")!!.archived)
        assertNull(data.exercise("ex-squat-bulgare"))
        val squats = data.training.program!!.days.flatMap { it.exercises }.filter { it.target.per == "jambe" && it.target.sets != 3 }
        assertEquals(listOf("mine", "mine"), squats.map { it.exerciseId })
        assertEquals("Développé couché", data.exercise("other")!!.name)
        assertEquals(1, data.entries.size)
    }

    @Test
    fun aBackupFromBeforeTheProgramGetsIt() = runTest {
        val repo = repository(FakeStorage())
        repo.replaceAll(AppData.initial().copy(exercises = listOf(Exercise("x", "Gainage", MuscleGroup.ABS))))
        val data = repo.data.value!!
        assertEquals(25, data.exercises.size)
        assertTrue(data.training.seeded)
    }
}
