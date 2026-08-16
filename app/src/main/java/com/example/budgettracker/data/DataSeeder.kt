package com.example.budgettracker.data

import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.theme.CategoryColors
import kotlinx.coroutines.flow.first
import java.util.Calendar

suspend fun seedDatabaseIfEmpty(repository: BudgetRepository) {
    val existingCategories = repository.getAllCategories().first()
    val existingAccounts = repository.getAllAccounts().first()

    if (existingAccounts.isEmpty()) {
        repository.insertAccount(
            Account(
                name = "Main Wallet",
                type = AccountType.CHECKING,
                balance = 0.0,
                colorArgb = 0xFF3B82F6.toInt() // Blue-500
            )
        )
        repository.insertAccount(
            Account(
                name = "Online Wallet",
                type = AccountType.SAVINGS,
                balance = 0.0,
                colorArgb = 0xFF10B981.toInt() // Emerald-500
            )
        )
    }

    val defaultCategories = listOf(
        Category(name = "Groceries", type = CategoryType.EXPENSE, colorArgb = CategoryColors[0].value.toInt()),
        Category(name = "Rent", type = CategoryType.EXPENSE, colorArgb = CategoryColors[1].value.toInt()),
        Category(name = "Entertainment", type = CategoryType.EXPENSE, colorArgb = CategoryColors[2].value.toInt()),
        Category(name = "Transport", type = CategoryType.EXPENSE, colorArgb = CategoryColors[3].value.toInt()),
        Category(name = "Emergency Fund", type = CategoryType.EXPENSE, colorArgb = CategoryColors[5].value.toInt()),
        Category(name = "Salary", type = CategoryType.INCOME, colorArgb = CategoryColors[4].value.toInt()),
        Category(name = "Utilities", type = CategoryType.EXPENSE, colorArgb = CategoryColors[6 % CategoryColors.size].value.toInt()),
        Category(name = "Health", type = CategoryType.EXPENSE, colorArgb = CategoryColors[7 % CategoryColors.size].value.toInt()),
        Category(name = "Shopping", type = CategoryType.EXPENSE, colorArgb = CategoryColors[8 % CategoryColors.size].value.toInt()),
        Category(name = "Education", type = CategoryType.EXPENSE, colorArgb = CategoryColors[9 % CategoryColors.size].value.toInt()),
        Category(name = "Insurance", type = CategoryType.EXPENSE, colorArgb = CategoryColors[10 % CategoryColors.size].value.toInt()),
        Category(name = "Investments", type = CategoryType.EXPENSE, colorArgb = CategoryColors[11 % CategoryColors.size].value.toInt()),
        Category(name = "Gifts", type = CategoryType.EXPENSE, colorArgb = CategoryColors[12 % CategoryColors.size].value.toInt()),
        Category(name = "Personal Care", type = CategoryType.EXPENSE, colorArgb = CategoryColors[13 % CategoryColors.size].value.toInt()),
        Category(name = "Travel", type = CategoryType.EXPENSE, colorArgb = CategoryColors[14 % CategoryColors.size].value.toInt()),
        Category(name = "Subscription", type = CategoryType.EXPENSE, colorArgb = CategoryColors[15 % CategoryColors.size].value.toInt()),
        Category(name = "Maintenance", type = CategoryType.EXPENSE, colorArgb = CategoryColors[16 % CategoryColors.size].value.toInt()),
        Category(name = "Savings", type = CategoryType.EXPENSE, colorArgb = CategoryColors[17 % CategoryColors.size].value.toInt()),
        Category(name = "Loan", type = CategoryType.EXPENSE, colorArgb = CategoryColors[18 % CategoryColors.size].value.toInt()),
        Category(name = "Loan Received", type = CategoryType.INCOME, colorArgb = CategoryColors[19 % CategoryColors.size].value.toInt()),
        Category(name = "Allowance", type = CategoryType.INCOME, colorArgb = CategoryColors[20 % CategoryColors.size].value.toInt()),
        Category(name = "Food", type = CategoryType.EXPENSE, colorArgb = CategoryColors[21 % CategoryColors.size].value.toInt()),
        Category(name = "Lost", type = CategoryType.EXPENSE, colorArgb = CategoryColors[22 % CategoryColors.size].value.toInt()),
        Category(name = "Rent Income", type = CategoryType.INCOME, colorArgb = CategoryColors[23 % CategoryColors.size].value.toInt()),
        Category(name = "Bonus", type = CategoryType.INCOME, colorArgb = CategoryColors[24 % CategoryColors.size].value.toInt()),
        Category(name = "Withdraw / Transfer Out", type = CategoryType.EXPENSE, colorArgb = CategoryColors[25 % CategoryColors.size].value.toInt()),
        Category(name = "Deposit / Transfer In", type = CategoryType.INCOME, colorArgb = CategoryColors[26 % CategoryColors.size].value.toInt())
    )

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentYear = calendar.get(Calendar.YEAR)
    val limits = listOf(8000.0, 15000.0, 3000.0, 3000.0, 5000.0, 0.0, 4000.0, 2000.0, 3000.0, 5000.0, 2500.0, 5000.0, 1000.0, 1500.0, 2000.0, 1000.0, 1500.0, 5000.0, 2000.0, 0.0, 0.0, 6000.0, 500.0, 0.0, 0.0, 0.0, 0.0)
    defaultCategories.forEachIndexed { index, category ->
        val existing = existingCategories.find { it.name == category.name && it.type == category.type }
        if (existing == null) {
            val id = repository.insertCategory(category)
            if (category.type == CategoryType.EXPENSE) {
                repository.setBudgetLimit(id, limits[index], currentMonth, currentYear)
            }
        }
    }
}
