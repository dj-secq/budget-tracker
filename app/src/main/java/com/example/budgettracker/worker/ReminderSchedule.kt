package com.example.budgettracker.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderSchedule {
    const val WORK_NAME = "daily_reminder_work"

    fun enqueue(context: Context, hour: Int, replace: Boolean) {
        val safeHour = if (hour == 8 || hour == 13 || hour == 20) hour else 20
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilHour(safeHour), TimeUnit.MILLISECONDS)
            .build()
        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }

    fun millisUntilHour(hour: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var due = now.toLocalDate().atTime(hour, 0).atZone(zone)
        if (!due.isAfter(now)) due = due.plusDays(1)
        return Duration.between(now.toInstant(), due.toInstant()).toMillis().coerceAtLeast(0L)
    }
}
