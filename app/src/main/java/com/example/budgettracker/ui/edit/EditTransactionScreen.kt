package com.example.budgettracker.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.res.stringResource
import com.example.budgettracker.R
import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localNoon
import com.example.budgettracker.ui.utils.CurrencyUtils
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.ui.theme.CategoryColors
import com.example.budgettracker.ui.theme.WalletPalette
import com.example.budgettracker.ui.utils.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long,
    viewModel: EditTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val transaction by viewModel.transactionFlow.collectAsState()
    
    val haptic = LocalHapticFeedback.current

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var transactionType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var selectedClassification by remember { mutableStateOf(ExpenseClassification.NONE) }
    
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    
    val overBudgetWarning by viewModel.showOverBudgetWarning.collectAsState()
    val bucketWarning by viewModel.showBucketWarning.collectAsState()

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    LaunchedEffect(transaction, categories) {
        if (!isInitialized && transaction != null && categories.isNotEmpty()) {
            val tx = transaction!!
            amount = Money.toInputString(tx.amount)
            note = tx.note
            selectedCategoryId = tx.categoryId
            selectedAccountId = tx.accountId
            selectedDateMillis = tx.timestamp
            selectedClassification = tx.classification
            
            val cat = categories.find { it.id == tx.categoryId }
            if (cat != null) {
                transactionType = cat.type
            }
            isInitialized = true
        }
    }
    
    val filteredCategories = categories.filter { it.type == transactionType }
    val loaded = transaction
    val formDirty = isInitialized && loaded != null && (
        amount != Money.toInputString(loaded.amount) ||
            note != loaded.note ||
            selectedCategoryId != loaded.categoryId ||
            selectedAccountId != loaded.accountId ||
            selectedDateMillis != loaded.timestamp ||
            selectedClassification != loaded.classification
        )
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(enabled = formDirty) { confirmLeave = true }
    fun requestLeave() {
        if (formDirty) confirmLeave = true else onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_transaction)) },
                navigationIcon = {
                    IconButton(onClick = { requestLeave() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (!isInitialized) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                label = { Text(stringResource(R.string.amount_label)) },
                prefix = { Text("₱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Transaction Type Toggle
            TabRow(
                selectedTabIndex = if (transactionType == CategoryType.EXPENSE) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = transactionType == CategoryType.EXPENSE,
                    onClick = { transactionType = CategoryType.EXPENSE; selectedCategoryId = null },
                    text = { Text(stringResource(R.string.expense), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = transactionType == CategoryType.INCOME,
                    onClick = { transactionType = CategoryType.INCOME; selectedCategoryId = null },
                    text = { Text(stringResource(R.string.income), fontWeight = FontWeight.Bold) }
                )
            }

            // Account Selection
            Text(stringResource(R.string.account_label), fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts) { account ->
                    val isSelected = selectedAccountId == account.id
                    val color = Color(account.colorArgb)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .background(
                                color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAccountId = account.id }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = account.name,
                            color = if (isSelected) WalletPalette.contentOn(account.colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Selection
            Text(stringResource(R.string.category_label), fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunkedCategories = filteredCategories.chunked(3)
                chunkedCategories.forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { category ->
                            val isSelected = selectedCategoryId == category.id
                            val color = CategoryColors[(category.id % CategoryColors.size).toInt()]
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 88.dp)
                                    .clickable { selectedCategoryId = category.id }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = com.example.budgettracker.ui.utils.CategoryIconHelper.getIconForCategory(category.name),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        softWrap = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        repeat(3 - rowCategories.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Classification Selection (Only for Expenses)
            if (transactionType == CategoryType.EXPENSE) {
                Text(stringResource(R.string.classification), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val classifications = listOf(
                        ExpenseClassification.NEED,
                        ExpenseClassification.WANT,
                        ExpenseClassification.SAVING,
                        ExpenseClassification.NONE
                    )
                    classifications.forEach { clazz ->
                        FilterChip(
                            selected = selectedClassification == clazz,
                            onClick = { selectedClassification = clazz },
                            label = { Text(clazz.label()) }
                        )
                    }
                }
            }
            
            // Date Selection
            val formattedDate = remember(selectedDateMillis) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            }
            OutlinedTextField(
                value = formattedDate,
                onValueChange = { },
                label = { Text(stringResource(R.string.date_label)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDateMillis = localNoon(localDateFromPickerUtc(it))
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel_button))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.weight(1f))

            val saveEnabled = amount.isNotEmpty() && selectedCategoryId != null && selectedAccountId != null && !isSaving
            val saveHint = when {
                isSaving -> stringResource(R.string.saving_hint)
                Money.parsePesos(amount) == null || (Money.parsePesos(amount) ?: 0L) <= 0L -> stringResource(R.string.amount_required)
                selectedAccountId == null -> stringResource(R.string.choose_wallet)
                selectedCategoryId == null -> stringResource(R.string.choose_category)
                else -> null
            }

            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.delete_transaction))
            }

            // Save Button
            Button(
                onClick = {
                    val parsedAmount = Money.parsePesos(amount)
                    if (parsedAmount != null && parsedAmount > 0L && selectedCategoryId != null && selectedAccountId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (transactionType == CategoryType.EXPENSE) {
                            viewModel.onConfirmSave(
                                transactionId = transactionId,
                                accountId = selectedAccountId!!,
                                categoryId = selectedCategoryId!!,
                                amount = parsedAmount,
                                note = note,
                                timestamp = selectedDateMillis,
                                classification = selectedClassification
                            ) {
                                onNavigateBack()
                            }
                        } else {
                            viewModel.saveTransactionWithoutLimits(
                                transactionId = transactionId,
                                accountId = selectedAccountId!!,
                                categoryId = selectedCategoryId!!,
                                amount = parsedAmount,
                                note = note,
                                timestamp = selectedDateMillis,
                                classification = ExpenseClassification.NONE
                            ) {
                                onNavigateBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = saveEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save_transaction), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
            if (!saveEnabled && saveHint != null) {
                Text(saveHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.keep_editing)) }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_deletion)) },
            text = { Text(stringResource(R.string.delete_confirmation_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteTransaction { onNavigateBack() }
                }) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    // Bucket warning dialog
    bucketWarning?.let { (bucketName, excess, isStrict) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarnings() },
            title = { Text(stringResource(R.string.cap_reached, bucketName)) },
            text = {
                Text(
                    if (isStrict) {
                        "This edit exceeds your $bucketName allocation by ${CurrencyUtils.formatAmount(excess)}.\n\nStrict limits are on, so this cannot be saved."
                    } else {
                        "This edit exceeds your $bucketName allocation by ${CurrencyUtils.formatAmount(excess)}.\n\nSave it anyway?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarnings() }) {
                    Text(if (isStrict) stringResource(R.string.ok) else stringResource(R.string.cancel_button))
                }
            },
            dismissButton = if (!isStrict) {
                {
                    TextButton(onClick = {
                        viewModel.dismissWarnings()
                        val parsedAmount = Money.parsePesos(amount)
                        if (parsedAmount != null && parsedAmount > 0L && selectedCategoryId != null && selectedAccountId != null) {
                            viewModel.saveTransactionWithoutLimits(
                                transactionId = transactionId,
                                accountId = selectedAccountId!!,
                                categoryId = selectedCategoryId!!,
                                amount = parsedAmount,
                                note = note,
                                timestamp = selectedDateMillis,
                                classification = selectedClassification
                            ) {
                                onNavigateBack()
                            }
                        }
                    }) { Text(stringResource(R.string.save_anyway)) }
                }
            } else null
        )
    }

    // Over budget warning dialog
    overBudgetWarning?.let { (excess, isStrict) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarnings() },
            title = { Text(stringResource(R.string.over_budget)) },
            text = { 
                if (isStrict) {
                    Text(stringResource(R.string.edit_over_budget_blocked, CurrencyUtils.formatAmount(excess)))
                } else {
                    Text(stringResource(R.string.edit_over_budget_confirm, CurrencyUtils.formatAmount(excess)))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarnings() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = if (!isStrict) {
                {
                    TextButton(onClick = {
                        viewModel.dismissWarnings()
                        val parsedAmount = Money.parsePesos(amount)
                        if (parsedAmount != null && parsedAmount > 0L && selectedCategoryId != null && selectedAccountId != null) {
                            viewModel.saveTransactionWithoutLimits(
                                transactionId = transactionId,
                                accountId = selectedAccountId!!,
                                categoryId = selectedCategoryId!!,
                                amount = parsedAmount,
                                note = note,
                                timestamp = selectedDateMillis,
                                classification = selectedClassification
                            ) {
                                onNavigateBack()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.save_anyway_title))
                    }
                }
            } else null
        )
    }
}
