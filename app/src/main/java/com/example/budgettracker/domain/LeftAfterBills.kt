package com.example.budgettracker.domain

import java.time.LocalDate
import java.time.ZoneId

enum class BillDirection {
    LEAVES, ARRIVES
}

data class PlannedRule(
    val name: String,
    val amountCentavos: Long,
    val nextRunTime: Long,
    val interval: RepeatInterval,
    val anchorDay: Int?,
    val paused: Boolean,
    val endDate: Long?,
    val countsAsExpense: Boolean,
    val countsAsIncome: Boolean
)

data class DueDebt(
    val name: String,
    val dueDate: Long?,
    val owedCentavos: Long,
    val borrowed: Boolean
)

data class UpcomingBill(
    val name: String,
    val whenMillis: Long,
    val amountCentavos: Long,
    val direction: BillDirection,
    val opensDebt: Boolean
)

data class BillsOutlook(
    val leftCentavos: Long,
    val upcoming: List<UpcomingBill>
)

private const val MAX_STAMPS_PER_RULE = 31
private const val MAX_SCHEDULE_STEPS = 500

/**
 * Money still in included wallets after expense bills and borrowed debts due
 * from the start of [today] through the end of that calendar month.
 * Stamps before [today] are omitted: catch-up posts them and the balance already drops.
 * Income and money lent are listed and stay out of [BillsOutlook.leftCentavos].
 */
fun leftAfterBills(
    includedBalanceCentavos: Long,
    rules: List<PlannedRule>,
    debts: List<DueDebt>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): BillsOutlook {
    val windowStart = startOfLocalDay(today, zone)
    val windowEnd = endOfLocalDay(today.withDayOfMonth(today.lengthOfMonth()), zone)
    val upcoming = ArrayList<UpcomingBill>()
    for (rule in rules) {
        if (rule.paused || rule.amountCentavos <= 0L) continue
        if (rule.endDate != null && rule.endDate < windowStart) continue
        val direction = when {
            rule.countsAsExpense -> BillDirection.LEAVES
            rule.countsAsIncome -> BillDirection.ARRIVES
            else -> continue
        }
        for (stamp in stampsInWindow(rule, windowStart, windowEnd, zone)) {
            upcoming += UpcomingBill(
                name = rule.name,
                whenMillis = stamp,
                amountCentavos = rule.amountCentavos,
                direction = direction,
                opensDebt = false
            )
        }
    }
    for (debt in debts) {
        val due = debt.dueDate ?: continue
        if (debt.owedCentavos <= 0L || due !in windowStart..windowEnd) continue
        upcoming += UpcomingBill(
            name = debt.name,
            whenMillis = due,
            amountCentavos = debt.owedCentavos,
            direction = if (debt.borrowed) BillDirection.LEAVES else BillDirection.ARRIVES,
            opensDebt = true
        )
    }
    upcoming.sortWith(compareBy({ it.whenMillis }, { it.name }))
    val leaving = upcoming.sumOf { bill ->
        if (bill.direction == BillDirection.LEAVES) bill.amountCentavos else 0L
    }
    return BillsOutlook(
        leftCentavos = includedBalanceCentavos - leaving,
        upcoming = upcoming
    )
}

private fun stampsInWindow(
    rule: PlannedRule,
    windowStart: Long,
    windowEnd: Long,
    zone: ZoneId
): List<Long> {
    val stamps = ArrayList<Long>(8)
    var cursor = rule.nextRunTime
    var steps = 0
    while (cursor < windowStart && steps < MAX_SCHEDULE_STEPS) {
        val advanced = advanceOccurrence(cursor, rule.interval, rule.anchorDay, zone)
        if (advanced <= cursor) return emptyList()
        cursor = advanced
        steps++
    }
    while (cursor <= windowEnd && stamps.size < MAX_STAMPS_PER_RULE && steps < MAX_SCHEDULE_STEPS) {
        if (rule.endDate != null && cursor > rule.endDate) break
        if (cursor >= windowStart) stamps += cursor
        val advanced = advanceOccurrence(cursor, rule.interval, rule.anchorDay, zone)
        if (advanced <= cursor) break
        cursor = advanced
        steps++
    }
    return stamps
}
