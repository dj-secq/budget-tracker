package com.example.budgettracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.TransactionTemplate
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar


class AddTransactionViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<TransactionTemplate>> = repository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSaving = MutableStateFlow(false)
    val showOverBudgetWarning = MutableStateFlow<Pair<Double, Boolean>?>(null) // category limit exceeded by, isStrict
    val showBucketWarning = MutableStateFlow<Pair<String, Double>?>(null) // bucket name and amount exceeded by

    fun onConfirmSave(
        accountId: Long,
        categoryId: Long,
        amount: Double,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        isRecurring: Boolean = false,
        recurringFrequency: Frequency? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            
            // 1. Check Category Limit
            val limits = repository.getBudgetLimitsForMonth(month, year).first()
            var limit = limits.find { it.categoryId == categoryId }?.assignedAmount ?: 0.0
            val prefs = preferencesRepository.generalPreferencesFlow.first()
            
            if (limit > 0) {
                if (prefs.rolloverBudgetsEnabled) {
                    limit += repository.getRolloverAmount(categoryId, month, year)
                }
                val spent = repository.getTotalSpentByCategory(categoryId, month, year).first() ?: 0.0
                if (spent + amount > limit) {
                    showOverBudgetWarning.value = Pair((spent + amount) - limit, prefs.strictLimitsEnabled)
                    return@launch
                }
            }

            // 2. Check Bucket Limit (Needs/Wants/Savings)
            if (classification != ExpenseClassification.NONE) {
                val transactions = repository.getTransactionsForMonth(month, year).first()
                val categories = repository.getAllCategories().first()
                val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
                val totalIncome = transactions.filter { tx -> incomeCategories.any { it.id == tx.categoryId } }.sumOf { it.amount }
                
                if (totalIncome > 0) {
                    val rule = preferencesRepository.budgetRulePreferencesFlow.first()
                    val bucketPercent = when (classification) {
                        ExpenseClassification.NEED -> rule.needsPercent
                        ExpenseClassification.WANT -> rule.wantsPercent
                        ExpenseClassification.SAVING -> rule.savingsPercent
                        else -> 0
                    }
                    val bucketLimit = totalIncome * (bucketPercent / 100.0)
                    val bucketSpent = transactions.filter { it.classification == classification }.sumOf { it.amount }
                    
                    if (bucketSpent + amount > bucketLimit) {
                        showBucketWarning.value = Pair(classification.name, (bucketSpent + amount) - bucketLimit)
                        return@launch
                    }
                }
            }
            
            saveTransaction(accountId, categoryId, amount, note, timestamp, classification, isRecurring, recurringFrequency, onComplete)
        }
    }

    fun saveTransaction(
        accountId: Long,
        categoryId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        classification: ExpenseClassification = ExpenseClassification.NONE,
        isRecurring: Boolean = false,
        recurringFrequency: Frequency? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            isSaving.value = true
            val transaction = Transaction(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                note = note,
                timestamp = timestamp,
                classification = classification
            )
            repository.insertTransaction(transaction)
            
            if (isRecurring && recurringFrequency != null) {
                // Calculate next run time
                val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                when (recurringFrequency) {
                    Frequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    Frequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    Frequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                    Frequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
                }
                
                val recurring = RecurringTransaction(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    note = note,
                    classification = classification,
                    frequency = recurringFrequency,
                    startDate = timestamp,
                    nextRunTime = calendar.timeInMillis
                )
                repository.insertRecurringTransaction(recurring)
            }
            
            isSaving.value = false
            onComplete()
        }
    }

    fun saveTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            isSaving.value = true
            
            val allCategories = repository.getAllCategories().first()
            var transferOutCat = allCategories.find { it.name == "Withdraw / Transfer Out" && it.type == CategoryType.EXPENSE }
            if (transferOutCat == null) {
                val id = repository.insertCategory(Category(name = "Withdraw / Transfer Out", type = CategoryType.EXPENSE, colorArgb = 0xFFF44336.toInt()))
                transferOutCat = Category(id = id, name = "Withdraw / Transfer Out", type = CategoryType.EXPENSE, colorArgb = 0xFFF44336.toInt())
            }
            
            var transferInCat = allCategories.find { it.name == "Deposit / Transfer In" && it.type == CategoryType.INCOME }
            if (transferInCat == null) {
                val id = repository.insertCategory(Category(name = "Deposit / Transfer In", type = CategoryType.INCOME, colorArgb = 0xFF4CAF50.toInt()))
                transferInCat = Category(id = id, name = "Deposit / Transfer In", type = CategoryType.INCOME, colorArgb = 0xFF4CAF50.toInt())
            }
            
            val txOut = Transaction(
                accountId = fromAccountId,
                categoryId = transferOutCat.id,
                amount = amount,
                note = if (note.isBlank()) "Transfer to another wallet" else note,
                timestamp = timestamp,
                classification = ExpenseClassification.NONE
            )
            
            val txIn = Transaction(
                accountId = toAccountId,
                categoryId = transferInCat.id,
                amount = amount,
                note = if (note.isBlank()) "Transfer from another wallet" else note,
                timestamp = timestamp,
                classification = ExpenseClassification.NONE
            )
            
            repository.insertTransfer(txOut, txIn)
            isSaving.value = false
            onComplete()
        }
    }

    fun dismissWarnings() {
        showOverBudgetWarning.value = null
        showBucketWarning.value = null
    }

    fun dismissWarning() {
        dismissWarnings()
    }

    fun saveTemplate(
        templateName: String,
        amount: Double,
        categoryId: Long,
        accountId: Long,
        note: String,
        transactionType: CategoryType,
        classification: ExpenseClassification
    ) {
        viewModelScope.launch {
            val template = TransactionTemplate(
                templateName = templateName,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionType = transactionType,
                classification = classification
            )
            repository.insertTemplate(template)
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }
}
