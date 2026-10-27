package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.shoppingKey
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.MealPlan

/** Brings data saved by an older version of the app up to date. */
internal fun migrate(data: AppData): AppData {
    var migrated = data
    if (migrated.schemaVersion < 2) migrated = oneNamePerFood(migrated)
    return migrated
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
