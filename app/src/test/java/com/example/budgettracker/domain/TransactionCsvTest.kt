package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TransactionCsvTest {
    @Test
    fun pesosUseTwoDecimalsFromCentavos() {
        assertEquals("100.50", TransactionCsv.pesos(10050L))
        assertEquals("0.01", TransactionCsv.pesos(1L))
        assertEquals("15000.00", TransactionCsv.pesos(1500000L))
        assertEquals("0.00", TransactionCsv.pesos(0L))
    }

    @Test
    fun transfersAreNotIncomeOrExpense() {
        assertEquals("TRANSFER", TransactionCsv.ledgerType("EXPENSE", "TRANSFER_OUT"))
        assertEquals("TRANSFER", TransactionCsv.ledgerType("INCOME", "TRANSFER_IN"))
        assertEquals("EXPENSE", TransactionCsv.ledgerType("EXPENSE", "NORMAL"))
        assertEquals("INCOME", TransactionCsv.ledgerType("INCOME", "NORMAL"))
        assertEquals("", TransactionCsv.ledgerType(null, null))
    }

    @Test
    fun buildQuotesCommasAndWritesPlainPesos() {
        val csv = TransactionCsv.build(
            listOf(
                CsvTransaction(
                    dateIso = "2026-10-03",
                    wallet = "Cash",
                    category = "Groceries",
                    type = "EXPENSE",
                    amountCentavos = 168000L,
                    note = "SM, \"fresh\"",
                    classification = "NEED"
                ),
                CsvTransaction(
                    dateIso = "2026-10-03",
                    wallet = "Bank",
                    category = "Deposit / Transfer In",
                    type = "TRANSFER",
                    amountCentavos = 50000L,
                    note = "move",
                    classification = "NONE"
                )
            )
        )
        val expected = TransactionCsv.HEADER + "\n" +
            "2026-10-03,Cash,Groceries,EXPENSE,1680.00,\"SM, \"\"fresh\"\"\",NEED\n" +
            "2026-10-03,Bank,Deposit / Transfer In,TRANSFER,500.00,move,NONE\n"
        assertEquals(expected, csv)
        assertFalse(csv.contains("1,680"))
    }

    @Test
    fun emptyLedgerIsHeaderOnly() {
        assertEquals("date,wallet,category,type,amount,note,classification\n", TransactionCsv.build(emptyList()))
    }
}
