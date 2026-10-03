package com.example.budgettracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.local.entity.countsAsIncome
import android.content.res.Resources
import com.example.budgettracker.R
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.domain.BillDirection
import com.example.budgettracker.domain.debtOwedCentavos
import com.example.budgettracker.data.repository.UserPreferencesRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetItem(
    val category: Category,
    val base: Long,
    val spent: Long,
    val rollover: Long = 0L
) {
    val limit: Long get() = base + rollover
}

data class HomeTransactionItem(
    val transaction: Transaction,
    val categoryName: String,
    val income: Boolean
)

data class HomeUpcoming(
    val name: String,
    val whenMillis: Long,
    val amountCentavos: Long,
    val arrives: Boolean,
    val opensDebt: Boolean
)

data class HomeUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpenses: Long = 0L,
    val budgetItems: List<BudgetItem> = emptyList(),
    val recent: List<HomeTransactionItem> = emptyList(),
    val owedToYou: Long = 0L,
    val youOwe: Long = 0L,
    val leftAfterBills: Long = 0L,
    val upcoming: List<HomeUpcoming> = emptyList(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val budgetRepository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val resources: Resources
) : ViewModel() {

    private val _monthYear = MutableStateFlow(currentMonthYear())

    init {
        viewModelScope.launch {
            val (month, year) = _monthYear.value
            budgetRepository.copyBudgetsForwardIfEmpty(month, year)
        }
    }

    private val monthlyDataFlow = _monthYear.flatMapLatest { (month, year) ->
        combine(
            budgetRepository.getTransactionsForMonth(month, year),
            budgetRepository.getBudgetLimitsForMonth(month, year)
        ) { txs, limits ->
            txs to limits
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            budgetRepository.getAllAccounts(),
            budgetRepository.getAllCategories(),
            monthlyDataFlow
        ) { accounts, categories, monthly ->
            Triple(accounts, categories, monthly)
        },
        combine(
            _monthYear,
            budgetRepository.getRecentTransactions(),
            preferencesRepository.generalPreferencesFlow
        ) { monthYear, transactions, prefs ->
            Triple(monthYear, transactions, prefs)
        },
        budgetRepository.getAllDebts(),
        budgetRepository.getAllRecurringTransactions()
    ) { left, right, debts, rules ->
        val (accounts, categories, monthly) = left
        val (monthYear, allTransactions, prefs) = right
        val (monthTransactions, budgetLimits) = monthly
        val (month, year) = monthYear
        val categoryById = categories.associateBy { it.id }

        val activeDays = allTransactions.map { dayKey(it.timestamp) }.toSet()
        val (currentStreak, longestStreak) = streaks(activeDays, allTransactions)

        val totalIncome = monthTransactions.filter { categoryById[it.categoryId]?.countsAsIncome() == true }
            .sumOf { it.amount }
        val totalExpenses = monthTransactions.filter { categoryById[it.categoryId]?.countsAsExpense() == true }
            .sumOf { it.amount }
        val totalBalance = accounts.filter { it.includeInTotalBalance }.sumOf { it.balance }

        val rolloverMap = if (prefs.rolloverBudgetsEnabled) {
            budgetRepository.rolloversForMonth(month, year)
        } else {
            emptyMap()
        }
        val items = categories.filter { it.countsAsExpense() }.map { category ->
            val base = budgetLimits.find { it.categoryId == category.id }?.assignedAmount ?: 0L
            val spent = monthTransactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            val rollover = rolloverMap[category.id] ?: 0L
            BudgetItem(category, base, spent, rollover)
        }.filter { it.limit > 0L || it.spent > 0L }

        val recent = allTransactions.take(5).map { tx ->
            val category = categoryById[tx.categoryId]
            HomeTransactionItem(
                transaction = tx,
                categoryName = category?.name ?: resources.getString(R.string.unknown_category),
                income = category?.type == CategoryType.INCOME
            )
        }

        val asOf = System.currentTimeMillis()
        val owedToYou = debts.filter { it.type == DebtType.LENT }.sumOf {
            debtOwedCentavos(it.amount, it.interestRate, it.date, asOf, it.isPaid)
        }
        val youOwe = debts.filter { it.type == DebtType.BORROWED }.sumOf {
            debtOwedCentavos(it.amount, it.interestRate, it.date, asOf, it.isPaid)
        }
        val outlook = outlookFromLedger(
            accounts = accounts,
            categories = categories,
            rules = rules,
            debts = debts,
            unknownDebtName = resources.getString(R.string.debt_tracker),
            today = LocalDate.now(),
            zone = ZoneId.systemDefault(),
            asOf = asOf,
            includedBalanceCentavos = totalBalance
        )

        HomeUiState(
            accounts = accounts,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            budgetItems = items,
            recent = recent,
            owedToYou = owedToYou,
            youOwe = youOwe,
            leftAfterBills = outlook.leftCentavos,
            upcoming = outlook.upcoming.map { bill ->
                HomeUpcoming(
                    name = bill.name,
                    whenMillis = bill.whenMillis,
                    amountCentavos = bill.amountCentavos,
                    arrives = bill.direction == BillDirection.ARRIVES,
                    opensDebt = bill.opensDebt
                )
            },
            currentMonth = month,
            currentYear = year,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setMonth(month: Int, year: Int) {
        _monthYear.value = month to year
        viewModelScope.launch {
            budgetRepository.copyBudgetsForwardIfEmpty(month, year)
        }
    }

    private fun dayKey(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.YEAR) * 10000 +
            calendar.get(Calendar.MONTH) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun streaks(activeDays: Set<Int>, transactions: List<Transaction>): Pair<Int, Int> {
        if (transactions.isEmpty()) return 0 to 0
        val firstTxTime = transactions.minOf { it.timestamp }
        val check = Calendar.getInstance().apply {
            timeInMillis = firstTxTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis
        var longest = 0
        var running = 0
        while (check.timeInMillis < todayEnd) {
            if (activeDays.contains(dayKey(check.timeInMillis))) {
                running++
                if (running > longest) longest = running
            } else {
                running = 0
            }
            check.add(Calendar.DAY_OF_YEAR, 1)
        }

        var current = 0
        val cursor = Calendar.getInstance()
        val oldest = activeDays.minOrNull() ?: dayKey(firstTxTime)
        while (true) {
            val key = dayKey(cursor.timeInMillis)
            if (key < oldest) break
            if (activeDays.contains(key)) {
                current++
                cursor.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return current to longest
    }

    private fun currentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.MONTH) + 1) to calendar.get(Calendar.YEAR)
    }
}
