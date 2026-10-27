package com.lehyanflouriot.muscuapp.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

/** Muscle groups, in the order they are displayed. */
@Serializable
enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, LEGS, GLUTES, ABS, CARDIO, OTHER }

/** What the "reps" value of a set means for an exercise. */
@Serializable
enum class Measure { REPS, SECONDS }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup = MuscleGroup.OTHER,
    val usesWeight: Boolean = true,
    val measure: Measure = Measure.REPS,
    /** Rest suggested between two sets, null when not set. */
    val restSeconds: Int? = null,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
)

/** One set: number of reps (or seconds for [Measure.SECONDS]) and the load in kg (0 = body weight). */
@Serializable
data class WorkSet(
    val reps: Int,
    val weight: Double = 0.0,
)

/** What was done for one exercise on a given day. */
@Serializable
data class WorkoutEntry(
    val id: String,
    val exerciseId: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val date: LocalDate,
    val sets: List<WorkSet>,
    val note: String = "",
    val createdAt: Long = 0L,
)

/** The user's own weight, in kg. There is at most one per day. */
@Serializable
data class BodyWeightEntry(
    @Serializable(with = LocalDateIsoSerializer::class)
    val date: LocalDate,
    val weight: Double,
    val createdAt: Long = 0L,
)

@Serializable
data class RestPreset(
    val id: String,
    val seconds: Int,
    val label: String = "",
)

@Serializable
data class Settings(
    /** Increment used by the +/- buttons of the weight field, in kg. */
    val weightStep: Double = 2.5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)

@Serializable
data class AppData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exercises: List<Exercise> = emptyList(),
    val entries: List<WorkoutEntry> = emptyList(),
    /** Sorted by date. */
    val bodyWeights: List<BodyWeightEntry> = emptyList(),
    val restPresets: List<RestPreset> = emptyList(),
    val settings: Settings = Settings(),
    val nutrition: Nutrition = Nutrition(),
) {
    fun exercise(id: String): Exercise? = exercises.firstOrNull { it.id == id }

    fun entriesOf(exerciseId: String): List<WorkoutEntry> = entries.filter { it.exerciseId == exerciseId }

    companion object {
        /** 2: each food of the plan of the app has a single name. */
        const val CURRENT_SCHEMA_VERSION = 2

        val DEFAULT_REST_PRESETS = listOf(
            RestPreset(id = "preset-45", seconds = 45, label = "Court"),
            RestPreset(id = "preset-60", seconds = 60),
            RestPreset(id = "preset-90", seconds = 90, label = "Classique"),
            RestPreset(id = "preset-120", seconds = 120),
            RestPreset(id = "preset-180", seconds = 180, label = "Lourd"),
            RestPreset(id = "preset-300", seconds = 300, label = "Très lourd"),
        )

        fun initial(): AppData = AppData(restPresets = DEFAULT_REST_PRESETS)
    }
}

/** Stores dates as ISO strings ("2026-10-26") so that backups stay readable. */
object LocalDateIsoSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("muscuapp.LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
