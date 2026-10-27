package com.lehyanflouriot.muscuapp.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
import java.util.Locale
import kotlin.math.abs

/**
 * Follows the entrance animations frame by frame, without a device: how far elements still are
 * from their place (in dp) and what counters show at each moment. The tables are printed in the
 * build log, and everything must have settled before the end.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class MotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 28)
    private val data = SampleData.build(today)
    private val times = listOf(0L, 50L, 100L, 200L, 300L, 450L, 600L, 900L, 1_500L, 3_000L, 4_000L)

    private class Tracked(val name: String, val matcher: SemanticsMatcher, val readText: Boolean = false)

    private fun follow(screen: String, tracked: List<Tracked>, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MuscuTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        val density = composeRule.density.density
        var elapsed = 0L
        val samples = times.map { time ->
            composeRule.mainClock.advanceTimeBy(time - elapsed, ignoreFrameDuration = true)
            elapsed = time
            tracked.map { item ->
                val node = composeRule.onAllNodes(item.matcher, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
                when {
                    node == null -> "-"
                    item.readText -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()
                    else -> (node.boundsInRoot.top / density).toString()
                }
            }
        }
        // Positions are shown as the distance left to travel to the final place.
        val final = samples.last()
        val header = "  time   " + tracked.joinToString(" | ") { it.name.take(14).padEnd(14) }
        val rows = samples.mapIndexed { index, values ->
            "  ${times[index].toString().padStart(4)}ms " + values.mapIndexed { column, value ->
                val cell = if (tracked[column].readText || value == "-") {
                    value
                } else {
                    String.format(Locale.US, "%.1f", value.toFloat() - final[column].toFloat())
                }
                cell.take(14).padEnd(14)
            }.joinToString(" | ")
        }
        println("MOTION $screen\n$header\n" + rows.joinToString("\n"))
        // A second before the end, everything is in place.
        tracked.indices.filterNot { tracked[it].readText }.forEach { column ->
            val settled = samples[samples.lastIndex - 1][column]
            if (settled != "-") assertEquals(0f, abs(settled.toFloat() - final[column].toFloat()), 0.5f)
        }
        tracked.indices.filter { tracked[it].readText }.forEach { column ->
            assertEquals(final[column], samples[samples.lastIndex - 1][column])
        }
    }

    @Test
    fun week() = follow(
        "Semaine",
        listOf(
            Tracked("selector", hasText("Cette semaine")),
            Tracked("summary", hasText("séances")),
            Tracked("first card", hasText("Développé couché")),
            Tracked("later card", hasText("Rowing barre")),
            Tracked("button", hasText("Exercice")),
            Tracked("volume text", hasText("Volume", substring = true), readText = true),
        ),
    ) {
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
    fun progress() = follow(
        "Progres",
        listOf(
            Tracked("streak card", hasText("Continue", substring = true)),
            Tracked("chart card", hasText("Séries par semaine")),
            Tracked("first row", hasText("Développé couché")),
            Tracked("streak text", hasText("d'affilée", substring = true), readText = true),
        ),
    ) {
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
    fun rest() = follow(
        "Repos",
        listOf(
            Tracked("dial", hasText("1:12")),
            Tracked("controls", hasText("+15 s")),
            Tracked("first preset", hasText("Court")),
            Tracked("last preset", hasText("Très lourd")),
        ),
    ) {
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
    fun weight() = follow(
        "Poids",
        listOf(
            Tracked("weigh-in", hasText("Ma pesée")),
            Tracked("summary", hasText("actuel")),
            Tracked("chart", hasText("Évolution")),
            Tracked("change value", hasText("+", substring = true), readText = true),
        ),
    ) {
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
