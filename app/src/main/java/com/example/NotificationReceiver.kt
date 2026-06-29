package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import com.example.data.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.scheduleDailyReminders(context)
            return
        }
        
        val type = intent.getStringExtra("type")
        if (type != null) {
            NotificationHelper.scheduleDailyReminders(context)
            val pendingResult = goAsync()
            val database = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = database.expenseDao()
                    val allExpenses = dao.getAllExpensesSync()
                    val (title, message) = generateNotificationContent(type, allExpenses)
                    
                    withContext(Dispatchers.Main) {
                        showNotification(context, title, message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            val title = intent.getStringExtra("title") ?: "Expense Tracker"
            val message = intent.getStringExtra("message") ?: "Don't forget to track your expenses today!"
            showNotification(context, title, message)
        }
    }

    private fun generateNotificationContent(type: String, expenses: List<Expense>): Pair<String, String> {
        val cal = Calendar.getInstance()
        
        // Today boundary
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = cal.timeInMillis
        val yesterdayEnd = todayStart - 1
        val todayEnd = todayStart + (24 * 60 * 60 * 1000) - 1

        return if (type == "morning") {
            val yesterdayExpenses = expenses.filter { it.date in yesterdayStart..yesterdayEnd }
            if (yesterdayExpenses.isEmpty()) {
                Pair(
                    "Yesterday's Summary",
                    "You didn't log any expenses yesterday. Keep tracking your budget!"
                )
            } else {
                val totalSpent = yesterdayExpenses.sumOf { it.amount }
                val categorySummary = yesterdayExpenses.groupBy { it.category.trim() }
                    .mapValues { entry -> entry.value.sumOf { entry2 -> entry2.amount } }
                    .entries
                    .joinToString(", ") { "${it.key}: ৳${String.format(java.util.Locale.US, "%.0f", it.value)}" }
                    
                val formattedTotal = String.format(java.util.Locale.US, "%,.2f", totalSpent)
                Pair(
                    "Yesterday's Expenses",
                    "Spent: ৳$formattedTotal ($categorySummary)"
                )
            }
        } else {
            val todayExpenses = expenses.filter { it.date in todayStart..todayEnd }
            val yesterdayExpenses = expenses.filter { it.date in yesterdayStart..yesterdayEnd }
            
            val todayTotal = todayExpenses.sumOf { it.amount }
            val yesterdayTotal = yesterdayExpenses.sumOf { it.amount }
            
            val diff = todayTotal - yesterdayTotal
            val formattedTodayTotal = String.format(java.util.Locale.US, "%,.2f", todayTotal)
            val formattedDiff = String.format(java.util.Locale.US, "%,.2f", Math.abs(diff))
            
            val diffMessage = if (diff < 0) {
                "Congratulations! You spent ৳$formattedDiff less than yesterday. Great job saving!"
            } else if (diff > 0) {
                "You spent ৳$formattedDiff more than yesterday. Please be careful with your expenses!"
            } else {
                "You spent the same amount as yesterday."
            }
            
            Pair(
                "Daily Expense Summary",
                "Today's spending: ৳$formattedTodayTotal. $diffMessage"
            )
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "expense_tracker_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminders & Alerts"
            val descriptionText = "Notifications for expense tracking reminders and insights"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
