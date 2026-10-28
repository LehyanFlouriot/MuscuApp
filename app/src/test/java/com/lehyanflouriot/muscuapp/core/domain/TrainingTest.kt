package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.data.HypertrophyProgram
import com.lehyanflouriot.muscuapp.core.data.updateDay
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TrainingTest {
    private val program = HypertrophyProgram.create()
    private val exercises = HypertrophyProgram.exercises()

    // Tuesday 27 October 2026.
    private val tuesday = LocalDate.of(2026, 10, 27)
    private val monday = tuesday.minusDays(1)

    private infix fun TrainingProgram.from(date: LocalDate) = this to date

    /** The programs followed one after the other, each from its day. */
    private fun followed(vararg periods: Pair<TrainingProgram, LocalDate>) = Training(
        programs = periods.map { it.first }.distinctBy { it.id },
        schedule = periods.map { (program, from) -> PlanPeriod(program.id, from) },
        seeded = true,
    )

    private fun String.plain() = replace(' ', ' ')

    private fun entry(id: String, exerciseId: String, date: LocalDate, vararg reps: Int, weight: Double = 0.0) =
        WorkoutEntry(id, exerciseId, date, reps.map { WorkSet(it, weight) })

    @Test
    fun theProgramIsTheOneOfThePdf() {
        assertEquals(DayOfWeek.entries, program.days.map { it.day })
        assertEquals(
            listOf("Pectoraux", "Dos", "Quadriceps", "Repos", "Épaules", "Ischios & fessiers", "Repos"),
            program.days.map { it.title },
        )
        assertEquals(listOf(6, 5, 6, 0, 5, 5, 0), program.days.map { it.exercises.size })
        // One exercise of emphasis per session, always the first one.
        program.days.filterNot { it.isRest }.forEach { day ->
            assertEquals(listOf(true), day.exercises.map { it.emphasis }.distinct().take(1))
            assertEquals(1, day.exercises.count { it.emphasis })
        }
        assertEquals(24, exercises.size)
        assertEquals(exercises.size, exercises.map { it.name }.toSet().size)
        val ids = exercises.map { it.id }.toSet()
        assertTrue(program.days.flatMap { it.exercises }.all { it.exerciseId in ids })
        // The same exercise comes back on several days, with what each session asks.
        val squats = program.days.flatMap { day -> day.exercises.filter { it.exerciseId == "ex-squat-bulgare" }.map { day.day to it.target } }
        assertEquals(
            listOf(DayOfWeek.MONDAY to SetTarget(2, 10, 15, "jambe", 3), DayOfWeek.WEDNESDAY to SetTarget(4, 8, 15, "jambe", 1)),
            squats,
        )
        assertEquals(3, program.days.count { day -> day.exercises.any { it.exerciseId == "ex-mollets-unilateraux" } })
        // 2 to 3 min of rest on the exercise of emphasis, 60 to 90 s on the others.
        assertEquals(listOf(180, 90), program.days[2].exercises.take(2).map { it.restSeconds })
        assertEquals(listOf(90), program.days[0].exercises.filter { it.exerciseId == "ex-squat-bulgare" }.map { it.restSeconds })
        assertEquals(6, program.notes.size)
    }

    @Test
    fun exercisesAreSetUpForTheirSessions() {
        val byName = exercises.associateBy { it.name }
        assertEquals(Measure.SECONDS, byName.getValue("Hollow body hold").measure)
        assertEquals(setOf("Soulevé de terre roumain unilatéral", "Rowing australien lesté"), exercises.filter { it.usesWeight }.map { it.name }.toSet())
        // Long rests on the exercises of emphasis.
        assertEquals(180, byName.getValue("Nordic curl").restSeconds)
        assertEquals(180, byName.getValue("Squat bulgare").restSeconds)
        assertEquals(90, byName.getValue("Rollout serviette").restSeconds)
    }

    @Test
    fun targetsAreWrittenAsInThePdf() {
        assertEquals("4 × 6–12 · RIR 1", Fmt.target(SetTarget(4, 6, 12, rir = 1), Measure.REPS).plain())
        assertEquals("2 × 10–15 / jambe · RIR 3", Fmt.target(SetTarget(2, 10, 15, "jambe", 3), Measure.REPS).plain())
        assertEquals("3 × 30–45 s", Fmt.target(SetTarget(3, 30, 45), Measure.SECONDS).plain())
        assertEquals("3 × 12 / jambe · RIR 3", Fmt.target(SetTarget(3, 12, 12, "jambe", 3), Measure.REPS).plain())
    }

    @Test
    fun anAddedExerciseAsksWhatItAsksElsewhere() {
        val byId = exercises.associateBy { it.id }
        assertEquals(SetTarget(3, 12, 20, rir = 1), defaultTarget(program, byId.getValue("ex-mollets-unilateraux")))
        assertEquals(SetTarget(3, 8, 12, rir = 2), defaultTarget(program, Exercise("x", "Dips")))
        assertEquals(SetTarget(3, 30, 45), defaultTarget(program, Exercise("y", "Planche", measure = Measure.SECONDS)))
    }

    @Test
    fun theGoalIsOneMoreRepOnEachSet() {
        val target = SetTarget(4, 6, 12, rir = 1)
        // The first time, the bottom of the range.
        assertEquals(List(4) { WorkSet(6) }, sessionGoal(target, null, Measure.REPS))
        assertEquals(
            listOf(WorkSet(9), WorkSet(9), WorkSet(8), WorkSet(7)),
            sessionGoal(target, listOf(WorkSet(8), WorkSet(8), WorkSet(7), WorkSet(6)), Measure.REPS),
        )
        // Never under the bottom nor over the top of the range, with as many sets as asked.
        assertEquals(List(4) { WorkSet(6) }, sessionGoal(target, listOf(WorkSet(3)), Measure.REPS))
        assertEquals(List(4) { WorkSet(12) }, sessionGoal(target, List(4) { WorkSet(12) }, Measure.REPS))
        assertTrue(topReached(target, List(4) { WorkSet(12) }))
        assertFalse(topReached(target, List(3) { WorkSet(12) }))
        assertFalse(topReached(target, listOf(WorkSet(12), WorkSet(12), WorkSet(12), WorkSet(11))))
        // Seconds go up 5 by 5, and the load stays.
        assertEquals(List(3) { WorkSet(40) }, sessionGoal(SetTarget(3, 30, 45), List(3) { WorkSet(35) }, Measure.SECONDS))
        assertEquals(List(3) { WorkSet(45) }, sessionGoal(SetTarget(3, 30, 45), List(3) { WorkSet(43) }, Measure.SECONDS))
        assertEquals(List(3) { WorkSet(9, 5.0) }, sessionGoal(SetTarget(3, 8, 12, rir = 1), List(3) { WorkSet(8, 5.0) }, Measure.REPS))
    }

    @Test
    fun theSessionOfADayShowsItsGoalsAndWhatWasDone() {
        val wednesday = tuesday.plusDays(1)
        val lastWednesday = wednesday.minusWeeks(1)
        val data = AppData(
            exercises = exercises,
            training = followed(program from monday),
            entries = listOf(
                entry("a", "ex-tractions-pronation", tuesday.minusWeeks(1), 7, 7, 6, 5),
                entry("b", "ex-tractions-pronation", tuesday, 8, 7, 7, 6),
                entry("c", "ex-squat-bulgare", lastWednesday, 10, 10, 9, 9),
                // Monday's squats are lighter: Wednesday's goal follows last Wednesday.
                entry("d", "ex-squat-bulgare", wednesday.minusDays(2), 14, 14),
            ),
        )
        val session = buildSession(data, tuesday)!!
        assertEquals("Dos", session.day.title)
        assertEquals(5, session.items.size)
        val tractions = session.items.first()
        assertTrue(tractions.line.emphasis)
        assertTrue(tractions.done)
        assertEquals("b", tractions.entries.single().id)
        assertEquals("a", tractions.last?.id)
        assertEquals(listOf(8, 8, 7, 6), tractions.goal.map { it.reps })
        assertEquals(1, session.doneCount)
        // Never done: the bottom of the range.
        assertEquals(List(3) { WorkSet(8) }, session.items[1].goal)

        val squat = buildSession(data, wednesday)!!.items.first()
        assertEquals("c", squat.last?.id)
        assertEquals(listOf(11, 11, 10, 10), squat.goal.map { it.reps })
        assertFalse(squat.done)

        assertTrue(buildSession(data, tuesday.plusDays(2))!!.isRest)
        // An exercise deleted from the app leaves the session.
        val withoutNordic = data.copy(exercises = exercises.filterNot { it.id == "ex-nordic-curl" })
        assertEquals(4, buildSession(withoutNordic, tuesday.plusDays(4))!!.items.size)
        assertNull(buildSession(data.copy(training = Training()), tuesday))
    }

    @Test
    fun eachDayFollowsTheProgramOfItsTime() {
        val strength = program.copy(id = "force", name = "Force").updateDay(DayOfWeek.THURSDAY) { day ->
            day.copy(title = "Tractions", exercises = listOf(SessionExercise("thu-1", "ex-tractions-pronation", SetTarget(5, 3, 5))))
        }
        val wednesday = monday.plusDays(2)
        val data = AppData(exercises = exercises, training = followed(program from wednesday, strength from monday.plusDays(3)))

        // Before the first program, there is no session; the week it starts, its first days have none.
        assertNull(buildWeekSessions(data, monday.minusWeeks(1)))
        val week = buildWeekSessions(data, monday)!!
        assertEquals(DayOfWeek.entries, week.map { it.date.dayOfWeek })
        assertEquals(listOf(null, null, program.id, "force", "force", "force", "force"), week.map { it.program?.id })
        assertTrue(week[0].isRest)
        assertEquals("Quadriceps", week[2].day.title)
        // The Thursday rest of the first program became a session in the second one.
        assertEquals("Tractions", week[3].day.title)
        assertEquals(listOf("ex-tractions-pronation"), week[3].items.map { it.exercise.id })
        assertEquals("force", buildSession(data, monday.plusWeeks(5))!!.program!!.id)

        val infos = data.training.programInfos(tuesday).associateBy { it.plan.id }
        assertEquals(ProgramStatus.PLANNED, infos.getValue(program.id).status)
        assertEquals(wednesday, infos.getValue(program.id).date)
        assertEquals(ProgramStatus.PLANNED, infos.getValue("force").status)
        val later = data.training.programInfos(monday.plusWeeks(1)).associateBy { it.plan.id }
        assertEquals(ProgramStatus.ACTIVE, later.getValue("force").status)
        assertEquals(ProgramStatus.PAST, later.getValue(program.id).status)
        assertEquals(wednesday, later.getValue(program.id).date)
    }

    @Test
    fun theCopiesKeptForThePastShowAsTheirProgram() {
        val kept = program.copy(id = "kept", versionOf = program.id)
        val changed = program.updateDay(DayOfWeek.MONDAY) { it.copy(title = "Pecs") }
        val training = Training(
            programs = listOf(changed, kept),
            schedule = listOf(PlanPeriod("kept", monday), PlanPeriod(program.id, tuesday)),
            seeded = true,
        )
        assertEquals("Pectoraux", training.planAt(monday)!!.day(DayOfWeek.MONDAY).title)
        assertEquals("Pecs", training.planAt(monday.plusWeeks(1))!!.day(DayOfWeek.MONDAY).title)
        assertEquals(program.id, training.programAt(monday)!!.id)
        // Only the program is shown, followed since the first of its days.
        val info = training.programInfos(tuesday).single()
        assertEquals(program.id, info.plan.id)
        assertEquals(ProgramStatus.ACTIVE, info.status)
        assertEquals(monday, info.date)

        val data = AppData(exercises = exercises, training = training)
        assertFalse(data.trainingHistory(program.id))
        val trained = data.copy(entries = listOf(entry("a", "ex-pompes-deficit", monday, 8)))
        assertTrue(trained.trainingHistory(program.id))
        assertFalse(trained.trainingHistory("kept"))
    }
}
