package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val iconName: String? = null,
    val targetDate: Long? = null,
    val contributionFrequency: String? = null,
    val contributionAmount: Double? = null
)
