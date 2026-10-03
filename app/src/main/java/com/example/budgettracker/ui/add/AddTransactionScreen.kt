package com.example.budgettracker.ui.add

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.budgettracker.R
import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.splitPartsMatch
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localNoon
import com.example.budgettracker.ui.utils.CurrencyUtils
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.ui.theme.CategoryColors
import com.example.budgettracker.ui.theme.WalletPalette
import com.example.budgettracker.ui.utils.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val haptic = LocalHapticFeedback.current

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var transactionType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var selectedClassification by remember { mutableStateOf(ExpenseClassification.NONE) }
    
    var isTransfer by remember { mutableStateOf(false) }
    var transferToAccountId by remember { mutableStateOf<Long?>(null) }
    
    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf(Frequency.MONTHLY) }
    var splitPurchase by remember { mutableStateOf(false) }
    var splitLines by remember { mutableStateOf(listOf(SplitDraft(), SplitDraft())) }
    var splitMenu by remember { mutableStateOf<Int?>(null) }
    
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    
    val accounts by viewModel.accounts.collectAsState()
    val lastAccountId by viewModel.lastAccountId.collectAsState()
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var showMore by remember { mutableStateOf(false) }
    var categoryQuery by remember { mutableStateOf("") }
    var confirmLeave by remember { mutableStateOf(false) }
    LaunchedEffect(accounts, lastAccountId) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.find { it.id == lastAccountId }?.id ?: accounts.first().id
        }
    }
    
    val templates by viewModel.templates.collectAsState()
    var showTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    
    val overBudgetWarning by viewModel.showOverBudgetWarning.collectAsState()
    val bucketWarning by viewModel.showBucketWarning.collectAsState()
    
    val filteredCategories = categories.filter { it.type == transactionType }
        .filter { categoryQuery.isBlank() || it.name.contains(categoryQuery, ignoreCase = true) }
    val formDirty = amount.isNotBlank() || note.isNotBlank() || selectedCategoryId != null || isTransfer || isRecurring || splitPurchase
    BackHandler(enabled = formDirty) { confirmLeave = true }
    fun requestLeave() {
        if (formDirty) confirmLeave = true else onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scannerHelper = remember { ReceiptScannerHelper(context) }
    var isScanning by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isScanning = true
                coroutineScope.launch {
                    val result = scannerHelper.scanReceipt(uri)
                    if (result != null) {
                        if (result.amount != null) {
                            amount = com.example.budgettracker.domain.ReceiptText.amountInput(result.amount)
                        }
                        if (result.note.isNotBlank()) {
                            note = result.note
                        }
                        Toast.makeText(context, context.getString(R.string.scanned_successfully), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.scan_failed), Toast.LENGTH_SHORT).show()
                    }
                    isScanning = false
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_transaction_title)) },
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quick Add Templates
            if (templates.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.quick_add_templates), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates) { template ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    amount = Money.toInputString(template.amount)
                                    selectedAccountId = template.accountId
                                    selectedCategoryId = template.categoryId
                                    transactionType = template.transactionType
                                    isTransfer = false
                                    note = template.note
                                    selectedClassification = template.classification
                                },
                                label = { Text(template.templateName) }
                            )
                        }
                    }
                }
            }

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

            TextButton(onClick = { showMore = !showMore }) {
                Text(if (showMore) stringResource(R.string.less) else stringResource(R.string.more))
            }
            if (showMore) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.transfer_between_wallets), fontWeight = FontWeight.Bold)
                    Switch(checked = isTransfer, onCheckedChange = {
                        isTransfer = it
                        if (it) {
                            selectedCategoryId = null
                            splitPurchase = false
                        }
                    })
                }
                if (!isTransfer) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        Text(if (isScanning) stringResource(R.string.scanning_receipt) else stringResource(R.string.scan_receipt))
                    }
                }
            }

            // Transaction Type Toggle
            TabRow(
                selectedTabIndex = if (transactionType == CategoryType.EXPENSE) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !isTransfer && transactionType == CategoryType.EXPENSE,
                    onClick = { isTransfer = false; transactionType = CategoryType.EXPENSE; selectedCategoryId = null },
                    text = { Text(stringResource(R.string.expense), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = !isTransfer && transactionType == CategoryType.INCOME,
                    onClick = {
                        isTransfer = false
                        transactionType = CategoryType.INCOME
                        selectedCategoryId = null
                        splitPurchase = false
                    },
                    text = { Text(stringResource(R.string.income), fontWeight = FontWeight.Bold) }
                )
            }

            // Account Selection
            if (!isTransfer) {
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
            } else {
                Text(stringResource(R.string.from_account), fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.to_account), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        val isSelected = transferToAccountId == account.id
                        val color = Color(account.colorArgb)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .background(
                                    color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { transferToAccountId = account.id }
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) WalletPalette.contentOn(account.colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Selection
            if (!isTransfer && !(splitPurchase && transactionType == CategoryType.EXPENSE)) {
                Text(stringResource(R.string.category_label), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = categoryQuery,
                    onValueChange = { categoryQuery = it },
                    label = { Text(stringResource(R.string.search_categories)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        // Fill empty spots if row is not full
                        repeat(3 - rowCategories.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            } // Close if (!isTransfer)

            if (!isTransfer && transactionType == CategoryType.EXPENSE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.split_purchase), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = splitPurchase,
                        onCheckedChange = { checked ->
                            splitPurchase = checked
                            if (checked) {
                                isRecurring = false
                                if (splitLines.size < 2) {
                                    splitLines = listOf(SplitDraft(selectedCategoryId, amount), SplitDraft())
                                }
                            }
                        }
                    )
                }
                if (splitPurchase) {
                    val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
                    splitLines.forEachIndexed { index, line ->
                        val chosen = expenseCategories.find { it.id == line.categoryId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextButton(
                                    onClick = { splitMenu = index },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = chosen?.name ?: stringResource(R.string.split_part_category),
                                        softWrap = true
                                    )
                                }
                                DropdownMenu(
                                    expanded = splitMenu == index,
                                    onDismissRequest = { splitMenu = null }
                                ) {
                                    expenseCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                splitLines = splitLines.mapIndexed { i, draft ->
                                                    if (i == index) draft.copy(categoryId = category.id) else draft
                                                }
                                                splitMenu = null
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = line.amountText,
                                onValueChange = { value ->
                                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        splitLines = splitLines.mapIndexed { i, draft ->
                                            if (i == index) draft.copy(amountText = value) else draft
                                        }
                                    }
                                },
                                prefix = { Text("₱") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (splitLines.size > 2) {
                            TextButton(onClick = {
                                splitLines = splitLines.filterIndexed { i, _ -> i != index }
                            }) { Text(stringResource(R.string.remove_split_part)) }
                        }
                    }
                    TextButton(onClick = { splitLines = splitLines + SplitDraft() }) {
                        Text(stringResource(R.string.add_split_part))
                    }
                    val total = Money.parsePesos(amount) ?: 0L
                    val partSum = splitLines.sumOf { Money.parsePesos(it.amountText) ?: 0L }
                    Text(
                        text = stringResource(
                            R.string.split_sum,
                            CurrencyUtils.formatAmount(partSum),
                            CurrencyUtils.formatAmount(total)
                        ),
                        color = if (partSum == total && total > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true
                    )
                }
            }

            // Classification Selection (Only for Expenses)
            if (showMore && !isTransfer && transactionType == CategoryType.EXPENSE) {
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
            
            // Recurring Transaction Toggle
            if (showMore && !isTransfer && !splitPurchase) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.repeat_transaction), fontWeight = FontWeight.Bold)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            
            if (isRecurring) {
                Text(stringResource(R.string.frequency), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Frequency.values()) { freq ->
                        FilterChip(
                            selected = recurringFrequency == freq,
                            onClick = { recurringFrequency = freq },
                            label = { Text(freq.label()) }
                        )
                    }
                }
            }
            } // Close if (!isTransfer)

            Spacer(modifier = Modifier.weight(1f))

            if (showMore && !isTransfer) {
                OutlinedButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount.isNotBlank() && selectedAccountId != null && selectedCategoryId != null
                ) {
                    Text(stringResource(R.string.save_as_template))
                }
                templates.forEach { template ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(template.templateName)
                        TextButton(onClick = { viewModel.deleteTemplate(template) }) {
                            Text(stringResource(R.string.delete_button))
                        }
                    }
                }
            }

            val parsedTotal = Money.parsePesos(amount)
            val splitParts = splitLines.mapNotNull { line ->
                val id = line.categoryId ?: return@mapNotNull null
                val part = Money.parsePesos(line.amountText) ?: return@mapNotNull null
                id to part
            }
            val splitting = splitPurchase && transactionType == CategoryType.EXPENSE && !isTransfer
            val partsReady = splitting &&
                splitParts.size == splitLines.size &&
                splitPartsMatch(parsedTotal ?: 0L, splitParts.map { it.second })
            val saveEnabled = if (isTransfer) {
                amount.isNotEmpty() && selectedAccountId != null && transferToAccountId != null && selectedAccountId != transferToAccountId && !isSaving
            } else if (splitting) {
                partsReady && selectedAccountId != null && !isSaving
            } else {
                amount.isNotEmpty() && selectedCategoryId != null && selectedAccountId != null && !isSaving
            }
            val saveHint = when {
                isSaving -> stringResource(R.string.saving_hint)
                parsedTotal == null || parsedTotal <= 0L -> stringResource(R.string.amount_required)
                selectedAccountId == null -> stringResource(R.string.choose_wallet)
                isTransfer && (transferToAccountId == null || transferToAccountId == selectedAccountId) -> stringResource(R.string.choose_other_wallet)
                splitting && !partsReady -> stringResource(R.string.split_mismatch)
                !isTransfer && !splitting && selectedCategoryId == null -> stringResource(R.string.choose_category)
                else -> null
            }

                // Save Button
                Button(
                    onClick = {
                    val parsedAmount = Money.parsePesos(amount)
                    if (isTransfer) {
                        if (parsedAmount != null && parsedAmount > 0L && selectedAccountId != null && transferToAccountId != null && selectedAccountId != transferToAccountId) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveTransfer(
                                fromAccountId = selectedAccountId!!,
                                toAccountId = transferToAccountId!!,
                                amount = parsedAmount,
                                note = note,
                                timestamp = selectedDateMillis
                            ) {
                                onNavigateBack()
                            }
                        } else if (selectedAccountId == transferToAccountId && selectedAccountId != null) {
                            Toast.makeText(context, context.getString(R.string.same_account_transfer), Toast.LENGTH_SHORT).show()
                        }
                    } else if (splitting && partsReady && selectedAccountId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onConfirmSplit(
                            accountId = selectedAccountId!!,
                            parts = splitParts,
                            note = note,
                            timestamp = selectedDateMillis,
                            classification = selectedClassification,
                            onComplete = onNavigateBack
                        )
                    } else {
                        if (parsedAmount != null && parsedAmount > 0L && selectedCategoryId != null && selectedAccountId != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (transactionType == CategoryType.EXPENSE) {
                                viewModel.onConfirmSave(
                                    selectedAccountId!!,
                                    selectedCategoryId!!,
                                    parsedAmount,
                                    note,
                                    selectedDateMillis,
                                    selectedClassification,
                                    isRecurring,
                                    if (isRecurring) recurringFrequency else null
                                ) {
                                    onNavigateBack()
                                }
                            } else {
                                viewModel.saveTransaction(
                                    selectedAccountId!!,
                                    selectedCategoryId!!,
                                    parsedAmount,
                                    note,
                                    selectedDateMillis,
                                    ExpenseClassification.NONE,
                                    isRecurring,
                                    if (isRecurring) recurringFrequency else null
                                ) {
                                    onNavigateBack()
                                }
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

    val saveDespiteWarning = {
        viewModel.dismissWarnings()
        val parsedAmount = Money.parsePesos(amount)
        val parts = splitLines.mapNotNull { line ->
            val id = line.categoryId ?: return@mapNotNull null
            val part = Money.parsePesos(line.amountText) ?: return@mapNotNull null
            id to part
        }
        val splittingNow = splitPurchase && transactionType == CategoryType.EXPENSE && !isTransfer
        val ready = splittingNow &&
            parts.size == splitLines.size &&
            splitPartsMatch(parsedAmount ?: 0L, parts.map { it.second })
        if (splittingNow && ready && selectedAccountId != null) {
            viewModel.saveSplit(
                accountId = selectedAccountId!!,
                parts = parts,
                note = note,
                timestamp = selectedDateMillis,
                classification = selectedClassification,
                onComplete = onNavigateBack
            )
        } else if (parsedAmount != null && parsedAmount > 0L && selectedCategoryId != null && selectedAccountId != null) {
            viewModel.saveTransaction(
                selectedAccountId!!,
                selectedCategoryId!!,
                parsedAmount,
                note,
                selectedDateMillis,
                selectedClassification,
                isRecurring,
                if (isRecurring) recurringFrequency else null
            ) { onNavigateBack() }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.discard_entry_title)) },
            text = { Text(stringResource(R.string.discard_entry_body)) },
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

    // Bucket warning dialog (Strict)
    bucketWarning?.let { (bucketName, excess, isStrict) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarnings() },
            title = { Text(stringResource(R.string.cap_reached, bucketName)) },
            text = {
                Text(
                    if (isStrict) {
                        "This transaction exceeds your $bucketName allocation by ${CurrencyUtils.formatAmount(excess)}.\n\nStrict limits are on, so this cannot be saved."
                    } else {
                        "This transaction exceeds your $bucketName allocation by ${CurrencyUtils.formatAmount(excess)}.\n\nSave it anyway?"
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
                    TextButton(onClick = saveDespiteWarning) { Text(stringResource(R.string.save_anyway)) }
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
                    Text(stringResource(R.string.over_budget_blocked, CurrencyUtils.formatAmount(excess)))
                } else {
                    Text(stringResource(R.string.over_budget_confirm, CurrencyUtils.formatAmount(excess)))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarnings() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = if (!isStrict) {
                {
                    TextButton(onClick = saveDespiteWarning) {
                        Text(stringResource(R.string.save_anyway_title))
                    }
                }
            } else null
        )
    }

    // Template Name Dialog
    if (showTemplateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text(stringResource(R.string.save_template_title)) },
            text = {
                OutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text(stringResource(R.string.template_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val parsedAmount = Money.parsePesos(amount) ?: return@TextButton
                        viewModel.saveTemplate(
                            templateName = newTemplateName,
                            amount = parsedAmount,
                            categoryId = selectedCategoryId!!,
                            accountId = selectedAccountId!!,
                            note = note,
                            transactionType = transactionType,
                            classification = selectedClassification
                        )
                        showTemplateDialog = false
                        newTemplateName = ""
                        Toast.makeText(context, context.getString(R.string.template_saved), Toast.LENGTH_SHORT).show()
                    },
                    enabled = newTemplateName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTemplateDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Handle back navigation on success (when not using manual callback)
    LaunchedEffect(isSaving) {
        if (!isSaving && overBudgetWarning == null && amount.isEmpty() && selectedCategoryId == null) {
            // This is a bit tricky since we don't have a clear "success" state other than the callback
            // For now, the onConfirmSave and saveTransaction with callback should handle it.
        }
    }
}

private data class SplitDraft(
    val categoryId: Long? = null,
    val amountText: String = ""
)
