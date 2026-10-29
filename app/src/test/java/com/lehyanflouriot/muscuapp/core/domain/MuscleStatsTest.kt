package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MuscleStatsTest {
    // Wednesday of the week of Monday 26 October.
    private val today = LocalDate.of(2026, 10, 28)
    private val thisMonday = LocalDate.of(2026, 10, 26)

    private val pushUps = Exercise(
        id = "pompes", name = "Pompes", muscleGroup = MuscleGroup.CHEST, usesWeight = false,
        secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
    )
    private val dips = Exercise(
        id = "dips", name = "Dips", muscleGroup = MuscleGroup.TRICEPS, usesWeight = false, secondaryMuscles = listOf(MuscleGroup.CHEST),
    )
    private val curl = Exercise(id = "curl", name = "Curl", muscleGroup = MuscleGroup.BICEPS)
    private val squat = Exercise(id = "squat", name = "Squat", muscleGroup = MuscleGroup.QUADS, secondaryMuscles = listOf(MuscleGroup.GLUTES))

    private var ids = 0

    private fun entry(exercise: Exercise, weeksAgo: Long, vararg sets: WorkSet) =
        WorkoutEntry(id = "e${ids++}", exerciseId = exercise.id, date = thisMonday.minusWeeks(weeksAgo), sets = sets.toList())

    private fun sets(count: Int, reps: Int, weight: Double = 0.0) = Array(count) { WorkSet(reps, weight) }

    private val data = AppData(
        exercises = listOf(pushUps, dips, curl, squat),
        entries = listOf(
            entry(pushUps, 5, *sets(3, 10)),
            entry(pushUps, 1, *sets(3, 12)),
            // This week is not over: it is not compared yet.
            entry(pushUps, 0, *sets(3, 20)),
            entry(dips, 5, *sets(3, 8)),
            entry(dips, 1, *sets(3, 8)),
            entry(squat, 5, *sets(3, 10, 50.0)),
            entry(squat, 1, *sets(3, 10, 55.0)),
            entry(curl, 1, *sets(3, 12, 10.0)),
        ),
    )

    private fun List<MuscleStats>.of(muscle: MuscleGroup) = single { it.muscle == muscle }

    @Test
    fun aSetCountsFullyForTheMainMuscleAndHalfForTheOthers() {
        val stats = buildMuscleStats(data, today)
        val chest = stats.of(MuscleGroup.CHEST)
        // Last week: 3 sets of push-ups, and 3 of dips which count for half.
        assertEquals(4.5, chest.weeks[chest.weeks.lastIndex - 1].value, 1e-9)
        assertEquals(3.0, chest.thisWeekSets, 1e-9)
        // On average over the 4 last full weeks.
        assertEquals(4.5 / 4, chest.averageSets, 1e-9)
        assertEquals(listOf(pushUps, dips), chest.exercises.map { it.exercise })
        assertEquals(listOf(true, false), chest.exercises.map { it.main })
        // A muscle no exercise works is left out.
        assertEquals(
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.QUADS, MuscleGroup.GLUTES),
            stats.map { it.muscle },
        )
    }

    @Test
    fun aMuscleProgressesAsItsMainExercises() {
        val stats = buildMuscleStats(data, today)
        // 12 reps a set instead of 10, four weeks earlier.
        assertEquals(0.2, stats.of(MuscleGroup.CHEST).progress!!, 1e-9)
        // The dips stay at 8: the push-ups, which only work the triceps too, do not count.
        assertEquals(0.0, stats.of(MuscleGroup.TRICEPS).progress!!, 1e-9)
        // 55 kg instead of 50 for the same reps.
        assertEquals(0.1, stats.of(MuscleGroup.QUADS).progress!!, 1e-9)
        // Too new to compare, or worked by no main exercise.
        assertNull(stats.of(MuscleGroup.BICEPS).progress)
        assertNull(stats.of(MuscleGroup.GLUTES).progress)
    }

    @Test
    fun theWeekComparedIsTheClosestToFourWeeksBefore() {
        val entries = listOf(
            entry(pushUps, 2, *sets(3, 9)),
            entry(pushUps, 5, *sets(3, 10)),
            entry(pushUps, 8, *sets(3, 6)),
        )
        // Nothing last week: the week before is compared to three weeks earlier.
        assertEquals(-0.1, exerciseProgress(pushUps, entries, today)!!, 1e-9)
        assertNull(exerciseProgress(pushUps, entries.take(1), today))
    }

    @Test
    fun theAverageOfANewcomerIsMadeOfTheWeeksTrained() {
        val newcomer = AppData(exercises = listOf(pushUps), entries = listOf(entry(pushUps, 1, *sets(4, 10)), entry(pushUps, 0, *sets(2, 10))))
        // Started last week: its only full week.
        assertEquals(4.0, buildMuscleStats(newcomer, today).of(MuscleGroup.CHEST).averageSets, 1e-9)
        val firstWeek = AppData(exercises = listOf(pushUps), entries = listOf(entry(pushUps, 0, *sets(2, 10))))
        assertEquals(2.0, buildMuscleStats(firstWeek, today).of(MuscleGroup.CHEST).averageSets, 1e-9)
    }
}
