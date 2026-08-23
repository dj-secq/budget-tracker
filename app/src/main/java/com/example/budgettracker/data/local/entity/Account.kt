package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CHECKING, SAVINGS, CREDIT
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val colorArgb: Int,
    val includeInTotalBalance: Boolean = true
)
