package com.example.budgettracker.ui.home

import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.local.entity.countsAsIncome
import com.example.budgettracker.domain.BillsOutlook
import com.example.budgettracker.domain.DueDebt
import com.example.budgettracker.domain.PlannedRule
import com.example.budgettracker.domain.debtOwedCentavos
import com.example.budgettracker.domain.leftAfterBills
import java.time.LocalDate
import java.time.ZoneId

fun outlookFromLedger(
    accounts: List<Account>,
    categories: List<Category>,
    rules: List<RecurringTransaction>,
    debts: List<Debt>,
    unknownDebtName: String,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    asOf: Long = System.currentTimeMillis(),
    includedBalanceCentavos: Long
): BillsOutlook {
    val includedAccountIds = accounts.filter { it.includeInTotalBalance }.map { it.id }.toSet()
    val categoryById = categories.associateBy { it.id }
    val planned = rules.mapNotNull { rule ->
        if (rule.accountId !in includedAccountIds) return@mapNotNull null
        val category = categoryById[rule.categoryId] ?: return@mapNotNull null
        val expense = category.countsAsExpense()
        val income = category.countsAsIncome()
        if (!expense && !income) return@mapNotNull null
        PlannedRule(
            name = rule.note.ifBlank { category.name },
            amountCentavos = rule.amount,
            nextRunTime = rule.nextRunTime,
            interval = rule.frequency.toInterval(),
            anchorDay = rule.anchorDay,
            paused = rule.paused,
            endDate = rule.endDate,
            countsAsExpense = expense,
            countsAsIncome = income
        )
    }
    val dueDebts = debts.mapNotNull { debt ->
        val accountId = debt.accountId
        if (accountId != null && accountId !in includedAccountIds) return@mapNotNull null
        DueDebt(
            name = debt.personName.ifBlank { unknownDebtName },
            dueDate = debt.dueDate,
            owedCentavos = debtOwedCentavos(debt.amount, debt.interestRate, debt.date, asOf, debt.isPaid),
            borrowed = debt.type == DebtType.BORROWED
        )
    }
    return leftAfterBills(
        includedBalanceCentavos = includedBalanceCentavos,
        rules = planned,
        debts = dueDebts,
        today = today,
        zone = zone
    )
}
