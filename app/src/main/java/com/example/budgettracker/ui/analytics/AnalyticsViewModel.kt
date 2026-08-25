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
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val insights: List<Insight> = emptyList(),
    val cashflowIncome: List<Float> = emptyList(),
    val cashflowExpense: List<Float> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(
        Pair(Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))
    )

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllCategories(),
        repository.getRecentTransactions(),
        preferencesRepository.budgetRulePreferencesFlow,
        _monthYear
    ) { categories, allTransactions, budgetRule, monthYear ->
        val (month, year) = monthYear
        
        val currentMonthTransactions = allTransactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
        }
        
        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        
        val previousMonthTransactions = allTransactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.MONTH) + 1 == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }

        // Total Income (monthly)
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME && it.name != "Deposit / Transfer In" }
        val totalIncome = currentMonthTransactions.filter { tx -> incomeCategories.any { it.id == tx.categoryId } }.sumOf { it.amount }

        // Needs / Wants / Savings
        val actualNeeds = currentMonthTransactions.filter { it.classification == ExpenseClassification.NEED }.sumOf { it.amount }
        val actualWants = currentMonthTransactions.filter { it.classification == ExpenseClassification.WANT }.sumOf { it.amount }
        val actualSavings = currentMonthTransactions.filter { it.classification == ExpenseClassification.SAVING }.sumOf { it.amount }

        val bucketStats = listOf(
            BucketStats("Needs (${budgetRule.needsPercent}%)", totalIncome * (budgetRule.needsPercent / 100.0), actualNeeds),
            BucketStats("Wants (${budgetRule.wantsPercent}%)", totalIncome * (budgetRule.wantsPercent / 100.0), actualWants),
            BucketStats("Savings (${budgetRule.savingsPercent}%)", totalIncome * (budgetRule.savingsPercent / 100.0), actualSavings)
        )

        // Spending by category (monthly)
        val expenseSpendingData = categories.filter { it.type == CategoryType.EXPENSE && it.name != "Withdraw / Transfer Out" }.map { category ->
            val sum = currentMonthTransactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            CategorySpending(category, sum)
        }.filter { it.totalSpent > 0 }

        val incomeSpendingData = incomeCategories.map { category ->
            val sum = currentMonthTransactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            CategorySpending(category, sum)
        }.filter { it.totalSpent > 0 }

        val totalExpenses = expenseSpendingData.sumOf { it.totalSpent }
        val ruleTitle = "${budgetRule.needsPercent}/${budgetRule.wantsPercent}/${budgetRule.savingsPercent} Budget Rule"
        
        // Generate Insights
        val insights = InsightsEngine.generateInsights(
            currentMonthTransactions = currentMonthTransactions,
            previousMonthTransactions = previousMonthTransactions,
            categories = categories,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses
        )
        
        // Calculate 6-month cashflow
        val cashflowIncome = mutableListOf<Float>()
        val cashflowExpense = mutableListOf<Float>()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.add(Calendar.MONTH, -i)
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            
            val mTransactions = allTransactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.MONTH) + 1 == m && txCal.get(Calendar.YEAR) == y
            }
            
            val inc = mTransactions.filter { tx -> incomeCategories.any { it.id == tx.categoryId } }.sumOf { tx -> tx.amount }.toFloat()
            val expCatIds = categories.filter { it.type == CategoryType.EXPENSE && it.name != "Withdraw / Transfer Out" }.map { it.id }
            val exp = mTransactions.filter { tx -> expCatIds.contains(tx.categoryId) }.sumOf { tx -> tx.amount }.toFloat()
            
            cashflowIncome.add(inc)
            cashflowExpense.add(exp)
        }

        AnalyticsUiState(
            ruleTitle = ruleTitle, 
            totalIncome = totalIncome, 
            totalExpenses = totalExpenses, 
            bucketStats = bucketStats, 
            expenseCategorySpending = expenseSpendingData, 
            incomeCategorySpending = incomeSpendingData, 
            currentMonth = month, 
            currentYear = year,
            insights = insights,
            cashflowIncome = cashflowIncome,
            cashflowExpense = cashflowExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = Pair(month, year)
    }
}
