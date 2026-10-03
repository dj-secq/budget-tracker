package com.example.budgettracker.domain

data class PresetCategory(
    val name: String,
    val expense: Boolean,
    val iconKey: String
)

/**
 * Everyday categories for a new ledger, and the older presets that were taken
 * off the fresh-install list. No amounts: a preset must not invent a budget.
 */
val presetCategories: List<PresetCategory> = listOf(
    PresetCategory("Groceries", expense = true, iconKey = "groceries"),
    PresetCategory("Food", expense = true, iconKey = "food"),
    PresetCategory("Transport", expense = true, iconKey = "transport"),
    PresetCategory("Rent", expense = true, iconKey = "rent"),
    PresetCategory("Utilities", expense = true, iconKey = "utilities"),
    PresetCategory("Health", expense = true, iconKey = "health"),
    PresetCategory("Shopping", expense = true, iconKey = "shopping"),
    PresetCategory("Entertainment", expense = true, iconKey = "entertainment"),
    PresetCategory("Education", expense = true, iconKey = "education"),
    PresetCategory("Insurance", expense = true, iconKey = "insurance"),
    PresetCategory("Investments", expense = true, iconKey = "investments"),
    PresetCategory("Gifts", expense = true, iconKey = "gifts"),
    PresetCategory("Personal Care", expense = true, iconKey = "personal care"),
    PresetCategory("Travel", expense = true, iconKey = "travel"),
    PresetCategory("Subscription", expense = true, iconKey = "subscription"),
    PresetCategory("Maintenance", expense = true, iconKey = "maintenance"),
    PresetCategory("Savings", expense = true, iconKey = "savings"),
    PresetCategory("Emergency Fund", expense = true, iconKey = "emergency fund"),
    PresetCategory("Loan", expense = true, iconKey = "loan"),
    PresetCategory("Lost", expense = true, iconKey = "lost"),
    PresetCategory("Salary", expense = false, iconKey = "salary"),
    PresetCategory("Allowance", expense = false, iconKey = "allowance"),
    PresetCategory("Bonus", expense = false, iconKey = "bonus"),
    PresetCategory("Rent Income", expense = false, iconKey = "rent income"),
    PresetCategory("Loan Received", expense = false, iconKey = "loan received")
)

fun presetsNotIn(existingNames: Collection<String>): List<PresetCategory> {
    val have = existingNames.map { it.trim().lowercase() }.toSet()
    return presetCategories.filter { it.name.lowercase() !in have }
}
