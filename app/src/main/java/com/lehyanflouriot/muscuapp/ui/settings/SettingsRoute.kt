package com.lehyanflouriot.muscuapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lehyanflouriot.muscuapp.BuildConfig
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.timer.RestNotifications
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.rest.openNotificationSettings
import com.lehyanflouriot.muscuapp.ui.rest.rememberNotificationEnabler
import com.lehyanflouriot.muscuapp.ui.theme.supportsDynamicColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Backups bigger than this are certainly not MuscuApp files. */
private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024

@Composable
fun SettingsRoute(
    data: AppData,
    repository: WorkoutRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val enableNotifications = rememberNotificationEnabler()
    var pendingImport by remember { mutableStateOf<AppData?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val backup = repository.exportBackup()
                val written = runCatching {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("No output stream")
                        stream.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
                    }
                }.isSuccess
                messenger.show(context.getString(if (written) R.string.backup_export_done else R.string.backup_export_failed))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val parsed = runCatching {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri) ?: error("No input stream")
                        val bytes = stream.use { it.readBytes() }
                        require(bytes.size <= MAX_BACKUP_BYTES) { "File too big" }
                        repository.parseBackup(bytes.toString(Charsets.UTF_8))
                    }
                }
                parsed
                    .onSuccess { pendingImport = it }
                    .onFailure { messenger.show(context.getString(R.string.backup_import_invalid)) }
            }
        }
    }

    SettingsScreen(
        settings = data.settings,
        exerciseCount = data.exercises.size,
        entryCount = data.entries.size,
        versionName = BuildConfig.VERSION_NAME,
        dynamicColorAvailable = supportsDynamicColor,
        onBack = onBack,
        onWeightStepChange = { step -> repository.updateSettings { it.copy(weightStep = step) } },
        onThemeChange = { mode -> repository.updateSettings { it.copy(themeMode = mode) } },
        onDynamicColorChange = { enabled -> repository.updateSettings { it.copy(dynamicColor = enabled) } },
        onOpenNotifications = enableNotifications,
        onOpenAlertSettings = { openNotificationSettings(context, RestNotifications.CHANNEL_DONE) },
        onExport = { exportLauncher.launch("muscuapp-sauvegarde-${LocalDate.now()}.json") },
        onImport = { importLauncher.launch(arrayOf("*/*")) },
    )

    pendingImport?.let { backup ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_title),
            text = stringResource(
                R.string.backup_import_text,
                pluralStringResource(R.plurals.exercises_count, backup.exercises.size, backup.exercises.size),
                pluralStringResource(R.plurals.sessions_count, backup.entries.size, backup.entries.size),
            ),
            confirmLabel = stringResource(R.string.backup_import_confirm),
            icon = Icons.Rounded.SettingsBackupRestore,
            onDismiss = { pendingImport = null },
            onConfirm = {
                repository.replaceAll(backup)
                pendingImport = null
                messenger.show(context.getString(R.string.backup_import_done))
            },
        )
    }
}
