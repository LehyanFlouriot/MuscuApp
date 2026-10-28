package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.data.removeFood
import com.lehyanflouriot.muscuapp.core.domain.FoodView
import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.foodNames
import com.lehyanflouriot.muscuapp.core.domain.foodsNamed
import com.lehyanflouriot.muscuapp.core.domain.hasHistory
import com.lehyanflouriot.muscuapp.core.domain.planAt
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.substitutesFor
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.NewProgramDialog
import com.lehyanflouriot.muscuapp.ui.common.ProgramsSheet
import com.lehyanflouriot.muscuapp.ui.common.StartProgramDialog
import com.lehyanflouriot.muscuapp.ui.common.announceStart
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private class Swap(val date: LocalDate, val food: FoodView)

@Composable
fun NutritionRoute(
    data: AppData,
    repository: WorkoutRepository,
    onEditPlan: (planId: String, day: DayOfWeek) -> Unit,
    onOpenWeight: () -> Unit,
    onOpenShopping: (weekStart: LocalDate?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = rememberToday()
    val nutrition = data.nutrition
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    // The day shown, as a day number so that it survives the app being recreated; null for today.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val date = pickedDay?.let(LocalDate::ofEpochDay) ?: today
    var swap by remember { mutableStateOf<Swap?>(null) }
    var editingFood by remember { mutableStateOf<Swap?>(null) }
    var showPrograms by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var starting by remember { mutableStateOf<MealPlan?>(null) }
    var deleting by remember { mutableStateOf<MealPlan?>(null) }

    NutritionScreen(
        nutrition = nutrition,
        weights = data.bodyWeights,
        today = today,
        date = date,
        actions = NutritionActions(
            onSelectDate = { pickedDay = it.takeIf { day -> day != today }?.toEpochDay() },
            onToggleFood = { day, food -> repository.toggleFood(day, food.item.id) },
            onToggleMeal = { day, meal -> repository.setMealEaten(day, meal.slot.id, eaten = !meal.done) },
            onWaterChange = { day, glasses -> repository.setWater(day, glasses) },
            onSwap = { day, food -> swap = Swap(day, food) },
            onEditPlan = { plan, day -> onEditPlan(plan.id, day) },
            onOpenPrograms = { showPrograms = true },
            onCreateProgram = { creating = true },
            onOpenWeight = onOpenWeight,
            // From today, the list is the one of the week to shop for; from another day, of its week.
            onOpenShopping = { day -> onOpenShopping(day.takeIf { it != today }?.startOfWeek()) },
            onOpenSettings = onOpenSettings,
        ),
        // What each day is about comes from the training program, when one is followed.
        dayType = { day ->
            buildSession(data, day)?.let { session -> DayType(label = if (session.isRest) "" else session.day.title, training = !session.isRest) }
        },
    )

    swap?.let { current ->
        val plan = nutrition.planAt(current.date)
        val dayName = if (current.date == today) {
            stringResource(R.string.date_today).lowercase(Fmt.locale)
        } else {
            Fmt.longDay(current.date)
        }
        SwapDialog(
            food = current.food,
            dayName = dayName,
            substitutes = plan?.substitutesFor(current.food.item.name).orEmpty(),
            onDismiss = { swap = null },
            onSwap = { name, quantity ->
                repository.swapFood(current.date, current.food.item.id, name, quantity)
                swap = null
            },
            onRestore = {
                repository.clearSwap(current.date, current.food.item.id)
                swap = null
            },
            onEditInPlan = {
                swap = null
                editingFood = current
            },
        )
    }

    editingFood?.let { current ->
        val program = nutrition.programAt(current.date)
        val item = current.food.item
        if (program != null) {
            val dayName = current.date.dayOfWeek.getDisplayName(TextStyle.FULL, Fmt.locale)
            FoodDialog(
                title = stringResource(R.string.nutrition_food_edit_title),
                supportingText = stringResource(R.string.nutrition_food_scope, dayName),
                quantity = item.quantity,
                name = item.name,
                note = item.note,
                onDismiss = { editingFood = null },
                suggestions = remember(program) { program.foodNames() },
                sameNameCount = program.foodsNamed(item.name) - 1,
                onSave = { name, quantity, note, everywhere ->
                    if (everywhere) repository.renameFood(program.id, item.name, name)
                    repository.updateFood(program.id, item.id, name, quantity, note)
                    editingFood = null
                },
                onDelete = {
                    repository.updatePlan(program.id) { it.removeFood(item.id) }
                    editingFood = null
                },
            )
        }
    }

    if (showPrograms) {
        ProgramsSheet(
            infos = remember(nutrition, today) { nutrition.programInfos(today) },
            today = today,
            onDismiss = { showPrograms = false },
            onEdit = { plan ->
                showPrograms = false
                onEditPlan(plan.id, date.dayOfWeek)
            },
            onStart = { starting = it },
            onCancelStart = { info ->
                info.date?.let { repository.cancelProgramStart(info.plan.id, it) }
            },
            onDelete = { plan ->
                val active = nutrition.programInfos(today).any { it.plan.id == plan.id && it.status == ProgramStatus.ACTIVE }
                if (active || nutrition.hasHistory(plan.id)) {
                    messenger.show(context.getString(R.string.nutrition_plan_cannot_delete))
                } else {
                    deleting = plan
                }
            },
            onNew = { creating = true },
            title = stringResource(R.string.nutrition_programs),
            newLabel = stringResource(R.string.nutrition_plan_new),
        )
    }

    if (creating) {
        NewProgramDialog(
            current = nutrition.programAt(today),
            today = today,
            title = stringResource(R.string.nutrition_plan_new),
            onDismiss = { creating = false },
            onCreate = { name, copyOf, start ->
                val id = repository.createProgram(name, copyOf, start)
                creating = false
                showPrograms = false
                if (start != null) announceStart(messenger, context, name.trim(), start, today)
                onEditPlan(id, start?.dayOfWeek ?: date.dayOfWeek)
            },
        )
    }

    starting?.let { plan ->
        StartProgramDialog(
            plan = plan,
            today = today,
            onDismiss = { starting = null },
            onStart = { start ->
                repository.startProgram(plan.id, start)
                starting = null
                announceStart(messenger, context, plan.name, start, today)
            },
        )
    }

    deleting?.let { plan ->
        ConfirmDialog(
            title = stringResource(R.string.nutrition_plan_delete_title),
            text = stringResource(R.string.program_delete_text, plan.name),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
            onConfirm = {
                if (repository.deleteProgram(plan.id)) messenger.show(context.getString(R.string.nutrition_plan_deleted))
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
