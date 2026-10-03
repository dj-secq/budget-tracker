package com.example.budgettracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.budgettracker.domain.RepeatInterval

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    SEMI_MONTHLY;

    fun toInterval(): RepeatInterval = when (this) {
        DAILY -> RepeatInterval.DAILY
        WEEKLY -> RepeatInterval.WEEKLY
        MONTHLY -> RepeatInterval.MONTHLY
        YEARLY -> RepeatInterval.YEARLY
        SEMI_MONTHLY -> RepeatInterval.SEMI_MONTHLY
    }
}

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("accountId")]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val amount: Long,
    val note: String,
    val classification: ExpenseClassification = ExpenseClassification.NONE,
    val frequency: Frequency,
    val startDate: Long,
    val nextRunTime: Long,
    val anchorDay: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val paused: Boolean = false,
    val endDate: Long? = null
)
