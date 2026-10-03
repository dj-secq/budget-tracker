package com.example.budgettracker.data.repository

data class CsvImportOutcome(
    val added: Int,
    val skipped: Int,
    val rejected: Int,
    val headerOk: Boolean
)
