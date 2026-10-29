package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.SessionItem
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.nutrition.ProgressRing
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate
import java.time.format.TextStyle

/** What the session card can do. */
class SessionActions(
    val onSelectDay: (LocalDate) -> Unit,
    val onLog: (SessionItem) -> Unit,
    val onEditEntry: (WorkoutEntry) -> Unit,
    val onOpenProgram: () -> Unit,
    /** Starts the session of a day, guided set by set. */
    val onStart: (LocalDate) -> Unit,
    /** Does today the session missed on another day. */
    val onDoMissed: (SessionView) -> Unit = {},
    /** Gives the session done on a day back to the day it was planned on. */
    val onCancelMove: (LocalDate) -> Unit = {},
)

/** The session of the program on the chosen day of the week: what to do, and what is done. */
@Composable
fun SessionCard(
    week: List<SessionView>,
    selected: LocalDate,
    today: LocalDate,
    actions: SessionActions,
    modifier: Modifier = Modifier,
    /** A session missed lately, which can be done today when today is free. */
    missed: SessionView? = null,
    /** How long the session of a day took, when it was timed. */
    sessionDuration: (LocalDate) -> Long? = { null },
) {
    val session = week.firstOrNull { it.date == selected } ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .animateContentSize(tween(350)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionHeader(session = session, today = today, onOpenProgram = actions.onOpenProgram)
            SessionDays(week = week, selected = selected, today = today, onSelect = actions.onSelectDay)
            // A session of today or of a day gone by can be done set by set, and looked over once done.
            if (!session.isRest && session.date <= today) {
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                if (session.complete) {
                    FilledTonalButton(
                        onClick = { actions.onStart(session.date) },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = buttonModifier,
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.guided_review))
                    }
                } else {
                    Button(
                        onClick = { actions.onStart(session.date) },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = buttonModifier,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (session.doneSets().isEmpty()) R.string.guided_start else R.string.guided_resume))
                    }
                }
            }
            val movedTo = session.movedTo
            if (movedTo != null) {
                MovedAway(to = movedTo, today = today, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            } else if (session.isRest) {
                RestDay(session, week)
                if (session.date == today && missed != null) {
                    MissedSession(
                        missed = missed,
                        onDoToday = { actions.onDoMissed(missed) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                session.movedFrom?.let { from ->
                    MovedFrom(
                        from = from,
                        date = session.date,
                        today = today,
                        canCancel = session.doneSets().isEmpty(),
                        onCancel = { actions.onCancelMove(session.date) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    session.items.forEach { item ->
                        SessionRow(
                            item = item,
                            onLog = { actions.onLog(item) },
                            onEdit = actions.onEditEntry,
                        )
                    }
                }
                if (session.doneCount == session.items.size) {
                    val duration = sessionDuration(session.date)
                    Text(
                        text = duration?.let { stringResource(R.string.session_complete_in, Fmt.sessionLength(it)) }
                            ?: stringResource(R.string.session_complete),
                        style = MaterialTheme.typography.titleSmall,
                        color = MuscuTheme.extendedColors.success,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHeader(session: SessionView, today: LocalDate, onOpenProgram: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (session.date == today) {
                    stringResource(R.string.session_today)
                } else {
                    stringResource(R.string.session_of, relativeDay(session.date, today))
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = sessionTitle(session),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (session.day.focus.isNotBlank()) {
                Text(
                    text = session.day.focus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!session.isRest) {
            val description = stringResource(R.string.session_progress_cd, session.doneCount, session.items.size)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Text(
                    text = stringResource(R.string.session_progress, session.doneCount, session.items.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        TextButton(onClick = onOpenProgram) { Text(stringResource(R.string.session_program)) }
    }
}

/** The days of the week, with how far each session went. */
@Composable
private fun SessionDays(week: List<SessionView>, selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    Row(Modifier.padding(horizontal = 8.dp)) {
        week.forEach { session ->
            val date = session.date
            val isSelected = date == selected
            val isToday = date == today
            val dayName = Fmt.longDay(date)
            val description = when {
                session.program == null -> stringResource(R.string.session_no_program_cd, dayName)
                session.isRest -> stringResource(R.string.session_rest_day_cd, dayName)
                else -> stringResource(R.string.session_day_cd, dayName, sessionTitle(session), session.doneCount, session.items.size)
            }
            val disc = animateColorAsState(if (isSelected) colors.primary else Color.Transparent, tween(250), label = "session-day")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(selected = isSelected, onClick = { onSelect(date) }, role = Role.Tab)
                    .semantics { contentDescription = description }
                    .padding(vertical = 4.dp),
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
                        .size(38.dp)
                        .clearAndSetSemantics { },
                ) {
                    if (!session.isRest) {
                        val fraction = session.doneCount.toFloat() / session.items.size
                        ProgressRing(
                            fraction = fraction,
                            color = if (fraction >= 1f) success else colors.primary,
                            trackColor = colors.outlineVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Canvas(Modifier.size(28.dp)) { drawCircle(disc.value) }
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            isSelected -> colors.onPrimary
                            isToday -> colors.primary
                            session.isRest -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                            else -> colors.onSurface
                        },
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else null,
                    )
                }
            }
        }
    }
}

/** "Pectoraux", "Repos", or no program yet that day. */
@Composable
private fun sessionTitle(session: SessionView): String =
    if (session.program == null) stringResource(R.string.session_no_program) else dayTitle(session.day)

@Composable
private fun RestDay(session: SessionView, week: List<SessionView>) {
    // Without a program that day, the program starts later in the week.
    val start = week.firstOrNull { it.program != null }?.date
    val text = when {
        session.program == null && start != null -> stringResource(R.string.session_no_program_before, Fmt.longDay(start))
        session.day.note.isNotBlank() -> session.day.note
        else -> stringResource(R.string.session_rest_text)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = if (session.program == null) Icons.Rounded.EventRepeat else Icons.Rounded.SelfImprovement,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = Fmt.typography(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** An exercise of the session: what is asked and the goal, or what was done. */
@Composable
private fun SessionRow(item: SessionItem, onLog: () -> Unit, onEdit: (WorkoutEntry) -> Unit) {
    val exercise = item.exercise
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    // The exercise of emphasis is highlighted, as in the program.
    val highlight = if (item.line.emphasis) colors.primary else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (item.line.emphasis) colors.primary.copy(alpha = 0.07f) else Color.Transparent)
            .drawBehind { drawRect(highlight, size = Size(3.dp.toPx(), size.height)) }
            .clickable(
                onClickLabel = if (item.done) {
                    stringResource(R.string.week_edit_entry)
                } else {
                    stringResource(R.string.week_log_cd, exercise.name)
                },
                onClick = { if (item.done) onEdit(item.entries.last()) else onLog() },
            )
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.line.emphasis) {
                    Spacer(Modifier.width(6.dp))
                    EmphasisBadge()
                }
            }
            Text(
                text = Fmt.target(item.line.target, exercise.measure),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            if (item.line.note.isNotBlank()) {
                Text(
                    text = Fmt.typography(item.line.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (item.done) {
                Text(
                    text = stringResource(R.string.session_done, item.entries.joinToString(" + ") { exercise.describe(it.sets) }),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = success,
                )
            } else {
                Text(
                    text = stringResource(R.string.session_goal, exercise.describe(item.goal)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (item.topReached) {
                    Text(
                        text = stringResource(R.string.session_top_reached),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.tertiary,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            // Not the one of the row: the button and the check take turns in the same place.
            androidx.compose.animation.AnimatedVisibility(
                visible = item.done,
                enter = scaleIn(tween(400, easing = EaseOutBack)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.week_done_cd),
                    tint = success,
                    modifier = Modifier.size(30.dp),
                )
            }
            androidx.compose.animation.AnimatedVisibility(visible = !item.done, enter = fadeIn(), exit = fadeOut()) {
                val press = rememberPressFeedback(pressedScale = 0.88f)
                FilledTonalIconButton(
                    onClick = onLog,
                    interactionSource = press.interactionSource,
                    modifier = press.modifier,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.week_log_cd, exercise.name))
                }
            }
        }
    }
}
