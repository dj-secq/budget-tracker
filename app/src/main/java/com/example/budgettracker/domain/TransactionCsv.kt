package com.example.budgettracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class ParsedCsv(
    val headerOk: Boolean,
    val rows: List<CsvTransaction>,
    val rejected: Int
)

data class CsvApply(
    val fresh: List<CsvTransaction>,
    val skipped: Int
)

/** One ledger row for a CSV export. Amounts stay in centavos until [TransactionCsv.pesos]. */
data class CsvTransaction(
    val dateIso: String,
    val wallet: String,
    val category: String,
    val type: String,
    val amountCentavos: Long,
    val note: String,
    val classification: String
)

object TransactionCsv {
    const val HEADER = "date,wallet,category,type,amount,note,classification"
    private val HEADER_FIELDS = HEADER.split(",")

    /** Transfers are their own type. Income and expense stay the category type. */
    fun ledgerType(categoryType: String?, role: String?): String {
        return when (role) {
            "TRANSFER_IN", "TRANSFER_OUT" -> "TRANSFER"
            else -> categoryType ?: ""
        }
    }

    fun pesos(centavos: Long): String {
        return BigDecimal.valueOf(centavos)
            .movePointLeft(2)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun build(rows: List<CsvTransaction>): String {
        val body = rows.joinToString(separator = "\n") { row ->
            listOf(
                row.dateIso,
                row.wallet,
                row.category,
                row.type,
                pesos(row.amountCentavos),
                row.note,
                row.classification
            ).joinToString(separator = ",") { cell(it) }
        }
        return if (body.isEmpty()) "$HEADER\n" else "$HEADER\n$body\n"
    }

    fun parse(text: String): ParsedCsv {
        val records = readRecords(text.removePrefix("\uFEFF"))
        if (records.isEmpty()) return ParsedCsv(headerOk = false, rows = emptyList(), rejected = 0)
        if (records.first().map { it.trim() } != HEADER_FIELDS) {
            return ParsedCsv(headerOk = false, rows = emptyList(), rejected = 0)
        }
        val rows = ArrayList<CsvTransaction>()
        var rejected = 0
        for (record in records.drop(1)) {
            if (record.all { it.isBlank() }) continue
            val row = parseRow(record)
            if (row == null) rejected++ else rows += row
        }
        return ParsedCsv(headerOk = true, rows = rows, rejected = rejected)
    }

    /** Rows whose identity is not already in [existingKeys]. A repeated row in [rows] counts once. */
    fun newRows(existingKeys: Set<String>, rows: List<CsvTransaction>): CsvApply {
        val seen = existingKeys.toMutableSet()
        val fresh = ArrayList<CsvTransaction>()
        var skipped = 0
        for (row in rows) {
            if (!seen.add(identity(row))) skipped++ else fresh += row
        }
        return CsvApply(fresh, skipped)
    }

    fun identity(row: CsvTransaction): String {
        return listOf(
            row.dateIso,
            row.wallet.trim().lowercase(),
            row.category.trim().lowercase(),
            row.type,
            row.amountCentavos.toString(),
            row.note,
            row.classification
        ).joinToString("\u0000")
    }

    private fun parseRow(record: List<String>): CsvTransaction? {
        if (record.size != HEADER_FIELDS.size) return null
        val date = try {
            LocalDate.parse(record[0].trim())
        } catch (_: DateTimeParseException) {
            return null
        }
        val wallet = record[1].trim()
        val category = record[2].trim()
        if (wallet.isEmpty() || category.isEmpty()) return null
        val type = record[3].trim()
        if (type != "EXPENSE" && type != "INCOME" && type != "TRANSFER") return null
        val amount = Money.parsePesos(record[4].trim()) ?: return null
        if (amount <= 0L) return null
        val classification = record[6].trim().ifBlank { "NONE" }
        if (classification != "NEED" && classification != "WANT" && classification != "SAVING" && classification != "NONE") {
            return null
        }
        return CsvTransaction(
            dateIso = date.toString(),
            wallet = wallet,
            category = category,
            type = type,
            amountCentavos = amount,
            note = record[5],
            classification = classification
        )
    }

    private fun readRecords(text: String): List<List<String>> {
        val records = ArrayList<List<String>>()
        val row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (quoted) {
                if (char == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        cell.append('"')
                        index += 2
                        continue
                    }
                    quoted = false
                    index++
                    continue
                }
                cell.append(char)
                index++
                continue
            }
            when (char) {
                '"' -> {
                    quoted = true
                    index++
                }
                ',' -> {
                    row += cell.toString()
                    cell.clear()
                    index++
                }
                '\n', '\r' -> {
                    row += cell.toString()
                    cell.clear()
                    records += row.toList()
                    row.clear()
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index += 2 else index++
                }
                else -> {
                    cell.append(char)
                    index++
                }
            }
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            records += row.toList()
        }
        return records
    }

    private fun cell(value: String): String {
        val mustQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (mustQuote) "\"$escaped\"" else escaped
    }
}
