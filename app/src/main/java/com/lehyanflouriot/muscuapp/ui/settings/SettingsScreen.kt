package com.lehyanflouriot.muscuapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.Reminder
import com.lehyanflouriot.muscuapp.core.domain.ReminderKind
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Settings
import com.lehyanflouriot.muscuapp.core.model.ThemeMode
import com.lehyanflouriot.muscuapp.core.sync.SyncStatus
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import java.time.LocalDate
import java.time.LocalTime

private val WEIGHT_STEPS = listOf(0.5, 1.0, 1.25, 2.0, 2.5, 5.0)

@Composable
fun SettingsScreen(
    settings: Settings,
    exerciseCount: Int,
    entryCount: Int,
    versionName: String,
    dynamicColorAvailable: Boolean,
    sync: SyncStatus,
    syncConnecting: Boolean,
    today: LocalDate,
    syncActions: SyncActions,
    reminders: Map<ReminderKind, Reminder>,
    onReminderChange: (ReminderKind, Reminder) -> Unit,
    onBack: () -> Unit,
    onWeightStepChange: (Double) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAlertSettings: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val reveal = rememberRevealState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.reveal(reveal, "sync"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_sync))
                SyncCard(status = sync, connecting = syncConnecting, today = today, actions = syncActions)
            }

            Column(Modifier.reveal(reveal, "reminders"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_reminders))
                ReminderKind.entries.forEach { kind ->
                    val reminder = reminders[kind] ?: Reminder(enabled = false, time = kind.defaultTime)
                    val time = Fmt.hourMinute(reminder.time)
                    ReminderRow(
                        icon = if (kind == ReminderKind.SESSION) Icons.Rounded.FitnessCenter else Icons.Rounded.MonitorWeight,
                        title = stringResource(if (kind == ReminderKind.SESSION) R.string.reminder_session else R.string.reminder_weigh_in),
                        subtitle = stringResource(
                            if (kind == ReminderKind.SESSION) R.string.reminder_session_hint else R.string.reminder_weigh_in_hint,
                            time,
                        ),
                        reminder = reminder,
                        onChange = { onReminderChange(kind, it) },
                    )
                }
            }

            Column(Modifier.reveal(reveal, "logging"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_logging))
                Text(stringResource(R.string.settings_weight_step), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_weight_step_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WEIGHT_STEPS.forEach { step ->
                        FilterChip(
                            selected = settings.weightStep == step,
                            onClick = { onWeightStepChange(step) },
                            label = { Text(Fmt.kg(step)) },
                        )
                    }
                }
            }

            Column(Modifier.reveal(reveal, "appearance"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_appearance))
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
                val modes = ThemeMode.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        ) {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.settings_theme_system
                                        ThemeMode.LIGHT -> R.string.settings_theme_light
                                        ThemeMode.DARK -> R.string.settings_theme_dark
                                    },
                                ),
                            )
                        }
                    }
                }
                if (dynamicColorAvailable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = stringResource(R.string.settings_dynamic_color_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = settings.dynamicColor, onCheckedChange = onDynamicColorChange)
                    }
                }
            }

            Column(Modifier.reveal(reveal, "timer"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_timer))
                SettingsItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_hint),
                    onClick = onOpenNotifications,
                )
                SettingsItem(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.settings_alert),
                    subtitle = stringResource(R.string.settings_alert_hint),
                    onClick = onOpenAlertSettings,
                )
            }

            Column(Modifier.reveal(reveal, "data"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_data))
                Text(
                    text = stringResource(
                        R.string.settings_data_summary,
                        pluralStringResource(R.plurals.exercises_count, exerciseCount, exerciseCount),
                        pluralStringResource(R.plurals.sessions_count, entryCount, entryCount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SettingsItem(
                    icon = Icons.Rounded.Upload,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_hint),
                    onClick = onExport,
                )
                SettingsItem(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_hint),
                    onClick = onImport,
                )
                Text(
                    text = stringResource(if (sync.account != null) R.string.settings_data_hint_synced else R.string.settings_data_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(Modifier.reveal(reveal, "about"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.settings_about))
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version, versionName),
                    subtitle = stringResource(R.string.settings_about_text),
                    onClick = null,
                )
            }
        }
    }
}

/** A daily reminder: on or off, and its time, changed by touching the row. */
@Composable
private fun ReminderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    reminder: Reminder,
    onChange: (Reminder) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.reminder_change_time)) { picking = true }
            .padding(vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = reminder.enabled, onCheckedChange = { onChange(reminder.copy(enabled = it)) })
    }
    if (picking) {
        val state = rememberTimePickerState(initialHour = reminder.time.hour, initialMinute = reminder.time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.reminder_time_title)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Choosing a time is asking for the reminder.
                        onChange(Reminder(enabled = true, time = LocalTime.of(state.hour, state.minute)))
                        picking = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
