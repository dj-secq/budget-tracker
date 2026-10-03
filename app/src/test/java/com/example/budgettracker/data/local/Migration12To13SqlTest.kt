package com.example.budgettracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Applies the ALTER from Migration12To13.kt to a transactions table that has no split column. */
class Migration12To13SqlTest {

    @Test
    fun addsNullableSplitGroup() {
        val root = moduleRoot()
        val migration = File(root, "src/main/java/com/example/budgettracker/data/local/Migration12To13.kt")
        val statements = kotlinExecSql(migration.readText())
        assertEquals(1, statements.size)
        val db = File.createTempFile("budget-v12-", ".db")
        db.deleteOnExit()
        val script = buildString {
            appendLine(".bail on")
            appendLine("PRAGMA foreign_keys = ON;")
            appendLine(
                """
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY NOT NULL,
                    amount INTEGER NOT NULL
                );
                INSERT INTO transactions (id, amount) VALUES (1, 500);
                """.trimIndent()
            )
            statements.forEach { sql ->
                append(sql.trim().trimEnd(';'))
                appendLine(";")
            }
            appendLine(".mode list")
            appendLine(".separator |")
            appendLine("SELECT 'row', id, ifnull(splitGroupId, 'null') FROM transactions;")
            appendLine("SELECT 'col', name, \"notnull\", ifnull(dflt_value, 'null') FROM pragma_table_info('transactions') WHERE name = 'splitGroupId';")
        }
        val output = runSqlite(db, script)
        val rows = output.lineSequence().map { it.split('|') }.filter { it.firstOrNull()?.isNotEmpty() == true }.toList()
        assertEquals(listOf("row", "1", "null"), rows.first { it.first() == "row" })
        assertEquals(listOf("col", "splitGroupId", "0", "null"), rows.first { it.first() == "col" })
    }
}
