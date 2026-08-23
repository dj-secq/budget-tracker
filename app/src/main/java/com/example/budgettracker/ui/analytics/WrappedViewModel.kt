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
    val verdict: String = ""
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
                
            val verdict = when {
                totalIncome == 0.0 && totalSpent == 0.0 -> "The Ghost"
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
                verdict = verdict
            )
        }
    }
}
