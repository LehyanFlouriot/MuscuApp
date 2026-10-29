package com.lehyanflouriot.muscuapp.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.NordicWalking
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Sports
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SportsKabaddi
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.WorkSet

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.group_chest
        MuscleGroup.BACK -> R.string.group_back
        MuscleGroup.SHOULDERS -> R.string.group_shoulders
        MuscleGroup.BICEPS -> R.string.group_biceps
        MuscleGroup.TRICEPS -> R.string.group_triceps
        MuscleGroup.QUADS -> R.string.group_quads
        MuscleGroup.HAMSTRINGS -> R.string.group_hamstrings
        MuscleGroup.GLUTES -> R.string.group_glutes
        MuscleGroup.CALVES -> R.string.group_calves
        MuscleGroup.LEGS -> R.string.group_legs
        MuscleGroup.ABS -> R.string.group_abs
        MuscleGroup.CARDIO -> R.string.group_cardio
        MuscleGroup.OTHER -> R.string.group_other
    }

/** Icon of a muscle group, so that exercises are told apart at a glance. */
val MuscleGroup.icon: ImageVector
    get() = when (this) {
        MuscleGroup.CHEST -> Icons.Rounded.SportsGymnastics
        MuscleGroup.BACK -> Icons.Rounded.Rowing
        MuscleGroup.SHOULDERS -> Icons.Rounded.AccessibilityNew
        MuscleGroup.BICEPS -> Icons.Rounded.FitnessCenter
        MuscleGroup.TRICEPS -> Icons.Rounded.SportsMartialArts
        MuscleGroup.QUADS, MuscleGroup.LEGS -> Icons.Rounded.Hiking
        MuscleGroup.HAMSTRINGS -> Icons.Rounded.NordicWalking
        MuscleGroup.GLUTES -> Icons.Rounded.SportsKabaddi
        MuscleGroup.CALVES -> Icons.AutoMirrored.Rounded.DirectionsWalk
        MuscleGroup.ABS -> Icons.Rounded.SelfImprovement
        MuscleGroup.CARDIO -> Icons.AutoMirrored.Rounded.DirectionsRun
        MuscleGroup.OTHER -> Icons.Rounded.Sports
    }

/** Icon of an exercise: the one of its muscle group. */
val Exercise.icon: ImageVector get() = muscleGroup.icon

/** Icon describing how an exercise is done: with a load, in seconds, or with the body weight. */
val Exercise.typeIcon: ImageVector
    get() = when {
        measure == Measure.SECONDS -> Icons.Rounded.Timer
        usesWeight -> Icons.Rounded.FitnessCenter
        else -> Icons.Rounded.SelfImprovement
    }

/** "Répétitions" or "Secondes", for the value of a set. */
@Composable
fun repsLabel(measure: Measure): String =
    stringResource(if (measure == Measure.SECONDS) R.string.field_seconds else R.string.field_reps)

/** "4 × 10 · 60 kg" for the sets of an exercise. */
fun Exercise.describe(sets: List<WorkSet>): String = Fmt.sets(sets, measure, showWeight = usesWeight)

/** "12 séries", "1 série"... */
@Composable
fun setsCount(count: Int): String = pluralStringResource(R.plurals.sets_count, count, count)

/** Total of reps, or seconds for timed exercises. */
@Composable
fun repsTotal(total: Int, measure: Measure): String =
    if (measure == Measure.SECONDS) {
        stringResource(R.string.seconds_total, Fmt.number(total))
    } else {
        pluralStringResource(R.plurals.reps_count, total, Fmt.number(total))
    }
