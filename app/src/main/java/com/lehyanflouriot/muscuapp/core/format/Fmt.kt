package com.lehyanflouriot.muscuapp.core.format

import com.lehyanflouriot.muscuapp.core.domain.Amount
import com.lehyanflouriot.muscuapp.core.domain.AmountUnit
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** French formatting of numbers, durations, sets and dates. */
object Fmt {
    val locale: Locale = Locale.FRANCE

    /** Non-breaking spaces keep a number and its unit on the same line. */
    private const val NBSP = '\u00A0'
    private const val NNBSP = '\u202F'
    private const val MINUS = '−'

    private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", locale)
    private val longDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

    fun number(value: Double, maxDecimals: Int = 2): String {
        val pattern = if (maxDecimals > 0) "#,##0." + "#".repeat(maxDecimals) else "#,##0"
        return DecimalFormat(pattern, DecimalFormatSymbols(locale)).format(value).replace('-', MINUS)
    }

    fun number(value: Int): String = number(value.toDouble(), 0)

    /** "62,5" */
    fun weight(kg: Double): String = number(kg)

    /** "62,5 kg" */
    fun kg(kg: Double): String = "${weight(kg)}${NBSP}kg"

    /** Body weight, always with one decimal: "78,0", "78,4". */
    fun bodyWeight(kg: Double): String = DecimalFormat("#,##0.0", DecimalFormatSymbols(locale)).format(kg)

    /** "78,4 kg" */
    fun bodyWeightKg(kg: Double): String = "${bodyWeight(kg)}${NBSP}kg"

    /** A change of weight: "+1,2 kg", "−0,4 kg", "0 kg". */
    fun signedKg(kg: Double, maxDecimals: Int = 1): String {
        val factor = 10.0.pow(maxDecimals)
        return "${signed((kg * factor).roundToLong() / factor)}${NBSP}kg"
    }

    /** "12 450 kg" */
    fun volume(kg: Double): String = "${number(kg.roundToLong().toDouble(), 0)}${NBSP}kg"

    /** Timer display: "1:30", "0:05", "1:02:03". */
    fun clock(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val minutes = seconds / 60
        val rest = seconds % 60
        return if (minutes >= 60) {
            String.format(locale, "%d:%02d:%02d", minutes / 60, minutes % 60, rest)
        } else {
            String.format(locale, "%d:%02d", minutes, rest)
        }
    }

    /** Remaining time of a running timer, rounded up so that "0:00" only shows at the very end. */
    fun clockMillis(millis: Long): String = clock((millis.coerceAtLeast(0) + 999) / 1000)

    /** "45 s", "1 min", "1 min 30", "2 min 05". */
    fun duration(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val minutes = s / 60
        val rest = s % 60
        return when {
            minutes == 0 -> "$rest${NBSP}s"
            rest == 0 -> "$minutes${NBSP}min"
            else -> String.format(locale, "%d${NBSP}min %02d", minutes, rest)
        }
    }

    /** "77 %" */
    fun percent(ratio: Double): String = "${(ratio * 100).roundToLong()}$NNBSP%"

    /** "3,25 L" */
    fun liters(ml: Int): String = "${number(ml / 1000.0)}${NBSP}L"

    /** "2 550 kcal" */
    fun kcal(kcal: Int): String = "${number(kcal)}${NBSP}kcal"

    /** Grams of protein: "155 g". */
    fun grams(grams: Int): String = "$grams${NBSP}g"

    /** A quantity to buy: "850 g", "1,05 kg", "2,1 L", "6", "1,5 c. à s.". */
    fun amount(amount: Amount): String {
        val value = amount.value
        return when (amount.unit) {
            AmountUnit.GRAM ->
                if (value >= 1_000) "${number(value / 1_000)}${NBSP}kg" else "${number(value.roundToLong().toDouble(), 0)}${NBSP}g"
            AmountUnit.MILLILITER ->
                if (value >= 1_000) "${number(value / 1_000)}${NBSP}L" else "${number(value.roundToLong().toDouble(), 0)}${NBSP}ml"
            AmountUnit.PIECE -> number(value, 1)
            AmountUnit.TABLESPOON -> "${number(value, 1)}${NBSP}c.${NBSP}à${NBSP}s."
            AmountUnit.TEASPOON -> "${number(value, 1)}${NBSP}c.${NBSP}à${NBSP}c."
            AmountUnit.OTHER -> "${number(value, 1)}$NBSP${amount.label}"
        }
    }

    /** Quantities in different units, added: "5 c. à s. + 2 c. à c.". */
    fun amounts(amounts: List<Amount>): String = amounts.joinToString(" + ") { amount(it) }

    /** A quantity never split over two lines: "1,5 c. à s.". */
    fun quantity(text: String): String = text.trim().replace(' ', NBSP)

    private val spaceBeforePunctuation = Regex(" ([:;!?»])")
    private val spaceAfterQuote = Regex("« ")
    private val spaceBeforePercent = Regex(" %")
    private val signAndNumber = Regex("([+−]) (?=\\d)")
    private val numberAndUnit = Regex("(\\d) (?=(?:g|kg|mg|ml|cl|L|h|min|s|kcal)(?![\\p{L}\\d]))")

    /**
     * French typography for free text: no line break before ":", "!", "?" or "%", nor between a
     * number and its unit ("150 g") or its sign ("− 150 kcal").
     */
    fun typography(text: String): String = text
        .replace(spaceBeforePunctuation, "$NBSP$1")
        .replace(spaceAfterQuote, "«$NBSP")
        .replace(spaceBeforePercent, "$NNBSP%")
        .replace(signAndNumber, "$1$NBSP")
        .replace(numberAndUnit, "$1$NBSP")

    /** "+12 %", "−5 %" */
    fun signedPercent(ratio: Double): String {
        val percent = (ratio * 100).roundToLong()
        val sign = when {
            percent > 0 -> "+"
            percent < 0 -> MINUS.toString()
            else -> ""
        }
        return "$sign${abs(percent)}$NNBSP%"
    }

    /** "+5", "−2,5" */
    fun signed(value: Double): String {
        val sign = when {
            value > 0 -> "+"
            value < 0 -> MINUS.toString()
            else -> ""
        }
        return sign + number(abs(value))
    }

    /** Value of one set: "10" or "45 s". */
    fun reps(reps: Int, measure: Measure): String =
        if (measure == Measure.SECONDS) "$reps${NBSP}s" else reps.toString()

    /**
     * Compact description of the sets of an entry:
     * "4 × 10 · 60 kg", "10-10-8 · 60 kg", "10×60 · 8×65 kg", "3 × 45 s".
     */
    fun sets(sets: List<WorkSet>, measure: Measure, showWeight: Boolean): String {
        if (sets.isEmpty()) return ""
        val unit = if (measure == Measure.SECONDS) "${NBSP}s" else ""
        val first = sets.first()
        val sameReps = sets.all { it.reps == first.reps }
        val sameWeight = sets.all { it.weight == first.weight }
        val withWeight = showWeight && sets.any { it.weight > 0.0 }
        val repsPart = if (sameReps) {
            "${sets.size}$NBSP×$NBSP${first.reps}$unit"
        } else {
            sets.joinToString("-") { it.reps.toString() } + unit
        }
        return when {
            !withWeight -> repsPart
            sameWeight -> "$repsPart · ${kg(first.weight)}"
            else -> sets.joinToString(" · ") { "${it.reps}$unit×${weight(it.weight)}" } + "${NBSP}kg"
        }
    }

    /** What a session asks for an exercise: "4 × 6–12 · RIR 1", "2 × 10–15 / jambe", "3 × 30–45 s". */
    fun target(target: SetTarget, measure: Measure): String = buildString {
        append(target.sets).append(NBSP).append('×').append(NBSP).append(target.min)
        if (target.max != target.min) append('–').append(target.max)
        if (measure == Measure.SECONDS) append(NBSP).append('s')
        if (target.per.isNotBlank()) append(" / ").append(target.per)
        target.rir?.let { append(" · RIR").append(NBSP).append(it) }
    }

    /** "lun. 20 oct." */
    fun day(date: LocalDate): String = date.format(dayFormatter)

    /** "lun." */
    fun dayOfWeek(date: LocalDate): String = date.format(dayOfWeekFormatter)

    /** "20 oct.", or "20 oct. 2025" with the year. */
    fun dayMonth(date: LocalDate, withYear: Boolean = false): String =
        date.format(if (withYear) dayMonthYearFormatter else dayMonthFormatter)

    /** "lundi 20 octobre" */
    fun longDay(date: LocalDate): String = date.format(longDayFormatter)

    /** "18:42" */
    fun hourMinute(time: LocalTime): String = time.format(hourMinuteFormatter)

    /** "20 – 26 oct.", "27 oct. – 2 nov.", with the year when it is not the current one. */
    fun weekRange(weekStart: LocalDate, today: LocalDate): String {
        val end = weekStart.plusDays(6)
        val withYear = weekStart.year != today.year || end.year != today.year
        val endText = end.format(if (withYear) dayMonthYearFormatter else dayMonthFormatter)
        val startText = when {
            weekStart.month == end.month -> weekStart.dayOfMonth.toString()
            weekStart.year == end.year -> weekStart.format(dayMonthFormatter)
            else -> weekStart.format(dayMonthYearFormatter)
        }
        return "$startText – $endText"
    }
}
