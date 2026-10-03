package com.example.budgettracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.DeleteOutcome
import com.example.budgettracker.ui.theme.WalletPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletManagementViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deleteBlocked = MutableStateFlow(false)
    val deleteBlocked: StateFlow<Boolean> = _deleteBlocked.asStateFlow()

    fun clearDeleteBlocked() {
        _deleteBlocked.value = false
    }

    fun addWallet(name: String, startingBalance: Long, colorIndex: Int, includeInTotalBalance: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name.trim(),
                    type = AccountType.CHECKING,
                    balance = startingBalance,
                    colorArgb = WalletPalette.argbAt(colorIndex),
                    includeInTotalBalance = includeInTotalBalance
                )
            )
        }
    }

    fun deleteWallet(account: Account) {
        viewModelScope.launch {
            when (repository.deleteAccount(account)) {
                DeleteOutcome.Deleted -> Unit
                DeleteOutcome.Blocked -> _deleteBlocked.value = true
            }
        }
    }

    fun updateWallet(account: Account, newName: String, includeInTotalBalance: Boolean) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateAccount(account.copy(name = newName.trim(), includeInTotalBalance = includeInTotalBalance))
        }
    }
}
