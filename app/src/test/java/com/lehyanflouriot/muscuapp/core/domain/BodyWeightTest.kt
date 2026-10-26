package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyWeightTest {
    // Monday 26 October 2026.
    private val today = LocalDate.of(2026, 10, 26)

    private fun weight(daysAgo: Long, kg: Double) = BodyWeightEntry(today.minusDays(daysAgo), kg)

    @Test
    fun weightsAreRoundedToTheTenth() {
        assertEquals(78.4, roundBodyWeight(78.36), 0.0)
        assertEquals(78.5, roundBodyWeight(78.4 + 0.1), 0.0)
    }

    @Test
    fun trendAveragesTheLastSevenDays() {
        val weights = listOf(weight(9, 80.0), weight(6, 78.0), weight(3, 79.0), weight(0, 80.0))
        val trend = weightTrend(weights.reversed())
        assertEquals(weights.map { it.date }, trend.map { it.date })
        // The weight of 9 days ago is out of the window of today's point.
        assertEquals(listOf(80.0, 79.0, 79.0, 79.0), trend.map { it.weight })
        assertEquals(emptyList<WeightPoint>(), weightTrend(emptyList()))
    }

    @Test
    fun weeklyRateFollowsTheSlopeOfTheLastFourWeeks() {
        val steadyGain = (0L..27L).map { weight(27 - it, 70.0 + it * 0.1) }
        assertEquals(0.7, weeklyRate(steadyGain)!!, 1e-9)
        // Older weights do not count.
        assertEquals(0.7, weeklyRate(steadyGain + weight(60, 50.0))!!, 1e-9)
        // One unusual day barely changes it.
        val withOutlier = steadyGain.map { if (it.date == today.minusDays(10)) it.copy(weight = it.weight + 1.5) else it }
        assertEquals(0.7, weeklyRate(withOutlier)!!, 0.1)
    }

    @Test
    fun weeklyRateNeedsEnoughWeights() {
        assertNull(weeklyRate(emptyList()))
        assertNull(weeklyRate(listOf(weight(10, 70.0), weight(0, 71.0))))
        assertNull(weeklyRate(listOf(weight(5, 70.0), weight(3, 70.5), weight(0, 71.0))))
        assertEquals(0.7, weeklyRate(listOf(weight(14, 70.0), weight(7, 70.7), weight(0, 71.4)))!!, 1e-9)
    }

    @Test
    fun summaryComparesWithTheFirstWeight() {
        assertNull(weightSummary(emptyList()))
        val summary = weightSummary(listOf(weight(0, 78.4), weight(40, 75.2), weight(20, 76.0)))!!
        assertEquals(today.minusDays(40), summary.first.date)
        assertEquals(today, summary.latest.date)
        assertEquals(3.2, summary.change, 1e-9)
        // Only two weights in the last four weeks.
        assertNull(summary.weeklyRate)
    }

    @Test
    fun periodsStartAtTheFirstWeightAtTheEarliest() {
        val first = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2026, 9, 26), WeightPeriod.MONTH.start(today, first))
        assertEquals(LocalDate.of(2026, 7, 26), WeightPeriod.THREE_MONTHS.start(today, first))
        assertEquals(first, WeightPeriod.YEAR.start(today, first))
        assertEquals(first, WeightPeriod.ALL.start(today, first))
        assertEquals(today.minusDays(5), WeightPeriod.MONTH.start(today, today.minusDays(5)))
    }

    @Test
    fun weightsBetweenIncludesBothEnds() {
        val weights = listOf(weight(0, 78.0), weight(10, 77.0), weight(5, 77.5), weight(11, 76.0))
        assertEquals(
            listOf(today.minusDays(10), today.minusDays(5), today),
            weightsBetween(weights, today.minusDays(10), today).map { it.date },
        )
    }

    @Test
    fun suggestionPrefersTheSameDayThenTheDayBefore() {
        val weights = listOf(weight(10, 77.0), weight(5, 77.5), weight(0, 78.0))
        assertEquals(77.5, suggestedWeight(weights, today.minusDays(5))!!, 0.0)
        assertEquals(77.5, suggestedWeight(weights, today.minusDays(2))!!, 0.0)
        assertEquals(77.0, suggestedWeight(weights, today.minusDays(30))!!, 0.0)
        assertNull(suggestedWeight(emptyList(), today))
        assertEquals(today.minusDays(5), previousWeight(weights, today)?.date)
        assertNull(previousWeight(weights, today.minusDays(10)))
    }
}
