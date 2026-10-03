package com.example.budgettracker.worker

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgettracker.BudgetTrackerApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as BudgetTrackerApplication
            app.container.budgetRepository.postDueRecurring(System.currentTimeMillis())
            Result.success()
        } catch (_: SQLiteConstraintException) {
            Result.failure()
        } catch (_: Exception) {
            if (runAttemptCount >= 2) Result.failure() else Result.retry()
        }
    }
}
