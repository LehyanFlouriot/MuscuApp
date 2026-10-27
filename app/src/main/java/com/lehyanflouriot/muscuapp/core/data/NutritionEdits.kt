package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.MAX_WATER_GLASSES
import com.lehyanflouriot.muscuapp.core.domain.periodAt
import com.lehyanflouriot.muscuapp.core.domain.planAt
import com.lehyanflouriot.muscuapp.core.domain.shoppingKey
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.model.DayPlan
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.NutritionDay
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.PlannedMeal
import com.lehyanflouriot.muscuapp.core.model.ShoppingWeek
import java.time.DayOfWeek
import java.time.LocalDate

/** Highest calories of a meal, and grams of protein, that can be entered. */
const val MAX_MEAL_KCAL = 5_000
const val MAX_MEAL_PROTEIN = 500

val GLASS_ML_RANGE = 50..2_000

/** Shopping lists older than this many weeks are forgotten. */
const val SHOPPING_WEEKS_KEPT = 8

// region Changes of a plan, kept free of ids generation so that they read simply

fun MealPlan.updateDay(dayOfWeek: DayOfWeek, transform: (DayPlan) -> DayPlan): MealPlan =
    copy(days = DayOfWeek.entries.map { if (it == dayOfWeek) transform(day(it)) else day(it) })

private fun PlannedMeal.isBlank(): Boolean = items.isEmpty() && note.isBlank() && kcal == null && protein == null

fun DayPlan.updateMeal(slotId: String, transform: (PlannedMeal) -> PlannedMeal): DayPlan {
    val updated = transform(meal(slotId))
    val others = meals.filterNot { it.slotId == slotId }
    return copy(meals = if (updated.isBlank()) others else others + updated)
}

/** Changes a food wherever it is in the week. */
fun MealPlan.updateFood(itemId: String, transform: (FoodItem) -> FoodItem): MealPlan = copy(
    days = days.map { day ->
        day.copy(meals = day.meals.map { meal -> meal.copy(items = meal.items.map { if (it.id == itemId) transform(it) else it }) })
    },
)

/** Gives a single new name to every food called [from] in the week, and in the substitutions. */
fun MealPlan.renameFoods(from: String, to: String): MealPlan {
    val key = shoppingKey(from)
    fun String.renamed() = if (shoppingKey(this) == key) to else this
    return copy(
        days = days.map { day ->
            day.copy(meals = day.meals.map { meal -> meal.copy(items = meal.items.map { it.copy(name = it.name.renamed()) }) })
        },
        substitutions = substitutions.map { group -> group.map { it.renamed() }.distinct() },
    )
}

fun MealPlan.removeFood(itemId: String): MealPlan = copy(
    days = days.map { day ->
        day.copy(
            meals = day.meals.map { meal -> meal.copy(items = meal.items.filterNot { it.id == itemId }) }.filterNot { it.isBlank() },
        )
    },
)

/** Moves a food up (negative [delta]) or down in its meal. */
fun MealPlan.moveFood(itemId: String, delta: Int): MealPlan = copy(
    days = days.map { day ->
        day.copy(
            meals = day.meals.map { meal ->
                val index = meal.items.indexOfFirst { it.id == itemId }
                if (index < 0) {
                    meal
                } else {
                    val target = (index + delta).coerceIn(0, meal.items.lastIndex)
                    meal.copy(items = meal.items.toMutableList().apply { add(target, removeAt(index)) })
                }
            },
        )
    },
)

fun MealPlan.updateSlot(slot: MealSlot): MealPlan = copy(slots = slots.map { if (it.id == slot.id) slot else it })

/** Removes a meal from every day of the week. */
fun MealPlan.removeSlot(slotId: String): MealPlan = copy(
    slots = slots.filterNot { it.id == slotId },
    days = days.map { day -> day.copy(meals = day.meals.filterNot { it.slotId == slotId }) },
)

fun MealPlan.moveSlot(slotId: String, delta: Int): MealPlan {
    val index = slots.indexOfFirst { it.id == slotId }
    if (index < 0) return this
    val target = (index + delta).coerceIn(0, slots.lastIndex)
    return copy(slots = slots.toMutableList().apply { add(target, removeAt(index)) })
}

// endregion

// region Plans over time

/** Sorted by date, without a period that only goes on with the plan of the one before. */
internal fun List<PlanPeriod>.merged(): List<PlanPeriod> {
    val sorted = sortedBy { it.from }
    return sorted.filterIndexed { index, period -> index == 0 || sorted[index - 1].planId != period.planId }
}

/** Adds the plan the app comes with, followed from the start of the current week. Done only once. */
internal fun Nutrition.withDefaultPlan(today: LocalDate, createdAt: Long): Nutrition {
    if (seeded) return this
    val plan = DefaultMealPlan.create(createdAt)
    if (plan(plan.id) != null) return copy(seeded = true)
    return copy(
        plans = plans + plan,
        schedule = (schedule + PlanPeriod(plan.id, today.startOfWeek())).sortedBy { it.from },
        seeded = true,
    )
}

/**
 * Changes a plan without changing the days already lived: when something was noted on days
 * followed with it before [today], those days move to a copy of the plan as it was (with the same
 * food ids, so that what was eaten stays checked), and the changed plan is followed from [today].
 */
internal fun Nutrition.changePlan(
    planId: String,
    today: LocalDate,
    newId: () -> String,
    transform: (MealPlan) -> MealPlan,
): Nutrition {
    val plan = plan(planId)?.takeIf { it.versionOf == null } ?: return this
    val changed = transform(plan)
    if (changed == plan) return this
    val lived = schedule.indices.filter { index ->
        val period = schedule[index]
        if (period.planId != planId || period.from >= today) return@filter false
        val end = minOf(schedule.getOrNull(index + 1)?.from ?: today, today)
        days.any { !it.isEmpty && it.date >= period.from && it.date < end }
    }
    val newPlans = plans.map { if (it.id == planId) changed else it }
    if (lived.isEmpty()) return copy(plans = newPlans)
    val kept = plan.copy(id = newId(), versionOf = planId)
    val newSchedule = buildList {
        schedule.forEachIndexed { index, period ->
            if (index in lived) {
                add(period.copy(planId = kept.id))
                val next = schedule.getOrNull(index + 1)?.from
                if (next == null || next > today) add(PlanPeriod(planId, today))
            } else {
                add(period)
            }
        }
    }
    return copy(plans = newPlans + kept, schedule = newSchedule.sortedBy { it.from })
}

/** Follows [planId] from [from] on, until the next period already planned. */
internal fun Nutrition.startPlan(planId: String, from: LocalDate): Nutrition {
    if (plan(planId)?.versionOf != null || plan(planId) == null) return this
    if (periodAt(from)?.planId == planId) return this
    return copy(schedule = (schedule.filterNot { it.from == from } + PlanPeriod(planId, from)).merged())
}

/** Cancels a start planned after [today]. */
internal fun Nutrition.cancelStart(planId: String, from: LocalDate, today: LocalDate): Nutrition =
    if (from <= today) this else copy(schedule = schedule.filterNot { it.planId == planId && it.from == from }.merged())

/** Changes the shopping list of the week of [weekStart], forgetting the lists of long ago. */
internal fun Nutrition.updateShoppingWeek(weekStart: LocalDate, transform: (ShoppingWeek) -> ShoppingWeek): Nutrition {
    val start = weekStart.startOfWeek()
    val updated = transform(shoppingWeek(start))
    val others = shopping.filterNot { it.weekStart == start || it.weekStart < start.minusWeeks(SHOPPING_WEEKS_KEPT.toLong()) }
    return copy(shopping = if (updated.isEmpty) others else (others + updated).sortedBy { it.weekStart })
}

internal fun Nutrition.updateDay(date: LocalDate, transform: (NutritionDay) -> NutritionDay): Nutrition {
    val updated = transform(day(date))
    val others = days.filterNot { it.date == date }
    return copy(days = if (updated.isEmpty) others else (others + updated).sortedBy { it.date })
}

// endregion

// region New programs

internal fun blankPlan(id: String, name: String, createdAt: Long, newId: () -> String): MealPlan = MealPlan(
    id = id,
    name = name,
    slots = listOf(
        MealSlot(newId(), "Petit-déjeuner", moment = MealMoment.MORNING),
        MealSlot(newId(), "Déjeuner", moment = MealMoment.LUNCH),
        MealSlot(newId(), "Collation", moment = MealMoment.SNACK),
        MealSlot(newId(), "Dîner", moment = MealMoment.DINNER),
    ),
    days = DayOfWeek.entries.map { DayPlan(it, waterGlasses = 8) },
    createdAt = createdAt,
)

/** A copy of [source] to start a new program from: the same foods, with their ids. */
internal fun copiedPlan(source: MealPlan, id: String, name: String, createdAt: Long): MealPlan =
    source.copy(id = id, name = name, versionOf = null, createdAt = createdAt)

/** The meals (and water) of [from] copied to the days [to], as foods of their own. */
internal fun MealPlan.copyDay(from: DayOfWeek, to: Collection<DayOfWeek>, newId: () -> String): MealPlan {
    val source = day(from)
    return to.filter { it != from }.fold(this) { plan, target ->
        plan.updateDay(target) { day ->
            day.copy(
                waterGlasses = source.waterGlasses,
                meals = source.meals.map { meal -> meal.copy(items = meal.items.map { it.copy(id = newId()) }) },
            )
        }
    }
}

// endregion

// region Sanitizing

private fun Int?.within(range: IntRange): Int? = this?.takeIf { it in range }

private fun sanitizePlan(plan: MealPlan): MealPlan {
    val slots = plan.slots
        .filter { it.id.isNotBlank() && it.name.isNotBlank() }
        .distinctBy { it.id }
        .map { it.copy(kcal = it.kcal.within(0..MAX_MEAL_KCAL), protein = it.protein.within(0..MAX_MEAL_PROTEIN)) }
    val slotIds = slots.mapTo(HashSet()) { it.id }
    // A food id is used only once in the week.
    val itemIds = HashSet<String>()
    val days = DayOfWeek.entries.map { dayOfWeek ->
        val day = plan.days.firstOrNull { it.day == dayOfWeek } ?: DayPlan(dayOfWeek)
        day.copy(
            waterGlasses = day.waterGlasses.coerceIn(0, MAX_WATER_GLASSES),
            meals = day.meals
                .filter { it.slotId in slotIds }
                .distinctBy { it.slotId }
                .map { meal ->
                    meal.copy(
                        items = meal.items.filter { it.id.isNotBlank() && it.name.isNotBlank() && itemIds.add(it.id) },
                        kcal = meal.kcal.within(0..MAX_MEAL_KCAL),
                        protein = meal.protein.within(0..MAX_MEAL_PROTEIN),
                    )
                }
                .filterNot { it.isBlank() },
        )
    }
    return plan.copy(
        name = plan.name.trim().ifEmpty { "Programme" },
        slots = slots,
        days = days,
        glassMl = plan.glassMl.takeIf { it in GLASS_ML_RANGE } ?: MealPlan.DEFAULT_GLASS_ML,
        substitutions = plan.substitutions
            .map { group -> group.map(String::trim).filter(String::isNotEmpty).distinct() }
            .filter { it.size >= 2 },
        notes = plan.notes.filter { it.title.isNotBlank() || it.text.isNotBlank() }.distinctBy { it.id },
    )
}

/** Drops what would break the nutrition screens: unknown plans, foods or days noted twice... */
internal fun sanitizeNutrition(nutrition: Nutrition): Nutrition {
    val plans = nutrition.plans
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .map(::sanitizePlan)
    val planIds = plans.mapTo(HashSet()) { it.id }
    // A copy kept for the past whose program is gone becomes a program of its own.
    val cleanPlans = plans.map { if (it.versionOf != null && it.versionOf !in planIds) it.copy(versionOf = null) else it }
    // One plan per start day: the last one wins.
    val schedule = nutrition.schedule
        .filter { it.planId in planIds }
        .asReversed()
        .distinctBy { it.from }
        .merged()
    val cleaned = Nutrition(plans = cleanPlans, schedule = schedule, seeded = nutrition.seeded)
    val days = nutrition.days
        .distinctBy { it.date }
        .sortedBy { it.date }
        .map { day ->
            val ids = cleaned.planAt(day.date)?.day(day.date.dayOfWeek)?.meals
                ?.flatMapTo(HashSet()) { meal -> meal.items.map { it.id } }
                .orEmpty()
            day.copy(
                eaten = day.eaten.filter { it in ids }.distinct(),
                swaps = day.swaps.filter { it.itemId in ids && it.name.isNotBlank() }.distinctBy { it.itemId },
                water = day.water.coerceIn(0, MAX_WATER_GLASSES),
            )
        }
        .filterNot { it.isEmpty }
    val shopping = nutrition.shopping
        .map { week ->
            week.copy(
                weekStart = week.weekStart.startOfWeek(),
                checked = week.checked.filter { it.isNotBlank() }.distinct(),
                extras = week.extras.filter { it.id.isNotBlank() && it.name.isNotBlank() }.distinctBy { it.id },
            )
        }
        .filterNot { it.isEmpty }
        .distinctBy { it.weekStart }
        .sortedBy { it.weekStart }
        .takeLast(SHOPPING_WEEKS_KEPT * 2)
    return cleaned.copy(days = days, shopping = shopping)
}

// endregion
