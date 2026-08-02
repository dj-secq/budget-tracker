package com.example.budgettracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class BudgetItem(
    val category: Category,
    val limit: Double,
    val spent: Double,
    val rollover: Double = 0.0
)

data class HomeUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val budgetItems: List<BudgetItem> = emptyList(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)

class HomeViewModel(
    private val budgetRepository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(
        Pair(Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))
    )

    private val monthlyDataFlow = _monthYear.flatMapLatest { (month, year) ->
        combine(
            budgetRepository.getTransactionsForMonth(month, year),
            budgetRepository.getBudgetLimitsForMonth(month, year)
        ) { txs, limits ->
            Pair(txs, limits)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            budgetRepository.getAllAccounts(),
            budgetRepository.getAllCategories(),
            monthlyDataFlow
        ) { a, b, c -> Triple(a, b, c) },
        combine(
            _monthYear,
            budgetRepository.getRecentTransactions(),
            preferencesRepository.generalPreferencesFlow
        ) { a, b, c -> Triple(a, b, c) }
    ) { (accounts, categories, monthlyData), (monthYear, allTransactions, prefs) ->
        
        val (monthTransactions, budgetLimits) = monthlyData
        val (month, year) = monthYear
        
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
        
        // Gamification: Streaks
        val expenseDaysLocal = allTransactions
            .filter { tx -> expenseCategories.any { it.id == tx.categoryId } }
            .map {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
            }.toSet()
            
        var currentStreak = 0
        var longestStreak = 0
        
        if (allTransactions.isNotEmpty()) {
            val firstTxTime = allTransactions.minOf { it.timestamp }
            val checkCal = Calendar.getInstance().apply { 
                timeInMillis = firstTxTime 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }
            val todayTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis
            
            var tempStreak = 0
            while(checkCal.timeInMillis < todayTime) {
                val d = checkCal.get(Calendar.YEAR) * 10000 + checkCal.get(Calendar.MONTH) * 100 + checkCal.get(Calendar.DAY_OF_MONTH)
                if (!expenseDaysLocal.contains(d)) {
                    tempStreak++
                    if (tempStreak > longestStreak) longestStreak = tempStreak
                } else {
                    tempStreak = 0
                }
                checkCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            // Calculate current streak (look backwards from today)
            val todayCal = Calendar.getInstance()
            while (true) {
                val d = todayCal.get(Calendar.YEAR) * 10000 + todayCal.get(Calendar.MONTH) * 100 + todayCal.get(Calendar.DAY_OF_MONTH)
                if (!expenseDaysLocal.contains(d)) {
                    currentStreak++
                    todayCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Use monthly transactions for income/expense/budget calculations
        val totalIncome = monthTransactions.filter { tx -> 
            incomeCategories.any { it.id == tx.categoryId } 
        }.sumOf { it.amount }

        val totalExpenses = monthTransactions.filter { tx -> 
            expenseCategories.any { it.id == tx.categoryId } 
        }.sumOf { it.amount }
        
        // Sum all account balances
        val totalBalance = accounts.sumOf { it.balance }
        
        // Budget items use monthly spending
        val items = expenseCategories.map { category ->
            val baseLimit = budgetLimits.find { it.categoryId == category.id }?.assignedAmount ?: 0.0
            val spent = monthTransactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            val rollover = if (prefs.rolloverBudgetsEnabled) budgetRepository.getRolloverAmount(category.id, month, year) else 0.0
            BudgetItem(category, baseLimit + rollover, spent, rollover)
        }
        
        HomeUiState(
            accounts = accounts,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            budgetItems = items,
            currentMonth = month,
            currentYear = year,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = Pair(month, year)
    }
}
