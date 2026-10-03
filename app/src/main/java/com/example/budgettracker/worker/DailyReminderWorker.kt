package com.example.budgettracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgettracker.BudgetTrackerApplication
import com.example.budgettracker.domain.endOfLocalDay
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.reminderMessage
import com.example.budgettracker.domain.startOfLocalDay
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as BudgetTrackerApplication
        val preferencesRepository = app.container.userPreferencesRepository
        val prefs = preferencesRepository.generalPreferencesFlow.first()
        if (!prefs.dailyRemindersEnabled) {
            return Result.success()
        }

        val repository = app.container.budgetRepository
        val today = localDateOf(System.currentTimeMillis())
        val start = startOfLocalDay(today)
        val end = endOfLocalDay(today)
        val loggedToday = repository.countBetween(start, end) > 0
        val recurring = repository.recurringDueOn(start, end).map { rule ->
            rule.note.ifBlank { "A recurring item" }
        }
        val debts = repository.debtsDueOn(start, end).map { it.personName }
        val text = reminderMessage(loggedToday, recurring, debts) ?: return Result.success()
        showNotification("Budget Tracker Reminder", text)
        return Result.success()
    }

    private fun showNotification(title: String, text: String) {
        val channelId = "daily_reminder_channel"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Reminders"
            val descriptionText = "Reminders to log expenses, recurring posts, and debts due today"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
