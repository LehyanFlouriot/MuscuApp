package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.currentIndex
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.domain.nextSetGoal
import com.lehyanflouriot.muscuapp.core.domain.plannedSets
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedSessionTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    // A Wednesday: the session of the quadriceps.
    private val today = LocalDate.of(2026, 10, 28)
    private var ids = 0

    private fun TestScope.repository(): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(FakeStorage(), backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    private fun WorkoutRepository.session() = buildSession(data.value!!, today)!!

    @Test
    fun aSessionIsDoneSetBySet() = runTest {
        val repo = repository()
        repo.saveEntry(null, "ex-squat-bulgare", today.minusWeeks(1), listOf(WorkSet(10), WorkSet(10), WorkSet(9), WorkSet(9)))
        var squat = repo.session().items.first()
        assertEquals(0, repo.session().currentIndex())
        assertEquals(4, squat.plannedSets)
        // One more rep than last Wednesday, set by set.
        assertEquals(WorkSet(11), squat.nextSetGoal())

        val entryId = repo.addSet("ex-squat-bulgare", today, WorkSet(12, 8.0))!!
        assertEquals(entryId, repo.addSet("ex-squat-bulgare", today, WorkSet(11, 8.0)))
        squat = repo.session().items.first()
        assertEquals(listOf(12, 11), squat.doneSets().map { it.set.reps })
        // A single entry for the day, which the week shows as done.
        assertEquals(1, repo.data.value!!.entries.count { it.date == today })
        assertTrue(squat.done)
        assertFalse(squat.complete)
        // The goal of the third set, with the load chosen on the first ones.
        assertEquals(WorkSet(10, 8.0), squat.nextSetGoal())

        repo.addSet("ex-squat-bulgare", today, WorkSet(10, 8.0))
        repo.addSet("ex-squat-bulgare", today, WorkSet(10, 8.0))
        assertTrue(repo.session().items.first().complete)
        assertEquals(1, repo.session().currentIndex())

        // A set noted by mistake is corrected, or removed.
        repo.updateSet(entryId, 1, WorkSet(9, 8.0))
        repo.updateSet(entryId, 3, null)
        assertEquals(listOf(12, 9, 10), repo.session().items.first().doneSets().map { it.set.reps })
        assertEquals(0, repo.session().currentIndex())
        repo.updateSet(entryId, 7, null)
        assertEquals(3, repo.session().items.first().doneSets().size)

        assertNull(repo.addSet("unknown", today, WorkSet(5)))
        assertNull(repo.addSet("ex-squat-bulgare", today, WorkSet(0)))
    }

    @Test
    fun anEntryLeftWithoutSetsGoesAway() = runTest {
        val repo = repository()
        val entryId = repo.addSet("ex-rollout-serviette", today, WorkSet(10))!!
        repo.updateSet(entryId, 0, null)
        assertTrue(repo.data.value!!.entries.isEmpty())
        assertFalse(repo.session().items.last().done)
    }

    @Test
    fun theSessionIsCompleteOnceEverySetIsDone() = runTest {
        val repo = repository()
        assertFalse(repo.session().complete)
        repo.session().items.forEach { item ->
            repeat(item.plannedSets) { repo.addSet(item.exercise.id, today, item.nextSetGoal()) }
        }
        val session = repo.session()
        assertTrue(session.complete)
        assertNull(session.currentIndex())
        assertEquals(session.items.sumOf { it.plannedSets }, session.doneSets().size)
        // Never done before: the bottom of the range on every set.
        assertEquals(List(4) { 8 }, session.items.first().doneSets().map { it.set.reps })
    }
}
