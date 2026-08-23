package com.example.budgettracker.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Transaction
import kotlinx.coroutines.launch

class DebtTrackerViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val debts: StateFlow<List<Debt>> = repository.getAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDebt(
        personName: String, 
        amount: Double, 
        type: DebtType, 
        note: String, 
        accountId: Long,
        dueDate: Long?,
        interestRate: Double,
        startDate: Long
    ) {
        viewModelScope.launch {
            val debt = Debt(
                personName = personName,
                amount = amount,
                type = type,
                date = startDate,
                isPaid = false,
                note = note,
                dueDate = dueDate,
                interestRate = interestRate,
                accountId = accountId
            )
            repository.insertDebt(debt)

            val allCategories = repository.getAllCategories().first()
            val catName = if (type == DebtType.LENT) "Loan" else "Loan Received"
            val catType = if (type == DebtType.LENT) CategoryType.EXPENSE else CategoryType.INCOME
            
            var category = allCategories.find { it.name == catName && it.type == catType }
            if (category == null) {
                val newCatId = repository.insertCategory(Category(name = catName, type = catType, colorArgb = 0xFF9E9E9E.toInt()))
                category = Category(id = newCatId, name = catName, type = catType, colorArgb = 0xFF9E9E9E.toInt())
            }
            
            val transaction = Transaction(
                accountId = accountId,
                categoryId = category.id,
                amount = amount,
                timestamp = startDate,
                note = if (type == DebtType.LENT) "Lent to $personName" else "Borrowed from $personName",
                classification = com.example.budgettracker.data.local.entity.ExpenseClassification.NONE
            )
            repository.insertTransaction(transaction)
        }
    }

    fun toggleDebtStatus(debt: Debt, accountId: Long?) {
        viewModelScope.launch {
            val newStatus = !debt.isPaid
            repository.updateDebt(debt.copy(isPaid = newStatus))

            if (newStatus && accountId != null) {
                // It was unpaid, now paid. Create offsetting transaction.
                val allCategories = repository.getAllCategories().first()
                val catName = if (debt.type == DebtType.LENT) "Loan Repaid" else "Loan Paid"
                // If I lent money, and it's paid back, that's an INCOME (money comes in).
                // If I borrowed money, and it's paid back, that's an EXPENSE (money goes out).
                val catType = if (debt.type == DebtType.LENT) CategoryType.INCOME else CategoryType.EXPENSE
                
                var category = allCategories.find { it.name == catName && it.type == catType }
                if (category == null) {
                    val newCatId = repository.insertCategory(Category(name = catName, type = catType, colorArgb = 0xFF4CAF50.toInt()))
                    category = Category(id = newCatId, name = catName, type = catType, colorArgb = 0xFF4CAF50.toInt())
                }
                
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - debt.date).coerceAtLeast(0)
                val interest = debt.amount * (debt.interestRate / 100.0) * (days / 365.0)
                val totalAmount = debt.amount + interest
                
                val transaction = Transaction(
                    accountId = accountId,
                    categoryId = category.id,
                    amount = totalAmount,
                    timestamp = System.currentTimeMillis(),
                    note = if (debt.type == DebtType.LENT) "Payment received from ${debt.personName}" else "Repayment to ${debt.personName}",
                    classification = com.example.budgettracker.data.local.entity.ExpenseClassification.NONE
                )
                repository.insertTransaction(transaction)
            }
        }
    }

    fun updateDebt(
        debt: Debt,
        personName: String, 
        amount: Double, 
        type: DebtType, 
        note: String, 
        accountId: Long,
        dueDate: Long?,
        interestRate: Double,
        startDate: Long
    ) {
        viewModelScope.launch {
            val updatedDebt = debt.copy(
                personName = personName,
                amount = amount,
                type = type,
                date = startDate,
                note = note,
                dueDate = dueDate,
                interestRate = interestRate,
                accountId = accountId
            )
            repository.updateDebt(updatedDebt)
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }
}
