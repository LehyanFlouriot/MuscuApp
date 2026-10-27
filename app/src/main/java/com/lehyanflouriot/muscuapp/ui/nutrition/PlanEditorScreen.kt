package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.MAX_WATER_GLASSES
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.DayPlan
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.PlannedMeal
import com.lehyanflouriot.muscuapp.ui.common.CompactStepper
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import java.time.DayOfWeek
import java.time.format.TextStyle

/** What the user opens from the plan editor. */
sealed interface PlanEdit {
    data object Program : PlanEdit
    data object DayLabel : PlanEdit
    data object Water : PlanEdit
    data object WaterSettings : PlanEdit
    data object CopyDay : PlanEdit
    data object Goal : PlanEdit
    data class Food(val slotId: String, val item: FoodItem?) : PlanEdit
    data class MealTargets(val slotId: String) : PlanEdit
    data class Slot(val slot: MealSlot?) : PlanEdit
    data class Substitution(val index: Int?) : PlanEdit
    data class Note(val note: PlanNote?) : PlanEdit
}

/** "Lun", "Mar"... */
fun DayOfWeek.shortName(): String =
    getDisplayName(TextStyle.SHORT, Fmt.locale).removeSuffix(".").replaceFirstChar { it.titlecase(Fmt.locale) }

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, Fmt.locale).replaceFirstChar { it.titlecase(Fmt.locale) }

/**
 * The whole program on one screen: the meals of each day of the week, then what is the same
 * every day (the meals of the day, the foods that can replace one another, the weight goal, the
 * water and the tips).
 */
@Composable
fun PlanEditorScreen(
    plan: MealPlan,
    day: DayOfWeek,
    keepsHistory: Boolean,
    onDayChange: (DayOfWeek) -> Unit,
    onOpen: (PlanEdit) -> Unit,
    onTrainingChange: (Boolean) -> Unit,
    onWaterChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val reveal = rememberRevealState()
    val dayPlan = plan.day(day)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "program") {
                ProgramHeader(
                    plan = plan,
                    keepsHistory = keepsHistory,
                    onEdit = { onOpen(PlanEdit.Program) },
                    modifier = Modifier.reveal(reveal, "program"),
                )
            }
            item(key = "days") {
                DayPicker(selected = day, onSelect = onDayChange, modifier = Modifier.reveal(reveal, "days"))
            }
            item(key = "day") {
                DaySettings(
                    day = dayPlan,
                    glassMl = plan.glassMl,
                    onEditLabel = { onOpen(PlanEdit.DayLabel) },
                    onTrainingChange = onTrainingChange,
                    onWaterChange = onWaterChange,
                    onEditWater = { onOpen(PlanEdit.Water) },
                    onCopy = { onOpen(PlanEdit.CopyDay) },
                    modifier = Modifier.reveal(reveal, "day"),
                )
            }
            items(plan.slots, key = { "meal-${it.id}" }) { slot ->
                MealEditor(
                    slot = slot,
                    meal = dayPlan.meal(slot.id),
                    onEditTargets = { onOpen(PlanEdit.MealTargets(slot.id)) },
                    onAddFood = { onOpen(PlanEdit.Food(slot.id, null)) },
                    onEditFood = { onOpen(PlanEdit.Food(slot.id, it)) },
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "meal-${slot.id}"),
                )
            }
            item(key = "slots") {
                EditorSection(
                    title = stringResource(R.string.plan_slots),
                    hint = stringResource(R.string.plan_slots_hint),
                    addLabel = stringResource(R.string.plan_add_slot),
                    onAdd = { onOpen(PlanEdit.Slot(null)) },
                    modifier = Modifier.reveal(reveal, "slots"),
                ) {
                    plan.slots.forEach { slot ->
                        EditorRow(
                            icon = slot.moment.icon,
                            title = slot.name,
                            subtitle = listOfNotNull(
                                slot.hint.takeIf { it.isNotBlank() },
                                slot.kcal?.let(Fmt::kcal),
                                slot.protein?.let { "${Fmt.grams(it)} P" },
                            ).joinToString(" · "),
                            onClick = { onOpen(PlanEdit.Slot(slot)) },
                        )
                    }
                }
            }
            item(key = "substitutions") {
                EditorSection(
                    title = stringResource(R.string.plan_substitutions),
                    hint = stringResource(R.string.plan_substitutions_hint),
                    addLabel = stringResource(R.string.plan_add_substitution),
                    onAdd = { onOpen(PlanEdit.Substitution(null)) },
                    modifier = Modifier.reveal(reveal, "substitutions"),
                ) {
                    plan.substitutions.forEachIndexed { index, group ->
                        EditorRow(
                            icon = Icons.Rounded.SwapHoriz,
                            title = group.joinToString(" ↔ "),
                            onClick = { onOpen(PlanEdit.Substitution(index)) },
                        )
                    }
                }
            }
            item(key = "goal") {
                val goal = plan.weightGoal
                EditorSection(
                    title = stringResource(R.string.plan_goal),
                    addLabel = if (goal == null) stringResource(R.string.plan_goal_add) else null,
                    onAdd = { onOpen(PlanEdit.Goal) },
                    modifier = Modifier.reveal(reveal, "goal"),
                ) {
                    if (goal != null) {
                        EditorRow(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            title = stringResource(
                                R.string.plan_goal_summary,
                                goal.start?.let(Fmt::kg) ?: "?",
                                goal.target?.let(Fmt::kg) ?: "?",
                                Fmt.signedKg(goal.minWeeklyChange, maxDecimals = 2),
                                Fmt.signedKg(goal.maxWeeklyChange, maxDecimals = 2),
                            ),
                            subtitle = listOf(goal.belowAdvice, goal.aboveAdvice).filter { it.isNotBlank() }.joinToString(" · "),
                            onClick = { onOpen(PlanEdit.Goal) },
                        )
                    }
                }
            }
            item(key = "water") {
                EditorSection(
                    title = stringResource(R.string.plan_water),
                    modifier = Modifier.reveal(reveal, "water"),
                ) {
                    EditorRow(
                        icon = Icons.Rounded.WaterDrop,
                        title = stringResource(R.string.plan_glass_size, Fmt.quantity("${plan.glassMl} ml")),
                        subtitle = plan.waterHint,
                        onClick = { onOpen(PlanEdit.WaterSettings) },
                    )
                }
            }
            item(key = "notes") {
                EditorSection(
                    title = stringResource(R.string.plan_notes),
                    addLabel = stringResource(R.string.plan_add_note),
                    onAdd = { onOpen(PlanEdit.Note(null)) },
                    modifier = Modifier.reveal(reveal, "notes"),
                ) {
                    plan.notes.forEach { note ->
                        EditorRow(
                            icon = Icons.Rounded.Lightbulb,
                            title = note.title.ifBlank { note.text },
                            subtitle = if (note.title.isBlank()) "" else note.text,
                            onClick = { onOpen(PlanEdit.Note(note)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramHeader(plan: MealPlan, keepsHistory: Boolean, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = plan.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            Text(
                text = plan.info.ifBlank { stringResource(R.string.plan_info_empty) },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (keepsHistory) {
                Row(Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.plan_editor_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** The days of the week, the one being changed filled. */
@Composable
private fun DayPicker(selected: DayOfWeek, onSelect: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day == selected
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                tween(250),
                label = "day-picker",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(container)
                    .selectable(selected = isSelected, onClick = { onSelect(day) }, role = Role.Tab),
            ) {
                Text(
                    text = day.shortName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else null,
                )
            }
        }
    }
}

@Composable
private fun DaySettings(
    day: DayPlan,
    glassMl: Int,
    onEditLabel: () -> Unit,
    onTrainingChange: (Boolean) -> Unit,
    onWaterChange: (Int) -> Unit,
    onEditWater: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = day.day.fullName(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditLabel)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.plan_day_label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = day.label.ifBlank { stringResource(R.string.plan_day_label_empty) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = day.training, role = Role.Switch, onValueChange = onTrainingChange)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.plan_training_day),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = day.training, onCheckedChange = null)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.plan_water_glasses), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = Fmt.liters(day.waterGlasses * glassMl),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CompactStepper(
                    value = day.waterGlasses.toString(),
                    description = stringResource(R.string.plan_water_glasses),
                    onDecrement = { onWaterChange(day.waterGlasses - 1) },
                    onIncrement = { onWaterChange((day.waterGlasses + 1).coerceAtMost(MAX_WATER_GLASSES)) },
                    onValueClick = onEditWater,
                    canDecrement = day.waterGlasses > 0,
                )
            }
            TextButton(
                onClick = onCopy,
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.plan_copy_day))
            }
        }
    }
}

@Composable
private fun MealEditor(
    slot: MealSlot,
    meal: PlannedMeal,
    onEditTargets: () -> Unit,
    onAddFood: () -> Unit,
    onEditFood: (FoodItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kcal = meal.kcal ?: slot.kcal
    val protein = meal.protein ?: slot.protein
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.animateContentSize(tween(300, easing = Motion.EaseOut))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.action_edit), onClick = onEditTargets)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Icon(
                        slot.moment.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = slot.name, style = MaterialTheme.typography.titleMedium)
                    val targets = listOfNotNull(kcal?.let(Fmt::kcal), protein?.let { "${Fmt.grams(it)} P" }, meal.note.takeIf { it.isNotBlank() })
                    if (targets.isNotEmpty()) {
                        Text(
                            text = Fmt.typography(targets.joinToString(" · ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            if (meal.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_no_food),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            meal.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditFood(item) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = buildAnnotatedString {
                                if (item.quantity.isNotBlank()) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(Fmt.quantity(item.quantity)) }
                                    append(" ")
                                }
                                append(item.name)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (item.note.isNotBlank()) {
                            Text(
                                text = Fmt.typography(item.note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            TextButton(
                onClick = onAddFood,
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.plan_add_food))
            }
        }
    }
}

/** A titled group of rows, with an optional button to add one more. */
@Composable
private fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    addLabel: String? = null,
    onAdd: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHeader(text = title)
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                content()
                if (addLabel != null) {
                    TextButton(
                        onClick = onAdd,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(addLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorRow(icon: ImageVector, title: String, onClick: () -> Unit, subtitle: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = Fmt.typography(title), style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    text = Fmt.typography(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
