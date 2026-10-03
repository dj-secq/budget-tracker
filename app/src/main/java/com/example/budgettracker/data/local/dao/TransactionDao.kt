package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

data class CategoryMonthTotal(
    val categoryId: Long,
    val year: Int,
    val month: Int,
    val total: Long
)

data class GoalTotal(val goalId: Long, val total: Long)

data class CategoryClassificationTotal(
    val categoryId: Long,
    val classification: String,
    val total: Long
)

data class MonthCategoryTotal(
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val total: Long
)

data class TransactionAmount(
    val id: Long,
    val amount: Long
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:start IS NULL OR timestamp >= :start)
          AND (:end IS NULL OR timestamp <= :end)
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun ledger(start: Long?, end: Long?, limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT id, amount FROM transactions WHERE id IN (:ids)")
    fun observeAmounts(ids: List<Long>): Flow<List<TransactionAmount>>

    @Query("SELECT * FROM transactions")
    suspend fun listAll(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun listBetween(startDate: Long, endDate: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getTotalAmountByCategoryAndDateRange(categoryId: Long, startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE categoryId = :categoryId AND timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun sumAmountByCategoryAndDateRange(categoryId: Long, startDate: Long, endDate: Long): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :start AND timestamp <= :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query("SELECT MIN(timestamp) FROM transactions")
    suspend fun earliestTimestamp(): Long?

    @Query(
        """
        SELECT categoryId AS categoryId, classification AS classification, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY categoryId, classification
        """
    )
    suspend fun totalsByCategory(start: Long, end: Long): List<CategoryClassificationTotal>

    @Query(
        """
        SELECT categoryId AS categoryId, classification AS classification, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY categoryId, classification
        """
    )
    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategoryClassificationTotal>>

    @Query(
        """
        SELECT CAST(strftime('%Y', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
               CAST(strftime('%m', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month,
               categoryId AS categoryId,
               SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY year, month, categoryId
        """
    )
    suspend fun totalsByMonthCategory(start: Long, end: Long): List<MonthCategoryTotal>

    @Query(
        """
        SELECT CAST(strftime('%Y', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
               CAST(strftime('%m', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month,
               categoryId AS categoryId,
               SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY year, month, categoryId
        """
    )
    fun observeTotalsByMonthCategory(start: Long, end: Long): Flow<List<MonthCategoryTotal>>

    @Query(
        """
        SELECT goalId AS goalId, SUM(amount) AS total
        FROM transactions
        WHERE goalId IS NOT NULL
        GROUP BY goalId
        """
    )
    fun goalTotals(): Flow<List<GoalTotal>>

    @Query(
        """
        SELECT goalId AS goalId, SUM(amount) AS total
        FROM transactions
        WHERE goalId IS NOT NULL
        GROUP BY goalId
        """
    )
    suspend fun goalTotalsList(): List<GoalTotal>

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun latestForGoal(goalId: Long): Transaction?

    @Query(
        """
        SELECT t.* FROM transactions t
        JOIN categories c ON c.id = t.categoryId
        WHERE c.role = 'TRANSFER_OUT'
          AND t.amount = :amount
          AND t.timestamp = :timestamp
          AND t.note = :note
          AND t.goalId IS NULL
        ORDER BY t.id DESC
        LIMIT 1
        """
    )
    suspend fun findTransferOut(amount: Long, timestamp: Long, note: String): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE goalId = :goalId")
    suspend fun countByGoal(goalId: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0)
        FROM transactions t
        JOIN categories c ON c.id = t.categoryId
        WHERE t.accountId = :accountId
        """
    )
    suspend fun netForAccount(accountId: Long): Long

    @Query(
        """
        SELECT categoryId,
               CAST(strftime('%Y', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
               CAST(strftime('%m', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month,
               SUM(amount) AS total
        FROM transactions
        WHERE timestamp < :beforeMillis
        GROUP BY categoryId, year, month
        """
    )
    suspend fun spentByCategoryMonthBefore(beforeMillis: Long): List<CategoryMonthTotal>

    @Query("SELECT id FROM transactions WHERE recurringId = :recurringId AND timestamp = :timestamp LIMIT 1")
    suspend fun findRecurringOccurrenceId(recurringId: Long, timestamp: Long): Long?

    @Query("SELECT timestamp FROM transactions WHERE recurringId = :recurringId")
    suspend fun timestampsForRecurring(recurringId: Long): List<Long>

    @Query("SELECT * FROM transactions WHERE splitGroupId = :groupId")
    suspend fun listBySplitGroup(groupId: String): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionIgnoringConflict(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
