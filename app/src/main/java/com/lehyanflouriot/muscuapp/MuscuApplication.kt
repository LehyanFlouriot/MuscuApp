package com.lehyanflouriot.muscuapp

import android.app.Application
import android.content.Context
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.data.FileDataStorage
import com.lehyanflouriot.muscuapp.reminder.ReminderNotifications
import com.lehyanflouriot.muscuapp.reminder.Reminders
import com.lehyanflouriot.muscuapp.sync.SyncController
import com.lehyanflouriot.muscuapp.timer.RestNotifications
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.widget.SessionWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.File

class MuscuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.repository.load()
        container.sync.start()
        // Alarms are lost when the app is stopped by force: set them again at each start.
        container.reminders.scheduleAll()
        container.updateWidgets(this)
    }
}

/** Objects shared by the whole app. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository = WorkoutRepository(
        storage = FileDataStorage(File(context.filesDir, "muscuapp-data.json")),
        scope = scope,
        ioDispatcher = Dispatchers.IO,
    )

    /** Keeps the data the same on the devices of the user's Google account. */
    val sync = SyncController(context, repository, scope)

    val restTimer: RestTimer

    /** The daily reminders of this device. */
    val reminders = Reminders(context)

    init {
        RestNotifications(context).createChannels()
        ReminderNotifications(context).createChannel()
        restTimer = RestTimer(context)
    }

    /** Keeps the home screen widget in line with the data and the rest. */
    fun updateWidgets(context: Context) {
        scope.launch {
            combine(repository.data.filterNotNull(), restTimer.state, ::Pair)
                .conflate()
                .collect { SessionWidget.update(context) }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MuscuApplication).container
