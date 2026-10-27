package com.lehyanflouriot.muscuapp.sync

import android.app.Activity
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.lehyanflouriot.muscuapp.BuildConfig
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.sync.AccessTokens
import com.lehyanflouriot.muscuapp.core.sync.DriveRemote
import com.lehyanflouriot.muscuapp.core.sync.FirstSyncChoice
import com.lehyanflouriot.muscuapp.core.sync.SyncEngine
import com.lehyanflouriot.muscuapp.core.sync.SyncException
import com.lehyanflouriot.muscuapp.core.sync.SyncProblem
import com.lehyanflouriot.muscuapp.core.sync.SyncStatus
import com.lehyanflouriot.muscuapp.data.FileDataStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.File

/** Where connecting the Google account stands. */
sealed interface ConnectStep {
    /** Google has to ask the user something: show [intent], then give its result to [SyncController.completeConnect]. */
    data class Ask(val intent: PendingIntent) : ConnectStep

    data object Connected : ConnectStep

    data object Canceled : ConnectStep

    data class Failed(val problem: SyncProblem, val detail: String?) : ConnectStep
}

/**
 * Syncs the data with the Google account when it is useful: at launch, when the app comes back,
 * a few seconds after a change, regularly while the app is open, and once the network is back
 * when changes could not be sent.
 */
class SyncController(
    context: Context,
    private val repository: WorkoutRepository,
    private val scope: CoroutineScope,
) {
    private val context = context.applicationContext
    private val authorizer by lazy { GoogleAuthorizer(this.context) }
    private val device = deviceName(this.context)
    private val engine = SyncEngine(
        repository = repository,
        storage = FileDataStorage(File(this.context.filesDir, FILE_NAME)),
        remoteFor = { account -> drive(authorizer.tokensFor(account)) },
        appVersion = BuildConfig.VERSION_CODE,
    )

    val status: StateFlow<SyncStatus> = engine.status

    private val requests = Channel<Unit>(Channel.CONFLATED)

    /** The answer to the question asked the first time, for the next sync. */
    @Volatile
    private var choice: FirstSyncChoice? = null

    private var poller: Job? = null

    fun start() {
        scope.launch(Dispatchers.IO) {
            engine.load()
            launch {
                for (request in requests) afterSync(engine.sync(choice.also { choice = null }))
            }
            requestSync()
            repository.data.filterNotNull().collectLatest { data ->
                if (engine.hasChanges(data)) {
                    delay(CHANGE_DELAY_MS)
                    requestSync()
                }
            }
        }
    }

    /** Syncs as soon as possible, or once more right after the sync running now. */
    fun requestSync() {
        if (engine.account != null) requests.trySend(Unit)
    }

    /** Syncs right now, for the user who asked for it. Returns what went wrong, if anything. */
    suspend fun syncNow(): SyncProblem? = engine.sync(choice.also { choice = null }).also(::afterSync)

    fun onForeground() {
        requestSync()
        poller?.cancel()
        poller = scope.launch {
            while (true) {
                delay(POLL_MS)
                requestSync()
            }
        }
    }

    fun onBackground() {
        poller?.cancel()
        poller = null
        if (hasChanges()) {
            requestSync()
            // In case the app is closed before, or without network.
            scheduleRetry()
        }
    }

    /** First step to connect a Google account, or to connect the same one again with [again]. */
    suspend fun connect(again: Boolean = false): ConnectStep = connecting {
        val account = engine.account.takeIf { again }
        val result = authorizer.authorize(account, chooseAccount = account.isNullOrEmpty())
        val intent = result.pendingIntent
        if (result.hasResolution() && intent != null) ConnectStep.Ask(intent) else finishConnect(result)
    }

    /** Ends connecting with what the screen of Google returned. */
    suspend fun completeConnect(resultCode: Int, data: Intent?): ConnectStep = connecting {
        if (resultCode != Activity.RESULT_OK) ConnectStep.Canceled else finishConnect(authorizer.resultOf(data))
    }

    /** Answers the question asked the first time this device syncs while the account holds data. */
    fun choose(answer: FirstSyncChoice) {
        choice = answer
        requestSync()
    }

    /** Stops syncing: the data stays here, and in the account for the other devices. */
    fun disconnect() {
        scope.launch {
            engine.disconnect()
            cancelRetry()
        }
    }

    /** Syncs for the background job. Returns whether to try again later. */
    fun runJob(onDone: (retry: Boolean) -> Unit): Job = scope.launch {
        val problem = engine.sync()
        onDone((problem == SyncProblem.OFFLINE || problem == SyncProblem.SERVER) && hasChanges())
    }

    private suspend fun finishConnect(result: AuthorizationResult): ConnectStep {
        val token = result.accessToken
        if (token == null || DriveRemote.SCOPE !in result.grantedScopes) {
            return ConnectStep.Failed(SyncProblem.SIGN_IN, null)
        }
        // Also tells right away whether the Drive API answers for the app.
        val email = drive(GivenToken(token)).accountEmail()
        engine.connect(email.orEmpty())
        requestSync()
        return ConnectStep.Connected
    }

    private inline fun connecting(block: () -> ConnectStep): ConnectStep = try {
        block()
    } catch (e: SyncException) {
        ConnectStep.Failed(e.problem, e.message)
    }

    private fun hasChanges(): Boolean = repository.data.value?.let(engine::hasChanges) == true

    private fun afterSync(problem: SyncProblem?) {
        when {
            problem == null && !hasChanges() -> cancelRetry()
            problem == SyncProblem.OFFLINE && hasChanges() -> scheduleRetry()
        }
    }

    private fun drive(tokens: AccessTokens) = DriveRemote(tokens, BuildConfig.VERSION_CODE, device)

    /** Runs [SyncJobService] once the network is there. */
    private fun scheduleRetry() {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (scheduler.getPendingJob(SyncJobService.JOB_ID) != null) return
        val job = JobInfo.Builder(SyncJobService.JOB_ID, ComponentName(context, SyncJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(RETRY_BACKOFF_MS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build()
        runCatching { scheduler.schedule(job) }
    }

    private fun cancelRetry() {
        runCatching { context.getSystemService(JobScheduler::class.java)?.cancel(SyncJobService.JOB_ID) }
    }

    /** A token just given by Google, used once. */
    private class GivenToken(private val token: String) : AccessTokens {
        override suspend fun token(): String = token

        override suspend fun invalidate(token: String) = Unit
    }

    private companion object {
        const val FILE_NAME = "muscuapp-sync.json"
        const val CHANGE_DELAY_MS = 3_000L
        const val POLL_MS = 60_000L
        const val RETRY_BACKOFF_MS = 60_000L

        /** The name the user gave the device, shown to the other devices. */
        fun deviceName(context: Context): String =
            runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: Build.MODEL
    }
}
