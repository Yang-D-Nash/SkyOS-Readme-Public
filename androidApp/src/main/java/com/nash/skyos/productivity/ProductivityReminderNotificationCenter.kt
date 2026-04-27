package com.nash.skyos.productivity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nash.skyos.MainActivity
import com.nash.skyos.R
import java.util.Date

private const val reminderTitlePrefs = "productivity_reminder_titles"
private fun reminderTitleKey(id: String) = "t_$id"

/**
 * Fires a local notification when a [users/{uid}/reminders] item is due.
 */
object ProductivityReminderNotificationCenter {
    const val ACTION_REMINDER = "com.nash.skyos.REMINDER_FIRE"
    const val CHANNEL_ID = "productivity_reminders"
    private const val REQ_BASE = 80_200

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel_productivity_name))
            .setDescription(context.getString(R.string.notification_channel_productivity_description))
            .setShowBadge(true)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun buildPendingIntent(context: Context, reminderId: String): PendingIntent {
        val app = context.applicationContext
        val actionIntent = Intent(app, ReminderAlarmReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .setData(Uri.parse("reminder://skyos/$reminderId"))
        return PendingIntent.getBroadcast(
            app,
            requestCodeFor(reminderId),
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(
        context: Context,
        reminderId: String,
        title: String,
        dueAt: Date,
    ) {
        createChannelIfNeeded(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val at = dueAt.time
        if (at <= System.currentTimeMillis() + 2_000) return
        val app = context.applicationContext
        app.getSharedPreferences(reminderTitlePrefs, Context.MODE_PRIVATE).edit()
            .putString(reminderTitleKey(reminderId), title)
            .apply()
        val pi = buildPendingIntent(context, reminderId)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setAlarmClock(
                        AlarmManager.AlarmClockInfo(at, activityLaunchPendingIntent(context)),
                        pi,
                    )
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            } else {
                @Suppress("DEPRECATION")
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            try {
                @Suppress("DEPRECATION")
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (e2: Exception) {
                Log.w("Reminders", "Could not schedule alarm: ${e2.message}")
            }
        } catch (e: Exception) {
            Log.w("Reminders", "Could not schedule alarm: ${e.message}")
        }
    }

    private fun activityLaunchPendingIntent(context: Context): PendingIntent {
        val app = context.applicationContext
        val i = Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("com.nash.skyos://reminders"))
        return PendingIntent.getActivity(
            app,
            1,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun cancel(context: Context, reminderId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val app = context.applicationContext
        val pi = buildPendingIntent(context, reminderId)
        am.cancel(pi)
        app.getSharedPreferences(reminderTitlePrefs, Context.MODE_PRIVATE).edit()
            .remove(reminderTitleKey(reminderId))
            .apply()
    }

    private fun requestCodeFor(reminderId: String): Int =
        REQ_BASE + (reminderId.hashCode() and 0x7fff)

    fun showNotification(
        context: Context,
        reminderId: String,
        title: String,
    ) {
        createChannelIfNeeded(context)
        val app = context.applicationContext
        val open = Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("com.nash.skyos://reminders"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPi = PendingIntent.getActivity(
            app,
            reminderId.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(app.getString(R.string.notification_productivity_reminder_title))
            .setContentText(title)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(app)
            .notify("reminder_$reminderId".hashCode(), n)
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ProductivityReminderNotificationCenter.ACTION_REMINDER) return
        val id = intent.data?.lastPathSegment ?: return
        val sp = context.getSharedPreferences(reminderTitlePrefs, Context.MODE_PRIVATE)
        val title = sp.getString(reminderTitleKey(id), null)?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        ProductivityReminderNotificationCenter.showNotification(context, id, title)
        sp.edit().remove(reminderTitleKey(id)).apply()
    }
}
