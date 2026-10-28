package com.lehyanflouriot.muscuapp.core.sync

import com.lehyanflouriot.muscuapp.core.data.AppJson
import com.lehyanflouriot.muscuapp.core.data.DataStorage
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.model.AppData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** What the device keeps about the sync. */
@Serializable
internal data class SyncState(
    val account: String? = null,
    val documentId: String? = null,
    val revision: String? = null,
    /** The data both sides had after the last sync: what tells what changed on each side since. */
    val base: JsonElement? = null,
    val lastSync: Long? = null,
)

/**
 * Keeps the data of this device and the copy shared with the other devices the same. The changes
 * of each side are found by comparing it to the data both had after the last sync, so that what
 * was done on two devices in between is kept from both.
 */
class SyncEngine(
    private val repository: WorkoutRepository,
    private val storage: DataStorage,
    private val remoteFor: (account: String) -> SyncRemote,
    private val appVersion: Int,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private sealed interface Outcome {
        data object Done : Outcome

        /** The shared copy changed during the sync: start again. */
        data object Changed : Outcome

        data class Ask(val document: RemoteDocument) : Outcome
    }

    private val syncing = Mutex()
    private val lock = Any()
    private val loaded = CompletableDeferred<Unit>()

    @Volatile
    private var state = SyncState()

    /** [SyncState.base], read once. */
    @Volatile
    private var base: AppData? = null

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val account: String? get() = state.account

    /** Reads what was kept about the sync. Called once, before anything else. */
    fun load() {
        if (loaded.isCompleted) return
        synchronized(lock) {
            val saved = runCatching { storage.read()?.let { AppJson.json.decodeFromString(SyncState.serializer(), it) } }
                .getOrNull() ?: SyncState()
            state = saved
            // Brought up to date like the data of both sides, so that only what they changed differs.
            base = saved.base?.let { runCatching { WorkoutRepository.sanitize(AppJson.fromTree(it)) }.getOrNull() }
            _status.value = SyncStatus(account = saved.account, lastSync = saved.lastSync)
            loaded.complete(Unit)
        }
    }

    /** Whether [data] holds changes the shared copy does not have yet. */
    fun hasChanges(data: AppData): Boolean = state.account != null && data != base

    /** Starts syncing with [account]. What was synced before with the same account goes on. */
    suspend fun connect(account: String) {
        loaded.await()
        synchronized(lock) {
            if (account != state.account) {
                save(SyncState(account = account))
                base = null
            }
            _status.value = SyncStatus(account = account, lastSync = state.lastSync)
        }
    }

    /** Stops syncing. The data stays on the device, and in the account for the other devices. */
    suspend fun disconnect() {
        loaded.await()
        synchronized(lock) {
            save(SyncState())
            base = null
            _status.value = SyncStatus()
        }
    }

    /**
     * Brings both sides up to date. [choice] answers the question asked the first time this device
     * syncs while the account already holds data. Returns what went wrong, if anything.
     */
    suspend fun sync(choice: FirstSyncChoice? = null): SyncProblem? {
        loaded.await()
        return syncing.withLock {
            val account = state.account ?: return@withLock null
            // Nothing can be done until the user tells what to do with the data of the account.
            if (choice == null && _status.value.choice != null) return@withLock null
            val outcome = try {
                run(account, remoteFor(account), choice)
            } catch (e: CancellationException) {
                _status.update { it.copy(running = false) }
                throw e
            } catch (e: SyncException) {
                e
            } catch (e: Exception) {
                SyncException(SyncProblem.SERVER, e.message, e)
            }
            synchronized(lock) {
                _status.value = when {
                    state.account != account -> _status.value.copy(running = false)
                    outcome is SyncException -> _status.value.copy(running = false, problem = outcome.problem, detail = outcome.message)
                    outcome is Outcome.Ask -> SyncStatus(account = account, lastSync = state.lastSync, choice = outcome.document)
                    else -> SyncStatus(account = account, lastSync = clock())
                }
            }
            (outcome as? SyncException)?.problem
        }
    }

    private suspend fun run(account: String, remote: SyncRemote, choice: FirstSyncChoice?): Outcome {
        repeat(MAX_ATTEMPTS) {
            val outcome = attempt(account, remote, choice)
            if (outcome != Outcome.Changed) return outcome
        }
        throw SyncException(SyncProblem.SERVER, "The shared copy keeps changing")
    }

    private suspend fun attempt(account: String, remote: SyncRemote, choice: FirstSyncChoice?): Outcome {
        val local = repository.data.filterNotNull().first()
        val document = remote.find()
        if (document == null) {
            // Nothing shared yet, or the shared copy was deleted: this device shares its data.
            transferring()
            synced(account, remote.upload(null, AppJson.encode(local)), local)
            return Outcome.Done
        }
        if (document.appVersion > appVersion) throw SyncException(SyncProblem.UPDATE_APP)
        val common = base?.takeIf { state.documentId == document.id }
        if (common != null && document.revision == state.revision) {
            // Nothing changed on the other devices since the last sync.
            if (local != common) {
                transferring()
                synced(account, remote.upload(document, AppJson.encode(local)), local)
            }
            return Outcome.Done
        }
        transferring()
        val text = remote.download(document)
        val shared = try {
            WorkoutRepository.sanitize(AppJson.decode(text))
        } catch (e: IllegalArgumentException) {
            throw SyncException(SyncProblem.UNREADABLE, e.message, e)
        }
        val merged = when {
            common != null -> mergeData(common, local, shared)
            // The shared copy was created again, by another device or after being deleted: everything is kept.
            state.documentId != null -> mergeData(null, local, shared)
            choice == FirstSyncChoice.USE_ACCOUNT -> shared
            choice == FirstSyncChoice.MERGE -> mergeData(null, local, shared)
            choice == FirstSyncChoice.KEEP_DEVICE -> local
            local.isPristine() -> shared
            else -> return Outcome.Ask(document)
        }
        val written = if (merged == shared) {
            document
        } else {
            // Another device may have written meanwhile: what it wrote must not be overwritten.
            val latest = remote.find()
            if (latest == null || latest.id != document.id || latest.revision != document.revision) return Outcome.Changed
            remote.upload(document, AppJson.encode(merged))
        }
        if (state.account != account) return Outcome.Done
        // Known before the data changes, so that the change is not taken for one to send.
        base = merged
        repository.applySynced(local, merged)
        synced(account, written, merged)
        return Outcome.Done
    }

    /** Shows that data is on its way: looking whether something changed is not worth showing. */
    private fun transferring() {
        _status.update { it.copy(running = true) }
    }

    /**
     * Notes that both sides now hold [data]. It is kept on the device only once the data itself is
     * written: an older document with a newer base would make the next sync undo the other devices.
     */
    private suspend fun synced(account: String, document: RemoteDocument, data: AppData) {
        synchronized(lock) {
            if (state.account != account) return
            base = data
            state = state.copy(documentId = document.id, revision = document.revision, base = null, lastSync = clock())
        }
        val durable = repository.awaitSaved()
        synchronized(lock) {
            if (durable && state.account == account && base == data) save(state.copy(base = AppJson.tree(data)))
        }
    }

    private fun save(newState: SyncState) {
        state = newState.copy(base = null)
        runCatching { storage.write(AppJson.json.encodeToString(SyncState.serializer(), newState)) }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
