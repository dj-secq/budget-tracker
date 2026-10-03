package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Spent minus an even share of [limit] through [dayOfMonth].
 * Positive means over that share. Null when there is no cap or the month has not started.
 */
fun paceDeltaCentavos(spent: Long, limit: Long, dayOfMonth: Int, lengthOfMonth: Int): Long? {
    if (limit <= 0L || lengthOfMonth <= 0 || dayOfMonth <= 0) return null
    val day = dayOfMonth.coerceAtMost(lengthOfMonth)
    val even = BigDecimal.valueOf(limit)
        .multiply(BigDecimal.valueOf(day.toLong()))
        .divide(BigDecimal.valueOf(lengthOfMonth.toLong()), 0, RoundingMode.HALF_UP)
        .longValueExact()
    return spent - even
}
