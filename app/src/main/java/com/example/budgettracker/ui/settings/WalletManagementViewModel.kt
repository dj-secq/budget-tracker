package com.example.budgettracker.ui.settings

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.theme.CatSoftBlue
import com.example.budgettracker.ui.theme.CatAmber


class WalletManagementViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWallet(name: String, startingBalance: Double, colorIndex: Int) {
        viewModelScope.launch {
            val colors = listOf(EmeraldGreen.toArgb(), CatSoftBlue.toArgb(), CatAmber.toArgb())
            val color = colors[colorIndex % colors.size]
            val account = Account(
                name = name,
                type = AccountType.CHECKING,
                balance = startingBalance,
                colorArgb = color
            )
            repository.insertAccount(account)
            // Note: If startingBalance > 0, we should arguably create an initial transaction, 
            // but for simplicity we just set the balance.
        }
    }

    fun deleteWallet(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
