package com.example.budgettracker.domain

import java.time.Instant
import java.time.ZoneId
import kotlin.math.min

enum class RepeatInterval {
    DAILY, WEEKLY, MONTHLY, YEARLY, SEMI_MONTHLY
}

data class OccurrencePlan(
    val dueTimestamps: List<Long>,
    val nextRunTime: Long
)

fun advanceOccurrence(
    fromMillis: Long,
    interval: RepeatInterval,
    anchorDay: Int?,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val zoned = Instant.ofEpochMilli(fromMillis).atZone(zone)
    val local = zoned.toLocalDateTime()
    val next = when (interval) {
        RepeatInterval.DAILY -> local.plusDays(1)
        RepeatInterval.WEEKLY -> local.plusWeeks(1)
        RepeatInterval.MONTHLY -> plusMonthsKeepingAnchor(local, 1, anchorDay)
        RepeatInterval.YEARLY -> plusMonthsKeepingAnchor(local, 12, anchorDay)
        RepeatInterval.SEMI_MONTHLY -> nextSemiMonthly(local)
    }
    return next.atZone(zone).toInstant().toEpochMilli()
}

/** The next 15th or last day after [local], keeping the clock time. */
private fun nextSemiMonthly(local: java.time.LocalDateTime): java.time.LocalDateTime {
    val date = local.toLocalDate()
    val lastDay = date.lengthOfMonth()
    val nextDate = when {
        date.dayOfMonth < 15 -> date.withDayOfMonth(15)
        date.dayOfMonth < lastDay -> date.withDayOfMonth(lastDay)
        else -> date.plusMonths(1).withDayOfMonth(15)
    }
    return nextDate.atTime(local.toLocalTime())
}

private fun plusMonthsKeepingAnchor(
    local: java.time.LocalDateTime,
    months: Long,
    anchorDay: Int?
): java.time.LocalDateTime {
    val anchor = (anchorDay ?: local.dayOfMonth).coerceIn(1, 31)
    val shifted = local.toLocalDate().withDayOfMonth(1).plusMonths(months)
    val day = min(anchor, shifted.lengthOfMonth())
    return shifted.withDayOfMonth(day).atTime(local.toLocalTime())
}

/**
 * Occurrences that are due at or before [now], plus the first run still in the future.
 * Never emits a future timestamp and never skips a missed period.
 * [maxOccurrences] bounds one pass; the returned [OccurrencePlan.nextRunTime] continues the rest.
 */
fun planOccurrences(
    nextRunTime: Long,
    interval: RepeatInterval,
    anchorDay: Int?,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    maxOccurrences: Int = 500
): OccurrencePlan {
    val due = ArrayList<Long>(8)
    var cursor = nextRunTime
    var guard = 0
    while (cursor <= now && guard < maxOccurrences) {
        val advanced = advanceOccurrence(cursor, interval, anchorDay, zone)
        if (advanced <= cursor) break
        due += cursor
        cursor = advanced
        guard++
    }
    return OccurrencePlan(due, cursor)
}

/** Timestamps from [dueTimestamps] that are not already stored for this rule. */
fun occurrencesToInsert(dueTimestamps: List<Long>, alreadyPosted: Set<Long>): List<Long> {
    return dueTimestamps.filter { it !in alreadyPosted }
}

/** Drops occurrences after an optional end. A null end keeps every due stamp. */
fun occurrencesOnOrBefore(dueTimestamps: List<Long>, endInclusive: Long?): List<Long> {
    if (endInclusive == null) return dueTimestamps
    return dueTimestamps.filter { it <= endInclusive }
}

/**
 * First run strictly after [now]. Missed stamps are skipped, which is what unpausing a rule should do.
 */
fun nextOpenOccurrence(
    nextRunTime: Long,
    interval: RepeatInterval,
    anchorDay: Int?,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    var cursor = nextRunTime
    var guard = 0
    while (cursor <= now && guard < 500) {
        val advanced = advanceOccurrence(cursor, interval, anchorDay, zone)
        if (advanced <= cursor) break
        cursor = advanced
        guard++
    }
    return cursor
}
