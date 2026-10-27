package com.lehyanflouriot.muscuapp.ui.nutrition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.FoodView
import com.lehyanflouriot.muscuapp.core.domain.MealView
import com.lehyanflouriot.muscuapp.core.domain.NutritionDayView
import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.domain.WeightAdvice
import com.lehyanflouriot.muscuapp.core.domain.WeightGuidance
import com.lehyanflouriot.muscuapp.core.domain.buildNutritionDay
import com.lehyanflouriot.muscuapp.core.domain.buildShoppingList
import com.lehyanflouriot.muscuapp.core.domain.defaultShoppingWeek
import com.lehyanflouriot.muscuapp.core.domain.nutritionWeek
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.programOf
import com.lehyanflouriot.muscuapp.core.domain.programs
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.weeksBetween
import com.lehyanflouriot.muscuapp.core.domain.weightGuidance
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.DayPlan
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.FillBar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.RevealState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.countUp
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** What the nutrition screen lets the user do. */
class NutritionActions(
    val onSelectDate: (LocalDate) -> Unit,
    val onToggleFood: (LocalDate, FoodView) -> Unit,
    val onToggleMeal: (LocalDate, MealView) -> Unit,
    val onWaterChange: (LocalDate, Int) -> Unit,
    val onSwap: (LocalDate, FoodView) -> Unit,
    val onEditPlan: (MealPlan, DayOfWeek) -> Unit,
    val onOpenPrograms: () -> Unit,
    val onCreateProgram: () -> Unit,
    val onOpenWeight: () -> Unit,
    /** Opens the shopping list of the week of the day shown. */
    val onOpenShopping: (LocalDate) -> Unit,
)

/**
 * The meals of a day, to check what was eaten: the week at the top, then the rings of the day,
 * the meals one after the other, the water and the tips of the program.
 */
@Composable
fun NutritionScreen(
    nutrition: Nutrition,
    weights: List<BodyWeightEntry>,
    today: LocalDate,
    date: LocalDate,
    actions: NutritionActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val reveal = rememberRevealState()
    val weekStart = date.startOfWeek()
    val week = remember(nutrition, weekStart) { nutritionWeek(nutrition, weekStart) }
    val program = nutrition.programAt(date) ?: nutrition.programAt(today)
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val shownDate by rememberUpdatedState(date)
    val selectDate by rememberUpdatedState(actions.onSelectDate)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nutrition_title)) },
                actions = {
                    AnimatedVisibility(visible = date != today, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        IconButton(onClick = { actions.onSelectDate(today) }) {
                            Icon(Icons.Rounded.Today, contentDescription = stringResource(R.string.nutrition_back_to_today))
                        }
                    }
                    IconButton(onClick = { actions.onOpenShopping(date) }) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = stringResource(R.string.shopping_title))
                    }
                    if (program != null) {
                        IconButton(onClick = { actions.onEditPlan(program, date.dayOfWeek) }) {
                            Icon(Icons.Rounded.EditNote, contentDescription = stringResource(R.string.nutrition_edit_plan))
                        }
                    }
                    IconButton(onClick = actions.onOpenPrograms) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = stringResource(R.string.nutrition_programs))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            WeekStrip(
                days = week,
                selected = date,
                today = today,
                onSelect = actions.onSelectDate,
                onPrevious = { actions.onSelectDate(date.minusWeeks(1)) },
                onNext = { actions.onSelectDate(date.plusWeeks(1)) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .reveal(reveal, "week"),
            )
            var dragTotal by remember { mutableFloatStateOf(0f) }
            AnimatedContent(
                targetState = date,
                transitionSpec = {
                    // Earlier days come from the left and later ones from the right, as when swiping.
                    val direction = if (targetState > initialState) 1 else -1
                    (
                        slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { it * direction / 3 } +
                            fadeIn(tween(250, delayMillis = 60))
                        ).togetherWith(
                        slideOutHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { -it * direction / 3 } +
                            fadeOut(tween(150)),
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Swipe left/right to change day.
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                when {
                                    dragTotal > swipeThreshold -> selectDate(shownDate.minusDays(1))
                                    dragTotal < -swipeThreshold -> selectDate(shownDate.plusDays(1))
                                }
                            },
                            onHorizontalDrag = { change, amount ->
                                dragTotal += amount
                                change.consume()
                            },
                        )
                    },
                label = "nutrition-day",
            ) { shown ->
                DayList(
                    nutrition = nutrition,
                    weights = weights,
                    today = today,
                    date = shown,
                    reveal = reveal,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun DayList(
    nutrition: Nutrition,
    weights: List<BodyWeightEntry>,
    today: LocalDate,
    date: LocalDate,
    reveal: RevealState,
    actions: NutritionActions,
) {
    val view = remember(nutrition, date) { buildNutritionDay(nutrition, date) }
    val canCheck = date <= today
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (view == null) {
            item(key = "no-plan") {
                NoPlan(
                    nutrition = nutrition,
                    onCreateProgram = actions.onCreateProgram,
                    onOpenPrograms = actions.onOpenPrograms,
                    modifier = Modifier.reveal(reveal, "no-plan"),
                )
            }
            return@LazyColumn
        }
        val program = nutrition.programOf(view.plan)
        item(key = "summary") {
            DaySummaryCard(
                view = view,
                today = today,
                modifier = Modifier
                    .animateItem(fadeInSpec = null)
                    .reveal(reveal, "summary"),
            )
        }
        val goal = program.weightGoal
        if (date == today && goal != null) {
            item(key = "guidance") {
                val guidance = remember(goal, weights, today) { weightGuidance(goal, weights, today) }
                GuidanceCard(
                    guidance = guidance,
                    today = today,
                    onOpenWeight = actions.onOpenWeight,
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "guidance"),
                )
            }
        }
        if (!canCheck) {
            item(key = "future") {
                Text(
                    text = stringResource(R.string.nutrition_future_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "future"),
                )
            }
        }
        val next = view.nextMeal?.slot?.id
        items(view.meals, key = { "meal-${it.slot.id}" }) { meal ->
            MealCard(
                meal = meal,
                date = date,
                canCheck = canCheck,
                highlighted = date == today && meal.slot.id == next,
                onToggleFood = { actions.onToggleFood(date, it) },
                onToggleMeal = { actions.onToggleMeal(date, meal) },
                onSwap = { actions.onSwap(date, it) },
                modifier = Modifier
                    .animateItem(fadeInSpec = null)
                    .reveal(reveal, "meal-${meal.slot.id}"),
            )
        }
        if (view.waterTarget > 0) {
            item(key = "water") {
                WaterCard(
                    water = view.water,
                    target = view.waterTarget,
                    glassMl = view.plan.glassMl,
                    hint = view.plan.waterHint,
                    canChange = canCheck,
                    onChange = { actions.onWaterChange(date, it) },
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "water"),
                )
            }
        }
        item(key = "shopping") {
            // On the weekend, today's list is the one of the week to come.
            val shoppingWeek = if (date == today) defaultShoppingWeek(today) else date.startOfWeek()
            val list = remember(nutrition, shoppingWeek) { buildShoppingList(nutrition, shoppingWeek, shoppingWeek.plusDays(6)) }
            val cart = nutrition.shoppingWeek(shoppingWeek)
            val checked = cart.checked.toSet()
            ShoppingCard(
                weekStart = shoppingWeek,
                today = today,
                items = list.items.size + cart.extras.size,
                taken = list.items.count { it.key in checked } + cart.extras.count { it.checked },
                onOpen = { actions.onOpenShopping(date) },
                modifier = Modifier
                    .animateItem(fadeInSpec = null)
                    .reveal(reveal, "shopping"),
            )
        }
        if (view.plan.notes.isNotEmpty()) {
            item(key = "tips") {
                SectionHeader(
                    text = stringResource(R.string.nutrition_tips),
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "tips"),
                )
            }
            items(view.plan.notes, key = { "note-${it.id}" }) { note ->
                NoteCard(
                    note = note,
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "note-${note.id}"),
                )
            }
        }
        item(key = "program") {
            val since = remember(nutrition, today, program.id) {
                nutrition.programInfos(today).firstOrNull { it.plan.id == program.id && it.status == ProgramStatus.ACTIVE }?.date
            }
            ProgramCard(
                program = program,
                since = since,
                today = today,
                onEdit = { actions.onEditPlan(program, date.dayOfWeek) },
                onOpenPrograms = actions.onOpenPrograms,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .animateItem(fadeInSpec = null)
                    .reveal(reveal, "program"),
            )
        }
    }
}

// region Week

/** Title of a week: "Cette semaine", "Semaine prochaine"... with its days below. */
@Composable
private fun WeekStrip(
    days: List<Pair<LocalDate, Double?>>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = days.first().first
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.week_previous))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val title = when (weeksBetween(weekStart, today)) {
                    0 -> stringResource(R.string.week_this)
                    1 -> stringResource(R.string.week_last)
                    -1 -> stringResource(R.string.nutrition_next_week)
                    else -> null
                }
                if (title != null) Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = Fmt.weekRange(weekStart, today),
                    style = if (title != null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
                    color = if (title != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.week_next))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { (date, progress) ->
                DayCell(
                    date = date,
                    progress = progress,
                    selected = date == selected,
                    isToday = date == today,
                    onClick = { onSelect(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    progress: Double?,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    val description = stringResource(R.string.nutrition_day_cd, Fmt.longDay(date), Fmt.percent(progress ?: 0.0))
    val disc = animateColorAsState(if (selected) colors.primary else Color.Transparent, tween(250), label = "day-disc")
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = description }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Fmt.locale),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) colors.primary else colors.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else null,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(Modifier.size(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clearAndSetSemantics { },
        ) {
            if (progress != null) {
                ProgressRing(
                    fraction = progress.toFloat(),
                    color = if (progress >= 1.0) success else colors.primary,
                    trackColor = colors.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Canvas(Modifier.size(30.dp)) { drawCircle(disc.value) }
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    selected -> colors.onPrimary
                    isToday -> colors.primary
                    else -> colors.onSurface
                },
                fontWeight = if (selected || isToday) FontWeight.Bold else null,
            )
        }
    }
}

// endregion

// region Day

/** "Aujourd'hui", "Hier", "Demain" or null. */
@Composable
private fun relativeDayName(date: LocalDate, today: LocalDate): String? = when (date) {
    today -> stringResource(R.string.date_today)
    today.minusDays(1) -> stringResource(R.string.date_yesterday)
    today.plusDays(1) -> stringResource(R.string.nutrition_tomorrow)
    else -> null
}


private fun share(value: Double, target: Int?): Float =
    if (target == null || target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

@Composable
private fun DaySummaryCard(view: NutritionDayView, today: LocalDate, modifier: Modifier = Modifier) {
    val rings = ringColors()
    val future = view.date > today
    val kcalTarget = view.kcalTarget
    val proteinTarget = view.proteinTarget
    val kcal = countUp(view.kcalEaten.roundToInt(), delayMillis = 150)
    val protein = countUp(view.proteinEaten.roundToInt(), delayMillis = 270)
    val water = countUp(view.water, delayMillis = 390)
    val kcalText = kcalTarget?.let { if (future) Fmt.kcal(it) else stringResource(R.string.nutrition_amount_of, Fmt.number(kcal), Fmt.kcal(it)) }
    val proteinText = proteinTarget?.let {
        if (future) Fmt.grams(it) else stringResource(R.string.nutrition_amount_of, Fmt.number(protein), Fmt.grams(it))
    }
    val glasses = pluralStringResource(R.plurals.nutrition_glasses, view.waterTarget, view.waterTarget)
    val waterText = if (future) glasses else stringResource(R.string.nutrition_amount_of, Fmt.number(water), glasses)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(tween(350, easing = Motion.EaseOut)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val relative = relativeDayName(view.date, today)
                    Text(
                        text = relative ?: Fmt.longDay(view.date).capitalized(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (relative != null) {
                        Text(
                            text = Fmt.longDay(view.date).capitalized(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DayTypeChip(view.dayPlan)
            }
            // A ring for each target of the program: calories, protein and water.
            val shown = listOfNotNull(
                kcalText?.let { Triple(Ring(rings.kcal, if (future) 0f else share(view.kcalEaten, kcalTarget)), R.string.nutrition_kcal, it) },
                proteinText?.let {
                    Triple(Ring(rings.protein, if (future) 0f else share(view.proteinEaten, proteinTarget)), R.string.nutrition_protein, it)
                },
                waterText.takeIf { view.waterTarget > 0 }?.let {
                    Triple(Ring(rings.water, if (future) 0f else share(view.water.toDouble(), view.waterTarget)), R.string.nutrition_water, it)
                },
            )
            if (shown.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActivityRings(rings = shown.map { it.first }, modifier = Modifier.size(112.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shown.forEach { (ring, label, value) -> RingLegend(ring.color, label, value) }
                    }
                }
            }
            when {
                future -> Text(
                    text = pluralStringResource(R.plurals.nutrition_foods_planned, view.foodCount, view.foodCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                view.perfect -> PerfectDay()
                view.allEaten && view.waterTarget > view.water -> Text(
                    text = pluralStringResource(R.plurals.nutrition_water_left, view.waterTarget - view.water, view.waterTarget - view.water),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = rings.water,
                )
            }
        }
    }
}

@Composable
private fun RingLegend(color: Color, label: Int, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "Pectoraux" with a dumbbell on training days, "Repos" with a leaf otherwise. */
@Composable
private fun DayTypeChip(day: DayPlan) {
    val label = day.label.ifBlank {
        stringResource(if (day.training) R.string.nutrition_training else R.string.nutrition_rest_day)
    }
    // Discreet next to the rings, which are what the eye should go to.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (day.training) Icons.Rounded.FitnessCenter else Icons.Rounded.Spa,
                contentDescription = null,
                tint = if (day.training) MaterialTheme.colorScheme.primary else MuscuTheme.extendedColors.success,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Pops in when everything is eaten and drunk. */
@Composable
private fun PerfectDay() {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(pop) { pop.animateTo(1f, tween(500, easing = EaseOutBack)) }
    val success = MuscuTheme.extendedColors
    Surface(
        shape = MaterialTheme.shapes.large,
        color = success.successContainer,
        contentColor = success.onSuccessContainer,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
            },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Celebration, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.nutrition_perfect_day),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// endregion

// region Meals

private fun MealView.subtitle(): String {
    val parts = listOfNotNull(
        slot.hint.takeIf { it.isNotBlank() },
        kcal?.let(Fmt::kcal),
        protein?.let { "${Fmt.grams(it)} P" },
    )
    return Fmt.typography(parts.joinToString(" · "))
}

@Composable
private fun MealCard(
    meal: MealView,
    date: LocalDate,
    canCheck: Boolean,
    highlighted: Boolean,
    onToggleFood: (FoodView) -> Unit,
    onToggleMeal: () -> Unit,
    onSwap: (FoodView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extended = MuscuTheme.extendedColors
    val done = meal.done
    var expanded by rememberSaveable(date, meal.slot.id) { mutableStateOf(!done || !canCheck) }
    // A meal just finished folds away after a moment, to leave room for the next one.
    LaunchedEffect(done) {
        if (done && expanded && canCheck) {
            delay(700)
            expanded = false
        }
    }
    val container by animateColorAsState(
        if (done && canCheck) extended.successContainer.copy(alpha = 0.45f) else colors.surfaceContainerLow,
        tween(400),
        label = "meal-container",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (highlighted) BorderStroke(2.dp, colors.primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.animateContentSize(tween(350, easing = Motion.EaseOut))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(if (expanded) R.string.action_hide else R.string.action_show),
                    ) { expanded = !expanded }
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MealBadge(meal = meal, done = done && canCheck)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.slot.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (highlighted) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary) {
                                Text(
                                    text = stringResource(R.string.nutrition_next_meal),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = if (done && canCheck && !expanded) stringResource(R.string.nutrition_meal_done) else meal.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (done && canCheck) extended.success else colors.onSurfaceVariant,
                    )
                }
                if (canCheck && meal.foods.isNotEmpty()) {
                    Text(
                        text = "${meal.eatenCount}/${meal.foods.size}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = colors.onSurfaceVariant,
                    )
                    val state = when {
                        done -> ToggleableState.On
                        meal.eatenCount > 0 -> ToggleableState.Indeterminate
                        else -> ToggleableState.Off
                    }
                    val description = stringResource(
                        if (done) R.string.nutrition_uncheck_meal else R.string.nutrition_check_meal,
                        meal.slot.name,
                    )
                    TriStateCheckbox(
                        state = state,
                        onClick = onToggleMeal,
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                } else {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(350, easing = Motion.EaseOut)) + fadeIn(tween(250, delayMillis = 80)),
                exit = shrinkVertically(tween(300, easing = Motion.EaseOut)) + fadeOut(tween(150)),
            ) {
                Column(Modifier.padding(bottom = 8.dp)) {
                    if (meal.note.isNotBlank()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Fmt.typography(meal.note),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    meal.foods.forEach { food ->
                        FoodRow(
                            food = food,
                            canCheck = canCheck,
                            onToggle = { onToggleFood(food) },
                            onSwap = { onSwap(food) },
                        )
                    }
                }
            }
        }
    }
}

/** Icon of the meal, which becomes a check once everything is eaten. */
@Composable
private fun MealBadge(meal: MealView, done: Boolean) {
    val colors = MaterialTheme.colorScheme
    val extended = MuscuTheme.extendedColors
    val container by animateColorAsState(if (done) extended.success else colors.secondaryContainer, tween(300), label = "badge")
    val content by animateColorAsState(if (done) extended.onSuccess else colors.onSecondaryContainer, tween(300), label = "badge-icon")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container),
    ) {
        AnimatedContent(
            targetState = done,
            transitionSpec = {
                (scaleIn(tween(350, easing = EaseOutBack), initialScale = 0.4f) + fadeIn(tween(150)))
                    .togetherWith(scaleOut(tween(150)) + fadeOut(tween(150)))
            },
            label = "meal-badge",
        ) { isDone ->
            Icon(
                imageVector = if (isDone) Icons.Rounded.Check else meal.slot.moment.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun FoodRow(food: FoodView, canCheck: Boolean, onToggle: () -> Unit, onSwap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        if (food.eaten && canCheck) colors.onSurfaceVariant else colors.onSurface,
        tween(250),
        label = "food-text",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canCheck) Modifier.toggleable(value = food.eaten, role = Role.Checkbox, onValueChange = { onToggle() }) else Modifier,
            )
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canCheck) {
            FoodCheck(checked = food.eaten)
        } else {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.outline),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    if (food.quantity.isNotBlank()) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(Fmt.quantity(food.quantity)) }
                        append(" ")
                    }
                    append(Fmt.typography(food.name))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            if (food.swap != null) {
                val planned = listOf(food.item.quantity, food.item.name).filter { it.isNotBlank() }.joinToString(" ")
                Text(
                    text = stringResource(R.string.nutrition_swapped_from, Fmt.typography(planned)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.tertiary,
                )
            }
            if (food.item.note.isNotBlank()) {
                Text(
                    text = Fmt.typography(food.item.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onSwap) {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = stringResource(R.string.nutrition_swap_food, food.name),
                tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

// endregion

// region Water

@Composable
private fun WaterCard(
    water: Int,
    target: Int,
    glassMl: Int,
    hint: String,
    canChange: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val waterColor = ringColors().water
    val colors = MaterialTheme.colorScheme
    val glasses = pluralStringResource(R.plurals.nutrition_glasses, target, target)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = waterColor)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.nutrition_water), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.nutrition_water_liters, Fmt.liters(water * glassMl), Fmt.liters(target * glassMl)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (canChange) stringResource(R.string.nutrition_amount_of, Fmt.number(water), glasses) else glasses,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(maxOf(target, water)) { index ->
                    Glass(
                        filled = index < water,
                        color = waterColor,
                        enabled = canChange,
                        description = stringResource(R.string.nutrition_water_glass_cd, index + 1),
                        // Tapping the last glass drunk empties it.
                        onClick = { onChange(if (index + 1 == water) index else index + 1) },
                    )
                }
            }
            if (canChange) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { onChange(water - 1) }, enabled = water > 0) {
                        Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.nutrition_water_remove))
                    }
                    val press = rememberPressFeedback()
                    Button(
                        onClick = { onChange(water + 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = waterColor, contentColor = colors.onTertiary),
                        interactionSource = press.interactionSource,
                        modifier = press.modifier,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.nutrition_water_add))
                    }
                }
            }
            if (hint.isNotBlank()) {
                Row {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = Fmt.typography(hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

/** A glass that fills, with a little bounce, when it is drunk. */
@Composable
private fun Glass(filled: Boolean, color: Color, enabled: Boolean, description: String, onClick: () -> Unit) {
    val tint by animateColorAsState(if (filled) color else color.copy(alpha = 0.2f), tween(250), label = "glass")
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(filled) }
    LaunchedEffect(filled) {
        if (filled && !previous) {
            scale.snapTo(0.75f)
            scale.animateTo(1f, Motion.bounce())
        }
        previous = filled
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalDrink,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

// endregion

// region Weight guidance

@Composable
private fun GuidanceCard(guidance: WeightGuidance, today: LocalDate, onOpenWeight: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val extended = MuscuTheme.extendedColors
    val goal = guidance.goal
    val rate = guidance.weeklyRate
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        rate == null || rate == 0.0 -> Icons.AutoMirrored.Rounded.TrendingFlat
                        rate > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
                        else -> Icons.AutoMirrored.Rounded.TrendingDown
                    },
                    contentDescription = null,
                    tint = colors.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.nutrition_guidance_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (guidance.advice != WeightAdvice.NEED_DATA) {
                    val onTrack = guidance.advice == WeightAdvice.ON_TRACK
                    Surface(
                        shape = CircleShape,
                        color = if (onTrack) extended.successContainer else colors.tertiaryContainer,
                        contentColor = if (onTrack) extended.onSuccessContainer else colors.onTertiaryContainer,
                    ) {
                        Text(
                            text = stringResource(
                                when (guidance.advice) {
                                    WeightAdvice.BELOW -> R.string.nutrition_guidance_below
                                    WeightAdvice.ABOVE -> R.string.nutrition_guidance_above
                                    else -> R.string.nutrition_guidance_on_track
                                },
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            val start = goal.start
            val target = goal.target
            if (start != null && target != null && guidance.current != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.kg(start), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        FillBar(
                            fraction = (guidance.progress ?: 0.0).toFloat(),
                            delayMillis = 200,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        Text(Fmt.kg(target), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                    Text(
                        text = if ((guidance.progress ?: 0.0) >= 1.0) {
                            stringResource(R.string.nutrition_guidance_reached)
                        } else {
                            stringResource(R.string.nutrition_guidance_current, Fmt.bodyWeightKg(guidance.current))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (rate == null) {
                Text(text = stringResource(R.string.nutrition_guidance_need_data), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onOpenWeight, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Icon(Icons.Rounded.MonitorWeight, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.nutrition_guidance_weigh))
                }
            } else {
                Column {
                    Text(
                        text = stringResource(R.string.nutrition_guidance_rate, Fmt.signedKg(rate, maxDecimals = 2)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.nutrition_guidance_period),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.nutrition_guidance_range,
                        Fmt.signedKg(goal.minWeeklyChange, maxDecimals = 2),
                        Fmt.signedKg(goal.maxWeeklyChange, maxDecimals = 2),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val advice = when (guidance.advice) {
                    WeightAdvice.BELOW -> goal.belowAdvice
                    WeightAdvice.ABOVE -> goal.aboveAdvice
                    else -> ""
                }
                when {
                    advice.isNotBlank() -> Text(
                        text = stringResource(R.string.nutrition_guidance_advice, Fmt.typography(advice)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    guidance.advice == WeightAdvice.ON_TRACK -> Text(
                        text = stringResource(R.string.nutrition_guidance_keep_going),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.success,
                    )
                }
                val eta = guidance.eta
                if (eta != null && target != null) {
                    Text(
                        text = stringResource(
                            R.string.nutrition_guidance_eta,
                            Fmt.kg(target),
                            Fmt.dayMonth(eta, withYear = eta.year != today.year),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// endregion

// region Tips and program

@Composable
private fun NoteCard(note: PlanNote, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(note.id) { mutableStateOf(false) }
    Surface(
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .animateContentSize(tween(300, easing = Motion.EaseOut)),
        ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (note.title.isNotBlank()) Text(text = note.title, style = MaterialTheme.typography.titleSmall)
                if (note.text.isNotBlank()) {
                    Text(
                        text = Fmt.typography(note.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgramCard(
    program: MealPlan,
    since: LocalDate?,
    today: LocalDate,
    onEdit: () -> Unit,
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.nutrition_program),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = program.name, style = MaterialTheme.typography.titleMedium)
            if (since != null) {
                Text(
                    text = stringResource(R.string.nutrition_program_since, Fmt.dayMonth(since, withYear = since.year != today.year)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (program.info.isNotBlank()) {
                Text(
                    text = Fmt.typography(program.info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onEdit, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.nutrition_edit_plan))
                }
                TextButton(onClick = onOpenPrograms) { Text(stringResource(R.string.nutrition_programs)) }
            }
        }
    }
}

@Composable
private fun NoPlan(
    nutrition: Nutrition,
    onCreateProgram: () -> Unit,
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = nutrition.schedule.firstOrNull()
    when {
        nutrition.programs.isEmpty() -> EmptyState(
            icon = Icons.Rounded.Restaurant,
            title = stringResource(R.string.nutrition_empty_title),
            text = stringResource(R.string.nutrition_empty_text),
            modifier = modifier,
            action = {
                Button(onClick = onCreateProgram) { Text(stringResource(R.string.nutrition_create_program)) }
            },
        )
        first == null -> EmptyState(
            icon = Icons.Rounded.EventRepeat,
            title = stringResource(R.string.nutrition_no_plan_title),
            text = stringResource(R.string.nutrition_no_active_text),
            modifier = modifier,
            action = {
                Button(onClick = onOpenPrograms) { Text(stringResource(R.string.nutrition_programs)) }
            },
        )
        else -> EmptyState(
            icon = Icons.Rounded.EventRepeat,
            title = stringResource(R.string.nutrition_no_plan_title),
            text = stringResource(R.string.nutrition_no_plan_before, Fmt.longDay(first.from)),
            modifier = modifier,
        )
    }
}

// endregion
