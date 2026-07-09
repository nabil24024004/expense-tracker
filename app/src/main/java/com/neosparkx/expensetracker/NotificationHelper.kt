package com.neosparkx.expensetracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationHelper {
    fun scheduleDailyReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        
        // 1. Morning Summary Alarm (8:30 AM)
        val intent1 = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "morning")
        }
        val pendingIntent1 = PendingIntent.getBroadcast(
            context,
            1001,
            intent1,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar1 = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now + 60 * 1000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        scheduleAlarm(alarmManager, calendar1.timeInMillis, pendingIntent1)

        // 2. Evening Summary Alarm (11:00 PM)
        val intent2 = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "evening")
        }
        val pendingIntent2 = PendingIntent.getBroadcast(
            context,
            1002,
            intent2,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar2 = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now + 60 * 1000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        scheduleAlarm(alarmManager, calendar2.timeInMillis, pendingIntent2)
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun triggerLiveNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        context.sendBroadcast(intent)
    }
}

