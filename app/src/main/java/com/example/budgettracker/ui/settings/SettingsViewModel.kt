package com.example.budgettracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.budgettracker.data.local.entity.BackupData
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.ThemeMode
import com.example.budgettracker.data.repository.UserPreferencesRepository
import com.google.gson.Gson
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
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        
    val generalPrefs = preferencesRepository.generalPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

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
    
    fun updateGeneralPreferences(reminders: Boolean, rollover: Boolean, strict: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGeneralPreferences(reminders, rollover, strict)
        }
    }

    fun exportData(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val data = budgetRepository.exportData()
                val json = Gson().toJson(data)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun importData(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader ->
                            reader.readText()
                        }
                    }
                }
                if (json != null) {
                    val data = Gson().fromJson(json, BackupData::class.java)
                    budgetRepository.restoreBackup(data)
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
}
