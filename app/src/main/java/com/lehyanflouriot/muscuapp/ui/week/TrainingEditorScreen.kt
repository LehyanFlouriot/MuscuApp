package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.ui.common.EditorRow
import com.lehyanflouriot.muscuapp.ui.common.EditorSection
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.ProgramHeader
import com.lehyanflouriot.muscuapp.ui.common.SettingsAction
import com.lehyanflouriot.muscuapp.ui.common.fullName
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import java.time.DayOfWeek
import java.time.format.TextStyle

/** What the user opens from the training program editor. */
sealed interface TrainingEdit {
    data object Program : TrainingEdit

    data class Day(val day: DayOfWeek) : TrainingEdit

    data class CopyDay(val day: DayOfWeek) : TrainingEdit

    /** Chooses an exercise to add to the session of [day], or to do instead of [replacing]. */
    data class Pick(val day: DayOfWeek, val replacing: SessionExercise? = null) : TrainingEdit

    /** Creates the exercise [name] first, then goes on as if it had been picked. */
    data class NewExercise(val day: DayOfWeek, val name: String, val replacing: SessionExercise?) : TrainingEdit

    /** What the session of [day] asks for [line], or for [exerciseId] when it is added. */
    data class Line(val day: DayOfWeek, val line: SessionExercise?, val exerciseId: String) : TrainingEdit

    data class Note(val note: PlanNote?) : TrainingEdit
}

/** "Pectoraux", or what the day is when it has no name. */
@Composable
fun dayTitle(day: TrainingDay): String =
    day.title.ifBlank { stringResource(if (day.isRest) R.string.session_rest_title else R.string.session_untitled) }

/** The badge of the exercise of emphasis of a session. */
@Composable
fun EmphasisBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.session_emphasis),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** The place of the card of [day] in the list, after the one of the program. */
private fun dayItemIndex(day: DayOfWeek): Int = if (day == DayOfWeek.MONDAY) 0 else day.ordinal + 1

/** The Programme tab when there is no program yet. */
@Composable
fun NoTrainingProgram(onCreate: () -> Unit, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_screen_title)) },
                actions = { SettingsAction(onOpenSettings) },
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.EventNote,
                title = stringResource(R.string.training_none_title),
                text = stringResource(R.string.training_none_text),
                action = {
                    Button(onClick = onCreate, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.training_none_create))
                    }
                },
            )
        }
    }
}

/**
 * The whole week of a training program, as on paper, where everything can be changed: its
 * sessions, what each one asks for its exercises, and the advice to follow it.
 */
@Composable
fun TrainingEditorScreen(
    program: TrainingProgram,
    exercises: Map<String, Exercise>,
    status: String?,
    keepsHistory: Boolean,
    today: DayOfWeek,
    scrollTo: DayOfWeek?,
    onScrolled: () -> Unit,
    onOpen: (TrainingEdit) -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val reveal = rememberRevealState()
    // Today's session shows first; from Monday on, the program shows above it.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = dayItemIndex(scrollTo ?: today))
    // The day asked from the week comes into view.
    LaunchedEffect(scrollTo) {
        if (scrollTo != null) {
            listState.animateScrollToItem(dayItemIndex(scrollTo))
            onScrolled()
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_screen_title)) },
                actions = {
                    TextButton(onClick = onOpenPrograms) { Text(stringResource(R.string.programs_title)) }
                    SettingsAction(onOpenSettings)
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "program") {
                ProgramHeader(
                    name = program.name,
                    info = program.info,
                    keepsHistory = keepsHistory,
                    onEdit = { onOpen(TrainingEdit.Program) },
                    status = status,
                    modifier = Modifier.reveal(reveal, "program"),
                )
            }
            items(DayOfWeek.entries, key = { it.name }) { dayOfWeek ->
                TrainingDayCard(
                    day = program.day(dayOfWeek),
                    exercises = exercises,
                    isToday = dayOfWeek == today,
                    onEditDay = { onOpen(TrainingEdit.Day(dayOfWeek)) },
                    onEditLine = { line -> onOpen(TrainingEdit.Line(dayOfWeek, line, line.exerciseId)) },
                    onAddExercise = { onOpen(TrainingEdit.Pick(dayOfWeek)) },
                    onCopy = { onOpen(TrainingEdit.CopyDay(dayOfWeek)) },
                    modifier = Modifier.reveal(reveal, dayOfWeek.name),
                )
            }
            item(key = "tips") {
                EditorSection(
                    title = stringResource(R.string.program_tips),
                    addLabel = stringResource(R.string.plan_add_note),
                    onAdd = { onOpen(TrainingEdit.Note(null)) },
                    modifier = Modifier.reveal(reveal, "tips"),
                ) {
                    program.notes.forEach { note ->
                        EditorRow(
                            icon = Icons.Rounded.Lightbulb,
                            title = note.title.ifBlank { note.text },
                            subtitle = if (note.title.isBlank()) "" else note.text,
                            onClick = { onOpen(TrainingEdit.Note(note)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingDayCard(
    day: TrainingDay,
    exercises: Map<String, Exercise>,
    isToday: Boolean,
    onEditDay: () -> Unit,
    onEditLine: (SessionExercise) -> Unit,
    onAddExercise: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lines = day.exercises.filter { it.exerciseId in exercises }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (lines.isEmpty()) colors.surfaceContainerLow else colors.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.animateContentSize(tween(300, easing = Motion.EaseOut))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.training_day_edit), onClick = onEditDay)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = day.day.fullName().uppercase(Fmt.locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) colors.primary else colors.onSurfaceVariant,
                    )
                    Text(dayTitle(day), style = MaterialTheme.typography.titleLarge)
                    if (day.focus.isNotBlank()) {
                        Text(
                            text = day.focus.uppercase(Fmt.locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            if (lines.isEmpty()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Fmt.typography(day.note.ifBlank { stringResource(R.string.training_day_rest_hint) }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                if (day.note.isNotBlank()) {
                    Text(
                        text = Fmt.typography(day.note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    )
                }
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    lines.forEach { line ->
                        LineRow(line = line, exercise = exercises.getValue(line.exerciseId), onClick = { onEditLine(line) })
                    }
                }
            }
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onAddExercise, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.training_add_exercise))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.training_copy_day_cd, day.day.getDisplayName(TextStyle.FULL, Fmt.locale)),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** An exercise of a session: what is asked, with how long to rest, and how to do it. */
@Composable
private fun LineRow(line: SessionExercise, exercise: Exercise, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // The exercise of emphasis is highlighted, as on paper.
    val bar = if (line.emphasis) colors.primary else Color.Transparent
    val rest = line.restSeconds ?: exercise.restSeconds
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (line.emphasis) colors.primary.copy(alpha = 0.07f) else Color.Transparent)
            .drawBehind { drawRect(bar, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClickLabel = stringResource(R.string.action_edit), onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (line.emphasis) {
                    Spacer(Modifier.width(6.dp))
                    EmphasisBadge()
                }
            }
            Text(
                text = listOfNotNull(
                    Fmt.target(line.target, exercise.measure),
                    rest?.let { stringResource(R.string.training_rest, Fmt.duration(it)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            if (line.note.isNotBlank()) {
                Text(
                    text = Fmt.typography(line.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.Edit,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}
