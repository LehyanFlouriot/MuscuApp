package com.lehyanflouriot.muscuapp.ui

import com.lehyanflouriot.muscuapp.core.data.DefaultMealPlan
import com.lehyanflouriot.muscuapp.core.data.HypertrophyProgram
import com.lehyanflouriot.muscuapp.core.domain.shoppingKey
import com.lehyanflouriot.muscuapp.core.domain.roundBodyWeight
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.FoodSwap
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.NutritionDay
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.ShoppingExtra
import com.lehyanflouriot.muscuapp.core.model.ShoppingWeek
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import java.time.LocalDate

/** A few weeks of realistic training, used by the screenshot tests. */
object SampleData {
    val bench = Exercise(id = "bench", name = "Développé couché", muscleGroup = MuscleGroup.CHEST, restSeconds = 120)
    val flyes = Exercise(id = "flyes", name = "Écarté à la poulie", muscleGroup = MuscleGroup.CHEST, restSeconds = 60)
    val pullUps = Exercise(id = "pull", name = "Tractions", muscleGroup = MuscleGroup.BACK, usesWeight = false, restSeconds = 120)
    val row = Exercise(id = "row", name = "Rowing barre", muscleGroup = MuscleGroup.BACK, restSeconds = 90)
    val squat = Exercise(id = "squat", name = "Squat", muscleGroup = MuscleGroup.LEGS, restSeconds = 180)
    val plank = Exercise(
        id = "plank", name = "Gainage", muscleGroup = MuscleGroup.ABS,
        usesWeight = false, measure = Measure.SECONDS, restSeconds = 45,
    )

    fun build(today: LocalDate): AppData {
        val thisWeek = today.startOfWeek()
        val entries = mutableListOf<WorkoutEntry>()
        var id = 0
        fun add(exercise: Exercise, date: LocalDate, vararg sets: WorkSet, note: String = "") {
            entries += WorkoutEntry(
                id = "e${id++}",
                exerciseId = exercise.id,
                date = date,
                sets = sets.toList(),
                note = note,
                createdAt = id.toLong(),
            )
        }
        for (back in 9 downTo 1) {
            val week = thisWeek.minusWeeks(back.toLong())
            val progress = 9 - back
            add(bench, week, *Array(4) { WorkSet(8 + progress % 3, 55.0 + progress * 2.5) })
            add(pullUps, week.plusDays(2), WorkSet(6 + progress / 2), WorkSet(6 + progress / 2), WorkSet(5 + progress / 2))
            if (back % 2 == 0) add(squat, week.plusDays(4), *Array(5) { WorkSet(5, 80.0 + progress * 5) })
            add(plank, week.plusDays(4), WorkSet(40 + progress * 5), WorkSet(40 + progress * 5), WorkSet(35 + progress * 5))
            add(row, week.plusDays(2), *Array(3) { WorkSet(10, 50.0 + progress * 2.5) })
        }
        add(bench, thisWeek, WorkSet(10, 77.5), WorkSet(10, 77.5), WorkSet(9, 77.5), WorkSet(8, 77.5), note = "Bonnes sensations")
        add(pullUps, thisWeek.plusDays(2), WorkSet(11), WorkSet(10), WorkSet(9))
        add(flyes, thisWeek, *Array(3) { WorkSet(12, 15.0) })
        return AppData.initial().copy(
            exercises = listOf(bench, flyes, pullUps, row, squat, plank),
            entries = entries,
            bodyWeights = bodyWeights(today),
        )
    }

    /**
     * The program of the app, followed since last week: last Wednesday's session, Monday and
     * Tuesday of this week, and the start of today's session (a Wednesday).
     */
    fun withProgram(today: LocalDate): AppData {
        val entries = mutableListOf<WorkoutEntry>()
        fun add(exerciseId: String, date: LocalDate, vararg reps: Int, weight: Double = 0.0) {
            entries += WorkoutEntry(
                id = "p${entries.size}",
                exerciseId = exerciseId,
                date = date,
                sets = reps.map { WorkSet(it, weight) },
                createdAt = entries.size.toLong(),
            )
        }
        val lastWednesday = today.minusWeeks(1)
        add("ex-squat-bulgare", lastWednesday, 10, 10, 9, 9)
        add("ex-sissy-squat-shrimp-squat", lastWednesday, 9, 8, 8)
        add("ex-pompes-archer-diamant", lastWednesday, 10, 9, 9)
        add("ex-rowing-australien", lastWednesday, 12, 12, 11)
        add("ex-mollets-unilateraux", lastWednesday, 13, 12, 12)
        add("ex-rollout-serviette", lastWednesday, 12, 12)
        val monday = today.minusDays(2)
        add("ex-pompes-deficit", monday, 9, 8, 8, 7)
        add("ex-rowing-australien-pieds-sureleves", monday, 10, 10, 9)
        add("ex-squat-bulgare", monday, 13, 13)
        add("ex-extensions-triceps-sol", monday, 12, 11, 10)
        add("ex-releves-jambes", monday, 12, 12, 11)
        add("ex-mollets-unilateraux", monday, 15, 14, 14)
        val tuesday = today.minusDays(1)
        add("ex-tractions-pronation", tuesday, 8, 7, 7, 6)
        add("ex-rowing-australien-supination", tuesday, 10, 10, 9)
        add("ex-pompes-pseudo-planche-pike", tuesday, 9, 8, 8)
        add("ex-souleve-de-terre-roumain-unilateral", tuesday, 10, 10, 10, weight = 8.0)
        add("ex-hollow-body-hold", tuesday, 35, 35, 30)
        add("ex-squat-bulgare", today, 11, 11, 10, 10)
        add("ex-sissy-squat-shrimp-squat", today, 10, 9, 9)
        return AppData.initial().copy(
            exercises = HypertrophyProgram.exercises(),
            entries = entries,
            training = Training(program = HypertrophyProgram.create(), seeded = true),
        )
    }

    /** Three months of weigh-ins during a bulk: about +0.25 kg a week, with daily ups and downs. */
    private fun bodyWeights(today: LocalDate): List<BodyWeightEntry> {
        val noise = listOf(0.3, -0.2, 0.4, 0.0, -0.4, 0.2, -0.1, 0.5, -0.3, 0.1)
        return (89 downTo 0).filter { it % 6 != 4 }.map { daysAgo ->
            val day = 89 - daysAgo
            BodyWeightEntry(
                date = today.minusDays(daysAgo.toLong()),
                weight = roundBodyWeight(74.0 + day * 0.036 + noise[day % noise.size]),
            )
        }
    }

    /** The plan of the app followed since last week, with what was eaten until today (a Wednesday). */
    fun nutrition(today: LocalDate): Nutrition {
        val plan = DefaultMealPlan.create()
        val monday = today.startOfWeek()
        fun foods(date: LocalDate, vararg slots: String, partial: Pair<String, Int>? = null): List<String> {
            val day = plan.day(date.dayOfWeek)
            val whole = slots.flatMap { slot -> day.meal(slot).items.map { it.id } }
            val part = partial?.let { (slot, count) -> day.meal(slot).items.take(count).map { it.id } }.orEmpty()
            return whole + part
        }
        val all = arrayOf("matin", "dejeuner", "pre-seance", "post-seance", "coucher")
        val lastWeek = (0L..6L).map { offset ->
            val date = monday.minusWeeks(1).plusDays(offset)
            val skipped = if (offset % 3 == 1L) all.dropLast(1).toTypedArray() else all
            NutritionDay(date, foods(date, *skipped), water = plan.day(date.dayOfWeek).waterGlasses - (offset % 2).toInt())
        }
        val tuesday = monday.plusDays(1)
        val pasta = plan.day(tuesday.dayOfWeek).meal("dejeuner").items.first()
        return Nutrition(
            plans = listOf(plan),
            schedule = listOf(PlanPeriod(plan.id, monday.minusWeeks(1))),
            days = lastWeek + listOf(
                NutritionDay(monday, foods(monday, *all), water = 13),
                NutritionDay(
                    tuesday,
                    foods(tuesday, "matin", "dejeuner", "pre-seance", "post-seance"),
                    swaps = listOf(FoodSwap(pasta.id, "riz basmati", "150 g")),
                    water = 11,
                ),
                NutritionDay(today, foods(today, "matin", partial = "dejeuner" to 2), water = 6),
            ),
            seeded = true,
            // A few things already in the cart, and two of the user's own.
            shopping = listOf(
                ShoppingWeek(
                    weekStart = monday,
                    checked = listOf("lait", "flocons d'avoine", "banane", "pomme", "poulet", "riz basmati", "skyr").map(::shoppingKey),
                    extras = listOf(ShoppingExtra("x1", "Éponges"), ShoppingExtra("x2", "Café moulu", checked = true)),
                ),
            ),
        )
    }

    /** Three months of a bulk that started at 66 kg, as the plan of the app aims at. */
    fun bulkWeights(today: LocalDate): List<BodyWeightEntry> {
        val noise = listOf(0.3, -0.2, 0.4, 0.0, -0.4, 0.2, -0.1, 0.5, -0.3, 0.1)
        return (89 downTo 0).filter { it % 3 != 1 }.map { daysAgo ->
            val day = 89 - daysAgo
            BodyWeightEntry(
                date = today.minusDays(daysAgo.toLong()),
                weight = roundBodyWeight(66.0 + day * 0.04 + noise[day % noise.size]),
            )
        }
    }
}
