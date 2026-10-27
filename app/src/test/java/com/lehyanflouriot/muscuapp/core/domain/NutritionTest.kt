package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.data.DefaultMealPlan
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.FoodSwap
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.NutritionDay
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.WeightGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class NutritionTest {
    // Tuesday 27 October 2026, in the first week of the plan.
    private val today = LocalDate.of(2026, 10, 27)
    private val monday = today.startOfWeek()
    private val plan = DefaultMealPlan.create()
    private val nutrition = Nutrition(plans = listOf(plan), schedule = listOf(PlanPeriod(plan.id, monday)), seeded = true)

    @Test
    fun defaultPlanFollowsTheWeekOfThePdf() {
        assertEquals(DayOfWeek.entries, plan.days.map { it.day })
        assertEquals(listOf("Matin", "Déjeuner", "Pré-séance", "Post-séance", "Avant coucher"), plan.slots.map { it.name })
        assertEquals(
            listOf("Pectoraux", "Dos", "Quadriceps", "Repos", "Épaules", "Ischios", "Repos"),
            plan.days.map { it.label },
        )
        assertEquals(listOf(13, 13, 14, 10, 13, 14, 10), plan.days.map { it.waterGlasses })
        // 3 000 kcal on training days, a bit more before the hardest sessions, 2 550 when resting.
        val kcal = DayOfWeek.entries.map { buildNutritionDay(nutrition, monday.plusDays(it.ordinal.toLong()))!!.kcalTarget }
        assertEquals(listOf(3_000, 3_000, 3_150, 2_550, 3_000, 3_150, 2_550), kcal)
        assertEquals(155, buildNutritionDay(nutrition, monday)!!.proteinTarget)
        assertEquals(3_000..3_150, plan.kcalRange(training = true))
        assertEquals(2_550..2_550, plan.kcalRange(training = false))
        // Every food has its own id, and the creatine is taken every morning.
        val items = plan.days.flatMap { day -> day.meals.flatMap { it.items } }
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertTrue(plan.days.all { day -> day.meal("matin").items.any { it.name == "créatine" && it.quantity == "5 g" } })
        // A food is always written the same way, and the substitutions use these names.
        assertEquals(listOf("riz basmati", "pâtes complètes", "semoule"), plan.substitutesFor("pommes de terre"))
        assertEquals(10, plan.foodsNamed("huile d'olive"))
        assertEquals("huile d'olive", plan.foodNames().first())
        assertEquals(plan.foodNames().size, plan.foodNames().map(::shoppingKey).toSet().size)
        val mondayLunch = plan.day(DayOfWeek.MONDAY).meal("dejeuner").items
        assertEquals(listOf("riz basmati", "poulet", "brocolis", "huile d'olive"), mondayLunch.map { it.name })
        assertEquals(listOf("150 g", "130 g", "", "1,5 c. à s."), mondayLunch.map { it.quantity })
    }

    @Test
    fun dayViewCountsWhatWasEaten() {
        val lunch = plan.day(DayOfWeek.TUESDAY).meal("dejeuner").items
        val shake = plan.day(DayOfWeek.TUESDAY).meal("matin").items
        val day = NutritionDay(
            date = today,
            eaten = shake.map { it.id } + lunch.first().id,
            swaps = listOf(FoodSwap(lunch.first().id, "riz", "150 g")),
            water = 6,
        )
        val view = buildNutritionDay(nutrition.copy(days = listOf(day)), today)!!
        assertEquals("Dos", view.dayPlan.label)
        assertEquals(5, view.meals.size)
        assertTrue(view.meals[0].done)
        assertEquals(1, view.meals[1].eatenCount)
        assertEquals("riz", view.meals[1].foods[0].name)
        assertEquals("pâtes complètes", view.meals[1].foods[0].item.name)
        // The shake, and a quarter of the lunch.
        assertEquals(450 + 900 / 4.0, view.kcalEaten, 1e-9)
        assertEquals(20 + 40 / 4.0, view.proteinEaten, 1e-9)
        assertEquals("Déjeuner", view.nextMeal?.slot?.name)
        assertEquals(13, view.waterTarget)
        assertFalse(view.perfect)

        val everything = plan.day(DayOfWeek.TUESDAY).meals.flatMap { meal -> meal.items.map { it.id } }
        val perfect = buildNutritionDay(nutrition.copy(days = listOf(NutritionDay(today, everything, water = 13))), today)!!
        assertTrue(perfect.perfect)
        assertEquals(3_000.0, perfect.kcalEaten, 1e-9)
        assertNull(perfect.nextMeal)
    }

    @Test
    fun noPlanBeforeTheFirstOneStarts() {
        assertNull(buildNutritionDay(nutrition, monday.minusDays(1)))
        val week = nutritionWeek(nutrition, monday.minusDays(7))
        assertEquals(List(7) { null }, week.map { it.second })
        assertEquals(List(7) { 0.0 }, nutritionWeek(nutrition, monday).map { it.second })
    }

    @Test
    fun substitutesComeFromTheGroupsOfThePlan() {
        assertEquals(listOf("pâtes complètes", "semoule", "pommes de terre"), plan.substitutesFor("riz basmati"))
        assertEquals(listOf("riz basmati", "semoule", "pommes de terre"), plan.substitutesFor("Pâtes complètes"))
        assertEquals(listOf("dinde", "cabillaud"), plan.substitutesFor("poulet"))
        assertEquals(listOf("fromage blanc"), plan.substitutesFor("skyr"))
        assertEquals(emptyList<String>(), plan.substitutesFor("fromage frais"))
        assertEquals(emptyList<String>(), plan.substitutesFor("banane"))
        // A word inside another one does not count.
        assertEquals(emptyList<String>(), plan.substitutesFor("rizotto"))
        assertEquals("oeufs", normalizeFood("Œufs"))
    }

    private fun weights(days: Int, perWeek: Double, start: Double = 70.0) =
        (0 until days).map { BodyWeightEntry(today.minusDays((days - 1 - it).toLong()), start + it * perWeek / 7) }

    @Test
    fun guidanceFollowsTheRulesOfThePlan() {
        val goal = plan.weightGoal!!
        assertEquals(WeightAdvice.NEED_DATA, weightGuidance(goal, weights(3, 0.3), today).advice)
        // Weights that stopped two weeks ago say nothing about now.
        val old = weights(20, 0.3).map { it.copy(date = it.date.minusDays(14)) }
        assertEquals(WeightAdvice.NEED_DATA, weightGuidance(goal, old, today).advice)

        val onTrack = weightGuidance(goal, weights(14, 0.3, start = 68.0), today)
        assertEquals(WeightAdvice.ON_TRACK, onTrack.advice)
        assertEquals(0.3, onTrack.weeklyRate!!, 1e-9)
        assertTrue(onTrack.progress!! in 0.25..0.3)
        // About 6,6 kg to go at 0,3 kg a week.
        assertTrue(onTrack.eta!! in today.plusWeeks(20)..today.plusWeeks(24))

        assertEquals(WeightAdvice.BELOW, weightGuidance(goal, weights(14, 0.0), today).advice)
        assertEquals(WeightAdvice.BELOW, weightGuidance(goal, weights(14, -0.3), today).advice)
        assertNull(weightGuidance(goal, weights(14, -0.3), today).eta)
        assertEquals(WeightAdvice.ABOVE, weightGuidance(goal, weights(14, 0.6), today).advice)

        // Losing weight works the same way.
        val cut = WeightGoal(start = 80.0, target = 72.0, minWeeklyChange = -0.7, maxWeeklyChange = -0.3)
        assertEquals(WeightAdvice.ON_TRACK, weightGuidance(cut, weights(14, -0.5, start = 78.0), today).advice)
        assertEquals(WeightAdvice.ABOVE, weightGuidance(cut, weights(14, -0.1, start = 78.0), today).advice)
        assertTrue(weightGuidance(cut, weights(14, -0.5, start = 78.0), today).progress!! > 0.2)
    }

    @Test
    fun programsKnowWhereTheyStand() {
        val next = plan.copy(id = "next", name = "Sèche", versionOf = null)
        val kept = plan.copy(id = "kept", versionOf = plan.id)
        val old = plan.copy(id = "old", name = "Ancien")
        val unused = plan.copy(id = "unused", name = "Brouillon")
        val history = Nutrition(
            plans = listOf(plan, next, kept, old, unused),
            schedule = listOf(
                PlanPeriod("old", monday.minusWeeks(8)),
                PlanPeriod("kept", monday.minusWeeks(2)),
                PlanPeriod(plan.id, today),
                PlanPeriod("next", today.plusWeeks(4)),
            ),
        )
        assertEquals(setOf(plan.id, "next", "old", "unused"), history.programs.map { it.id }.toSet())
        val infos = history.programInfos(today).associateBy { it.plan.id }
        assertEquals(ProgramStatus.ACTIVE, infos.getValue(plan.id).status)
        // The kept copy counts as the same program: it is followed since two weeks.
        assertEquals(monday.minusWeeks(2), infos.getValue(plan.id).date)
        assertEquals(ProgramStatus.PLANNED, infos.getValue("next").status)
        assertEquals(today.plusWeeks(4), infos.getValue("next").date)
        assertEquals(ProgramStatus.PAST, infos.getValue("old").status)
        assertEquals(monday.minusWeeks(2).minusDays(1), infos.getValue("old").date)
        assertEquals(ProgramStatus.UNUSED, infos.getValue("unused").status)
        assertFalse(infos.containsKey("kept"))
        assertEquals(plan.id, history.programAt(monday.minusWeeks(1))?.id)
        assertEquals("kept", history.planAt(monday.minusWeeks(1))?.id)
    }
}
