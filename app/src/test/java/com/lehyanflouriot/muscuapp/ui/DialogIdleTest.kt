package com.lehyanflouriot.muscuapp.ui

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.ui.common.CompactStepper
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import com.lehyanflouriot.muscuapp.ui.week.SessionLineDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Temporary: finds what keeps the dialog of an exercise of a session from resting in the screenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class DialogIdleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent {
            MuscuTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    @Composable
    private fun InDialog(content: @Composable () -> Unit) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Squat bulgare") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { content() } },
            confirmButton = { TextButton(onClick = {}) { Text("Enregistrer") } },
        )
    }

    @Test
    fun wholeDialogWithManualClock() {
        composeRule.mainClock.autoAdvance = false
        var applies = 0
        val handle = Snapshot.registerApplyObserver { changed, _ ->
            applies++
            if (applies <= 60 || applies % 500 == 0) {
                println(
                    "apply $applies: " + changed.take(6).joinToString { state ->
                        state.javaClass.simpleName + "=" + ((state as? State<*>)?.value?.toString()?.take(50) ?: "?")
                    },
                )
            }
        }
        val exercise = Exercise("ex-squat-bulgare", "Squat bulgare", MuscleGroup.LEGS, usesWeight = false, restSeconds = 180)
        val line = SessionExercise("wed-1", exercise.id, SetTarget(4, 8, 15, "jambe", 1), true, "Excentrique 3 s", 180)
        println("before setContent")
        show {
            SessionLineDialog(exercise, "mercredi", line, line.target, {}, { _, _, _, _ -> }, {}, {}, {})
        }
        println("after setContent, applies=$applies")
        repeat(120) { frame ->
            val before = applies
            composeRule.mainClock.advanceTimeByFrame()
            if (frame % 20 == 0 || frame >= 115) println("frame $frame: ${applies - before} applies")
        }
        val focused = composeRule.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes()
        println("focused: " + focused.map { it.config.toString().take(300) })
        handle.dispose()
    }

    @Test
    fun stepperAlone() {
        show { InDialog { CompactStepper("4", "Séries", {}, {}, {}, Modifier.width(140.dp)) } }
        println("stepperAlone rests")
    }

    @Test
    fun fieldsAlone() {
        show {
            InDialog {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField("8", {}, label = { Text("Au moins") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField("15", {}, label = { Text("Au plus") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField("Excentrique 3 s", {}, label = { Text("Consigne") }, modifier = Modifier.fillMaxWidth())
            }
        }
        println("fieldsAlone rests")
    }

    @Test
    fun switchAlone() {
        show {
            InDialog {
                Row(Modifier.fillMaxWidth().toggleable(value = true, role = Role.Switch, onValueChange = {})) {
                    Text("Exercice d'emphase", Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = null)
                }
            }
        }
        println("switchAlone rests")
    }

    @Test
    fun restRowAlone() {
        show {
            InDialog {
                Row(Modifier.fillMaxWidth().clickable {}) {
                    Text("3 min", Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Annuler") }
                }
            }
        }
        println("restRowAlone rests")
    }

    @Test
    fun actionsAlone() {
        show {
            InDialog {
                DialogAction("Monter", Icons.Rounded.ArrowUpward, {})
                DialogAction("Retirer", Icons.Rounded.DeleteOutline, {}, destructive = true)
            }
        }
        println("actionsAlone rests")
    }
}
