package com.lehyanflouriot.muscuapp.ui

import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
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
        )
    }
}
