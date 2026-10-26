package com.lehyanflouriot.muscuapp.timer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.DrawableRes
import com.lehyanflouriot.muscuapp.MainActivity
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.timer.TimerState

/** Notifications of the rest timer: a silent countdown, then an alert at the end. */
class RestNotifications(private val context: Context) {
    private val manager: NotificationManager = checkNotNull(context.getSystemService(NotificationManager::class.java))

    fun createChannels() {
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.channel_running_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_running_description)
            setShowBadge(false)
        }
        val done = NotificationChannel(
            CHANNEL_DONE,
            context.getString(R.string.channel_done_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_done_description)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), ALERT_ATTRIBUTES)
        }
        manager.createNotificationChannels(listOf(running, done))
    }

    fun showRunning(state: TimerState.Running) {
        val notification = builder(CHANNEL_RUNNING)
            .setContentTitle(context.getString(R.string.notif_running_title))
            .setContentText(state.label ?: context.getString(R.string.notif_running_text))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(state.endAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(action(R.drawable.ic_notif_add, context.getString(R.string.notif_action_add), RestTimerReceiver.ACTION_ADD))
            .addAction(action(R.drawable.ic_notif_pause, context.getString(R.string.notif_action_pause), RestTimerReceiver.ACTION_PAUSE))
            .addAction(action(R.drawable.ic_notif_stop, context.getString(R.string.notif_action_stop), RestTimerReceiver.ACTION_STOP))
            .build()
        manager.cancel(ID_DONE)
        post(ID_ONGOING, notification)
    }

    fun showPaused(state: TimerState.Paused) {
        val notification = builder(CHANNEL_RUNNING)
            .setContentTitle(context.getString(R.string.notif_paused_title))
            .setContentText(context.getString(R.string.notif_paused_text, Fmt.clockMillis(state.remaining)))
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(action(R.drawable.ic_notif_play, context.getString(R.string.notif_action_resume), RestTimerReceiver.ACTION_RESUME))
            .addAction(action(R.drawable.ic_notif_stop, context.getString(R.string.notif_action_stop), RestTimerReceiver.ACTION_STOP))
            .build()
        post(ID_ONGOING, notification)
    }

    fun showFinished(state: TimerState.Finished) {
        val text = state.label?.let { context.getString(R.string.notif_done_text_label, it) }
            ?: context.getString(R.string.notif_done_text)
        val restart = context.getString(R.string.notif_action_restart, Fmt.clockMillis(state.total))
        val notification = builder(CHANNEL_DONE)
            .setContentTitle(context.getString(R.string.notif_done_title))
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setWhen(state.finishedAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setTimeoutAfter(DONE_TIMEOUT_MS)
            .addAction(action(R.drawable.ic_notif_replay, restart, RestTimerReceiver.ACTION_RESTART))
            .build()
        if (canAlert()) post(ID_DONE, notification) else alertWithoutNotification()
    }

    /** Whether the end-of-rest notification can ring (notifications allowed and channel not muted). */
    private fun canAlert(): Boolean =
        manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_DONE)?.importance != NotificationManager.IMPORTANCE_NONE

    /** When notifications are refused, still vibrate and ring so that the end of the rest is noticed. */
    private fun alertWithoutNotification() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        // Without a notification usage, Android drops the vibrations of apps in the background.
        @Suppress("DEPRECATION")
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1), ALERT_ATTRIBUTES) }
        runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                ?.apply { audioAttributes = ALERT_ATTRIBUTES }
                ?.play()
        }
    }

    fun cancelOngoing() = manager.cancel(ID_ONGOING)

    fun cancelAll() {
        manager.cancel(ID_ONGOING)
        manager.cancel(ID_DONE)
    }

    private fun builder(channel: String): Notification.Builder =
        Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_rest)
            .setColor(context.getColor(R.color.brand))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openRestIntent())

    private fun action(@DrawableRes icon: Int, title: String, receiverAction: String): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(context, icon),
            title,
            RestTimerReceiver.pendingIntent(context, receiverAction),
        ).build()

    private fun openRestIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_REST)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // Notifications are optional: when the user refused them the timer still works in the app.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: Notification) {
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_RUNNING = "rest_running"
        const val CHANNEL_DONE = "rest_done"
        private const val ID_ONGOING = 1
        private const val ID_DONE = 2
        private const val DONE_TIMEOUT_MS = 2 * 60_000L
        private val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400, 200, 700)
        private val ALERT_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
