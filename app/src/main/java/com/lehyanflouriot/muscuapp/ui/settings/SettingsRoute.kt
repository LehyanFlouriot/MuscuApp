package com.lehyanflouriot.muscuapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.BuildConfig
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.sync.SyncProblem
import com.lehyanflouriot.muscuapp.sync.ConnectStep
import com.lehyanflouriot.muscuapp.sync.SyncController
import com.lehyanflouriot.muscuapp.timer.RestNotifications
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
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
    sync: SyncController,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val enableNotifications = rememberNotificationEnabler()
    var pendingImport by remember { mutableStateOf<AppData?>(null) }
    val syncStatus by sync.status.collectAsStateWithLifecycle()
    var connecting by rememberSaveable { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf(true) }
    var disconnecting by remember { mutableStateOf(false) }
    var syncingNow by remember { mutableStateOf(false) }

    fun report(step: ConnectStep) {
        connecting = false
        when (step) {
            ConnectStep.Connected -> {
                choosing = true
                messenger.show(context.getString(R.string.sync_connected))
            }
            is ConnectStep.Failed -> messenger.show(
                context.getString(
                    when (step.problem) {
                        SyncProblem.SIGN_IN -> R.string.sync_connect_refused
                        SyncProblem.OFFLINE -> R.string.sync_problem_offline
                        SyncProblem.SETUP -> R.string.sync_problem_setup
                        else -> R.string.sync_problem_server
                    },
                ),
            )
            is ConnectStep.Ask, ConnectStep.Canceled -> Unit
        }
    }
    // Connecting goes through the screens of Google when it has something to ask.
    val authorizeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        scope.launch { report(sync.completeConnect(result.resultCode, result.data)) }
    }
    fun connect(again: Boolean) {
        connecting = true
        scope.launch {
            when (val step = sync.connect(again)) {
                is ConnectStep.Ask -> authorizeLauncher.launch(IntentSenderRequest.Builder(step.intent.intentSender).build())
                else -> report(step)
            }
        }
    }

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
        sync = if (syncingNow) syncStatus.copy(running = true) else syncStatus,
        syncConnecting = connecting,
        today = rememberToday(),
        syncActions = SyncActions(
            onConnect = { connect(again = false) },
            onReconnect = { connect(again = true) },
            onSyncNow = {
                syncingNow = true
                scope.launch {
                    val problem = sync.syncNow()
                    syncingNow = false
                    messenger.show(context.getString(problem?.let(::problemTextRes) ?: R.string.sync_done_now))
                }
            },
            onDisconnect = { disconnecting = true },
            onChoose = { choosing = true },
        ),
        onBack = onBack,
        onWeightStepChange = { step -> repository.updateSettings { it.copy(weightStep = step) } },
        onThemeChange = { mode -> repository.updateSettings { it.copy(themeMode = mode) } },
        onDynamicColorChange = { enabled -> repository.updateSettings { it.copy(dynamicColor = enabled) } },
        onOpenNotifications = enableNotifications,
        onOpenAlertSettings = { openNotificationSettings(context, RestNotifications.CHANNEL_DONE) },
        onExport = { exportLauncher.launch("muscuapp-sauvegarde-${LocalDate.now()}.json") },
        onImport = { importLauncher.launch(arrayOf("*/*")) },
    )

    syncStatus.choice?.takeIf { choosing }?.let { document ->
        FirstSyncDialog(
            document = document,
            today = rememberToday(),
            onDismiss = { choosing = false },
            onConfirm = { choice ->
                choosing = false
                sync.choose(choice)
            },
        )
    }

    if (disconnecting) {
        ConfirmDialog(
            title = stringResource(R.string.sync_disconnect_title),
            text = stringResource(R.string.sync_disconnect_text),
            confirmLabel = stringResource(R.string.sync_disconnect),
            icon = Icons.Rounded.CloudOff,
            onDismiss = { disconnecting = false },
            onConfirm = {
                disconnecting = false
                sync.disconnect()
                messenger.show(context.getString(R.string.sync_disconnected))
            },
        )
    }

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
