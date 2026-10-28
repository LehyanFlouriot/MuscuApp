package com.lehyanflouriot.muscuapp.core.sync

import com.lehyanflouriot.muscuapp.core.data.AppJson
import com.lehyanflouriot.muscuapp.core.data.DataStorage
import com.lehyanflouriot.muscuapp.core.data.ExerciseDraft
import com.lehyanflouriot.muscuapp.core.data.HypertrophyProgram
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.data.removeLine
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.Training
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncEngineTest {
    private class MemoryStorage(var content: String? = null) : DataStorage {
        override fun read(): String? = content
        override fun write(content: String) {
            this.content = content
        }
        override fun keepUnreadableCopy(content: String) = Unit
    }

    /** The account, shared by the devices of the test. */
    private class FakeRemote : SyncRemote {
        var document: RemoteDocument? = null
        var content: String? = null
        var uploads = 0
        var offline = false
        var appVersion = APP_VERSION

        /** Runs once, right before the next look for the document: another device acting meanwhile. */
        var meanwhile: (() -> Unit)? = null
        private var finds = 0

        override suspend fun find(): RemoteDocument? {
            if (offline) throw SyncException(SyncProblem.OFFLINE)
            if (++finds > 1) meanwhile?.also { meanwhile = null }?.invoke()
            return document
        }

        override suspend fun download(document: RemoteDocument): String = content!!

        override suspend fun upload(document: RemoteDocument?, content: String): RemoteDocument {
            if (offline) throw SyncException(SyncProblem.OFFLINE)
            uploads++
            this.content = content
            return RemoteDocument(
                id = document?.id ?: "doc-${UUID.randomUUID()}",
                revision = content.hashCode().toString(),
                appVersion = appVersion,
            ).also { this.document = it }
        }

        /** What the account holds. */
        fun data(): AppData = AppJson.decode(content!!)

        fun resetFinds() {
            finds = 0
        }
    }

    private class Device(val repository: WorkoutRepository, val engine: SyncEngine, val syncStorage: MemoryStorage) {
        val data: AppData get() = repository.data.value!!
    }

    private val today = LocalDate.of(2026, 10, 27)
    private val remote = FakeRemote()

    private fun TestScope.device(
        syncStorage: MemoryStorage = MemoryStorage(),
        storage: MemoryStorage = MemoryStorage(),
        appVersion: Int = APP_VERSION,
        seedProgram: Boolean = false,
    ): Device {
        val repository = WorkoutRepository(
            storage,
            backgroundScope,
            UnconfinedTestDispatcher(testScheduler),
            today = { today },
            seedProgram = seedProgram,
        )
        repository.load()
        val engine = SyncEngine(repository, syncStorage, remoteFor = { remote }, appVersion = appVersion, clock = { 42L })
        engine.load()
        return Device(repository, engine, syncStorage)
    }

    private suspend fun Device.connectAndSync(choice: FirstSyncChoice? = null): SyncProblem? {
        engine.connect(ACCOUNT)
        return engine.sync(choice)
    }

    private fun Device.addSquat(): String {
        val id = repository.addExercise(ExerciseDraft("Squat", MuscleGroup.LEGS))
        repository.saveEntry(null, id, today, listOf(WorkSet(5, 100.0)))
        return id
    }

    @Test
    fun theFirstDeviceSharesItsData() = runTest {
        val phone = device()
        phone.addSquat()
        assertNull(phone.connectAndSync())
        assertEquals(phone.data, remote.data())
        assertEquals(1, remote.uploads)
        val status = phone.engine.status.value
        assertEquals(ACCOUNT, status.account)
        assertEquals(42L, status.lastSync)
        assertNull(status.problem)
        assertFalse(phone.engine.hasChanges(phone.data))
        // Nothing changed: nothing is sent again.
        assertNull(phone.engine.sync())
        assertEquals(1, remote.uploads)
    }

    @Test
    fun aNewDeviceTakesTheDataOfTheAccount() = runTest {
        val phone = device()
        phone.addSquat()
        phone.repository.saveBodyWeight(today, 70.4)
        phone.connectAndSync()

        val tablet = device()
        assertNull(tablet.connectAndSync())
        assertEquals(phone.data, tablet.data)
        assertEquals(1, remote.uploads)
        assertNull(tablet.engine.status.value.choice)
    }

    @Test
    fun whatEachDeviceDidIsKept() = runTest {
        val phone = device()
        phone.connectAndSync()
        val tablet = device()
        tablet.connectAndSync()

        // Both are used before syncing again.
        val squat = phone.addSquat()
        tablet.repository.saveBodyWeight(today, 71.0)
        tablet.repository.setWater(today, 6)
        phone.repository.setWater(today, 4)
        assertTrue(phone.engine.hasChanges(phone.data))

        phone.engine.sync()
        tablet.engine.sync()
        phone.engine.sync()

        for (device in listOf(phone, tablet)) {
            assertEquals(squat, device.data.exercises.single().id)
            assertEquals(1, device.data.entries.size)
            assertEquals(71.0, device.data.bodyWeights.single().weight, 0.0)
            // Both changed the water of the day: the last device to sync won.
            assertEquals(6, device.data.nutrition.day(today).water)
        }
        assertEquals(phone.data, tablet.data)
        assertEquals(phone.data, remote.data())
        assertFalse(phone.engine.hasChanges(phone.data))
        assertFalse(tablet.engine.hasChanges(tablet.data))
    }

    @Test
    fun deletionsReachTheOtherDevices() = runTest {
        val phone = device()
        val squat = phone.addSquat()
        phone.repository.addExercise(ExerciseDraft("Tractions", MuscleGroup.BACK))
        phone.connectAndSync()
        val tablet = device()
        tablet.connectAndSync()

        tablet.repository.deleteExercise(squat)
        tablet.engine.sync()
        phone.engine.sync()
        assertEquals(listOf("Tractions"), phone.data.exercises.map { it.name })
        assertTrue(phone.data.entries.isEmpty())
    }

    @Test
    fun aDeviceWithItsOwnDataAsksWhatToDo() = runTest {
        val phone = device()
        phone.addSquat()
        phone.connectAndSync()
        val tablet = device()
        tablet.repository.addExercise(ExerciseDraft("Pompes", MuscleGroup.CHEST))
        val own = tablet.data

        assertNull(tablet.connectAndSync())
        val asked = tablet.engine.status.value.choice
        assertNotNull(asked)
        assertEquals(remote.document, asked)
        // Nothing moved, and nothing moves until the user answers.
        assertEquals(own, tablet.data)
        assertEquals(1, remote.uploads)
        assertNull(tablet.engine.sync())
        assertEquals(own, tablet.data)

        assertNull(tablet.engine.sync(FirstSyncChoice.MERGE))
        assertNull(tablet.engine.status.value.choice)
        assertEquals(setOf("Squat", "Pompes"), tablet.data.exercises.map { it.name }.toSet())
        assertEquals(tablet.data, remote.data())
        phone.engine.sync()
        assertEquals(tablet.data, phone.data)
    }

    @Test
    fun theAccountOrTheDeviceCanBeKept() = runTest {
        val phone = device()
        phone.addSquat()
        phone.connectAndSync()

        val tablet = device()
        tablet.repository.addExercise(ExerciseDraft("Pompes", MuscleGroup.CHEST))
        tablet.connectAndSync()
        tablet.engine.sync(FirstSyncChoice.USE_ACCOUNT)
        assertEquals(phone.data, tablet.data)

        val laptop = device()
        laptop.repository.addExercise(ExerciseDraft("Dips", MuscleGroup.TRICEPS))
        laptop.connectAndSync()
        laptop.engine.sync(FirstSyncChoice.KEEP_DEVICE)
        assertEquals(listOf("Dips"), remote.data().exercises.map { it.name })
        phone.engine.sync()
        assertEquals(listOf("Dips"), phone.data.exercises.map { it.name })
    }

    @Test
    fun changesMadeDuringTheSyncAreKept() = runTest {
        val phone = device()
        phone.connectAndSync()
        val tablet = device()
        tablet.connectAndSync()

        tablet.repository.saveBodyWeight(today, 71.0)
        tablet.engine.sync()
        phone.repository.saveBodyWeight(today.minusDays(1), 70.0)
        // The phone looks again before sending what it merged, while the user notes a session.
        remote.resetFinds()
        var squat: String? = null
        remote.meanwhile = { squat = phone.addSquat() }
        phone.engine.sync()

        assertEquals(squat, phone.data.exercises.single().id)
        assertEquals(2, phone.data.bodyWeights.size)
        // What was noted during the sync is sent the next time.
        assertTrue(phone.engine.hasChanges(phone.data))
        phone.engine.sync()
        tablet.engine.sync()
        assertEquals(phone.data, tablet.data)
        assertEquals(squat, tablet.data.exercises.single().id)
    }

    @Test
    fun whatAnotherDeviceSendsDuringTheSyncIsNotOverwritten() = runTest {
        val phone = device()
        phone.connectAndSync()
        val tablet = device()
        tablet.connectAndSync()

        tablet.repository.saveBodyWeight(today, 71.0)
        tablet.engine.sync()
        phone.repository.saveBodyWeight(today.minusDays(1), 70.0)
        tablet.repository.setWater(today, 3)
        // The tablet sends its change while the phone merges.
        remote.resetFinds()
        remote.meanwhile = {
            val document = remote.document!!
            val changed = AppJson.decode(remote.content!!).let { data ->
                data.copy(nutrition = data.nutrition.copy(days = tablet.data.nutrition.days))
            }
            remote.content = AppJson.encode(changed)
            remote.document = document.copy(revision = "tablet")
        }
        phone.engine.sync()

        assertEquals(2, remote.data().bodyWeights.size)
        assertEquals(3, remote.data().nutrition.day(today).water)
        assertEquals(remote.data(), phone.data)
    }

    @Test
    fun anOlderAppDoesNotTouchWhatANewerOneWrote() = runTest {
        val phone = device()
        phone.addSquat()
        remote.appVersion = APP_VERSION + 1
        phone.connectAndSync()
        val shared = remote.content

        val tablet = device()
        tablet.repository.addExercise(ExerciseDraft("Pompes", MuscleGroup.CHEST))
        val own = tablet.data
        assertEquals(SyncProblem.UPDATE_APP, tablet.connectAndSync(FirstSyncChoice.KEEP_DEVICE))
        assertEquals(SyncProblem.UPDATE_APP, tablet.engine.status.value.problem)
        assertEquals(shared, remote.content)
        assertEquals(own, tablet.data)
    }

    @Test
    fun withoutNetworkNothingChangesUntilItIsBack() = runTest {
        val phone = device()
        phone.connectAndSync()
        phone.addSquat()
        remote.offline = true
        assertEquals(SyncProblem.OFFLINE, phone.engine.sync())
        assertEquals(SyncProblem.OFFLINE, phone.engine.status.value.problem)
        assertTrue(phone.engine.hasChanges(phone.data))

        remote.offline = false
        assertNull(phone.engine.sync())
        assertNull(phone.engine.status.value.problem)
        assertEquals(phone.data, remote.data())
    }

    @Test
    fun theSyncGoesOnAfterARestart() = runTest {
        val storage = MemoryStorage()
        val syncStorage = MemoryStorage()
        val phone = device(syncStorage, storage)
        phone.addSquat()
        phone.connectAndSync()

        val restarted = device(syncStorage, MemoryStorage(storage.content))
        assertEquals(ACCOUNT, restarted.engine.status.value.account)
        assertFalse(restarted.engine.hasChanges(restarted.data))
        assertNull(restarted.engine.sync())
        assertEquals(1, remote.uploads)
    }

    @Test
    fun aDeletedSharedCopyIsCreatedAgainWithEverything() = runTest {
        val phone = device()
        phone.addSquat()
        phone.connectAndSync()
        val tablet = device()
        tablet.connectAndSync()

        // The copy is deleted from Drive, and the tablet shares its data again first.
        remote.document = null
        remote.content = null
        tablet.repository.saveBodyWeight(today, 71.0)
        tablet.engine.sync()
        phone.repository.addExercise(ExerciseDraft("Pompes", MuscleGroup.CHEST))
        phone.engine.sync()

        assertEquals(setOf("Squat", "Pompes"), remote.data().exercises.map { it.name }.toSet())
        assertEquals(1, remote.data().bodyWeights.size)
    }

    @Test
    fun disconnectingKeepsTheDataAndStopsTheSync() = runTest {
        val phone = device()
        phone.addSquat()
        phone.connectAndSync()
        phone.engine.disconnect()
        assertNull(phone.engine.status.value.account)
        assertFalse(phone.engine.hasChanges(phone.data))
        phone.repository.saveBodyWeight(today, 70.0)
        assertNull(phone.engine.sync())
        assertEquals(1, remote.uploads)
        assertEquals(1, phone.data.exercises.size)
    }

    @Test
    fun aFreshDeviceHoldsNothingOfItsOwn() = runTest {
        val storage = MemoryStorage()
        val fresh = device(storage = storage, seedProgram = true)
        assertTrue(fresh.data.isPristine())
        assertTrue(device(storage = MemoryStorage(storage.content), seedProgram = true).data.isPristine())
        fresh.repository.setWater(today, 1)
        assertFalse(fresh.data.isPristine())
        val trained = device(seedProgram = true)
        trained.repository.saveEntry(null, "ex-nordic-curl", today, listOf(WorkSet(6)))
        assertFalse(trained.data.isPristine())
    }

    @Test
    fun aNewDeviceWithTheProgramTakesTheDataOfTheAccount() = runTest {
        val phone = device(seedProgram = true)
        phone.repository.saveEntry(null, "ex-tractions-pronation", today, listOf(WorkSet(7), WorkSet(6)))
        phone.connectAndSync()

        val tablet = device(seedProgram = true)
        assertNull(tablet.connectAndSync())
        assertNull(tablet.engine.status.value.choice)
        assertEquals(phone.data.entries, tablet.data.entries)
        assertEquals(phone.data.training, tablet.data.training)
        assertEquals(phone.data.exercises.map { it.id }, tablet.data.exercises.map { it.id })
    }

    @Test
    fun aDeviceUpdatedFromTheSingleProgramKeepsMergingWhatChanged() = runTest {
        // A phone on 1.7.0 synced the single training program of that version.
        val legacy = AppData(
            schemaVersion = 2,
            exercises = HypertrophyProgram.exercises(),
            nutrition = Nutrition(seeded = true),
            training = Training(legacyProgram = HypertrophyProgram.create(), seeded = true),
        )
        val state = SyncState(account = ACCOUNT, documentId = "doc", revision = "r1", base = AppJson.tree(legacy), lastSync = 1L)
        val syncStorage = MemoryStorage(AppJson.json.encodeToString(SyncState.serializer(), state))
        // Meanwhile, a tablet still on 1.7.0 noted a weight.
        remote.content = AppJson.encode(legacy.copy(bodyWeights = listOf(BodyWeightEntry(today, 71.0))))
        remote.document = RemoteDocument(id = "doc", revision = "r2", appVersion = 17)

        // Once updated, the phone has nothing new to send.
        val phone = device(syncStorage, MemoryStorage(AppJson.encode(legacy)), seedProgram = true)
        assertFalse(phone.engine.hasChanges(phone.data))

        // It takes an exercise out of Monday's session, then syncs: both changes are kept.
        phone.repository.updateTrainingProgram(HypertrophyProgram.ID) { it.removeLine("mon-6") }
        assertNull(phone.engine.sync())
        assertEquals(5, phone.data.training.programs.single().day(DayOfWeek.MONDAY).exercises.size)
        assertEquals(71.0, phone.data.bodyWeights.single().weight, 0.0)
        assertEquals(phone.data, remote.data())
    }

    private companion object {
        const val ACCOUNT = "lehyan@example.com"
        const val APP_VERSION = 20
    }
}
