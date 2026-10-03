package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Average of finished-month spending, in centavos. All-zero history has no suggestion. */
fun suggestedBaseCentavos(monthlySpent: List<Long>): Long? {
    if (monthlySpent.isEmpty() || monthlySpent.all { it == 0L }) return null
    return BigDecimal.valueOf(monthlySpent.sum())
        .divide(BigDecimal.valueOf(monthlySpent.size.toLong()), 0, RoundingMode.HALF_UP)
        .longValueExact()
}

/** Up to [count] finished months ending at [today], oldest first. An open month is skipped. */
fun lastFinishedMonths(today: LocalDate, count: Int = 3): List<YearMonthKey> {
    if (count <= 0) return emptyList()
    val found = ArrayList<YearMonthKey>(count)
    var cursor = YearMonthKey(today.year, today.monthValue)
    var guard = 0
    while (found.size < count && guard < 36) {
        if (!monthIsOpen(cursor.month, cursor.year, today)) found += cursor
        cursor = cursor.plusMonths(-1)
        guard++
    }
    return found.asReversed()
}
