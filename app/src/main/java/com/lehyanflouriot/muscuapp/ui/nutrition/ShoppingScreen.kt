package com.lehyanflouriot.muscuapp.ui.nutrition

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RiceBowl
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.Aisle
import com.lehyanflouriot.muscuapp.core.domain.Amount
import com.lehyanflouriot.muscuapp.core.domain.AmountUnit
import com.lehyanflouriot.muscuapp.core.domain.ShoppingItem
import com.lehyanflouriot.muscuapp.core.domain.ShoppingList
import com.lehyanflouriot.muscuapp.core.domain.spoonsVolume
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.weeksBetween
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.ShoppingExtra
import com.lehyanflouriot.muscuapp.core.model.ShoppingWeek
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.FillBar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.countUp
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.common.shortName
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@get:StringRes
val Aisle.labelRes: Int
    get() = when (this) {
        Aisle.PRODUCE -> R.string.aisle_produce
        Aisle.MEAT_FISH -> R.string.aisle_meat_fish
        Aisle.DAIRY -> R.string.aisle_dairy
        Aisle.STARCHES -> R.string.aisle_starches
        Aisle.GROCERY -> R.string.aisle_grocery
        Aisle.SUPPLEMENTS -> R.string.aisle_supplements
        Aisle.OTHER -> R.string.aisle_other
    }

val Aisle.icon: ImageVector
    get() = when (this) {
        Aisle.PRODUCE -> Icons.Rounded.Eco
        Aisle.MEAT_FISH -> Icons.Rounded.SetMeal
        Aisle.DAIRY -> Icons.Rounded.Kitchen
        Aisle.STARCHES -> Icons.Rounded.RiceBowl
        Aisle.GROCERY -> Icons.Rounded.Inventory2
        Aisle.SUPPLEMENTS -> Icons.Rounded.Medication
        Aisle.OTHER -> Icons.Rounded.ShoppingBasket
    }

/** "Cette semaine", "Semaine prochaine"..., or null for weeks further away. */
@Composable
fun nutritionWeekTitle(weekStart: LocalDate, today: LocalDate): String? = when (weeksBetween(weekStart, today)) {
    0 -> stringResource(R.string.week_this)
    1 -> stringResource(R.string.week_last)
    -1 -> stringResource(R.string.nutrition_next_week)
    else -> null
}

/** "1,7 kg", "5 c. à s. + 2 c. à c.", or for how many meals a food without quantity is. */
fun ShoppingItem.quantityText(context: Context): String = when {
    amounts.isEmpty() -> context.resources.getQuantityString(R.plurals.shopping_meals, unmeasured, unmeasured)
    unmeasured > 0 -> "${Fmt.amounts(amounts)} + ${context.resources.getQuantityString(R.plurals.shopping_meals_extra, unmeasured, unmeasured)}"
    else -> Fmt.amounts(amounts)
}

/** What is left to buy, as text to send in a message or a note. */
fun shoppingText(context: Context, list: ShoppingList, week: ShoppingWeek, title: String): String {
    val checked = week.checked.toSet()
    val toBuy = list.items.filterNot { it.key in checked }
    val extras = week.extras.filterNot { it.checked }
    return buildString {
        append(context.getString(R.string.shopping_share_header, title))
        toBuy.groupBy { it.aisle }.toSortedMap().forEach { (aisle, items) ->
            append("\n\n").append(context.getString(aisle.labelRes))
            items.forEach { append("\n• ").append(it.name.capitalized()).append(" : ").append(it.quantityText(context)) }
        }
        if (extras.isNotEmpty()) {
            append("\n\n").append(context.getString(R.string.shopping_extras))
            extras.forEach { append("\n• ").append(it.name) }
        }
    }
}

/** The shopping list of the week, from the nutrition screen. */
@Composable
fun ShoppingCard(
    weekStart: LocalDate,
    today: LocalDate,
    items: Int,
    taken: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = rememberPressFeedback(pressedScale = 0.98f)
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onOpen,
        interactionSource = press.interactionSource,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier
            .then(press.modifier)
            .fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.tertiaryContainer),
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = colors.onTertiaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(R.string.shopping_title), style = MaterialTheme.typography.titleMedium)
                val summary = (nutritionWeekTitle(weekStart, today) ?: Fmt.weekRange(weekStart, today)) + " · " +
                    pluralStringResource(R.plurals.shopping_items, items, items)
                Text(
                    text = if (taken > 0) stringResource(R.string.shopping_in_cart, summary, taken) else summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

/** What the shopping list lets the user do. */
class ShoppingActions(
    val onBack: () -> Unit,
    val onWeekChange: (LocalDate) -> Unit,
    val onFromTodayChange: (Boolean) -> Unit,
    val onToggle: (String) -> Unit,
    val onToggleExtra: (String) -> Unit,
    val onRemoveExtra: (String) -> Unit,
    val onAddExtra: (String) -> Unit,
    val onClear: () -> Unit,
    val onShare: () -> Unit,
)

/**
 * Everything to buy for the meals of a week, aisle by aisle, to check off in the supermarket.
 * What is taken goes to the bottom of its aisle, so that what is left stays in sight.
 */
@Composable
fun ShoppingScreen(
    list: ShoppingList,
    week: ShoppingWeek,
    today: LocalDate,
    fromToday: Boolean,
    actions: ShoppingActions,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val reveal = rememberRevealState()
    val weekStart = week.weekStart
    val checked = week.checked.toSet()
    val checkedExtras = week.extras.filter { it.checked }.mapTo(HashSet()) { it.id }
    // What was just taken stays under the finger for a moment before going down its aisle, so that
    // several foods can be checked in a row without the list moving between two taps.
    var placed by remember(weekStart) { mutableStateOf(checked to checkedExtras) }
    LaunchedEffect(checked, checkedExtras) {
        delay(SETTLE_MS)
        placed = checked to checkedExtras
    }
    val total = list.items.size + week.extras.size
    val taken = list.items.count { it.key in checked } + week.extras.count { it.checked }
    val days = ChronoUnit.DAYS.between(list.from, list.to) + 1
    val canStartToday = weekStart == today.startOfWeek() && today != weekStart

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (total > 0) {
                        IconButton(onClick = actions.onShare) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.shopping_share))
                        }
                    }
                    // Unchecking everything says what it does: an icon alone did not.
                    if (taken > 0) {
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.shopping_clear)) },
                                    leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        actions.onClear()
                                    },
                                )
                            }
                        }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            item(key = "week") {
                ShoppingWeekHeader(
                    weekStart = weekStart,
                    today = today,
                    onPrevious = { actions.onWeekChange(weekStart.minusWeeks(1)) },
                    onNext = { actions.onWeekChange(weekStart.plusWeeks(1)) },
                    modifier = Modifier.reveal(reveal, "week"),
                )
            }
            if (canStartToday) {
                item(key = "range") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .reveal(reveal, "range"),
                    ) {
                        FilterChip(
                            selected = !fromToday,
                            onClick = { actions.onFromTodayChange(false) },
                            label = { Text(stringResource(R.string.shopping_whole_week)) },
                        )
                        FilterChip(
                            selected = fromToday,
                            onClick = { actions.onFromTodayChange(true) },
                            label = { Text(stringResource(R.string.shopping_from_today)) },
                        )
                    }
                }
            }
            if (list.items.isEmpty() && week.extras.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.ShoppingCart,
                        title = stringResource(R.string.shopping_empty_title),
                        text = stringResource(R.string.shopping_empty_text),
                        modifier = Modifier.reveal(reveal, "empty"),
                    )
                }
            } else {
                item(key = "progress") {
                    ShoppingProgress(
                        taken = taken,
                        total = total,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp)
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "progress"),
                    )
                }
            }
            list.aisles.forEach { (aisle, items) ->
                // Stable: within what is left and what is taken, the order of the list stays.
                val sorted = items.sortedBy { it.key in placed.first }
                item(key = "aisle-$aisle") {
                    AisleHeader(
                        icon = aisle.icon,
                        label = stringResource(aisle.labelRes),
                        taken = items.count { it.key in checked },
                        total = items.size,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "aisle-$aisle"),
                    )
                }
                itemsIndexed(sorted, key = { _, item -> "item-${item.key}" }) { index, item ->
                    ShoppingRow(
                        item = item,
                        checked = item.key in checked,
                        everyDay = days == 7L && item.days.size == 7,
                        shape = groupShape(index, sorted.size),
                        divider = index > 0,
                        onToggle = { actions.onToggle(item.key) },
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "item-${item.key}"),
                    )
                }
            }
            val extras = week.extras.sortedBy { it.id in placed.second }
            item(key = "extras") {
                AisleHeader(
                    icon = Icons.Rounded.AddShoppingCart,
                    label = stringResource(R.string.shopping_extras),
                    taken = extras.count { it.checked },
                    total = extras.size,
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "extras"),
                )
            }
            itemsIndexed(extras, key = { _, extra -> "extra-${extra.id}" }) { index, extra ->
                ExtraRow(
                    extra = extra,
                    shape = groupShape(index, extras.size),
                    divider = index > 0,
                    onToggle = { actions.onToggleExtra(extra.id) },
                    onRemove = { actions.onRemoveExtra(extra.id) },
                    modifier = Modifier.animateItem(fadeInSpec = null),
                )
            }
            item(key = "add") {
                AddExtraField(
                    onAdd = actions.onAddExtra,
                    modifier = Modifier
                        .padding(top = if (extras.isEmpty()) 0.dp else 8.dp)
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "add"),
                )
            }
            if (list.items.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.shopping_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 16.dp, start = 4.dp, end = 4.dp)
                            .animateItem(fadeInSpec = null),
                    )
                }
            }
        }
    }
}

private const val SETTLE_MS = 700L

/** Rows of a group look like one rounded card: only the first and the last have round corners. */
private fun groupShape(index: Int, count: Int): Shape {
    val radius = 16.dp
    return when {
        count == 1 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
        index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RectangleShape
    }
}

@Composable
private fun ShoppingWeekHeader(
    weekStart: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.week_previous))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = nutritionWeekTitle(weekStart, today)
            if (title != null) Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = Fmt.weekRange(weekStart, today),
                style = if (title != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = if (title != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.week_next))
        }
    }
}

@Composable
private fun ShoppingProgress(taken: Int, total: Int, modifier: Modifier = Modifier) {
    val success = MuscuTheme.extendedColors
    val done = total > 0 && taken == total
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) success.successContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (done) success.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedContent(
                targetState = done,
                transitionSpec = {
                    (scaleIn(tween(400, easing = EaseOutBack), initialScale = 0.8f) + fadeIn())
                        .togetherWith(scaleOut(tween(150)) + fadeOut(tween(150)))
                },
                label = "shopping-done",
            ) { isDone ->
                Text(
                    text = if (isDone) {
                        stringResource(R.string.shopping_done)
                    } else {
                        stringResource(R.string.shopping_progress, countUp(taken), total)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val color = if (done) success.success else MaterialTheme.colorScheme.onPrimaryContainer
            FillBar(
                fraction = if (total == 0) 0f else taken.toFloat() / total,
                color = color,
                trackColor = color.copy(alpha = 0.18f),
            )
        }
    }
}

@Composable
private fun AisleHeader(icon: ImageVector, label: String, taken: Int, total: Int, modifier: Modifier = Modifier) {
    val extended = MuscuTheme.extendedColors
    val done = total > 0 && taken == total
    val container by animateColorAsState(
        if (done) extended.success else MaterialTheme.colorScheme.secondaryContainer,
        tween(300),
        label = "aisle",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(container),
        ) {
            Icon(
                imageVector = if (done) Icons.Rounded.Check else icon,
                contentDescription = null,
                tint = if (done) extended.onSuccess else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                text = "$taken/$total",
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    checked: Boolean,
    everyDay: Boolean,
    shape: Shape,
    divider: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        if (checked) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
        tween(250),
        label = "shopping-text",
    )
    val context = LocalContext.current
    val volume = spoonsVolume(item.amounts)
    Surface(shape = shape, color = colors.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Column {
            if (divider) HorizontalDivider(Modifier.padding(start = 54.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodCheck(checked = checked)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Fmt.typography(item.name.capitalized()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        textDecoration = if (checked) TextDecoration.LineThrough else null,
                    )
                    Text(
                        text = if (everyDay) {
                            stringResource(R.string.shopping_every_day)
                        } else {
                            item.days.joinToString(" · ") { it.shortName() }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.quantityText(context),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.End,
                    )
                    if (volume > 0) {
                        Text(
                            text = stringResource(R.string.shopping_spoons_volume, Fmt.amount(Amount(volume, AmountUnit.MILLILITER))),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraRow(
    extra: ShoppingExtra,
    shape: Shape,
    divider: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        if (extra.checked) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
        tween(250),
        label = "extra-text",
    )
    Surface(shape = shape, color = colors.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Column {
            if (divider) HorizontalDivider(Modifier.padding(start = 54.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .toggleable(value = extra.checked, role = Role.Checkbox, onValueChange = { onToggle() })
                    .heightIn(min = 56.dp)
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodCheck(checked = extra.checked)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = extra.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    textDecoration = if (extra.checked) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.shopping_remove, extra.name),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Adds something to buy: type it and press enter, the field is ready for the next one. */
@Composable
private fun AddExtraField(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    val add = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(stringResource(R.string.shopping_add_hint)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        trailingIcon = {
            if (text.isNotBlank()) {
                IconButton(onClick = add) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.shopping_add))
                }
            }
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { add() }),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    )
}
