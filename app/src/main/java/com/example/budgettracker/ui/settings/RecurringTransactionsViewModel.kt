package com.example.budgettracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.nextOpenOccurrence
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringTransactionItem(
    val recurringTransaction: RecurringTransaction,
    val category: Category?,
    val account: Account?
)

class RecurringTransactionsViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransactionItem>> = combine(
        repository.getAllRecurringTransactions(),
        categories,
        accounts
    ) { transactions, categoryList, accountList ->
        transactions.map { tx ->
            RecurringTransactionItem(
                recurringTransaction = tx,
                category = categoryList.find { it.id == tx.categoryId },
                account = accountList.find { it.id == tx.accountId }
            )
        }.sortedBy { it.recurringTransaction.nextRunTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        frequency: Frequency,
        start: Long,
        endDate: Long?
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            repository.insertRecurringTransaction(
                RecurringTransaction(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    note = note.trim(),
                    frequency = frequency,
                    startDate = start,
                    nextRunTime = start,
                    anchorDay = localDateOf(start).dayOfMonth,
                    paused = false,
                    endDate = endDate
                )
            )
            repository.postDueRecurring(System.currentTimeMillis())
        }
    }

    fun updateRule(
        rule: RecurringTransaction,
        accountId: Long,
        categoryId: Long,
        amount: Long,
        note: String,
        frequency: Frequency,
        endDate: Long?
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            repository.updateRecurringTransaction(
                rule.copy(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    note = note.trim(),
                    frequency = frequency,
                    endDate = endDate
                )
            )
        }
    }

    fun setPaused(rule: RecurringTransaction, paused: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val next = if (!paused && rule.nextRunTime <= now) {
                nextOpenOccurrence(rule.nextRunTime, rule.frequency.toInterval(), rule.anchorDay, now)
            } else {
                rule.nextRunTime
            }
            repository.updateRecurringTransaction(rule.copy(paused = paused, nextRunTime = next))
        }
    }

    fun deleteRecurringTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(transaction)
        }
    }
}
