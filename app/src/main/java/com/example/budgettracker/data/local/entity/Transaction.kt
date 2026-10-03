package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("timestamp"),
        Index(value = ["categoryId", "timestamp"]),
        Index(value = ["recurringId", "timestamp"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val amount: Long,
    val timestamp: Long,
    val note: String,
    val payeeOrPayer: String? = null,
    val classification: ExpenseClassification = ExpenseClassification.NONE,
    val recurringId: Long? = null,
    val goalId: Long? = null,
    val splitGroupId: String? = null
)
