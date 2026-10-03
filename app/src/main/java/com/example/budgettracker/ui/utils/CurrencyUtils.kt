package com.example.budgettracker.ui.utils

import com.example.budgettracker.domain.Money

object CurrencyUtils {
    fun formatAmount(centavos: Long): String = Money.format(centavos)
}
