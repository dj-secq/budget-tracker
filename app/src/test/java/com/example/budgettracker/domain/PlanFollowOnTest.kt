package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PlanFollowOnTest {
    private val manila = ZoneId.of("Asia/Manila")

    @Test
    fun splitPartsMustAddUpToThePurchase() {
        assertTrue(splitPartsMatch(1_000L, listOf(400L, 600L)))
        assertFalse(splitPartsMatch(1_000L, listOf(400L, 500L)))
        assertFalse(splitPartsMatch(1_000L, listOf(1_000L)))
        assertFalse(splitPartsMatch(1_000L, listOf(0L, 1_000L)))
    }

    @Test
    fun paceIsTheEvenShareThroughToday() {
        assertEquals(200L, paceDeltaCentavos(spent = 1_200L, limit = 3_100L, dayOfMonth = 10, lengthOfMonth = 31))
        assertEquals(0L, paceDeltaCentavos(spent = 1_000L, limit = 3_100L, dayOfMonth = 10, lengthOfMonth = 31))
        assertEquals(-200L, paceDeltaCentavos(spent = 800L, limit = 3_100L, dayOfMonth = 10, lengthOfMonth = 31))
        assertEquals(33L, paceDeltaCentavos(spent = 100L, limit = 100L, dayOfMonth = 2, lengthOfMonth = 3))
        assertEquals(-33L, paceDeltaCentavos(spent = 0L, limit = 100L, dayOfMonth = 1, lengthOfMonth = 3))
        assertNull(paceDeltaCentavos(spent = 10L, limit = 0L, dayOfMonth = 10, lengthOfMonth = 31))
        assertNull(paceDeltaCentavos(spent = 10L, limit = 100L, dayOfMonth = 0, lengthOfMonth = 31))
    }

    @Test
    fun suggestionAveragesFinishedMonthsAndSkipsAnOpenOne() {
        assertEquals(200L, suggestedBaseCentavos(listOf(100L, 200L, 300L)))
        assertEquals(50L, suggestedBaseCentavos(listOf(100L, 0L)))
        assertEquals(2L, suggestedBaseCentavos(listOf(1L, 2L)))
        assertNull(suggestedBaseCentavos(listOf(0L, 0L, 0L)))
        assertNull(suggestedBaseCentavos(emptyList()))
        assertEquals(
            listOf(YearMonthKey(2026, 7), YearMonthKey(2026, 8), YearMonthKey(2026, 9)),
            lastFinishedMonths(LocalDate.of(2026, 10, 3))
        )
        assertEquals(
            listOf(YearMonthKey(2026, 8), YearMonthKey(2026, 9), YearMonthKey(2026, 10)),
            lastFinishedMonths(LocalDate.of(2026, 10, 31))
        )
    }

    @Test
    fun semiMonthlyLandsOnThe15thAndTheLastDay() {
        val oct3 = localNoon(LocalDate.of(2026, 10, 3), manila)
        val oct15 = advanceOccurrence(oct3, RepeatInterval.SEMI_MONTHLY, anchorDay = 3, zone = manila)
        val oct31 = advanceOccurrence(oct15, RepeatInterval.SEMI_MONTHLY, anchorDay = 3, zone = manila)
        val nov15 = advanceOccurrence(oct31, RepeatInterval.SEMI_MONTHLY, anchorDay = 3, zone = manila)
        assertEquals(LocalDate.of(2026, 10, 15), localDateOf(oct15, manila))
        assertEquals(LocalDate.of(2026, 10, 31), localDateOf(oct31, manila))
        assertEquals(LocalDate.of(2026, 11, 15), localDateOf(nov15, manila))
        val feb15 = localNoon(LocalDate.of(2026, 2, 15), manila)
        val feb28 = advanceOccurrence(feb15, RepeatInterval.SEMI_MONTHLY, null, manila)
        assertEquals(LocalDate.of(2026, 2, 28), localDateOf(feb28, manila))
        val leap = localNoon(LocalDate.of(2024, 2, 15), manila)
        assertEquals(LocalDate.of(2024, 2, 29), localDateOf(advanceOccurrence(leap, RepeatInterval.SEMI_MONTHLY, null, manila), manila))
    }

    @Test
    fun semiMonthlyBillsCountBothDatesLeftThisMonth() {
        val outlook = leftAfterBills(
            includedBalanceCentavos = 1_000L,
            rules = listOf(
                PlannedRule(
                    name = "Pay",
                    amountCentavos = 100L,
                    nextRunTime = localNoon(LocalDate.of(2026, 10, 15), manila),
                    interval = RepeatInterval.SEMI_MONTHLY,
                    anchorDay = 15,
                    paused = false,
                    endDate = null,
                    countsAsExpense = true,
                    countsAsIncome = false
                )
            ),
            debts = emptyList(),
            today = LocalDate.of(2026, 10, 3),
            zone = manila
        )
        assertEquals(listOf(15, 31), outlook.upcoming.map { localDateOf(it.whenMillis, manila).dayOfMonth })
        assertEquals(800L, outlook.leftCentavos)
    }

    @Test
    fun csvRoundTripKeepsQuotedNotesAndSkipsARepeat() {
        val row = CsvTransaction(
            dateIso = "2026-10-03",
            wallet = "Cash",
            category = "Groceries",
            type = "EXPENSE",
            amountCentavos = 168000L,
            note = "SM, \"fresh\"",
            classification = "NEED"
        )
        val parsed = TransactionCsv.parse(TransactionCsv.build(listOf(row)))
        assertTrue(parsed.headerOk)
        assertEquals(listOf(row), parsed.rows)
        assertEquals(0, parsed.rejected)
        val again = TransactionCsv.newRows(setOf(TransactionCsv.identity(row)), listOf(row, row))
        assertTrue(again.fresh.isEmpty())
        assertEquals(2, again.skipped)
        val bad = TransactionCsv.parse("date,wallet\n2026-10-03,Cash\n")
        assertFalse(bad.headerOk)
        val rejected = TransactionCsv.parse(
            TransactionCsv.HEADER + "\n2026-10-03,Cash,Groceries,EXPENSE,-5.00,nope,NEED\n"
        )
        assertTrue(rejected.headerOk)
        assertEquals(1, rejected.rejected)
        assertTrue(rejected.rows.isEmpty())
    }
}
