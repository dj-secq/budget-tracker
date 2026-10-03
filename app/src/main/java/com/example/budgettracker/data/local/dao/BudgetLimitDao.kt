package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.BudgetLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits")
    fun getAllBudgetLimits(): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits")
    suspend fun listAll(): List<BudgetLimit>

    @Query("SELECT * FROM budget_limits WHERE month = :month AND year = :year")
    fun getBudgetLimitsForMonth(month: Int, year: Int): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits WHERE categoryId = :categoryId AND month = :month AND year = :year")
    suspend fun getBudgetLimit(categoryId: Long, month: Int, year: Int): BudgetLimit?

    @Query("SELECT * FROM budget_limits WHERE month = :month AND year = :year")
    suspend fun getBudgetLimitsForMonthOnce(month: Int, year: Int): List<BudgetLimit>

    @Query("SELECT * FROM budget_limits WHERE (year < :year) OR (year = :year AND month < :month)")
    suspend fun getBudgetLimitsBefore(month: Int, year: Int): List<BudgetLimit>

    @Query("DELETE FROM budget_limits")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetLimit(budgetLimit: BudgetLimit): Long

    @Update
    suspend fun updateBudgetLimit(budgetLimit: BudgetLimit)

    @Delete
    suspend fun deleteBudgetLimit(budgetLimit: BudgetLimit)
}
