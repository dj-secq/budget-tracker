package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetCategoriesTest {
    private val shortList = listOf(
        "Groceries", "Food", "Transport", "Rent", "Utilities", "Health",
        "Shopping", "Entertainment", "Salary", "Allowance"
    )

    @Test
    fun catalogKeepsUniqueNamesAndAnIconForEach() {
        assertEquals(25, presetCategories.size)
        assertEquals(
            presetCategories.size,
            presetCategories.map { it.name.lowercase() }.toSet().size
        )
        assertTrue(presetCategories.all { it.iconKey.isNotBlank() })
        assertTrue(shortList.all { name -> presetCategories.any { it.name == name } })
    }

    @Test
    fun missingPresetsSkipNamesTheLedgerAlreadyHas() {
        val missing = presetsNotIn(shortList).map { it.name }
        assertEquals(
            listOf(
                "Education", "Insurance", "Investments", "Gifts", "Personal Care",
                "Travel", "Subscription", "Maintenance", "Savings", "Emergency Fund",
                "Loan", "Lost", "Bonus", "Rent Income", "Loan Received"
            ),
            missing
        )
        assertTrue(presetsNotIn(listOf("  GROCERIES ")).none { it.name == "Groceries" })
        assertTrue(presetsNotIn(presetCategories.map { it.name }).isEmpty())
    }
}
