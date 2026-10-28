package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SessionMove
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import java.time.LocalDate

// region Programs over time

/** The program followed on [date], as it was that day. */
fun Training.planAt(date: LocalDate): TrainingProgram? = schedule.periodAt(date)?.let { program(it.planId) }

fun Training.programOf(plan: TrainingProgram): TrainingProgram = plan.versionOf?.let(::program) ?: plan

/** The program the user sees for [date]: the copies keeping past days point to the program they come from. */
fun Training.programAt(date: LocalDate): TrainingProgram? = planAt(date)?.let(::programOf)

/** Where each program stands on [today]: the current one first, then the planned ones, then the others. */
fun Training.programInfos(today: LocalDate): List<ProgramInfo<TrainingProgram>> = programInfos(programs, schedule, today)

/** Whether a workout was noted on a day followed with [programId] (or one of its kept copies). */
fun AppData.trainingHistory(programId: String): Boolean =
    entries.any { entry -> training.planAt(entry.date)?.let { training.programOf(it).id } == programId }

// endregion

// region Sessions done on another day

/** The move of a session done on [date] instead of its own day. */
fun Training.moveTo(date: LocalDate): SessionMove? = moves.firstOrNull { it.date == date }

/** The move of the session planned on [date] to another day. */
fun Training.moveFrom(date: LocalDate): SessionMove? = moves.firstOrNull { it.from == date }

/** The day whose session of the program is done on [date]: [date] itself, unless a session was moved to it. */
fun Training.sessionDate(date: LocalDate): LocalDate = moveTo(date)?.from ?: date

/** How many days back a missed session can still be done another day. */
const val MISSED_SESSION_DAYS = 6

/**
 * The last session missed before [today] that can still be done today instead: today has no
 * session of its own, and nothing was noted since that session, which was not started. Null
 * when there is none.
 */
fun missedSession(data: AppData, today: LocalDate): SessionView? {
    val own = buildSession(data, today) ?: return null
    if (!own.isRest || own.movedTo != null) return null
    val trainedOn = data.entries.mapTo(HashSet()) { it.date }
    for (offset in 1L..MISSED_SESSION_DAYS) {
        val date = today.minusDays(offset)
        // A day trained ends the search: what was missed before it is over.
        if (date in trainedOn) return null
        val session = buildSession(data, date) ?: return null
        when {
            // Already moved once: it is not moved again.
            session.movedFrom != null -> return null
            session.movedTo == null && !session.isRest -> return session
        }
    }
    return null
}

// endregion

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
    /** The program followed that day, as it was then: null before the first one started. */
    val program: TrainingProgram?,
    val day: TrainingDay,
    val items: List<SessionItem>,
    /** The day the session was planned on, when it is done on [date] instead. */
    val movedFrom: LocalDate? = null,
    /** The day the session planned on [date] is done instead: [date] then has no session. */
    val movedTo: LocalDate? = null,
) {
    val doneCount: Int get() = items.count { it.done }

    val isRest: Boolean get() = items.isEmpty()
}

/**
 * The session planned on [date] by the program, or the one moved to that day, or null when no
 * program is followed that day.
 */
fun buildSession(data: AppData, date: LocalDate): SessionView? {
    val training = data.training
    val move = training.moveTo(date)
    val planned = move?.from ?: date
    val program = training.planAt(planned) ?: return null
    val day = program.day(planned.dayOfWeek)
    if (move == null) {
        // The session of the day is done on another day: it has nothing left to do here.
        training.moveFrom(date)?.let { away ->
            return SessionView(date, program, day.copy(exercises = emptyList()), emptyList(), movedTo = away.date)
        }
    }
    val byExercise = data.entries.groupBy { it.exerciseId }
    val items = day.exercises.mapNotNull { line ->
        val exercise = data.exercise(line.exerciseId) ?: return@mapNotNull null
        val entries = byExercise[exercise.id].orEmpty()
        val before = entries.filter { it.date < date }
        // The same session last week, or else the last time the exercise was done.
        val last = before.filter { training.sessionDate(it.date).dayOfWeek == planned.dayOfWeek }.maxWithOrNull(entryOrder)
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
    return SessionView(date, program, day, items, movedFrom = move?.from)
}

/**
 * The sessions of the week starting [weekStart], from Monday to Sunday, or null when no program is
 * followed that week. The days before the first program have no session.
 */
fun buildWeekSessions(data: AppData, weekStart: LocalDate): List<SessionView>? {
    val start = weekStart.startOfWeek()
    val week = (0L until 7L).map { offset ->
        val date = start.plusDays(offset)
        buildSession(data, date) ?: SessionView(date, null, TrainingDay(date.dayOfWeek), emptyList())
    }
    return week.takeIf { days -> days.any { it.program != null } }
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

/**
 * What a session asks at first for [exercise] when it is added: what the program asks for it on
 * another day, or else a usual range.
 */
fun defaultTarget(program: TrainingProgram, exercise: Exercise): SetTarget =
    program.days.firstNotNullOfOrNull { day -> day.exercises.firstOrNull { it.exerciseId == exercise.id }?.target }
        ?: if (exercise.measure == Measure.SECONDS) SetTarget(3, 30, 45) else SetTarget(3, 8, 12, rir = 2)

/** Whether all the sets asked were done at the top of the range. */
fun topReached(target: SetTarget, sets: List<WorkSet>): Boolean =
    sets.size >= target.sets && sets.all { it.reps >= target.max }

/** Seconds added each week to a timed exercise, as the steppers do. */
private const val SECONDS_STEP = 5
