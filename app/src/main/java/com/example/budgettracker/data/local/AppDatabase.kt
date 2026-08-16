package com.example.budgettracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BudgetLimitDao
import com.example.budgettracker.data.local.dao.CategoryDao
import com.example.budgettracker.data.local.dao.SavingsGoalDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.dao.RecurringTransactionDao
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.dao.DebtDao
import com.example.budgettracker.data.local.entity.TransactionTemplate
import com.example.budgettracker.data.local.dao.TransactionTemplateDao

@Database(
    entities = [
        Account::class,
        Category::class,
        BudgetLimit::class,
        Transaction::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Debt::class,
        TransactionTemplate::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
}
