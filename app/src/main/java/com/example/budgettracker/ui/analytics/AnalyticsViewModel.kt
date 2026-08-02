package com.example.budgettracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.local.entity.Category
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

import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification

data class CategorySpending(
    val category: Category,
    val totalSpent: Double
)

data class BucketStats(
    val name: String,
    val goal: Double,
    val actual: Double
) {
    val net: Double get() = goal - actual
}

data class AnalyticsUiState(
    val ruleTitle: String = "50/30/20 Rule",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val bucketStats: List<BucketStats> = emptyList(),
    val expenseCategorySpending: List<CategorySpending> = emptyList(),
    val incomeCategorySpending: List<CategorySpending> = emptyList(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(
        Pair(Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))
    )

    private val transactionsFlow = _monthYear.flatMapLatest { (month, year) ->
        repository.getTransactionsForMonth(month, year)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllCategories(),
        transactionsFlow,
        preferencesRepository.budgetRulePreferencesFlow,
        _monthYear
    ) { categories, transactions, budgetRule, monthYear ->
        val (month, year) = monthYear
        
        // Total Income (monthly)
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
        val totalIncome = transactions.filter { tx -> incomeCategories.any { it.id == tx.categoryId } }.sumOf { it.amount }

        // Needs / Wants / Savings
        val actualNeeds = transactions.filter { it.classification == ExpenseClassification.NEED }.sumOf { it.amount }
        val actualWants = transactions.filter { it.classification == ExpenseClassification.WANT }.sumOf { it.amount }
        val actualSavings = transactions.filter { it.classification == ExpenseClassification.SAVING }.sumOf { it.amount }

        val bucketStats = listOf(
            BucketStats("Needs (${budgetRule.needsPercent}%)", totalIncome * (budgetRule.needsPercent / 100.0), actualNeeds),
            BucketStats("Wants (${budgetRule.wantsPercent}%)", totalIncome * (budgetRule.wantsPercent / 100.0), actualWants),
            BucketStats("Savings (${budgetRule.savingsPercent}%)", totalIncome * (budgetRule.savingsPercent / 100.0), actualSavings)
        )

        // Spending by category (monthly)
        val expenseSpendingData = categories.filter { it.type == CategoryType.EXPENSE }.map { category ->
            val sum = transactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            CategorySpending(category, sum)
        }.filter { it.totalSpent > 0 }

        val incomeSpendingData = categories.filter { it.type == CategoryType.INCOME }.map { category ->
            val sum = transactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            CategorySpending(category, sum)
        }.filter { it.totalSpent > 0 }

        val totalExpenses = expenseSpendingData.sumOf { it.totalSpent }
        val ruleTitle = "${budgetRule.needsPercent}/${budgetRule.wantsPercent}/${budgetRule.savingsPercent} Budget Rule"

        AnalyticsUiState(ruleTitle, totalIncome, totalExpenses, bucketStats, expenseSpendingData, incomeSpendingData, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = Pair(month, year)
    }
}
