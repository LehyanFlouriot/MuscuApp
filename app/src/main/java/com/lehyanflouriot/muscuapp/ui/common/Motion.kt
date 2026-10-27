package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Motion of the app, in the spirit of Apple's interfaces: elements glide in and settle softly,
 * numbers count up, bars fill, and what is pressed gives a little under the finger.
 */
object Motion {
    /** Quick start and long, soft landing: for what appears, fills up or counts. */
    val EaseOut: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** For what travels from one place to another. */
    val EaseInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    const val REVEAL_MS = 700
    const val STAGGER_MS = 55
    const val FILL_MS = 1_100
    const val COUNT_MS = 1_000
    const val SCREEN_MS = 420
    const val ROLL_MS = 320

    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 700f)

    fun <T> bounce(): SpringSpec<T> = spring(dampingRatio = 0.42f, stiffness = 420f)
}

private fun uptimeMs(): Long = System.nanoTime() / 1_000_000

/**
 * What a screen has already shown. When the screen opens, its elements appear one after the
 * other; the ones scrolled into view later glide in once, and never again during the visit.
 */
@Stable
class RevealState internal constructor() {
    private val shown = HashSet<Any>()
    private var staggered = 0

    // Counted from the first element shown rather than from the creation of the screen, which
    // can take a while on a slow phone.
    private var openedAtMs = -1L

    /** Delay before revealing [key], or null when it is already on screen. */
    internal fun delayFor(key: Any): Int? {
        if (!shown.add(key)) return null
        val now = uptimeMs()
        if (openedAtMs < 0) openedAtMs = now
        val opening = now - openedAtMs < OPENING_MS
        return if (opening) (staggered++ * Motion.STAGGER_MS).coerceAtMost(MAX_DELAY_MS) else 0
    }

    private companion object {
        const val OPENING_MS = 500L
        const val MAX_DELAY_MS = 6 * Motion.STAGGER_MS
    }
}

@Composable
fun rememberRevealState(): RevealState = remember { RevealState() }

/** Fades the element in while it glides up into place, the first time it is laid out on screen. */
fun Modifier.reveal(state: RevealState, key: Any): Modifier = this then RevealElement(state, key)

private val REVEAL_OFFSET = 28.dp

private data class RevealElement(val state: RevealState, val key: Any) : ModifierNodeElement<RevealNode>() {
    override fun create() = RevealNode(state, key)

    override fun update(node: RevealNode) {
        node.state = state
        node.key = key
    }
}

private class RevealNode(var state: RevealState, var key: Any) : Modifier.Node(), LayoutModifierNode {
    private var progress: Animatable<Float, AnimationVector1D>? = null

    // Started at the first placement rather than at composition: lists compose some items ahead,
    // and those must not finish appearing before they are actually seen.
    private fun start(): Animatable<Float, AnimationVector1D> {
        val delay = state.delayFor(key)
        val animatable = Animatable(if (delay == null) 1f else 0f)
        if (delay != null) {
            coroutineScope.launch { animatable.animateTo(1f, tween(Motion.REVEAL_MS, delay, Motion.EaseOut)) }
        }
        progress = animatable
        return animatable
    }

    override fun onDetach() {
        progress = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val animatable = progress ?: start()
            placeable.placeWithLayer(0, 0) {
                val value = animatable.value
                alpha = value
                translationY = (1f - value) * REVEAL_OFFSET.toPx()
                val scale = 0.97f + 0.03f * value
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

/** Makes the element shrink a little while it is pressed, and spring back when released. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier =
    this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float,
) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource, pressedScale)

    override fun update(node: PressScaleNode) = node.update(interactionSource, pressedScale)
}

private class PressScaleNode(
    private var interactionSource: InteractionSource,
    private var pressedScale: Float,
) : Modifier.Node(), LayoutModifierNode {
    private val scale = Animatable(1f)
    private var collecting: Job? = null

    override fun onAttach() = collectPresses()

    fun update(source: InteractionSource, pressed: Float) {
        pressedScale = pressed
        if (source != interactionSource) {
            interactionSource = source
            if (isAttached) collectPresses()
        }
    }

    private fun collectPresses() {
        collecting?.cancel()
        collecting = coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                val target = if (presses.isEmpty()) 1f else pressedScale
                launch { scale.animateTo(target, Motion.press()) }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}

/** An interaction source for a clickable component, with the [pressScale] modifier following it. */
@Stable
class PressFeedback(val interactionSource: MutableInteractionSource, pressedScale: Float) {
    val modifier: Modifier = Modifier.pressScale(interactionSource, pressedScale)
}

@Composable
fun rememberPressFeedback(pressedScale: Float = 0.96f): PressFeedback =
    remember(pressedScale) { PressFeedback(MutableInteractionSource(), pressedScale) }

/** A start-up animation runs with its delay only the first time; later changes follow at once. */
private class FirstRun {
    var pending = true

    fun delay(delayMillis: Int): Int = if (pending) delayMillis.also { pending = false } else 0
}

/**
 * [target] counted up from zero when it first appears, then animated from its previous value
 * whenever it changes.
 */
@Composable
fun countUp(target: Double, delayMillis: Int = 0): Double {
    val value = remember { Animatable(0f) }
    val firstRun = remember { FirstRun() }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(Motion.COUNT_MS, firstRun.delay(delayMillis), Motion.EaseOut))
    }
    return value.value.toDouble()
}

@Composable
fun countUp(target: Int, delayMillis: Int = 0): Int = countUp(target.toDouble(), delayMillis).roundToInt()

/**
 * A share from 0 to 1 that fills up from zero when it first appears, then follows [fraction]
 * smoothly. Read it while drawing, so that only the drawing is redone at each frame.
 */
@Composable
fun animateFillAsState(fraction: Float, delayMillis: Int = 0): State<Float> {
    val fill = remember { Animatable(0f) }
    val firstRun = remember { FirstRun() }
    LaunchedEffect(fraction) {
        fill.animateTo(fraction.coerceIn(0f, 1f), tween(Motion.FILL_MS, firstRun.delay(delayMillis), Motion.EaseOut))
    }
    return fill.asState()
}

/** Rounded bar that fills up from the left when it appears, then follows [fraction] smoothly. */
@Composable
fun FillBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.18f),
    height: Dp = 8.dp,
    delayMillis: Int = 0,
) {
    val fill = animateFillAsState(fraction, delayMillis)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * fill.value
        if (width > 0f) {
            drawRoundRect(color, size = Size(width.coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    }
}

private class LastNumber(var value: Double?)

private fun String.numberValue(): Double? = filter { it.isDigit() || it == ',' }.replace(',', '.').toDoubleOrNull()

/**
 * Text whose characters roll like a counter when they change: up when the number grows, down
 * when it shrinks, while the unchanged characters stay still.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val number = text.numberValue()
    val last = remember { LastNumber(number) }
    val previous = last.value
    val up = number == null || previous == null || number >= previous
    SideEffect { last.value = number }
    Row(modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { index, char ->
            // Keyed from the end, so that units and decimals stay aligned when digits are added.
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (up) 1 else -1
                        (
                            slideInVertically(tween(Motion.ROLL_MS, easing = Motion.EaseOut)) { it * direction } +
                                fadeIn(tween(Motion.ROLL_MS / 2))
                            ).togetherWith(
                            slideOutVertically(tween(Motion.ROLL_MS, easing = Motion.EaseOut)) { -it * direction } +
                                fadeOut(tween(Motion.ROLL_MS / 2)),
                        ).using(SizeTransform(clip = true))
                    },
                    label = "rolling-char",
                ) { shown ->
                    Text(text = shown.toString(), style = style, color = color, fontWeight = fontWeight, maxLines = 1)
                }
            }
        }
    }
}
