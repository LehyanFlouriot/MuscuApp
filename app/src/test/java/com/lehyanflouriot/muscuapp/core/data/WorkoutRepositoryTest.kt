package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.ThemeMode
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        var writes = 0
        val unreadable = mutableListOf<String>()
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
            writes++
        }
        override fun keepUnreadableCopy(content: String) {
            unreadable += content
        }
    }

    private var ids = 0
    private val day = LocalDate.of(2026, 10, 26)

    private fun TestScope.repository(storage: FakeStorage): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(storage, backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" })
            .also { it.load() }
    }

    @Test
    fun startsWithDefaultPresetsAndSavesThem() = runTest {
        val storage = FakeStorage()
        val repo = repository(storage)
        val data = repo.data.value!!
        assertTrue(data.exercises.isEmpty())
        assertEquals(AppData.DEFAULT_REST_PRESETS, data.restPresets)
        assertEquals(1, storage.writes)
    }

    @Test
    fun changesAreWrittenAndReloaded() = runTest {
        val storage = FakeStorage()
        val repo = repository(storage)
        val id = repo.addExercise(ExerciseDraft("  Squat ", MuscleGroup.LEGS, restSeconds = 180))
        repo.saveEntry(null, id, day, listOf(WorkSet(5, 100.0), WorkSet(0, 100.0), WorkSet(5, -3.0)), " lourd ")
        runCurrent()

        val reloaded = repository(FakeStorage(storage.content)).data.value!!
        val exercise = reloaded.exercises.single()
        assertEquals("Squat", exercise.name)
        assertEquals(MuscleGroup.LEGS, exercise.muscleGroup)
        assertEquals(180, exercise.restSeconds)
        val entry = reloaded.entries.single()
        // Empty sets are dropped and negative loads clamped.
        assertEquals(listOf(WorkSet(5, 100.0), WorkSet(5, 0.0)), entry.sets)
        assertEquals("lourd", entry.note)
        assertEquals(1000L, entry.createdAt)
    }

    @Test
    fun updatingAnEntryKeepsItsIdentity() = runTest {
        val repo = repository(FakeStorage())
        val exerciseId = repo.addExercise(ExerciseDraft("Tractions", usesWeight = false))
        val entryId = repo.saveEntry(null, exerciseId, day, listOf(WorkSet(8)))
        repo.saveEntry(entryId, exerciseId, day.minusDays(1), listOf(WorkSet(10), WorkSet(9)))
        val entry = repo.data.value!!.entries.single()
        assertEquals(entryId, entry.id)
        assertEquals(day.minusDays(1), entry.date)
        assertEquals(2, entry.sets.size)
    }

    @Test
    fun deletingAnExerciseRemovesItsHistoryAndCanBeUndone() = runTest {
        val repo = repository(FakeStorage())
        val keep = repo.addExercise(ExerciseDraft("Curl"))
        val remove = repo.addExercise(ExerciseDraft("Dips", usesWeight = false))
        repo.saveEntry(null, keep, day, listOf(WorkSet(10, 12.0)))
        repo.saveEntry(null, remove, day, listOf(WorkSet(12)))

        val deleted = repo.deleteExercise(remove)
        assertNotNull(deleted)
        assertEquals(1, deleted!!.entries.size)
        assertEquals(listOf(keep), repo.data.value!!.exercises.map { it.id })
        assertEquals(listOf(keep), repo.data.value!!.entries.map { it.exerciseId })

        repo.restoreExercise(deleted)
        assertEquals(2, repo.data.value!!.exercises.size)
        assertEquals(2, repo.data.value!!.entries.size)
        assertNull(repo.deleteExercise("unknown"))
    }

    @Test
    fun deletingAnEntryCanBeUndone() = runTest {
        val repo = repository(FakeStorage())
        val exerciseId = repo.addExercise(ExerciseDraft("Curl"))
        val entryId = repo.saveEntry(null, exerciseId, day, listOf(WorkSet(10, 12.0)))
        val deleted = repo.deleteEntry(entryId)!!
        assertTrue(repo.data.value!!.entries.isEmpty())
        repo.restoreEntry(deleted)
        repo.restoreEntry(deleted)
        assertEquals(1, repo.data.value!!.entries.size)
    }

    @Test
    fun presetsStaySortedByDuration() = runTest {
        val repo = repository(FakeStorage())
        repo.addPreset(75, " Moyen ")
        repo.addPreset(2)
        val presets = repo.data.value!!.restPresets
        assertEquals(presets.map { it.seconds }.sorted(), presets.map { it.seconds })
        assertEquals("Moyen", presets.first { it.seconds == 75 }.label)
        assertEquals(WorkoutRepository.MIN_REST, presets.first().seconds)

        val id = presets.first { it.seconds == 75 }.id
        repo.updatePreset(id, 400, "Long")
        assertEquals(400, repo.data.value!!.restPresets.last().seconds)
        repo.deletePreset(id)
        assertTrue(repo.data.value!!.restPresets.none { it.id == id })
    }

    @Test
    fun archiveAndSettings() = runTest {
        val repo = repository(FakeStorage())
        val id = repo.addExercise(ExerciseDraft("Squat"))
        repo.setExerciseArchived(id, true)
        assertTrue(repo.data.value!!.exercise(id)!!.archived)
        repo.updateSettings { it.copy(weightStep = 1.0, themeMode = ThemeMode.DARK) }
        assertEquals(1.0, repo.data.value!!.settings.weightStep, 0.0)
        assertEquals(ThemeMode.DARK, repo.data.value!!.settings.themeMode)
        repo.updateExercise(id, ExerciseDraft("Gainage", MuscleGroup.ABS, usesWeight = false, measure = Measure.SECONDS))
        val updated = repo.data.value!!.exercise(id)!!
        assertEquals("Gainage", updated.name)
        assertEquals(Measure.SECONDS, updated.measure)
        assertTrue(updated.archived)
    }

    @Test
    fun unreadableFileIsKeptAside() = runTest {
        val storage = FakeStorage("{ this is not json")
        val repo = repository(storage)
        assertEquals(listOf("{ this is not json"), storage.unreadable)
        assertNotNull(repo.data.value)
    }

    @Test
    fun backupRoundTrip() = runTest {
        val repo = repository(FakeStorage())
        val id = repo.addExercise(ExerciseDraft("Développé couché", MuscleGroup.CHEST))
        repo.saveEntry(null, id, day, listOf(WorkSet(10, 60.0)))
        val backup = repo.exportBackup()
        assertTrue(backup.contains("\"2026-10-26\""))

        val other = repository(FakeStorage())
        val parsed = other.parseBackup(backup)
        other.replaceAll(parsed)
        assertEquals(repo.data.value!!.exercises, other.data.value!!.exercises)
        assertEquals(repo.data.value!!.entries, other.data.value!!.entries)

        try {
            other.parseBackup("[1, 2, 3]")
            fail("A random document must be rejected")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun sanitizeDropsBrokenData() {
        val raw = """
            {
              "exercises": [
                {"id": "a", "name": "Squat", "muscleGroup": "TENTACLES", "futureField": 3},
                {"id": "b", "name": "   "}
              ],
              "entries": [
                {"id": "1", "exerciseId": "a", "date": "2026-10-26", "sets": [{"reps": 5, "weight": 100.0}]},
                {"id": "2", "exerciseId": "b", "date": "2026-10-26", "sets": [{"reps": 5}]},
                {"id": "3", "exerciseId": "a", "date": "2026-10-27", "sets": [{"reps": 0}]}
              ],
              "restPresets": [{"id": "p", "seconds": 0}],
              "settings": {"weightStep": -1}
            }
        """.trimIndent()
        val data = WorkoutRepository.sanitize(AppJson.decode(raw))
        assertEquals(listOf("a"), data.exercises.map { it.id })
        // Unknown values fall back to defaults.
        assertEquals(MuscleGroup.OTHER, data.exercises.single().muscleGroup)
        assertEquals(listOf("1"), data.entries.map { it.id })
        assertTrue(data.restPresets.isEmpty())
        assertEquals(2.5, data.settings.weightStep, 0.0)
    }
}
