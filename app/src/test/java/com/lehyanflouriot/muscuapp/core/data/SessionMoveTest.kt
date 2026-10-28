package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.missedSession
import com.lehyanflouriot.muscuapp.core.domain.sessionToRemind
import com.lehyanflouriot.muscuapp.core.model.SessionMove
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SessionMoveTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    // The program starts on Monday 26: the quadriceps on Wednesday, then a rest day.
    private val wednesday = LocalDate.of(2026, 10, 28)
    private val thursday = wednesday.plusDays(1)
    private var today = thursday
    private var ids = 0

    private fun TestScope.repository(): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(FakeStorage(), backgroundScope, dispatcher, clock = { 1000L }, newId = { "id${ids++}" }, today = { today })
            .also { it.load() }
    }

    private val WorkoutRepository.app get() = data.value!!

    @Test
    fun aMissedSessionIsDoneOnTheRestDayAfter() = runTest {
        val repo = repository()
        val missed = missedSession(repo.app, thursday)!!
        assertEquals(wednesday, missed.date)
        assertEquals("Quadriceps", missed.day.title)

        repo.moveSession(from = wednesday, to = thursday)
        val moved = buildSession(repo.app, thursday)!!
        assertEquals(wednesday, moved.movedFrom)
        assertEquals("Quadriceps", moved.day.title)
        assertEquals(6, moved.items.size)
        // Wednesday has nothing left to do, and nothing more is missed.
        val away = buildSession(repo.app, wednesday)!!
        assertEquals(thursday, away.movedTo)
        assertTrue(away.isRest)
        assertNull(missedSession(repo.app, thursday))
        // It is the session reminded today, and the sets of today count for it.
        assertEquals(6, sessionToRemind(repo.app, thursday)!!.items.size)
        repo.addSet("ex-squat-bulgare", thursday, WorkSet(12))
        assertEquals(listOf(12), buildSession(repo.app, thursday)!!.items.first().entries.single().sets.map { it.reps })

        // A day takes a single session, and a session moves once.
        repo.moveSession(from = wednesday.minusDays(1), to = thursday)
        repo.moveSession(from = wednesday, to = thursday.plusDays(1))
        assertEquals(listOf(SessionMove(thursday, wednesday)), repo.app.training.moves)

        repo.cancelSessionMove(thursday)
        assertTrue(buildSession(repo.app, thursday)!!.isRest)
        assertEquals(6, buildSession(repo.app, wednesday)!!.items.size)
    }

    @Test
    fun aMovedSessionAimsAtMoreThanItsOwnDayLastWeek() = runTest {
        val repo = repository()
        // Last Wednesday, and last Thursday's mobility work, which is not that session.
        repo.saveEntry(null, "ex-squat-bulgare", wednesday.minusWeeks(1), listOf(WorkSet(10), WorkSet(10), WorkSet(9), WorkSet(9)))
        repo.saveEntry(null, "ex-squat-bulgare", thursday.minusWeeks(1), listOf(WorkSet(5)))
        repo.moveSession(from = wednesday, to = thursday)
        val squat = buildSession(repo.app, thursday)!!.items.first()
        assertEquals(wednesday.minusWeeks(1), squat.last!!.date)
        assertEquals(listOf(11, 11, 10, 10), squat.goal.map { it.reps })
    }

    @Test
    fun onlyTheLastSessionMissedSinceTrainingCanBeMoved() = runTest {
        val repo = repository()
        // Friday has a session of its own.
        assertNull(missedSession(repo.app, thursday.plusDays(1)))

        // A started session is not missed: it is done on its day.
        repo.addSet("ex-squat-bulgare", wednesday, WorkSet(10))
        assertNull(missedSession(repo.app, thursday))

        // Trained on Saturday: nothing is missed on Sunday.
        today = thursday.plusDays(3)
        val sunday = today
        repo.saveEntry(null, "ex-squat-bulgare", sunday.minusDays(1), listOf(WorkSet(10)))
        assertNull(missedSession(repo.app, sunday))
        // Without anything noted since, Saturday's session can be done on Sunday, a rest day...
        val repo2 = repository()
        assertEquals(sunday.minusDays(1), missedSession(repo2.app, sunday)!!.date)
        // ...and on the next Thursday, the last one missed is Wednesday's.
        assertEquals(sunday.plusDays(3), missedSession(repo2.app, sunday.plusDays(4))!!.date)
    }

    @Test
    fun movesThatCannotBeAreDropped() {
        val clean = sanitizeTraining(
            Training(
                moves = listOf(
                    SessionMove(date = thursday, from = wednesday),
                    // Backwards, twice the same day, or the same session twice.
                    SessionMove(date = wednesday, from = thursday),
                    SessionMove(date = thursday, from = wednesday.minusDays(1)),
                    SessionMove(date = thursday.plusDays(3), from = wednesday),
                ),
            ),
            emptySet(),
        )
        assertEquals(listOf(SessionMove(date = thursday, from = wednesday)), clean.moves)
    }
}
