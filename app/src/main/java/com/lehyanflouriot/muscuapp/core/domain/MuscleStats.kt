package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import java.time.LocalDate

/** What a set is worth for a muscle its exercise works too, the main one counting fully. */
const val SECONDARY_SHARE = 0.5

/** The weeks over which an exercise, or a muscle, is said to progress. */
const val PROGRESS_WEEKS = 4L

/** The full weeks the sets per week of a muscle are an average of. */
const val AVERAGE_WEEKS = 4

/** The sets per week usually advised for a muscle to grow, as a guide. */
val ADVISED_WEEKLY_SETS = 10.0..20.0

/** How much of a set of [this] exercise counts for [muscle]: 1 for its main muscle, half for the others it works. */
fun Exercise.share(muscle: MuscleGroup): Double = when (muscle) {
    muscleGroup -> 1.0
    in secondaryMuscles -> SECONDARY_SHARE
    else -> 0.0
}

/** An exercise as seen from a muscle it works. */
data class MuscleExercise(
    val exercise: Exercise,
    /** Whether the muscle is its main one, or one it works too. */
    val main: Boolean,
    /** Its sets of the last full weeks, on average, as they count for the muscle. */
    val weeklySets: Double,
    /** Change of what a set of it is worth over [PROGRESS_WEEKS] weeks (0.12 for +12 %), null when not known. */
    val progress: Double?,
    val lastDate: LocalDate?,
)

data class MuscleStats(
    val muscle: MuscleGroup,
    /** Sets of each week, oldest first, those of the exercises it works too counted for half. */
    val weeks: List<WeekPoint>,
    /** Sets per week, on average over the last full weeks. */
    val averageSets: Double,
    val thisWeekSets: Double,
    /** Change of its main exercises over [PROGRESS_WEEKS] weeks, null when not known yet. */
    val progress: Double?,
    /** Its main exercises first, then those that work it too; the most done first. */
    val exercises: List<MuscleExercise>,
)

/**
 * Change of what a set of [exercise] is worth: in its last full week (or the one before) compared
 * to [PROGRESS_WEEKS] weeks earlier, or the closest week around it. Whole weeks compare the same
 * sessions, for an exercise done on several days. A set is worth its reps (or seconds), or its
 * reps × kg when it has a load both times. Null without both weeks.
 */
fun exerciseProgress(exercise: Exercise, entries: List<WorkoutEntry>, today: LocalDate): Double? {
    val byWeek = entries.groupBy { it.date.startOfWeek() }
    val lastFullWeek = today.startOfWeek().minusWeeks(1)
    val recent = listOf(lastFullWeek, lastFullWeek.minusWeeks(1)).firstOrNull { it in byWeek } ?: return null
    val earlier = listOf(PROGRESS_WEEKS, PROGRESS_WEEKS - 1, PROGRESS_WEEKS + 1)
        .map { recent.minusWeeks(it) }
        .firstOrNull { it in byWeek } ?: return null
    val now = byWeek.getValue(recent).setStats()
    val then = byWeek.getValue(earlier).setStats()
    if (now.isEmpty || then.isEmpty) return null
    val byVolume = exercise.usesWeight && now.volume > 0.0 && then.volume > 0.0
    val nowValue = (if (byVolume) now.volume else now.reps.toDouble()) / now.sets
    val thenValue = (if (byVolume) then.volume else then.reps.toDouble()) / then.sets
    return if (thenValue > 0.0) nowValue / thenValue - 1.0 else null
}

/** The full weeks the average is made of: the last [AVERAGE_WEEKS], since the first one trained. */
private fun averageWeeks(entries: List<WorkoutEntry>, today: LocalDate): List<LocalDate> {
    val thisWeek = today.startOfWeek()
    val first = entries.minOfOrNull { it.date }?.startOfWeek() ?: return listOf(thisWeek)
    return (1..AVERAGE_WEEKS).map { thisWeek.minusWeeks(it.toLong()) }.filter { it >= first }.ifEmpty { listOf(thisWeek) }
}

/**
 * The muscles worked by the exercises, with their sets of each of the last [weeks] weeks, and how
 * their exercises progress. Muscles no exercise works are left out.
 */
fun buildMuscleStats(data: AppData, today: LocalDate, weeks: Int = 12): List<MuscleStats> {
    val byExercise = data.entries.groupBy { it.exerciseId }
    val averaged = averageWeeks(data.entries, today)
    val thisWeek = today.startOfWeek()
    val progressOf = HashMap<String, Double?>()
    fun progress(exercise: Exercise) = progressOf.getOrPut(exercise.id) {
        exerciseProgress(exercise, byExercise[exercise.id].orEmpty(), today)
    }
    return MuscleGroup.muscles.mapNotNull { muscle ->
        val working = data.exercises.filter { it.share(muscle) > 0.0 }
            // An archived exercise only shows for the time it was done.
            .filter { !it.archived || byExercise[it.id].orEmpty().isNotEmpty() }
        if (working.isEmpty()) return@mapNotNull null
        fun setsOf(exercise: Exercise, week: LocalDate): Double =
            byExercise[exercise.id].orEmpty().filter { it.date.isInWeek(week) }.sumOf { it.sets.size } * exercise.share(muscle)
        val exercises = working.map { exercise ->
            MuscleExercise(
                exercise = exercise,
                main = exercise.muscleGroup == muscle,
                weeklySets = averaged.sumOf { setsOf(exercise, it) } / averaged.size,
                progress = progress(exercise),
                lastDate = byExercise[exercise.id]?.maxOfOrNull { it.date },
            )
        }.sortedWith(
            compareByDescending<MuscleExercise> { it.main }.thenByDescending { it.weeklySets }.thenBy(exerciseOrder) { it.exercise },
        )
        // The muscle progresses as its main exercises, each by its share of the sets.
        val measured = exercises.filter { it.main && it.progress != null && it.weeklySets > 0.0 }
        val progress = if (measured.isEmpty()) {
            null
        } else {
            measured.sumOf { it.progress!! * it.weeklySets } / measured.sumOf { it.weeklySets }
        }
        MuscleStats(
            muscle = muscle,
            weeks = (weeks - 1 downTo 0).map { back ->
                val week = thisWeek.minusWeeks(back.toLong())
                WeekPoint(week, working.sumOf { setsOf(it, week) })
            },
            averageSets = exercises.sumOf { it.weeklySets },
            thisWeekSets = working.sumOf { setsOf(it, thisWeek) },
            progress = progress,
            exercises = exercises,
        )
    }
}
