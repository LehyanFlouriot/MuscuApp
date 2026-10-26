package com.lehyanflouriot.muscuapp.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.SelfImprovement
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
        MuscleGroup.LEGS -> R.string.group_legs
        MuscleGroup.GLUTES -> R.string.group_glutes
        MuscleGroup.ABS -> R.string.group_abs
        MuscleGroup.CARDIO -> R.string.group_cardio
        MuscleGroup.OTHER -> R.string.group_other
    }

/** Icon describing how an exercise is done. */
val Exercise.icon: ImageVector
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
