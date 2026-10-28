package com.lehyanflouriot.muscuapp.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.FoodView
import com.lehyanflouriot.muscuapp.core.domain.MealView
import com.lehyanflouriot.muscuapp.core.domain.NutritionDayView
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.currentIndex
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.domain.plannedSets
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SettingsAction
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.nutrition.MealCard
import com.lehyanflouriot.muscuapp.ui.nutrition.WaterRow
import com.lehyanflouriot.muscuapp.ui.nutrition.capitalized
import com.lehyanflouriot.muscuapp.ui.week.MissedSession
import com.lehyanflouriot.muscuapp.ui.week.MovedFrom
import com.lehyanflouriot.muscuapp.ui.week.dayTitle
import com.lehyanflouriot.muscuapp.ui.weight.WeighInCard
import com.lehyanflouriot.muscuapp.ui.weight.WeighInState
import java.time.LocalDate
import kotlin.math.roundToInt

/** What the screen of the day lets the user do. */
class TodayActions(
    val onSaveWeight: () -> Unit,
    val onTypeWeight: () -> Unit,
    val onStartSession: () -> Unit,
    /** Does today the session missed on another day. */
    val onDoMissed: (SessionView) -> Unit,
    /** Gives the session done today back to its own day. */
    val onCancelMove: () -> Unit,
    val onToggleFood: (FoodView) -> Unit,
    val onToggleMeal: (MealView) -> Unit,
    val onWaterChange: (Int) -> Unit,
    val onOpenProgram: () -> Unit,
    val onOpenNutrition: () -> Unit,
    val onOpenWeight: () -> Unit,
    val onOpenRest: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * The day at a glance, in the order it goes: the weigh-in of the morning, the session of the
 * day, the next meal and the water, and the rest timer.
 */
@Composable
fun TodayScreen(
    today: LocalDate,
    weights: List<BodyWeightEntry>,
    weighIn: WeighInState,
    /** The session of today, null when no program is followed. */
    session: SessionView?,
    /** A session missed lately, which can be done today. */
    missed: SessionView?,
    /** When the program starts, when it is still to come. */
    programStart: LocalDate?,
    /** The meals of today, null when no meal plan is followed. */
    nutrition: NutritionDayView?,
    actions: TodayActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val reveal = rememberRevealState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.today_title)) },
                actions = { SettingsAction(actions.onOpenSettings) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "date") {
                Text(
                    text = Fmt.longDay(today).capitalized(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .reveal(reveal, "date"),
                )
            }
            item(key = "weigh-in") {
                WeighInCard(
                    form = weighIn,
                    weights = weights,
                    today = today,
                    onType = actions.onTypeWeight,
                    onSave = actions.onSaveWeight,
                    title = stringResource(R.string.today_weigh_in),
                    onOpenCurve = actions.onOpenWeight,
                    modifier = Modifier.reveal(reveal, "weigh-in"),
                )
            }
            item(key = "session") {
                SessionOfTheDay(
                    session = session,
                    missed = missed,
                    programStart = programStart,
                    today = today,
                    actions = actions,
                    modifier = Modifier.reveal(reveal, "session"),
                )
            }
            item(key = "meals") {
                MealsOfTheDay(
                    nutrition = nutrition,
                    today = today,
                    actions = actions,
                    modifier = Modifier.reveal(reveal, "meals"),
                )
            }
            item(key = "rest") {
                RestTimerLink(onOpen = actions.onOpenRest, modifier = Modifier.reveal(reveal, "rest"))
            }
        }
    }
}

@Composable
private fun TodayCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(tween(300)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

/** "Séance du jour" above the title of a card, with what goes on the right of it. */
@Composable
private fun CardHeader(label: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        }
        trailing?.invoke()
    }
}

/** The session of today: what comes next and the button to do it, or the rest of the day. */
@Composable
private fun SessionOfTheDay(
    session: SessionView?,
    missed: SessionView?,
    programStart: LocalDate?,
    today: LocalDate,
    actions: TodayActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.session_today)
    TodayCard(modifier) {
        when {
            session == null -> {
                CardHeader(label = label, title = stringResource(R.string.session_no_program))
                Text(
                    text = programStart?.let { stringResource(R.string.session_no_program_before, Fmt.longDay(it)) }
                        ?: stringResource(R.string.today_no_program),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                TextButton(onClick = actions.onOpenProgram, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Text(stringResource(R.string.today_open_program))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            session.isRest -> {
                CardHeader(label = label, title = dayTitle(session.day))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Fmt.typography(session.day.note.ifBlank { stringResource(R.string.widget_rest_day) }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (missed != null) MissedSession(missed = missed, onDoToday = { actions.onDoMissed(missed) })
            }
            else -> {
                val doneCount = session.doneCount
                CardHeader(
                    label = label,
                    title = dayTitle(session.day),
                    trailing = {
                        Surface(shape = MaterialTheme.shapes.small, color = colors.surfaceContainerHighest) {
                            Text(
                                text = stringResource(R.string.session_progress, doneCount, session.items.size),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    },
                )
                if (session.day.focus.isNotBlank()) {
                    Text(session.day.focus, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                session.movedFrom?.let { from ->
                    MovedFrom(from = from, date = today, today = today, canCancel = session.doneSets().isEmpty(), onCancel = actions.onCancelMove)
                }
                val current = session.currentIndex()?.let { session.items[it] }
                Text(
                    text = when {
                        current == null -> stringResource(R.string.session_complete)
                        session.doneSets().isEmpty() -> stringResource(
                            R.string.widget_first,
                            current.exercise.name,
                            Fmt.target(current.line.target, current.exercise.measure),
                        )
                        else -> stringResource(R.string.guided_then, current.exercise.name, current.doneSets().size + 1, current.plannedSets)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                val buttonModifier = Modifier.fillMaxWidth()
                if (session.complete) {
                    FilledTonalButton(
                        onClick = actions.onStartSession,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = buttonModifier,
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.guided_review))
                    }
                } else {
                    Button(
                        onClick = actions.onStartSession,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = buttonModifier,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (session.doneSets().isEmpty()) R.string.guided_start else R.string.guided_resume))
                    }
                }
            }
        }
    }
}

/** How the meals of today go, the water, and the next meal to check. */
@Composable
private fun MealsOfTheDay(nutrition: NutritionDayView?, today: LocalDate, actions: TodayActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    TodayCard(modifier) {
        CardHeader(
            label = stringResource(R.string.today_meals),
            title = nutrition?.dayPlan?.let { stringResource(R.string.today_meals_title) } ?: stringResource(R.string.today_no_plan_title),
            trailing = {
                TextButton(onClick = actions.onOpenNutrition) { Text(stringResource(R.string.today_open_day)) }
            },
        )
        if (nutrition == null) {
            Text(
                text = stringResource(R.string.today_no_plan),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            return@TodayCard
        }
        val totals = listOfNotNull(
            nutrition.kcalTarget?.let {
                stringResource(R.string.nutrition_amount_of, Fmt.number(nutrition.kcalEaten.roundToInt()), Fmt.kcal(it))
            },
            nutrition.proteinTarget?.let {
                stringResource(R.string.today_protein, Fmt.number(nutrition.proteinEaten.roundToInt()), Fmt.grams(it))
            },
        )
        if (totals.isNotEmpty()) {
            Text(text = totals.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (nutrition.waterTarget > 0) {
            WaterRow(
                water = nutrition.water,
                target = nutrition.waterTarget,
                glassMl = nutrition.plan.glassMl,
                canChange = true,
                onChange = actions.onWaterChange,
            )
        }
        val next = nutrition.nextMeal
        if (next == null) {
            Text(
                text = stringResource(R.string.today_all_eaten),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
            )
        } else {
            Text(stringResource(R.string.today_next_meal), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            MealCard(
                meal = next,
                date = today,
                canCheck = true,
                highlighted = false,
                onToggleFood = actions.onToggleFood,
                onToggleMeal = { actions.onToggleMeal(next) },
                onSwap = null,
            )
        }
    }
}

/** The way to the rest timer, for a rest outside a session. */
@Composable
private fun RestTimerLink(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_rest_timer), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.today_rest_timer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
