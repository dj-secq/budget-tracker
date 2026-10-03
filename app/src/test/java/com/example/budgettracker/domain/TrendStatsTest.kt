package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrendStatsTest {
    @Test
    fun openMonthIsOnlyTheUnfinishedCurrentMonth() {
        val third = LocalDate.of(2026, 10, 3)
        assertTrue(monthIsOpen(10, 2026, third))
        assertFalse(monthIsOpen(10, 2026, LocalDate.of(2026, 10, 31)))
        assertFalse(monthIsOpen(9, 2026, third))
    }

    @Test
    fun averageSkipsTheOpenMonthAndNeedsTwoFinishedMonths() {
        assertEquals(200L, averageMonthlyExpenses(listOf(100L, 200L, 300L), openMonthIndex = null))
        assertEquals(150L, averageMonthlyExpenses(listOf(100L, 200L, 300L), openMonthIndex = 2))
        assertNull(averageMonthlyExpenses(listOf(100L, 300L), openMonthIndex = 1))
        assertNull(averageMonthlyExpenses(listOf(100L), openMonthIndex = null))
    }

    @Test
    fun largestMovesKeepTheThreeBiggestChanges() {
        val moves = largestExpenseMoves(
            current = mapOf("Groceries" to 1_400L, "Rent" to 5_000L, "Food" to 200L, "Transport" to 50L),
            previous = mapOf("Groceries" to 1_000L, "Rent" to 5_000L, "Transport" to 80L, "Health" to 300L)
        )
        assertEquals(
            listOf(
                CategoryMove("Groceries", 400L),
                CategoryMove("Health", -300L),
                CategoryMove("Food", 200L)
            ),
            moves
        )
    }

    @Test
    fun openMonthDoesNotTrendAndAFinishedJumpStillDoes() {
        assertEquals(SpendingTrend.NONE, spendingTrend(15_000L, 10_000L, monthOpen = true))
        assertEquals(SpendingTrend.UP, spendingTrend(15_000L, 10_000L, monthOpen = false))
        assertEquals(50, spendingIncreasePercent(15_000L, 10_000L))
        assertEquals(SpendingTrend.DOWN, spendingTrend(8_000L, 10_000L, monthOpen = false))
        assertEquals(SpendingTrend.NONE, spendingTrend(9_500L, 10_000L, monthOpen = false))
    }
}
