package com.example.budgettracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.DeleteOutcome
import com.example.budgettracker.ui.theme.CategoryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryManagementViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deleteBlocked = MutableStateFlow(false)
    val deleteBlocked: StateFlow<Boolean> = _deleteBlocked.asStateFlow()

    fun clearDeleteBlocked() {
        _deleteBlocked.value = false
    }

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val color = CategoryColors[(categories.value.size % CategoryColors.size)].value.toInt()
            repository.insertCategory(Category(name = name.trim(), type = type, colorArgb = color))
        }
    }

    fun renameCategory(category: Category, name: String, iconName: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = name.trim(), iconName = iconName?.takeIf { it.isNotBlank() }))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            when (repository.deleteCategory(category)) {
                DeleteOutcome.Deleted -> Unit
                DeleteOutcome.Blocked -> _deleteBlocked.value = true
            }
        }
    }
}
