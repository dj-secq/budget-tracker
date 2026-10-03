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
import com.example.budgettracker.domain.advanceOccurrence
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.data.local.entity.countsAsExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val lastAccountId: StateFlow<Long?> = preferencesRepository.lastAccountIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSaving = MutableStateFlow(false)
    val showOverBudgetWarning = MutableStateFlow<Pair<Long, Boolean>?>(null)
    val showBucketWarning = MutableStateFlow<Triple<String, Long, Boolean>?>(null)

    fun onConfirmSave(
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        isRecurring: Boolean = false,
        recurringFrequency: Frequency? = null,
        onComplete: () -> Unit
    ) {
        if (isSaving.value || amount <= 0L) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                val prefs = preferencesRepository.generalPreferencesFlow.first()
                val rule = preferencesRepository.budgetRulePreferencesFlow.first()
                val evaluation = repository.evaluateSpend(
                    categoryId = categoryId,
                    amount = amount,
                    timestamp = timestamp,
                    classification = classification,
                    strict = prefs.strictLimitsEnabled,
                    rolloverEnabled = prefs.rolloverBudgetsEnabled,
                    needsPercent = rule.needsPercent,
                    wantsPercent = rule.wantsPercent,
                    savingsPercent = rule.savingsPercent
                )
                if (evaluation.categoryExceeded) {
                    showOverBudgetWarning.value = evaluation.categoryExcess to prefs.strictLimitsEnabled
                    return@launch
                }
                if (evaluation.bucketExceeded) {
                    showBucketWarning.value = Triple(
                        evaluation.bucketName ?: "Budget",
                        evaluation.bucketExcess,
                        prefs.strictLimitsEnabled
                    )
                    return@launch
                }
                persist(accountId, categoryId, amount, note, timestamp, classification, isRecurring, recurringFrequency)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun saveTransaction(
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        classification: ExpenseClassification = ExpenseClassification.NONE,
        isRecurring: Boolean = false,
        recurringFrequency: Frequency? = null,
        onComplete: () -> Unit
    ) {
        if (isSaving.value || amount <= 0L) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                persist(accountId, categoryId, amount, note, timestamp, classification, isRecurring, recurringFrequency)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    private suspend fun persist(
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        isRecurring: Boolean,
        recurringFrequency: Frequency?
    ) {
        val recurring = if (isRecurring && recurringFrequency != null) {
            val anchor = localDateOf(timestamp).dayOfMonth
            RecurringTransaction(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                note = note,
                classification = classification,
                frequency = recurringFrequency,
                startDate = timestamp,
                nextRunTime = advanceOccurrence(timestamp, recurringFrequency.toInterval(), anchor),
                anchorDay = anchor
            )
        } else {
            null
        }
        repository.saveTransaction(
            Transaction(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                note = note,
                timestamp = timestamp,
                classification = classification
            ),
            recurring
        )
        preferencesRepository.setLastAccountId(accountId)
    }

    fun saveTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        if (isSaving.value || amount <= 0L) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.insertTransfer(fromAccountId, toAccountId, amount, note, timestamp)
                preferencesRepository.setLastAccountId(fromAccountId)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun onConfirmSplit(
        accountId: Long,
        parts: List<Pair<Long, Long>>,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        onComplete: () -> Unit
    ) {
        val total = parts.sumOf { it.second }
        if (isSaving.value || total <= 0L || parts.size < 2) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                val prefs = preferencesRepository.generalPreferencesFlow.first()
                val rule = preferencesRepository.budgetRulePreferencesFlow.first()
                val byCategory = parts.groupBy { it.first }.mapValues { entry -> entry.value.sumOf { it.second } }
                for ((categoryId, amount) in byCategory) {
                    val category = categories.value.find { it.id == categoryId }
                    if (category == null || !category.countsAsExpense()) return@launch
                    val evaluation = repository.evaluateSpend(
                        categoryId = categoryId,
                        amount = amount,
                        timestamp = timestamp,
                        classification = classification,
                        strict = prefs.strictLimitsEnabled,
                        rolloverEnabled = prefs.rolloverBudgetsEnabled,
                        needsPercent = rule.needsPercent,
                        wantsPercent = rule.wantsPercent,
                        savingsPercent = rule.savingsPercent
                    )
                    if (evaluation.categoryExceeded) {
                        showOverBudgetWarning.value = evaluation.categoryExcess to prefs.strictLimitsEnabled
                        return@launch
                    }
                }
                val bucket = repository.evaluateSpend(
                    categoryId = parts.first().first,
                    amount = total,
                    timestamp = timestamp,
                    classification = classification,
                    strict = prefs.strictLimitsEnabled,
                    rolloverEnabled = prefs.rolloverBudgetsEnabled,
                    needsPercent = rule.needsPercent,
                    wantsPercent = rule.wantsPercent,
                    savingsPercent = rule.savingsPercent
                )
                if (bucket.bucketExceeded) {
                    showBucketWarning.value = Triple(
                        bucket.bucketName ?: "Budget",
                        bucket.bucketExcess,
                        prefs.strictLimitsEnabled
                    )
                    return@launch
                }
                repository.saveSplit(accountId, parts, note, timestamp, classification)
                preferencesRepository.setLastAccountId(accountId)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun saveSplit(
        accountId: Long,
        parts: List<Pair<Long, Long>>,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        onComplete: () -> Unit
    ) {
        val total = parts.sumOf { it.second }
        if (isSaving.value || total <= 0L || parts.size < 2) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.saveSplit(accountId, parts, note, timestamp, classification)
                preferencesRepository.setLastAccountId(accountId)
                onComplete()
            } finally {
                isSaving.value = false
            }
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
        amount: Long,
        categoryId: Long,
        accountId: Long,
        note: String,
        transactionType: CategoryType,
        classification: ExpenseClassification
    ) {
        if (amount <= 0L || templateName.isBlank()) return
        viewModelScope.launch {
            repository.insertTemplate(
                TransactionTemplate(
                    templateName = templateName.trim(),
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note,
                    transactionType = transactionType,
                    classification = classification
                )
            )
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }
}
