package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private val bench = Exercise(id = "bench", name = "Développé couché", muscleGroup = MuscleGroup.CHEST)
    private val pullUps = Exercise(id = "pull", name = "Tractions", muscleGroup = MuscleGroup.BACK, usesWeight = false)
    private val plank = Exercise(
        id = "plank", name = "Gainage", muscleGroup = MuscleGroup.ABS,
        usesWeight = false, measure = Measure.SECONDS,
    )
    private val curl = Exercise(id = "curl", name = "Curl", muscleGroup = MuscleGroup.BICEPS, archived = true)

    // Monday 26 October 2026.
    private val thisWeek = LocalDate.of(2026, 10, 26)
    private val lastWeek = thisWeek.minusWeeks(1)

    private var counter = 0L
    private fun entry(exercise: Exercise, date: LocalDate, vararg sets: WorkSet) =
        WorkoutEntry(id = "e${counter++}", exerciseId = exercise.id, date = date, sets = sets.toList(), createdAt = counter)

    @Test
    fun weekHelpers() {
        val sunday = LocalDate.of(2026, 11, 1)
        assertEquals(thisWeek, sunday.startOfWeek())
        assertEquals(sunday, thisWeek.endOfWeek())
        assertEquals(44, thisWeek.isoWeekNumber)
        assertTrue(sunday.isInWeek(thisWeek))
        assertFalse(sunday.plusDays(1).isInWeek(thisWeek))
        assertEquals(1, weeksBetween(lastWeek.plusDays(6), thisWeek))
        assertEquals(LocalDate.of(2026, 10, 27), defaultLogDate(thisWeek, LocalDate.of(2026, 10, 27)))
        // Looking at last week on a Tuesday: log on last week's Tuesday.
        assertEquals(LocalDate.of(2026, 10, 20), defaultLogDate(lastWeek, LocalDate.of(2026, 10, 27)))
    }

    @Test
    fun weekOverviewComparesWithThePreviousWeekDone() {
        val data = AppData(
            exercises = listOf(pullUps, bench, curl, plank),
            entries = listOf(
                entry(bench, thisWeek.minusWeeks(3), WorkSet(10, 50.0)),
                entry(bench, lastWeek.plusDays(2), WorkSet(8, 60.0), WorkSet(8, 60.0)),
                entry(bench, thisWeek.plusDays(1), WorkSet(10, 60.0), WorkSet(10, 60.0)),
                entry(pullUps, thisWeek.minusWeeks(2), WorkSet(8), WorkSet(8)),
                entry(pullUps, thisWeek, WorkSet(6), WorkSet(6)),
                entry(plank, thisWeek, WorkSet(45), WorkSet(45)),
            ),
        )
        val overview = buildWeekOverview(data, thisWeek.plusDays(3))
        assertEquals(thisWeek, overview.weekStart)
        // Sorted by muscle group; the archived exercise is hidden because it was not done this week.
        assertEquals(listOf("bench", "pull", "plank"), overview.items.map { it.exercise.id })

        val benchItem = overview.items[0]
        assertEquals(1, benchItem.entries.size)
        assertEquals(1, benchItem.reference?.weeksAgo)
        val benchProgress = benchItem.progress!!
        assertEquals(ProgressMetric.VOLUME, benchProgress.metric)
        assertEquals(1200.0, benchProgress.current, 0.0)
        assertEquals(960.0, benchProgress.previous, 0.0)
        assertEquals(Trend.UP, benchProgress.trend)
        assertEquals(0.25, benchProgress.ratio!!, 1e-9)

        val pullItem = overview.items[1]
        assertEquals(2, pullItem.reference?.weeksAgo)
        assertEquals(ProgressMetric.REPS, pullItem.progress?.metric)
        assertEquals(Trend.DOWN, pullItem.progress?.trend)

        assertNull(overview.items[2].reference)
        assertNull(overview.items[2].progress)

        val summary = overview.summary
        assertEquals(2, summary.activeDays)
        assertEquals(3, summary.exercisesDone)
        assertEquals(6, summary.sets)
        // Seconds of the plank are not counted as reps.
        assertEquals(32, summary.reps)
        assertEquals(1200.0, summary.volume, 0.0)
        assertEquals(2, overview.previousSummary.sets)
    }

    @Test
    fun archivedExerciseDoneThisWeekStaysVisible() {
        val data = AppData(exercises = listOf(curl), entries = listOf(entry(curl, thisWeek, WorkSet(12, 12.0))))
        assertEquals(listOf("curl"), buildWeekOverview(data, thisWeek).items.map { it.exercise.id })
        assertTrue(buildWeekOverview(data, lastWeek).items.isEmpty())
    }

    @Test
    fun noReferenceMeansNoProgress() {
        val data = AppData(exercises = listOf(bench), entries = listOf(entry(bench, thisWeek, WorkSet(5, 80.0))))
        val item = buildWeekOverview(data, thisWeek).items.single()
        assertTrue(item.done)
        assertNull(item.progress)
        assertFalse(buildWeekOverview(data, lastWeek).items.single().done)
    }

    @Test
    fun weightedExerciseWithoutLoadFallsBackToReps() {
        val progress = compare(bench, listOf(WorkSet(12)).stats(), listOf(WorkSet(10, 40.0)).stats())!!
        assertEquals(ProgressMetric.REPS, progress.metric)
        assertEquals(2.0, progress.delta, 0.0)
        assertNull(compare(bench, listOf<WorkSet>().stats(), listOf(WorkSet(10)).stats()))
    }

    @Test
    fun streakCountsConsecutiveWeeks() {
        val entries = listOf(
            entry(bench, thisWeek.minusWeeks(1), WorkSet(5, 80.0)),
            entry(bench, thisWeek.minusWeeks(2), WorkSet(5, 80.0)),
            entry(bench, thisWeek.minusWeeks(4), WorkSet(5, 80.0)),
        )
        // Nothing yet this week: the streak of the previous weeks is still alive.
        assertEquals(2, weekStreak(entries, thisWeek.plusDays(2)))
        assertEquals(3, weekStreak(entries + entry(bench, thisWeek, WorkSet(1, 1.0)), thisWeek))
        assertEquals(0, weekStreak(entries, thisWeek.plusWeeks(2)))
        assertEquals(0, weekStreak(emptyList(), thisWeek))
    }

    @Test
    fun weeklySeriesFillsMissingWeeks() {
        val entries = listOf(
            entry(bench, thisWeek, WorkSet(10, 50.0)),
            entry(bench, thisWeek.minusWeeks(2).plusDays(4), WorkSet(10, 40.0), WorkSet(10, 40.0)),
        )
        val series = weeklySeries(entries, thisWeek.plusDays(5), 4) { metricValue(it, ChartMetric.SETS) }
        assertEquals(listOf(thisWeek.minusWeeks(3), thisWeek.minusWeeks(2), lastWeek, thisWeek), series.map { it.weekStart })
        assertEquals(listOf(0.0, 2.0, 0.0, 1.0), series.map { it.value })
    }

    @Test
    fun recordsFindTheBestSets() {
        val entries = listOf(
            entry(bench, lastWeek, WorkSet(10, 60.0), WorkSet(6, 70.0)),
            entry(bench, thisWeek, WorkSet(5, 70.0), WorkSet(12, 50.0)),
        )
        val records = records(bench, entries)
        assertEquals(WorkSet(6, 70.0), records.heaviest?.set)
        assertEquals(lastWeek, records.heaviest?.date)
        assertEquals(WorkSet(12, 50.0), records.mostReps?.set)
        assertEquals(lastWeek, records.bestWeek?.weekStart)
        assertEquals(1020.0, records.bestWeek!!.value, 0.0)
        assertTrue(records.bestWeekByVolume)
        assertEquals(84.0, records.estimatedOneRepMax!!, 1e-9)
        assertEquals(2, records.sessions)

        val bodyWeight = records(pullUps, listOf(entry(pullUps, thisWeek, WorkSet(8), WorkSet(10))))
        assertNull(bodyWeight.heaviest)
        assertNull(bodyWeight.estimatedOneRepMax)
        assertEquals(10, bodyWeight.mostReps?.set?.reps)
        assertFalse(bodyWeight.bestWeekByVolume)
        assertEquals(18.0, bodyWeight.bestWeek!!.value, 0.0)
        assertTrue(records(bench, emptyList()).isEmpty)
    }

    @Test
    fun historyIsGroupedByWeekNewestFirst() {
        val entries = listOf(
            entry(bench, lastWeek, WorkSet(5, 80.0)),
            entry(bench, thisWeek, WorkSet(5, 80.0)),
            entry(bench, thisWeek.plusDays(2), WorkSet(5, 82.5)),
        )
        val weeks = history(entries)
        assertEquals(listOf(thisWeek, lastWeek), weeks.map { it.weekStart })
        assertEquals(listOf(thisWeek.plusDays(2), thisWeek), weeks[0].entries.map { it.date })
        assertEquals(2, weeks[0].stats.sets)
        assertEquals(thisWeek.plusDays(2), lastEntry(entries)?.date)
    }

    @Test
    fun progressItemsCompareTheLastTwoWeeksDone() {
        val data = AppData(
            exercises = listOf(bench, pullUps),
            entries = listOf(
                entry(bench, thisWeek.minusWeeks(5), WorkSet(10, 60.0)),
                entry(bench, thisWeek.minusWeeks(2), WorkSet(10, 65.0)),
            ),
        )
        val items = buildProgressItems(data, thisWeek, weeks = 4)
        val benchItem = items.first { it.exercise.id == "bench" }
        assertEquals(Trend.UP, benchItem.progress?.trend)
        assertEquals(listOf(0.0, 650.0, 0.0, 0.0), benchItem.spark)
        assertEquals(thisWeek.minusWeeks(2), benchItem.lastDate)
        val pullItem = items.first { it.exercise.id == "pull" }
        assertNull(pullItem.progress)
        assertNull(pullItem.lastDate)
    }
}
