package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import java.text.Collator
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class SetStats(
    val sets: Int = 0,
    val reps: Int = 0,
    val volume: Double = 0.0,
    val maxWeight: Double = 0.0,
    val maxReps: Int = 0,
) {
    val isEmpty: Boolean get() = sets == 0
}

fun Iterable<WorkSet>.stats(): SetStats {
    var sets = 0
    var reps = 0
    var volume = 0.0
    var maxWeight = 0.0
    var maxReps = 0
    for (set in this) {
        sets++
        reps += set.reps
        volume += set.reps * set.weight
        maxWeight = max(maxWeight, set.weight)
        maxReps = max(maxReps, set.reps)
    }
    return SetStats(sets, reps, volume, maxWeight, maxReps)
}

fun List<WorkoutEntry>.setStats(): SetStats = flatMap { it.sets }.stats()

/** How two weeks of the same exercise are compared. */
enum class ProgressMetric { VOLUME, REPS }

enum class Trend { UP, SAME, DOWN }

data class Progress(val metric: ProgressMetric, val current: Double, val previous: Double) {
    val delta: Double get() = current - previous
    val ratio: Double? get() = if (previous > 0.0) delta / previous else null
    val trend: Trend
        get() = when {
            abs(delta) < 1e-9 -> Trend.SAME
            delta > 0 -> Trend.UP
            else -> Trend.DOWN
        }
}

/**
 * Compares two performances: total volume (reps x kg) for weighted exercises, total reps
 * (or seconds) otherwise. Returns null when one of them is empty.
 */
fun compare(exercise: Exercise, current: SetStats, previous: SetStats): Progress? {
    if (current.isEmpty || previous.isEmpty) return null
    return if (exercise.usesWeight && current.volume > 0.0 && previous.volume > 0.0) {
        Progress(ProgressMetric.VOLUME, current.volume, previous.volume)
    } else {
        Progress(ProgressMetric.REPS, current.reps.toDouble(), previous.reps.toDouble())
    }
}

/** Single number summarizing a performance, used for "best week" and sparklines. */
fun score(exercise: Exercise, stats: SetStats): Double =
    if (exercise.usesWeight && stats.volume > 0.0) stats.volume else stats.reps.toDouble()

private val frenchCollator: Collator = Collator.getInstance(Locale.FRENCH).apply { strength = Collator.SECONDARY }

/** Exercises are displayed by muscle group, then alphabetically. */
val exerciseOrder: Comparator<Exercise> =
    compareBy<Exercise> { it.muscleGroup.ordinal }.thenComparator { a, b -> frenchCollator.compare(a.name, b.name) }

/** Oldest first. */
val entryOrder: Comparator<WorkoutEntry> = compareBy<WorkoutEntry>({ it.date }, { it.createdAt })

// region Week view

/** The last week before the displayed one in which the exercise was done. */
data class ReferenceWeek(val weekStart: LocalDate, val weeksAgo: Int, val entries: List<WorkoutEntry>)

data class ExerciseWeekItem(
    val exercise: Exercise,
    val entries: List<WorkoutEntry>,
    val reference: ReferenceWeek?,
    val progress: Progress?,
) {
    val done: Boolean get() = entries.isNotEmpty()
}

data class WeekSummary(
    val activeDays: Int = 0,
    val exercisesDone: Int = 0,
    val sets: Int = 0,
    /** Reps of the exercises measured in repetitions (timed exercises are not counted). */
    val reps: Int = 0,
    val volume: Double = 0.0,
) {
    val isEmpty: Boolean get() = sets == 0
}

data class WeekOverview(
    val weekStart: LocalDate,
    val items: List<ExerciseWeekItem>,
    val summary: WeekSummary,
    val previousSummary: WeekSummary,
)

fun buildWeekOverview(data: AppData, weekStart: LocalDate): WeekOverview {
    val start = weekStart.startOfWeek()
    val byExercise = data.entries.groupBy { it.exerciseId }
    val items = data.exercises
        .filter { exercise -> !exercise.archived || byExercise[exercise.id].orEmpty().any { it.date.isInWeek(start) } }
        .sortedWith(exerciseOrder)
        .map { exercise ->
            val all = byExercise[exercise.id].orEmpty()
            val thisWeek = all.filter { it.date.isInWeek(start) }.sortedWith(entryOrder)
            val earlier = all.filter { it.date.isBefore(start) }
            val reference = earlier.maxByOrNull { it.date }?.let { last ->
                val referenceStart = last.date.startOfWeek()
                ReferenceWeek(
                    weekStart = referenceStart,
                    weeksAgo = weeksBetween(referenceStart, start),
                    entries = earlier.filter { it.date.isInWeek(referenceStart) }.sortedWith(entryOrder),
                )
            }
            val progress = reference?.let { compare(exercise, thisWeek.setStats(), it.entries.setStats()) }
            ExerciseWeekItem(exercise, thisWeek, reference, progress)
        }
    return WeekOverview(
        weekStart = start,
        items = items,
        summary = weekSummary(data, start),
        previousSummary = weekSummary(data, start.minusWeeks(1)),
    )
}

fun weekSummary(data: AppData, weekStart: LocalDate): WeekSummary {
    val entries = data.entries.filter { it.date.isInWeek(weekStart) }
    if (entries.isEmpty()) return WeekSummary()
    val measures = data.exercises.associate { it.id to it.measure }
    var sets = 0
    var reps = 0
    var volume = 0.0
    for (entry in entries) {
        val stats = entry.sets.stats()
        sets += stats.sets
        volume += stats.volume
        if (measures[entry.exerciseId] != Measure.SECONDS) reps += stats.reps
    }
    return WeekSummary(
        activeDays = entries.mapTo(HashSet()) { it.date }.size,
        exercisesDone = entries.mapTo(HashSet()) { it.exerciseId }.size,
        sets = sets,
        reps = reps,
        volume = volume,
    )
}

// endregion

// region Progress

data class WeekPoint(val weekStart: LocalDate, val value: Double)

/** Number of consecutive weeks with at least one entry, up to this week (or last week if nothing yet). */
fun weekStreak(entries: List<WorkoutEntry>, today: LocalDate): Int {
    val activeWeeks = entries.mapTo(HashSet()) { it.date.startOfWeek() }
    var week = today.startOfWeek()
    if (week !in activeWeeks) week = week.minusWeeks(1)
    var count = 0
    while (week in activeWeeks) {
        count++
        week = week.minusWeeks(1)
    }
    return count
}

/** Values of the [count] weeks ending with [lastWeek] (oldest first), 0 for weeks without entries. */
fun weeklySeries(
    entries: List<WorkoutEntry>,
    lastWeek: LocalDate,
    count: Int,
    value: (List<WorkoutEntry>) -> Double,
): List<WeekPoint> {
    val byWeek = entries.groupBy { it.date.startOfWeek() }
    val end = lastWeek.startOfWeek()
    return (count - 1 downTo 0).map { back ->
        val week = end.minusWeeks(back.toLong())
        WeekPoint(week, byWeek[week]?.let(value) ?: 0.0)
    }
}

enum class ChartMetric { VOLUME, MAX_WEIGHT, REPS, SETS }

fun chartMetrics(exercise: Exercise): List<ChartMetric> =
    if (exercise.usesWeight) {
        listOf(ChartMetric.VOLUME, ChartMetric.MAX_WEIGHT, ChartMetric.REPS)
    } else {
        listOf(ChartMetric.REPS, ChartMetric.SETS)
    }

fun metricValue(entries: List<WorkoutEntry>, metric: ChartMetric): Double {
    val stats = entries.setStats()
    return when (metric) {
        ChartMetric.VOLUME -> stats.volume
        ChartMetric.MAX_WEIGHT -> stats.maxWeight
        ChartMetric.REPS -> stats.reps.toDouble()
        ChartMetric.SETS -> stats.sets.toDouble()
    }
}

data class DatedSet(val set: WorkSet, val date: LocalDate)

data class ExerciseRecords(
    val heaviest: DatedSet? = null,
    val mostReps: DatedSet? = null,
    /** Best week by volume when [bestWeekByVolume], by total reps otherwise. */
    val bestWeek: WeekPoint? = null,
    val bestWeekByVolume: Boolean = false,
    val estimatedOneRepMax: Double? = null,
    val sessions: Int = 0,
) {
    val isEmpty: Boolean get() = sessions == 0
}

fun records(exercise: Exercise, entries: List<WorkoutEntry>): ExerciseRecords {
    if (entries.isEmpty()) return ExerciseRecords()
    val dated = entries.flatMap { entry -> entry.sets.map { DatedSet(it, entry.date) } }
    val heaviest = if (exercise.usesWeight) {
        dated.filter { it.set.weight > 0.0 }
            .maxWithOrNull(compareBy<DatedSet>({ it.set.weight }, { it.set.reps }))
    } else {
        null
    }
    val mostReps = dated.maxWithOrNull(compareBy<DatedSet>({ it.set.reps }, { it.set.weight }))
    // Weeks are compared on volume only when there is a load to compare.
    val byVolume = exercise.usesWeight && dated.any { it.set.weight > 0.0 }
    val bestWeek = entries.groupBy { it.date.startOfWeek() }
        .map { (week, weekEntries) ->
            val stats = weekEntries.setStats()
            WeekPoint(week, if (byVolume) stats.volume else stats.reps.toDouble())
        }
        .maxWithOrNull(compareBy<WeekPoint>({ it.value }, { it.weekStart }))
    val oneRepMax = if (exercise.usesWeight) {
        dated.asSequence()
            .filter { it.set.weight > 0.0 && it.set.reps in 1..12 }
            .maxOfOrNull { estimateOneRepMax(it.set) }
    } else {
        null
    }
    return ExerciseRecords(
        heaviest = heaviest,
        mostReps = mostReps,
        bestWeek = bestWeek,
        bestWeekByVolume = byVolume,
        estimatedOneRepMax = oneRepMax,
        sessions = entries.size,
    )
}

/** Epley formula. */
fun estimateOneRepMax(set: WorkSet): Double =
    if (set.reps <= 1) set.weight else set.weight * (1.0 + set.reps / 30.0)

data class ExerciseProgressItem(
    val exercise: Exercise,
    /** Scores of the last weeks, oldest first. */
    val spark: List<Double>,
    val lastDate: LocalDate?,
    val records: ExerciseRecords,
    /** Last week with data compared to the one before. */
    val progress: Progress?,
)

fun buildProgressItems(data: AppData, today: LocalDate, weeks: Int = 8): List<ExerciseProgressItem> {
    val byExercise = data.entries.groupBy { it.exerciseId }
    return data.exercises.sortedWith(exerciseOrder).map { exercise ->
        val entries = byExercise[exercise.id].orEmpty()
        val byWeek = entries.groupBy { it.date.startOfWeek() }.toSortedMap()
        val lastWeeks = byWeek.keys.toList().takeLast(2)
        val progress = if (lastWeeks.size == 2) {
            compare(exercise, byWeek.getValue(lastWeeks[1]).setStats(), byWeek.getValue(lastWeeks[0]).setStats())
        } else {
            null
        }
        ExerciseProgressItem(
            exercise = exercise,
            spark = weeklySeries(entries, today, weeks) { score(exercise, it.setStats()) }.map { it.value },
            lastDate = entries.maxOfOrNull { it.date },
            records = records(exercise, entries),
            progress = progress,
        )
    }
}

// endregion

// region History

data class HistoryWeek(val weekStart: LocalDate, val entries: List<WorkoutEntry>, val stats: SetStats)

/** Entries grouped by week, newest first. */
fun history(entries: List<WorkoutEntry>): List<HistoryWeek> =
    entries.groupBy { it.date.startOfWeek() }
        .toSortedMap(reverseOrder())
        .map { (week, weekEntries) ->
            HistoryWeek(week, weekEntries.sortedWith(entryOrder.reversed()), weekEntries.setStats())
        }

/** Most recent entry, used to pre-fill a new one. */
fun lastEntry(entries: List<WorkoutEntry>): WorkoutEntry? = entries.maxWithOrNull(entryOrder)

// endregion
