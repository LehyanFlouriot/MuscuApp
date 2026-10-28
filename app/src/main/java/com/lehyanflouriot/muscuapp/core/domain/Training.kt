package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import java.time.LocalDate

/** An exercise of a session: what the program asks, and what was done that day. */
data class SessionItem(
    val line: SessionExercise,
    val exercise: Exercise,
    /** What was noted for the exercise on the day of the session. */
    val entries: List<WorkoutEntry>,
    /** The last time it was done before, on the same day of the week when there is one. */
    val last: WorkoutEntry?,
    /** What to aim for this time. */
    val goal: List<WorkSet>,
    /** Every set reached the top of the range last time: time for a harder variant. */
    val topReached: Boolean,
) {
    val done: Boolean get() = entries.isNotEmpty()
}

/** The session of the program planned on [date], and how far it went. */
data class SessionView(
    val date: LocalDate,
    val day: TrainingDay,
    val items: List<SessionItem>,
) {
    val doneCount: Int get() = items.count { it.done }

    val isRest: Boolean get() = items.isEmpty()
}

/** The session planned on [date] by the program, or null when no program is followed. */
fun buildSession(data: AppData, date: LocalDate): SessionView? {
    val program = data.training.program ?: return null
    val day = program.day(date.dayOfWeek)
    val byExercise = data.entries.groupBy { it.exerciseId }
    val items = day.exercises.mapNotNull { line ->
        val exercise = data.exercise(line.exerciseId) ?: return@mapNotNull null
        val entries = byExercise[exercise.id].orEmpty()
        val before = entries.filter { it.date < date }
        // The same session last week, or else the last time the exercise was done.
        val last = before.filter { it.date.dayOfWeek == date.dayOfWeek }.maxWithOrNull(entryOrder)
            ?: before.maxWithOrNull(entryOrder)
        SessionItem(
            line = line,
            exercise = exercise,
            entries = entries.filter { it.date == date }.sortedWith(entryOrder),
            last = last,
            goal = sessionGoal(line.target, last?.sets, exercise.measure),
            topReached = last != null && topReached(line.target, last.sets),
        )
    }
    return SessionView(date, day, items)
}

/** The sessions of the week starting [weekStart], from Monday to Sunday, or null without a program. */
fun buildWeekSessions(data: AppData, weekStart: LocalDate): List<SessionView>? {
    if (data.training.program == null) return null
    val start = weekStart.startOfWeek()
    return (0L until 7L).mapNotNull { buildSession(data, start.plusDays(it)) }
}

/**
 * What to aim for after [last]: one more rep on each set than last time, within the range of
 * [target] ("+1 rep par série chaque semaine"), and the same load. Without history, the
 * bottom of the range.
 */
fun sessionGoal(target: SetTarget, last: List<WorkSet>?, measure: Measure): List<WorkSet> {
    if (last.isNullOrEmpty()) return List(target.sets) { WorkSet(target.min) }
    val step = if (measure == Measure.SECONDS) SECONDS_STEP else 1
    return List(target.sets) { index ->
        val previous = last.getOrElse(index) { last.last() }
        WorkSet(reps = (previous.reps + step).coerceIn(target.min, target.max), weight = previous.weight)
    }
}

/** Whether all the sets asked were done at the top of the range. */
fun topReached(target: SetTarget, sets: List<WorkSet>): Boolean =
    sets.size >= target.sets && sets.all { it.reps >= target.max }

/** Seconds added each week to a timed exercise, as the steppers do. */
private const val SECONDS_STEP = 5
