package com.lehyanflouriot.muscuapp.ui.weight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.DEFAULT_BODY_WEIGHT
import com.lehyanflouriot.muscuapp.core.domain.WeightPeriod
import com.lehyanflouriot.muscuapp.core.domain.suggestedWeight
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show

@Composable
fun WeightRoute(data: AppData, repository: WorkoutRepository) {
    val today = rememberToday()
    val weights = data.bodyWeights
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val form = remember { WeighInState(suggestedWeight(weights, today) ?: DEFAULT_BODY_WEIGHT) }
    var period by rememberSaveable { mutableStateOf(WeightPeriod.THREE_MONTHS) }
    val date = form.date(today)

    // Shows the weight of the chosen day again when it changes elsewhere (history, restored backup).
    val savedWeight = weights.firstOrNull { it.date == date }?.weight
    LaunchedEffect(date, savedWeight) {
        if (savedWeight != null) form.weight = savedWeight
    }

    WeightScreen(
        weights = weights,
        today = today,
        form = form,
        period = period,
        onDateChange = { picked ->
            form.pickedDate = picked.takeIf { it != today }
            form.weight = suggestedWeight(weights, picked) ?: DEFAULT_BODY_WEIGHT
        },
        onSave = {
            val isUpdate = weights.any { it.date == date }
            repository.saveBodyWeight(date, form.weight)
            messenger.show(context.getString(if (isUpdate) R.string.weight_updated else R.string.weight_saved))
        },
        onPeriodChange = { period = it },
        onUpdate = { entry, weight ->
            repository.saveBodyWeight(entry.date, weight)
            messenger.show(context.getString(R.string.weight_updated))
        },
        onDelete = { entry ->
            val deleted = repository.deleteBodyWeight(entry.date)
            if (deleted != null) {
                messenger.show(
                    context.getString(R.string.weight_deleted),
                    context.getString(R.string.action_undo),
                ) { repository.restoreBodyWeight(deleted) }
            }
        },
    )
}
