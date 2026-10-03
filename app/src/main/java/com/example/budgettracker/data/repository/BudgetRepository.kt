package com.example.budgettracker.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.budgettracker.data.backup.BackupCodec
import com.example.budgettracker.data.backup.BackupData
import com.example.budgettracker.data.backup.BackupPreferences
import com.example.budgettracker.data.backup.DecodedBackup
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BudgetLimitDao
import com.example.budgettracker.data.local.dao.CategoryDao
import com.example.budgettracker.data.local.dao.DebtDao
import com.example.budgettracker.data.local.dao.RecurringTransactionDao
import com.example.budgettracker.data.local.dao.SavingsGoalDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.dao.TransactionTemplateDao
import com.example.budgettracker.data.local.dao.CategoryClassificationTotal
import com.example.budgettracker.data.local.dao.GoalTotal
import com.example.budgettracker.data.local.dao.MonthCategoryTotal
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryRole
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.TransactionTemplate
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.data.local.entity.countsAsIncome
import com.example.budgettracker.domain.CsvTransaction
import com.example.budgettracker.domain.MonthWindow
import com.example.budgettracker.domain.TransactionCsv
import com.example.budgettracker.domain.YearMonthKey
import com.example.budgettracker.domain.bucketCap
import com.example.budgettracker.domain.categoryCap
import com.example.budgettracker.domain.interestCentavos
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.localNoon
import java.time.LocalDate
import java.util.UUID
import com.example.budgettracker.domain.monthWindow
import com.example.budgettracker.domain.occurrencesOnOrBefore
import com.example.budgettracker.domain.occurrencesToInsert
import com.example.budgettracker.domain.planOccurrences
import com.example.budgettracker.domain.previousMonth
import com.example.budgettracker.domain.rolloverCentavos
import com.example.budgettracker.domain.signedDelta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class DeleteOutcome {
    data object Deleted : DeleteOutcome()
    data object Blocked : DeleteOutcome()
}

data class SpendEvaluation(
    val categoryExcess: Long = 0L,
    val bucketName: String? = null,
    val bucketExcess: Long = 0L,
    val strict: Boolean = false
) {
    val categoryExceeded: Boolean get() = categoryExcess > 0L
    val bucketExceeded: Boolean get() = bucketExcess > 0L
}

class BudgetRepository(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetLimitDao: BudgetLimitDao,
    private val transactionDao: TransactionDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val debtDao: DebtDao,
    private val transactionTemplateDao: TransactionTemplateDao
) {
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: Account): Long {
        val opening = if (account.openingBalance != 0L) account.openingBalance else account.balance
        return accountDao.insertAccount(account.copy(balance = opening, openingBalance = opening))
    }

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account): DeleteOutcome {
        if (transactionDao.countByAccount(account.id) > 0 ||
            recurringTransactionDao.countByAccount(account.id) > 0 ||
            transactionTemplateDao.countByAccount(account.id) > 0
        ) {
            return DeleteOutcome.Blocked
        }
        accountDao.deleteAccount(account)
        return DeleteOutcome.Deleted
    }

    suspend fun recalculateBalances() {
        database.withTransaction { rebuildBalances() }
    }

    fun getAllTemplates(): Flow<List<TransactionTemplate>> = transactionTemplateDao.getAllTemplates()

    suspend fun insertTemplate(template: TransactionTemplate): Long = transactionTemplateDao.insertTemplate(template)

    suspend fun deleteTemplate(template: TransactionTemplate) = transactionTemplateDao.deleteTemplate(template)

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = categoryDao.getCategoriesByType(type)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category): DeleteOutcome {
        if (transactionDao.countByCategory(category.id) > 0 || recurringTransactionDao.countByCategory(category.id) > 0) {
            return DeleteOutcome.Blocked
        }
        categoryDao.deleteCategory(category)
        return DeleteOutcome.Deleted
    }

    fun getBudgetLimitsForMonth(month: Int, year: Int): Flow<List<BudgetLimit>> =
        budgetLimitDao.getBudgetLimitsForMonth(month, year)

    suspend fun setBudgetLimit(categoryId: Long, amount: Long, month: Int, year: Int) {
        database.withTransaction {
            val existing = budgetLimitDao.getBudgetLimit(categoryId, month, year)
            if (existing != null) {
                budgetLimitDao.updateBudgetLimit(existing.copy(assignedAmount = amount))
            } else {
                budgetLimitDao.insertBudgetLimit(
                    BudgetLimit(categoryId = categoryId, assignedAmount = amount, month = month, year = year)
                )
            }
        }
    }

    suspend fun copyBudgetsForwardIfEmpty(month: Int, year: Int) {
        try {
            database.withTransaction {
                if (budgetLimitDao.getBudgetLimitsForMonthOnce(month, year).isNotEmpty()) return@withTransaction
                val previous = previousMonth(month, year)
                val rows = budgetLimitDao.getBudgetLimitsForMonthOnce(previous.month, previous.year)
                    .filter { it.assignedAmount > 0L }
                for (row in rows) {
                    budgetLimitDao.insertBudgetLimit(row.copy(id = 0, month = month, year = year))
                }
            }
        } catch (_: SQLiteConstraintException) {
            // The month was filled by another writer.
        }
    }

    suspend fun rolloversForMonth(month: Int, year: Int): Map<Long, Long> {
        val window = monthWindow(month, year)
        val spent = transactionDao.spentByCategoryMonthBefore(window.startInclusive)
        val bases = budgetLimitDao.getBudgetLimitsBefore(month, year)
        val categoryIds = (spent.map { it.categoryId } + bases.map { it.categoryId }).toSet()
        val target = YearMonthKey(year, month)
        return categoryIds.associateWith { categoryId ->
            val spentMap = spent.filter { it.categoryId == categoryId }
                .associate { YearMonthKey(it.year, it.month) to it.total }
            val baseMap = bases.filter { it.categoryId == categoryId }
                .associate { YearMonthKey(it.year, it.month) to it.assignedAmount }
            rolloverCentavos(baseMap, spentMap, target)
        }
    }

    suspend fun getRolloverAmount(categoryId: Long, month: Int, year: Int): Long {
        return rolloversForMonth(month, year)[categoryId] ?: 0L
    }

    fun getRecentTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun observeLedger(start: Long?, end: Long?, limit: Int): Flow<List<Transaction>> =
        transactionDao.ledger(start, end, limit)

    suspend fun countBetween(start: Long, end: Long): Int = transactionDao.countBetween(start, end)

    suspend fun earliestTimestamp(): Long? = transactionDao.earliestTimestamp()

    suspend fun totalsByCategory(start: Long, end: Long): List<CategoryClassificationTotal> =
        transactionDao.totalsByCategory(start, end)

    suspend fun totalsByMonthCategory(start: Long, end: Long): List<MonthCategoryTotal> =
        transactionDao.totalsByMonthCategory(start, end)

    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategoryClassificationTotal>> =
        transactionDao.observeTotalsByCategory(start, end)

    fun observeTotalsByMonthCategory(start: Long, end: Long): Flow<List<MonthCategoryTotal>> =
        transactionDao.observeTotalsByMonthCategory(start, end)

    fun goalTotals(): Flow<List<GoalTotal>> = transactionDao.goalTotals()

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val window = monthWindow(month, year)
        return transactionDao.getTransactionsBetweenDates(window.startInclusive, window.endInclusive)
    }

    fun monthBounds(month: Int, year: Int): MonthWindow = monthWindow(month, year)

    suspend fun hasTransactionsForCategory(categoryId: Long): Boolean {
        return transactionDao.countByCategory(categoryId) > 0
    }

    fun getTotalSpentByCategory(categoryId: Long, month: Int, year: Int): Flow<Long?> {
        val window = monthWindow(month, year)
        return transactionDao.getTotalAmountByCategoryAndDateRange(
            categoryId,
            window.startInclusive,
            window.endInclusive
        )
    }

    suspend fun evaluateSpend(
        categoryId: Long,
        amount: Long,
        timestamp: Long,
        classification: ExpenseClassification,
        strict: Boolean,
        rolloverEnabled: Boolean,
        needsPercent: Int,
        wantsPercent: Int,
        savingsPercent: Int,
        replacing: Transaction? = null
    ): SpendEvaluation {
        val category = categoryDao.getCategoryById(categoryId) ?: return SpendEvaluation(strict = strict)
        val date = localDateOf(timestamp)
        val month = date.monthValue
        val year = date.year
        val window = monthWindow(month, year)

        var categoryExcess = 0L
        if (category.countsAsExpense()) {
            val base = budgetLimitDao.getBudgetLimit(categoryId, month, year)?.assignedAmount ?: 0L
            val rollover = if (rolloverEnabled) getRolloverAmount(categoryId, month, year) else 0L
            var spent = transactionDao.sumAmountByCategoryAndDateRange(
                categoryId,
                window.startInclusive,
                window.endInclusive
            )
            if (replacing != null && replacing.categoryId == categoryId && sameMonth(replacing.timestamp, month, year)) {
                spent -= replacing.amount
            }
            categoryExcess = categoryCap(spent, amount, base, rollover, strict).excess
        }

        var bucketName: String? = null
        var bucketExcess = 0L
        if (classification != ExpenseClassification.NONE && category.countsAsExpense()) {
            val txs = transactionDao.listBetween(window.startInclusive, window.endInclusive)
                .filter { replacing == null || it.id != replacing.id }
            val categories = categoryDao.listCategories().associateBy { it.id }
            val income = txs.filter { categories[it.categoryId]?.countsAsIncome() == true }.sumOf { it.amount }
            val percent = when (classification) {
                ExpenseClassification.NEED -> needsPercent
                ExpenseClassification.WANT -> wantsPercent
                ExpenseClassification.SAVING -> savingsPercent
                ExpenseClassification.NONE -> 0
            }
            bucketName = when (classification) {
                ExpenseClassification.NEED -> "Needs"
                ExpenseClassification.WANT -> "Wants"
                ExpenseClassification.SAVING -> "Savings"
                ExpenseClassification.NONE -> null
            }
            val bucketSpent = txs.filter {
                it.classification == classification && categories[it.categoryId]?.countsAsExpense() == true
            }.sumOf { it.amount }
            bucketExcess = bucketCap(bucketSpent, amount, income, percent, strict).excess
        }
        return SpendEvaluation(categoryExcess, bucketName, bucketExcess, strict)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return database.withTransaction { post(transaction) }
    }

    suspend fun saveTransaction(transaction: Transaction, recurring: RecurringTransaction? = null): Long {
        return database.withTransaction {
            val id = post(transaction)
            if (recurring != null) recurringTransactionDao.insertRecurringTransaction(recurring)
            id
        }
    }

    suspend fun insertTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        goalId: Long? = null
    ) {
        database.withTransaction {
            postTransfer(fromAccountId, toAccountId, amount, note, timestamp, goalId)
        }
    }

    private suspend fun postTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        note: String,
        timestamp: Long,
        goalId: Long?
    ): Long {
        val outCategory = ensureRole(
            CategoryRole.TRANSFER_OUT,
            "Withdraw / Transfer Out",
            CategoryType.EXPENSE,
            0xFFF44336.toInt()
        )
        val inCategory = ensureRole(
            CategoryRole.TRANSFER_IN,
            "Deposit / Transfer In",
            CategoryType.INCOME,
            0xFF4CAF50.toInt()
        )
        val label = note.ifBlank { "Transfer" }
        post(
            Transaction(
                accountId = fromAccountId,
                categoryId = outCategory.id,
                amount = amount,
                note = label,
                timestamp = timestamp
            )
        )
        return post(
            Transaction(
                accountId = toAccountId,
                categoryId = inCategory.id,
                amount = amount,
                note = label,
                timestamp = timestamp,
                goalId = goalId
            )
        )
    }

    suspend fun deleteTransaction(transaction: Transaction): List<Transaction> {
        return database.withTransaction {
            val current = transactionDao.getTransactionById(transaction.id) ?: transaction
            val group = current.splitGroupId?.takeIf { it.isNotBlank() }
            val rows = if (group == null) {
                listOf(current)
            } else {
                transactionDao.listBySplitGroup(group).ifEmpty { listOf(current) }
            }
            rows.forEach { remove(it) }
            rows
        }
    }

    suspend fun restoreTransactions(transactions: List<Transaction>) {
        database.withTransaction {
            transactions.forEach { post(it) }
        }
    }

    suspend fun saveSplit(
        accountId: Long,
        parts: List<Pair<Long, Long>>,
        note: String,
        timestamp: Long,
        classification: ExpenseClassification
    ) {
        database.withTransaction {
            val group = UUID.randomUUID().toString()
            for ((categoryId, amount) in parts) {
                post(
                    Transaction(
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        note = note,
                        timestamp = timestamp,
                        classification = classification,
                        splitGroupId = group
                    )
                )
            }
        }
    }

    suspend fun updateTransaction(newTransaction: Transaction) {
        database.withTransaction {
            val old = transactionDao.getTransactionById(newTransaction.id) ?: return@withTransaction
            replace(old, newTransaction)
        }
    }

    fun getAllGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()

    suspend fun insertGoal(goal: SavingsGoal): Long = savingsGoalDao.insertGoal(goal)

    suspend fun updateGoal(goal: SavingsGoal) = savingsGoalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: SavingsGoal) = savingsGoalDao.deleteGoal(goal)

    /**
     * Moves [amount] into a savings wallet and tags that deposit with the goal.
     * The transfer is not spending. Pass [spendCategoryId] only when the user picked an expense category on purpose.
     */
    suspend fun fundGoal(
        goalId: Long,
        fromAccountId: Long,
        amount: Long,
        timestamp: Long = System.currentTimeMillis(),
        spendCategoryId: Long? = null
    ): Long {
        require(amount > 0L)
        return database.withTransaction {
            val goal = savingsGoalDao.getGoalById(goalId) ?: error("Goal is missing")
            if (spendCategoryId != null) {
                val category = categoryDao.getCategoryById(spendCategoryId)
                    ?: error("Category is missing")
                if (!category.countsAsExpense()) error("Pick an expense category")
                post(
                    Transaction(
                        accountId = fromAccountId,
                        categoryId = category.id,
                        amount = amount,
                        timestamp = timestamp,
                        note = "Funded goal: ${goal.name}",
                        classification = ExpenseClassification.SAVING,
                        goalId = goalId
                    )
                )
            } else {
                val savingsId = savingsDestination(fromAccountId)
                postTransfer(
                    fromAccountId,
                    savingsId,
                    amount,
                    "Funded goal: ${goal.name}",
                    timestamp,
                    goalId
                )
            }
        }
    }

    suspend fun unfundLatest(goalId: Long) {
        database.withTransaction {
            val latest = transactionDao.latestForGoal(goalId) ?: return@withTransaction
            val category = categoryDao.getCategoryById(latest.categoryId)
            if (category?.role == CategoryRole.TRANSFER_IN) {
                transactionDao.findTransferOut(latest.amount, latest.timestamp, latest.note)?.let { remove(it) }
            }
            remove(latest)
        }
    }

    private suspend fun savingsDestination(sourceId: Long): Long {
        val accounts = accountDao.listAccounts()
        val existing = accounts.firstOrNull { it.id != sourceId && it.type == AccountType.SAVINGS }
            ?: accounts.firstOrNull { it.id != sourceId && it.name.equals("Savings", ignoreCase = true) }
        if (existing != null) return existing.id
        val source = accounts.firstOrNull { it.id == sourceId }
        if (source?.type == AccountType.SAVINGS || source?.name.equals("Savings", ignoreCase = true)) {
            error("Pick a spending wallet. This one is already savings.")
        }
        return accountDao.insertAccount(
            Account(
                name = "Savings",
                type = AccountType.SAVINGS,
                balance = 0L,
                openingBalance = 0L,
                colorArgb = 0xFF10B981.toInt(),
                includeInTotalBalance = true
            )
        )
    }

    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getAllRecurringTransactions()

    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransaction> =
        recurringTransactionDao.getDueRecurringTransactions(currentTime)

    suspend fun recurringDueOn(start: Long, end: Long): List<RecurringTransaction> =
        recurringTransactionDao.dueOnDay(start, end)

    suspend fun debtsDueOn(start: Long, end: Long): List<Debt> = debtDao.dueBetween(start, end)

    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long =
        recurringTransactionDao.insertRecurringTransaction(recurringTransaction)

    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) =
        recurringTransactionDao.updateRecurringTransaction(recurringTransaction)

    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) =
        recurringTransactionDao.deleteRecurringTransaction(recurringTransaction)

    suspend fun postDueRecurring(now: Long) {
        val due = recurringTransactionDao.getDueRecurringTransactions(now)
        for (rule in due) {
            try {
                database.withTransaction { postRule(rule.id, now) }
            } catch (_: SQLiteConstraintException) {
                // Leave this rule due. The unique occurrence key blocks a double post.
            }
        }
    }

    fun getAllDebts(): Flow<List<Debt>> = debtDao.getAllDebts()

    fun observeTransactionAmounts(ids: List<Long>): Flow<Map<Long, Long>> {
        if (ids.isEmpty()) return flowOf(emptyMap())
        return transactionDao.observeAmounts(ids).map { rows -> rows.associate { it.id to it.amount } }
    }

    suspend fun createDebt(
        personName: String,
        amount: Long,
        type: DebtType,
        note: String,
        accountId: Long,
        dueDate: Long?,
        interestRate: Double,
        startDate: Long
    ): Long {
        require(amount > 0L)
        return database.withTransaction {
            val category = if (type == DebtType.LENT) {
                ensureNamed("Loan", CategoryType.EXPENSE, 0xFF9E9E9E.toInt(), iconName = "loan")
            } else {
                ensureNamed("Loan Received", CategoryType.INCOME, 0xFF9E9E9E.toInt(), iconName = "loan received")
            }
            val txId = post(
                Transaction(
                    accountId = accountId,
                    categoryId = category.id,
                    amount = amount,
                    timestamp = startDate,
                    note = if (type == DebtType.LENT) "Lent to $personName" else "Borrowed from $personName"
                )
            )
            debtDao.insertDebt(
                Debt(
                    personName = personName,
                    amount = amount,
                    type = type,
                    date = startDate,
                    note = note,
                    dueDate = dueDate,
                    interestRate = interestRate,
                    accountId = accountId,
                    originTransactionId = txId
                )
            )
        }
    }

    suspend fun settleDebt(debtId: Long, accountId: Long, now: Long) {
        database.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction
            if (debt.isPaid) return@withTransaction
            val interest = interestCentavos(debt.amount, debt.interestRate, debt.date, now)
            val category = if (debt.type == DebtType.LENT) {
                ensureNamed("Loan Repaid", CategoryType.INCOME, 0xFF4CAF50.toInt(), iconName = "loan repaid")
            } else {
                ensureNamed("Loan Paid", CategoryType.EXPENSE, 0xFF4CAF50.toInt(), iconName = "loan paid")
            }
            val txId = post(
                Transaction(
                    accountId = accountId,
                    categoryId = category.id,
                    amount = debt.amount + interest,
                    timestamp = now,
                    note = if (debt.type == DebtType.LENT) {
                        "Payment received from ${debt.personName}"
                    } else {
                        "Repayment to ${debt.personName}"
                    }
                )
            )
            debtDao.updateDebt(debt.copy(isPaid = true, settlementTransactionId = txId, accountId = accountId))
        }
    }

    suspend fun unsettleDebt(debtId: Long) {
        database.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction
            if (!debt.isPaid) return@withTransaction
            debt.settlementTransactionId?.let { id ->
                transactionDao.getTransactionById(id)?.let { remove(it) }
            }
            debtDao.updateDebt(debt.copy(isPaid = false, settlementTransactionId = null))
        }
    }

    suspend fun updateDebt(updated: Debt) {
        database.withTransaction {
            val old = debtDao.getDebtById(updated.id) ?: return@withTransaction
            if (old.isPaid) {
                debtDao.updateDebt(
                    old.copy(
                        personName = updated.personName,
                        note = updated.note,
                        dueDate = updated.dueDate
                    )
                )
                return@withTransaction
            }
            val origin = old.originTransactionId?.let { transactionDao.getTransactionById(it) }
            if (origin != null && updated.accountId != null && updated.amount > 0L) {
                val category = if (updated.type == DebtType.LENT) {
                    ensureNamed("Loan", CategoryType.EXPENSE, 0xFF9E9E9E.toInt(), iconName = "loan")
                } else {
                    ensureNamed("Loan Received", CategoryType.INCOME, 0xFF9E9E9E.toInt(), iconName = "loan received")
                }
                replace(
                    origin,
                    origin.copy(
                        accountId = updated.accountId,
                        categoryId = category.id,
                        amount = updated.amount,
                        timestamp = updated.date,
                        note = if (updated.type == DebtType.LENT) {
                            "Lent to ${updated.personName}"
                        } else {
                            "Borrowed from ${updated.personName}"
                        }
                    )
                )
            }
            debtDao.updateDebt(
                updated.copy(
                    isPaid = false,
                    originTransactionId = old.originTransactionId,
                    settlementTransactionId = null
                )
            )
        }
    }

    suspend fun deleteDebt(debt: Debt) {
        database.withTransaction {
            val fresh = debtDao.getDebtById(debt.id) ?: return@withTransaction
            fresh.settlementTransactionId?.let { id ->
                transactionDao.getTransactionById(id)?.let { remove(it) }
            }
            fresh.originTransactionId?.let { id ->
                transactionDao.getTransactionById(id)?.let { remove(it) }
            }
            debtDao.deleteDebt(fresh)
        }
    }

    suspend fun exportData(preferences: BackupPreferences): BackupData {
        return database.withTransaction {
            BackupData(
                version = BackupCodec.CURRENT_VERSION,
                accounts = accountDao.listAccounts(),
                categories = categoryDao.listCategories(),
                transactions = transactionDao.listAll(),
                budgetLimits = budgetLimitDao.listAll(),
                savingsGoals = savingsGoalDao.listAll().let { goals ->
                    val funded = transactionDao.goalTotalsList().associate { it.goalId to it.total }
                    goals.map { goal -> goal.copy(currentAmount = funded[goal.id] ?: 0L) }
                },
                recurringTransactions = recurringTransactionDao.listAll(),
                debts = debtDao.listAll(),
                templates = transactionTemplateDao.listAll(),
                preferences = preferences
            )
        }
    }

    suspend fun exportJson(preferences: BackupPreferences): String = BackupCodec.encode(exportData(preferences))

    suspend fun exportTransactionsCsv(): String {
        val accounts = accountDao.listAccounts().associateBy { it.id }
        val categories = categoryDao.listCategories().associateBy { it.id }
        val rows = transactionDao.listAll()
            .sortedBy { it.timestamp }
            .map { transaction ->
                val category = categories[transaction.categoryId]
                CsvTransaction(
                    dateIso = localDateOf(transaction.timestamp).toString(),
                    wallet = accounts[transaction.accountId]?.name.orEmpty(),
                    category = category?.name.orEmpty(),
                    type = TransactionCsv.ledgerType(category?.type?.name, category?.role?.name),
                    amountCentavos = transaction.amount,
                    note = transaction.note,
                    classification = transaction.classification.name
                )
            }
        return TransactionCsv.build(rows)
    }

    suspend fun importTransactionsCsv(csv: String): CsvImportOutcome {
        val parsed = TransactionCsv.parse(csv)
        if (!parsed.headerOk) return CsvImportOutcome(added = 0, skipped = 0, rejected = parsed.rejected, headerOk = false)
        return database.withTransaction {
            val accounts = accountDao.listAccounts().toMutableList()
            val categories = categoryDao.listCategories().toMutableList()
            val existing = transactionDao.listAll().map { transaction ->
                val category = categories.find { it.id == transaction.categoryId }
                CsvTransaction(
                    dateIso = localDateOf(transaction.timestamp).toString(),
                    wallet = accounts.find { it.id == transaction.accountId }?.name.orEmpty(),
                    category = category?.name.orEmpty(),
                    type = TransactionCsv.ledgerType(category?.type?.name, category?.role?.name),
                    amountCentavos = transaction.amount,
                    note = transaction.note,
                    classification = transaction.classification.name
                )
            }
            val apply = TransactionCsv.newRows(existing.map { TransactionCsv.identity(it) }.toSet(), parsed.rows)
            var colorCursor = categories.size
            for (row in apply.fresh) {
                val account = accountForImport(accounts, row.wallet)
                val category = categoryForImport(categories, row, colorCursor)
                if (categories.none { it.id == category.id }) {
                    categories += category
                    colorCursor++
                }
                val classification = runCatching { ExpenseClassification.valueOf(row.classification) }
                    .getOrDefault(ExpenseClassification.NONE)
                post(
                    Transaction(
                        accountId = account.id,
                        categoryId = category.id,
                        amount = row.amountCentavos,
                        timestamp = localNoon(LocalDate.parse(row.dateIso)),
                        note = row.note,
                        classification = classification
                    )
                )
            }
            CsvImportOutcome(
                added = apply.fresh.size,
                skipped = apply.skipped,
                rejected = parsed.rejected,
                headerOk = true
            )
        }
    }

    private suspend fun accountForImport(accounts: MutableList<Account>, name: String): Account {
        accounts.find { it.name.equals(name, ignoreCase = true) }?.let { return it }
        val id = accountDao.insertAccount(
            Account(
                name = name,
                type = AccountType.CHECKING,
                balance = 0L,
                openingBalance = 0L,
                colorArgb = 0xFF3B82F6.toInt(),
                includeInTotalBalance = true
            )
        )
        val created = accountDao.getAccountById(id) ?: error("Wallet insert failed")
        accounts += created
        return created
    }

    private suspend fun categoryForImport(
        categories: MutableList<Category>,
        row: CsvTransaction,
        colorCursor: Int
    ): Category {
        categories.find { it.name.equals(row.category, ignoreCase = true) }?.let { return it }
        if (row.type == "TRANSFER") {
            val inbound = row.category.contains("deposit", ignoreCase = true) ||
                row.category.contains("transfer in", ignoreCase = true)
            return if (inbound) {
                ensureRole(CategoryRole.TRANSFER_IN, "Deposit / Transfer In", CategoryType.INCOME, 0xFF4CAF50.toInt())
            } else {
                ensureRole(CategoryRole.TRANSFER_OUT, "Withdraw / Transfer Out", CategoryType.EXPENSE, 0xFFF44336.toInt())
            }
        }
        val type = if (row.type == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE
        val colors = intArrayOf(
            0xFF3B82F6.toInt(),
            0xFFF43F5E.toInt(),
            0xFFF59E0B.toInt(),
            0xFF14B8A6.toInt(),
            0xFF6366F1.toInt(),
            0xFF8B5CF6.toInt(),
            0xFF10B981.toInt()
        )
        val id = categoryDao.insertCategory(
            Category(
                name = row.category,
                type = type,
                colorArgb = colors[colorCursor % colors.size],
                role = CategoryRole.NORMAL
            )
        )
        return categoryDao.getCategoryById(id) ?: error("Category insert failed")
    }

    suspend fun restoreBackup(decoded: DecodedBackup): BackupPreferences? {
        return database.withTransaction {
            transactionDao.deleteAll()
            recurringTransactionDao.deleteAll()
            transactionTemplateDao.deleteAll()
            budgetLimitDao.deleteAll()
            debtDao.deleteAll()
            savingsGoalDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()

            val data = decoded.data
            for (account in data.accounts) accountDao.insertAccount(account)
            for (category in data.categories) categoryDao.insertCategory(category)
            for (goal in data.savingsGoals) savingsGoalDao.insertGoal(goal)
            for (limit in data.budgetLimits) budgetLimitDao.insertBudgetLimit(limit)
            for (debt in data.debts) debtDao.insertDebt(debt)
            for (transaction in data.transactions) transactionDao.insertTransaction(transaction)
            for (recurring in data.recurringTransactions) recurringTransactionDao.insertRecurringTransaction(recurring)
            for (template in data.templates) transactionTemplateDao.insertTemplate(template)

            if (decoded.legacyPesos) {
                for (account in accountDao.listAccounts()) {
                    val net = transactionDao.netForAccount(account.id)
                    accountDao.updateAccount(account.copy(openingBalance = account.balance - net))
                }
            }
            rebuildBalances()
            data.preferences
        }
    }

    private suspend fun rebuildBalances() {
        for (account in accountDao.listAccounts()) {
            val net = transactionDao.netForAccount(account.id)
            accountDao.setBalance(account.id, account.openingBalance + net)
        }
    }

    private suspend fun post(transaction: Transaction): Long {
        val category = categoryDao.getCategoryById(transaction.categoryId)
            ?: error("Category ${transaction.categoryId} is missing")
        val id = transactionDao.insertTransaction(transaction)
        accountDao.adjustBalance(transaction.accountId, signedDelta(category.type.toKind(), transaction.amount))
        return id
    }

    private suspend fun remove(transaction: Transaction) {
        val category = categoryDao.getCategoryById(transaction.categoryId)
        if (category != null) {
            accountDao.adjustBalance(transaction.accountId, -signedDelta(category.type.toKind(), transaction.amount))
        }
        transactionDao.deleteTransaction(transaction)
    }

    private suspend fun replace(old: Transaction, updated: Transaction) {
        remove(old)
        post(updated.copy(id = old.id))
    }

    private suspend fun postRule(ruleId: Long, now: Long) {
        val fresh = recurringTransactionDao.getById(ruleId) ?: return
        if (fresh.paused) return
        if (fresh.nextRunTime > now) return
        if (fresh.endDate != null && fresh.nextRunTime > fresh.endDate) return
        val plan = planOccurrences(
            nextRunTime = fresh.nextRunTime,
            interval = fresh.frequency.toInterval(),
            anchorDay = fresh.anchorDay,
            now = now
        )
        val category = categoryDao.getCategoryById(fresh.categoryId) ?: return
        val already = transactionDao.timestampsForRecurring(fresh.id).toSet()
        val stamps = occurrencesOnOrBefore(occurrencesToInsert(plan.dueTimestamps, already), fresh.endDate)
        for (stamp in stamps) {
            val inserted = transactionDao.insertTransactionIgnoringConflict(
                Transaction(
                    accountId = fresh.accountId,
                    categoryId = fresh.categoryId,
                    amount = fresh.amount,
                    timestamp = stamp,
                    note = fresh.note,
                    classification = fresh.classification,
                    recurringId = fresh.id
                )
            )
            if (inserted > 0L) {
                accountDao.adjustBalance(fresh.accountId, signedDelta(category.type.toKind(), fresh.amount))
            }
        }
        if (plan.nextRunTime != fresh.nextRunTime) {
            recurringTransactionDao.updateRecurringTransaction(fresh.copy(nextRunTime = plan.nextRunTime))
        }
    }

    private suspend fun ensureRole(role: CategoryRole, name: String, type: CategoryType, colorArgb: Int): Category {
        categoryDao.findByRole(role)?.let { return it }
        return ensureNamed(name, type, colorArgb, role)
    }

    private suspend fun ensureNamed(
        name: String,
        type: CategoryType,
        colorArgb: Int,
        role: CategoryRole = CategoryRole.NORMAL,
        iconName: String? = null
    ): Category {
        val named = categoryDao.findByNameAndType(name, type)
        if (named != null) {
            val nextIcon = if (named.iconName.isNullOrBlank()) iconName else named.iconName
            if (named.role != role || nextIcon != named.iconName) {
                val updated = named.copy(role = role, iconName = nextIcon)
                categoryDao.updateCategory(updated)
                return updated
            }
            return named
        }
        val id = categoryDao.insertCategory(
            Category(name = name, type = type, colorArgb = colorArgb, role = role, iconName = iconName)
        )
        return categoryDao.getCategoryById(id) ?: error("Category insert failed")
    }

    private fun sameMonth(timestamp: Long, month: Int, year: Int): Boolean {
        val date = localDateOf(timestamp)
        return date.monthValue == month && date.year == year
    }
}
