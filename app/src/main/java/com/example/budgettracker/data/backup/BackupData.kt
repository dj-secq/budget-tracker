package com.example.budgettracker.data.backup

import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.TransactionTemplate

data class BackupPreferences(
    val needsPercent: Int = 50,
    val wantsPercent: Int = 30,
    val savingsPercent: Int = 20,
    val themeMode: String = "SYSTEM",
    val dailyRemindersEnabled: Boolean = false,
    val rolloverBudgetsEnabled: Boolean = false,
    val strictLimitsEnabled: Boolean = false,
    val lastAccountId: Long? = null,
    val accent: String = "EMERALD",
    val dynamicColor: Boolean = false,
    val reminderHour: Int = 20
)

data class BackupData(
    val version: Int = BackupCodec.CURRENT_VERSION,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgetLimits: List<BudgetLimit> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val templates: List<TransactionTemplate> = emptyList(),
    val preferences: BackupPreferences? = null
)

data class DecodedBackup(
    val data: BackupData,
    val legacyPesos: Boolean
)
