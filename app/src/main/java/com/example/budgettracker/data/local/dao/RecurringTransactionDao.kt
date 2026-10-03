package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun listAll(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransaction?

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE paused = 0 AND nextRunTime <= :currentTime
          AND (endDate IS NULL OR nextRunTime <= endDate)
        """
    )
    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransaction>

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE paused = 0
          AND (
            (nextRunTime >= :start AND nextRunTime <= :end AND (endDate IS NULL OR nextRunTime <= endDate))
            OR id IN (
              SELECT recurringId FROM transactions
              WHERE recurringId IS NOT NULL AND timestamp >= :start AND timestamp <= :end
            )
          )
        """
    )
    suspend fun dueOnDay(start: Long, end: Long): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long

    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
