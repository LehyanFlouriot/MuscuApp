package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.DayPlan
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.PlannedMeal
import com.lehyanflouriot.muscuapp.core.model.WeightGoal
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/**
 * The meal plan the app comes with: a typical week of a bulk from 66 to 75 kg, with light
 * mornings, five meals a day and the tips that go with it. It can then be changed freely.
 */
object DefaultMealPlan {
    const val ID = "plan-prise-de-masse"

    private const val MORNING = "matin"
    private const val LUNCH = "dejeuner"
    private const val PRE = "pre-seance"
    private const val POST = "post-seance"
    private const val NIGHT = "coucher"

    private class Food(val quantity: String, val name: String, val note: String = "")

    private class Meal(val slotId: String, val foods: List<Food>, val kcal: Int? = null, val note: String = "")

    private fun meal(slotId: String, vararg foods: Food, kcal: Int? = null, note: String = "") =
        Meal(slotId, foods.toList(), kcal, note)

    private fun food(quantity: String, name: String, note: String = "") = Food(quantity, name, note)

    private val creatine = food("5 g", "créatine", "Tous les jours, même au repos")

    /** With the names of the foods of the plan, so that a replaced food keeps one name in the shopping list. */
    internal val SUBSTITUTIONS = listOf(
        listOf("riz basmati", "pâtes complètes", "semoule", "pommes de terre"),
        listOf("poulet", "dinde", "cabillaud"),
        listOf("fromage blanc", "skyr"),
    )

    private fun day(day: DayOfWeek, label: String, training: Boolean, water: Int, vararg meals: Meal): DayPlan {
        val code = day.name.take(3).lowercase()
        return DayPlan(
            day = day,
            label = label,
            training = training,
            waterGlasses = water,
            meals = meals.map { meal ->
                PlannedMeal(
                    slotId = meal.slotId,
                    items = meal.foods.mapIndexed { index, food ->
                        FoodItem(id = "$code-${meal.slotId}-${index + 1}", name = food.name, quantity = food.quantity, note = food.note)
                    },
                    kcal = meal.kcal,
                    note = meal.note,
                )
            },
        )
    }

    fun create(createdAt: Long = 0L): MealPlan = MealPlan(
        id = ID,
        name = "Prise de masse 66 → 75 kg",
        info = "Entraînement 3 000 kcal · repos 2 550 kcal · 155 g de protéines. " +
            "Féculents, légumineuses, viandes et poissons : poids cru.",
        slots = listOf(
            MealSlot(MORNING, "Matin", "Shake, à boire", MealMoment.MORNING, kcal = 450, protein = 20),
            MealSlot(LUNCH, "Déjeuner", "Le gros repas", MealMoment.LUNCH, kcal = 900, protein = 40),
            MealSlot(PRE, "Pré-séance", "2 h avant", MealMoment.PRE_WORKOUT, kcal = 550, protein = 25),
            MealSlot(POST, "Post-séance", "Dans les 2 h", MealMoment.POST_WORKOUT, kcal = 800, protein = 45),
            MealSlot(NIGHT, "Avant coucher", "30 min avant", MealMoment.NIGHT, kcal = 300, protein = 25),
        ),
        days = listOf(
            day(
                MONDAY, "Pectoraux", training = true, water = 13,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("40 g", "flocons d'avoine"), food("1", "banane"),
                    food("10 g", "beurre de cacahuète"), creatine,
                ),
                meal(LUNCH, food("150 g", "riz basmati"), food("130 g", "poulet"), food("", "brocolis"), food("1,5 c. à s.", "huile d'olive")),
                meal(PRE, food("200 g", "fromage blanc"), food("60 g", "flocons d'avoine"), food("1 c. à s.", "miel"), food("1", "pomme")),
                meal(
                    POST,
                    food("120 g", "pâtes complètes"), food("130 g", "bœuf haché 5 %"), food("", "sauce tomate"),
                    food("50 g", "pain complet"), food("1 c. à s.", "huile d'olive"),
                ),
                meal(NIGHT, food("200 g", "skyr"), food("25 g", "amandes")),
            ),
            day(
                TUESDAY, "Dos", training = true, water = 13,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("40 g", "flocons d'avoine"), food("150 g", "fruits rouges"),
                    food("15 g", "amandes"), creatine,
                ),
                meal(LUNCH, food("150 g", "pâtes complètes"), food("130 g", "thon"), food("", "courgettes"), food("1,5 c. à s.", "huile d'olive")),
                meal(
                    PRE,
                    food("100 g", "pain complet"), food("40 g", "blanc de dinde", "En tranches"), food("50 g", "fromage frais"),
                    food("1", "banane"),
                ),
                meal(
                    POST,
                    food("120 g", "riz basmati"), food("170 g", "saumon"), food("", "brocolis"), food("", "citron"),
                    note = "Sans huile, le saumon suffit",
                ),
                meal(NIGHT, food("200 g", "fromage blanc"), food("20 g", "noix"), food("1 c. à c.", "miel")),
            ),
            day(
                WEDNESDAY, "Quadriceps", training = true, water = 14,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("50 g", "flocons d'avoine"), food("1", "banane"),
                    food("10 g", "cacao non sucré"), food("1 c. à c.", "miel"), creatine,
                ),
                meal(LUNCH, food("150 g", "semoule"), food("130 g", "dinde"), food("", "ratatouille"), food("1,5 c. à s.", "huile d'olive")),
                meal(
                    PRE,
                    food("250 g", "fromage blanc"), food("60 g", "flocons d'avoine"), food("2 c. à s.", "miel"), food("1", "banane"),
                    food("20 g", "raisins secs"),
                    kcal = 700, note = "Séance la plus dure",
                ),
                meal(
                    POST,
                    food("400 g", "pommes de terre"), food("150 g", "bœuf haché 5 %"), food("", "haricots verts"),
                    food("60 g", "pain complet"), food("1 c. à s.", "huile d'olive"),
                ),
                meal(NIGHT, food("250 g", "skyr"), food("20 g", "beurre de cacahuète")),
            ),
            day(
                THURSDAY, "Repos", training = false, water = 10,
                meal(MORNING, food("300 ml", "lait"), food("40 g", "flocons d'avoine"), food("1", "poire"), food("15 g", "noix"), creatine),
                meal(
                    LUNCH,
                    food("120 g", "riz basmati"), food("150 g", "cabillaud"), food("", "haricots verts"),
                    food("1,5 c. à s.", "huile d'olive"),
                    kcal = 800, note = "Féculents réduits",
                ),
                meal(PRE, food("200 g", "skyr"), food("1", "fruit", "Au choix"), kcal = 250, note = "Simple collation"),
                meal(
                    POST,
                    food("120 g", "pâtes complètes"), food("130 g", "poulet"), food("", "courgettes"), food("30 g", "parmesan"),
                    kcal = 750,
                ),
                meal(NIGHT, food("200 g", "fromage blanc"), food("25 g", "noisettes")),
            ),
            day(
                FRIDAY, "Épaules", training = true, water = 13,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("40 g", "flocons d'avoine"), food("1", "banane"),
                    food("10 g", "purée d'amande"), creatine,
                ),
                meal(
                    LUNCH,
                    food("500 g", "patate douce"), food("130 g", "poulet"), food("", "poivrons"), food("60 g", "pain complet"),
                    food("1 c. à s.", "huile d'olive"),
                ),
                meal(PRE, food("200 g", "fromage blanc"), food("60 g", "muesli"), food("1 c. à s.", "miel"), food("1", "poire")),
                meal(
                    POST,
                    food("120 g", "riz basmati"), food("150 g", "cabillaud"), food("2", "œufs"), food("", "légumes variés", "En wok"),
                    food("1 c. à s.", "huile d'olive"),
                ),
                meal(NIGHT, food("200 g", "skyr"), food("25 g", "amandes"), food("", "cannelle")),
            ),
            day(
                SATURDAY, "Ischios", training = true, water = 14,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("50 g", "flocons d'avoine"), food("150 g", "mangue"),
                    food("1 c. à s.", "miel"), creatine,
                ),
                meal(LUNCH, food("150 g", "riz basmati"), food("130 g", "saumon"), food("", "épinards"), food("1 c. à s.", "huile d'olive")),
                meal(
                    PRE,
                    food("100 g", "pain complet"), food("40 g", "miel", "Ou confiture"), food("200 g", "fromage blanc"),
                    food("1", "banane", "Une grosse"),
                    kcal = 700, note = "Séance la plus dure",
                ),
                meal(
                    POST,
                    food("120 g", "semoule"), food("150 g", "poulet"), food("100 g", "pois chiches", "Pesés cuits"),
                    food("", "légumes variés", "Pour le couscous"),
                ),
                meal(NIGHT, food("250 g", "fromage blanc"), food("20 g", "noix de cajou")),
            ),
            day(
                SUNDAY, "Repos", training = false, water = 10,
                meal(
                    MORNING,
                    food("300 ml", "lait"), food("40 g", "flocons d'avoine"), food("1", "pomme"), food("", "cannelle"),
                    food("15 g", "noisettes"), creatine,
                ),
                meal(
                    LUNCH,
                    food("120 g", "lentilles"), food("3", "œufs"), food("", "salade"), food("1,5 c. à s.", "huile d'olive"),
                    kcal = 800, note = "Féculents réduits",
                ),
                meal(PRE, food("200 g", "fromage blanc"), food("20 g", "amandes"), kcal = 250, note = "Simple collation"),
                meal(
                    POST,
                    food("120 g", "riz basmati"), food("150 g", "dinde"), food("", "légumes variés", "En poêlée"),
                    food("40 g", "emmental"),
                    kcal = 750,
                ),
                meal(NIGHT, food("200 g", "skyr"), food("20 g", "purée d'amande")),
            ),
        ),
        glassMl = 250,
        waterHint = "2 au réveil · 3 dans la matinée · 2 au déjeuner · 2 avant la séance · 2 pendant · 2 le soir. " +
            "Le lait et le thé comptent, le café ne se déduit pas.",
        substitutions = SUBSTITUTIONS,
        notes = listOf(
            PlanNote(
                "note-creatine",
                "La créatine",
                "5 g par jour, tous les jours, dans le shake du matin. Le moment importe peu : c'est la saturation " +
                    "du muscle sur plusieurs semaines qui compte, pas le timing. Ne saute jamais un jour de repos.",
            ),
            PlanNote(
                "note-substitutions",
                "Les substitutions libres",
                "Riz ↔ pâtes ↔ semoule ↔ pommes de terre à poids sec équivalent. Poulet ↔ dinde ↔ cabillaud. " +
                    "Fromage blanc ↔ skyr. Garde les quantités, change les aliments.",
            ),
            PlanNote(
                "note-pilotage",
                "Le pilotage",
                "Pèse-toi 3 à 4 matins par semaine à jeun, fais la moyenne. Stable après 2 semaines : + 200 kcal " +
                    "de féculents. Plus de 0,4 kg par semaine : − 150 kcal.",
            ),
            PlanNote(
                "note-shake",
                "Le shake du matin",
                "20 g, c'est la limite basse pour déclencher la synthèse protéique (~25 g). Si tu le tolères, 15 g " +
                    "de whey dedans rend la journée parfaite. Sinon, laisse tel quel.",
            ),
            PlanNote(
                "note-matin",
                "Si tu t'entraînes le matin",
                "Le shake devient le repas pré-séance, bu 60 à 90 min avant, et le déjeuner devient le post-séance. " +
                    "Le contenu des repas ne change pas, seul l'ordre bouge.",
            ),
        ),
        weightGoal = WeightGoal(
            start = 66.0,
            target = 75.0,
            minWeeklyChange = 0.1,
            maxWeeklyChange = 0.4,
            belowAdvice = "+ 200 kcal de féculents",
            aboveAdvice = "− 150 kcal",
        ),
        createdAt = createdAt,
    )
}
