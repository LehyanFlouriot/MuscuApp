package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.shoppingKey
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import java.time.LocalDate

/** Brings data saved by an older version of the app up to date. */
internal fun migrate(data: AppData): AppData {
    var migrated = data
    if (migrated.schemaVersion < 2) migrated = oneNamePerFood(migrated)
    if (migrated.training.legacyProgram != null) migrated = programsOverTime(migrated)
    if (migrated.schemaVersion < 4) migrated = musclesInDetail(migrated)
    // An older version of the app, on another device, may still name the legs as a whole.
    if (migrated.exercises.any { it.muscleGroup == MuscleGroup.LEGS }) {
        migrated = migrated.copy(exercises = migrated.exercises.map { it.withLegsSplit() })
    }
    return migrated
}

/** The muscles of the exercises the app knows: those of its program, then those of its catalog. */
private object KnownMuscles {
    private val program = HypertrophyProgram.exercises()
    private val byId = program.associate { it.id to (it.muscleGroup to it.secondaryMuscles) }
    private val byName = ExerciseCatalog.all.associate { ExerciseCatalog.normalize(it.name) to (it.muscleGroup to it.secondaryMuscles) } +
        program.associate { ExerciseCatalog.normalize(it.name) to (it.muscleGroup to it.secondaryMuscles) }

    fun of(exercise: Exercise): Pair<MuscleGroup, List<MuscleGroup>>? =
        byId[exercise.id] ?: byName[ExerciseCatalog.normalize(exercise.name)]
}

private val legParts = setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
private val calfWords = listOf("mollet", "calf", "calves")
private val hamstringWords = listOf("ischio", "leg curl", "nordic", "souleve de terre", "good morning", "hamstring")

/**
 * The part of the legs an exercise of the legs works: the one the app knows for it, or else the
 * one its name tells, the quadriceps by default.
 */
private fun Exercise.withLegsSplit(): Exercise {
    if (muscleGroup != MuscleGroup.LEGS) return this
    val known = KnownMuscles.of(this)?.first?.takeIf { it in legParts }
    val normalized = ExerciseCatalog.normalize(name)
    val group = known ?: when {
        calfWords.any { it in normalized } -> MuscleGroup.CALVES
        hamstringWords.any { it in normalized } -> MuscleGroup.HAMSTRINGS
        else -> MuscleGroup.QUADS
    }
    return copy(muscleGroup = group)
}

/**
 * 1.13.0 tells the muscles apart: the legs become quadriceps, hamstrings or calves, and the
 * exercises the app knows name the muscles they work too. A main group chosen by the user stays.
 */
private fun musclesInDetail(data: AppData): AppData = data.copy(
    exercises = data.exercises.map { exercise ->
        val split = exercise.withLegsSplit()
        val secondary = KnownMuscles.of(exercise)?.second.orEmpty().filter { it != split.muscleGroup }
        split.copy(secondaryMuscles = split.secondaryMuscles.ifEmpty { secondary })
    },
)

/** Monday of the week the training program came, with 1.7.0: it is followed from then on. */
private val FIRST_PROGRAM_WEEK: LocalDate = LocalDate.of(2026, 10, 26)

/** The single training program of 1.7.0 becomes the first of the programs, followed since it came. */
private fun programsOverTime(data: AppData): AppData {
    val training = data.training
    val program = training.legacyProgram ?: return data
    return data.copy(
        training = training.copy(
            programs = if (training.program(program.id) == null) training.programs + program else training.programs,
            schedule = training.schedule.ifEmpty { listOf(PlanPeriod(program.id, FIRST_PROGRAM_WEEK)) },
            legacyProgram = null,
        ),
    )
}

/**
 * The first plan of the app named the same food in several ways ("huile" and "huile d'olive",
 * "riz" and "riz basmati"...), which split it in the shopping list. Only the foods still named as
 * the app had added them change: the ones the user renamed stay as they are.
 */
private val renamedFoods: Map<String, Pair<String, String>> = mapOf(
    "huile" to ("huile d'olive" to ""),
    "riz" to ("riz basmati" to ""),
    "pâtes" to ("pâtes complètes" to ""),
    "pain" to ("pain complet" to ""),
    "bœuf 5 %" to ("bœuf haché 5 %" to ""),
    "steak haché 5 %" to ("bœuf haché 5 %" to ""),
    "grosse banane" to ("banane" to "Une grosse"),
    "miel ou confiture" to ("miel" to "Ou confiture"),
    "wok de légumes" to ("légumes variés" to "En wok"),
    "poêlée de légumes" to ("légumes variés" to "En poêlée"),
    "légumes couscous" to ("légumes variés" to "Pour le couscous"),
    "pois chiches cuits" to ("pois chiches" to "Pesés cuits"),
    "fruit" to ("fruit" to "Au choix"),
)

/** Foods whose name meant two different things: the turkey of the sandwich is sliced. */
private val renamedById: Map<String, Pair<String, Pair<String, String>>> = mapOf(
    "tue-pre-seance-2" to ("dinde" to ("blanc de dinde" to "En tranches")),
)

private val firstSubstitutions = listOf(
    listOf("riz", "pâtes", "semoule", "pommes de terre"),
    listOf("poulet", "dinde", "cabillaud"),
    listOf("fromage blanc", "skyr"),
)

private fun FoodItem.renamed(): FoodItem {
    renamedById[id]?.let { (old, new) ->
        if (name == old) return copy(name = new.first, note = note.ifBlank { new.second })
    }
    val (newName, newNote) = renamedFoods[name] ?: return this
    return copy(name = newName, note = note.ifBlank { newNote })
}

private fun MealPlan.withOneNamePerFood(): MealPlan = copy(
    days = days.map { day ->
        day.copy(meals = day.meals.map { meal -> meal.copy(items = meal.items.map { it.renamed() }) })
    },
    substitutions = if (substitutions == firstSubstitutions) DefaultMealPlan.SUBSTITUTIONS else substitutions,
)

private fun oneNamePerFood(data: AppData): AppData {
    val nutrition = data.nutrition
    val fromApp = { plan: MealPlan -> plan.id == DefaultMealPlan.ID || plan.versionOf == DefaultMealPlan.ID }
    if (nutrition.plans.none(fromApp)) return data
    // What was already in the cart stays checked under the new names.
    val keys = renamedFoods.entries.associate { (old, new) -> shoppingKey(old) to shoppingKey(new.first) }
    return data.copy(
        nutrition = nutrition.copy(
            plans = nutrition.plans.map { if (fromApp(it)) it.withOneNamePerFood() else it },
            shopping = nutrition.shopping.map { week -> week.copy(checked = week.checked.map { keys[it] ?: it }.distinct()) },
        ),
    )
}
