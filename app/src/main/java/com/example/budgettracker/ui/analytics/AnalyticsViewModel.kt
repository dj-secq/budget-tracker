package com.example.budgettracker.ui.analytics

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.local.entity.countsAsIncome
import com.example.budgettracker.data.local.dao.CategoryClassificationTotal
import com.example.budgettracker.data.local.dao.MonthCategoryTotal
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.BudgetRulePreferences
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.example.budgettracker.domain.CategoryMove
import com.example.budgettracker.domain.averageMonthlyExpenses
import com.example.budgettracker.domain.largestExpenseMoves
import com.example.budgettracker.domain.monthIsOpen
import com.example.budgettracker.domain.monthWindow
import com.example.budgettracker.domain.previousMonth
import com.example.budgettracker.domain.trailingMonths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

data class CategorySpending(
    val category: Category,
    val totalSpent: Long
)

data class BucketStats(
    val name: String,
    val goal: Long,
    val actual: Long,
    val savingsTarget: Boolean = false
) {
    val net: Long get() = goal - actual

    val onTrack: Boolean get() = when {
        goal <= 0L -> true
        savingsTarget -> actual >= goal
        else -> actual <= goal
    }
}

data class MonthTrend(
    val label: String,
    val incomeCentavos: Long,
    val expenseCentavos: Long,
    val open: Boolean
) {
    val leftoverCentavos: Long get() = incomeCentavos - expenseCentavos
}

data class AnalyticsUiState(
    val ruleTitle: String = "50/30/20 Rule",
    val totalIncome: Long = 0L,
    val totalExpenses: Long = 0L,
    val bucketStats: List<BucketStats> = emptyList(),
    val expenseCategorySpending: List<CategorySpending> = emptyList(),
    val incomeCategorySpending: List<CategorySpending> = emptyList(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val insights: List<Insight> = emptyList(),
    val cashflowIncome: List<Float> = emptyList(),
    val cashflowExpense: List<Float> = emptyList(),
    val cashflowLabels: List<String> = emptyList(),
    val monthTrends: List<MonthTrend> = emptyList(),
    val averageExpenseCentavos: Long? = null,
    val categoryMoves: List<CategoryMove> = emptyList(),
    val hasTransactions: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val resources: Resources
) : ViewModel() {

    private val _monthYear = MutableStateFlow(currentMonthYear())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllCategories(),
        preferencesRepository.budgetRulePreferencesFlow,
        _monthYear
    ) { categories, budgetRule, monthYear ->
        Triple(categories, budgetRule, monthYear)
    }.flatMapLatest { (categories, budgetRule, monthYear) ->
        val month = monthYear.first
        val year = monthYear.second
        val window = monthWindow(month, year)
        val keys = trailingMonths(month, year, 6)
        val rangeStart = monthWindow(keys.first().month, keys.first().year).startInclusive
        combine(
            repository.observeTotalsByCategory(window.startInclusive, window.endInclusive),
            repository.observeTotalsByMonthCategory(rangeStart, window.endInclusive)
        ) { breakdown, monthRows ->
            load(categories, budgetRule, month, year, breakdown, monthRows)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = month to year
    }

    private fun load(
        categories: List<Category>,
        budgetRule: BudgetRulePreferences,
        month: Int,
        year: Int,
        breakdown: List<CategoryClassificationTotal>,
        monthRows: List<MonthCategoryTotal>,
        today: LocalDate = LocalDate.now()
    ): AnalyticsUiState {
        val categoryById = categories.associateBy { it.id }

        val expenseByCategory = linkedMapOf<Long, Long>()
        val incomeByCategory = linkedMapOf<Long, Long>()
        val bucketActual = mutableMapOf<ExpenseClassification, Long>()
        for (row in breakdown) {
            val category = categoryById[row.categoryId] ?: continue
            when {
                category.countsAsIncome() -> incomeByCategory.addAmount(row.categoryId, row.total)
                category.countsAsExpense() -> {
                    expenseByCategory.addAmount(row.categoryId, row.total)
                    val classification = runCatching { ExpenseClassification.valueOf(row.classification) }.getOrNull()
                    if (classification != null && classification != ExpenseClassification.NONE) {
                        bucketActual.addAmount(classification, row.total)
                    }
                }
            }
        }

        val expenseSpending = expenseByCategory.mapNotNull { (id, total) ->
            categoryById[id]?.let { CategorySpending(it, total) }
        }.filter { it.totalSpent > 0L }
        val incomeSpending = incomeByCategory.mapNotNull { (id, total) ->
            categoryById[id]?.let { CategorySpending(it, total) }
        }.filter { it.totalSpent > 0L }
        val totalIncome = incomeSpending.sumOf { it.totalSpent }
        val totalExpenses = expenseSpending.sumOf { it.totalSpent }

        val bucketStats = listOf(
            BucketStats(
                resources.getString(R.string.bucket_needs, budgetRule.needsPercent),
                totalIncome * budgetRule.needsPercent / 100L,
                bucketActual[ExpenseClassification.NEED] ?: 0L
            ),
            BucketStats(
                resources.getString(R.string.bucket_wants, budgetRule.wantsPercent),
                totalIncome * budgetRule.wantsPercent / 100L,
                bucketActual[ExpenseClassification.WANT] ?: 0L
            ),
            BucketStats(
                resources.getString(R.string.bucket_savings, budgetRule.savingsPercent),
                totalIncome * budgetRule.savingsPercent / 100L,
                bucketActual[ExpenseClassification.SAVING] ?: 0L,
                savingsTarget = true
            )
        )

        val keys = trailingMonths(month, year, 6)
        val incomeByMonth = mutableMapOf<Pair<Int, Int>, Long>()
        val expenseByMonth = mutableMapOf<Pair<Int, Int>, Long>()
        for (row in monthRows) {
            val category = categoryById[row.categoryId] ?: continue
            val key = row.year to row.month
            when {
                category.countsAsIncome() -> incomeByMonth.addAmount(key, row.total)
                category.countsAsExpense() -> expenseByMonth.addAmount(key, row.total)
            }
        }
        val open = monthIsOpen(month, year, today)
        val trends = keys.map { key ->
            val pair = key.year to key.month
            MonthTrend(
                label = Month.of(key.month).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                incomeCentavos = incomeByMonth[pair] ?: 0L,
                expenseCentavos = expenseByMonth[pair] ?: 0L,
                open = open && key.year == year && key.month == month
            )
        }
        val cashflowIncome = trends.map { (it.incomeCentavos / 100.0).toFloat() }
        val cashflowExpense = trends.map { (it.expenseCentavos / 100.0).toFloat() }
        val labels = trends.map { it.label }
        val openIndex = trends.indexOfFirst { it.open }.takeIf { it >= 0 }
        val previous = previousMonth(month, year)
        val previousExpenses = expenseByMonth[previous.year to previous.month] ?: 0L
        val hasPrevious = monthRows.any { it.year == previous.year && it.month == previous.month }
        val top = expenseSpending.maxByOrNull { it.totalSpent }

        return AnalyticsUiState(
            ruleTitle = resources.getString(
                R.string.budget_rule_named,
                budgetRule.needsPercent,
                budgetRule.wantsPercent,
                budgetRule.savingsPercent
            ),
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            bucketStats = bucketStats,
            expenseCategorySpending = expenseSpending,
            incomeCategorySpending = incomeSpending,
            currentMonth = month,
            currentYear = year,
            insights = InsightsEngine.generateInsights(
                hasTransactions = breakdown.isNotEmpty(),
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                topExpenseName = top?.category?.name,
                topExpenseAmount = top?.totalSpent ?: 0L,
                previousExpenseTotal = previousExpenses,
                hasPreviousMonth = hasPrevious,
                monthOpen = open,
                resources = resources
            ),
            cashflowIncome = cashflowIncome,
            cashflowExpense = cashflowExpense,
            cashflowLabels = labels,
            monthTrends = trends,
            averageExpenseCentavos = averageMonthlyExpenses(trends.map { it.expenseCentavos }, openIndex),
            categoryMoves = largestExpenseMoves(
                expensesNamed(monthRows, categoryById, year, month),
                expensesNamed(monthRows, categoryById, previous.year, previous.month)
            ),
            hasTransactions = breakdown.isNotEmpty()
        )
    }

    private fun expensesNamed(
        rows: List<MonthCategoryTotal>,
        categoryById: Map<Long, Category>,
        year: Int,
        month: Int
    ): Map<String, Long> {
        val totals = linkedMapOf<String, Long>()
        for (row in rows) {
            if (row.year != year || row.month != month) continue
            val category = categoryById[row.categoryId] ?: continue
            if (!category.countsAsExpense()) continue
            totals[category.name] = (totals[category.name] ?: 0L) + row.total
        }
        return totals
    }

    private fun <K> MutableMap<K, Long>.addAmount(key: K, amount: Long) {
        this[key] = (this[key] ?: 0L) + amount
    }

    private fun currentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.MONTH) + 1) to calendar.get(Calendar.YEAR)
    }
}
