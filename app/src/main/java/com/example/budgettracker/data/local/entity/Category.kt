package com.example.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgettracker.domain.CategoryKind
import com.example.budgettracker.domain.CategoryUse
import com.example.budgettracker.domain.countsAsExpense as expenseKind
import com.example.budgettracker.domain.countsAsIncome as incomeKind

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val colorArgb: Int,
    val iconName: String? = null,
    val role: CategoryRole = CategoryRole.NORMAL
)

enum class CategoryType {
    INCOME, EXPENSE;

    fun toKind(): CategoryKind = if (this == INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
}

enum class CategoryRole {
    NORMAL, TRANSFER_IN, TRANSFER_OUT;

    fun toUse(): CategoryUse = when (this) {
        NORMAL -> CategoryUse.NORMAL
        TRANSFER_IN -> CategoryUse.TRANSFER_IN
        TRANSFER_OUT -> CategoryUse.TRANSFER_OUT
    }
}

enum class ExpenseClassification {
    NEED, WANT, SAVING, NONE
}

fun Category.countsAsIncome(): Boolean = incomeKind(type.toKind(), role.toUse())

fun Category.countsAsExpense(): Boolean = expenseKind(type.toKind(), role.toUse())
