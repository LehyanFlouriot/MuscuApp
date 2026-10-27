package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.domain.buildNutritionDay
import com.lehyanflouriot.muscuapp.core.domain.foodsNamed
import com.lehyanflouriot.muscuapp.core.domain.planAt
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.programs
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.NutritionDay
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.ShoppingExtra
import com.lehyanflouriot.muscuapp.core.model.ShoppingWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionRepositoryTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        var writes = 0
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
            writes++
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    private var ids = 0

    // Tuesday 27 October 2026.
    private var today = LocalDate.of(2026, 10, 27)
    private val monday = LocalDate.of(2026, 10, 26)

    private fun TestScope.repository(storage: FakeStorage): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(storage, backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    private val WorkoutRepository.nutrition: Nutrition get() = data.value!!.nutrition

    private fun Nutrition.items(date: LocalDate, slotId: String) =
        planAt(date)!!.day(date.dayOfWeek).meal(slotId).items

    @Test
    fun thePlanOfThePdfIsAddedOnceFromThisWeek() = runTest {
        val storage = FakeStorage()
        val repo = repository(storage)
        val nutrition = repo.nutrition
        assertTrue(nutrition.seeded)
        assertEquals(listOf(DefaultMealPlan.ID), nutrition.plans.map { it.id })
        assertEquals(listOf(PlanPeriod(DefaultMealPlan.ID, monday)), nutrition.schedule)
        assertEquals("Dos", buildNutritionDay(nutrition, today)!!.dayPlan.label)
        assertEquals(1, storage.writes)

        // Data of an older version gets it too, and keeps everything else.
        val old = FakeStorage("""{"schemaVersion": 1, "restPresets": [{"id": "p", "seconds": 60}]}""")
        val upgraded = repository(old)
        assertEquals(DefaultMealPlan.ID, upgraded.nutrition.plans.single().id)
        assertEquals(60, upgraded.data.value!!.restPresets.single().seconds)
        assertEquals(1, old.writes)

        // Never again once it was added, even when the user removed it.
        val removed = AppData.initial().copy(nutrition = Nutrition(seeded = true))
        val stored = FakeStorage(AppJson.encode(removed))
        assertTrue(repository(stored).nutrition.plans.isEmpty())
        assertEquals(0, stored.writes)
    }

    @Test
    fun foodsAndWaterAreNotedPerDay() = runTest {
        val storage = FakeStorage()
        val repo = repository(storage)
        val shake = repo.nutrition.items(today, "matin")
        repo.toggleFood(today, shake[0].id)
        repo.toggleFood(today, shake[1].id)
        repo.toggleFood(today, shake[1].id)
        assertEquals(listOf(shake[0].id), repo.nutrition.day(today).eaten)

        repo.setMealEaten(today, "matin", eaten = true)
        assertTrue(buildNutritionDay(repo.nutrition, today)!!.meals[0].done)
        repo.setMealEaten(today, "matin", eaten = false)
        assertTrue(repo.nutrition.day(today).eaten.isEmpty())

        repo.setWater(today, 5)
        repo.setWater(today.minusDays(1), 99)
        assertEquals(5, repo.nutrition.day(today).water)
        assertEquals(30, repo.nutrition.day(today.minusDays(1)).water)
        repo.setWater(today.minusDays(1), 0)
        // Days where nothing is noted are not kept.
        assertEquals(listOf(today), repo.nutrition.days.map { it.date })

        runCurrent()
        val reloaded = repository(FakeStorage(storage.content)).nutrition
        assertEquals(5, reloaded.day(today).water)
        assertEquals(repo.nutrition.plans, reloaded.plans)
    }

    @Test
    fun swapsOnlyChangeTheDay() = runTest {
        val repo = repository(FakeStorage())
        val pasta = repo.nutrition.items(today, "dejeuner").first()
        assertEquals("pâtes complètes", pasta.name)
        repo.swapFood(today, pasta.id, " riz ", "150 g")
        val view = buildNutritionDay(repo.nutrition, today)!!
        assertEquals("riz", view.meals[1].foods[0].name)
        assertEquals("pâtes complètes", buildNutritionDay(repo.nutrition, today.plusWeeks(1))!!.meals[1].foods[0].name)
        // Coming back to the planned food removes the swap.
        repo.swapFood(today, pasta.id, "pâtes complètes", "150 g")
        assertTrue(repo.nutrition.day(today).swaps.isEmpty())
        repo.swapFood(today, pasta.id, "semoule", "150 g")
        repo.clearSwap(today, pasta.id)
        assertTrue(repo.nutrition.days.isEmpty())
    }

    @Test
    fun changingThePlanKeepsThePastAsItWas() = runTest {
        val repo = repository(FakeStorage())
        val planId = DefaultMealPlan.ID
        // Nothing noted yet: the plan simply changes.
        repo.updatePlan(planId) { it.copy(name = "Prise de masse") }
        assertEquals(1, repo.nutrition.plans.size)

        val mondayRice = repo.nutrition.items(monday, "dejeuner").first()
        repo.toggleFood(monday, mondayRice.id)
        val shake = repo.nutrition.items(today, "matin")
        repo.toggleFood(today, shake[0].id)

        // Monday's rice becomes 170 g from today on.
        repo.updateFood(planId, mondayRice.id, "riz basmati", "170 g", "")
        val nutrition = repo.nutrition
        assertEquals(2, nutrition.plans.size)
        assertEquals(1, nutrition.programs.size)
        assertEquals(listOf(monday, today), nutrition.schedule.map { it.from })
        // Last Monday keeps its 150 g and stays checked; next Monday has 170 g.
        assertEquals("150 g", nutrition.items(monday, "dejeuner").first().quantity)
        assertTrue(buildNutritionDay(nutrition, monday)!!.meals[1].foods[0].eaten)
        assertEquals("170 g", nutrition.items(monday.plusWeeks(1), "dejeuner").first().quantity)
        // Today was not lived yet: it follows the change and keeps what was checked.
        assertEquals(planId, nutrition.planAt(today)!!.id)
        assertTrue(buildNutritionDay(nutrition, today)!!.meals[0].foods[0].eaten)
        assertEquals(planId, nutrition.programAt(monday)!!.id)

        // A second change the same day does not copy the plan again.
        repo.addFood(planId, DayOfWeek.MONDAY, "dejeuner", "citron", "", "")
        assertEquals(2, repo.nutrition.plans.size)
        assertEquals(5, repo.nutrition.items(monday.plusWeeks(1), "dejeuner").size)
        assertEquals(4, repo.nutrition.items(monday, "dejeuner").size)
        // The copy kept for the past never changes.
        val keptId = repo.nutrition.planAt(monday)!!.id
        repo.updatePlan(keptId) { it.copy(name = "Autre") }
        assertEquals("Prise de masse", repo.nutrition.plan(keptId)!!.name)

        // The program is still followed since Monday.
        val info = repo.nutrition.programInfos(today).single()
        assertEquals(ProgramStatus.ACTIVE, info.status)
        assertEquals(monday, info.date)
    }

    @Test
    fun newProgramsStartOnTheChosenDay() = runTest {
        val repo = repository(FakeStorage())
        val nextMonday = monday.plusWeeks(1)
        val copyId = repo.createProgram("  Sèche ", copyOf = DefaultMealPlan.ID, start = nextMonday)
        val blankId = repo.createProgram("", copyOf = null, start = null)
        var nutrition = repo.nutrition
        assertEquals("Sèche", nutrition.plan(copyId)!!.name)
        assertEquals("Programme", nutrition.plan(blankId)!!.name)
        assertEquals(4, nutrition.plan(blankId)!!.slots.size)
        assertEquals(DefaultMealPlan.ID, nutrition.planAt(today)!!.id)
        assertEquals(copyId, nutrition.planAt(nextMonday)!!.id)
        // The copy has the same foods.
        assertEquals(nutrition.items(today, "matin"), nutrition.items(nextMonday.plusDays(1), "matin"))
        val statuses = nutrition.programInfos(today).associate { it.plan.id to it.status }
        assertEquals(ProgramStatus.PLANNED, statuses[copyId])
        assertEquals(ProgramStatus.UNUSED, statuses[blankId])

        // A start can never be in the past.
        repo.startProgram(blankId, monday.minusWeeks(3))
        assertEquals(blankId, repo.nutrition.planAt(today)!!.id)
        assertEquals(DefaultMealPlan.ID, repo.nutrition.planAt(monday)!!.id)
        repo.startProgram(DefaultMealPlan.ID, today)
        assertEquals(DefaultMealPlan.ID, repo.nutrition.planAt(today)!!.id)

        repo.cancelProgramStart(copyId, nextMonday)
        assertEquals(DefaultMealPlan.ID, repo.nutrition.planAt(nextMonday)!!.id)

        // A program can be deleted unless it is followed today or was followed on noted days.
        assertFalse(repo.deleteProgram(DefaultMealPlan.ID))
        assertTrue(repo.deleteProgram(copyId))
        assertTrue(repo.deleteProgram(blankId))
        nutrition = repo.nutrition
        assertEquals(listOf(DefaultMealPlan.ID), nutrition.plans.map { it.id })
        assertEquals(listOf(monday), nutrition.schedule.map { it.from })
    }

    @Test
    fun theWeekCanBeShapedFreely() = runTest {
        val repo = repository(FakeStorage())
        val planId = DefaultMealPlan.ID
        repo.copyDay(planId, DayOfWeek.MONDAY, listOf(DayOfWeek.TUESDAY, DayOfWeek.MONDAY))
        val plan = repo.nutrition.plan(planId)!!
        val mondayMeals = plan.day(DayOfWeek.MONDAY).meals
        val tuesdayMeals = plan.day(DayOfWeek.TUESDAY).meals
        assertEquals(mondayMeals.map { m -> m.items.map { it.name } }, tuesdayMeals.map { m -> m.items.map { it.name } })
        assertNotEquals(mondayMeals.first().items.first().id, tuesdayMeals.first().items.first().id)
        // The label of the day stays.
        assertEquals("Dos", plan.day(DayOfWeek.TUESDAY).label)

        repo.addSlot(planId, "Collation", "", MealMoment.SNACK, kcal = 200, protein = null)
        val slot = repo.nutrition.plan(planId)!!.slots.last()
        repo.addFood(planId, DayOfWeek.FRIDAY, slot.id, "pomme", "1", "")
        assertEquals("pomme", buildNutritionDay(repo.nutrition, today.plusDays(3))!!.meals.last().foods.single().name)
        repo.updatePlan(planId) { it.moveSlot(slot.id, -10) }
        assertEquals(slot.id, repo.nutrition.plan(planId)!!.slots.first().id)
        repo.updatePlan(planId) { it.removeSlot(slot.id) }
        assertNull(repo.nutrition.plan(planId)!!.slot(slot.id))
        assertTrue(repo.nutrition.plan(planId)!!.days.none { day -> day.meals.any { it.slotId == slot.id } })

        repo.addNote(planId, "Le sel", "Sans excès.")
        assertEquals("Le sel", repo.nutrition.plan(planId)!!.notes.last().title)
    }

    @Test
    fun theShoppingCartIsKeptPerWeek() = runTest {
        val storage = FakeStorage()
        val repo = repository(storage)
        // Any day of the week opens the list of its week.
        repo.toggleShoppingItem(today, "lait")
        repo.toggleShoppingItem(monday, "riz")
        repo.toggleShoppingItem(monday, "riz")
        repo.addShoppingExtra(monday, "  éponges ")
        repo.addShoppingExtra(monday, " ")
        var week = repo.nutrition.shoppingWeek(monday)
        assertEquals(listOf("lait"), week.checked)
        assertEquals(listOf("éponges"), week.extras.map { it.name })
        repo.toggleShoppingExtra(monday, week.extras.single().id)
        assertTrue(repo.nutrition.shoppingWeek(monday).extras.single().checked)
        // Next week starts with an empty cart.
        assertTrue(repo.nutrition.shoppingWeek(monday.plusWeeks(1)).isEmpty)

        runCurrent()
        assertEquals(repo.nutrition.shopping, repository(FakeStorage(storage.content)).nutrition.shopping)

        repo.clearShoppingChecks(monday)
        week = repo.nutrition.shoppingWeek(monday)
        assertTrue(week.checked.isEmpty())
        assertFalse(week.extras.single().checked)
        repo.removeShoppingExtra(monday, week.extras.single().id)
        assertTrue(repo.nutrition.shopping.isEmpty())

        // The lists of long ago are forgotten.
        repo.toggleShoppingItem(monday.minusWeeks(20), "riz")
        repo.toggleShoppingItem(monday, "riz")
        assertEquals(listOf(monday), repo.nutrition.shopping.map { it.weekStart })
    }

    /** The plan as the first version of the app added it, before each food had a single name. */
    private fun firstVersionPlan() = DefaultMealPlan.create().let { plan ->
        val oldNames = mapOf(
            "mon-post-seance-1" to "pâtes",
            "mon-post-seance-2" to "bœuf 5 %",
            "mon-post-seance-5" to "huile",
            "tue-pre-seance-2" to "dinde",
            "fri-post-seance-4" to "wok de légumes",
            "sat-pre-seance-4" to "grosse banane",
        )
        plan.copy(
            days = plan.days.map { day ->
                day.copy(
                    meals = day.meals.map { meal ->
                        meal.copy(
                            items = meal.items.map { item ->
                                oldNames[item.id]?.let { item.copy(name = it, note = "") } ?: item
                            },
                        )
                    },
                )
            },
            substitutions = listOf(
                listOf("riz", "pâtes", "semoule", "pommes de terre"),
                listOf("poulet", "dinde", "cabillaud"),
                listOf("fromage blanc", "skyr"),
            ),
        )
    }

    @Test
    fun savedPlansGetOneNamePerFood() = runTest {
        val old = firstVersionPlan()
        // A food the user renamed in the meantime keeps its name.
        val edited = old.updateFood("sun-dejeuner-4") { it.copy(name = "huile de colza") }
        val saved = AppData.initial().copy(
            schemaVersion = 1,
            nutrition = Nutrition(
                plans = listOf(edited),
                schedule = listOf(PlanPeriod(edited.id, monday)),
                days = listOf(NutritionDay(monday, eaten = listOf("mon-post-seance-5"))),
                seeded = true,
                shopping = listOf(ShoppingWeek(monday, checked = listOf("huile", "lait"))),
            ),
        )
        val storage = FakeStorage(AppJson.encode(saved))
        val repo = repository(storage)
        val plan = repo.nutrition.plans.single()
        fun item(id: String) = plan.days.flatMap { day -> day.meals.flatMap { it.items } }.single { it.id == id }
        assertEquals("pâtes complètes", item("mon-post-seance-1").name)
        assertEquals("bœuf haché 5 %", item("mon-post-seance-2").name)
        assertEquals("huile d'olive", item("mon-post-seance-5").name)
        assertEquals("blanc de dinde", item("tue-pre-seance-2").name)
        assertEquals("En tranches", item("tue-pre-seance-2").note)
        assertEquals("légumes variés", item("fri-post-seance-4").name)
        assertEquals("En wok", item("fri-post-seance-4").note)
        assertEquals("banane", item("sat-pre-seance-4").name)
        assertEquals("huile de colza", item("sun-dejeuner-4").name)
        assertEquals("dinde", item("wed-dejeuner-2").name)
        assertEquals(DefaultMealPlan.SUBSTITUTIONS, plan.substitutions)
        // What was eaten or put in the cart stays.
        assertEquals(listOf("mon-post-seance-5"), repo.nutrition.day(monday).eaten)
        assertEquals(listOf("huile d'olive", "lait"), repo.nutrition.shoppingWeek(monday).checked)
        // The document is written again, up to date, and not changed a second time.
        assertEquals(1, storage.writes)
        val reloaded = FakeStorage(storage.content)
        assertEquals(repo.nutrition, repository(reloaded).nutrition)
        assertEquals(0, reloaded.writes)
    }

    @Test
    fun aFoodCanBeRenamedEverywhere() = runTest {
        val repo = repository(FakeStorage())
        repo.renameFood(DefaultMealPlan.ID, "Huile d'olive", "huile de colza")
        val plan = repo.nutrition.plan(DefaultMealPlan.ID)!!
        assertEquals(0, plan.foodsNamed("huile d'olive"))
        assertEquals(10, plan.foodsNamed("huile de colza"))
        assertEquals(7, plan.foodsNamed("lait"))
        repo.renameFood(DefaultMealPlan.ID, "pâtes complètes", "pâtes")
        assertTrue(repo.nutrition.plan(DefaultMealPlan.ID)!!.substitutions.first().contains("pâtes"))
    }

    @Test
    fun sanitizeDropsBrokenNutrition() {
        val plan = DefaultMealPlan.create()
        val mondayShake = plan.day(DayOfWeek.MONDAY).meal("matin").items
        val raw = AppData(
            nutrition = Nutrition(
                plans = listOf(plan, plan.copy(name = "Doublon")),
                schedule = listOf(PlanPeriod("unknown", monday.minusWeeks(1)), PlanPeriod(plan.id, monday)),
                days = listOf(
                    NutritionDay(monday, eaten = listOf(mondayShake[0].id, "unknown", mondayShake[0].id), water = 40),
                    NutritionDay(monday, eaten = listOf(mondayShake[1].id)),
                    NutritionDay(monday.minusWeeks(1), eaten = listOf(mondayShake[0].id)),
                ),
                seeded = true,
            ),
        )
        val nutrition = WorkoutRepository.sanitize(raw).nutrition
        assertEquals(listOf("Prise de masse 66 → 75 kg"), nutrition.plans.map { it.name })
        val shopping = WorkoutRepository.sanitize(
            raw.copy(
                nutrition = raw.nutrition.copy(
                    shopping = listOf(
                        ShoppingWeek(monday.plusDays(2), checked = listOf("riz", "riz", " ")),
                        ShoppingWeek(monday, checked = listOf("lait")),
                        ShoppingWeek(monday.plusWeeks(1), extras = listOf(ShoppingExtra("e", " "))),
                    ),
                ),
            ),
        ).nutrition.shopping
        // One list per week, starting on Monday, without empty names.
        assertEquals(listOf(ShoppingWeek(monday, checked = listOf("riz"))), shopping)
        assertEquals(listOf(PlanPeriod(plan.id, monday)), nutrition.schedule)
        // One record per day, with known foods only; nothing can be noted before the plan.
        assertEquals(listOf(NutritionDay(monday, eaten = listOf(mondayShake[0].id), water = 30)), nutrition.days)
        assertEquals(nutrition, WorkoutRepository.sanitize(AppJson.decode(AppJson.encode(raw))).nutrition)
    }
}
