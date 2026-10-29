package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.ABS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.BACK
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.BICEPS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CALVES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CARDIO
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CHEST
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.GLUTES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.HAMSTRINGS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.QUADS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.SHOULDERS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.TRICEPS
import java.text.Normalizer

/** A common exercise offered when creating a new one. */
data class ExerciseSuggestion(
    val name: String,
    val muscleGroup: MuscleGroup,
    val usesWeight: Boolean = true,
    val measure: Measure = Measure.REPS,
    val restSeconds: Int = 90,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
) {
    fun toDraft(): ExerciseDraft = ExerciseDraft(
        name = name,
        muscleGroup = muscleGroup,
        usesWeight = usesWeight,
        measure = measure,
        restSeconds = restSeconds,
        secondaryMuscles = secondaryMuscles,
    )
}

object ExerciseCatalog {
    private const val HEAVY = 120
    private const val SHORT = 60
    private const val CORE = 45

    val all: List<ExerciseSuggestion> = listOf(
        ExerciseSuggestion("Développé couché", CHEST, restSeconds = HEAVY, secondaryMuscles = listOf(TRICEPS, SHOULDERS)),
        ExerciseSuggestion("Développé incliné haltères", CHEST, secondaryMuscles = listOf(SHOULDERS, TRICEPS)),
        ExerciseSuggestion("Développé décliné", CHEST, secondaryMuscles = listOf(TRICEPS)),
        ExerciseSuggestion("Écarté couché haltères", CHEST),
        ExerciseSuggestion("Écarté à la poulie", CHEST),
        ExerciseSuggestion("Pec deck", CHEST),
        ExerciseSuggestion("Pompes", CHEST, usesWeight = false, restSeconds = SHORT, secondaryMuscles = listOf(TRICEPS, SHOULDERS)),
        ExerciseSuggestion("Dips", CHEST, usesWeight = false, secondaryMuscles = listOf(TRICEPS, SHOULDERS)),
        ExerciseSuggestion("Tractions", BACK, usesWeight = false, restSeconds = HEAVY, secondaryMuscles = listOf(BICEPS)),
        ExerciseSuggestion("Tirage vertical", BACK, secondaryMuscles = listOf(BICEPS)),
        ExerciseSuggestion("Tirage horizontal", BACK, secondaryMuscles = listOf(BICEPS)),
        ExerciseSuggestion("Rowing barre", BACK, restSeconds = HEAVY, secondaryMuscles = listOf(BICEPS)),
        ExerciseSuggestion("Rowing haltère", BACK, secondaryMuscles = listOf(BICEPS)),
        ExerciseSuggestion("Soulevé de terre", BACK, restSeconds = 180, secondaryMuscles = listOf(HAMSTRINGS, GLUTES)),
        ExerciseSuggestion("Pull-over", BACK, secondaryMuscles = listOf(CHEST)),
        ExerciseSuggestion("Shrugs", BACK, restSeconds = SHORT),
        ExerciseSuggestion("Développé militaire", SHOULDERS, restSeconds = HEAVY, secondaryMuscles = listOf(TRICEPS)),
        ExerciseSuggestion("Développé haltères assis", SHOULDERS, secondaryMuscles = listOf(TRICEPS)),
        ExerciseSuggestion("Élévations latérales", SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Élévations frontales", SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Oiseau", SHOULDERS, restSeconds = SHORT, secondaryMuscles = listOf(BACK)),
        ExerciseSuggestion("Face pull", SHOULDERS, restSeconds = SHORT, secondaryMuscles = listOf(BACK)),
        ExerciseSuggestion("Curl barre", BICEPS),
        ExerciseSuggestion("Curl haltères", BICEPS),
        ExerciseSuggestion("Curl marteau", BICEPS),
        ExerciseSuggestion("Curl pupitre", BICEPS),
        ExerciseSuggestion("Curl à la poulie", BICEPS, restSeconds = SHORT),
        ExerciseSuggestion("Extension triceps à la poulie", TRICEPS, restSeconds = SHORT),
        ExerciseSuggestion("Barre au front", TRICEPS),
        ExerciseSuggestion("Extension nuque haltère", TRICEPS),
        ExerciseSuggestion("Développé couché prise serrée", TRICEPS, restSeconds = HEAVY, secondaryMuscles = listOf(CHEST)),
        ExerciseSuggestion("Dips sur banc", TRICEPS, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Squat", QUADS, restSeconds = 180, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Presse à cuisses", QUADS, restSeconds = HEAVY, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Fentes", QUADS, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Squat bulgare", QUADS, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Leg extension", QUADS, restSeconds = SHORT),
        ExerciseSuggestion("Leg curl", HAMSTRINGS, restSeconds = SHORT),
        ExerciseSuggestion("Soulevé de terre roumain", HAMSTRINGS, restSeconds = HEAVY, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Hack squat", QUADS, restSeconds = HEAVY, secondaryMuscles = listOf(GLUTES)),
        ExerciseSuggestion("Mollets debout", CALVES, restSeconds = SHORT),
        ExerciseSuggestion("Mollets assis", CALVES, restSeconds = SHORT),
        ExerciseSuggestion("Hip thrust", GLUTES, restSeconds = HEAVY, secondaryMuscles = listOf(HAMSTRINGS)),
        ExerciseSuggestion("Pont fessier", GLUTES, usesWeight = false, restSeconds = SHORT, secondaryMuscles = listOf(HAMSTRINGS)),
        ExerciseSuggestion("Abducteurs machine", GLUTES, restSeconds = SHORT),
        ExerciseSuggestion("Kickback à la poulie", GLUTES, restSeconds = SHORT),
        ExerciseSuggestion("Crunch", ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Gainage", ABS, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Gainage latéral", ABS, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Relevé de jambes", ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Russian twist", ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Roue abdominale", ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Crunch à la poulie", ABS, restSeconds = CORE),
        ExerciseSuggestion("Burpees", CARDIO, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Corde à sauter", CARDIO, usesWeight = false, measure = Measure.SECONDS, restSeconds = SHORT),
        ExerciseSuggestion("Mountain climbers", CARDIO, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Jumping jacks", CARDIO, usesWeight = false, restSeconds = CORE),
    )

    private val popularNames = listOf(
        "Pompes", "Tractions", "Squat", "Dips", "Gainage", "Hip thrust",
        "Développé couché", "Soulevé de terre", "Développé militaire", "Rowing barre", "Curl haltères", "Presse à cuisses",
    )

    /** Shown before anything is typed. */
    val popular: List<ExerciseSuggestion> = popularNames.mapNotNull { name -> all.firstOrNull { it.name == name } }

    /**
     * Suggestions matching [query] (accents and case ignored), names starting with the query first.
     * Exercises whose name is in [existingNames] are left out.
     */
    fun search(query: String, existingNames: Collection<String> = emptyList(), limit: Int = 8): List<ExerciseSuggestion> {
        val existing = existingNames.mapTo(HashSet()) { normalize(it) }
        val available = all.filter { normalize(it.name) !in existing }
        val needle = normalize(query)
        if (needle.isEmpty()) {
            return popular.filter { normalize(it.name) !in existing }.take(limit)
        }
        return available
            .filter { normalize(it.name).contains(needle) }
            .sortedBy { if (normalize(it.name).startsWith(needle)) 0 else 1 }
            .take(limit)
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
