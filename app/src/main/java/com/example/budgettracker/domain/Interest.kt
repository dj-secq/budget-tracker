package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * What an open debt counts toward "Owed to you" or "You owe".
 * A settled debt counts as zero. Interest is not added again after settlement.
 */
fun debtOwedCentavos(
    principal: Long,
    annualPercent: Double,
    startMillis: Long,
    asOf: Long,
    settled: Boolean,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    if (settled) return 0L
    return principal + interestCentavos(principal, annualPercent, startMillis, asOf, zone)
}

/**
 * Amount shown on the row. Settled rows use the posted payment, or the principal
 * when an older paid debt has no linked payment.
 */
fun debtDisplayCentavos(
    principal: Long,
    annualPercent: Double,
    startMillis: Long,
    asOf: Long,
    settled: Boolean,
    settlementCentavos: Long?,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    if (settled) return settlementCentavos ?: principal
    return principal + interestCentavos(principal, annualPercent, startMillis, asOf, zone)
}

/** Blank means no interest. Negative and non-numeric text are rejected. */
fun parseAnnualPercent(text: String): Double? {
    val cleaned = text.trim().removeSuffix("%").trim()
    if (cleaned.isEmpty()) return 0.0
    val value = cleaned.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value < 0.0) return null
    return value
}

/** Simple interest in centavos: principal * annualPercent * days / (365 * 100). */
fun interestCentavos(
    principal: Long,
    annualPercent: Double,
    startMillis: Long,
    endMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    if (principal <= 0L || annualPercent <= 0.0 || endMillis <= startMillis) return 0L
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
    )
    if (days <= 0L) return 0L
    return BigDecimal.valueOf(principal)
        .multiply(BigDecimal.valueOf(annualPercent))
        .multiply(BigDecimal.valueOf(days))
        .divide(BigDecimal.valueOf(36_500L), 0, RoundingMode.HALF_UP)
        .longValueExact()
}
