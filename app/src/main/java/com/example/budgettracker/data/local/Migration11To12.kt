package com.example.budgettracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recurring_transactions ADD COLUMN paused INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE recurring_transactions ADD COLUMN endDate INTEGER")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId_timestamp` ON `transactions` (`categoryId`, `timestamp`)"
        )
    }
}
