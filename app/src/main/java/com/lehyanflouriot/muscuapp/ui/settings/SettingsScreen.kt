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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Settings
import com.lehyanflouriot.muscuapp.core.model.ThemeMode
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader

private val WEIGHT_STEPS = listOf(0.5, 1.0, 1.25, 2.0, 2.5, 5.0)

@Composable
fun SettingsScreen(
    settings: Settings,
    exerciseCount: Int,
    entryCount: Int,
    versionName: String,
    dynamicColorAvailable: Boolean,
    onBack: () -> Unit,
    onWeightStepChange: (Double) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAlertSettings: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
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
                text = stringResource(R.string.settings_data_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            Modifier.fillMaxWidth()
        },
    )
}
