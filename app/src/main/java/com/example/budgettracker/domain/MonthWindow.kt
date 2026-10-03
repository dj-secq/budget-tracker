package com.example.budgettracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class YearMonthKey(val year: Int, val month: Int) : Comparable<YearMonthKey> {
    init {
        require(month in 1..12)
    }

    override fun compareTo(other: YearMonthKey): Int {
        val yearDiff = year.compareTo(other.year)
        return if (yearDiff != 0) yearDiff else month.compareTo(other.month)
    }

    fun plusMonths(delta: Int): YearMonthKey {
        val raw = year * 12L + (month - 1) + delta
        val newYear = Math.floorDiv(raw, 12L).toInt()
        val newMonth = Math.floorMod(raw, 12L).toInt() + 1
        return YearMonthKey(newYear, newMonth)
    }
}

data class MonthWindow(val startInclusive: Long, val endInclusive: Long)

fun monthWindow(month: Int, year: Int, zone: ZoneId = ZoneId.systemDefault()): MonthWindow {
    val start = LocalDate.of(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()
    val next = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return MonthWindow(start, next - 1)
}

fun previousMonth(month: Int, year: Int): YearMonthKey {
    return YearMonthKey(year, month).plusMonths(-1)
}

/** Material3 DatePicker millis are UTC midnight of the selected calendar day. */
fun localDateFromPickerUtc(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}

/** UTC midnight for [date], which is what the Material date picker highlights. */
fun pickerUtcMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun startOfLocalDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun endOfLocalDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
    return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
}

/** Noon keeps the picked calendar day in every offset from UTC-12 through UTC+14. */
fun localNoon(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
    return date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}

fun localDateOf(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
}

/** Six months ending at [month]/[year], oldest first. Day-of-month is never consulted. */
fun trailingMonths(month: Int, year: Int, count: Int = 6): List<YearMonthKey> {
    val end = YearMonthKey(year, month)
    return (count - 1 downTo 0).map { end.plusMonths(-it) }
}
