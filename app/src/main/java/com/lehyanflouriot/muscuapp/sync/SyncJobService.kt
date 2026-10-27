package com.lehyanflouriot.muscuapp.sync

import android.app.job.JobParameters
import android.app.job.JobService
import com.lehyanflouriot.muscuapp.appContainer
import kotlinx.coroutines.Job

/** Sends the changes that could not be sent, once the network is back, even with the app closed. */
class SyncJobService : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = appContainer.sync.runJob { retry -> jobFinished(params, retry) }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return true
    }

    companion object {
        const val JOB_ID = 4_101
    }
}
