package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    LENT, BORROWED
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val type: DebtType,
    val date: Long,
    val isPaid: Boolean = false,
    val note: String = "",
    val dueDate: Long? = null,
    val interestRate: Double = 0.0
)
