package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.data.DefaultMealPlan
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.FoodSwap
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.NutritionDay
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ShoppingTest {
    // The week of Monday 26 October 2026.
    private val monday = LocalDate.of(2026, 10, 26)
    private val sunday = monday.plusDays(6)
    private val plan = DefaultMealPlan.create()
    private val nutrition = Nutrition(plans = listOf(plan), schedule = listOf(PlanPeriod(plan.id, monday)), seeded = true)

    private fun String.plain() = replace('\u202F', ' ').replace('\u00A0', ' ')

    @Test
    fun quantitiesAreRead() {
        assertEquals(Amount(150.0, AmountUnit.GRAM), parseQuantity("150 g"))
        assertEquals(Amount(1_200.0, AmountUnit.GRAM), parseQuantity("1,2 kg"))
        assertEquals(Amount(300.0, AmountUnit.MILLILITER), parseQuantity("300 ml"))
        assertEquals(Amount(250.0, AmountUnit.MILLILITER), parseQuantity("25 cl"))
        assertEquals(Amount(1_500.0, AmountUnit.MILLILITER), parseQuantity("1.5 L"))
        assertEquals(Amount(2.0, AmountUnit.PIECE), parseQuantity("2"))
        assertEquals(Amount(0.5, AmountUnit.PIECE), parseQuantity("1/2"))
        assertEquals(Amount(1.5, AmountUnit.TABLESPOON), parseQuantity("1,5 c. à s."))
        assertEquals(Amount(1.0, AmountUnit.TABLESPOON), parseQuantity("1 càs"))
        assertEquals(Amount(1.0, AmountUnit.TEASPOON), parseQuantity("1 c. à c."))
        assertEquals(Amount(2.0, AmountUnit.TEASPOON), parseQuantity("2 cuillères à café"))
        assertEquals(Amount(2.0, AmountUnit.OTHER, "tranches"), parseQuantity("2 tranches"))
        assertNull(parseQuantity(""))
        assertNull(parseQuantity("une poignée"))
        assertEquals(85.0, spoonsVolume(listOf(Amount(5.0, AmountUnit.TABLESPOON), Amount(2.0, AmountUnit.TEASPOON))), 1e-9)
    }

    @Test
    fun foodsGoToTheirAisle() {
        assertEquals(Aisle.PRODUCE, aisleOf("banane"))
        assertEquals(Aisle.PRODUCE, aisleOf("Brocolis"))
        assertEquals(Aisle.PRODUCE, aisleOf("pommes de terre"))
        assertEquals(Aisle.PRODUCE, aisleOf("haricots verts"))
        assertEquals(Aisle.PRODUCE, aisleOf("wok de légumes"))
        assertEquals(Aisle.MEAT_FISH, aisleOf("steak haché 5 %"))
        assertEquals(Aisle.MEAT_FISH, aisleOf("œufs"))
        assertEquals(Aisle.DAIRY, aisleOf("fromage blanc"))
        assertEquals(Aisle.DAIRY, aisleOf("skyr"))
        assertEquals(Aisle.STARCHES, aisleOf("flocons d'avoine"))
        assertEquals(Aisle.STARCHES, aisleOf("pâtes complètes"))
        assertEquals(Aisle.STARCHES, aisleOf("pois chiches cuits"))
        // The longest name wins: no butter in the dairy aisle for peanut butter.
        assertEquals(Aisle.GROCERY, aisleOf("beurre de cacahuète"))
        assertEquals(Aisle.GROCERY, aisleOf("sauce tomate"))
        assertEquals(Aisle.GROCERY, aisleOf("raisins secs"))
        assertEquals(Aisle.GROCERY, aisleOf("huile d'olive"))
        assertEquals(Aisle.SUPPLEMENTS, aisleOf("créatine"))
        assertEquals(Aisle.OTHER, aisleOf("éponges"))
        assertEquals(shoppingKey("Œufs"), shoppingKey("oeuf"))
    }

    @Test
    fun theWeekOfThePlanIsAddedUp() {
        val list = buildShoppingList(nutrition, monday, sunday)
        val items = list.items.associateBy { it.name }
        fun amounts(name: String) = Fmt.amounts(items.getValue(name).amounts).plain()
        assertEquals("2,1 L", amounts("lait"))
        assertEquals("420 g", amounts("flocons d'avoine"))
        assertEquals("35 g", amounts("créatine"))
        assertEquals(DayOfWeek.entries, items.getValue("créatine").days)
        assertEquals("6", amounts("banane"))
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            items.getValue("banane").days,
        )
        // Each food is bought under one name: no "riz" next to "riz basmati", no "huile" next to "huile d'olive".
        assertEquals("780 g", amounts("riz basmati"))
        assertEquals("12,5 c. à s.", amounts("huile d'olive"))
        assertEquals("390 g", amounts("pâtes complètes"))
        assertEquals("370 g", amounts("pain complet"))
        assertEquals("280 g", amounts("bœuf haché 5 %"))
        assertEquals("280 g", amounts("dinde"))
        assertEquals("40 g", amounts("blanc de dinde"))
        assertEquals("40 g + 5 c. à s. + 2 c. à c.", amounts("miel"))
        assertEquals("1,05 kg", amounts("skyr"))
        assertEquals("1,7 kg", amounts("fromage blanc"))
        assertEquals("540 g", amounts("poulet"))
        assertEquals("5", amounts("œufs"))
        val doubles = listOf(
            "huile", "riz", "pâtes", "pain", "bœuf 5 %", "steak haché 5 %", "grosse banane", "miel ou confiture",
            "wok de légumes", "poêlée de légumes", "légumes couscous", "pois chiches cuits",
        )
        assertTrue(doubles.none { it in items })
        // Foods without a quantity tell for how many meals they are.
        assertEquals(2, items.getValue("brocolis").unmeasured)
        assertTrue(items.getValue("brocolis").amounts.isEmpty())
        assertEquals(3, items.getValue("légumes variés").unmeasured)
        // Aisles come in the order of the supermarket.
        assertEquals(Aisle.entries.filter { it != Aisle.OTHER }, list.aisles.map { it.first })
        assertEquals(list.items.size, list.items.map { it.key }.toSet().size)
    }

    @Test
    fun swapsAndDaysAreTakenIntoAccount() {
        val tuesday = monday.plusDays(1)
        val pasta = plan.day(DayOfWeek.TUESDAY).meal("dejeuner").items.first()
        val swapped = nutrition.copy(days = listOf(NutritionDay(tuesday, swaps = listOf(FoodSwap(pasta.id, "riz basmati", "150 g")))))
        val items = buildShoppingList(swapped, monday, sunday).items.associateBy { it.name }
        assertEquals("930 g", Fmt.amounts(items.getValue("riz basmati").amounts).plain())
        assertEquals("240 g", Fmt.amounts(items.getValue("pâtes complètes").amounts).plain())

        // From Thursday on, only what is left to eat.
        val rest = buildShoppingList(nutrition, monday.plusDays(3), sunday).items.associateBy { it.name }
        assertEquals("1,2 L", Fmt.amounts(rest.getValue("lait").amounts).plain())
        assertNull(rest["brocolis"])
        // No plan, nothing to buy.
        assertTrue(buildShoppingList(nutrition, monday.minusWeeks(1), sunday.minusWeeks(1)).items.isEmpty())
    }

    @Test
    fun theListIsForNextWeekFromSaturday() {
        assertEquals(monday, defaultShoppingWeek(monday))
        assertEquals(monday, defaultShoppingWeek(monday.plusDays(4)))
        assertEquals(monday.plusWeeks(1), defaultShoppingWeek(monday.plusDays(5)))
        assertEquals(monday.plusWeeks(1), defaultShoppingWeek(sunday))
    }
}
