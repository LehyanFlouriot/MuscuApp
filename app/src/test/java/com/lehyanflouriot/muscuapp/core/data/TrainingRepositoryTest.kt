package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.planAt
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.trainingHistory
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
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

    // A Tuesday.
    private val today = LocalDate.of(2026, 10, 27)
    private val monday = today.minusDays(1)
    private var ids = 0

    private val WorkoutRepository.training: Training get() = data.value!!.training

    private fun WorkoutRepository.session(date: LocalDate) = buildSession(data.value!!, date)!!

    private fun TestScope.repository(storage: FakeStorage = FakeStorage()): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(
            storage, backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today },
            builtInPrograms = listOf(HypertrophyProgram),
        )
            .also { it.load() }
    }

    @Test
    fun theProgramAndItsExercisesAreAddedOnce() = runTest {
        val storage = FakeStorage()
        val data = repository(storage).data.value!!
        assertEquals(HypertrophyProgram.exercises(createdAt = 1000L), data.exercises)
        assertEquals(listOf(HypertrophyProgram.create(createdAt = 1000L)), data.training.programs)
        // Followed from the start of the week.
        assertEquals(listOf(PlanPeriod(HypertrophyProgram.ID, monday)), data.training.schedule)
        assertTrue(data.training.seeded)

        // A deleted exercise does not come back, and leaves the program.
        val repo = repository(FakeStorage(storage.content))
        repo.deleteExercise("ex-rollout-serviette")
        val reloaded = repository(FakeStorage(AppJson.encode(repo.data.value!!))).data.value!!
        assertEquals(23, reloaded.exercises.size)
        val wednesday = reloaded.training.programs.single().days[2]
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
        val squats = data.training.programs.single().days.flatMap { it.exercises }.filter { it.target.per == "jambe" && it.target.sets != 3 }
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

    @Test
    fun changingTheProgramKeepsThePastAsItWas() = runTest {
        val repo = repository()
        val id = HypertrophyProgram.ID
        // Nothing done yet: the program simply changes.
        repo.updateTrainingProgram(id) { it.copy(name = "Poids du corps") }
        assertEquals(1, repo.training.programs.size)

        repo.saveEntry(null, "ex-pompes-deficit", monday, listOf(WorkSet(9), WorkSet(8), WorkSet(8), WorkSet(7)))
        // From today on, the push-ups of Monday are heavier, with longer rests.
        repo.updateSessionExercise(id, "mon-1", SetTarget(5, 5, 8, rir = 0), emphasis = true, note = "Lesté", restSeconds = 240)
        val training = repo.training
        assertEquals(2, training.programs.size)
        assertEquals(listOf(monday, today), training.schedule.map { it.from })
        // Last Monday keeps what was asked, and what was done; next Monday asks the new target.
        val lastMonday = repo.session(monday).items.first()
        assertEquals(SetTarget(4, 6, 12, rir = 1), lastMonday.line.target)
        assertTrue(lastMonday.done)
        val nextMonday = repo.session(monday.plusWeeks(1)).items.first()
        assertEquals(SetTarget(5, 5, 8, rir = 0), nextMonday.line.target)
        assertEquals(240, nextMonday.line.restSeconds)
        assertEquals("Lesté", nextMonday.line.note)
        assertEquals(id, training.planAt(today)!!.id)
        assertEquals(id, training.programAt(monday)!!.id)
        assertTrue(repo.data.value!!.trainingHistory(id))

        // A second change the same day does not copy the program again.
        repo.updateTrainingProgram(id) { it.removeLine("mon-6") }
        assertEquals(2, repo.training.programs.size)
        assertEquals(6, repo.session(monday).items.size)
        assertEquals(5, repo.session(monday.plusWeeks(1)).items.size)
        // The copy kept for the past never changes.
        val keptId = repo.training.planAt(monday)!!.id
        repo.updateTrainingProgram(keptId) { it.copy(name = "Autre") }
        assertEquals("Poids du corps", repo.training.program(keptId)!!.name)

        // The program is still followed since Monday.
        val info = repo.training.programInfos(today).single()
        assertEquals(id, info.plan.id)
        assertEquals(ProgramStatus.ACTIVE, info.status)
        assertEquals(monday, info.date)

        // Saved and read again as it is.
        val reloaded = repository(FakeStorage(AppJson.encode(repo.data.value!!)))
        assertEquals(repo.training, reloaded.training)
    }

    @Test
    fun sessionsCanBeShapedFreely() = runTest {
        val repo = repository()
        val id = HypertrophyProgram.ID
        // Thursday, a rest day, gets a session of its own.
        repo.addSessionExercise(id, DayOfWeek.THURSDAY, "ex-nordic-curl", SetTarget(3, 5, 8, rir = 2), note = "Lent")
        repo.addSessionExercise(id, DayOfWeek.THURSDAY, "unknown", SetTarget(3, 8))
        var thursday = repo.training.programs.single().day(DayOfWeek.THURSDAY)
        assertEquals(1, thursday.exercises.size)
        val line = thursday.exercises.single()
        assertEquals("ex-nordic-curl", line.exerciseId)
        assertEquals("Lent", line.note)
        assertTrue(line.id.isNotBlank())
        assertFalse(repo.session(today.plusDays(2)).isRest)

        // What is asked is kept within sensible bounds.
        repo.updateSessionExercise(id, line.id, SetTarget(50, 12, 8, " jambe ", rir = 15), emphasis = false, note = "", restSeconds = 2)
        thursday = repo.training.programs.single().day(DayOfWeek.THURSDAY)
        assertEquals(SetTarget(MAX_TARGET_SETS, 12, 12, "jambe", MAX_RIR), thursday.exercises.single().target)
        assertEquals(WorkoutRepository.MIN_REST, thursday.exercises.single().restSeconds)

        // A harder variant takes the place of an exercise, with the same target.
        repo.replaceSessionExercise(id, "tue-1", "ex-tractions-supination")
        val tuesday = repo.training.programs.single().day(DayOfWeek.TUESDAY)
        assertEquals("ex-tractions-supination", tuesday.exercises.first().exerciseId)
        assertEquals(SetTarget(4, 4, 10, rir = 1), tuesday.exercises.first().target)

        // Exercises move within their session, or leave it.
        repo.updateTrainingProgram(id) { it.moveLine("mon-6", -2) }
        repo.updateTrainingProgram(id) { it.moveLine("mon-1", -1) }
        repo.updateTrainingProgram(id) { it.removeLine("mon-2") }
        assertEquals(
            listOf("mon-1", "mon-3", "mon-6", "mon-4", "mon-5"),
            repo.training.programs.single().day(DayOfWeek.MONDAY).exercises.map { it.id },
        )

        // A whole session is copied to other days, with exercises of their own.
        repo.copyTrainingDay(id, DayOfWeek.MONDAY, listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY))
        val program = repo.training.programs.single()
        val sunday = program.day(DayOfWeek.SUNDAY)
        assertEquals("Pectoraux", sunday.title)
        assertEquals(program.day(DayOfWeek.MONDAY).exercises.map { it.exerciseId }, sunday.exercises.map { it.exerciseId })
        assertTrue(sunday.exercises.none { line -> program.day(DayOfWeek.MONDAY).exercises.any { it.id == line.id } })

        repo.addTrainingNote(id, " Sommeil ", "8 h")
        repo.addTrainingNote(id, "", " ")
        assertEquals(7, repo.training.programs.single().notes.size)
        assertEquals("Sommeil", repo.training.programs.single().notes.last().title)
    }

    @Test
    fun newProgramsStartOnTheChosenDay() = runTest {
        val repo = repository()
        val id = HypertrophyProgram.ID
        val nextMonday = monday.plusWeeks(1)
        val copyId = repo.createTrainingProgram("  Force ", copyOf = id, start = nextMonday)
        val blankId = repo.createTrainingProgram("", copyOf = null, start = null)
        var training = repo.training
        assertEquals("Force", training.program(copyId)!!.name)
        assertEquals("Programme", training.program(blankId)!!.name)
        assertTrue(training.program(blankId)!!.days.all { it.isRest })
        assertEquals(DayOfWeek.entries, training.program(blankId)!!.days.map { it.day })
        assertEquals(id, training.planAt(today)!!.id)
        assertEquals(copyId, training.planAt(nextMonday)!!.id)
        // The copy has the same sessions.
        assertEquals(training.program(id)!!.days, training.program(copyId)!!.days)
        val statuses = training.programInfos(today).associate { it.plan.id to it.status }
        assertEquals(ProgramStatus.ACTIVE, statuses[id])
        assertEquals(ProgramStatus.PLANNED, statuses[copyId])
        assertEquals(ProgramStatus.UNUSED, statuses[blankId])

        // A start can never be in the past.
        repo.startTrainingProgram(blankId, monday.minusWeeks(3))
        assertEquals(blankId, repo.training.planAt(today)!!.id)
        assertEquals(id, repo.training.planAt(monday)!!.id)
        assertTrue(repo.session(today).isRest)
        repo.startTrainingProgram(id, today)
        assertEquals(id, repo.training.planAt(today)!!.id)

        repo.cancelTrainingProgramStart(copyId, nextMonday)
        assertEquals(id, repo.training.planAt(nextMonday)!!.id)

        // A program can be deleted unless it is followed today or was followed on days with workouts.
        assertFalse(repo.deleteTrainingProgram(id))
        assertTrue(repo.deleteTrainingProgram(copyId))
        assertTrue(repo.deleteTrainingProgram(blankId))
        training = repo.training
        assertEquals(listOf(id), training.programs.map { it.id })
        assertEquals(listOf(monday), training.schedule.map { it.from })

        // Followed last week, on a day with a workout: it stays in the history.
        val strength = repo.createTrainingProgram("Force", copyOf = null, start = null)
        repo.replaceAll(
            repo.data.value!!.let { data ->
                data.copy(
                    entries = listOf(WorkoutEntry("w", "ex-nordic-curl", monday.minusDays(3), listOf(WorkSet(5)))),
                    training = data.training.copy(
                        schedule = listOf(PlanPeriod(strength, monday.minusWeeks(1)), PlanPeriod(id, monday)),
                    ),
                )
            },
        )
        assertEquals(ProgramStatus.PAST, repo.training.programInfos(today).single { it.plan.id == strength }.status)
        assertFalse(repo.deleteTrainingProgram(strength))
    }

    @Test
    fun theProgramOfVersion170IsFollowedSinceItCame() = runTest {
        // What 1.7.0 saved: the program on its own, always followed.
        val saved = AppData(
            schemaVersion = 2,
            exercises = HypertrophyProgram.exercises(),
            entries = listOf(WorkoutEntry("e", "ex-tractions-pronation", today, listOf(WorkSet(7)))),
            training = Training(legacyProgram = HypertrophyProgram.create(), seeded = true),
        )
        val tree = AppJson.tree(saved).jsonObject
        val training = JsonObject(tree.getValue("training").jsonObject.filterKeys { it == "program" || it == "seeded" })
        val storage = FakeStorage(JsonObject(tree + ("training" to training)).toString())

        val repo = repository(storage)
        val data = repo.data.value!!
        assertEquals(listOf(HypertrophyProgram.create()), data.training.programs)
        assertEquals(listOf(PlanPeriod(HypertrophyProgram.ID, LocalDate.of(2026, 10, 26))), data.training.schedule)
        assertNull(data.training.legacyProgram)
        assertEquals(24, data.exercises.size)
        assertEquals("Dos", repo.session(today).day.title)
        assertTrue(repo.session(today).items.first().done)
        // Written once in the new way.
        val written = AppJson.decode(storage.content!!)
        assertEquals(AppData.CURRENT_SCHEMA_VERSION, written.schemaVersion)
        assertEquals(data.training, written.training)
    }
}
