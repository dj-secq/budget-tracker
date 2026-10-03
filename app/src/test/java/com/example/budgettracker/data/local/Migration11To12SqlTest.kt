package com.example.budgettracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Applies the ALTER statements from Migration11To12.kt to a v11-shaped database.
 * The statements are read from that source so the test cannot drift from a copied script.
 */
class Migration11To12SqlTest {

    @Test
    fun addsPauseEndDateAndCategoryTimestampIndex() {
        val root = moduleRoot()
        val migration = File(root, "src/main/java/com/example/budgettracker/data/local/Migration11To12.kt")
        val statements = kotlinExecSql(migration.readText())
        assertEquals(3, statements.size)
        val db = File.createTempFile("budget-v11-", ".db")
        db.deleteOnExit()
        val script = buildString {
            appendLine(".bail on")
            appendLine("PRAGMA foreign_keys = ON;")
            appendLine(
                """
                CREATE TABLE recurring_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    amount INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    classification TEXT NOT NULL,
                    frequency TEXT NOT NULL,
                    startDate INTEGER NOT NULL,
                    nextRunTime INTEGER NOT NULL,
                    anchorDay INTEGER NOT NULL
                );
                INSERT INTO recurring_transactions
                    (id, accountId, categoryId, amount, note, classification, frequency, startDate, nextRunTime, anchorDay)
                    VALUES (1, 1, 1, 50000, 'rent', 'NEED', 'MONTHLY', 1, 1, 31);
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY NOT NULL,
                    categoryId INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                );
                """.trimIndent()
            )
            statements.forEach { sql ->
                append(sql.trim().trimEnd(';'))
                appendLine(";")
            }
            appendLine(".mode list")
            appendLine(".separator |")
            appendLine("SELECT 'rule', paused, ifnull(endDate, 'null') FROM recurring_transactions;")
            appendLine("SELECT 'col', name, \"notnull\", ifnull(dflt_value, 'null') FROM pragma_table_info('recurring_transactions') WHERE name IN ('paused', 'endDate') ORDER BY name;")
            appendLine("SELECT 'idx', name FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_categoryId_timestamp';")
        }
        val output = runSqlite(db, script)
        val rows = output.lineSequence().map { it.split('|') }.filter { it.firstOrNull()?.isNotEmpty() == true }.toList()
        assertEquals(listOf("rule", "0", "null"), rows.first { it.first() == "rule" })
        val columns = rows.filter { it.first() == "col" }.associate { it[1] to it }
        assertEquals(listOf("endDate", "0", "null"), columns.getValue("endDate").drop(1))
        assertEquals(listOf("paused", "1", "0"), columns.getValue("paused").drop(1))
        assertTrue(rows.any { it.first() == "idx" && it.getOrNull(1) == "index_transactions_categoryId_timestamp" })
    }
}
