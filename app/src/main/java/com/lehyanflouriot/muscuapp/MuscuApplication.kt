package com.lehyanflouriot.muscuapp

import android.app.Application
import android.content.Context
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.data.FileDataStorage
import com.lehyanflouriot.muscuapp.timer.RestNotifications
import com.lehyanflouriot.muscuapp.timer.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class MuscuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.repository.load()
    }
}

/** Objects shared by the whole app. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository = WorkoutRepository(
        storage = FileDataStorage(File(context.filesDir, "muscuapp-data.json")),
        scope = scope,
        ioDispatcher = Dispatchers.IO,
    )

    val restTimer: RestTimer

    init {
        RestNotifications(context).createChannels()
        restTimer = RestTimer(context)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MuscuApplication).container
