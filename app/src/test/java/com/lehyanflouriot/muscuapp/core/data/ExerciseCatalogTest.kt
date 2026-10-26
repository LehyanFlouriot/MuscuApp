package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Measure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun searchIgnoresAccentsAndCase() {
        val names = ExerciseCatalog.search("DEVELOPPE").map { it.name }
        assertTrue("Développé couché" in names)
        assertTrue(names.all { ExerciseCatalog.normalize(it).contains("developpe") })
    }

    @Test
    fun namesStartingWithTheQueryComeFirst() {
        val names = ExerciseCatalog.search("squat").map { it.name }
        assertEquals("Squat", names.first())
        assertTrue("Hack squat" in names)
    }

    @Test
    fun existingExercisesAreNotSuggestedAgain() {
        val names = ExerciseCatalog.search("", existingNames = listOf("squat ")).map { it.name }
        assertFalse("Squat" in names)
        assertTrue(names.isNotEmpty())
    }

    @Test
    fun catalogIsConsistent() {
        val all = ExerciseCatalog.all
        assertEquals(all.size, all.map { ExerciseCatalog.normalize(it.name) }.toSet().size)
        assertEquals(12, ExerciseCatalog.popular.size)
        val plank = all.first { it.name == "Gainage" }
        assertEquals(Measure.SECONDS, plank.measure)
        assertFalse(plank.toDraft().usesWeight)
    }
}
