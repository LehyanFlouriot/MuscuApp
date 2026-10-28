package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.GLASS_ML_RANGE
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_KCAL
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_PROTEIN
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.data.moveFood
import com.lehyanflouriot.muscuapp.core.data.moveSlot
import com.lehyanflouriot.muscuapp.core.data.removeFood
import com.lehyanflouriot.muscuapp.core.data.removeSlot
import com.lehyanflouriot.muscuapp.core.data.updateDay
import com.lehyanflouriot.muscuapp.core.data.updateMeal
import com.lehyanflouriot.muscuapp.core.data.updateSlot
import com.lehyanflouriot.muscuapp.core.domain.BODY_WEIGHT_RANGE
import com.lehyanflouriot.muscuapp.core.domain.MAX_WATER_GLASSES
import com.lehyanflouriot.muscuapp.core.domain.foodNames
import com.lehyanflouriot.muscuapp.core.domain.foodsNamed
import com.lehyanflouriot.muscuapp.core.domain.hasHistory
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.WeightGoal
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.CopyDayDialog
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.FormDialog
import com.lehyanflouriot.muscuapp.ui.common.FormField
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.NumberInputDialog
import com.lehyanflouriot.muscuapp.ui.common.fullName
import com.lehyanflouriot.muscuapp.ui.common.parseDecimal
import com.lehyanflouriot.muscuapp.ui.common.parseWholeNumber
import com.lehyanflouriot.muscuapp.ui.common.show
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Foods typed one after the other: "riz, pâtes, semoule" or "riz ↔ pâtes ↔ semoule". */
private fun parseGroup(text: String): List<String> =
    text.split(',', '↔', ';', '/').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

private fun Double.editable(): String = Fmt.number(this)

@Composable
fun PlanEditorRoute(
    planId: String,
    initialDay: DayOfWeek,
    data: AppData,
    repository: WorkoutRepository,
    onBack: () -> Unit,
) {
    val nutrition = data.nutrition
    val plan = nutrition.plan(planId)?.takeIf { it.versionOf == null }
    if (plan == null) {
        // The program was deleted (or a backup restored) while it was open.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var day by rememberSaveable { mutableStateOf(initialDay) }
    var edit by remember { mutableStateOf<PlanEdit?>(null) }
    var deletingSlot by remember { mutableStateOf<MealSlot?>(null) }
    val update: ((MealPlan) -> MealPlan) -> Unit = { repository.updatePlan(planId, it) }
    val close = { edit = null }
    val dayName = day.getDisplayName(TextStyle.FULL, Fmt.locale)

    PlanEditorScreen(
        plan = plan,
        day = day,
        keepsHistory = nutrition.hasHistory(planId),
        onDayChange = { day = it },
        onOpen = { edit = it },
        onTrainingChange = { training -> update { it.updateDay(day) { d -> d.copy(training = training) } } },
        onWaterChange = { glasses ->
            update { it.updateDay(day) { d -> d.copy(waterGlasses = glasses.coerceIn(0, MAX_WATER_GLASSES)) } }
        },
        onBack = onBack,
    )

    when (val current = edit) {
        null -> Unit
        PlanEdit.Program -> FormDialog(
            title = stringResource(R.string.plan_editor_title),
            fields = listOf(
                FormField(stringResource(R.string.plan_name), plan.name, required = true),
                FormField(stringResource(R.string.plan_info), plan.info, singleLine = false),
            ),
            onDismiss = close,
            onConfirm = { (name, info) ->
                update { it.copy(name = name, info = info) }
                close()
            },
        )
        PlanEdit.DayLabel -> FormDialog(
            title = stringResource(R.string.plan_day_label),
            fields = listOf(
                FormField(
                    label = day.fullName(),
                    initial = plan.day(day).label,
                    placeholder = stringResource(R.string.plan_day_label_hint),
                ),
            ),
            onDismiss = close,
            onConfirm = { (label) ->
                update { it.updateDay(day) { d -> d.copy(label = label) } }
                close()
            },
        )
        PlanEdit.Water -> NumberInputDialog(
            title = stringResource(R.string.plan_water_glasses),
            initialValue = plan.day(day).waterGlasses.toString(),
            allowDecimal = false,
            valueRange = 0.0..MAX_WATER_GLASSES.toDouble(),
            onDismiss = close,
            onConfirm = { value ->
                update { it.updateDay(day) { d -> d.copy(waterGlasses = value.toInt()) } }
                close()
            },
        )
        PlanEdit.WaterSettings -> FormDialog(
            title = stringResource(R.string.plan_water),
            fields = listOf(
                FormField(
                    label = stringResource(R.string.plan_glass_ml),
                    initial = plan.glassMl.toString(),
                    keyboardType = KeyboardType.Number,
                    required = true,
                    isValid = { parseWholeNumber(it, GLASS_ML_RANGE) != null },
                ),
                FormField(stringResource(R.string.plan_water_hint), plan.waterHint, singleLine = false),
            ),
            onDismiss = close,
            onConfirm = { (ml, hint) ->
                update { it.copy(glassMl = parseWholeNumber(ml, GLASS_ML_RANGE) ?: it.glassMl, waterHint = hint) }
                close()
            },
        )
        PlanEdit.CopyDay -> CopyDayDialog(
            from = day,
            title = stringResource(R.string.plan_copy_day_title, dayName),
            text = stringResource(R.string.plan_copy_day_text, dayName),
            onDismiss = close,
            onCopy = { targets ->
                repository.copyDay(planId, day, targets)
                close()
                messenger.show(context.getString(R.string.plan_copy_done))
            },
        )
        PlanEdit.Goal -> GoalDialog(
            goal = plan.weightGoal,
            onDismiss = close,
            onSave = { goal ->
                update { it.copy(weightGoal = goal) }
                close()
            },
        )
        is PlanEdit.Food -> {
            val item = current.item
            FoodDialog(
                title = stringResource(if (item == null) R.string.nutrition_food_add_title else R.string.nutrition_food_edit_title),
                supportingText = stringResource(R.string.nutrition_food_scope, dayName),
                quantity = item?.quantity.orEmpty(),
                name = item?.name.orEmpty(),
                note = item?.note.orEmpty(),
                onDismiss = close,
                suggestions = remember(plan) { plan.foodNames() },
                sameNameCount = item?.let { plan.foodsNamed(it.name) - 1 } ?: 0,
                onSave = { name, quantity, note, everywhere ->
                    if (item == null) {
                        repository.addFood(planId, day, current.slotId, name, quantity, note)
                    } else {
                        if (everywhere) repository.renameFood(planId, item.name, name)
                        repository.updateFood(planId, item.id, name, quantity, note)
                    }
                    close()
                },
                onMove = item?.let { food -> { delta -> update { it.moveFood(food.id, delta) } } },
                onDelete = item?.let { food ->
                    {
                        update { it.removeFood(food.id) }
                        close()
                    }
                },
            )
        }
        is PlanEdit.MealTargets -> {
            val slot = plan.slot(current.slotId)
            if (slot != null) {
                val meal = plan.day(day).meal(slot.id)
                val usual = listOfNotNull(slot.kcal?.let(Fmt::kcal), slot.protein?.let(Fmt::grams)).joinToString(" · ")
                FormDialog(
                    title = stringResource(R.string.plan_meal_targets_title, slot.name, dayName),
                    supportingText = stringResource(R.string.plan_meal_targets_hint) + if (usual.isEmpty()) "" else " ($usual)",
                    fields = listOf(
                        FormField(
                            label = stringResource(R.string.plan_kcal),
                            initial = meal.kcal?.toString().orEmpty(),
                            placeholder = slot.kcal?.toString(),
                            keyboardType = KeyboardType.Number,
                            isValid = { parseWholeNumber(it, 0..MAX_MEAL_KCAL) != null },
                        ),
                        FormField(
                            label = stringResource(R.string.plan_protein),
                            initial = meal.protein?.toString().orEmpty(),
                            placeholder = slot.protein?.toString(),
                            keyboardType = KeyboardType.Number,
                            isValid = { parseWholeNumber(it, 0..MAX_MEAL_PROTEIN) != null },
                        ),
                        FormField(stringResource(R.string.plan_meal_note), meal.note),
                    ),
                    onDismiss = close,
                    onConfirm = { (kcal, protein, note) ->
                        update { plan ->
                            plan.updateDay(day) { d ->
                                d.updateMeal(slot.id) {
                                    it.copy(
                                        kcal = parseWholeNumber(kcal, 0..MAX_MEAL_KCAL),
                                        protein = parseWholeNumber(protein, 0..MAX_MEAL_PROTEIN),
                                        note = note,
                                    )
                                }
                            }
                        }
                        close()
                    },
                )
            }
        }
        is PlanEdit.Slot -> {
            val slot = current.slot
            SlotDialog(
                slot = slot,
                onDismiss = close,
                onSave = { name, hint, moment, kcal, protein ->
                    if (slot == null) {
                        repository.addSlot(planId, name, hint, moment, kcal, protein)
                    } else {
                        update { it.updateSlot(slot.copy(name = name, hint = hint, moment = moment, kcal = kcal, protein = protein)) }
                    }
                    close()
                },
                onMove = slot?.let { meal -> { delta -> update { it.moveSlot(meal.id, delta) } } },
                onDelete = slot?.let { meal ->
                    {
                        deletingSlot = meal
                        close()
                    }
                },
            )
        }
        is PlanEdit.Substitution -> {
            val index = current.index
            val group = index?.let { plan.substitutions.getOrNull(it) }
            FormDialog(
                title = stringResource(R.string.plan_substitution_title),
                supportingText = stringResource(R.string.plan_substitutions_hint),
                fields = listOf(
                    FormField(
                        label = stringResource(R.string.plan_substitution_field),
                        initial = group?.joinToString(", ").orEmpty(),
                        capitalization = KeyboardCapitalization.None,
                        required = true,
                        isValid = { parseGroup(it).size >= 2 },
                    ),
                ),
                onDismiss = close,
                onConfirm = { (text) ->
                    val foods = parseGroup(text)
                    update {
                        val groups = it.substitutions.toMutableList()
                        if (index != null && index in groups.indices) groups[index] = foods else groups += listOf(foods)
                        it.copy(substitutions = groups)
                    }
                    close()
                },
                extraContent = if (index == null) {
                    null
                } else {
                    {
                        DialogAction(
                            text = stringResource(R.string.plan_substitution_delete),
                            icon = Icons.Rounded.DeleteOutline,
                            destructive = true,
                            onClick = {
                                update { it.copy(substitutions = it.substitutions.filterIndexed { i, _ -> i != index }) }
                                close()
                            },
                        )
                    }
                },
            )
        }
        is PlanEdit.Note -> {
            val note = current.note
            FormDialog(
                title = stringResource(if (note == null) R.string.plan_note_new else R.string.plan_note_edit),
                fields = listOf(
                    FormField(stringResource(R.string.plan_note_title), note?.title.orEmpty(), required = true),
                    FormField(stringResource(R.string.plan_note_text), note?.text.orEmpty(), singleLine = false),
                ),
                onDismiss = close,
                onConfirm = { (title, text) ->
                    if (note == null) {
                        repository.addNote(planId, title, text)
                    } else {
                        update { it.copy(notes = it.notes.map { n -> if (n.id == note.id) PlanNote(n.id, title, text) else n }) }
                    }
                    close()
                },
                extraContent = if (note == null) {
                    null
                } else {
                    {
                        DialogAction(
                            text = stringResource(R.string.plan_note_delete),
                            icon = Icons.Rounded.DeleteOutline,
                            destructive = true,
                            onClick = {
                                update { it.copy(notes = it.notes.filterNot { n -> n.id == note.id }) }
                                close()
                            },
                        )
                    }
                },
            )
        }
    }

    deletingSlot?.let { slot ->
        ConfirmDialog(
            title = stringResource(R.string.plan_slot_delete_title, slot.name),
            text = stringResource(R.string.plan_slot_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
            onConfirm = {
                update { it.removeSlot(slot.id) }
                deletingSlot = null
            },
            onDismiss = { deletingSlot = null },
        )
    }
}

/** The weight the program goes from and to, the pace aimed at, and what to do when it is not kept. */
@Composable
private fun GoalDialog(goal: WeightGoal?, onDismiss: () -> Unit, onSave: (WeightGoal?) -> Unit) {
    val weight = { text: String -> parseDecimal(text)?.takeIf { it in BODY_WEIGHT_RANGE } }
    val rate = { text: String -> parseDecimal(text)?.takeIf { it in -5.0..5.0 } }
    FormDialog(
        title = stringResource(R.string.plan_goal),
        supportingText = stringResource(R.string.plan_goal_rates_hint),
        fields = listOf(
            FormField(
                label = stringResource(R.string.plan_goal_start),
                initial = goal?.start?.editable().orEmpty(),
                keyboardType = KeyboardType.Decimal,
                isValid = { weight(it) != null },
            ),
            FormField(
                label = stringResource(R.string.plan_goal_target),
                initial = goal?.target?.editable().orEmpty(),
                keyboardType = KeyboardType.Decimal,
                isValid = { weight(it) != null },
            ),
            FormField(
                label = stringResource(R.string.plan_goal_min),
                initial = (goal?.minWeeklyChange ?: 0.1).editable(),
                keyboardType = KeyboardType.Text,
                required = true,
                isValid = { rate(it) != null },
            ),
            FormField(
                label = stringResource(R.string.plan_goal_max),
                initial = (goal?.maxWeeklyChange ?: 0.4).editable(),
                keyboardType = KeyboardType.Text,
                required = true,
                isValid = { rate(it) != null },
            ),
            FormField(stringResource(R.string.plan_goal_below), goal?.belowAdvice.orEmpty()),
            FormField(stringResource(R.string.plan_goal_above), goal?.aboveAdvice.orEmpty()),
        ),
        onDismiss = onDismiss,
        onConfirm = { values ->
            val low = rate(values[2]) ?: 0.1
            val high = rate(values[3]) ?: 0.4
            onSave(
                WeightGoal(
                    start = weight(values[0]),
                    target = weight(values[1]),
                    minWeeklyChange = minOf(low, high),
                    maxWeeklyChange = maxOf(low, high),
                    belowAdvice = values[4],
                    aboveAdvice = values[5],
                ),
            )
        },
        extraContent = if (goal == null) {
            null
        } else {
            {
                DialogAction(
                    text = stringResource(R.string.plan_goal_remove),
                    icon = Icons.Rounded.DeleteOutline,
                    destructive = true,
                    onClick = { onSave(null) },
                )
            }
        },
    )
}
