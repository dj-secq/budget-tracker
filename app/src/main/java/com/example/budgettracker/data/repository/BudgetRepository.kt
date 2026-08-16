package com.example.budgettracker.data.repository

import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BudgetLimitDao
import com.example.budgettracker.data.local.dao.CategoryDao
import com.example.budgettracker.data.local.dao.RecurringTransactionDao
import com.example.budgettracker.data.local.dao.SavingsGoalDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.BackupData
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import androidx.room.withTransaction
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.dao.DebtDao
import com.example.budgettracker.data.local.entity.TransactionTemplate
import com.example.budgettracker.data.local.dao.TransactionTemplateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class BudgetRepository(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetLimitDao: BudgetLimitDao,
    private val transactionDao: TransactionDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val debtDao: DebtDao,
    private val transactionTemplateDao: TransactionTemplateDao
) {
    // Accounts
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)
    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)

    // Transaction Templates
    fun getAllTemplates(): Flow<List<TransactionTemplate>> = transactionTemplateDao.getAllTemplates()
    suspend fun insertTemplate(template: TransactionTemplate): Long = transactionTemplateDao.insertTemplate(template)
    suspend fun deleteTemplate(template: TransactionTemplate) = transactionTemplateDao.deleteTemplate(template)

    // Categories
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = categoryDao.getCategoriesByType(type)
    
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Budget Limits
    fun getBudgetLimitsForMonth(month: Int, year: Int): Flow<List<BudgetLimit>> =
        budgetLimitDao.getBudgetLimitsForMonth(month, year)

    suspend fun setBudgetLimit(categoryId: Long, amount: Double, month: Int, year: Int) {
        val existing = budgetLimitDao.getBudgetLimit(categoryId, month, year)
        if (existing != null) {
            budgetLimitDao.updateBudgetLimit(existing.copy(assignedAmount = amount))
        } else {
            budgetLimitDao.insertBudgetLimit(
                BudgetLimit(
                    categoryId = categoryId,
                    assignedAmount = amount,
                    month = month,
                    year = year
                )
            )
        }
    }

    suspend fun getRolloverAmount(categoryId: Long, currentMonth: Int, currentYear: Int): Double {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfCurrentMonth = calendar.timeInMillis

        val totalAssignedBefore = budgetLimitDao.getTotalAssignedBeforeMonth(categoryId, currentMonth, currentYear) ?: 0.0
        val totalSpentBefore = transactionDao.getTotalSpentBeforeDate(categoryId, startOfCurrentMonth) ?: 0.0

        val rollover = totalAssignedBefore - totalSpentBefore
        return if (rollover > 0) rollover else 0.0 // Ensure we don't return negative rollover for simplicity
    }

    // Transactions
    fun getRecentTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    
    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    suspend fun hasTransactionsForCategory(categoryId: Long): Boolean {
        val transactions = transactionDao.getTransactionsByCategory(categoryId).first()
        return transactions.isNotEmpty()
    }
    
    fun getTotalSpentByCategory(categoryId: Long, month: Int, year: Int): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar months are 0-indexed
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis
        
        return transactionDao.getTotalAmountByCategoryAndDateRange(categoryId, startDate, endDate)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val category = categoryDao.getCategoryById(transaction.categoryId)
        val account = accountDao.getAccountById(transaction.accountId)
        if (category != null && account != null) {
            val updatedBalance = if (category.type == CategoryType.INCOME) {
                account.balance + transaction.amount
            } else {
                account.balance - transaction.amount
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))
        }
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransfer(expenseTransaction: Transaction, incomeTransaction: Transaction) {
        database.withTransaction {
            insertTransaction(expenseTransaction)
            insertTransaction(incomeTransaction)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        val category = categoryDao.getCategoryById(transaction.categoryId)
        val account = accountDao.getAccountById(transaction.accountId)
        if (category != null && account != null) {
            val updatedBalance = if (category.type == CategoryType.INCOME) {
                account.balance - transaction.amount
            } else {
                account.balance + transaction.amount
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))
        }
        transactionDao.deleteTransaction(transaction)
    }
    
    suspend fun updateTransaction(newTransaction: Transaction) {
        database.withTransaction {
            val oldTransaction = transactionDao.getTransactionById(newTransaction.id)
            if (oldTransaction != null) {
                // Reverse the old transaction
                val oldCategory = categoryDao.getCategoryById(oldTransaction.categoryId)
                val oldAccount = accountDao.getAccountById(oldTransaction.accountId)
                if (oldCategory != null && oldAccount != null) {
                    val reversedBalance = if (oldCategory.type == CategoryType.INCOME) {
                        oldAccount.balance - oldTransaction.amount
                    } else {
                        oldAccount.balance + oldTransaction.amount
                    }
                    accountDao.updateAccount(oldAccount.copy(balance = reversedBalance))
                }

                // Apply the new transaction
                val newCategory = categoryDao.getCategoryById(newTransaction.categoryId)
                // Refetch the account because it might have been updated by the reversal!
                val newAccount = accountDao.getAccountById(newTransaction.accountId)
                if (newCategory != null && newAccount != null) {
                    val updatedBalance = if (newCategory.type == CategoryType.INCOME) {
                        newAccount.balance + newTransaction.amount
                    } else {
                        newAccount.balance - newTransaction.amount
                    }
                    accountDao.updateAccount(newAccount.copy(balance = updatedBalance))
                }
                
                transactionDao.updateTransaction(newTransaction)
            }
        }
    }
    
    // Savings Goals
    fun getAllGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()
    suspend fun insertGoal(goal: SavingsGoal): Long = savingsGoalDao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingsGoal) = savingsGoalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoal) = savingsGoalDao.deleteGoal(goal)

    // Recurring Transactions
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = recurringTransactionDao.getAllRecurringTransactions()
    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransaction> = recurringTransactionDao.getDueRecurringTransactions(currentTime)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long = recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) = recurringTransactionDao.updateRecurringTransaction(recurringTransaction)
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) = recurringTransactionDao.deleteRecurringTransaction(recurringTransaction)

    // Debts
    fun getAllDebts(): Flow<List<Debt>> = debtDao.getAllDebts()
    suspend fun insertDebt(debt: Debt): Long = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.deleteDebt(debt)

    // Export & Import
    suspend fun exportData(): BackupData {
        return BackupData(
            accounts = accountDao.getAllAccounts().first(),
            categories = categoryDao.getAllCategories().first(),
            transactions = transactionDao.getAllTransactions().first(),
            budgetLimits = budgetLimitDao.getAllBudgetLimits().first(),
            savingsGoals = savingsGoalDao.getAllGoals().first(),
            recurringTransactions = recurringTransactionDao.getAllRecurringTransactions().first()
        )
    }

    suspend fun restoreBackup(backupData: BackupData) {
        database.withTransaction {
            database.clearAllTables()
            
            // Insert in order of dependencies
            for (account in backupData.accounts) accountDao.insertAccount(account)
            for (category in backupData.categories) categoryDao.insertCategory(category)
            for (limit in backupData.budgetLimits) budgetLimitDao.insertBudgetLimit(limit)
            for (goal in backupData.savingsGoals) savingsGoalDao.insertGoal(goal)
            for (transaction in backupData.transactions) transactionDao.insertTransaction(transaction)
            for (recurring in backupData.recurringTransactions) recurringTransactionDao.insertRecurringTransaction(recurring)
        }
    }
}
