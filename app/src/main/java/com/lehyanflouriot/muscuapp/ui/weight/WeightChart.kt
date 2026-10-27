package com.lehyanflouriot.muscuapp.ui.weight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.core.domain.WeightPoint
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.ui.common.Motion
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Round bounds and step of the weight axis, with a little room above and below the values. */
private class WeightAxis(val min: Double, val max: Double, step: Double) {
    val ticks: List<Double> = generateSequence(min) { it + step }.takeWhile { it <= max + step / 100 }.toList()

    companion object {
        private val STEPS = listOf(0.5, 1.0, 2.0, 2.5, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0)

        fun of(low: Double, high: Double): WeightAxis {
            val span = (high - low).coerceAtLeast(1.0)
            val step = STEPS.firstOrNull { span / it <= 3 } ?: STEPS.last()
            return WeightAxis(
                min = floor((low - step / 4) / step) * step,
                max = ceil((high + step / 4) / step) * step,
                step = step,
            )
        }
    }
}

/** Position of a day between [left] and [right]; a single day is drawn in the middle. */
private class TimeScale(private val start: LocalDate, end: LocalDate, private val left: Float, private val right: Float) {
    private val days = ChronoUnit.DAYS.between(start, end).toFloat()

    /** Where [date] is between the first and the last day, from 0 to 1; a single day counts as the end. */
    fun fraction(date: LocalDate): Float = if (days <= 0f) 1f else ChronoUnit.DAYS.between(start, date) / days

    fun x(date: LocalDate): Float =
        if (days <= 0f) (left + right) / 2 else left + fraction(date) * (right - left)
}

/**
 * Weight curve from [start] to [end]: a dot for each weight noted and the [trend] as a line,
 * drawn from left to right when it appears or when the period changes. Tapping the chart shows
 * the closest weight; the last one is shown by default.
 */
@Composable
fun WeightChart(
    weights: List<WeightPoint>,
    trend: List<WeightPoint>,
    start: LocalDate,
    end: LocalDate,
    description: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    if (weights.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(weights) { mutableIntStateOf(weights.lastIndex) }
    val colors = MaterialTheme.colorScheme
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface, fontWeight = FontWeight.Bold)
    val lineColor = colors.primary
    val dotColor = colors.primary.copy(alpha = 0.4f)
    val gridColor = colors.outlineVariant
    val valueBackground = colors.surfaceContainerHighest

    val axis = remember(weights, trend) {
        val values = weights.map { it.weight } + trend.map { it.weight }
        WeightAxis.of(values.min(), values.max())
    }
    val tickLabels = remember(axis, axisStyle) {
        axis.ticks.map { textMeasurer.measure(Fmt.number(it, 1), style = axisStyle) }
    }
    val density = LocalDensity.current
    // Room on the left for the values of the axis, and around the dots so that none is cut.
    val gutter = tickLabels.maxOf { it.size.width } + with(density) { 8.dp.toPx() }
    val inset = with(density) { 8.dp.toPx() }
    fun scale(width: Float) = TimeScale(start, end, gutter + inset, width - inset)
    val drawn = remember(start, end) { Animatable(0f) }
    LaunchedEffect(drawn) { drawn.animateTo(1f, tween(1_200, easing = Motion.EaseOut)) }
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = description }
            .pointerInput(weights, start, end, gutter) {
                detectTapGestures { offset ->
                    val timeScale = scale(size.width.toFloat())
                    weights.indices.minByOrNull { abs(timeScale.x(weights[it].date) - offset.x) }?.let { selected = it }
                }
            },
    ) {
        val top = 36.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val timeScale = scale(size.width)
        fun y(weight: Double): Float = (bottom - (weight - axis.min) / (axis.max - axis.min) * (bottom - top)).toFloat()
        val progress = drawn.value
        // The grid shows up first, the selected weight last, once the line has reached it.
        val gridAlpha = (progress * 4).coerceAtMost(1f)
        val markerAlpha = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)

        axis.ticks.forEachIndexed { index, tick ->
            val tickY = y(tick)
            drawLine(gridColor, Offset(gutter, tickY), Offset(size.width, tickY), strokeWidth = 1.dp.toPx(), alpha = gridAlpha)
            val label = tickLabels[index]
            drawText(
                label,
                topLeft = Offset(gutter - 6.dp.toPx() - label.size.width, tickY - label.size.height / 2f),
                alpha = gridAlpha,
            )
        }

        // Dates under the curve: the first and the last day, and the middle one when there is room.
        val withYear = start.year != end.year
        val labelTop = bottom + 6.dp.toPx()
        val startLabel = textMeasurer.measure(Fmt.dayMonth(start, withYear), style = axisStyle)
        if (start == end) {
            drawText(startLabel, topLeft = Offset(timeScale.x(start) - startLabel.size.width / 2f, labelTop), alpha = gridAlpha)
        } else {
            val endLabel = textMeasurer.measure(Fmt.dayMonth(end, withYear), style = axisStyle)
            val startX = gutter + inset
            val endX = size.width - inset - endLabel.size.width
            drawText(startLabel, topLeft = Offset(startX, labelTop), alpha = gridAlpha)
            drawText(endLabel, topLeft = Offset(endX, labelTop), alpha = gridAlpha)
            val middle = start.plusDays(ChronoUnit.DAYS.between(start, end) / 2)
            val middleLabel = textMeasurer.measure(Fmt.dayMonth(middle, withYear), style = axisStyle)
            val middleX = timeScale.x(middle) - middleLabel.size.width / 2f
            val gap = 12.dp.toPx()
            if (middleX > startX + startLabel.size.width + gap && middleX + middleLabel.size.width + gap < endX) {
                drawText(middleLabel, topLeft = Offset(middleX, labelTop), alpha = gridAlpha)
            }
        }

        val point = weights.getOrNull(selected)
        val padX = 8.dp.toPx()
        val padY = 3.dp.toPx()
        val valueLabel = point?.let {
            textMeasurer.measure("${Fmt.bodyWeightKg(it.weight)} · ${Fmt.dayMonth(it.date)}", style = valueStyle)
        }
        val boxHeight = (valueLabel?.size?.height ?: 0) + padY * 2
        if (point != null && markerAlpha > 0f) {
            val x = timeScale.x(point.date)
            drawLine(gridColor, Offset(x, boxHeight), Offset(x, bottom), strokeWidth = 1.dp.toPx(), alpha = markerAlpha)
        }

        // Each dot appears when the line passes by.
        weights.forEach {
            val shown = ((progress * 1.1f - timeScale.fraction(it.date)) / 0.1f).coerceIn(0f, 1f)
            if (shown > 0f) {
                drawCircle(
                    dotColor,
                    radius = 3.5.dp.toPx() * (0.5f + 0.5f * shown),
                    center = Offset(timeScale.x(it.date), y(it.weight)),
                    alpha = dotColor.alpha * shown,
                )
            }
        }
        if (trend.size > 1) {
            val path = Path()
            trend.forEachIndexed { index, trendPoint ->
                val x = timeScale.x(trendPoint.date)
                if (index == 0) path.moveTo(x, y(trendPoint.weight)) else path.lineTo(x, y(trendPoint.weight))
            }
            measure.setPath(path, false)
            segment.reset()
            measure.getSegment(0f, measure.length * progress, segment, true)
            drawPath(segment, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (point != null && valueLabel != null && markerAlpha > 0f) {
            val center = Offset(timeScale.x(point.date), y(point.weight))
            drawCircle(backgroundColor, radius = 7.dp.toPx() * markerAlpha, center = center)
            drawCircle(lineColor, radius = 5.dp.toPx() * markerAlpha, center = center)
            val boxWidth = valueLabel.size.width + padX * 2
            val left = (center.x - boxWidth / 2).coerceAtMost(size.width - boxWidth).coerceAtLeast(0f)
            drawRoundRect(
                color = valueBackground,
                topLeft = Offset(left, 0f),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(boxHeight / 2, boxHeight / 2),
                alpha = markerAlpha,
            )
            drawText(valueLabel, topLeft = Offset(left + padX, padY), alpha = markerAlpha)
        }
    }
}
