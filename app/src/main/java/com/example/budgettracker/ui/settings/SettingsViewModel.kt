package com.example.budgettracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.backup.BackupCodec
import com.example.budgettracker.data.repository.Accent
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.CsvImportOutcome
import com.example.budgettracker.data.repository.ThemeMode
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.example.budgettracker.worker.ReminderSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val budgetRule = preferencesRepository.budgetRulePreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val generalPrefs = preferencesRepository.generalPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateRule(needs: Int, wants: Int, savings: Int) {
        if (needs + wants + savings == 100) {
            viewModelScope.launch {
                preferencesRepository.updateBudgetRule(needs, wants, savings)
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateAccent(accent: Accent) {
        viewModelScope.launch {
            preferencesRepository.updateAccent(accent)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(enabled)
        }
    }

    fun setReminderHour(context: Context, hour: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderHour(hour)
            withContext(Dispatchers.IO) {
                ReminderSchedule.enqueue(context.applicationContext, hour, replace = true)
            }
        }
    }

    fun updateGeneralPreferences(reminders: Boolean, rollover: Boolean, strict: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGeneralPreferences(reminders, rollover, strict)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockEnabled(enabled)
        }
    }

    fun exportData(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = budgetRepository.exportJson(preferencesRepository.snapshot())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer -> writer.write(json) }
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun exportCsv(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = budgetRepository.exportTransactionsCsv()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer -> writer.write(csv) }
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun importCsv(context: Context, uri: Uri, onComplete: (CsvImportOutcome?) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader -> reader.readText() }
                    }
                }
                if (csv == null) {
                    onComplete(null)
                } else {
                    onComplete(budgetRepository.importTransactionsCsv(csv))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    fun importData(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader -> reader.readText() }
                    }
                }
                if (json != null) {
                    val decoded = BackupCodec.decode(json)
                    val preferences = budgetRepository.restoreBackup(decoded)
                    if (preferences != null) {
                        preferencesRepository.applySnapshot(preferences)
                        withContext(Dispatchers.IO) {
                            ReminderSchedule.enqueue(context.applicationContext, preferences.reminderHour, replace = true)
                        }
                    }
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun recalculateBalances(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.recalculateBalances()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}
