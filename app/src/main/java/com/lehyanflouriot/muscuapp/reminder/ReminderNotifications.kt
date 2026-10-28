package com.lehyanflouriot.muscuapp.reminder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.lehyanflouriot.muscuapp.MainActivity
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.SessionView

/** The notifications of the daily reminders. */
class ReminderNotifications(private val context: Context) {
    private val manager: NotificationManager = checkNotNull(context.getSystemService(NotificationManager::class.java))

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminders_description) }
        manager.createNotificationChannel(channel)
    }

    fun showSession(session: SessionView) {
        val title = session.day.title.ifBlank { context.getString(R.string.session_untitled) }
        val left = session.items.count { !it.done }
        val text = if (left == session.items.size) {
            context.resources.getQuantityString(R.plurals.reminder_session_text, left, left)
        } else {
            context.resources.getQuantityString(R.plurals.reminder_session_left, left, left)
        }
        val open = openIntent(MainActivity.ACTION_OPEN_SESSION, REQUEST_SESSION)
        val notification = builder()
            .setContentTitle(context.getString(R.string.reminder_session_title, title))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notif_play),
                    context.getString(R.string.guided_start),
                    open,
                ).build(),
            )
            .build()
        post(ID_SESSION, notification)
    }

    fun showWeighIn() {
        val notification = builder()
            .setContentTitle(context.getString(R.string.reminder_weigh_in_title))
            .setContentText(context.getString(R.string.reminder_weigh_in_text))
            .setContentIntent(openIntent(MainActivity.ACTION_OPEN_WEIGHT, REQUEST_WEIGHT))
            .build()
        post(ID_WEIGH_IN, notification)
    }

    private fun builder(): Notification.Builder =
        Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(context.getColor(R.color.brand))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)

    private fun openIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // Reminders are optional: without the permission, they simply do not show.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: Notification) {
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    companion object {
        const val CHANNEL = "reminders"
        private const val ID_SESSION = 10
        private const val ID_WEIGH_IN = 11
        private const val REQUEST_SESSION = 10
        private const val REQUEST_WEIGHT = 11
    }
}
