package com.example.budgettracker.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.UserPreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface AppContainer {
    val database: AppDatabase
    val budgetRepository: BudgetRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE debts ADD COLUMN dueDate INTEGER")
                database.execSQL("ALTER TABLE debts ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "budget_tracker_db"
        )
         .addMigrations(MIGRATION_8_9)
         .fallbackToDestructiveMigration()
         .build()
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(
            database = database,
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            budgetLimitDao = database.budgetLimitDao(),
            transactionDao = database.transactionDao(),
            savingsGoalDao = database.savingsGoalDao(),
            recurringTransactionDao = database.recurringTransactionDao(),
            debtDao = database.debtDao(),
            transactionTemplateDao = database.transactionTemplateDao()
        )
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }
}
