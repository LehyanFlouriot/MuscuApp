package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.core.model.WarmUp
import java.time.DayOfWeek
import java.time.LocalDate

/** Most sets, and most reps or seconds, a session can ask for an exercise. */
const val MAX_TARGET_SETS = 20
const val MAX_TARGET_REPS = 999
const val MAX_RIR = 10

/** Most minutes of mobility, and light sets, of the warm-up of a session. */
const val MAX_WARM_UP_MINUTES = 30
const val MAX_LIGHT_SETS = 5

/**
 * Adds the program the app comes with and its exercises, followed from the start of the week of
 * [today] when no other program is. An exercise the user already has under the same name is used
 * as it is, so that its history goes on. Done only once.
 */
internal fun AppData.withDefaultProgram(today: LocalDate, createdAt: Long): AppData {
    if (training.seeded) return this
    if (training.program(HypertrophyProgram.ID) != null) return copy(training = training.copy(seeded = true))
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
    val default = HypertrophyProgram.create(createdAt)
    val program = default.copy(
        days = default.days.map { day ->
            day.copy(exercises = day.exercises.map { it.copy(exerciseId = ids[it.exerciseId] ?: it.exerciseId) })
        },
    )
    return copy(
        // The exercises of the program are done again: they are not archived any more.
        exercises = exercises.map { if (it.id in reused) it.copy(archived = false) else it } + added,
        training = training.copy(
            programs = training.programs + program,
            schedule = training.schedule.ifEmpty { listOf(PlanPeriod(program.id, today.startOfWeek())) },
            seeded = true,
        ),
    )
}

// region Changes of a program, kept free of ids generation so that they read simply

fun TrainingProgram.updateDay(dayOfWeek: DayOfWeek, transform: (TrainingDay) -> TrainingDay): TrainingProgram =
    copy(days = DayOfWeek.entries.map { if (it == dayOfWeek) transform(day(it)) else day(it) })

/** Changes an exercise of a session. */
fun TrainingProgram.updateLine(lineId: String, transform: (SessionExercise) -> SessionExercise): TrainingProgram =
    copy(days = days.map { day -> day.copy(exercises = day.exercises.map { if (it.id == lineId) transform(it) else it }) })

fun TrainingProgram.removeLine(lineId: String): TrainingProgram =
    copy(days = days.map { day -> day.copy(exercises = day.exercises.filterNot { it.id == lineId }) })

/** Moves an exercise up (negative [delta]) or down in its session. */
fun TrainingProgram.moveLine(lineId: String, delta: Int): TrainingProgram = copy(
    days = days.map { day ->
        val index = day.exercises.indexOfFirst { it.id == lineId }
        if (index < 0) {
            day
        } else {
            val target = (index + delta).coerceIn(0, day.exercises.lastIndex)
            day.copy(exercises = day.exercises.toMutableList().apply { add(target, removeAt(index)) })
        }
    },
)

// endregion

// region Programs over time

/**
 * Changes a program without changing the days already lived: when workouts were noted on days
 * followed with it before [today], those days move to a copy of the program as it was, and the
 * changed program is followed from [today].
 */
internal fun AppData.changeTrainingProgram(
    programId: String,
    today: LocalDate,
    newId: () -> String,
    transform: (TrainingProgram) -> TrainingProgram,
): AppData {
    val program = training.program(programId)?.takeIf { it.versionOf == null } ?: return this
    val changed = transform(program)
    if (changed == program) return this
    val lived = training.schedule.livedPeriods(programId, today) { from, until ->
        entries.any { it.date >= from && it.date < until }
    }
    val programs = training.programs.map { if (it.id == programId) changed else it }
    if (lived.isEmpty()) return copy(training = training.copy(programs = programs))
    val kept = program.copy(id = newId(), versionOf = programId)
    return copy(
        training = training.copy(
            programs = programs + kept,
            schedule = training.schedule.keptAs(lived, programId, kept.id, today),
        ),
    )
}

/** Follows [programId] from [from] on, until the next period already planned. */
internal fun Training.startProgram(programId: String, from: LocalDate): Training {
    val program = program(programId)
    if (program == null || program.versionOf != null) return this
    return copy(schedule = schedule.starting(programId, from))
}

/** Cancels a start planned after [today]. */
internal fun Training.cancelStart(programId: String, from: LocalDate, today: LocalDate): Training =
    copy(schedule = schedule.withoutStart(programId, from, today))

// endregion

// region New programs

internal fun blankProgram(id: String, name: String, createdAt: Long): TrainingProgram =
    TrainingProgram(id = id, name = name, days = DayOfWeek.entries.map { TrainingDay(it) }, createdAt = createdAt)

/** A copy of [source] to start a new program from: the same sessions and advice. */
internal fun copiedProgram(source: TrainingProgram, id: String, name: String, createdAt: Long): TrainingProgram =
    source.copy(id = id, name = name, versionOf = null, createdAt = createdAt)

/** The session of [from] copied to the days [to], with exercises of their own. */
internal fun TrainingProgram.copyDay(from: DayOfWeek, to: Collection<DayOfWeek>, newId: () -> String): TrainingProgram {
    val source = day(from)
    return to.filter { it != from }.fold(this) { program, target ->
        program.updateDay(target) { source.copy(day = target, exercises = source.exercises.map { it.copy(id = newId()) }) }
    }
}

// endregion

// region Sanitizing

internal fun SetTarget.sanitized(): SetTarget {
    val low = min.coerceIn(1, MAX_TARGET_REPS)
    return SetTarget(
        sets = sets.coerceIn(1, MAX_TARGET_SETS),
        min = low,
        max = max.coerceIn(low, MAX_TARGET_REPS),
        per = per.trim(),
        rir = rir?.coerceIn(0, MAX_RIR),
    )
}

private fun sanitizeProgram(program: TrainingProgram, exerciseIds: Set<String>): TrainingProgram {
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
    return program.copy(
        name = program.name.trim().ifEmpty { "Programme" },
        days = days,
        notes = program.notes.filter { it.title.isNotBlank() || it.text.isNotBlank() }.distinctBy { it.id },
        warmUp = program.warmUp.sanitized(),
    )
}

internal fun WarmUp.sanitized(): WarmUp =
    copy(mobilityMinutes = mobilityMinutes.coerceIn(0, MAX_WARM_UP_MINUTES), lightSets = lightSets.coerceIn(0, MAX_LIGHT_SETS))

/** Drops what would break the sessions: unknown programs, exercises that do not exist any more, days twice... */
internal fun sanitizeTraining(training: Training, exerciseIds: Set<String>): Training {
    val programs = training.programs
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .map { sanitizeProgram(it, exerciseIds) }
    val ids = programs.mapTo(HashSet()) { it.id }
    // A copy kept for the past whose program is gone becomes a program of its own.
    val cleanPrograms = programs.map { if (it.versionOf != null && it.versionOf !in ids) it.copy(versionOf = null) else it }
    // One program per start day: the last one wins.
    val schedule = training.schedule
        .filter { it.planId in ids }
        .asReversed()
        .distinctBy { it.from }
        .merged()
    // A session is moved once, to a later day, and a day takes a single session.
    val moves = training.moves
        .filter { it.from < it.date }
        .distinctBy { it.date }
        .distinctBy { it.from }
        .sortedBy { it.date }
    // One time per day, which cannot end before it starts.
    val times = training.times
        .filter { it.startedAt > 0 }
        .map { if (it.lastSetAt < it.startedAt) it.copy(lastSetAt = 0) else it }
        .distinctBy { it.date }
        .sortedBy { it.date }
    return Training(programs = cleanPrograms, schedule = schedule, moves = moves, times = times, seeded = training.seeded)
}

// endregion
