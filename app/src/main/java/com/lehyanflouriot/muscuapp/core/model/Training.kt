package com.lehyanflouriot.muscuapp.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

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

/** What is done before each session of a program: mobility, then light sets of the exercise of emphasis. */
@Serializable
data class WarmUp(
    val enabled: Boolean = true,
    /** Minutes of mobility, 0 for none. */
    val mobilityMinutes: Int = 5,
    /** Light sets of the exercise of emphasis, 0 for none. They are not noted. */
    val lightSets: Int = 2,
) {
    /** Whether there is anything to do before the session. */
    val isActive: Boolean get() = enabled && (mobilityMinutes > 0 || lightSets > 0)
}

/** The sessions of a typical week, done again week after week until another program takes over. */
@Serializable
data class TrainingProgram(
    override val id: String,
    override val name: String,
    /** "5 séances full body · 2 jours de repos". */
    val info: String = "",
    /** One per day of the week, from Monday to Sunday. */
    val days: List<TrainingDay> = emptyList(),
    /** How to follow the program: warm-up, rests, progression... */
    val notes: List<PlanNote> = emptyList(),
    override val versionOf: String? = null,
    val createdAt: Long = 0L,
    val warmUp: WarmUp = WarmUp(),
) : Program {
    fun day(day: DayOfWeek): TrainingDay = days.firstOrNull { it.day == day } ?: TrainingDay(day)

    fun line(id: String): SessionExercise? = days.firstNotNullOfOrNull { day -> day.exercises.firstOrNull { it.id == id } }
}

/** A session done on another day than planned: the session planned on [from] is done on [date]. */
@Serializable
data class SessionMove(
    @Serializable(with = LocalDateIsoSerializer::class)
    val date: LocalDate,
    @Serializable(with = LocalDateIsoSerializer::class)
    val from: LocalDate,
)

/**
 * How long the session of [date] took: from the opening of its guided session, warm-up included,
 * to its last set. Times are wall-clock milliseconds.
 */
@Serializable
data class SessionTime(
    @Serializable(with = LocalDateIsoSerializer::class)
    val date: LocalDate,
    val startedAt: Long,
    /** When its last set was noted, 0 before the first one. */
    val lastSetAt: Long = 0L,
)

@Serializable
data class Training(
    val programs: List<TrainingProgram> = emptyList(),
    /** Sorted by date. */
    val schedule: List<PlanPeriod> = emptyList(),
    /** Sessions done on another day than planned, sorted by date. */
    val moves: List<SessionMove> = emptyList(),
    /** When the sessions done with the guided session took place, sorted by date. */
    val times: List<SessionTime> = emptyList(),
    /** Whether the program the app comes with was already added: it is never added again. */
    val seeded: Boolean = false,
    /** The programs the app came with later, already added to the data that had the first one: each is added once. */
    val addedPrograms: List<String> = emptyList(),
    /** The single program the app had before programs could follow one another: moved to [programs] when read. */
    @SerialName("program")
    val legacyProgram: TrainingProgram? = null,
) {
    fun program(id: String): TrainingProgram? = programs.firstOrNull { it.id == id }
}
