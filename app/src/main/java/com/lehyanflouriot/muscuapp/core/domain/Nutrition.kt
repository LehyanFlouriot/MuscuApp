package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.DayPlan
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.FoodSwap
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.WeightGoal
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

/** Most glasses of water that can be noted in a day. */
const val MAX_WATER_GLASSES = 30

// region Plans over time

/** The period of the plan followed on [date], null before the first plan started. */
fun Nutrition.periodAt(date: LocalDate): PlanPeriod? = schedule.periodAt(date)

/** The plan followed on [date], as it was that day. */
fun Nutrition.planAt(date: LocalDate): MealPlan? = periodAt(date)?.let { plan(it.planId) }

/** The program the user sees for [date]: the copies keeping past days point to the plan they come from. */
fun Nutrition.programAt(date: LocalDate): MealPlan? = planAt(date)?.let(::programOf)

fun Nutrition.programOf(plan: MealPlan): MealPlan = plan.versionOf?.let(::plan) ?: plan

/** The programs the user can see and change, without the copies kept for the past. */
val Nutrition.programs: List<MealPlan> get() = plans.filter { it.versionOf == null }

/** Whether something was noted on a day followed with [planId] (or one of its kept copies). */
fun Nutrition.hasHistory(planId: String): Boolean = days.any { day ->
    !day.isEmpty && planAt(day.date)?.let { programOf(it).id } == planId
}

/** Where each program stands on [today]: the current one first, then the planned ones, then the others. */
fun Nutrition.programInfos(today: LocalDate): List<ProgramInfo<MealPlan>> = programInfos(plans, schedule, today)

// endregion

// region A day of the plan

data class FoodView(val item: FoodItem, val swap: FoodSwap?, val eaten: Boolean) {
    val name: String get() = swap?.name ?: item.name
    val quantity: String get() = swap?.quantity ?: item.quantity
}

data class MealView(
    val slot: MealSlot,
    val foods: List<FoodView>,
    val kcal: Int?,
    val protein: Int?,
    val note: String,
) {
    val eatenCount: Int get() = foods.count { it.eaten }
    val done: Boolean get() = foods.isNotEmpty() && foods.all { it.eaten }

    /** Share of the foods eaten, from 0 to 1. */
    val progress: Double get() = if (foods.isEmpty()) 0.0 else eatenCount.toDouble() / foods.size
}

data class NutritionDayView(
    val date: LocalDate,
    val plan: MealPlan,
    val dayPlan: DayPlan,
    val meals: List<MealView>,
    val water: Int,
) {
    val waterTarget: Int get() = dayPlan.waterGlasses
    val foodCount: Int get() = meals.sumOf { it.foods.size }
    val eatenCount: Int get() = meals.sumOf { it.eatenCount }
    val kcalTarget: Int? get() = meals.mapNotNull { it.kcal }.takeIf { it.isNotEmpty() }?.sum()
    val proteinTarget: Int? get() = meals.mapNotNull { it.protein }.takeIf { it.isNotEmpty() }?.sum()

    /** Calories of the foods eaten, each food counting for its share of its meal. */
    val kcalEaten: Double get() = meals.sumOf { (it.kcal ?: 0) * it.progress }
    val proteinEaten: Double get() = meals.sumOf { (it.protein ?: 0) * it.progress }

    /** Share of the foods eaten, from 0 to 1. */
    val progress: Double get() = if (foodCount == 0) 0.0 else eatenCount.toDouble() / foodCount
    val allEaten: Boolean get() = foodCount > 0 && eatenCount == foodCount
    val waterDone: Boolean get() = water >= waterTarget
    val perfect: Boolean get() = allEaten && waterDone

    /** The first meal still to finish. */
    val nextMeal: MealView? get() = meals.firstOrNull { !it.done }
}

/** What is planned on [date] and what was eaten, or null when no plan is followed that day. */
fun buildNutritionDay(nutrition: Nutrition, date: LocalDate): NutritionDayView? {
    val plan = nutrition.planAt(date) ?: return null
    val dayPlan = plan.day(date.dayOfWeek)
    val record = nutrition.day(date)
    val eaten = record.eaten.toHashSet()
    val meals = plan.slots.mapNotNull { slot ->
        val meal = dayPlan.meal(slot.id)
        if (meal.items.isEmpty() && meal.note.isBlank()) return@mapNotNull null
        MealView(
            slot = slot,
            foods = meal.items.map { FoodView(it, record.swap(it.id), it.id in eaten) },
            kcal = meal.kcal ?: slot.kcal,
            protein = meal.protein ?: slot.protein,
            note = meal.note,
        )
    }
    return NutritionDayView(date, plan, dayPlan, meals, record.water)
}

/** Share of the foods eaten on each day of the week starting [weekStart], null when no plan is followed. */
fun nutritionWeek(nutrition: Nutrition, weekStart: LocalDate): List<Pair<LocalDate, Double?>> =
    (0L until 7L).map { offset ->
        val date = weekStart.plusDays(offset)
        date to buildNutritionDay(nutrition, date)?.takeIf { it.foodCount > 0 }?.progress
    }

/** Calories planned on the training days and on the rest days of [plan]: "3 000 – 3 150". */
fun MealPlan.kcalRange(training: Boolean): IntRange? {
    val totals = days.filter { it.training == training }.mapNotNull { day ->
        slots.mapNotNull { slot ->
            val meal = day.meal(slot.id)
            if (meal.items.isEmpty()) null else meal.kcal ?: slot.kcal
        }.takeIf { it.isNotEmpty() }?.sum()
    }
    return if (totals.isEmpty()) null else totals.min()..totals.max()
}

// endregion

// region Swaps

private val accents = Regex("\\p{Mn}+")

/** Lowercase, without accents nor ligatures: "Pâtes" → "pates", "œufs" → "oeufs". */
internal fun normalizeFood(text: String): String =
    Normalizer.normalize(text.lowercase().replace("œ", "oe").replace("æ", "ae"), Normalizer.Form.NFD)
        .replace(accents, "")
        .trim()

internal fun String.containsWord(word: String): Boolean =
    word.isNotEmpty() && Regex("(^|[^a-z0-9])" + Regex.escape(word) + "($|[^a-z0-9])").containsMatchIn(this)

/** Foods that can replace [name] according to the substitutions of the plan, in their order. */
fun MealPlan.substitutesFor(name: String): List<String> {
    val food = normalizeFood(name)
    return substitutions
        .filter { group -> group.any { food.containsWord(normalizeFood(it)) } }
        .flatMap { group -> group.filterNot { food.containsWord(normalizeFood(it)) } }
        .distinctBy(::normalizeFood)
}

/** The names of the foods of the week, the most used first: to keep calling a food the same way. */
fun MealPlan.foodNames(): List<String> =
    days.flatMap { day -> day.meals.flatMap { meal -> meal.items.map { it.name.trim() } } }
        .filter { it.isNotEmpty() }
        .groupBy(::shoppingKey)
        .values
        .sortedWith(compareByDescending<List<String>> { it.size }.thenBy { normalizeFood(it.first()) })
        .map { spellings -> spellings.groupingBy { it }.eachCount().maxBy { it.value }.key }

/** How many foods of the week are called [name], whatever their case or accents. */
fun MealPlan.foodsNamed(name: String): Int {
    val key = shoppingKey(name)
    return days.sumOf { day -> day.meals.sumOf { meal -> meal.items.count { shoppingKey(it.name) == key } } }
}

// endregion

// region Weight guidance

enum class WeightAdvice { NEED_DATA, BELOW, ON_TRACK, ABOVE }

data class WeightGuidance(
    val goal: WeightGoal,
    /** Average of the last days, as advised to compare weights. */
    val current: Double?,
    /** kg per week over the last [RATE_DAYS] days, as shown with the weights. */
    val weeklyRate: Double?,
    val advice: WeightAdvice,
    /** Way already done from the start to the target, from 0 to 1. */
    val progress: Double?,
    /** When the target would be reached at this pace. */
    val eta: LocalDate?,
)

/** Compares how the weight changes with what the plan aims at, the way the plan says to do it. */
fun weightGuidance(goal: WeightGoal, weights: List<BodyWeightEntry>, today: LocalDate): WeightGuidance {
    val known = weights.filter { it.date <= today }.sortedBy { it.date }
    val current = weightTrend(known).lastOrNull()?.weight
    val recent = known.lastOrNull()?.let { it.date >= today.minusDays(7) } == true
    val rate = if (recent) weeklyRate(known, RATE_DAYS) else null
    val advice = when {
        rate == null -> WeightAdvice.NEED_DATA
        rate < goal.minWeeklyChange -> WeightAdvice.BELOW
        rate > goal.maxWeeklyChange -> WeightAdvice.ABOVE
        else -> WeightAdvice.ON_TRACK
    }
    val start = goal.start
    val target = goal.target
    val progress = if (current != null && start != null && target != null && target != start) {
        ((current - start) / (target - start)).coerceIn(0.0, 1.0)
    } else {
        null
    }
    // Only when the weight goes the right way, and not for an unrealistic far future.
    val eta = if (current != null && target != null && rate != null && abs(rate) >= 0.05) {
        val weeks = (target - current) / rate
        if (weeks > 0 && weeks <= 104) today.plusDays(ceil(weeks * 7).toLong()) else null
    } else {
        null
    }
    return WeightGuidance(goal, current, rate, advice, progress, eta)
}

// endregion
