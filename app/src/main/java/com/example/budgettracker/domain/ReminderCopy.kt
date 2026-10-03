package com.example.budgettracker.domain

/** Notification body for today's log, recurring posts, and debts that are due today. */
fun reminderMessage(
    loggedToday: Boolean,
    recurringLabels: List<String>,
    debtNames: List<String>
): String? {
    val lines = ArrayList<String>(3)
    if (!loggedToday) {
        lines += "You haven't logged any transactions today."
    }
    when (recurringLabels.size) {
        0 -> Unit
        1 -> lines += "${recurringLabels[0]} posts today."
        else -> lines += "${recurringLabels.size} recurring items post today."
    }
    when (debtNames.size) {
        0 -> Unit
        1 -> lines += "Debt with ${debtNames[0]} is due today."
        else -> lines += "${debtNames.size} debts are due today."
    }
    return lines.joinToString(" ").ifBlank { null }
}
