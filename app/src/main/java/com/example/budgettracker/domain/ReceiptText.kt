package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pulls a receipt total and a merchant line out of OCR text.
 * The total is the amount on the last line that says total, amount due, or balance due.
 * Subtotal lines are ignored. When no total line exists, the scan has no amount.
 */
object ReceiptText {
    private val amountPattern = Regex("""(?<!\d)(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{2}))?(?!\d)""")
    private val totalLine = Regex("""(?i)\b(grand\s+total|amount\s+due|balance\s+due|total\s+due|total\s+amount|total)\b""")
    private val subtotalLine = Regex("""(?i)sub\s*total""")
    private val skipMerchant = Regex("""(?i)^(official\s+receipt|sales\s+invoice|invoice|receipt|welcome|thank\s+you|tel|tin|vat|or\s*#|si\s*#).*$""")
    private val dateLike = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")

    fun totalPesos(text: String): Double? {
        val lines = text.lines()
        var chosen: Double? = null
        for (index in lines.indices) {
            val line = lines[index]
            if (!totalLine.containsMatchIn(line) || subtotalLine.containsMatchIn(line)) continue
            val onLine = amounts(line)
            val amount = onLine.lastOrNull()
                ?: lines.getOrNull(index + 1)?.let { next -> amounts(next).lastOrNull() }
            if (amount != null) chosen = amount
        }
        return chosen
    }

    fun merchantLine(text: String): String {
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.length < 2) continue
            if (skipMerchant.matches(line)) continue
            val letters = line.count { it.isLetter() }
            if (letters < 3) continue
            if (dateLike.containsMatchIn(line) && letters < 4) continue
            if (totalLine.containsMatchIn(line)) continue
            return line.take(80)
        }
        return ""
    }

    /** Stable peso text for [Money.parsePesos]. Whole pesos drop the decimal. */
    fun amountInput(pesos: Double): String {
        if (!pesos.isFinite() || pesos <= 0.0) return ""
        val scaled = BigDecimal.valueOf(pesos).setScale(2, RoundingMode.HALF_UP)
        return if (scaled.stripTrailingZeros().scale() <= 0) {
            scaled.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
        } else {
            scaled.toPlainString()
        }
    }

    private fun amounts(line: String): List<Double> {
        return amountPattern.findAll(line).mapNotNull { match ->
            val whole = match.groupValues[1].replace(",", "")
            val fraction = match.groupValues[2]
            val text = if (fraction.isEmpty()) whole else "$whole.$fraction"
            text.toDoubleOrNull()
        }.toList()
    }
}
