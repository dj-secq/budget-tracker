package com.example.budgettracker.ui.model

import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Transaction

data class TransactionUiItem(
    val transaction: Transaction,
    val categoryName: String,
    val categoryType: CategoryType = CategoryType.EXPENSE,
    val accountName: String = ""
)
