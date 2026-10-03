package com.example.budgettracker.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalProgress(val goal: SavingsGoal, val savedCentavos: Long)

class GoalsViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalProgress>> = combine(
        repository.getAllGoals(),
        repository.goalTotals()
    ) { goalList, totals ->
        val saved = totals.associate { it.goalId to it.total }
        goalList.map { goal -> GoalProgress(goal, saved[goal.id] ?: 0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fundError = MutableStateFlow<String?>(null)
    val fundError: StateFlow<String?> = _fundError

    fun clearFundError() {
        _fundError.value = null
    }

    fun fundGoal(goal: SavingsGoal, amount: Long, accountId: Long, spendCategoryId: Long? = null) {
        if (amount <= 0L) return
        viewModelScope.launch {
            try {
                repository.fundGoal(goal.id, accountId, amount, spendCategoryId = spendCategoryId)
                _fundError.value = null
            } catch (error: IllegalStateException) {
                _fundError.value = error.message ?: "Could not fund this goal"
            }
        }
    }

    fun unfundLatest(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.unfundLatest(goal.id)
        }
    }

    fun addGoal(
        name: String,
        targetAmount: Long,
        targetDate: Long? = null,
        frequency: String? = null,
        contributionAmount: Long? = null,
        iconName: String? = null
    ) {
        if (name.isBlank() || targetAmount <= 0L) return
        viewModelScope.launch {
            repository.insertGoal(
                SavingsGoal(
                    name = name.trim(),
                    targetAmount = targetAmount,
                    currentAmount = 0L,
                    targetDate = targetDate,
                    contributionFrequency = frequency,
                    contributionAmount = contributionAmount,
                    iconName = iconName
                )
            )
        }
    }

    fun updateGoal(
        goal: SavingsGoal,
        name: String,
        targetAmount: Long,
        targetDate: Long?,
        frequency: String?,
        contributionAmount: Long?,
        iconName: String? = null
    ) {
        if (name.isBlank() || targetAmount <= 0L) return
        viewModelScope.launch {
            repository.updateGoal(
                goal.copy(
                    name = name.trim(),
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    contributionFrequency = frequency,
                    contributionAmount = contributionAmount,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
