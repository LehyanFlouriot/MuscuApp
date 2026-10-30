package com.lehyanflouriot.muscuapp.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.buildNutritionDay
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.missedSession
import com.lehyanflouriot.muscuapp.core.domain.sessionDuration
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.lowerName
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.weight.WeightInputDialog
import com.lehyanflouriot.muscuapp.ui.weight.rememberWeighIn
import com.lehyanflouriot.muscuapp.ui.weight.saveWeighIn
import java.time.LocalDate

@Composable
fun TodayRoute(
    data: AppData,
    repository: WorkoutRepository,
    onStartSession: (LocalDate) -> Unit,
    onOpenProgram: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenRest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = rememberToday()
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val weights = data.bodyWeights
    val weighIn = rememberWeighIn(weights, today)
    val session = remember(data, today) { buildSession(data, today) }
    val missed = remember(data, today) { missedSession(data, today) }
    val nutrition = remember(data.nutrition, today) { buildNutritionDay(data.nutrition, today) }
    val programStart = data.training.schedule.firstOrNull { it.from > today }?.from
    var typing by remember { mutableStateOf(false) }

    TodayScreen(
        today = today,
        weights = weights,
        weighIn = weighIn,
        session = session,
        missed = missed,
        programStart = programStart,
        nutrition = nutrition,
        sessionDuration = data.training.sessionDuration(today),
        actions = TodayActions(
            onSaveWeight = { saveWeighIn(repository, messenger, context, today, weighIn.weight) },
            onTypeWeight = { typing = true },
            onStartSession = { onStartSession(today) },
            onDoMissed = { session ->
                repository.moveSession(from = session.date, to = today)
                messenger.show(
                    context.getString(R.string.missed_moved, session.date.dayOfWeek.lowerName()),
                    context.getString(R.string.action_undo),
                ) { repository.cancelSessionMove(today) }
            },
            onCancelMove = { repository.cancelSessionMove(today) },
            onToggleFood = { food -> repository.toggleFood(today, food.item.id) },
            onToggleMeal = { meal -> repository.setMealEaten(today, meal.slot.id, eaten = !meal.done) },
            onSkipMeal = { meal ->
                repository.setMealSkipped(today, meal.slot.id, skipped = true)
                messenger.show(
                    context.getString(R.string.today_meal_skipped, meal.slot.name),
                    context.getString(R.string.action_undo),
                ) { repository.setMealSkipped(today, meal.slot.id, skipped = false) }
            },
            onWaterChange = { glasses -> repository.setWater(today, glasses) },
            onOpenProgram = onOpenProgram,
            onOpenNutrition = onOpenNutrition,
            onOpenWeight = onOpenWeight,
            onOpenRest = onOpenRest,
            onOpenSettings = onOpenSettings,
        ),
    )

    if (typing) {
        WeightInputDialog(
            initialWeight = weighIn.weight,
            onDismiss = { typing = false },
            onConfirm = {
                weighIn.weight = it
                typing = false
            },
        )
    }
}
