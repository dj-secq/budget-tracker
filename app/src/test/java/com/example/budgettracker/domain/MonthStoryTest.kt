package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class MonthStoryTest {

    @Test
    fun finishedMonthSaysWhenSpendingRoseAndAnOpenMonthDoesNot() {
        val finished = monthStory(
            incomeCentavos = 0L,
            spentCentavos = 150L,
            previousSpentCentavos = 100L,
            monthOpen = false,
            noSpendDays = 0,
            daysCounted = 31,
            incomeSources = 0,
            largest = null,
            topCategories = emptyList(),
            caps = emptyList()
        )
        assertEquals(SpendCompare.UP, finished.compare)
        assertEquals(50, finished.comparePercent)

        val open = monthStory(
            incomeCentavos = 0L,
            spentCentavos = 150L,
            previousSpentCentavos = 100L,
            monthOpen = true,
            noSpendDays = 0,
            daysCounted = 10,
            incomeSources = 0,
            largest = null,
            topCategories = emptyList(),
            caps = emptyList()
        )
        assertEquals(SpendCompare.HIDDEN, open.compare)
        assertNull(open.comparePercent)
    }

    @Test
    fun capsCountWhoStayedInsideAndNameWhoWentOver() {
        val story = monthStory(
            incomeCentavos = 0L,
            spentCentavos = 1_200L,
            previousSpentCentavos = 0L,
            monthOpen = false,
            noSpendDays = 0,
            daysCounted = 31,
            incomeSources = 0,
            largest = null,
            topCategories = emptyList(),
            caps = listOf(
                CapLine("Groceries", spentCentavos = 1_000L, limitCentavos = 800L),
                CapLine("Transport", spentCentavos = 200L, limitCentavos = 500L)
            )
        )
        assertEquals(1, story.insideCap)
        assertEquals(2, story.capped)
        assertEquals(listOf(NamedAmount("Groceries", 200L)), story.overCap)
    }

    @Test
    fun splitRowsAreOnePurchaseAndALoneRowStaysItself() {
        val split = largestPurchase(
            listOf(
                PurchasePart("g1", 400L, "Food", "market"),
                PurchasePart("g1", 600L, "Home", ""),
                PurchasePart(null, 250L, "Transport", "jeep")
            )
        )
        assertEquals(PurchaseSummary(1_000L, "Home", "market"), split)

        val alone = largestPurchase(
            listOf(PurchasePart(null, 250L, "Transport", "jeep"))
        )
        assertEquals(PurchaseSummary(250L, "Transport", "jeep"), alone)
    }

    @Test
    fun busiestWeekdayIsTheOneWithMorePesos() {
        val day = busiestExpenseWeekday(
            mapOf(DayOfWeek.SATURDAY to 200L, DayOfWeek.SUNDAY to 50L)
        )
        assertEquals(DayOfWeek.SATURDAY, day)
    }

    @Test
    fun titlesComeFromAmountsAndTheTopCategoryKeepsItsOwnName() {
        val empty = story(income = 0L, spent = 0L)
        assertEquals(StoryTitle.EMPTY, empty.title)
        assertNull(empty.mostlyCategory)

        val saver = story(income = 1_000L, spent = 400L)
        assertEquals(StoryTitle.SAVER, saver.title)

        val named = story(
            income = 0L,
            spent = 1_000L,
            top = listOf(NamedAmount("School fees", 600L))
        )
        assertEquals("School fees", named.mostlyCategory)
    }

    private fun story(
        income: Long,
        spent: Long,
        top: List<NamedAmount> = emptyList()
    ) = monthStory(
        incomeCentavos = income,
        spentCentavos = spent,
        previousSpentCentavos = 0L,
        monthOpen = false,
        noSpendDays = 0,
        daysCounted = 30,
        incomeSources = 1,
        largest = null,
        topCategories = top,
        caps = emptyList()
    )
}
