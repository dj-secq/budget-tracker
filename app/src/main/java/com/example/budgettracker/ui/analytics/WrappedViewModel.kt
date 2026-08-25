package com.example.budgettracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class WrappedUiState(
    val isLoading: Boolean = true,
    val monthName: String = "",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val netSavings: Double = 0.0,
    val largestExpense: Transaction? = null,
    val largestExpenseCategory: String = "",
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val verdict: String = "",
    val busiestDayOfWeek: String = "",
    val totalTransactions: Int = 0,
    val noSpendDays: Int = 0
)

class WrappedViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrappedUiState())
    val uiState: StateFlow<WrappedUiState> = _uiState.asStateFlow()

    fun loadWrappedData(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val transactions = repository.getTransactionsForMonth(month, year).first()
            val categories = repository.getAllCategories().first()
            
            val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val monthName = if (month in 1..12) monthNames[month - 1] else ""
            
            val incomeCatIds = categories.filter { it.type == CategoryType.INCOME && it.name != "Deposit / Transfer In" }.map { it.id }
            val expenseCatIds = categories.filter { it.type == CategoryType.EXPENSE && it.name != "Withdraw / Transfer Out" }.map { it.id }
            
            val incomeTxs = transactions.filter { incomeCatIds.contains(it.categoryId) }
            val expenseTxs = transactions.filter { expenseCatIds.contains(it.categoryId) }
            
            val totalIncome = incomeTxs.sumOf { it.amount }
            val totalSpent = expenseTxs.sumOf { it.amount }
            val netSavings = totalIncome - totalSpent
            
            val largestExpense = expenseTxs.maxByOrNull { it.amount }
            val largestExpenseCategoryName = categories.find { it.id == largestExpense?.categoryId }?.name ?: "Unknown"
            
            val categoryMap = categories.associateBy { it.id }
            val topCategories = expenseTxs
                .groupBy { it.categoryId }
                .map { entry -> 
                    val name = categoryMap[entry.key]?.name ?: "Unknown"
                    val sum = entry.value.sumOf { it.amount }
                    name to sum
                }
                .sortedByDescending { it.second }
                .take(3)
                
            // Busiest Day of Week
            val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val busiestDay = expenseTxs.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.DAY_OF_WEEK)
            }.maxByOrNull { it.value.size }
            
            val busiestDayName = busiestDay?.let { dayNames[it.key - 1] } ?: "Unknown"
            
            // Total Transactions
            val totalTxs = incomeTxs.size + expenseTxs.size
            
            // No Spend Days
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val spendDays = expenseTxs.map { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.DAY_OF_MONTH)
            }.toSet()
            
            // Only count past days if it's the current month, else whole month
            val currentCal = Calendar.getInstance()
            val isCurrentMonth = currentCal.get(Calendar.MONTH) + 1 == month && currentCal.get(Calendar.YEAR) == year
            val maxDayToCount = if (isCurrentMonth) currentCal.get(Calendar.DAY_OF_MONTH) else daysInMonth
            
            // Find the user's very first transaction ever to avoid counting days before they started using the app
            val allTxs = repository.getRecentTransactions().first()
            val firstTxTime = allTxs.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
            val firstTxCal = Calendar.getInstance().apply { timeInMillis = firstTxTime }
            
            val startDay = if (firstTxCal.get(Calendar.MONTH) + 1 == month && firstTxCal.get(Calendar.YEAR) == year) {
                firstTxCal.get(Calendar.DAY_OF_MONTH)
            } else if (firstTxCal.get(Calendar.YEAR) > year || (firstTxCal.get(Calendar.YEAR) == year && firstTxCal.get(Calendar.MONTH) + 1 > month)) {
                // The wrapped month is BEFORE the user started using the app
                maxDayToCount + 1 // This will make daysToCount <= 0
            } else {
                1
            }
            
            val daysToCount = maxDayToCount - startDay + 1
            val noSpendDaysCount = if (daysToCount <= 0) 0 else {
                daysToCount - spendDays.filter { it in startDay..maxDayToCount }.size
            }
                
            val topCategoryName = topCategories.firstOrNull()?.first ?: ""
            val topCategorySpent = topCategories.firstOrNull()?.second ?: 0.0
            val topCategoryRatio = if (totalSpent > 0) topCategorySpent / totalSpent else 0.0
            
            val incomeSourcesCount = incomeTxs.map { it.categoryId }.toSet().size
            
            val verdict = when {
                totalIncome == 0.0 && totalSpent == 0.0 -> "The Ghost"
                
                // Behavioral Verdicts
                noSpendDaysCount >= 20 -> "The Financial Zen Master"
                noSpendDaysCount <= 3 && totalTxs > 50 -> "The Micro-Spender"
                largestExpense != null && largestExpense.amount > totalSpent * 0.7 -> "The Whale"
                incomeSourcesCount >= 3 && totalIncome > totalSpent * 1.5 -> "The Hustler"
                
                // Category-Based Verdicts
                topCategoryName in listOf("Food", "Groceries", "Dining") && topCategoryRatio > 0.3 -> "The Foodie"
                topCategoryName == "Travel" && topCategoryRatio > 0.2 -> "The Wanderlust"
                topCategoryName == "Entertainment" && topCategoryRatio > 0.2 -> "The Socialite"
                topCategoryName in listOf("Rent", "Utilities", "Housing", "Groceries") && topCategoryRatio > 0.7 -> "The Essentialist"
                
                // Basic Verdicts
                netSavings > totalIncome * 0.5 -> "The Super Saver"
                netSavings > 0 -> "The Responsible Spender"
                else -> "The High Roller"
            }
            
            _uiState.value = WrappedUiState(
                isLoading = false,
                monthName = monthName,
                totalIncome = totalIncome,
                totalSpent = totalSpent,
                netSavings = netSavings,
                largestExpense = largestExpense,
                largestExpenseCategory = largestExpenseCategoryName,
                topCategories = topCategories,
                verdict = verdict,
                busiestDayOfWeek = busiestDayName,
                totalTransactions = totalTxs,
                noSpendDays = Math.max(0, noSpendDaysCount)
            )
        }
    }
}
