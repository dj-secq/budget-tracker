package com.example.budgettracker

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budgettracker.di.AppContainer
import com.example.budgettracker.di.DefaultAppContainer
import com.example.budgettracker.worker.RecurringTransactionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BudgetTrackerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        
        // Schedule Recurring Transactions Worker
        val constraints = Constraints.Builder()
            // .setRequiresBatteryNotLow(true) // Don't block entirely for demo purposes
            .build()
            
        val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_transactions_work",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWorkRequest
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            val hour = container.userPreferencesRepository.reminderHour()
            com.example.budgettracker.worker.ReminderSchedule.enqueue(this@BudgetTrackerApplication, hour, replace = false)
            val repository = container.budgetRepository
            try {
                com.example.budgettracker.data.seedDatabaseIfEmpty(repository, container.userPreferencesRepository)
            } finally {
                repository.postDueRecurring(System.currentTimeMillis())
                val today = java.time.LocalDate.now()
                repository.copyBudgetsForwardIfEmpty(today.monthValue, today.year)
            }
        }
    }
}
