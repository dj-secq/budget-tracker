package com.example.budgettracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class StoryTitle {
    EMPTY,
    QUIET,
    ONE_PURCHASE,
    SEVERAL_INCOMES,
    SAVER,
    AHEAD,
    OVER
}

enum class SpendCompare {
    UP, DOWN, SAME, HIDDEN
}

data class NamedAmount(val name: String, val centavos: Long)

data class CapLine(val name: String, val spentCentavos: Long, val limitCentavos: Long)

data class PurchasePart(
    val groupId: String?,
    val amountCentavos: Long,
    val categoryName: String,
    val note: String
)

data class PurchaseSummary(
    val amountCentavos: Long,
    val categoryName: String,
    val note: String
)

data class MonthStory(
    val leftoverCentavos: Long = 0L,
    val compare: SpendCompare = SpendCompare.HIDDEN,
    val comparePercent: Int? = null,
    val insideCap: Int = 0,
    val capped: Int = 0,
    val overCap: List<NamedAmount> = emptyList(),
    val largest: PurchaseSummary? = null,
    val topCategories: List<NamedAmount> = emptyList(),
    val busiestWeekday: DayOfWeek? = null,
    val noSpendDays: Int = 0,
    val daysCounted: Int = 0,
    val title: StoryTitle = StoryTitle.EMPTY,
    val mostlyCategory: String? = null,
    val monthOpen: Boolean = false
)

/**
 * Quiet days in [month], from the first day or the first logged day, through today when
 * this month is current, otherwise through the last day.
 * Returns no-spend days to days counted.
 */
fun noSpendWindow(
    month: Int,
    year: Int,
    today: LocalDate,
    firstLogged: LocalDate?,
    expenseDays: Set<Int>
): Pair<Int, Int> {
    val length = YearMonth.of(year, month).lengthOfMonth()
    val maxDay = if (today.year == year && today.monthValue == month) today.dayOfMonth else length
    val startDay = when {
        firstLogged == null -> 1
        firstLogged.year == year && firstLogged.monthValue == month -> firstLogged.dayOfMonth
        firstLogged.isAfter(LocalDate.of(year, month, 1)) -> maxDay + 1
        else -> 1
    }
    val days = maxDay - startDay + 1
    if (days <= 0) return 0 to 0
    val quiet = days - expenseDays.count { it in startDay..maxDay }
    return quiet.coerceAtLeast(0) to days
}

/** Weekday with the most expense centavos. A day with nothing spent is ignored. */
fun busiestExpenseWeekday(spentByWeekday: Map<DayOfWeek, Long>): DayOfWeek? {
    return spentByWeekday.filter { it.value > 0L }.maxByOrNull { it.value }?.key
}

/**
 * The biggest expense purchase. Rows that share a split id are one purchase.
 * A blank id stays its own row. The category and note come from the largest part.
 */
fun largestPurchase(parts: List<PurchasePart>): PurchaseSummary? {
    if (parts.isEmpty()) return null
    val summaries = ArrayList<PurchaseSummary>()
    val grouped = parts.filter { !it.groupId.isNullOrBlank() }.groupBy { it.groupId }
    for (rows in grouped.values) {
        val top = rows.maxBy { it.amountCentavos }
        val note = top.note.ifBlank { rows.firstOrNull { it.note.isNotBlank() }?.note.orEmpty() }
        summaries += PurchaseSummary(rows.sumOf { it.amountCentavos }, top.categoryName, note)
    }
    for (part in parts) {
        if (part.groupId.isNullOrBlank()) {
            summaries += PurchaseSummary(part.amountCentavos, part.categoryName, part.note)
        }
    }
    val best = summaries.maxBy { it.amountCentavos }
    return best
}

fun monthStory(
    incomeCentavos: Long,
    spentCentavos: Long,
    previousSpentCentavos: Long,
    monthOpen: Boolean,
    noSpendDays: Int,
    daysCounted: Int,
    incomeSources: Int,
    largest: PurchaseSummary?,
    topCategories: List<NamedAmount>,
    caps: List<CapLine>,
    busiestWeekday: DayOfWeek? = null
): MonthStory {
    val leftover = incomeCentavos - spentCentavos
    val trend = spendingTrend(spentCentavos, previousSpentCentavos, monthOpen)
    val compare = when (trend) {
        SpendingTrend.UP -> SpendCompare.UP
        SpendingTrend.DOWN -> SpendCompare.DOWN
        SpendingTrend.NONE -> if (!monthOpen && spentCentavos > 0L && previousSpentCentavos > 0L) {
            SpendCompare.SAME
        } else {
            SpendCompare.HIDDEN
        }
    }
    val percent = when (compare) {
        SpendCompare.UP -> spendingIncreasePercent(spentCentavos, previousSpentCentavos)
        SpendCompare.DOWN -> spendingDecreasePercent(spentCentavos, previousSpentCentavos)
        else -> null
    }
    val active = caps.filter { it.limitCentavos > 0L }
    val over = active
        .filter { it.spentCentavos > it.limitCentavos }
        .sortedByDescending { it.spentCentavos - it.limitCentavos }
        .take(3)
        .map { NamedAmount(it.name, it.spentCentavos - it.limitCentavos) }
    val top = topCategories.firstOrNull()
    val mostly = if (top != null && spentCentavos > 0L && top.centavos * 2 > spentCentavos) top.name else null
    val title = when {
        incomeCentavos == 0L && spentCentavos == 0L -> StoryTitle.EMPTY
        noSpendDays >= 20 -> StoryTitle.QUIET
        largest != null && spentCentavos > 0L && largest.amountCentavos * 10 > spentCentavos * 7 -> StoryTitle.ONE_PURCHASE
        incomeSources >= 3 && incomeCentavos * 2 > spentCentavos * 3 -> StoryTitle.SEVERAL_INCOMES
        incomeCentavos > 0L && leftover * 2 > incomeCentavos -> StoryTitle.SAVER
        leftover > 0L -> StoryTitle.AHEAD
        else -> StoryTitle.OVER
    }
    return MonthStory(
        leftoverCentavos = leftover,
        compare = compare,
        comparePercent = percent,
        insideCap = active.count { it.spentCentavos <= it.limitCentavos },
        capped = active.size,
        overCap = over,
        largest = largest,
        topCategories = topCategories,
        busiestWeekday = busiestWeekday,
        noSpendDays = noSpendDays,
        daysCounted = daysCounted,
        title = title,
        mostlyCategory = mostly,
        monthOpen = monthOpen
    )
}
