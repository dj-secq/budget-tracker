package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Philippine peso amounts stored as integer centavos. */
object Money {
    private val locale = Locale("en", "PH")

    fun fromDoublePesos(value: Double): Long {
        if (!value.isFinite()) return 0L
        return BigDecimal.valueOf(value)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun parsePesos(text: String): Long? {
        val cleaned = text.trim()
            .replace("₱", "")
            .replace("PHP", "", ignoreCase = true)
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return null
        val decimal = try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            return null
        }
        if (decimal.signum() < 0) return null
        return try {
            decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun format(centavos: Long): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return format.format(centavos.toBigDecimal().movePointLeft(2))
    }

    /** Peso string for text fields. Whole pesos stay without a decimal. */
    fun toInputString(centavos: Long): String {
        val pesos = centavos.toBigDecimal().movePointLeft(2).stripTrailingZeros()
        return if (pesos.scale() <= 0) pesos.toPlainString() else pesos.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
    }
}
