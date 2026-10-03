package com.example.budgettracker.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DebtTrackerViewModel(
    private val repository: BudgetRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val debts: StateFlow<List<Debt>> = repository.getAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastAccountId: StateFlow<Long?> = preferencesRepository.lastAccountIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settlementAmounts: StateFlow<Map<Long, Long>> = debts
        .map { list -> list.mapNotNull { it.settlementTransactionId }.distinct() }
        .distinctUntilChanged()
        .flatMapLatest { repository.observeTransactionAmounts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addDebt(
        personName: String,
        amount: Long,
        type: DebtType,
        note: String,
        accountId: Long,
        dueDate: Long?,
        interestRate: Double,
        startDate: Long
    ) {
        if (personName.isBlank() || amount <= 0L) return
        viewModelScope.launch {
            repository.createDebt(personName.trim(), amount, type, note, accountId, dueDate, interestRate, startDate)
        }
    }

    fun toggleDebtStatus(debt: Debt, accountId: Long?) {
        viewModelScope.launch {
            if (debt.isPaid) {
                repository.unsettleDebt(debt.id)
            } else if (accountId != null) {
                repository.settleDebt(debt.id, accountId, System.currentTimeMillis())
            }
        }
    }

    fun updateDebt(
        debt: Debt,
        personName: String,
        amount: Long,
        type: DebtType,
        note: String,
        accountId: Long,
        dueDate: Long?,
        interestRate: Double,
        startDate: Long
    ) {
        if (personName.isBlank() || amount <= 0L) return
        viewModelScope.launch {
            repository.updateDebt(
                debt.copy(
                    personName = personName.trim(),
                    amount = amount,
                    type = type,
                    date = startDate,
                    note = note,
                    dueDate = dueDate,
                    interestRate = interestRate,
                    accountId = accountId
                )
            )
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }
}
