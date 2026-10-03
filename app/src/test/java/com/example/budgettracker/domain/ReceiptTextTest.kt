package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptTextTest {

    @Test
    fun totalLineBeatsTheLargestNumberAndSkipsSubtotal() {
        val text = """
            SM SUPERMARKET
            Milk 1,500.00
            Subtotal 1,500.00
            VAT 180.00
            TOTAL 1,680.00
            Change 320.00
        """.trimIndent()
        assertEquals(1680.0, ReceiptText.totalPesos(text)!!, 0.001)
        assertEquals("SM SUPERMARKET", ReceiptText.merchantLine(text))
    }

    @Test
    fun amountDueOnTheNextLine() {
        val text = """
            Jollibee
            Amount Due
            245.50
        """.trimIndent()
        assertEquals(245.5, ReceiptText.totalPesos(text)!!, 0.001)
        assertEquals("Jollibee", ReceiptText.merchantLine(text))
    }

    @Test
    fun noTotalLineMeansNoAmount() {
        assertNull(ReceiptText.totalPesos("Item A 99.00\nItem B 10.00"))
        assertEquals("", ReceiptText.merchantLine("   \n12"))
    }

    @Test
    fun amountInputDropsWholePesos() {
        assertEquals("1680", ReceiptText.amountInput(1680.0))
        assertEquals("245.50", ReceiptText.amountInput(245.5))
        assertEquals("", ReceiptText.amountInput(Double.NaN))
    }
}
