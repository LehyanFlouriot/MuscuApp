package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.animateFillAsState
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme

val MealMoment.icon: ImageVector
    get() = when (this) {
        MealMoment.MORNING -> Icons.Rounded.WbSunny
        MealMoment.LUNCH -> Icons.Rounded.LunchDining
        MealMoment.SNACK -> Icons.Rounded.Cookie
        MealMoment.PRE_WORKOUT -> Icons.Rounded.Bolt
        MealMoment.POST_WORKOUT -> Icons.Rounded.FitnessCenter
        MealMoment.DINNER -> Icons.Rounded.DinnerDining
        MealMoment.NIGHT -> Icons.Rounded.Bedtime
    }

@get:StringRes
val MealMoment.labelRes: Int
    get() = when (this) {
        MealMoment.MORNING -> R.string.moment_morning
        MealMoment.LUNCH -> R.string.moment_lunch
        MealMoment.SNACK -> R.string.moment_snack
        MealMoment.PRE_WORKOUT -> R.string.moment_pre_workout
        MealMoment.POST_WORKOUT -> R.string.moment_post_workout
        MealMoment.DINNER -> R.string.moment_dinner
        MealMoment.NIGHT -> R.string.moment_night
    }

/** Colors of the rings of a day, as on a watch: calories, protein and water. */
@Immutable
class RingColors(val kcal: Color, val protein: Color, val water: Color)

@Composable
fun ringColors(): RingColors = RingColors(
    kcal = MaterialTheme.colorScheme.primary,
    protein = MuscuTheme.extendedColors.success,
    water = MaterialTheme.colorScheme.tertiary,
)

/** One of the [ActivityRings]: its color and how much of it is closed, from 0 to 1. */
@Immutable
class Ring(val color: Color, val fraction: Float)

/**
 * Rings inside one another that close as the day goes. They fill up one after the other when
 * they appear, then follow their values.
 */
@Composable
fun ActivityRings(
    rings: List<Ring>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    gap: Dp = 3.dp,
) {
    val fills = rings.mapIndexed { index, ring -> animateFillAsState(ring.fraction, delayMillis = 150 + index * 120) }
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val space = gap.toPx()
        rings.forEachIndexed { index, ring ->
            val inset = stroke / 2 + index * (stroke + space)
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            if (arcSize.minDimension <= 0f) return@forEachIndexed
            val topLeft = Offset(inset, inset)
            drawArc(ring.color.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val fraction = fills[index].value
            if (fraction > 0f) {
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** A ring alone, thinner, around a day of the week. */
@Composable
fun ProgressRing(fraction: Float, color: Color, trackColor: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 3.dp) {
    val fill = animateFillAsState(fraction)
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (fill.value > 0f) {
            drawArc(color, -90f, 360f * fill.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

private class Started(var value: Boolean = false)

/** Round box of a food: an empty circle that fills with a check, with a little bounce. */
@Composable
fun FoodCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val success = MuscuTheme.extendedColors
    val fill by animateColorAsState(if (checked) success.success else Color.Transparent, tween(200), label = "check-fill")
    val border by animateColorAsState(
        if (checked) success.success else MaterialTheme.colorScheme.outline,
        tween(200),
        label = "check-border",
    )
    val scale = remember { Animatable(1f) }
    val started = remember { Started() }
    LaunchedEffect(checked) {
        if (started.value) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, Motion.bounce())
        }
        started.value = true
    }
    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = checked, enter = scaleIn(tween(250, easing = Motion.EaseOut)) + fadeIn(), exit = fadeOut(tween(120))) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = success.onSuccess, modifier = Modifier.size(16.dp))
        }
    }
}
