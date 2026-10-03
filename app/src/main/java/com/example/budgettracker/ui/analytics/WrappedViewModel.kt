package com.example.budgettracker.ui.analytics

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.local.entity.countsAsIncome
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.example.budgettracker.domain.CapLine
import com.example.budgettracker.domain.MonthStory
import com.example.budgettracker.domain.NamedAmount
import com.example.budgettracker.domain.PurchasePart
import com.example.budgettracker.domain.busiestExpenseWeekday
import com.example.budgettracker.domain.largestPurchase
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.monthIsOpen
import com.example.budgettracker.domain.monthStory
import com.example.budgettracker.domain.noSpendWindow
import com.example.budgettracker.domain.previousMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class WrappedUiState(
    val isLoading: Boolean = true,
    val monthName: String = "",
    val year: Int = 0,
    val totalIncome: Long = 0L,
    val totalSpent: Long = 0L,
    val transactionCount: Int = 0,
    val story: MonthStory = MonthStory()
)

class WrappedViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val resources: Resources
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrappedUiState())
    val uiState: StateFlow<WrappedUiState> = _uiState.asStateFlow()

    fun loadWrappedData(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val transactions = repository.getTransactionsForMonth(month, year).first()
            val categories = repository.getAllCategories().first()
            val categoryById = categories.associateBy { it.id }
            val prefs = preferencesRepository.generalPreferencesFlow.first()
            val limits = repository.getBudgetLimitsForMonth(month, year).first()
            val rollover = if (prefs.rolloverBudgetsEnabled) {
                repository.rolloversForMonth(month, year)
            } else {
                emptyMap()
            }
            val previous = previousMonth(month, year)
            val previousTransactions = repository.getTransactionsForMonth(previous.month, previous.year).first()

            val incomeTxs = transactions.filter { categoryById[it.categoryId]?.countsAsIncome() == true }
            val expenseTxs = transactions.filter { categoryById[it.categoryId]?.countsAsExpense() == true }
            val totalIncome = incomeTxs.sumOf { it.amount }
            val totalSpent = expenseTxs.sumOf { it.amount }
            val previousSpent = previousTransactions
                .filter { categoryById[it.categoryId]?.countsAsExpense() == true }
                .sumOf { it.amount }
            val unknown = resources.getString(R.string.unknown)
            val topCategories = expenseTxs
                .groupBy { it.categoryId }
                .map { entry ->
                    val name = categoryById[entry.key]?.name ?: unknown
                    NamedAmount(name, entry.value.sumOf { it.amount })
                }
                .sortedByDescending { it.centavos }
                .take(3)
            val spentByWeekday = expenseTxs
                .groupBy { localDateOf(it.timestamp).dayOfWeek }
                .mapValues { (_, rows) -> rows.sumOf { it.amount } }
            val largest = largestPurchase(
                expenseTxs.map { tx ->
                    PurchasePart(
                        groupId = tx.splitGroupId,
                        amountCentavos = tx.amount,
                        categoryName = categoryById[tx.categoryId]?.name ?: unknown,
                        note = tx.note
                    )
                }
            )
            val caps = categories.filter { it.countsAsExpense() }.map { category ->
                val base = limits.find { it.categoryId == category.id }?.assignedAmount ?: 0L
                val extra = rollover[category.id] ?: 0L
                val spent = expenseTxs.filter { it.categoryId == category.id }.sumOf { it.amount }
                CapLine(category.name, spent, base + extra)
            }
            val today = LocalDate.now()
            val firstLogged = repository.earliestTimestamp()?.let { localDateOf(it) }
            val expenseDays = expenseTxs.map { localDateOf(it.timestamp).dayOfMonth }.toSet()
            val (quietDays, daysCounted) = noSpendWindow(month, year, today, firstLogged, expenseDays)
            val story = monthStory(
                incomeCentavos = totalIncome,
                spentCentavos = totalSpent,
                previousSpentCentavos = previousSpent,
                monthOpen = monthIsOpen(month, year, today),
                noSpendDays = quietDays,
                daysCounted = daysCounted,
                incomeSources = incomeTxs.map { it.categoryId }.toSet().size,
                largest = largest,
                topCategories = topCategories,
                caps = caps,
                busiestWeekday = busiestExpenseWeekday(spentByWeekday)
            )
            val monthName = if (month in 1..12) {
                Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            } else {
                ""
            }
            _uiState.value = WrappedUiState(
                isLoading = false,
                monthName = monthName,
                year = year,
                totalIncome = totalIncome,
                totalSpent = totalSpent,
                transactionCount = incomeTxs.size + expenseTxs.size,
                story = story
            )
        }
    }
}
