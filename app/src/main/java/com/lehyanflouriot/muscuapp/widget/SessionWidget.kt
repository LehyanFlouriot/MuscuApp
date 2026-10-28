package com.lehyanflouriot.muscuapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.lehyanflouriot.muscuapp.appContainer
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId

/**
 * The home screen widget: the session of the day and the rest going on. The app updates it
 * whenever its data or the rest change; an alarm also updates it when the day changes.
 */
class SessionWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateWhenLoaded(context)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> updateWhenLoaded(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) = cancelRefresh(context)

    /** Updates the widget once the data is read from the disk, when the app was not running. */
    private fun updateWhenLoaded(context: Context) {
        val pending = goAsync()
        val container = context.appContainer
        container.scope.launch {
            try {
                withTimeoutOrNull(LOAD_TIMEOUT_MS) { container.repository.data.filterNotNull().first() }
                update(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.lehyanflouriot.muscuapp.action.REFRESH_WIDGET"
        private const val REQUEST_REFRESH = 32
        private const val LOAD_TIMEOUT_MS = 8_000L

        /** Shows the current session and rest on the widgets placed on the home screen. */
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SessionWidget::class.java))
            if (ids.isEmpty()) return
            val container = context.appContainer
            // Until the data is loaded, the widget keeps what it shows: it is updated right after.
            val data = container.repository.data.value ?: return
            val timer = container.restTimer.state.value
            val now = container.restTimer.now()
            manager.updateAppWidget(ids, SessionWidgetViews.build(context, data, timer, LocalDate.now(), now))
            scheduleRefresh(context, timer, now)
        }

        /** Updates the widget at midnight, or when a finished rest stops showing. */
        private fun scheduleRefresh(context: Context, timer: TimerState, now: Long) {
            val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val restGone = (timer as? TimerState.Finished)?.let { it.finishedAt + RestTimer.FINISHED_DISPLAY_MS + 1_000 }
            val at = listOfNotNull(midnight, restGone?.takeIf { it > now }).min()
            // Not a wakeup alarm: a widget only needs to be right when the screen is on.
            context.getSystemService(AlarmManager::class.java)?.set(AlarmManager.RTC, at, refreshIntent(context))
        }

        private fun cancelRefresh(context: Context) {
            context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
        }

        private fun refreshIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_REFRESH,
                Intent(context, SessionWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
