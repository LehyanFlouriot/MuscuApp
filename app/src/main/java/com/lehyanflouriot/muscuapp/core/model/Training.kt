package com.lehyanflouriot.muscuapp.core.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/** What a session asks for an exercise: "4 × 6-12 · RIR 1", "2 × 10-15 / jambe · RIR 3". */
@Serializable
data class SetTarget(
    val sets: Int,
    /** Fewest reps of a set, or seconds for an exercise measured in seconds. */
    val min: Int,
    /** Most reps of a set: the top of the range. */
    val max: Int = min,
    /** What the reps are counted for, when it is not the whole body: "jambe". */
    val per: String = "",
    /** Reps left in reserve at the end of each set. */
    val rir: Int? = null,
)

/** An exercise of a session, with what the session asks for it. */
@Serializable
data class SessionExercise(
    val id: String,
    val exerciseId: String,
    val target: SetTarget,
    /** The exercise of the session that gets the most effort, and the longest rests. */
    val emphasis: Boolean = false,
    /** How to do it: "Mains sur 2 livres", "< 4 reps : négatives 5 s". */
    val note: String = "",
    /** Rest between two sets in this session, in seconds, when it differs from the exercise's. */
    val restSeconds: Int? = null,
)

/** What a day of the week holds: a session, or rest when it has no exercise. */
@Serializable
data class TrainingDay(
    @Serializable(with = DayOfWeekSerializer::class)
    val day: DayOfWeek,
    /** "Pectoraux", "Repos". */
    val title: String = "",
    /** "Poussée horizontale", "Récup.". */
    val focus: String = "",
    val exercises: List<SessionExercise> = emptyList(),
    /** Said about the day, for instance why it is a rest day. */
    val note: String = "",
) {
    val isRest: Boolean get() = exercises.isEmpty()
}

/** The sessions of a typical week, done again week after week. */
@Serializable
data class TrainingProgram(
    val id: String,
    val name: String,
    /** "5 séances full body · 2 jours de repos". */
    val info: String = "",
    /** One per day of the week, from Monday to Sunday. */
    val days: List<TrainingDay> = emptyList(),
    /** How to follow the program: warm-up, rests, progression... */
    val notes: List<PlanNote> = emptyList(),
) {
    fun day(day: DayOfWeek): TrainingDay = days.firstOrNull { it.day == day } ?: TrainingDay(day)
}

@Serializable
data class Training(
    /** The program followed, if any. */
    val program: TrainingProgram? = null,
    /** Whether the program the app comes with was already added: it is never added again. */
    val seeded: Boolean = false,
)
