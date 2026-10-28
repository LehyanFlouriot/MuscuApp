package com.lehyanflouriot.muscuapp.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.sync.FirstSyncChoice
import com.lehyanflouriot.muscuapp.core.sync.RemoteDocument
import com.lehyanflouriot.muscuapp.core.sync.SyncProblem
import com.lehyanflouriot.muscuapp.core.sync.SyncStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the sync card of the settings can do. */
data class SyncActions(
    val onConnect: () -> Unit,
    val onReconnect: () -> Unit,
    val onSyncNow: () -> Unit,
    val onDisconnect: () -> Unit,
    val onChoose: () -> Unit,
)

/** The Google account the data is synced with, and how the sync goes. */
@Composable
fun SyncCard(
    status: SyncStatus,
    connecting: Boolean,
    today: LocalDate,
    actions: SyncActions,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (status.account == null) {
                SyncHeader(
                    icon = Icons.Rounded.CloudSync,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.sync_off_title),
                    text = stringResource(R.string.sync_off_text),
                )
                Button(onClick = actions.onConnect, enabled = !connecting, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (connecting) R.string.sync_connecting else R.string.sync_connect))
                }
            } else {
                val problem = status.problem
                val serious = problem != null && problem != SyncProblem.OFFLINE && problem != SyncProblem.SERVER
                SyncHeader(
                    icon = when {
                        status.running -> Icons.Rounded.Sync
                        problem == SyncProblem.OFFLINE -> Icons.Rounded.CloudOff
                        problem != null || status.choice != null -> Icons.Rounded.SyncProblem
                        else -> Icons.Rounded.CloudDone
                    },
                    tint = if (serious) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    title = status.account.ifEmpty { stringResource(R.string.sync_account_unknown) },
                    text = statusText(status, today),
                    spinning = status.running,
                )
                // The message of Google, in English and technical, only for who asks for it.
                val detail = status.detail
                if (problem == SyncProblem.SETUP && !detail.isNullOrBlank()) {
                    var showDetail by rememberSaveable { mutableStateOf(false) }
                    TextButton(onClick = { showDetail = !showDetail }, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                        Icon(
                            imageVector = if (showDetail) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (showDetail) R.string.sync_detail_hide else R.string.sync_detail_show))
                    }
                    AnimatedVisibility(visible = showDetail) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        status.choice != null -> FilledTonalButton(onClick = actions.onChoose) {
                            Text(stringResource(R.string.sync_choose))
                        }
                        problem == SyncProblem.SIGN_IN -> FilledTonalButton(onClick = actions.onReconnect, enabled = !connecting) {
                            Text(stringResource(if (connecting) R.string.sync_connecting else R.string.sync_reconnect))
                        }
                        else -> FilledTonalButton(onClick = actions.onSyncNow, enabled = !status.running) {
                            Text(stringResource(R.string.sync_now))
                        }
                    }
                    TextButton(onClick = actions.onDisconnect) { Text(stringResource(R.string.sync_disconnect)) }
                }
                if (problem == null && status.choice == null) {
                    Text(
                        text = stringResource(R.string.sync_auto_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncHeader(
    icon: ImageVector,
    tint: Color,
    title: String,
    text: String,
    spinning: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
        ) {
            val turn = if (spinning) {
                val transition = rememberInfiniteTransition(label = "sync")
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -360f,
                    animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
                    label = "turn",
                ).value
            } else {
                0f
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.rotate(turn))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun statusText(status: SyncStatus, today: LocalDate): String = when {
    status.running -> stringResource(R.string.sync_running)
    status.choice != null -> stringResource(R.string.sync_problem_choice)
    status.problem != null -> stringResource(problemTextRes(status.problem))
    status.lastSync != null -> stringResource(R.string.sync_done, syncedWhen(status.lastSync, today))
    else -> stringResource(R.string.sync_never)
}

/** What a sync problem means for the user. */
@StringRes
fun problemTextRes(problem: SyncProblem): Int = when (problem) {
    SyncProblem.OFFLINE -> R.string.sync_problem_offline
    SyncProblem.SIGN_IN -> R.string.sync_problem_sign_in
    SyncProblem.SETUP -> R.string.sync_problem_setup
    SyncProblem.SERVER -> R.string.sync_problem_server
    SyncProblem.UPDATE_APP -> R.string.sync_problem_update
    SyncProblem.UNREADABLE -> R.string.sync_problem_unreadable
}

/** "aujourd'hui à 18:42", "hier à 18:42" or "le 26 oct. à 18:42". */
@Composable
private fun syncedWhen(millis: Long, today: LocalDate): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val hour = Fmt.hourMinute(time.toLocalTime())
    return when (time.toLocalDate()) {
        today -> stringResource(R.string.sync_when_today, hour)
        today.minusDays(1) -> stringResource(R.string.sync_when_yesterday, hour)
        else -> stringResource(R.string.sync_when_on, Fmt.dayMonth(time.toLocalDate(), withYear = time.year != today.year), hour)
    }
}

/** Asked the first time this device syncs, when the account already holds data. */
@Composable
fun FirstSyncDialog(
    document: RemoteDocument,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (FirstSyncChoice) -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(FirstSyncChoice.USE_ACCOUNT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
        title = { Text(stringResource(R.string.sync_choice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.sync_choice_text))
                document.modifiedAt?.let { millis ->
                    val sent = syncedWhen(millis, today)
                    Text(
                        text = document.device?.let { stringResource(R.string.sync_choice_origin_device, it, sent) }
                            ?: stringResource(R.string.sync_choice_origin, sent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.selectableGroup()) {
                    FirstSyncOption(choice == FirstSyncChoice.USE_ACCOUNT, R.string.sync_choice_account, R.string.sync_choice_account_hint) {
                        choice = FirstSyncChoice.USE_ACCOUNT
                    }
                    FirstSyncOption(choice == FirstSyncChoice.MERGE, R.string.sync_choice_merge, R.string.sync_choice_merge_hint) {
                        choice = FirstSyncChoice.MERGE
                    }
                    FirstSyncOption(choice == FirstSyncChoice.KEEP_DEVICE, R.string.sync_choice_device, R.string.sync_choice_device_hint) {
                        choice = FirstSyncChoice.KEEP_DEVICE
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(choice) }) { Text(stringResource(R.string.sync_choice_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun FirstSyncOption(selected: Boolean, @StringRes title: Int, @StringRes hint: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
