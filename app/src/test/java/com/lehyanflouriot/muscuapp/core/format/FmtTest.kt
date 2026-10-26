package com.lehyanflouriot.muscuapp.core.format

import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FmtTest {
    /** Grouping separators differ between JDK/ICU versions (nbsp or narrow nbsp). */
    private fun String.plain() = replace('\u202F', ' ').replace('\u00A0', ' ')

    @Test
    fun numbersUseFrenchDecimalComma() {
        assertEquals("62,5", Fmt.weight(62.5))
        assertEquals("60", Fmt.weight(60.0))
        assertEquals("1,25", Fmt.weight(1.25))
        assertEquals("62,5 kg", Fmt.kg(62.5).plain())
        assertEquals("12 450 kg", Fmt.volume(12450.4).plain())
    }

    @Test
    fun clockShowsMinutesAndSeconds() {
        assertEquals("1:30", Fmt.clock(90))
        assertEquals("0:05", Fmt.clock(5))
        assertEquals("0:00", Fmt.clock(-3))
        assertEquals("1:02:03", Fmt.clock(3723))
        assertEquals("0:01", Fmt.clockMillis(1))
        assertEquals("0:00", Fmt.clockMillis(0))
        assertEquals("1:30", Fmt.clockMillis(89_500))
    }

    @Test
    fun durationsAreReadable() {
        assertEquals("45 s", Fmt.duration(45).plain())
        assertEquals("1 min", Fmt.duration(60).plain())
        assertEquals("1 min 30", Fmt.duration(90).plain())
        assertEquals("2 min 05", Fmt.duration(125).plain())
    }

    @Test
    fun signedValues() {
        assertEquals("+12 %", Fmt.signedPercent(0.123).plain())
        assertEquals("−5 %", Fmt.signedPercent(-0.05).plain())
        assertEquals("0 %", Fmt.signedPercent(0.0).plain())
        assertEquals("+2,5", Fmt.signed(2.5))
        assertEquals("−3", Fmt.signed(-3.0))
    }

    @Test
    fun bodyWeights() {
        assertEquals("78,0", Fmt.bodyWeight(78.0))
        assertEquals("78,4 kg", Fmt.bodyWeightKg(78.4).plain())
        assertEquals("+1,2 kg", Fmt.signedKg(1.2000000001).plain())
        assertEquals("−0,4 kg", Fmt.signedKg(-0.36).plain())
        assertEquals("0 kg", Fmt.signedKg(0.04).plain())
        assertEquals("+0,29 kg", Fmt.signedKg(0.2857, maxDecimals = 2).plain())
        assertEquals("26 oct.", Fmt.dayMonth(LocalDate.of(2026, 10, 26)))
        assertEquals("26 oct. 2025", Fmt.dayMonth(LocalDate.of(2025, 10, 26), withYear = true).plain())
    }

    @Test
    fun uniformSetsAreCompact() {
        val sets = List(4) { WorkSet(10, 60.0) }
        assertEquals("4 × 10 · 60 kg", Fmt.sets(sets, Measure.REPS, showWeight = true).plain())
        assertEquals("4 × 10", Fmt.sets(sets, Measure.REPS, showWeight = false).plain())
    }

    @Test
    fun differentRepsKeepTheSameWeight() {
        val sets = listOf(WorkSet(10, 60.0), WorkSet(10, 60.0), WorkSet(8, 60.0))
        assertEquals("10-10-8 · 60 kg", Fmt.sets(sets, Measure.REPS, showWeight = true).plain())
    }

    @Test
    fun differentWeightsAreListedPerSet() {
        val sets = listOf(WorkSet(10, 60.0), WorkSet(8, 62.5))
        assertEquals("10×60 · 8×62,5 kg", Fmt.sets(sets, Measure.REPS, showWeight = true).plain())
    }

    @Test
    fun bodyWeightAndTimedSets() {
        assertEquals("3 × 12", Fmt.sets(List(3) { WorkSet(12) }, Measure.REPS, showWeight = true).plain())
        assertEquals("3 × 45 s", Fmt.sets(List(3) { WorkSet(45) }, Measure.SECONDS, showWeight = false).plain())
        assertEquals("60-45 s", Fmt.sets(listOf(WorkSet(60), WorkSet(45)), Measure.SECONDS, showWeight = false).plain())
        assertEquals("", Fmt.sets(emptyList(), Measure.REPS, showWeight = true))
    }

    @Test
    fun datesAreInFrench() {
        val monday = LocalDate.of(2026, 10, 19)
        assertEquals("lun. 19 oct.", Fmt.day(monday))
        assertEquals("lundi 19 octobre", Fmt.longDay(monday))
        assertEquals("lun.", Fmt.dayOfWeek(monday))
    }

    @Test
    fun weekRanges() {
        val today = LocalDate.of(2026, 10, 26)
        assertEquals("19 – 25 oct.", Fmt.weekRange(LocalDate.of(2026, 10, 19), today))
        assertEquals("26 oct. – 1 nov.", Fmt.weekRange(LocalDate.of(2026, 10, 26), today))
        assertEquals("20 – 26 oct. 2025", Fmt.weekRange(LocalDate.of(2025, 10, 20), today))
        assertEquals("29 déc. 2025 – 4 janv. 2026", Fmt.weekRange(LocalDate.of(2025, 12, 29), today))
    }
}
