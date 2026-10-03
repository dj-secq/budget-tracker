package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LeftAfterBillsTest {
    private val manila = ZoneId.of("Asia/Manila")
    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun rentAndBorrowedDebtLeave7500() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 1_000_000L,
            rules = listOf(expenseRule("Rent", 200_000L, today, RepeatInterval.MONTHLY)),
            debts = listOf(borrowed("Ana", LocalDate.of(2026, 10, 20), 50_000L)),
            today = today,
            zone = manila
        )
        assertEquals(750_000L, outlook.leftCentavos)
        assertEquals(listOf("Rent", "Ana"), outlook.upcoming.map { it.name })
        assertTrue(outlook.upcoming.all { it.direction == BillDirection.LEAVES })
    }

    @Test
    fun pausedAndEndedRulesAddNothing() {
        val paused = expenseRule("Rent", 200_000L, today, RepeatInterval.MONTHLY).copy(paused = true)
        val ended = expenseRule("Utilities", 50_000L, today, RepeatInterval.MONTHLY).copy(
            endDate = endOfLocalDay(LocalDate.of(2026, 10, 2), manila)
        )
        val outlook = leftAfterBills(1_000_000L, listOf(paused, ended), emptyList(), today, manila)
        assertEquals(1_000_000L, outlook.leftCentavos)
        assertTrue(outlook.upcoming.isEmpty())
    }

    @Test
    fun weeklyRuleCountsEveryRemainingWeek() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 1_000_000L,
            rules = listOf(expenseRule("Allowance out", 200_000L, today, RepeatInterval.WEEKLY)),
            debts = emptyList(),
            today = today,
            zone = manila
        )
        assertEquals(listOf(3, 10, 17, 24, 31), outlook.upcoming.map { localDateOf(it.whenMillis, manila).dayOfMonth })
        assertEquals(1_000_000L - 5 * 200_000L, outlook.leftCentavos)
    }

    @Test
    fun dailyRuleCountsEachRemainingDay() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 0L,
            rules = listOf(expenseRule("Coffee", 100L, today, RepeatInterval.DAILY)),
            debts = emptyList(),
            today = today,
            zone = manila
        )
        assertEquals(29, outlook.upcoming.size)
        assertEquals(-2_900L, outlook.leftCentavos)
    }

    @Test
    fun salaryArrivesWithoutIncreasingTheRemainder() {
        val salary = PlannedRule(
            name = "Salary",
            amountCentavos = 500_000L,
            nextRunTime = localNoon(LocalDate.of(2026, 10, 15), manila),
            interval = RepeatInterval.MONTHLY,
            anchorDay = 15,
            paused = false,
            endDate = null,
            countsAsExpense = false,
            countsAsIncome = true
        )
        val outlook = leftAfterBills(1_000_000L, listOf(salary), emptyList(), today, manila)
        assertEquals(1_000_000L, outlook.leftCentavos)
        assertEquals(BillDirection.ARRIVES, outlook.upcoming.single().direction)
        assertEquals("Salary", outlook.upcoming.single().name)
    }

    @Test
    fun moneyLentThisMonthIsListedAndDoesNotIncreaseTheRemainder() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 1_000_000L,
            rules = emptyList(),
            debts = listOf(
                DueDebt(
                    name = "Ben",
                    dueDate = localNoon(LocalDate.of(2026, 10, 18), manila),
                    owedCentavos = 40_000L,
                    borrowed = false
                )
            ),
            today = today,
            zone = manila
        )
        assertEquals(1_000_000L, outlook.leftCentavos)
        assertEquals(BillDirection.ARRIVES, outlook.upcoming.single().direction)
        assertTrue(outlook.upcoming.single().opensDebt)
    }

    @Test
    fun noBillsLeavesTheBalanceUnchanged() {
        val outlook = leftAfterBills(1_000_000L, emptyList(), emptyList(), today, manila)
        assertEquals(1_000_000L, outlook.leftCentavos)
        assertTrue(outlook.upcoming.isEmpty())
    }

    @Test
    fun overdueAndNextMonthDebtsStayOutOfThisMonth() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 1_000_000L,
            rules = emptyList(),
            debts = listOf(
                borrowed("Late", LocalDate.of(2026, 10, 2), 50_000L),
                borrowed("Later", LocalDate.of(2026, 11, 1), 50_000L)
            ),
            today = today,
            zone = manila
        )
        assertEquals(1_000_000L, outlook.leftCentavos)
        assertTrue(outlook.upcoming.isEmpty())
    }

    private fun expenseRule(name: String, amount: Long, day: LocalDate, interval: RepeatInterval) = PlannedRule(
        name = name,
        amountCentavos = amount,
        nextRunTime = localNoon(day, manila),
        interval = interval,
        anchorDay = day.dayOfMonth,
        paused = false,
        endDate = null,
        countsAsExpense = true,
        countsAsIncome = false
    )

    private fun borrowed(name: String, day: LocalDate, owed: Long) = DueDebt(
        name = name,
        dueDate = localNoon(day, manila),
        owedCentavos = owed,
        borrowed = true
    )
}
