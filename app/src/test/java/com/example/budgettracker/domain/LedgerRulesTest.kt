package com.example.budgettracker.domain

import com.example.budgettracker.domain.RepeatInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class LedgerRulesTest {
    private val manila = ZoneId.of("Asia/Manila")

    @Test
    fun pesosRoundHalfUpToCentavos() {
        assertEquals(10L, Money.fromDoublePesos(0.1))
        assertEquals(30L, Money.fromDoublePesos(0.1 + 0.2))
        assertEquals(101L, Money.fromDoublePesos(1.005))
        assertEquals(1500000L, Money.fromDoublePesos(15000.0))
    }

    @Test
    fun parsePesosAcceptsPlainAndGroupedInput() {
        assertEquals(10050L, Money.parsePesos("100.50"))
        assertEquals(10050L, Money.parsePesos("₱100.5"))
        assertEquals(123456L, Money.parsePesos("1,234.56"))
        assertEquals(50000L, Money.parsePesos("500"))
        assertNull(Money.parsePesos(""))
        assertNull(Money.parsePesos("-5"))
    }

    @Test
    fun formatIsStableForCentavos() {
        assertEquals(Money.format(10050L), Money.format(Money.parsePesos("100.50")!!))
        assertTrue(Money.format(1500000L).contains("15,000.00"))
    }

    @Test
    fun manilaMonthWindowIncludesLateEveningAndExcludesNextMidnight() {
        val window = monthWindow(9, 2026, manila)
        val late = localNoon(LocalDate.of(2026, 9, 30), manila)
        val evening = LocalDate.of(2026, 9, 30).atTime(23, 30).atZone(manila).toInstant().toEpochMilli()
        val nextMidnight = startOfLocalDay(LocalDate.of(2026, 10, 1), manila)
        assertTrue(evening in window.startInclusive..window.endInclusive)
        assertTrue(late in window.startInclusive..window.endInclusive)
        assertTrue(nextMidnight > window.endInclusive)
    }

    @Test
    fun pickerUtcMidnightStaysOnTheSelectedManilaDay() {
        val picked = localDateFromPickerUtc(
            LocalDate.of(2026, 9, 30).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        assertEquals(LocalDate.of(2026, 9, 30), picked)
        val stored = localNoon(picked, manila)
        assertEquals(LocalDate.of(2026, 9, 30), localDateOf(stored, manila))
    }

    @Test
    fun endOfLocalDayIncludesTheWholeSelectedDay() {
        val day = LocalDate.of(2026, 3, 15)
        val start = startOfLocalDay(day, manila)
        val end = endOfLocalDay(day, manila)
        val evening = day.atTime(21, 0).atZone(manila).toInstant().toEpochMilli()
        assertTrue(evening in start..end)
        assertTrue(startOfLocalDay(day.plusDays(1), manila) > end)
    }

    @Test
    fun trailingMonthsDoNotShiftOnThe30th() {
        val months = trailingMonths(2, 2026, 6)
        assertEquals(
            listOf(
                YearMonthKey(2025, 9),
                YearMonthKey(2025, 10),
                YearMonthKey(2025, 11),
                YearMonthKey(2025, 12),
                YearMonthKey(2026, 1),
                YearMonthKey(2026, 2)
            ),
            months
        )
    }

    @Test
    fun rolloverIsLastMonthsLeftoverNotLifetime() {
        val bases = mapOf(
            YearMonthKey(2026, 1) to 10000L,
            YearMonthKey(2026, 2) to 10000L
        )
        val spent = mapOf(
            YearMonthKey(2026, 1) to 20000L,
            YearMonthKey(2026, 2) to 0L
        )
        assertEquals(0L, rolloverCentavos(bases, spent, YearMonthKey(2026, 2)))
        assertEquals(10000L, rolloverCentavos(bases, spent, YearMonthKey(2026, 3)))
    }

    @Test
    fun unbudgetedSpendingDoesNotCreateANegativeCarry() {
        val spent = mapOf(YearMonthKey(2026, 1) to 5000L)
        assertEquals(0L, rolloverCentavos(emptyMap(), spent, YearMonthKey(2026, 2)))
    }

    @Test
    fun carryWithNoNewBaseStillCapsSpending() {
        val check = categoryCap(spent = 0L, adding = 70000L, base = 0L, rollover = 60000L, strict = true)
        assertTrue(check.blocked)
        assertEquals(10000L, check.excess)
    }

    @Test
    fun zeroBaseAndZeroCarryIsNotACap() {
        val check = categoryCap(spent = 500L, adding = 500L, base = 0L, rollover = 0L, strict = true)
        assertFalse(check.exceeded)
    }

    @Test
    fun transfersDoNotCountAsIncomeOrExpense() {
        assertFalse(countsAsIncome(CategoryKind.INCOME, CategoryUse.TRANSFER_IN))
        assertFalse(countsAsExpense(CategoryKind.EXPENSE, CategoryUse.TRANSFER_OUT))
        assertTrue(countsAsIncome(CategoryKind.INCOME, CategoryUse.NORMAL))
        assertTrue(countsAsExpense(CategoryKind.EXPENSE, CategoryUse.NORMAL))
    }

    @Test
    fun recurringCatchUpDoesNotSkipOrPostTheFuture() {
        val zone = manila
        val day0 = LocalDate.of(2026, 1, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val day1 = advanceOccurrence(day0, RepeatInterval.DAILY, null, zone)
        val now = day1
        val plan = planOccurrences(day0, RepeatInterval.DAILY, null, now, zone)
        assertEquals(listOf(day0, day1), plan.dueTimestamps)
        assertTrue(plan.nextRunTime > now)
    }

    @Test
    fun monthlyAnchorSurvivesFebruary() {
        val zone = manila
        val jan31 = LocalDate.of(2026, 1, 31).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val feb = advanceOccurrence(jan31, RepeatInterval.MONTHLY, anchorDay = 31, zone)
        val march = advanceOccurrence(feb, RepeatInterval.MONTHLY, anchorDay = 31, zone)
        assertEquals(LocalDate.of(2026, 2, 28), localDateOf(feb, zone))
        assertEquals(LocalDate.of(2026, 3, 31), localDateOf(march, zone))
    }

    @Test
    fun threeMissedMonthsAreAllDueAndNoneAreFuture() {
        val zone = manila
        val jan = LocalDate.of(2026, 1, 15).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDate.of(2026, 3, 20).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val plan = planOccurrences(jan, RepeatInterval.MONTHLY, anchorDay = 15, now, zone)
        val dates = plan.dueTimestamps.map { localDateOf(it, zone) }
        assertEquals(
            listOf(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 15)),
            dates
        )
        assertEquals(LocalDate.of(2026, 4, 15), localDateOf(plan.nextRunTime, zone))
    }

    @Test
    fun interestIsSimpleAndHalfUp() {
        val start = LocalDate.of(2026, 1, 1).atStartOfDay(manila).toInstant().toEpochMilli()
        val end = LocalDate.of(2027, 1, 1).atStartOfDay(manila).toInstant().toEpochMilli()
        assertEquals(365L, ChronoUnit.DAYS.between(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
        assertEquals(1000L, interestCentavos(10_000L, 10.0, start, end, manila))
        val oneDayLater = LocalDate.of(2026, 1, 2).atStartOfDay(manila).toInstant().toEpochMilli()
        assertEquals(3L, interestCentavos(10_000L, 10.0, start, oneDayLater, manila))
        assertEquals(0L, interestCentavos(0L, 10.0, start, end, manila))
        assertEquals(0L, interestCentavos(10_000L, 0.0, start, end, manila))
        assertEquals(0L, interestCentavos(10_000L, 10.0, end, start, manila))
    }

    @Test
    fun openDebtIncludesInterestAndSettledDebtDoesNot() {
        val start = LocalDate.of(2026, 1, 1).atStartOfDay(manila).toInstant().toEpochMilli()
        val end = LocalDate.of(2027, 1, 1).atStartOfDay(manila).toInstant().toEpochMilli()
        val oneDayLater = LocalDate.of(2026, 1, 2).atStartOfDay(manila).toInstant().toEpochMilli()
        assertEquals(11_000L, debtOwedCentavos(10_000L, 10.0, start, end, settled = false, zone = manila))
        assertEquals(10_003L, debtOwedCentavos(10_000L, 10.0, start, oneDayLater, settled = false, zone = manila))
        assertEquals(0L, debtOwedCentavos(10_000L, 10.0, start, end, settled = true, zone = manila))
        assertEquals(
            10_003L,
            debtDisplayCentavos(10_000L, 10.0, start, oneDayLater, settled = false, settlementCentavos = null, zone = manila)
        )
        assertEquals(
            11_000L,
            debtDisplayCentavos(10_000L, 10.0, start, end, settled = true, settlementCentavos = 11_000L, zone = manila)
        )
        assertEquals(
            10_000L,
            debtDisplayCentavos(10_000L, 10.0, start, end, settled = true, settlementCentavos = null, zone = manila)
        )
    }

    @Test
    fun annualPercentBlankIsZeroAndNegativeIsRejected() {
        assertEquals(0.0, parseAnnualPercent("")!!, 0.0)
        assertEquals(0.0, parseAnnualPercent("   ")!!, 0.0)
        assertEquals(10.0, parseAnnualPercent("10")!!, 0.0)
        assertEquals(10.5, parseAnnualPercent("10.5%")!!, 0.0)
        assertNull(parseAnnualPercent("-1"))
        assertNull(parseAnnualPercent("abc"))
    }

    @Test
    fun pickerUtcMillisRoundTripsTheCalendarDay() {
        val day = LocalDate.of(2026, 10, 3)
        assertEquals(day, localDateFromPickerUtc(pickerUtcMillis(day)))
    }

    @Test
    fun assignSaveStoresTheEditedBaseWithoutRollover() {
        val editedBase = 10_000L
        val rollover = 5_000L
        assertEquals(editedBase, budgetBaseToStore(editedBase))
        assertEquals(0L, budgetBaseToStore(0L))
        assertTrue(budgetBaseToStore(editedBase) != editedBase + rollover)
    }

    @Test
    fun postingTheSameDueSetAgainInsertsNothing() {
        val due = listOf(1_700_000_000_000L, 1_700_086_400_000L, 1_700_172_800_000L)
        val firstPass = occurrencesToInsert(due, emptySet())
        assertEquals(due, firstPass)
        assertTrue(occurrencesToInsert(due, firstPass.toSet()).isEmpty())
        assertEquals(listOf(due[1]), occurrencesToInsert(due, setOf(due[0], due[2])))
    }

    @Test
    fun endDateDropsLaterOccurrences() {
        val due = listOf(10L, 20L, 30L)
        assertEquals(due, occurrencesOnOrBefore(due, null))
        assertEquals(listOf(10L, 20L), occurrencesOnOrBefore(due, 20L))
        assertTrue(occurrencesOnOrBefore(due, 5L).isEmpty())
    }

    @Test
    fun unpausingSkipsMissedMonths() {
        val january = LocalDate.of(2026, 1, 31).atTime(8, 0).atZone(manila).toInstant().toEpochMilli()
        val march15 = LocalDate.of(2026, 3, 15).atTime(8, 0).atZone(manila).toInstant().toEpochMilli()
        val march31 = LocalDate.of(2026, 3, 31).atTime(8, 0).atZone(manila).toInstant().toEpochMilli()
        assertEquals(march31, nextOpenOccurrence(january, RepeatInterval.MONTHLY, 31, march15, manila))
    }

    @Test
    fun aZeroPreviousMonthIsNotTwentyPercentHigher() {
        assertNull(spendingIncreasePercent(50_000L, 0L))
        assertNull(spendingIncreasePercent(120L, 100L))
        assertEquals(50, spendingIncreasePercent(150L, 100L))
        assertEquals(21, spendingIncreasePercent(121L, 100L))
    }

    @Test
    fun reminderNamesOnlyWhatIsDueToday() {
        assertNull(reminderMessage(loggedToday = true, recurringLabels = emptyList(), debtNames = emptyList()))
        assertEquals(
            "You haven't logged any transactions today.",
            reminderMessage(false, emptyList(), emptyList())
        )
        assertEquals(
            "Rent posts today. Debt with Ana is due today.",
            reminderMessage(true, listOf("Rent"), listOf("Ana"))
        )
        val many = reminderMessage(true, listOf("Rent", "Netflix"), listOf("Ana", "Ben"))
        assertEquals("2 recurring items post today. 2 debts are due today.", many)
        assertFalse(many!!.contains("pending"))
    }
}
