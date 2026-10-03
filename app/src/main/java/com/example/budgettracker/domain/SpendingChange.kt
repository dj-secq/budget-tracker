package com.example.budgettracker.domain

import kotlin.math.roundToInt

/**
 * Percent by which this month's spending exceeds last month.
 * A previous month of ₱0 is not an increase. At or under 20% returns null.
 */
fun spendingIncreasePercent(currentCentavos: Long, previousCentavos: Long): Int? {
    if (previousCentavos <= 0L || currentCentavos <= 0L) return null
    val ratio = currentCentavos.toDouble() / previousCentavos.toDouble()
    if (ratio <= 1.2) return null
    return ((ratio - 1.0) * 100.0).roundToInt().coerceAtLeast(1)
}

/** Percent by which this month's spending is under last month. Within 10% returns null. */
fun spendingDecreasePercent(currentCentavos: Long, previousCentavos: Long): Int? {
    if (previousCentavos <= 0L) return null
    if (currentCentavos.toDouble() >= previousCentavos * 0.9) return null
    val ratio = currentCentavos.toDouble() / previousCentavos.toDouble()
    return ((1.0 - ratio) * 100.0).roundToInt().coerceAtLeast(1)
}

enum class SpendingTrend { UP, DOWN, NONE }

/** An open month is not compared with a finished month. Thresholds match the percent helpers. */
fun spendingTrend(currentCentavos: Long, previousCentavos: Long, monthOpen: Boolean): SpendingTrend {
    if (monthOpen) return SpendingTrend.NONE
    if (spendingIncreasePercent(currentCentavos, previousCentavos) != null) return SpendingTrend.UP
    if (spendingDecreasePercent(currentCentavos, previousCentavos) != null) return SpendingTrend.DOWN
    return SpendingTrend.NONE
}
