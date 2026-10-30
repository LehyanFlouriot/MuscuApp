package com.lehyanflouriot.muscuapp.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDate

/** A food of a meal: "150 g" of "riz basmati". The quantity is free text ("1", "1,5 c. à s."). */
@Serializable
data class FoodItem(
    val id: String,
    val name: String,
    val quantity: String = "",
    val note: String = "",
)

/** When a meal is eaten, to give it an icon. */
@Serializable
enum class MealMoment { MORNING, LUNCH, SNACK, PRE_WORKOUT, POST_WORKOUT, DINNER, NIGHT }

/** One of the meals of the day ("Matin", "Déjeuner"...), with its usual targets. */
@Serializable
data class MealSlot(
    val id: String,
    val name: String,
    /** When or how to take it: "2 h avant", "shake, à boire". */
    val hint: String = "",
    val moment: MealMoment = MealMoment.SNACK,
    val kcal: Int? = null,
    /** Grams of protein. */
    val protein: Int? = null,
)

/** What is planned for one meal on one day of the week. */
@Serializable
data class PlannedMeal(
    val slotId: String,
    val items: List<FoodItem> = emptyList(),
    /** Targets of this day, when they differ from the usual ones of the meal. */
    val kcal: Int? = null,
    val protein: Int? = null,
    val note: String = "",
)

@Serializable
data class DayPlan(
    @Serializable(with = DayOfWeekSerializer::class)
    val day: DayOfWeek,
    /** What the day is about: "Pectoraux", "Repos". */
    val label: String = "",
    val training: Boolean = true,
    val waterGlasses: Int = 0,
    val meals: List<PlannedMeal> = emptyList(),
) {
    fun meal(slotId: String): PlannedMeal = meals.firstOrNull { it.slotId == slotId } ?: PlannedMeal(slotId)
}

/** A tip of the plan, shown with the meals. */
@Serializable
data class PlanNote(
    val id: String,
    val title: String,
    val text: String = "",
)

/** How the plan should make the weight change, and what to do when it does not. */
@Serializable
data class WeightGoal(
    val start: Double? = null,
    val target: Double? = null,
    /** Weekly change aimed at, in kg: negative to lose weight. */
    val minWeeklyChange: Double = 0.1,
    val maxWeeklyChange: Double = 0.4,
    /** What to do when the weight changes less than [minWeeklyChange]. */
    val belowAdvice: String = "",
    /** What to do when the weight changes more than [maxWeeklyChange]. */
    val aboveAdvice: String = "",
)

/** A week of meals, repeated until another plan takes over. */
@Serializable
data class MealPlan(
    override val id: String,
    override val name: String,
    /** Free description: targets, how to weigh the food... */
    val info: String = "",
    val slots: List<MealSlot> = emptyList(),
    /** One per day of the week, from Monday to Sunday. */
    val days: List<DayPlan> = emptyList(),
    /** Size of a glass of water, in ml. */
    val glassMl: Int = DEFAULT_GLASS_ML,
    val waterHint: String = "",
    /** Groups of foods that can replace one another. */
    val substitutions: List<List<String>> = emptyList(),
    val notes: List<PlanNote> = emptyList(),
    val weightGoal: WeightGoal? = null,
    override val versionOf: String? = null,
    val createdAt: Long = 0L,
) : Program {
    fun day(day: DayOfWeek): DayPlan = days.firstOrNull { it.day == day } ?: DayPlan(day)

    fun slot(id: String): MealSlot? = slots.firstOrNull { it.id == id }

    fun item(id: String): FoodItem? = days.firstNotNullOfOrNull { day ->
        day.meals.firstNotNullOfOrNull { meal -> meal.items.firstOrNull { it.id == id } }
    }

    companion object {
        const val DEFAULT_GLASS_ML = 250
    }
}

/** A food replaced by another one for a single day. */
@Serializable
data class FoodSwap(
    val itemId: String,
    val name: String,
    val quantity: String = "",
)

/** What was eaten and drunk on a day. */
@Serializable
data class NutritionDay(
    @Serializable(with = LocalDateIsoSerializer::class)
    val date: LocalDate,
    /** Ids of the foods of the plan that were eaten. */
    val eaten: List<String> = emptyList(),
    val swaps: List<FoodSwap> = emptyList(),
    /** Glasses of water drunk. */
    val water: Int = 0,
    /** Slots of the meals passed over before they were finished: the next meal comes after them. */
    val skipped: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = eaten.isEmpty() && swaps.isEmpty() && water == 0 && skipped.isEmpty()

    fun swap(itemId: String): FoodSwap? = swaps.firstOrNull { it.itemId == itemId }
}

/** Something the user added to a shopping list, besides the foods of the program. */
@Serializable
data class ShoppingExtra(
    val id: String,
    val name: String,
    val checked: Boolean = false,
)

/** What is already in the cart for the shopping list of a week. */
@Serializable
data class ShoppingWeek(
    @Serializable(with = LocalDateIsoSerializer::class)
    val weekStart: LocalDate,
    /** Keys of the foods of the program already taken. */
    val checked: List<String> = emptyList(),
    val extras: List<ShoppingExtra> = emptyList(),
) {
    val isEmpty: Boolean get() = checked.isEmpty() && extras.isEmpty()
}

@Serializable
data class Nutrition(
    val plans: List<MealPlan> = emptyList(),
    /** Sorted by date. */
    val schedule: List<PlanPeriod> = emptyList(),
    /** Sorted by date, only the days where something was noted. */
    val days: List<NutritionDay> = emptyList(),
    /** Whether the plan the app comes with was already added: it is never added again. */
    val seeded: Boolean = false,
    /** Sorted by week, only the recent weeks where something was checked or added. */
    val shopping: List<ShoppingWeek> = emptyList(),
) {
    fun plan(id: String): MealPlan? = plans.firstOrNull { it.id == id }

    fun day(date: LocalDate): NutritionDay = days.firstOrNull { it.date == date } ?: NutritionDay(date)

    fun shoppingWeek(weekStart: LocalDate): ShoppingWeek =
        shopping.firstOrNull { it.weekStart == weekStart } ?: ShoppingWeek(weekStart)
}

/** Stores days of the week by name ("MONDAY"), like the rest of the readable backups. */
object DayOfWeekSerializer : KSerializer<DayOfWeek> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("muscuapp.DayOfWeek", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DayOfWeek) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): DayOfWeek = DayOfWeek.valueOf(decoder.decodeString())
}
