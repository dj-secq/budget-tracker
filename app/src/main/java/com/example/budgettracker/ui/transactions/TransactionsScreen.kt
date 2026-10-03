package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.budgettracker.R
import com.example.budgettracker.ui.model.TransactionUiItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.domain.endOfLocalDay
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.startOfLocalDay
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CategoryIconHelper
import com.example.budgettracker.ui.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var transactionToDelete by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Transaction?>(null) }
    
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    val groupedTransactions = remember(uiState.transactions) {
        uiState.transactions.groupBy { dayFormat.format(localDateOf(it.transaction.timestamp)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transaction_short))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_transactions), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_cd),
                            tint = if (uiState.filterCategoryIds.isNotEmpty() || uiState.filterAccountId != null || uiState.startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.search_transactions)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_cd)) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, top = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (uiState.searchQuery.isNotEmpty() || uiState.filterCategoryIds.isNotEmpty() || uiState.filterAccountId != null || uiState.startDate != null)
                            stringResource(R.string.no_transactions_match)
                        else stringResource(R.string.no_transactions), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(transactions, key = { it.transaction.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        transactionToDelete = item.transaction
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )
                            
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete_transaction_cd),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Card(
                                    onClick = { onEditTransaction(item.transaction.id) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIconForCategory(item.categoryName),
                                            contentDescription = item.categoryName,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.transaction.note.ifEmpty { item.categoryName }, 
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.categoryName,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                if (item.accountName.isNotBlank()) {
                                                    Text(
                                                        text = " • ${item.accountName}",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                                if (item.transaction.classification != com.example.budgettracker.data.local.entity.ExpenseClassification.NONE) {
                                                    Text(
                                                        text = " • ${item.transaction.classification.name}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        val isIncome = item.categoryType == com.example.budgettracker.data.local.entity.CategoryType.INCOME
                                        Text(
                                            text = "${if (isIncome) "+" else "-"}${CurrencyUtils.formatAmount(item.transaction.amount)}",
                                            color = if (isIncome) EmeraldGreen else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.canLoadMore) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMore() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.load_more))
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion)) },
            text = { Text(stringResource(R.string.delete_confirmation_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteTransaction(transaction) { removed ->
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transaction deleted",
                                    actionLabel = "Undo"
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTransactions(removed)
                                }
                            }
                        }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (showFilterSheet) {
        var tempCategoryIds by remember { mutableStateOf(uiState.filterCategoryIds) }
        var tempAccountId by remember { mutableStateOf(uiState.filterAccountId) }
        var tempStartDate by remember { mutableStateOf(uiState.startDate) }
        var tempEndDate by remember { mutableStateOf(uiState.endDate) }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { 
                        tempCategoryIds = emptySet()
                        tempAccountId = null
                        tempStartDate = null
                        tempEndDate = null
                        viewModel.clearFilters()
                    }) {
                        Text(stringResource(R.string.clear_all))
                    }
                }

                Text(stringResource(R.string.category_label), fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = tempCategoryIds.isEmpty(),
                            onClick = { tempCategoryIds = emptySet() },
                            label = { Text(stringResource(R.string.filter_all)) }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = tempCategoryIds.contains(category.id),
                            onClick = { 
                                tempCategoryIds = if (tempCategoryIds.contains(category.id)) {
                                    tempCategoryIds - category.id
                                } else {
                                    tempCategoryIds + category.id
                                }
                            },
                            label = { Text(category.name) }
                        )
                    }
                }

                Text(stringResource(R.string.account_label), fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = tempAccountId == null,
                            onClick = { tempAccountId = null },
                            label = { Text(stringResource(R.string.filter_all)) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                    items(uiState.accounts) { account ->
                        FilterChip(
                            selected = tempAccountId == account.id,
                            onClick = { tempAccountId = account.id },
                            label = { Text(account.name) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                Text(stringResource(R.string.date_label), fontWeight = FontWeight.Bold)
                
                var showStartDatePicker by remember { mutableStateOf(false) }
                var showEndDatePicker by remember { mutableStateOf(false) }

                val startState = rememberDatePickerState(initialSelectedDateMillis = tempStartDate)
                val endState = rememberDatePickerState(initialSelectedDateMillis = tempEndDate)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tempStartDate?.let { 
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                        } ?: stringResource(R.string.start_date))
                    }

                    Text(stringResource(R.string.filter_to))

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tempEndDate?.let { 
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                        } ?: "End Date")
                    }
                }

                if (showStartDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                tempStartDate = startState.selectedDateMillis?.let {
                                    startOfLocalDay(localDateFromPickerUtc(it))
                                }
                                showStartDatePicker = false
                            }) { Text(stringResource(R.string.ok)) }
                        }
                    ) { DatePicker(state = startState) }
                }

                if (showEndDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                tempEndDate = endState.selectedDateMillis?.let {
                                    endOfLocalDay(localDateFromPickerUtc(it))
                                }
                                showEndDatePicker = false
                            }) { Text(stringResource(R.string.ok)) }
                        }
                    ) { DatePicker(state = endState) }
                }

                Button(
                    onClick = { 
                        viewModel.applyFilters(tempCategoryIds, tempAccountId, tempStartDate, tempEndDate)
                        showFilterSheet = false 
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.apply_filters))
                }
            }
        }
    }
}
