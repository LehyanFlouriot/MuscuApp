package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import java.time.DayOfWeek

/** A training program the app comes with, made of exercises it adds to the user's. */
interface BuiltInProgram {
    val id: String

    /** The exercises of the program, as they are added to the app. */
    fun exercises(createdAt: Long = 0L): List<Exercise>

    /** The program, made of the exercises of [exercises]. */
    fun create(createdAt: Long = 0L): TrainingProgram
}

/**
 * The programs the app comes with, in the order they came: new data gets the last one, and data
 * that had the ones before gets each one that came after them.
 */
val BUILT_IN_PROGRAMS: List<BuiltInProgram> = listOf(HypertrophyProgram, NoEquipmentProgram)

/** Rests of the exercise of emphasis of a session, which gets the most effort, and of the others. */
private const val EMPHASIS_REST = 180
private const val REST = 90

/** An exercise of a program the app comes with. */
internal class Move(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val usesWeight: Boolean = false,
    val measure: Measure = Measure.REPS,
    /** The exercise of emphasis of a session: it rests longer. */
    val emphasis: Boolean = false,
    val secondary: List<MuscleGroup> = emptyList(),
)

internal fun List<Move>.toExercises(createdAt: Long): List<Exercise> = map { move ->
    Exercise(
        id = move.id,
        name = move.name,
        muscleGroup = move.group,
        usesWeight = move.usesWeight,
        measure = move.measure,
        restSeconds = if (move.emphasis) EMPHASIS_REST else REST,
        createdAt = createdAt,
        secondaryMuscles = move.secondary,
    )
}

/** What a session asks for a move. */
internal class Line(val move: Move, val target: SetTarget, val emphasis: Boolean = false, val note: String = "")

internal fun line(
    move: Move,
    sets: Int,
    min: Int,
    max: Int = min,
    rir: Int? = null,
    per: String = "",
    emphasis: Boolean = false,
    note: String = "",
) = Line(move, SetTarget(sets, min, max, per, rir), emphasis, note)

/** The session of [day], its exercises numbered after the day: "mon-1", "mon-2"... */
internal fun session(day: DayOfWeek, title: String, focus: String, vararg lines: Line): TrainingDay {
    val code = day.name.take(3).lowercase()
    return TrainingDay(
        day = day,
        title = title,
        focus = focus,
        exercises = lines.mapIndexed { index, line ->
            SessionExercise(
                id = "$code-${index + 1}",
                exerciseId = line.move.id,
                target = line.target,
                emphasis = line.emphasis,
                note = line.note,
                restSeconds = if (line.emphasis) EMPHASIS_REST else REST,
            )
        },
    )
}

internal fun restDay(day: DayOfWeek, note: String) = TrainingDay(day, "Repos", "Récup.", note = note)
