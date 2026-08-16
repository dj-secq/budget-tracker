package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgettracker.data.local.entity.TransactionTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Query("SELECT * FROM transaction_templates ORDER BY templateName ASC")
    fun getAllTemplates(): Flow<List<TransactionTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TransactionTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplate)
}
