package com.lehyanflouriot.muscuapp.ui.weight

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
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
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.Messenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import java.time.LocalDate

/** The weigh-in being noted, showing again the weight of its day when it changes elsewhere (history, backup). */
@Composable
fun rememberWeighIn(weights: List<BodyWeightEntry>, today: LocalDate): WeighInState {
    val form = remember { WeighInState(suggestedWeight(weights, today) ?: DEFAULT_BODY_WEIGHT) }
    val date = form.date(today)
    val savedWeight = weights.firstOrNull { it.date == date }?.weight
    LaunchedEffect(date, savedWeight) {
        if (savedWeight != null) form.weight = savedWeight
    }
    return form
}

/** Saves the weight of [date], saying whether it was noted or corrected. */
fun saveWeighIn(repository: WorkoutRepository, messenger: Messenger, context: Context, date: LocalDate, weight: Double) {
    val isUpdate = repository.data.value?.bodyWeights?.any { it.date == date } == true
    repository.saveBodyWeight(date, weight)
    messenger.show(context.getString(if (isUpdate) R.string.weight_updated else R.string.weight_saved))
}

/** The weight part of the Progrès tab. */
@Composable
fun WeightSection(data: AppData, repository: WorkoutRepository, padding: PaddingValues) {
    val today = rememberToday()
    val weights = data.bodyWeights
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val form = rememberWeighIn(weights, today)
    var period by rememberSaveable { mutableStateOf(WeightPeriod.THREE_MONTHS) }

    WeightContent(
        padding = padding,
        weights = weights,
        today = today,
        form = form,
        period = period,
        onDateChange = { picked ->
            form.pickedDate = picked.takeIf { it != today }
            form.weight = suggestedWeight(weights, picked) ?: DEFAULT_BODY_WEIGHT
        },
        onSave = { saveWeighIn(repository, messenger, context, form.date(today), form.weight) },
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
