package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import java.time.DayOfWeek

/** Most sets, and most reps or seconds, a session can ask for an exercise. */
private const val MAX_TARGET_SETS = 20
private const val MAX_TARGET_REPS = 999
private const val MAX_RIR = 10

/**
 * Adds the program the app comes with, and its exercises. An exercise the user already has under
 * the same name is used as it is, so that its history goes on. Done only once.
 */
internal fun AppData.withDefaultProgram(createdAt: Long): AppData {
    if (training.seeded) return this
    val byName = exercises.associateBy { ExerciseCatalog.normalize(it.name) }
    val ids = HashMap<String, String>()
    val reused = HashSet<String>()
    val added = HypertrophyProgram.exercises(createdAt).filter { exercise ->
        val same = byName[ExerciseCatalog.normalize(exercise.name)] ?: exercises.firstOrNull { it.id == exercise.id }
        if (same != null) {
            ids[exercise.id] = same.id
            reused += same.id
        }
        same == null
    }
    val default = HypertrophyProgram.create()
    val program = default.copy(
        days = default.days.map { day ->
            day.copy(exercises = day.exercises.map { it.copy(exerciseId = ids[it.exerciseId] ?: it.exerciseId) })
        },
    )
    return copy(
        // The exercises of the program are done again: they are not archived any more.
        exercises = exercises.map { if (it.id in reused) it.copy(archived = false) else it } + added,
        training = training.copy(program = training.program ?: program, seeded = true),
    )
}

private fun SetTarget.sanitized(): SetTarget {
    val low = min.coerceIn(1, MAX_TARGET_REPS)
    return SetTarget(
        sets = sets.coerceIn(1, MAX_TARGET_SETS),
        min = low,
        max = max.coerceIn(low, MAX_TARGET_REPS),
        per = per.trim(),
        rir = rir?.coerceIn(0, MAX_RIR),
    )
}

/** Drops what would break the sessions: exercises that do not exist any more, days twice... */
internal fun sanitizeTraining(training: Training, exerciseIds: Set<String>): Training {
    val program = training.program ?: return training
    val days = DayOfWeek.entries.map { dayOfWeek ->
        val day = program.days.firstOrNull { it.day == dayOfWeek } ?: TrainingDay(dayOfWeek)
        day.copy(
            exercises = day.exercises
                .filter { it.id.isNotBlank() && it.exerciseId in exerciseIds }
                .distinctBy { it.id }
                .map {
                    it.copy(
                        target = it.target.sanitized(),
                        restSeconds = it.restSeconds?.coerceIn(WorkoutRepository.MIN_REST, WorkoutRepository.MAX_REST),
                    )
                },
        )
    }
    return training.copy(
        program = program.copy(
            name = program.name.trim().ifEmpty { "Programme" },
            days = days,
            notes = program.notes.filter { it.title.isNotBlank() || it.text.isNotBlank() }.distinctBy { it.id },
        ),
    )
}
