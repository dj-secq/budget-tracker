package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_templates",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
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
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateName: String,
    val amount: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val transactionType: CategoryType = CategoryType.EXPENSE,
    val classification: ExpenseClassification = ExpenseClassification.NONE
)
