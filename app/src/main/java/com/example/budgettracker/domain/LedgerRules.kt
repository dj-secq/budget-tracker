package com.example.budgettracker.domain

enum class CategoryKind { INCOME, EXPENSE }

enum class CategoryUse { NORMAL, TRANSFER_IN, TRANSFER_OUT }

fun countsAsIncome(kind: CategoryKind, use: CategoryUse): Boolean {
    return kind == CategoryKind.INCOME && use == CategoryUse.NORMAL
}

fun countsAsExpense(kind: CategoryKind, use: CategoryUse): Boolean {
    return kind == CategoryKind.EXPENSE && use == CategoryUse.NORMAL
}

fun signedDelta(kind: CategoryKind, amount: Long): Long {
    return if (kind == CategoryKind.INCOME) amount else -amount
}

data class CapCheck(
    val excess: Long,
    val strict: Boolean
) {
    val exceeded: Boolean get() = excess > 0L
    val blocked: Boolean get() = exceeded && strict
}

/**
 * A base of 0 with no carry means "no cap".
 * A base of 0 with a positive carry is still a cap.
 */
fun categoryCap(spent: Long, adding: Long, base: Long, rollover: Long, strict: Boolean): CapCheck {
    val effective = base + rollover
    if (effective <= 0L) return CapCheck(0L, strict)
    val excess = (spent + adding - effective).coerceAtLeast(0L)
    return CapCheck(excess, strict)
}

fun bucketCap(spent: Long, adding: Long, income: Long, percent: Int, strict: Boolean): CapCheck {
    if (income <= 0L || percent <= 0) return CapCheck(0L, strict)
    val limit = income * percent / 100L
    val excess = (spent + adding - limit).coerceAtLeast(0L)
    return CapCheck(excess, strict)
}
