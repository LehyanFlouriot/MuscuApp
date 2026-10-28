package com.lehyanflouriot.muscuapp.ui.weight

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.BODY_WEIGHT_RANGE
import com.lehyanflouriot.muscuapp.core.domain.WeightPeriod
import com.lehyanflouriot.muscuapp.core.domain.WeightPoint
import com.lehyanflouriot.muscuapp.core.domain.WeightSummary
import com.lehyanflouriot.muscuapp.core.domain.previousWeight
import com.lehyanflouriot.muscuapp.core.domain.roundBodyWeight
import com.lehyanflouriot.muscuapp.core.domain.weightSummary
import com.lehyanflouriot.muscuapp.core.domain.weightTrend
import com.lehyanflouriot.muscuapp.core.domain.weightsBetween
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.ui.common.DateChips
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.NumberInputDialog
import com.lehyanflouriot.muscuapp.ui.common.PastDatePickerDialog
import com.lehyanflouriot.muscuapp.ui.common.RollingText
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.StatTile
import com.lehyanflouriot.muscuapp.ui.common.StepButton
import com.lehyanflouriot.muscuapp.ui.common.countUp
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate

/** Increment of the +/- buttons, in kg. */
private const val WEIGHT_STEP = 0.1

/** The day and the weight being noted. */
@Stable
class WeighInState(initialWeight: Double) {
    /** Day chosen with the chips, null for today: a screen left open overnight then follows the new day. */
    var pickedDate by mutableStateOf<LocalDate?>(null)
    var weight by mutableDoubleStateOf(initialWeight)

    fun date(today: LocalDate): LocalDate = pickedDate ?: today
}

@get:StringRes
private val WeightPeriod.labelRes: Int
    get() = when (this) {
        WeightPeriod.MONTH -> R.string.weight_period_month
        WeightPeriod.THREE_MONTHS -> R.string.weight_period_three_months
        WeightPeriod.YEAR -> R.string.weight_period_year
        WeightPeriod.ALL -> R.string.weight_period_all
    }

/**
 * The user's own weight, in the Progrès tab: the weigh-in at the top, then the curve and the
 * history. [weights] are sorted by date.
 */
@Composable
fun WeightContent(
    padding: PaddingValues,
    weights: List<BodyWeightEntry>,
    today: LocalDate,
    form: WeighInState,
    period: WeightPeriod,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onPeriodChange: (WeightPeriod) -> Unit,
    onUpdate: (BodyWeightEntry, Double) -> Unit,
    onDelete: (BodyWeightEntry) -> Unit,
) {
    val reveal = rememberRevealState()
    var typing by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyWeightEntry?>(null) }
    val summary = remember(weights) { weightSummary(weights) }
    val history = remember(weights) { weights.sortedByDescending { it.date } }

    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
    ) {
        item(key = "weigh-in") {
            WeighInCard(
                form = form,
                weights = weights,
                today = today,
                onType = { typing = true },
                onSave = onSave,
                dates = { date ->
                    DateChips(date = date, today = today, onSelect = onDateChange, onPickDate = { pickingDate = true })
                },
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .reveal(reveal, "weigh-in"),
            )
        }
        when {
            summary == null -> item(key = "intro") {
                EmptyState(
                    icon = Icons.Rounded.MonitorWeight,
                    title = stringResource(R.string.weight_intro_title),
                    text = stringResource(R.string.weight_intro_text),
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "intro"),
                )
            }
            weights.size == 1 -> item(key = "one") {
                OneWeightCard(
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .padding(bottom = 12.dp)
                        .reveal(reveal, "one"),
                )
            }
            else -> {
                item(key = "summary") {
                    SummaryCard(
                        summary = summary,
                        today = today,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .padding(bottom = 12.dp)
                            .reveal(reveal, "summary"),
                    )
                }
                item(key = "chart") {
                    ChartCard(
                        weights = weights,
                        today = today,
                        period = period,
                        onPeriodChange = onPeriodChange,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .padding(bottom = 12.dp)
                            .reveal(reveal, "chart"),
                    )
                }
            }
        }
        if (history.isNotEmpty()) {
            item(key = "history-title") {
                SectionHeader(
                    text = stringResource(R.string.weight_history),
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "history-title"),
                )
            }
            itemsIndexed(history, key = { _, entry -> entry.date.toEpochDay() }) { index, entry ->
                HistoryRow(
                    entry = entry,
                    previous = history.getOrNull(index + 1),
                    today = today,
                    onClick = { editing = entry },
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, entry.date),
                )
            }
        }
    }

    if (typing) {
        WeightInputDialog(
            initialWeight = form.weight,
            onDismiss = { typing = false },
            onConfirm = {
                form.weight = it
                typing = false
            },
        )
    }
    if (pickingDate) {
        PastDatePickerDialog(
            initialDate = form.date(today),
            today = today,
            onDismiss = { pickingDate = false },
            onConfirm = {
                onDateChange(it)
                pickingDate = false
            },
        )
    }
    editing?.let { entry ->
        WeightEditDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(entry, it)
                editing = null
            },
            onDelete = {
                onDelete(entry)
                editing = null
            },
        )
    }
}

/** Big "- 78,4 kg +" control; tapping the number lets the user type it. */
@Composable
private fun WeightStepper(
    weight: Double,
    onChange: (Double) -> Unit,
    onType: () -> Unit,
    numberStyle: TextStyle = MaterialTheme.typography.displayMedium,
) {
    val label = stringResource(R.string.field_weight)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(R.string.cd_decrease, label),
            enabled = weight - WEIGHT_STEP >= BODY_WEIGHT_RANGE.start,
            onStep = { onChange(roundBodyWeight((weight - WEIGHT_STEP).coerceIn(BODY_WEIGHT_RANGE))) },
            size = 56.dp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = stringResource(R.string.cd_type_value), onClick = onType)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText(
                    text = Fmt.bodyWeight(weight),
                    style = numberStyle.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " " + stringResource(R.string.unit_kg),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        StepButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.cd_increase, label),
            enabled = weight + WEIGHT_STEP <= BODY_WEIGHT_RANGE.endInclusive,
            onStep = { onChange(roundBodyWeight((weight + WEIGHT_STEP).coerceIn(BODY_WEIGHT_RANGE))) },
            size = 56.dp,
        )
    }
}

/**
 * The weigh-in of a day. Once its weight is noted, the card shows it with how it changed, and
 * "Modifier" brings the buttons back to correct it. [dates] lets the user choose the day.
 */
@Composable
fun WeighInCard(
    form: WeighInState,
    weights: List<BodyWeightEntry>,
    today: LocalDate,
    onType: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.weight_weigh_in),
    dates: (@Composable (LocalDate) -> Unit)? = null,
    onOpenCurve: (() -> Unit)? = null,
) {
    val date = form.date(today)
    val saved = weights.firstOrNull { it.date == date }
    val previous = previousWeight(weights, date)
    var editing by remember(date) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(tween(300, easing = Motion.EaseOut)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onOpenCurve != null && weights.size > 1) {
                    TextButton(onClick = onOpenCurve) { Text(stringResource(R.string.weight_open_curve)) }
                }
            }
            dates?.invoke(date)
            if (saved != null && !editing) {
                SavedWeighIn(saved = saved, previous = previous, today = today, onEdit = { editing = true })
            } else {
                WeightStepper(weight = form.weight, onChange = { form.weight = it }, onType = onType)
                val hint = when {
                    previous != null -> stringResource(
                        R.string.weight_previous,
                        Fmt.bodyWeightKg(previous.weight),
                        relativeDay(previous.date, today).lowercase(Fmt.locale),
                    )
                    weights.isEmpty() -> stringResource(R.string.weight_type_hint)
                    else -> null
                }
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (saved != null) {
                        TextButton(
                            onClick = {
                                form.weight = saved.weight
                                editing = false
                            },
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                    val press = rememberPressFeedback()
                    Button(
                        onClick = {
                            onSave()
                            editing = false
                        },
                        interactionSource = press.interactionSource,
                        modifier = press.modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (saved != null) R.string.log_update else R.string.action_save),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

/** "77,3 kg · Pesée notée ✓ · +0,4 kg depuis hier", with the button to correct it. */
@Composable
private fun SavedWeighIn(saved: BodyWeightEntry, previous: BodyWeightEntry?, today: LocalDate, onEdit: () -> Unit) {
    val success = MuscuTheme.extendedColors.success
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Fmt.bodyWeight(saved.weight),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " " + stringResource(R.string.unit_kg),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = success, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.weight_noted),
                    style = MaterialTheme.typography.titleSmall,
                    color = success,
                )
            }
            if (previous != null) {
                Text(
                    text = stringResource(
                        R.string.weight_change_since,
                        Fmt.signedKg(saved.weight - previous.weight),
                        relativeDay(previous.date, today).lowercase(Fmt.locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_edit))
        }
    }
}

@Composable
private fun OneWeightCard(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.weight_one_entry), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummaryCard(summary: WeightSummary, today: LocalDate, modifier: Modifier = Modifier) {
    val rate = summary.weeklyRate
    val firstDay = Fmt.dayMonth(summary.first.date, withYear = summary.first.date.year != today.year)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                StatTile(
                    value = Fmt.bodyWeightKg(summary.latest.weight),
                    label = stringResource(R.string.weight_current),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = Fmt.signedKg(countUp(summary.change, delayMillis = 80)),
                    label = stringResource(R.string.weight_since_start),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = rate?.let { Fmt.signedKg(countUp(it, delayMillis = 160), maxDecimals = 2) } ?: "–",
                    label = stringResource(R.string.weight_per_week),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(
                    if (rate != null) R.string.weight_summary_hint else R.string.weight_summary_hint_pending,
                    firstDay,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ChartCard(
    weights: List<BodyWeightEntry>,
    today: LocalDate,
    period: WeightPeriod,
    onPeriodChange: (WeightPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val end = maxOf(today, weights.maxOf { it.date })
    val start = period.start(today, weights.minOf { it.date })
    val shown = remember(weights, start, end) { weightsBetween(weights, start, end) }
    // The trend uses the weights before the period too, so that it is right from the first day.
    val trend = remember(weights, start, end) { weightTrend(weights).filter { it.date in start..end } }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(stringResource(R.string.weight_chart_title), style = MaterialTheme.typography.titleMedium)
                if (shown.size > 1) {
                    val change = roundBodyWeight(shown.last().weight - shown.first().weight)
                    Text(
                        text = if (change == 0.0) {
                            stringResource(R.string.weight_period_stable)
                        } else {
                            stringResource(R.string.weight_period_change, Fmt.signedKg(change))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val periods = WeightPeriod.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = item == period,
                        onClick = { onPeriodChange(item) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                        icon = {},
                    ) {
                        Text(stringResource(item.labelRes), maxLines = 1)
                    }
                }
            }
            if (shown.isEmpty()) {
                Text(
                    text = stringResource(R.string.weight_period_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                WeightChart(
                    weights = shown.map { WeightPoint(it.date, it.weight) },
                    trend = trend,
                    start = start,
                    end = end,
                    description = stringResource(
                        R.string.weight_chart_description,
                        Fmt.bodyWeightKg(shown.first().weight),
                        Fmt.bodyWeightKg(shown.last().weight),
                    ),
                )
                ChartLegend()
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    val color = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(stringResource(R.string.weight_legend_trend)) {
            Box(
                Modifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(color, CircleShape),
            )
        }
        LegendItem(stringResource(R.string.weight_legend_weights)) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(color.copy(alpha = 0.4f), CircleShape),
            )
        }
    }
}

@Composable
private fun LegendItem(text: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        mark()
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryRow(
    entry: BodyWeightEntry,
    previous: BodyWeightEntry?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = relativeDay(entry.date, today),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Fmt.bodyWeightKg(entry.weight),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = previous?.let { Fmt.signedKg(entry.weight - it.weight) }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun WeightInputDialog(initialWeight: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    NumberInputDialog(
        title = stringResource(R.string.field_weight),
        initialValue = Fmt.bodyWeight(initialWeight),
        allowDecimal = true,
        suffix = stringResource(R.string.unit_kg),
        valueRange = BODY_WEIGHT_RANGE,
        rangeError = stringResource(R.string.weight_range_error),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(roundBodyWeight(it)) },
    )
}

/** Corrects or deletes a weight of the history. */
@Composable
private fun WeightEditDialog(
    entry: BodyWeightEntry,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    var weight by remember(entry) { mutableDoubleStateOf(entry.weight) }
    var typing by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_edit_title, Fmt.day(entry.date))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeightStepper(
                    weight = weight,
                    onChange = { weight = it },
                    onType = { typing = true },
                    numberStyle = MaterialTheme.typography.displaySmall,
                )
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.weight_delete))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(weight) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
    if (typing) {
        WeightInputDialog(
            initialWeight = weight,
            onDismiss = { typing = false },
            onConfirm = {
                weight = it
                typing = false
            },
        )
    }
}
