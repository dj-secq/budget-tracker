package com.example.budgettracker.domain

import java.time.LocalDate
import kotlin.math.abs

data class CategoryMove(
    val name: String,
    val deltaCentavos: Long
)

/** The selected month is still in progress on this calendar day. The last day counts as finished. */
fun monthIsOpen(month: Int, year: Int, today: LocalDate): Boolean {
    if (today.year != year || today.monthValue != month) return false
    return today.dayOfMonth < today.lengthOfMonth()
}

/**
 * Mean of the finished months. [openMonthIndex] is left out.
 * Fewer than two finished months is not an average.
 */
fun averageMonthlyExpenses(expensesCentavos: List<Long>, openMonthIndex: Int?): Long? {
    val finished = expensesCentavos.filterIndexed { index, _ -> index != openMonthIndex }
    if (finished.size < 2) return null
    return finished.sum() / finished.size
}

/** Largest peso changes, up or down. A zero change is omitted. */
fun largestExpenseMoves(
    current: Map<String, Long>,
    previous: Map<String, Long>,
    limit: Int = 3
): List<CategoryMove> {
    if (limit <= 0) return emptyList()
    val names = LinkedHashSet<String>()
    names.addAll(current.keys)
    names.addAll(previous.keys)
    return names
        .map { name -> CategoryMove(name, (current[name] ?: 0L) - (previous[name] ?: 0L)) }
        .filter { it.deltaCentavos != 0L }
        .sortedByDescending { abs(it.deltaCentavos) }
        .take(limit)
}
