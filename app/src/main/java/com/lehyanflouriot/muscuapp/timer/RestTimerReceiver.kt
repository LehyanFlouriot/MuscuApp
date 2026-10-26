package com.lehyanflouriot.muscuapp.timer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lehyanflouriot.muscuapp.appContainer

/** Receives the end-of-rest alarm and the buttons of the timer notifications. */
class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timer = context.appContainer.restTimer
        when (intent.action) {
            ACTION_FINISH -> timer.onAlarm()
            ACTION_ADD -> timer.adjust(ADD_MS)
            ACTION_PAUSE -> timer.pause()
            ACTION_RESUME -> timer.resume()
            ACTION_STOP -> timer.stop()
            ACTION_RESTART -> timer.restart()
        }
    }

    companion object {
        const val ACTION_FINISH = "com.lehyanflouriot.muscuapp.action.REST_FINISH"
        const val ACTION_ADD = "com.lehyanflouriot.muscuapp.action.REST_ADD"
        const val ACTION_PAUSE = "com.lehyanflouriot.muscuapp.action.REST_PAUSE"
        const val ACTION_RESUME = "com.lehyanflouriot.muscuapp.action.REST_RESUME"
        const val ACTION_STOP = "com.lehyanflouriot.muscuapp.action.REST_STOP"
        const val ACTION_RESTART = "com.lehyanflouriot.muscuapp.action.REST_RESTART"

        /** Time added by the "+30 s" button of the notification. */
        const val ADD_MS = 30_000L

        private val REQUEST_CODES = mapOf(
            ACTION_FINISH to 1,
            ACTION_ADD to 2,
            ACTION_PAUSE to 3,
            ACTION_RESUME to 4,
            ACTION_STOP to 5,
            ACTION_RESTART to 6,
        )

        fun pendingIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODES.getValue(action),
                Intent(context, RestTimerReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
