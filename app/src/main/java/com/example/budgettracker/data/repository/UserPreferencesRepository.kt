package com.example.budgettracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.budgettracker.data.backup.BackupPreferences
import kotlinx.coroutines.flow.first

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class Accent {
    EMERALD, OCEAN, SUNSET
}

data class GeneralPreferences(
    val dailyRemindersEnabled: Boolean,
    val rolloverBudgetsEnabled: Boolean,
    val strictLimitsEnabled: Boolean,
    val appLockEnabled: Boolean = false,
    val reminderHour: Int = 20
)

data class BudgetRulePreferences(
    val needsPercent: Int,
    val wantsPercent: Int,
    val savingsPercent: Int,
    val themeMode: ThemeMode,
    val accent: Accent = Accent.EMERALD,
    val dynamicColor: Boolean = false
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val NEEDS_PERCENT = intPreferencesKey("needs_percent")
        val WANTS_PERCENT = intPreferencesKey("wants_percent")
        val SAVINGS_PERCENT = intPreferencesKey("savings_percent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        
        val DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
        val ROLLOVER_BUDGETS_ENABLED = booleanPreferencesKey("rollover_budgets_enabled")
        val STRICT_LIMITS_ENABLED = booleanPreferencesKey("strict_limits_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded")
        // Local only, like the app lock. A restore must not re-add categories the backup left out.
        val PRESET_CATALOG_FILLED = booleanPreferencesKey("preset_catalog_filled")
        val LAST_ACCOUNT_ID = longPreferencesKey("last_account_id")
    }

    val budgetRulePreferencesFlow: Flow<BudgetRulePreferences> = dataStore.data
        .map { preferences ->
            val needs = preferences[PreferencesKeys.NEEDS_PERCENT] ?: 50
            val wants = preferences[PreferencesKeys.WANTS_PERCENT] ?: 30
            val savings = preferences[PreferencesKeys.SAVINGS_PERCENT] ?: 20
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (e: Exception) { ThemeMode.SYSTEM }
            val accentName = preferences[PreferencesKeys.ACCENT] ?: Accent.EMERALD.name
            val accent = try { Accent.valueOf(accentName) } catch (e: Exception) { Accent.EMERALD }
            val dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false
            BudgetRulePreferences(needs, wants, savings, themeMode, accent, dynamicColor)
        }

    val generalPreferencesFlow: Flow<GeneralPreferences> = dataStore.data
        .map { preferences ->
            val reminders = preferences[PreferencesKeys.DAILY_REMINDERS_ENABLED] ?: false
            val rollover = preferences[PreferencesKeys.ROLLOVER_BUDGETS_ENABLED] ?: false
            val strict = preferences[PreferencesKeys.STRICT_LIMITS_ENABLED] ?: false
            val appLock = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
            GeneralPreferences(reminders, rollover, strict, appLock, reminderHour(preferences[PreferencesKeys.REMINDER_HOUR]))
        }

    val lastAccountIdFlow: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_ACCOUNT_ID]
    }

    suspend fun hasSeeded(): Boolean = dataStore.data.first()[PreferencesKeys.HAS_SEEDED] == true

    suspend fun setHasSeeded() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEDED] = true
        }
    }

    suspend fun presetCatalogFilled(): Boolean =
        dataStore.data.first()[PreferencesKeys.PRESET_CATALOG_FILLED] == true

    suspend fun setPresetCatalogFilled() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESET_CATALOG_FILLED] = true
        }
    }

    suspend fun setLastAccountId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ACCOUNT_ID] = id
        }
    }

    suspend fun snapshot(): BackupPreferences {
        val preferences = dataStore.data.first()
        val theme = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        return BackupPreferences(
            needsPercent = preferences[PreferencesKeys.NEEDS_PERCENT] ?: 50,
            wantsPercent = preferences[PreferencesKeys.WANTS_PERCENT] ?: 30,
            savingsPercent = preferences[PreferencesKeys.SAVINGS_PERCENT] ?: 20,
            themeMode = theme,
            dailyRemindersEnabled = preferences[PreferencesKeys.DAILY_REMINDERS_ENABLED] ?: false,
            rolloverBudgetsEnabled = preferences[PreferencesKeys.ROLLOVER_BUDGETS_ENABLED] ?: false,
            strictLimitsEnabled = preferences[PreferencesKeys.STRICT_LIMITS_ENABLED] ?: false,
            lastAccountId = preferences[PreferencesKeys.LAST_ACCOUNT_ID],
            accent = preferences[PreferencesKeys.ACCENT] ?: Accent.EMERALD.name,
            dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
            reminderHour = reminderHour(preferences[PreferencesKeys.REMINDER_HOUR])
        )
    }

    suspend fun applySnapshot(snapshot: BackupPreferences) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEEDS_PERCENT] = snapshot.needsPercent
            preferences[PreferencesKeys.WANTS_PERCENT] = snapshot.wantsPercent
            preferences[PreferencesKeys.SAVINGS_PERCENT] = snapshot.savingsPercent
            preferences[PreferencesKeys.THEME_MODE] = snapshot.themeMode
            preferences[PreferencesKeys.DAILY_REMINDERS_ENABLED] = snapshot.dailyRemindersEnabled
            preferences[PreferencesKeys.ROLLOVER_BUDGETS_ENABLED] = snapshot.rolloverBudgetsEnabled
            preferences[PreferencesKeys.STRICT_LIMITS_ENABLED] = snapshot.strictLimitsEnabled
            preferences[PreferencesKeys.ACCENT] = snapshot.accent
            preferences[PreferencesKeys.DYNAMIC_COLOR] = snapshot.dynamicColor
            preferences[PreferencesKeys.REMINDER_HOUR] = reminderHour(snapshot.reminderHour)
            if (snapshot.lastAccountId != null) {
                preferences[PreferencesKeys.LAST_ACCOUNT_ID] = snapshot.lastAccountId
            }
        }
    }

    suspend fun reminderHour(): Int = reminderHour(dataStore.data.first()[PreferencesKeys.REMINDER_HOUR])

    suspend fun updateBudgetRule(needs: Int, wants: Int, savings: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEEDS_PERCENT] = needs
            preferences[PreferencesKeys.WANTS_PERCENT] = wants
            preferences[PreferencesKeys.SAVINGS_PERCENT] = savings
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateAccent(accent: Accent) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT] = accent.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setReminderHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_HOUR] = reminderHour(hour)
        }
    }
    
    suspend fun updateGeneralPreferences(reminders: Boolean, rollover: Boolean, strict: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDERS_ENABLED] = reminders
            preferences[PreferencesKeys.ROLLOVER_BUDGETS_ENABLED] = rollover
            preferences[PreferencesKeys.STRICT_LIMITS_ENABLED] = strict
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }
}

private fun reminderHour(hour: Int?): Int = if (hour == 8 || hour == 13 || hour == 20) hour else 20
