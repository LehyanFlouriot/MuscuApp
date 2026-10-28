package com.lehyanflouriot.muscuapp.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.lehyanflouriot.muscuapp.MainActivity
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.currentIndex
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.domain.plannedSets
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.rest.displayed
import java.time.LocalDate

/** What the widget shows: the session of the day and the rest running. */
object SessionWidgetViews {
    /**
     * The widget for [data] on [today], with the rest timer in [timer]. [now] is the wall-clock
     * time, [elapsed] the time since boot of the same instant, which the countdown needs.
     */
    fun build(
        context: Context,
        data: AppData?,
        timer: TimerState,
        today: LocalDate,
        now: Long,
        elapsed: Long = SystemClock.elapsedRealtime(),
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_session)
        val session = data?.let { buildSession(it, today) }
        val rest = timer.displayed(now)
        views.setTextViewText(R.id.widget_label, context.getString(R.string.session_today))
        when {
            session == null -> {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.session_no_program))
                views.setTextViewText(R.id.widget_detail, context.getString(R.string.widget_no_program))
                views.setViewVisibility(R.id.widget_progress, View.GONE)
                views.setViewVisibility(R.id.widget_start, View.GONE)
            }
            session.isRest -> {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.session_rest_title))
                views.setTextViewText(R.id.widget_detail, context.getString(R.string.widget_rest_day))
                views.setViewVisibility(R.id.widget_progress, View.GONE)
                views.setViewVisibility(R.id.widget_start, View.GONE)
            }
            else -> showSession(context, views, session)
        }
        showRest(context, views, rest, now, elapsed)

        // A tap on the widget opens the app, on the rest when one is going on.
        val open = if (rest is TimerState.Idle) null else MainActivity.ACTION_OPEN_REST
        views.setOnClickPendingIntent(R.id.widget_root, openIntent(context, open, REQUEST_OPEN))
        views.setOnClickPendingIntent(R.id.widget_start, openIntent(context, MainActivity.ACTION_OPEN_SESSION, REQUEST_START))
        return views
    }

    private fun showSession(context: Context, views: RemoteViews, session: SessionView) {
        val title = session.day.title.ifBlank { context.getString(R.string.session_untitled) }
        views.setTextViewText(R.id.widget_title, title)
        views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_progress,
            context.getString(R.string.session_progress, session.doneCount, session.items.size),
        )
        views.setContentDescription(
            R.id.widget_progress,
            context.getString(R.string.session_progress_cd, session.doneCount, session.items.size),
        )
        val current = session.currentIndex()?.let { session.items[it] }
        val started = session.items.any { it.doneSets().isNotEmpty() }
        val detail = when {
            current == null -> context.getString(R.string.session_complete)
            !started -> context.getString(
                R.string.widget_first,
                current.exercise.name,
                Fmt.target(current.line.target, current.exercise.measure),
            )
            else -> context.getString(
                R.string.guided_then,
                current.exercise.name,
                current.doneSets().size + 1,
                current.plannedSets,
            )
        }
        views.setTextViewText(R.id.widget_detail, detail)
        views.setViewVisibility(R.id.widget_start, if (session.complete) View.GONE else View.VISIBLE)
        views.setTextViewText(
            R.id.widget_start,
            context.getString(if (started) R.string.widget_resume else R.string.widget_start),
        )
    }

    private fun showRest(context: Context, views: RemoteViews, timer: TimerState, now: Long, elapsed: Long) {
        views.setViewVisibility(R.id.widget_rest, if (timer is TimerState.Idle) View.GONE else View.VISIBLE)
        val label = TimerLogic.label(timer)
        views.setTextViewText(
            R.id.widget_rest_label,
            when {
                timer is TimerState.Finished -> context.getString(R.string.guided_rest_done)
                timer is TimerState.Paused -> context.getString(R.string.widget_rest_paused)
                label != null -> context.getString(R.string.widget_rest_after, label)
                else -> context.getString(R.string.guided_rest)
            },
        )
        // The countdown runs by itself: the widget is not updated every second.
        when (timer) {
            is TimerState.Running -> {
                views.setChronometer(R.id.widget_chrono, elapsed + (timer.endAt - now), null, true)
                views.setChronometerCountDown(R.id.widget_chrono, true)
            }
            // The time since the end of the rest.
            is TimerState.Finished -> {
                views.setChronometer(R.id.widget_chrono, elapsed - (now - timer.finishedAt), OVERTIME_FORMAT, true)
                views.setChronometerCountDown(R.id.widget_chrono, false)
            }
            else -> views.setChronometer(R.id.widget_chrono, elapsed, null, false)
        }
        // A paused rest shows its time left, which a stopped chronometer would not keep.
        val paused = timer is TimerState.Paused
        views.setViewVisibility(R.id.widget_chrono, if (paused) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_rest_time, if (paused) View.VISIBLE else View.GONE)
        if (timer is TimerState.Paused) views.setTextViewText(R.id.widget_rest_time, Fmt.clockMillis(timer.remaining))
    }

    private fun openIntent(context: Context, action: String?, requestCode: Int): PendingIntent {
        // Without an action, the app opens as from the home screen, where it was left.
        val intent = if (action == null) {
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } else {
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private const val OVERTIME_FORMAT = "+%s"
    private const val REQUEST_OPEN = 30
    private const val REQUEST_START = 31
}
