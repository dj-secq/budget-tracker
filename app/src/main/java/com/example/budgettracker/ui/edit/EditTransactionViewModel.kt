package com.example.budgettracker.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSaving = MutableStateFlow(false)
    val transactionFlow = MutableStateFlow<Transaction?>(null)
    val showOverBudgetWarning = MutableStateFlow<Pair<Long, Boolean>?>(null)
    val showBucketWarning = MutableStateFlow<Triple<String, Long, Boolean>?>(null)

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionFlow.value = repository.getTransactionById(transactionId)
        }
    }

    fun onConfirmSave(
        transactionId: Long,
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        onComplete: () -> Unit
    ) {
        if (isSaving.value || amount <= 0L) return
        val oldTransaction = transactionFlow.value ?: return
        if (oldTransaction.id != transactionId) return
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
                    savingsPercent = rule.savingsPercent,
                    replacing = oldTransaction
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
                repository.updateTransaction(
                    oldTransaction.copy(
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        note = note,
                        timestamp = timestamp,
                        classification = classification
                    )
                )
                preferencesRepository.setLastAccountId(accountId)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun saveTransactionWithoutLimits(
        transactionId: Long,
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification,
        onComplete: () -> Unit
    ) {
        if (isSaving.value || amount <= 0L) return
        val oldTransaction = transactionFlow.value ?: return
        if (oldTransaction.id != transactionId) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.updateTransaction(
                    oldTransaction.copy(
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        note = note,
                        timestamp = timestamp,
                        classification = classification
                    )
                )
                preferencesRepository.setLastAccountId(accountId)
                onComplete()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        val transaction = transactionFlow.value ?: return
        if (isSaving.value) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
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
}
