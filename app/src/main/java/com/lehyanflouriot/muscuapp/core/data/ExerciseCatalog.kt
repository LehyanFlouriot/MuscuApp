package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import java.text.Normalizer

/** A common exercise offered when creating a new one. */
data class ExerciseSuggestion(
    val name: String,
    val muscleGroup: MuscleGroup,
    val usesWeight: Boolean = true,
    val measure: Measure = Measure.REPS,
    val restSeconds: Int = 90,
) {
    fun toDraft(): ExerciseDraft = ExerciseDraft(
        name = name,
        muscleGroup = muscleGroup,
        usesWeight = usesWeight,
        measure = measure,
        restSeconds = restSeconds,
    )
}

object ExerciseCatalog {
    private const val HEAVY = 120
    private const val SHORT = 60
    private const val CORE = 45

    val all: List<ExerciseSuggestion> = listOf(
        ExerciseSuggestion("Développé couché", MuscleGroup.CHEST, restSeconds = HEAVY),
        ExerciseSuggestion("Développé incliné haltères", MuscleGroup.CHEST),
        ExerciseSuggestion("Développé décliné", MuscleGroup.CHEST),
        ExerciseSuggestion("Écarté couché haltères", MuscleGroup.CHEST),
        ExerciseSuggestion("Écarté à la poulie", MuscleGroup.CHEST),
        ExerciseSuggestion("Pec deck", MuscleGroup.CHEST),
        ExerciseSuggestion("Pompes", MuscleGroup.CHEST, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Dips", MuscleGroup.CHEST, usesWeight = false),
        ExerciseSuggestion("Tractions", MuscleGroup.BACK, usesWeight = false, restSeconds = HEAVY),
        ExerciseSuggestion("Tirage vertical", MuscleGroup.BACK),
        ExerciseSuggestion("Tirage horizontal", MuscleGroup.BACK),
        ExerciseSuggestion("Rowing barre", MuscleGroup.BACK, restSeconds = HEAVY),
        ExerciseSuggestion("Rowing haltère", MuscleGroup.BACK),
        ExerciseSuggestion("Soulevé de terre", MuscleGroup.BACK, restSeconds = 180),
        ExerciseSuggestion("Pull-over", MuscleGroup.BACK),
        ExerciseSuggestion("Shrugs", MuscleGroup.BACK, restSeconds = SHORT),
        ExerciseSuggestion("Développé militaire", MuscleGroup.SHOULDERS, restSeconds = HEAVY),
        ExerciseSuggestion("Développé haltères assis", MuscleGroup.SHOULDERS),
        ExerciseSuggestion("Élévations latérales", MuscleGroup.SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Élévations frontales", MuscleGroup.SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Oiseau", MuscleGroup.SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Face pull", MuscleGroup.SHOULDERS, restSeconds = SHORT),
        ExerciseSuggestion("Curl barre", MuscleGroup.BICEPS),
        ExerciseSuggestion("Curl haltères", MuscleGroup.BICEPS),
        ExerciseSuggestion("Curl marteau", MuscleGroup.BICEPS),
        ExerciseSuggestion("Curl pupitre", MuscleGroup.BICEPS),
        ExerciseSuggestion("Curl à la poulie", MuscleGroup.BICEPS, restSeconds = SHORT),
        ExerciseSuggestion("Extension triceps à la poulie", MuscleGroup.TRICEPS, restSeconds = SHORT),
        ExerciseSuggestion("Barre au front", MuscleGroup.TRICEPS),
        ExerciseSuggestion("Extension nuque haltère", MuscleGroup.TRICEPS),
        ExerciseSuggestion("Développé couché prise serrée", MuscleGroup.TRICEPS, restSeconds = HEAVY),
        ExerciseSuggestion("Dips sur banc", MuscleGroup.TRICEPS, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Squat", MuscleGroup.LEGS, restSeconds = 180),
        ExerciseSuggestion("Presse à cuisses", MuscleGroup.LEGS, restSeconds = HEAVY),
        ExerciseSuggestion("Fentes", MuscleGroup.LEGS),
        ExerciseSuggestion("Squat bulgare", MuscleGroup.LEGS),
        ExerciseSuggestion("Leg extension", MuscleGroup.LEGS, restSeconds = SHORT),
        ExerciseSuggestion("Leg curl", MuscleGroup.LEGS, restSeconds = SHORT),
        ExerciseSuggestion("Soulevé de terre roumain", MuscleGroup.LEGS, restSeconds = HEAVY),
        ExerciseSuggestion("Hack squat", MuscleGroup.LEGS, restSeconds = HEAVY),
        ExerciseSuggestion("Mollets debout", MuscleGroup.LEGS, restSeconds = SHORT),
        ExerciseSuggestion("Mollets assis", MuscleGroup.LEGS, restSeconds = SHORT),
        ExerciseSuggestion("Hip thrust", MuscleGroup.GLUTES, restSeconds = HEAVY),
        ExerciseSuggestion("Pont fessier", MuscleGroup.GLUTES, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Abducteurs machine", MuscleGroup.GLUTES, restSeconds = SHORT),
        ExerciseSuggestion("Kickback à la poulie", MuscleGroup.GLUTES, restSeconds = SHORT),
        ExerciseSuggestion("Crunch", MuscleGroup.ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Gainage", MuscleGroup.ABS, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Gainage latéral", MuscleGroup.ABS, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Relevé de jambes", MuscleGroup.ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Russian twist", MuscleGroup.ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Roue abdominale", MuscleGroup.ABS, usesWeight = false, restSeconds = CORE),
        ExerciseSuggestion("Crunch à la poulie", MuscleGroup.ABS, restSeconds = CORE),
        ExerciseSuggestion("Burpees", MuscleGroup.CARDIO, usesWeight = false, restSeconds = SHORT),
        ExerciseSuggestion("Corde à sauter", MuscleGroup.CARDIO, usesWeight = false, measure = Measure.SECONDS, restSeconds = SHORT),
        ExerciseSuggestion("Mountain climbers", MuscleGroup.CARDIO, usesWeight = false, measure = Measure.SECONDS, restSeconds = CORE),
        ExerciseSuggestion("Jumping jacks", MuscleGroup.CARDIO, usesWeight = false, restSeconds = CORE),
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
