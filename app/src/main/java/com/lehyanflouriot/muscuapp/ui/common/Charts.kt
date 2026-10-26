package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.core.domain.WeekPoint
import kotlin.math.max

/**
 * Bars of the last weeks. Tapping a bar shows its value; by default the value of the last week
 * with data is shown.
 */
@Composable
fun WeeklyBarChart(
    points: List<WeekPoint>,
    formatValue: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val defaultIndex = points.indexOfLast { it.value > 0.0 }.takeIf { it >= 0 } ?: points.lastIndex
    var selected by remember(points) { mutableIntStateOf(defaultIndex) }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val selectedLabelStyle = labelStyle.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    val valueStyle = MaterialTheme.typography.labelLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val valueBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val dimBarColor = barColor.copy(alpha = 0.35f)
    val maxValue = max(points.maxOf { it.value }, 1e-9)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = description }
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val slot = size.width / points.size.toFloat()
                    selected = (offset.x / slot).toInt().coerceIn(0, points.lastIndex)
                }
            },
    ) {
        val topSpace = 26.dp.toPx()
        val bottomSpace = 22.dp.toPx()
        val chartHeight = size.height - topSpace - bottomSpace
        val slot = size.width / points.size
        val barWidth = slot * 0.56f
        val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        val baseline = topSpace + chartHeight
        val labelEvery = if (points.size > 8) 2 else 1

        drawLine(
            color = baselineColor,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        points.forEachIndexed { index, point ->
            val centerX = slot * index + slot / 2
            val height = if (point.value > 0) max((point.value / maxValue * chartHeight).toFloat(), 4.dp.toPx()) else 0f
            if (height > 0f) {
                drawRoundRect(
                    color = if (index == selected) barColor else dimBarColor,
                    topLeft = Offset(centerX - barWidth / 2, baseline - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
            val isLast = index == points.lastIndex
            if (index == selected || isLast || (points.lastIndex - index) % labelEvery == 0) {
                val layout = textMeasurer.measure(
                    weekShortLabel(point.weekStart),
                    style = if (index == selected) selectedLabelStyle else labelStyle,
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        (centerX - layout.size.width / 2f).coerceAtMost(size.width - layout.size.width).coerceAtLeast(0f),
                        baseline + 4.dp.toPx(),
                    ),
                )
            }
        }

        // The value of the selected week, drawn last so that no bar covers it.
        points.getOrNull(selected)?.let { point ->
            val centerX = slot * selected + slot / 2
            val height = if (point.value > 0) max((point.value / maxValue * chartHeight).toFloat(), 4.dp.toPx()) else 0f
            val layout = textMeasurer.measure(formatValue(point.value), style = valueStyle)
            val padX = 6.dp.toPx()
            val padY = 2.dp.toPx()
            val boxWidth = layout.size.width + padX * 2
            val boxHeight = layout.size.height + padY * 2
            val left = (centerX - boxWidth / 2f).coerceAtMost(size.width - boxWidth).coerceAtLeast(0f)
            val top = (baseline - height - boxHeight - 4.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                color = valueBackground,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(boxHeight / 2, boxHeight / 2),
            )
            drawText(textLayoutResult = layout, topLeft = Offset(left + padX, top + padY))
        }
    }
}

/** Small trend line; weeks without training are skipped rather than drawn as zero. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val points = values.withIndex().filter { it.value > 0.0 }
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val maxValue = points.maxOf { it.value }
        val minValue = points.minOf { it.value }
        val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val stroke = 2.5.dp.toPx()
        val stepX = if (values.size > 1) (size.width - stroke * 2) / (values.size - 1) else 0f
        fun x(index: Int) = stroke + index * stepX
        fun y(value: Double): Float {
            val usable = size.height - stroke * 2
            return if (maxValue == minValue) size.height / 2 else stroke + (usable - ((value - minValue) / range * usable).toFloat())
        }
        if (points.size > 1) {
            val path = Path()
            points.forEachIndexed { i, point ->
                if (i == 0) path.moveTo(x(point.index), y(point.value)) else path.lineTo(x(point.index), y(point.value))
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val last = points.last()
        drawCircle(color, radius = stroke * 1.4f, center = Offset(x(last.index), y(last.value)))
    }
}
