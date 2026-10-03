package com.example.budgettracker.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.data.repository.Accent
import com.example.budgettracker.data.repository.ThemeMode

@Composable
fun CategoryType.label(): String = stringResource(
    if (this == CategoryType.INCOME) R.string.income else R.string.expense
)

@Composable
fun ExpenseClassification.label(): String = stringResource(
    when (this) {
        ExpenseClassification.NEED -> R.string.classification_need
        ExpenseClassification.WANT -> R.string.classification_want
        ExpenseClassification.SAVING -> R.string.classification_saving
        ExpenseClassification.NONE -> R.string.classification_none
    }
)

@Composable
fun Frequency.label(): String = stringResource(
    when (this) {
        Frequency.DAILY -> R.string.frequency_daily
        Frequency.WEEKLY -> R.string.frequency_weekly
        Frequency.MONTHLY -> R.string.frequency_monthly
        Frequency.YEARLY -> R.string.frequency_yearly
        Frequency.SEMI_MONTHLY -> R.string.frequency_semi_monthly
    }
)

@Composable
fun Accent.label(): String = stringResource(
    when (this) {
        Accent.EMERALD -> R.string.accent_emerald
        Accent.OCEAN -> R.string.accent_ocean
        Accent.SUNSET -> R.string.accent_sunset
    }
)

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
)
