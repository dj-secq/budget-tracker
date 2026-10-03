package com.example.budgettracker.domain

/**
 * Carry into [target] is the previous month's unspent effective budget.
 * A deficit in an older month does not reduce a later month's own leftover.
 * Months with no assignment and no spending keep the previous carry.
 */
fun rolloverCentavos(
    bases: Map<YearMonthKey, Long>,
    spent: Map<YearMonthKey, Long>,
    target: YearMonthKey
): Long {
    val earliest = (bases.keys + spent.keys).filter { it < target }.minOrNull() ?: return 0L
    var cursor = earliest
    var carry = 0L
    while (cursor < target) {
        val base = bases[cursor] ?: 0L
        val spentAmount = spent[cursor] ?: 0L
        carry = (base + carry - spentAmount).coerceAtLeast(0L)
        cursor = cursor.plusMonths(1)
    }
    return carry
}

/**
 * The assign dialog stores this value as the month's base.
 * Rollover is derived and must not be added before the write.
 */
fun budgetBaseToStore(editedBase: Long): Long = editedBase
