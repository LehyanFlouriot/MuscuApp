package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_KCAL
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_PROTEIN
import com.lehyanflouriot.muscuapp.core.domain.FoodView
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.FormDialog
import com.lehyanflouriot.muscuapp.ui.common.FormField
import com.lehyanflouriot.muscuapp.ui.common.parseWholeNumber

/** The food of a meal: what it is, how much, and an optional note. */
@Composable
fun FoodDialog(
    title: String,
    quantity: String,
    name: String,
    note: String,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: String, note: String, everywhere: Boolean) -> Unit,
    supportingText: String? = null,
    /** Names of the other foods of the program, offered while typing. */
    suggestions: List<String> = emptyList(),
    /** How many other foods of the program have the same name, to rename them all at once. */
    sameNameCount: Int = 0,
    onMove: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var everywhere by remember { mutableStateOf(true) }
    FormDialog(
        title = title,
        supportingText = supportingText,
        fields = listOf(
            FormField(
                label = stringResource(R.string.nutrition_food_name),
                initial = name,
                capitalization = KeyboardCapitalization.None,
                required = true,
                suggestions = suggestions,
            ),
            FormField(
                label = stringResource(R.string.nutrition_food_quantity),
                initial = quantity,
                placeholder = stringResource(R.string.nutrition_food_quantity_hint),
                capitalization = KeyboardCapitalization.None,
            ),
            FormField(label = stringResource(R.string.nutrition_food_note), initial = note),
        ),
        onDismiss = onDismiss,
        onConfirm = { (newName, newQuantity, newNote) -> onSave(newName, newQuantity, newNote, everywhere && sameNameCount > 0) },
        extraContent = if (onMove == null && onDelete == null && sameNameCount == 0) {
            null
        } else {
            {
                Column {
                    if (sameNameCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(value = everywhere, role = Role.Checkbox, onValueChange = { everywhere = it }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = everywhere, onCheckedChange = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = pluralStringResource(R.plurals.nutrition_rename_everywhere, sameNameCount, sameNameCount, name),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (onMove != null) {
                        Row {
                            DialogAction(stringResource(R.string.nutrition_move_up), Icons.Rounded.ArrowUpward, { onMove(-1) })
                            DialogAction(stringResource(R.string.nutrition_move_down), Icons.Rounded.ArrowDownward, { onMove(1) })
                        }
                    }
                    if (onDelete != null) {
                        DialogAction(stringResource(R.string.nutrition_food_delete), Icons.Rounded.DeleteOutline, onDelete, destructive = true)
                    }
                }
            }
        },
    )
}

/** Eats something else, for a single day: ideas come from the substitutions of the program. */
@Composable
fun SwapDialog(
    food: FoodView,
    dayName: String,
    substitutes: List<String>,
    onDismiss: () -> Unit,
    onSwap: (name: String, quantity: String) -> Unit,
    onRestore: () -> Unit,
    onEditInPlan: () -> Unit,
) {
    var name by remember { mutableStateOf(food.swap?.name.orEmpty()) }
    var quantity by remember { mutableStateOf(food.quantity) }
    val planned = listOf(food.item.quantity, food.item.name).filter { it.isNotBlank() }.joinToString(" ")
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
        title = { Text(stringResource(R.string.nutrition_swap_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.nutrition_swap_planned, Fmt.typography(planned)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (substitutes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.nutrition_swap_ideas),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            substitutes.forEach { substitute ->
                                FilterChip(
                                    selected = name.trim().equals(substitute, ignoreCase = true),
                                    onClick = { name = substitute },
                                    label = { Text(substitute) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nutrition_food_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.nutrition_food_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSwap(name, quantity) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.nutrition_swap_hint, dayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                    if (food.swap != null) {
                        DialogAction(stringResource(R.string.nutrition_swap_restore), Icons.AutoMirrored.Rounded.Undo, onRestore)
                    }
                    DialogAction(stringResource(R.string.nutrition_swap_edit_plan), Icons.Rounded.EditNote, onEditInPlan)
                }
            }
            // Ideas are a tap away: the keyboard only opens at once when there are none.
            if (substitutes.isEmpty()) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = { onSwap(name, quantity) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.nutrition_swap_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}


// region Plan editor

/** A meal of every day: its name, when to eat it, its icon and its usual targets. */
@Composable
fun SlotDialog(
    slot: MealSlot?,
    onDismiss: () -> Unit,
    onSave: (name: String, hint: String, moment: MealMoment, kcal: Int?, protein: Int?) -> Unit,
    onMove: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var moment by remember { mutableStateOf(slot?.moment ?: MealMoment.SNACK) }
    FormDialog(
        title = stringResource(if (slot == null) R.string.plan_slot_new else R.string.plan_slot_edit),
        fields = listOf(
            FormField(stringResource(R.string.plan_slot_name), slot?.name.orEmpty(), required = true),
            FormField(stringResource(R.string.plan_slot_hint), slot?.hint.orEmpty()),
            FormField(
                label = stringResource(R.string.plan_kcal),
                initial = slot?.kcal?.toString().orEmpty(),
                keyboardType = KeyboardType.Number,
                isValid = { parseWholeNumber(it, 0..MAX_MEAL_KCAL) != null },
            ),
            FormField(
                label = stringResource(R.string.plan_protein),
                initial = slot?.protein?.toString().orEmpty(),
                keyboardType = KeyboardType.Number,
                isValid = { parseWholeNumber(it, 0..MAX_MEAL_PROTEIN) != null },
            ),
        ),
        onDismiss = onDismiss,
        onConfirm = { (name, hint, kcal, protein) ->
            onSave(name, hint, moment, parseWholeNumber(kcal, 0..MAX_MEAL_KCAL), parseWholeNumber(protein, 0..MAX_MEAL_PROTEIN))
        },
        extraContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.plan_slot_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealMoment.entries.forEach { option ->
                        FilterChip(
                            selected = option == moment,
                            onClick = { moment = option },
                            label = { Text(stringResource(option.labelRes)) },
                            leadingIcon = {
                                Icon(option.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                            },
                        )
                    }
                }
                if (onMove != null) {
                    Row {
                        DialogAction(stringResource(R.string.nutrition_move_up), Icons.Rounded.ArrowUpward, { onMove(-1) })
                        DialogAction(stringResource(R.string.nutrition_move_down), Icons.Rounded.ArrowDownward, { onMove(1) })
                    }
                }
                if (onDelete != null) {
                    DialogAction(stringResource(R.string.plan_slot_delete), Icons.Rounded.DeleteOutline, onDelete, destructive = true)
                }
            }
        },
    )
}

// endregion
