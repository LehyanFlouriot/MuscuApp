package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.reveal
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun ProgramRoute(data: AppData, onBack: () -> Unit) {
    val program = data.training.program
    if (program == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    ProgramScreen(
        program = program,
        exercises = data.exercises.associateBy { it.id },
        today = rememberToday().dayOfWeek,
        onBack = onBack,
    )
}

/** The whole week of the program, as on paper, with its advice. */
@Composable
fun ProgramScreen(
    program: TrainingProgram,
    exercises: Map<String, Exercise>,
    today: DayOfWeek,
    onBack: () -> Unit,
) {
    val reveal = rememberRevealState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Column(Modifier.reveal(reveal, "intro"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(program.name, style = MaterialTheme.typography.headlineSmall)
                    if (program.info.isNotBlank()) {
                        Text(
                            text = Fmt.typography(program.info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(program.days, key = { it.day.name }) { day ->
                ProgramDayCard(
                    day = day,
                    exercises = exercises,
                    isToday = day.day == today,
                    modifier = Modifier.reveal(reveal, day.day.name),
                )
            }
            if (program.notes.isNotEmpty()) {
                item(key = "tips") {
                    SectionHeader(stringResource(R.string.program_tips), Modifier.reveal(reveal, "tips"))
                }
                items(program.notes, key = { it.id }) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .reveal(reveal, note.id),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(note.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = Fmt.typography(note.text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramDayCard(
    day: TrainingDay,
    exercises: Map<String, Exercise>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (day.isRest) colors.surfaceContainerLow else colors.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = day.day.getDisplayName(TextStyle.FULL, Fmt.locale).uppercase(Fmt.locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) colors.primary else colors.onSurfaceVariant,
                )
                Text(day.title, style = MaterialTheme.typography.titleLarge)
                if (day.focus.isNotBlank()) {
                    Text(
                        text = day.focus.uppercase(Fmt.locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
            }
            if (day.isRest) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Fmt.typography(day.note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    day.exercises.forEach { line ->
                        exercises[line.exerciseId]?.let { ProgramLine(line, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramLine(line: SessionExercise, exercise: Exercise) {
    val colors = MaterialTheme.colorScheme
    // The exercise of emphasis is highlighted, as on paper.
    val bar = if (line.emphasis) colors.primary else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (line.emphasis) colors.primary.copy(alpha = 0.07f) else Color.Transparent)
            .drawBehind { drawRect(bar, size = Size(3.dp.toPx(), size.height)) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(exercise.name, style = MaterialTheme.typography.titleSmall)
        Text(
            text = Fmt.target(line.target, exercise.measure),
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
}
