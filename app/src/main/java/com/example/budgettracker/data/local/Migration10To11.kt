package com.example.budgettracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `balance` INTEGER NOT NULL,
                `colorArgb` INTEGER NOT NULL,
                `includeInTotalBalance` INTEGER NOT NULL,
                `openingBalance` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_accounts (id, name, type, balance, colorArgb, includeInTotalBalance, openingBalance)
            SELECT id, name, type, CAST(ROUND(balance * 100) AS INTEGER), colorArgb, includeInTotalBalance, 0
            FROM accounts
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `colorArgb` INTEGER NOT NULL,
                `iconName` TEXT,
                `role` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_categories (id, name, type, colorArgb, iconName, role)
            SELECT id, name, type, colorArgb, iconName,
                CASE
                    WHEN name = 'Withdraw / Transfer Out' AND type = 'EXPENSE' THEN 'TRANSFER_OUT'
                    WHEN name = 'Deposit / Transfer In' AND type = 'INCOME' THEN 'TRANSFER_IN'
                    ELSE 'NORMAL'
                END
            FROM categories
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_budget_limits` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `assignedAmount` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `year` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_budget_limits (id, categoryId, assignedAmount, month, year)
            SELECT b.id, b.categoryId, CAST(ROUND(b.assignedAmount * 100) AS INTEGER), b.month, b.year
            FROM budget_limits b
            INNER JOIN (
                SELECT categoryId, month, year, MAX(id) AS id
                FROM budget_limits
                GROUP BY categoryId, month, year
            ) keep ON b.id = keep.id
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `payeeOrPayer` TEXT,
                `classification` TEXT NOT NULL,
                `recurringId` INTEGER,
                `goalId` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_transactions (
                id, accountId, categoryId, amount, timestamp, note, payeeOrPayer, classification, recurringId, goalId
            )
            SELECT id, accountId, categoryId, CAST(ROUND(amount * 100) AS INTEGER), timestamp, note,
                   payeeOrPayer, classification, NULL, NULL
            FROM transactions
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_savings_goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `targetAmount` INTEGER NOT NULL,
                `currentAmount` INTEGER NOT NULL,
                `iconName` TEXT,
                `targetDate` INTEGER,
                `contributionFrequency` TEXT,
                `contributionAmount` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_savings_goals (
                id, name, targetAmount, currentAmount, iconName, targetDate, contributionFrequency, contributionAmount
            )
            SELECT id, name,
                   CAST(ROUND(targetAmount * 100) AS INTEGER),
                   CAST(ROUND(currentAmount * 100) AS INTEGER),
                   iconName, targetDate, contributionFrequency,
                   CASE WHEN contributionAmount IS NULL THEN NULL
                        ELSE CAST(ROUND(contributionAmount * 100) AS INTEGER) END
            FROM savings_goals
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_recurring` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `classification` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `nextRunTime` INTEGER NOT NULL,
                `anchorDay` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_recurring (
                id, accountId, categoryId, amount, note, classification, frequency, startDate, nextRunTime, anchorDay
            )
            SELECT id, accountId, categoryId, CAST(ROUND(amount * 100) AS INTEGER), note, classification,
                   frequency, startDate, nextRunTime,
                   COALESCE(CAST(strftime('%d', startDate / 1000, 'unixepoch', 'localtime') AS INTEGER), 1)
            FROM recurring_transactions
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_debts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `personName` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `isPaid` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `dueDate` INTEGER,
                `interestRate` REAL NOT NULL,
                `accountId` INTEGER,
                `originTransactionId` INTEGER,
                `settlementTransactionId` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_debts (
                id, personName, amount, type, date, isPaid, note, dueDate, interestRate, accountId,
                originTransactionId, settlementTransactionId
            )
            SELECT id, personName, CAST(ROUND(amount * 100) AS INTEGER), type, date, isPaid, note,
                   dueDate, interestRate, accountId, NULL, NULL
            FROM debts
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hold_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `templateName` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `accountId` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `transactionType` TEXT NOT NULL,
                `classification` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hold_templates (
                id, templateName, amount, categoryId, accountId, note, transactionType, classification
            )
            SELECT id, templateName, CAST(ROUND(amount * 100) AS INTEGER), categoryId, accountId, note,
                   transactionType, classification
            FROM transaction_templates
            """.trimIndent()
        )

        db.execSQL("DROP TABLE transactions")
        db.execSQL("DROP TABLE recurring_transactions")
        db.execSQL("DROP TABLE transaction_templates")
        db.execSQL("DROP TABLE budget_limits")
        db.execSQL("DROP TABLE debts")
        db.execSQL("DROP TABLE savings_goals")
        db.execSQL("DROP TABLE categories")
        db.execSQL("DROP TABLE accounts")

        db.execSQL("ALTER TABLE hold_accounts RENAME TO accounts")
        db.execSQL("ALTER TABLE hold_categories RENAME TO categories")
        db.execSQL("ALTER TABLE hold_savings_goals RENAME TO savings_goals")
        db.execSQL("ALTER TABLE hold_debts RENAME TO debts")

        val recoveredColor = 0xFF6B7280.toInt()
        db.execSQL(
            """
            INSERT INTO accounts (name, type, balance, colorArgb, includeInTotalBalance, openingBalance)
            SELECT 'Recovered wallet', 'CHECKING', 0, $recoveredColor, 0, 0
            WHERE EXISTS (
                SELECT 1 FROM hold_transactions t
                WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.id = t.accountId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE hold_transactions
            SET accountId = (SELECT id FROM accounts WHERE name = 'Recovered wallet' ORDER BY id DESC LIMIT 1)
            WHERE accountId NOT IN (SELECT id FROM accounts)
            """.trimIndent()
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_limits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `assignedAmount` INTEGER NOT NULL, `month` INTEGER NOT NULL, `year` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_limits_categoryId_month_year` ON `budget_limits` (`categoryId`, `month`, `year`)"
        )
        db.execSQL(
            "INSERT INTO budget_limits (id, categoryId, assignedAmount, month, year) SELECT id, categoryId, assignedAmount, month, year FROM hold_budget_limits"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `payeeOrPayer` TEXT, `classification` TEXT NOT NULL, `recurringId` INTEGER, `goalId` INTEGER, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_recurringId_timestamp` ON `transactions` (`recurringId`, `timestamp`)"
        )
        db.execSQL(
            """
            INSERT INTO transactions (
                id, accountId, categoryId, amount, timestamp, note, payeeOrPayer, classification, recurringId, goalId
            )
            SELECT id, accountId, categoryId, amount, timestamp, note, payeeOrPayer, classification, recurringId, goalId
            FROM hold_transactions
            """.trimIndent()
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT NOT NULL, `classification` TEXT NOT NULL, `frequency` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `nextRunTime` INTEGER NOT NULL, `anchorDay` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)")
        db.execSQL(
            """
            INSERT INTO recurring_transactions (
                id, accountId, categoryId, amount, note, classification, frequency, startDate, nextRunTime, anchorDay
            )
            SELECT id, accountId, categoryId, amount, note, classification, frequency, startDate, nextRunTime, anchorDay
            FROM hold_recurring
            WHERE accountId IN (SELECT id FROM accounts) AND categoryId IN (SELECT id FROM categories)
            """.trimIndent()
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transaction_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateName` TEXT NOT NULL, `amount` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `note` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `classification` TEXT NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` ON `transaction_templates` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` ON `transaction_templates` (`accountId`)")
        db.execSQL(
            """
            INSERT INTO transaction_templates (
                id, templateName, amount, categoryId, accountId, note, transactionType, classification
            )
            SELECT id, templateName, amount, categoryId, accountId, note, transactionType, classification
            FROM hold_templates
            WHERE accountId IN (SELECT id FROM accounts) AND categoryId IN (SELECT id FROM categories)
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE accounts
            SET openingBalance = balance - COALESCE((
                SELECT SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                FROM transactions t
                JOIN categories c ON c.id = t.categoryId
                WHERE t.accountId = accounts.id
            ), 0)
            """.trimIndent()
        )

        db.execSQL("DROP TABLE hold_budget_limits")
        db.execSQL("DROP TABLE hold_transactions")
        db.execSQL("DROP TABLE hold_recurring")
        db.execSQL("DROP TABLE hold_templates")
    }
}
