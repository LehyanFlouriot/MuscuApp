package com.lehyanflouriot.muscuapp.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.lehyanflouriot.muscuapp.core.data.ExerciseCatalog
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.DEFAULT_BODY_WEIGHT
import com.lehyanflouriot.muscuapp.core.domain.WeightPeriod
import com.lehyanflouriot.muscuapp.core.domain.buildProgressItems
import com.lehyanflouriot.muscuapp.core.domain.buildWeekOverview
import com.lehyanflouriot.muscuapp.core.domain.metricValue
import com.lehyanflouriot.muscuapp.core.domain.suggestedWeight
import com.lehyanflouriot.muscuapp.core.domain.weekStreak
import com.lehyanflouriot.muscuapp.core.domain.weeklySeries
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.progress.ProgressScreen
import com.lehyanflouriot.muscuapp.ui.rest.RestScreen
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import com.lehyanflouriot.muscuapp.ui.week.WeekScreen
import com.lehyanflouriot.muscuapp.ui.weight.WeighInState
import com.lehyanflouriot.muscuapp.ui.weight.WeightScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.abs

/**
 * Follows the entrance animations frame by frame: for each moment, the share of each quarter of
 * the screen that is not yet in place. The table is printed in the build log, and the screens must
 * end up still.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class MotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 28)
    private val data = SampleData.build(today)
    private val times = listOf(16L, 100L, 200L, 300L, 450L, 600L, 900L, 1_500L, 3_000L, 4_000L)

    private fun follow(name: String, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MuscuTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        var elapsed = 0L
        var width = 0
        var height = 0
        val frames = times.map { time ->
            composeRule.mainClock.advanceTimeBy(time - elapsed, ignoreFrameDuration = true)
            elapsed = time
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            width = bitmap.width
            height = bitmap.height
            time to IntArray(width * height).also { bitmap.getPixels(it, 0, width, 0, 0, width, height) }
        }
        val final = frames.last().second
        val table = frames.joinToString("\n") { (time, pixels) ->
            val quarters = (0 until 4).map { quarter -> movingShare(pixels, final, width, height, quarter) }
            "  ${time.toString().padStart(5)} ms  " + quarters.joinToString("  ") { "${it.toString().padStart(3)} %" }
        }
        println("MOTION $name: share of each quarter of the screen not yet in place, top to bottom\n$table")
        // One second before the end, everything has settled.
        assertEquals(0, (0 until 4).sumOf { movingShare(frames[frames.lastIndex - 1].second, final, width, height, it) })
    }

    private fun movingShare(pixels: IntArray, final: IntArray, width: Int, height: Int, quarter: Int): Int {
        val from = quarter * height / 4 * width
        val to = (quarter + 1) * height / 4 * width
        var moving = 0
        for (i in from until to) {
            val a = pixels[i]
            val b = final[i]
            val difference = abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF)) +
                abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF)) +
                abs((a and 0xFF) - (b and 0xFF))
            if (difference > 24) moving++
        }
        return moving * 100 / (to - from)
    }

    @Test
    fun week() = follow("Semaine") {
        WeekScreen(
            overview = buildWeekOverview(data, today),
            today = today,
            hasAnyEntry = true,
            suggestions = ExerciseCatalog.popular,
            onPreviousWeek = {},
            onNextWeek = {},
            onThisWeek = {},
            onLog = {},
            onEditEntry = {},
            onOpenExercise = {},
            onAddExercise = {},
            onQuickAdd = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun progress() = follow("Progres") {
        ProgressScreen(
            items = buildProgressItems(data, today),
            activity = weeklySeries(data.entries, today, 12) { metricValue(it, ChartMetric.SETS) },
            streak = weekStreak(data.entries, today),
            trainedThisWeek = true,
            today = today,
            onOpenExercise = {},
        )
    }

    @Test
    fun rest() = follow("Repos") {
        RestScreen(
            state = TimerState.Running(endAt = 1_072_000, total = 90_000, label = "Développé couché"),
            now = 1_000_000,
            idleDurationMs = 90_000,
            presets = data.restPresets,
            notificationsEnabled = true,
            onToggle = {},
            onAdjust = {},
            onStop = {},
            onRestart = {},
            onStartPreset = {},
            onAddPreset = {},
            onEditPreset = {},
            onEnableNotifications = {},
            onEditIdleDuration = {},
        )
    }

    @Test
    fun weight() = follow("Poids") {
        val weights = data.bodyWeights
        val form = remember { WeighInState(suggestedWeight(weights, today) ?: DEFAULT_BODY_WEIGHT) }
        WeightScreen(
            weights = weights,
            today = today,
            form = form,
            period = WeightPeriod.THREE_MONTHS,
            onDateChange = {},
            onSave = {},
            onPeriodChange = {},
            onUpdate = { _, _ -> },
            onDelete = {},
        )
    }
}
