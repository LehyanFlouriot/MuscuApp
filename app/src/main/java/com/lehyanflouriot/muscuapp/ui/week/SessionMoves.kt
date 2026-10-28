package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.ui.common.lowerName
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import java.time.LocalDate

// Sessions done on another day than planned.

/** "Séance manquée: Quadriceps était prévue mercredi", with the button to do it today. */
@Composable
fun MissedSession(missed: SessionView, onDoToday: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EventRepeat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.missed_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = stringResource(R.string.missed_text, dayTitle(missed.day), missed.date.dayOfWeek.lowerName()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDoToday, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.missed_do_today))
            }
        }
    }
}

/** "Séance de mercredi, faite aujourd'hui", with the button to give it back to its day while nothing is noted. */
@Composable
fun MovedFrom(from: LocalDate, date: LocalDate, today: LocalDate, canCancel: Boolean, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.EventRepeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.moved_from, from.dayOfWeek.lowerName(), relativeDay(date, today).lowercase(Fmt.locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (canCancel) TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
    }
}

/** "Faite aujourd'hui à la place.", on the day a session was planned on. */
@Composable
fun MovedAway(to: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.EventRepeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.moved_to, relativeDay(to, today).lowercase(Fmt.locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
