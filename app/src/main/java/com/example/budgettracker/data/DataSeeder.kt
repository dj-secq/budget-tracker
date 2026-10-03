package com.example.budgettracker.data

import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.example.budgettracker.domain.PresetCategory
import com.example.budgettracker.domain.presetCategories
import com.example.budgettracker.domain.presetsNotIn
import kotlinx.coroutines.flow.first

private val seedColors = intArrayOf(
    0xFF3B82F6.toInt(),
    0xFFF43F5E.toInt(),
    0xFFF59E0B.toInt(),
    0xFF14B8A6.toInt(),
    0xFF6366F1.toInt(),
    0xFF8B5CF6.toInt(),
    0xFF10B981.toInt()
)

suspend fun seedDatabaseIfEmpty(
    repository: BudgetRepository,
    preferences: UserPreferencesRepository
) {
    if (!preferences.hasSeeded()) {
        val existingCategories = repository.getAllCategories().first()
        val existingAccounts = repository.getAllAccounts().first()
        if (existingCategories.isEmpty() && existingAccounts.isEmpty()) {
            repository.insertAccount(
                Account(name = "Cash", type = AccountType.CHECKING, balance = 0, colorArgb = 0xFF3B82F6.toInt())
            )
            repository.insertAccount(
                Account(name = "Bank", type = AccountType.SAVINGS, balance = 0, colorArgb = 0xFF10B981.toInt())
            )
            presetCategories.forEach { repository.insertCategory(it.toCategory()) }
            preferences.setPresetCatalogFilled()
        }
        preferences.setHasSeeded()
    }
    if (!preferences.presetCatalogFilled()) {
        fillMissingPresets(repository)
        preferences.setPresetCatalogFilled()
    }
}

private suspend fun fillMissingPresets(repository: BudgetRepository) {
    val existing = repository.getAllCategories().first()
    for (category in existing) {
        if (!category.iconName.isNullOrBlank()) continue
        val preset = presetCategories.find { it.name.equals(category.name.trim(), ignoreCase = true) } ?: continue
        repository.updateCategory(category.copy(iconName = preset.iconKey))
    }
    presetsNotIn(existing.map { it.name }).forEach { preset ->
        repository.insertCategory(preset.toCategory())
    }
}

private fun PresetCategory.toCategory(): Category {
    val index = presetCategories.indexOf(this).coerceAtLeast(0)
    return Category(
        name = name,
        type = if (expense) CategoryType.EXPENSE else CategoryType.INCOME,
        colorArgb = seedColors[index % seedColors.size],
        iconName = iconKey
    )
}
