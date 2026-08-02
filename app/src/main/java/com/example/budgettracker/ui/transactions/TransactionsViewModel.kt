package com.example.budgettracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.model.TransactionUiItem
import kotlinx.coroutines.launch
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class TransactionsUiState(
    val transactions: List<TransactionUiItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val filterCategoryIds: Set<Long> = emptySet(),
    val filterAccountId: Long? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val searchQuery: String = ""
)


class TransactionsViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _filterCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _filterAccountId = MutableStateFlow<Long?>(null)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.getAllCategories(),
        repository.getAllAccounts(),
        repository.getRecentTransactions(),
        _filterCategoryIds,
        _filterAccountId,
        _startDate,
        _endDate,
        _searchQuery
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val categories = params[0] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val accounts = params[1] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val transactions = params[2] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val filterCats = params[3] as Set<Long>
        val filterAcc = params[4] as Long?
        val start = params[5] as Long?
        val end = params[6] as Long?
        val search = params[7] as String

        val filteredTxs = transactions.filter { tx ->
            val matchCat = filterCats.isEmpty() || tx.categoryId in filterCats
            val matchAcc = filterAcc == null || tx.accountId == filterAcc
            val matchDate = (start == null || tx.timestamp >= start) && (end == null || tx.timestamp <= end)
            
            // Search by note or category name (category name check requires fetching category, but tx has categoryId, 
            // we can just check note for now, or find category first)
            val categoryName = categories.find { it.id == tx.categoryId }?.name ?: ""
            val matchSearch = search.isEmpty() || 
                tx.note.contains(search, ignoreCase = true) || 
                categoryName.contains(search, ignoreCase = true)
                
            matchCat && matchAcc && matchDate && matchSearch
        }

        val uiItems = filteredTxs.map { tx ->
            val category = categories.find { it.id == tx.categoryId }
            TransactionUiItem(
                transaction = tx,
                categoryName = category?.name ?: "Unknown",
                categoryType = category?.type ?: CategoryType.EXPENSE
            )
        }
        TransactionsUiState(
            transactions = uiItems,
            categories = categories,
            accounts = accounts,
            filterCategoryIds = filterCats,
            filterAccountId = filterAcc,
            startDate = start,
            endDate = end,
            searchQuery = search
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun applyFilters(categoryIds: Set<Long>, accountId: Long?, start: Long?, end: Long?) {
        _filterCategoryIds.value = categoryIds
        _filterAccountId.value = accountId
        _startDate.value = start
        _endDate.value = end
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _filterCategoryIds.value = emptySet()
        _filterAccountId.value = null
        _startDate.value = null
        _endDate.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
