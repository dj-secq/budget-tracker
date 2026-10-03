package com.example.budgettracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs the migration statements from Migration10To11.kt against a real v10 SQLite file.
 * The statements are read from that source so this test cannot drift from a copied script.
 */
class Migration10To11SqlTest {

    @Test
    fun generatedRoomCreatesMatchTheMigration() {
        val root = moduleRoot()
        val impl = File(root, "build/generated/ksp/debug/kotlin/com/example/budgettracker/data/local/AppDatabase_Impl.kt")
        val migration = File(root, "src/main/java/com/example/budgettracker/data/local/Migration10To11.kt")
        assertTrue("KSP output missing: ${impl.path}", impl.isFile)
        val generated = quotedSql(impl.readText())
            .filter { it.startsWith("CREATE ") && !it.contains("room_master_table") }
        val migrated = kotlinExecSql(migration.readText()).map(::normalizeSql)
        assertTrue(generated.isNotEmpty())
        for (statement in generated) {
            val normalizedRaw = normalizeSql(statement)
            if (normalizedRaw.contains("categoryId, timestamp")) continue
            val normalized = stripPhase3Columns(normalizedRaw)
            val table = tableName(normalized)
            val expected = when (table) {
                "accounts" -> normalized.replaceFirst(" accounts (", " hold_accounts (")
                "categories" -> normalized.replaceFirst(" categories (", " hold_categories (")
                "savings_goals" -> normalized.replaceFirst(" savings_goals (", " hold_savings_goals (")
                "debts" -> normalized.replaceFirst(" debts (", " hold_debts (")
                else -> normalized
            }
            assertTrue("Migration SQL is missing:\n$expected", migrated.any { it == expected })
        }
    }

    @Test
    fun migratesARealV10Database() {
        val root = moduleRoot()
        val migration = File(root, "src/main/java/com/example/budgettracker/data/local/Migration10To11.kt")
        val statements = kotlinExecSql(migration.readText())
        val start = LocalDate.of(2026, 1, 31).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val db = File.createTempFile("budget-v10-", ".db")
        db.deleteOnExit()
        val script = buildString {
            appendLine(".bail on")
            appendLine("PRAGMA foreign_keys = ON;")
            appendLine(v10Schema())
            appendLine(v10Rows(start))
            statements.forEach { sql ->
                val trimmed = sql.trim().trimEnd(';')
                append(trimmed)
                appendLine(";")
            }
            appendLine(".mode list")
            appendLine(".separator |")
            appendLine("SELECT 'acct', id, name, balance, openingBalance, colorArgb, includeInTotalBalance, type FROM accounts ORDER BY id;")
            appendLine("SELECT 'cat', name, role FROM categories ORDER BY id;")
            appendLine("SELECT 'limit', id, categoryId, assignedAmount, month, year FROM budget_limits ORDER BY id;")
            appendLine("SELECT 'tx', id, accountId, amount FROM transactions ORDER BY id;")
            appendLine("SELECT 'goal', targetAmount, currentAmount, ifnull(contributionAmount, 'null') FROM savings_goals ORDER BY id;")
            appendLine("SELECT 'recur', accountId, amount, anchorDay FROM recurring_transactions ORDER BY id;")
            appendLine("SELECT 'debt', amount, ifnull(originTransactionId, 'null'), ifnull(settlementTransactionId, 'null') FROM debts;")
            appendLine("SELECT 'tmpl', amount FROM transaction_templates;")
            appendLine("SELECT 'holds', count(*) FROM sqlite_master WHERE name LIKE 'hold_%';")
            appendLine("SELECT 'idx', name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%' ORDER BY name;")
        }
        val output = runSqlite(db, script)
        val rows = output.lineSequence().map { it.split('|') }.filter { it.firstOrNull()?.isNotEmpty() == true }.toList()

        fun section(tag: String) = rows.filter { it.first() == tag }

        val accounts = section("acct").associate { it[2] to it }
        assertEquals(listOf("30", "-5970"), accounts.getValue("Cash").subList(3, 5))
        assertEquals(listOf("1500000", "1500000"), accounts.getValue("Bank").subList(3, 5))
        val recovered = accounts.getValue("Recovered wallet")
        assertEquals("0", recovered[3])
        assertEquals("1000", recovered[4])
        assertEquals(0xFF6B7280.toInt().toString(), recovered[5])
        assertEquals("0", recovered[6])
        assertEquals("CHECKING", recovered[7])

        val roles = section("cat").associate { it[1] to it[2] }
        assertEquals("TRANSFER_OUT", roles.getValue("Withdraw / Transfer Out"))
        assertEquals("TRANSFER_IN", roles.getValue("Deposit / Transfer In"))
        assertEquals("NORMAL", roles.getValue("Groceries"))

        val limits = section("limit")
        assertEquals(2, limits.size)
        val kept = limits.single { it[2] == "1" && it[4] == "1" && it[5] == "2026" }
        assertEquals("9", kept[1])
        assertEquals("25050", kept[3])
        assertEquals("8000", limits.single { it[4] == "2" }[3])

        val txs = section("tx")
        assertEquals(listOf("1", "1", "10000"), txs[0].drop(1))
        assertEquals(listOf("2", "1", "4000"), txs[1].drop(1))
        assertEquals("1000", txs[2][3])
        assertEquals(recovered[1], txs[2][2])

        assertEquals(listOf("100000", "1050", "null"), section("goal")[0].drop(1))
        assertEquals(listOf("200000", "0", "2525"), section("goal")[1].drop(1))

        val recurring = section("recur")
        assertEquals(1, recurring.size)
        assertEquals(listOf("1", "500", "31"), recurring.single().drop(1))

        assertEquals(listOf("2050", "null", "null"), section("debt").single().drop(1))
        assertEquals(listOf("350"), section("tmpl").single().drop(1))
        assertEquals(listOf("0"), section("holds").single().drop(1))

        val indexes = section("idx").map { it[1] }.toSet()
        assertTrue(indexes.contains("index_budget_limits_categoryId_month_year"))
        assertTrue(indexes.contains("index_transactions_recurringId_timestamp"))
        assertTrue(indexes.contains("index_transactions_categoryId"))
        assertTrue(indexes.contains("index_transactions_accountId"))
        assertTrue(indexes.contains("index_transactions_timestamp"))
        assertTrue(indexes.contains("index_recurring_transactions_categoryId"))
        assertTrue(indexes.contains("index_recurring_transactions_accountId"))
        assertTrue(indexes.contains("index_transaction_templates_categoryId"))
        assertTrue(indexes.contains("index_transaction_templates_accountId"))
    }
}

internal fun moduleRoot(): File {
    val candidates = listOf(File("."), File("app"))
    return candidates.firstOrNull {
        File(it, "src/main/java/com/example/budgettracker/data/local/Migration10To11.kt").isFile
    } ?: error("Cannot find the app module from ${File(".").absolutePath}")
}

private fun v10Schema(): String = """
    CREATE TABLE accounts (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        balance REAL NOT NULL,
        colorArgb INTEGER NOT NULL,
        includeInTotalBalance INTEGER NOT NULL
    );
    CREATE TABLE categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        colorArgb INTEGER NOT NULL,
        iconName TEXT
    );
    CREATE TABLE budget_limits (
        id INTEGER PRIMARY KEY NOT NULL,
        categoryId INTEGER NOT NULL,
        assignedAmount REAL NOT NULL,
        month INTEGER NOT NULL,
        year INTEGER NOT NULL
    );
    CREATE TABLE transactions (
        id INTEGER PRIMARY KEY NOT NULL,
        accountId INTEGER NOT NULL,
        categoryId INTEGER NOT NULL,
        amount REAL NOT NULL,
        timestamp INTEGER NOT NULL,
        note TEXT NOT NULL,
        payeeOrPayer TEXT,
        classification TEXT NOT NULL
    );
    CREATE TABLE savings_goals (
        id INTEGER PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        targetAmount REAL NOT NULL,
        currentAmount REAL NOT NULL,
        iconName TEXT,
        targetDate INTEGER,
        contributionFrequency TEXT,
        contributionAmount REAL
    );
    CREATE TABLE recurring_transactions (
        id INTEGER PRIMARY KEY NOT NULL,
        accountId INTEGER NOT NULL,
        categoryId INTEGER NOT NULL,
        amount REAL NOT NULL,
        note TEXT NOT NULL,
        classification TEXT NOT NULL,
        frequency TEXT NOT NULL,
        startDate INTEGER NOT NULL,
        nextRunTime INTEGER NOT NULL
    );
    CREATE TABLE debts (
        id INTEGER PRIMARY KEY NOT NULL,
        personName TEXT NOT NULL,
        amount REAL NOT NULL,
        type TEXT NOT NULL,
        date INTEGER NOT NULL,
        isPaid INTEGER NOT NULL,
        note TEXT NOT NULL,
        dueDate INTEGER,
        interestRate REAL NOT NULL,
        accountId INTEGER
    );
    CREATE TABLE transaction_templates (
        id INTEGER PRIMARY KEY NOT NULL,
        templateName TEXT NOT NULL,
        amount REAL NOT NULL,
        categoryId INTEGER NOT NULL,
        accountId INTEGER NOT NULL,
        note TEXT NOT NULL,
        transactionType TEXT NOT NULL,
        classification TEXT NOT NULL
    );
""".trimIndent()

private fun v10Rows(start: Long): String = """
    INSERT INTO accounts (id, name, type, balance, colorArgb, includeInTotalBalance) VALUES
        (1, 'Cash', 'CHECKING', 0.1 + 0.2, 1, 1),
        (2, 'Bank', 'SAVINGS', 15000.0, 2, 1);
    INSERT INTO categories (id, name, type, colorArgb, iconName) VALUES
        (1, 'Groceries', 'EXPENSE', 3, NULL),
        (2, 'Salary', 'INCOME', 4, NULL),
        (3, 'Withdraw / Transfer Out', 'EXPENSE', 5, NULL),
        (4, 'Deposit / Transfer In', 'INCOME', 6, NULL);
    INSERT INTO budget_limits (id, categoryId, assignedAmount, month, year) VALUES
        (5, 1, 100.0, 1, 2026),
        (9, 1, 250.5, 1, 2026),
        (10, 1, 80.0, 2, 2026);
    INSERT INTO transactions (id, accountId, categoryId, amount, timestamp, note, payeeOrPayer, classification) VALUES
        (1, 1, 2, 100.0, $start, 'pay', NULL, 'NONE'),
        (2, 1, 1, 40.0, $start, 'food', NULL, 'NEED'),
        (3, 99, 1, 10.0, $start, 'orphan', NULL, 'NEED');
    INSERT INTO savings_goals (id, name, targetAmount, currentAmount, iconName, targetDate, contributionFrequency, contributionAmount) VALUES
        (1, 'Trip', 1000.0, 10.5, NULL, NULL, NULL, NULL),
        (2, 'Emergency', 2000.0, 0.0, NULL, NULL, 'Monthly', 25.25);
    INSERT INTO recurring_transactions (id, accountId, categoryId, amount, note, classification, frequency, startDate, nextRunTime) VALUES
        (1, 1, 1, 5.0, 'weekly', 'NONE', 'MONTHLY', $start, $start),
        (2, 404, 1, 9.0, 'missing wallet', 'NONE', 'MONTHLY', $start, $start);
    INSERT INTO debts (id, personName, amount, type, date, isPaid, note, dueDate, interestRate, accountId) VALUES
        (1, 'Ana', 20.5, 'LENT', $start, 0, '', NULL, 10.0, 1);
    INSERT INTO transaction_templates (id, templateName, amount, categoryId, accountId, note, transactionType, classification) VALUES
        (1, 'Coffee', 3.5, 1, 1, '', 'EXPENSE', 'WANT'),
        (2, 'Gone', 1.0, 1, 404, '', 'EXPENSE', 'WANT');
""".trimIndent()

private fun quotedSql(source: String): List<String> {
    val pattern = Regex("""execSQL\("((?:\\.|[^"\\])*)"\)""")
    return pattern.findAll(source).map { it.groupValues[1] }.toList()
}

internal fun kotlinExecSql(source: String): List<String> {
    val recovered = 0xFF6B7280.toInt().toString()
    val statements = ArrayList<String>()
    var cursor = 0
    val needle = "execSQL("
    while (true) {
        val at = source.indexOf(needle, cursor)
        if (at < 0) return statements
        var index = at + needle.length
        while (index < source.length && source[index].isWhitespace()) index++
        val (sql, next) = readKotlinString(source, index)
        statements += sql.replace("\$recoveredColor", recovered)
        cursor = next
    }
}

internal fun readKotlinString(source: String, start: Int): Pair<String, Int> {
    if (source.startsWith("\"\"\"", start)) {
        val end = source.indexOf("\"\"\"", start + 3)
        check(end > start) { "Unclosed triple string at $start" }
        return source.substring(start + 3, end).trimIndent() to end + 3
    }
    check(source[start] == '"') { "Expected a string at $start" }
    val text = StringBuilder()
    var index = start + 1
    while (index < source.length) {
        val char = source[index]
        if (char == '\\') {
            text.append(source[index + 1])
            index += 2
            continue
        }
        if (char == '"') return text.toString() to index + 1
        text.append(char)
        index++
    }
    error("Unclosed string at $start")
}

private fun stripPhase3Columns(sql: String): String {
    return sql
        .replace(", paused INTEGER NOT NULL DEFAULT 0, endDate INTEGER", "")
        .replace(", paused INTEGER NOT NULL, endDate INTEGER", "")
        .replace(", splitGroupId TEXT", "")
}

private fun normalizeSql(sql: String): String {
    return sql.replace("`", "")
        .replace(Regex("\\s+"), " ")
        .replace("( ", "(")
        .replace(" )", ")")
        .trim()
}

private fun tableName(normalizedCreate: String): String {
    val marker = "EXISTS "
    val at = normalizedCreate.indexOf(marker)
    if (at < 0) return ""
    val rest = normalizedCreate.substring(at + marker.length)
    return rest.substringBefore(' ').substringBefore('(').trim()
}

internal fun runSqlite(db: File, script: String): String {
    val process = ProcessBuilder("sqlite3", db.absolutePath)
        .redirectErrorStream(true)
        .start()
    process.outputStream.bufferedWriter().use { it.write(script) }
    val output = process.inputStream.bufferedReader().readText()
    val code = process.waitFor()
    assertEquals(output, 0, code)
    return output
}
