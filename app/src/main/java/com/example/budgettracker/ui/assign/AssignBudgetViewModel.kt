package com.example.budgettracker.ui.assign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.example.budgettracker.domain.budgetBaseToStore
import com.example.budgettracker.domain.lastFinishedMonths
import com.example.budgettracker.domain.monthWindow
import com.example.budgettracker.domain.suggestedBaseCentavos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

data class AssignBudgetItem(
    val category: Category,
    val base: Long,
    val spent: Long,
    val rollover: Long = 0L,
    val suggestedBase: Long? = null
) {
    val effective: Long get() = base + rollover
}

data class AssignBudgetUiState(
    val budgetItems: List<AssignBudgetItem> = emptyList(),
    val rolloverEnabled: Boolean = false,
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@OptIn(ExperimentalCoroutinesApi::class)
class AssignBudgetViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(currentMonthYear())

    init {
        viewModelScope.launch {
            val (month, year) = _monthYear.value
            repository.copyBudgetsForwardIfEmpty(month, year)
        }
    }

    private val monthlyDataFlow = _monthYear.flatMapLatest { (month, year) ->
        combine(
            repository.getTransactionsForMonth(month, year),
            repository.getBudgetLimitsForMonth(month, year)
        ) { txs, limits ->
            txs to limits
        }
    }

    private val finishedMonths = lastFinishedMonths(LocalDate.now())

    private val historyFlow = run {
        val oldest = finishedMonths.firstOrNull()
        val newest = finishedMonths.lastOrNull()
        if (oldest == null || newest == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            val start = monthWindow(oldest.month, oldest.year).startInclusive
            val end = monthWindow(newest.month, newest.year).endInclusive
            repository.observeTotalsByMonthCategory(start, end)
        }
    }

    val uiState: StateFlow<AssignBudgetUiState> = combine(
        repository.getAllCategories(),
        monthlyDataFlow,
        _monthYear,
        preferencesRepository.generalPreferencesFlow,
        historyFlow
    ) { categories, monthlyData, monthYear, prefs, history ->
        val (monthTransactions, budgetLimits) = monthlyData
        val (month, year) = monthYear
        val rollovers = if (prefs.rolloverBudgetsEnabled) repository.rolloversForMonth(month, year) else emptyMap()
        val items = categories.filter { it.countsAsExpense() }.map { category ->
            val base = budgetLimits.find { it.categoryId == category.id }?.assignedAmount ?: 0L
            val spent = monthTransactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            val monthly = finishedMonths.map { key ->
                history.find { it.categoryId == category.id && it.year == key.year && it.month == key.month }?.total ?: 0L
            }
            AssignBudgetItem(
                category = category,
                base = base,
                spent = spent,
                rollover = rollovers[category.id] ?: 0L,
                suggestedBase = suggestedBaseCentavos(monthly)
            )
        }
        AssignBudgetUiState(items, prefs.rolloverBudgetsEnabled, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AssignBudgetUiState())

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = month to year
        viewModelScope.launch {
            repository.copyBudgetsForwardIfEmpty(month, year)
        }
    }

    fun updateBudgetLimit(categoryId: Long, editedBase: Long) {
        viewModelScope.launch {
            val (month, year) = _monthYear.value
            repository.setBudgetLimit(categoryId, budgetBaseToStore(editedBase), month, year)
        }
    }

    private fun currentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.MONTH) + 1) to calendar.get(Calendar.YEAR)
    }
}
