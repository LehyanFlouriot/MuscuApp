package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CALVES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CHEST
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.GLUTES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.HAMSTRINGS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.LEGS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.QUADS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.SHOULDERS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.TRICEPS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleMigrationTest {
    private fun exercise(id: String, name: String, group: MuscleGroup, secondary: List<MuscleGroup> = emptyList()) =
        Exercise(id = id, name = name, muscleGroup = group, secondaryMuscles = secondary)

    private fun AppData.muscles(id: String): Pair<MuscleGroup, List<MuscleGroup>> =
        exercise(id)!!.let { it.muscleGroup to it.secondaryMuscles }

    @Test
    fun theLegsOfOlderDataAreSplitAndKnownExercisesGetTheirMusclesWorkedToo() {
        val old = AppData(
            schemaVersion = 3,
            exercises = listOf(
                // The program of the app.
                exercise("ex-squat-bulgare", "Squat bulgare", LEGS),
                exercise("ex-mollets-unilateraux", "Mollets unilatéraux", LEGS),
                exercise("ex-nordic-curl", "Nordic curl", LEGS),
                exercise("ex-pompes-deficit", "Pompes en déficit, pieds surélevés", CHEST),
                // Exercises of the user: from the catalog, or named freely.
                exercise("a", "Leg curl", LEGS),
                exercise("b", "Mollets à la presse", LEGS),
                exercise("c", "Soulevé de terre", LEGS),
                exercise("d", "Presse inclinée", LEGS),
                exercise("e", "Dips", TRICEPS),
            ),
        )
        val data = WorkoutRepository.sanitize(old)

        assertEquals(AppData.CURRENT_SCHEMA_VERSION, data.schemaVersion)
        assertEquals(QUADS to listOf(GLUTES), data.muscles("ex-squat-bulgare"))
        assertEquals(CALVES to emptyList<MuscleGroup>(), data.muscles("ex-mollets-unilateraux"))
        assertEquals(HAMSTRINGS to emptyList<MuscleGroup>(), data.muscles("ex-nordic-curl"))
        assertEquals(CHEST to listOf(TRICEPS, SHOULDERS), data.muscles("ex-pompes-deficit"))
        assertEquals(HAMSTRINGS, data.muscles("a").first)
        assertEquals(CALVES, data.muscles("b").first)
        // Put among the legs, the deadlift stays there, on the hamstrings, rather than in the back.
        assertEquals(HAMSTRINGS to listOf(GLUTES), data.muscles("c"))
        assertEquals(QUADS to emptyList<MuscleGroup>(), data.muscles("d"))
        // The main muscle chosen by the user stays, and is not one it works too.
        assertEquals(TRICEPS to listOf(SHOULDERS), data.muscles("e"))
    }

    @Test
    fun aFileOfAnOlderVersionStillReadsItsLegs() {
        val old = """{"schemaVersion":3,"exercises":[{"id":"x","name":"Fentes","muscleGroup":"LEGS"}]}"""
        val data = WorkoutRepository.sanitize(AppJson.decode(old))
        assertEquals(QUADS to listOf(GLUTES), data.muscles("x"))
    }

    @Test
    fun upToDateDataKeepsWhatTheUserChose() {
        val current = AppData(
            exercises = listOf(
                // Its muscles worked too were taken away by the user: they do not come back.
                exercise("ex-pompes-deficit", "Pompes en déficit, pieds surélevés", CHEST),
                // Legs named as a whole by an older version on another device are split all the same.
                exercise("f", "Squat sumo", LEGS),
                // Muscles worked too are muscles, each once, and not the main one.
                exercise("g", "Hip thrust", GLUTES, listOf(GLUTES, HAMSTRINGS, HAMSTRINGS, MuscleGroup.CARDIO, LEGS)),
            ),
        )
        val data = WorkoutRepository.sanitize(current)
        assertEquals(CHEST to emptyList<MuscleGroup>(), data.muscles("ex-pompes-deficit"))
        assertEquals(QUADS to emptyList<MuscleGroup>(), data.muscles("f"))
        assertEquals(GLUTES to listOf(HAMSTRINGS), data.muscles("g"))
    }

    @Test
    fun theProgramAndTheCatalogNoLongerUseTheLegsAsAWhole() {
        val groups = HypertrophyProgram.exercises().map { it.muscleGroup } + ExerciseCatalog.all.map { it.muscleGroup }
        assertTrue(LEGS !in groups)
        assertTrue(LEGS !in MuscleGroup.choices)
        val secondary = HypertrophyProgram.exercises().flatMap { it.secondaryMuscles } + ExerciseCatalog.all.flatMap { it.secondaryMuscles }
        assertTrue(secondary.all { it in MuscleGroup.muscles })
    }
}
