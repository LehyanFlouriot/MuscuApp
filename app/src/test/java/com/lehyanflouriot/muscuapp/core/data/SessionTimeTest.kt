package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.averageSessionDuration
import com.lehyanflouriot.muscuapp.core.domain.sessionDuration
import com.lehyanflouriot.muscuapp.core.domain.sessionStartedAt
import com.lehyanflouriot.muscuapp.core.model.SessionTime
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTimeTest {
    private class FakeStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    // Wednesday, the session of the quadriceps.
    private val today = LocalDate.of(2026, 10, 28)
    private var now = 1_000_000L
    private var ids = 0

    private val WorkoutRepository.training: Training get() = data.value!!.training

    private fun TestScope.repository(): WorkoutRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutRepository(
            FakeStorage(), backgroundScope, dispatcher, clock = { now }, newId = { "id${ids++}" }, today = { today },
            builtInPrograms = listOf(HypertrophyProgram),
        )
            .also { it.load() }
    }

    private fun minutes(count: Int) = count * 60_000L

    @Test
    fun aSessionLastsFromItsOpeningToItsLastSet() = runTest {
        val repo = repository()
        repo.startSessionClock(today)
        val opened = now
        assertEquals(opened, repo.training.sessionStartedAt(today))
        assertNull(repo.training.sessionDuration(today))

        now += minutes(10)
        repo.addSet("ex-squat-bulgare", today, WorkSet(12))
        // Opened again, the session under way keeps its start.
        repo.startSessionClock(today)
        now += minutes(42)
        val entry = repo.saveEntry(null, "ex-sissy-squat-shrimp-squat", today, listOf(WorkSet(10), WorkSet(9))).entryId
        assertEquals(minutes(52), repo.training.sessionDuration(today))

        // A value corrected later, or a day already past, does not make it longer.
        now += minutes(90)
        repo.saveEntry(entry, "ex-sissy-squat-shrimp-squat", today, listOf(WorkSet(11), WorkSet(9)))
        repo.addSet("ex-pompes-deficit", today.minusDays(2), WorkSet(8))
        assertEquals(minutes(52), repo.training.sessionDuration(today))
        assertNull(repo.training.sessionDuration(today.minusDays(2)))
    }

    @Test
    fun aSessionOpenedAgainBeforeItsFirstSetStartsAgain() = runTest {
        val repo = repository()
        repo.startSessionClock(today)
        now += minutes(180)
        repo.startSessionClock(today)
        assertEquals(now, repo.training.sessionStartedAt(today))
        // Only today's session is timed.
        repo.startSessionClock(today.minusDays(1))
        assertNull(repo.training.sessionStartedAt(today.minusDays(1)))
    }

    @Test
    fun aSessionOnlyLookedAtStartsWithItsFirstSet() = runTest {
        val repo = repository()
        // Opened in the morning, then noted in the evening from the week.
        repo.startSessionClock(today)
        now += minutes(9 * 60)
        repo.saveEntry(null, "ex-squat-bulgare", today, listOf(WorkSet(12), WorkSet(11)))
        now += minutes(35)
        repo.saveEntry(null, "ex-sissy-squat-shrimp-squat", today, listOf(WorkSet(10)))
        assertEquals(minutes(35), repo.training.sessionDuration(today))
    }

    @Test
    fun theAverageIsMadeOfTheTimedSessionsOfTheLastFourWeeks() {
        val training = Training(
            times = listOf(
                SessionTime(today.minusDays(40), startedAt = 1, lastSetAt = 1 + minutes(90)),
                SessionTime(today.minusDays(7), startedAt = 1, lastSetAt = 1 + minutes(50)),
                SessionTime(today.minusDays(1), startedAt = 1, lastSetAt = 1 + minutes(40)),
                // Opened without any set: not timed.
                SessionTime(today, startedAt = 1),
            ),
        )
        assertEquals(minutes(45), training.averageSessionDuration(today))
        assertNull(Training().averageSessionDuration(today))
    }
}
