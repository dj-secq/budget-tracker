package com.example.budgettracker.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


class GoalsViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fundGoal(goal: SavingsGoal, amount: Double, accountId: Long) {
        viewModelScope.launch {
            // Find or create "Savings" category
            val categories = repository.getAllCategories().first()
            var savingsCategory = categories.find { it.name == "Savings" }
            if (savingsCategory == null) {
                val newCategoryId = repository.insertCategory(
                    com.example.budgettracker.data.local.entity.Category(
                        name = "Savings",
                        type = com.example.budgettracker.data.local.entity.CategoryType.EXPENSE,
                        colorArgb = 0xFF10B981.toInt()
                    )
                )
                savingsCategory = repository.getAllCategories().first().find { it.id == newCategoryId }
            }

            // Create transaction
            if (savingsCategory != null) {
                repository.insertTransaction(
                    Transaction(
                        accountId = accountId,
                        categoryId = savingsCategory.id,
                        amount = amount,
                        timestamp = System.currentTimeMillis(),
                        note = "Funded goal: ${goal.name}",
                        classification = ExpenseClassification.SAVING
                    )
                )
            }

            val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.updateGoal(updatedGoal)
        }
    }

    fun addGoal(name: String, targetAmount: Double, targetDate: Long? = null, frequency: String? = null, contributionAmount: Double? = null, iconName: String? = null) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDate = targetDate,
                contributionFrequency = frequency,
                contributionAmount = contributionAmount,
                iconName = iconName
            )
            repository.insertGoal(goal)
        }
    }

    fun updateGoal(goal: SavingsGoal, name: String, targetAmount: Double, targetDate: Long?, frequency: String?, contributionAmount: Double?, iconName: String? = null) {
        viewModelScope.launch {
            repository.updateGoal(
                goal.copy(
                    name = name,
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
