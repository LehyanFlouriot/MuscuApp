package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** Weights accepted for a person, in kg. */
val BODY_WEIGHT_RANGE: ClosedFloatingPointRange<Double> = 20.0..400.0

/** Shown before the very first weight has been noted. */
const val DEFAULT_BODY_WEIGHT = 70.0

/** The trend averages the weights noted during this many days. */
const val TREND_DAYS = 7

/** The weekly rate is measured over this many days. */
const val RATE_DAYS = 28

/** Rounds to 0.1 kg, the precision of bathroom scales. */
fun roundBodyWeight(kg: Double): Double = (kg * 10).roundToLong() / 10.0

data class WeightPoint(val date: LocalDate, val weight: Double)

/** Periods that can be shown on the weight curve. */
enum class WeightPeriod(private val months: Long?) {
    MONTH(1),
    THREE_MONTHS(3),
    YEAR(12),
    ALL(null),
    ;

    /** First day shown. Never before the first weight, so that the curve uses the whole width. */
    fun start(today: LocalDate, firstDate: LocalDate): LocalDate =
        months?.let { maxOf(today.minusMonths(it), firstDate) } ?: firstDate
}

/** Weights noted from [from] to [to] included, sorted by date. */
fun weightsBetween(weights: List<BodyWeightEntry>, from: LocalDate, to: LocalDate): List<BodyWeightEntry> =
    weights.filter { it.date in from..to }.sortedBy { it.date }

/**
 * Smoothed curve: for each day with a weight, the average of the weights noted during the
 * [days] days ending that day. It hides the daily ups and downs (water, meals...).
 */
fun weightTrend(weights: List<BodyWeightEntry>, days: Int = TREND_DAYS): List<WeightPoint> {
    val sorted = weights.sortedBy { it.date }
    var windowStart = 0
    return sorted.mapIndexed { index, entry ->
        val firstDay = entry.date.minusDays(days - 1L)
        while (sorted[windowStart].date < firstDay) windowStart++
        val window = sorted.subList(windowStart, index + 1)
        WeightPoint(entry.date, window.sumOf { it.weight } / window.size)
    }
}

/**
 * kg gained (or lost) per week during the [days] days ending at the last weight. It comes from
 * a linear regression, so that one unusual day does not count too much. Null with fewer than
 * 3 weights or less than a week between the first and the last of them.
 */
fun weeklyRate(weights: List<BodyWeightEntry>, days: Int = RATE_DAYS): Double? {
    val last = weights.maxByOrNull { it.date } ?: return null
    val from = last.date.minusDays(days - 1L)
    val window = weights.filter { it.date >= from }
    if (window.size < 3) return null
    val xs = window.map { ChronoUnit.DAYS.between(from, it.date).toDouble() }
    if (xs.max() - xs.min() < 7) return null
    val meanX = xs.average()
    val meanY = window.sumOf { it.weight } / window.size
    var covariance = 0.0
    var variance = 0.0
    window.forEachIndexed { index, entry ->
        val dx = xs[index] - meanX
        covariance += dx * (entry.weight - meanY)
        variance += dx * dx
    }
    return covariance / variance * 7
}

data class WeightSummary(
    val first: BodyWeightEntry,
    val latest: BodyWeightEntry,
    /** See [weeklyRate]. */
    val weeklyRate: Double?,
) {
    val change: Double get() = latest.weight - first.weight
}

fun weightSummary(weights: List<BodyWeightEntry>): WeightSummary? {
    val sorted = weights.sortedBy { it.date }
    val first = sorted.firstOrNull() ?: return null
    return WeightSummary(first = first, latest = sorted.last(), weeklyRate = weeklyRate(sorted))
}

/** The last weight noted before [date]. */
fun previousWeight(weights: List<BodyWeightEntry>, date: LocalDate): BodyWeightEntry? =
    weights.filter { it.date < date }.maxByOrNull { it.date }

/** Weight to propose for [date]: the one noted that day, else the last one before, else the next one. */
fun suggestedWeight(weights: List<BodyWeightEntry>, date: LocalDate): Double? {
    val entry = weights.firstOrNull { it.date == date }
        ?: previousWeight(weights, date)
        ?: weights.filter { it.date > date }.minByOrNull { it.date }
    return entry?.weight
}
