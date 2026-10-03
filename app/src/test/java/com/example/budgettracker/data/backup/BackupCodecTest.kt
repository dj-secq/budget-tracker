package com.example.budgettracker.data.backup

import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.data.local.entity.BudgetLimit
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryRole
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.TransactionTemplate
import com.example.budgettracker.domain.localDateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test
    fun versionTwoRoundTripKeepsDebtsTemplatesPreferencesAndCentavos() {
        val start = 1_767_225_600_000L
        val original = BackupData(
            accounts = listOf(
                Account(1, "Cash", AccountType.CHECKING, 150_000L, 0xFF3B82F6.toInt(), true, 100_000L)
            ),
            categories = listOf(
                Category(2, "Withdraw / Transfer Out", CategoryType.EXPENSE, 1, role = CategoryRole.TRANSFER_OUT)
            ),
            transactions = listOf(
                Transaction(3, 1, 2, 4_050L, start, "move", classification = ExpenseClassification.NONE)
            ),
            budgetLimits = listOf(BudgetLimit(4, 2, 20_000L, 9, 2026)),
            savingsGoals = listOf(SavingsGoal(5, "Trip", 100_000L, 2_500L, contributionAmount = 1_000L)),
            recurringTransactions = listOf(
                RecurringTransaction(
                    6, 1, 2, 5_000L, "rent", ExpenseClassification.NEED,
                    Frequency.MONTHLY, start, start, anchorDay = 31,
                    paused = true, endDate = start
                )
            ),
            debts = listOf(Debt(7, "Ana", 8_000L, DebtType.LENT, start, note = "lunch", interestRate = 5.0, accountId = 1)),
            templates = listOf(
                TransactionTemplate(8, "Coffee", 6_500L, 2, 1, "", CategoryType.EXPENSE, ExpenseClassification.WANT)
            ),
            preferences = BackupPreferences(
                needsPercent = 55,
                wantsPercent = 25,
                savingsPercent = 20,
                rolloverBudgetsEnabled = true,
                strictLimitsEnabled = false,
                lastAccountId = 1
            )
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(original))

        assertFalse(decoded.legacyPesos)
        assertEquals(BackupCodec.CURRENT_VERSION, decoded.data.version)
        assertEquals(150_000L, decoded.data.accounts.single().balance)
        assertEquals(100_000L, decoded.data.accounts.single().openingBalance)
        assertEquals(CategoryRole.TRANSFER_OUT, decoded.data.categories.single().role)
        assertEquals(4_050L, decoded.data.transactions.single().amount)
        assertEquals(20_000L, decoded.data.budgetLimits.single().assignedAmount)
        assertEquals(1_000L, decoded.data.savingsGoals.single().contributionAmount)
        assertEquals(31, decoded.data.recurringTransactions.single().anchorDay)
        assertTrue(decoded.data.recurringTransactions.single().paused)
        assertEquals(start, decoded.data.recurringTransactions.single().endDate)
        assertEquals("Ana", decoded.data.debts.single().personName)
        assertEquals(8_000L, decoded.data.debts.single().amount)
        assertEquals(6_500L, decoded.data.templates.single().amount)
        assertEquals(55, decoded.data.preferences?.needsPercent)
        assertEquals(true, decoded.data.preferences?.rolloverBudgetsEnabled)
        assertEquals(1L, decoded.data.preferences?.lastAccountId)
    }

    @Test
    fun legacyPesoBackupConvertsMoneyRolesAndAnchorDay() {
        val awkward = 0.1 + 0.2
        val start = 1_767_225_600_000L
        val json = """
            {
              "accounts": [
                {"id": 1, "name": "Cash", "type": "CHECKING", "balance": $awkward, "colorArgb": 12}
              ],
              "categories": [
                {"id": 1, "name": "Withdraw / Transfer Out", "type": "EXPENSE", "colorArgb": 1},
                {"id": 2, "name": "Deposit / Transfer In", "type": "INCOME", "colorArgb": 2},
                {"id": 3, "name": "Groceries", "type": "EXPENSE", "colorArgb": 3}
              ],
              "transactions": [
                {"id": 9, "accountId": 1, "categoryId": 3, "amount": 10.5, "timestamp": $start, "note": "milk", "classification": "NEED"}
              ],
              "budgetLimits": [
                {"id": 4, "categoryId": 3, "assignedAmount": 100.25, "month": 9, "year": 2026}
              ],
              "savingsGoals": [
                {"id": 5, "name": "Trip", "targetAmount": 1000.0, "currentAmount": 10.5, "contributionAmount": 2.5}
              ],
              "recurringTransactions": [
                {
                  "id": 6, "accountId": 1, "categoryId": 3, "amount": 5.0, "note": "",
                  "classification": "NONE", "frequency": "MONTHLY",
                  "startDate": $start, "nextRunTime": $start
                }
              ],
              "debts": [
                {"id": 7, "personName": "Ana", "amount": 20.0, "type": "LENT", "date": $start, "isPaid": false, "note": "", "interestRate": 10.0, "accountId": 1}
              ],
              "templates": [
                {"id": 8, "templateName": "Coffee", "amount": 3.5, "categoryId": 3, "accountId": 1, "note": "", "transactionType": "EXPENSE", "classification": "WANT"}
              ],
              "preferences": {
                "needsPercent": 50, "wantsPercent": 30, "savingsPercent": 20,
                "themeMode": "SYSTEM", "dailyRemindersEnabled": false,
                "rolloverBudgetsEnabled": true, "strictLimitsEnabled": false,
                "lastAccountId": 1
              }
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(json)
        val data = decoded.data

        assertTrue(decoded.legacyPesos)
        assertEquals(2, data.version)
        assertEquals(30L, data.accounts.single().balance)
        assertEquals(0L, data.accounts.single().openingBalance)
        assertTrue(data.accounts.single().includeInTotalBalance)
        assertEquals(CategoryRole.TRANSFER_OUT, data.categories[0].role)
        assertEquals(CategoryRole.TRANSFER_IN, data.categories[1].role)
        assertEquals(CategoryRole.NORMAL, data.categories[2].role)
        assertEquals(1_050L, data.transactions.single().amount)
        assertEquals(10_025L, data.budgetLimits.single().assignedAmount)
        assertEquals(100_000L, data.savingsGoals.single().targetAmount)
        assertEquals(1_050L, data.savingsGoals.single().currentAmount)
        assertEquals(250L, data.savingsGoals.single().contributionAmount)
        assertEquals(500L, data.recurringTransactions.single().amount)
        assertEquals(localDateOf(start).dayOfMonth, data.recurringTransactions.single().anchorDay)
        assertFalse(data.recurringTransactions.single().paused)
        assertNull(data.recurringTransactions.single().endDate)
        assertEquals(2_000L, data.debts.single().amount)
        assertNull(data.debts.single().originTransactionId)
        assertEquals(350L, data.templates.single().amount)
        assertEquals("EMERALD", data.preferences?.accent)
        assertEquals(false, data.preferences?.dynamicColor)
        assertEquals(20, data.preferences?.reminderHour)
        assertEquals(true, data.preferences?.rolloverBudgetsEnabled)
        assertEquals(1L, data.preferences?.lastAccountId)
    }

    @Test
    fun missingAppearanceFieldsRestoreEmeraldOffAndEightPm() {
        val json = """
            {
              "version": 2,
              "preferences": {
                "needsPercent": 50, "wantsPercent": 30, "savingsPercent": 20,
                "themeMode": "DARK"
              }
            }
        """.trimIndent()
        val prefs = BackupCodec.decode(json).data.preferences
        assertEquals("EMERALD", prefs?.accent)
        assertEquals(false, prefs?.dynamicColor)
        assertEquals(20, prefs?.reminderHour)
    }
}
