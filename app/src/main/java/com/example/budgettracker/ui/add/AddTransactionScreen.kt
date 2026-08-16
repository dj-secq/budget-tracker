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
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.ui.theme.CategoryColors
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
    
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    
    val accounts by viewModel.accounts.collectAsState()
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    
    val templates by viewModel.templates.collectAsState()
    var showTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    
    val overBudgetWarning by viewModel.showOverBudgetWarning.collectAsState()
    val bucketWarning by viewModel.showBucketWarning.collectAsState()
    
    val filteredCategories = categories.filter { it.type == transactionType }

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
                            amount = result.amount.toString()
                        }
                        if (result.note.isNotBlank()) {
                            note = result.note
                        }
                        Toast.makeText(context, "Scanned successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to scan receipt", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quick Add Templates
            if (templates.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Quick Add Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates) { template ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    amount = template.amount.toString()
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
                trailingIcon = {
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Scan Receipt", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold)
            )

            // Transaction Type Toggle
            TabRow(
                selectedTabIndex = if (isTransfer) 2 else if (transactionType == CategoryType.EXPENSE) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !isTransfer && transactionType == CategoryType.EXPENSE,
                    onClick = { isTransfer = false; transactionType = CategoryType.EXPENSE; selectedCategoryId = null },
                    text = { Text("Expense", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = !isTransfer && transactionType == CategoryType.INCOME,
                    onClick = { isTransfer = false; transactionType = CategoryType.INCOME; selectedCategoryId = null },
                    text = { Text("Income", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = isTransfer,
                    onClick = { isTransfer = true; selectedCategoryId = null },
                    text = { Text("Transfer", fontWeight = FontWeight.Bold) }
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
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedAccountId = account.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text("From Account", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        val isSelected = selectedAccountId == account.id
                        val color = Color(account.colorArgb)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedAccountId = account.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text("To Account", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        val isSelected = transferToAccountId == account.id
                        val color = Color(account.colorArgb)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { transferToAccountId = account.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Selection
            if (!isTransfer) {
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
                                    .aspectRatio(1f)
                                    .clickable { selectedCategoryId = category.id }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
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
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

            // Classification Selection (Only for Expenses)
            if (!isTransfer && transactionType == CategoryType.EXPENSE) {
                Text("Classification", fontWeight = FontWeight.Bold)
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
                            label = { Text(clazz.name) }
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
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDateMillis = it
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
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
            if (!isTransfer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.repeat_transaction), fontWeight = FontWeight.Bold)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            
            if (isRecurring) {
                Text("Frequency", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Frequency.values()) { freq ->
                        FilterChip(
                            selected = recurringFrequency == freq,
                            onClick = { recurringFrequency = freq },
                            label = { Text(freq.name) }
                        )
                    }
                }
            }
            } // Close if (!isTransfer)

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isTransfer && amount.isNotBlank() && selectedAccountId != null && selectedCategoryId != null
                ) {
                    Text("Save Template")
                }

                // Save Button
                Button(
                    onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (isTransfer) {
                        if (parsedAmount != null && selectedAccountId != null && transferToAccountId != null && selectedAccountId != transferToAccountId) {
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
                            Toast.makeText(context, "Cannot transfer to the same account", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (parsedAmount != null && selectedCategoryId != null && selectedAccountId != null) {
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
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = if (isTransfer) {
                    amount.isNotEmpty() && selectedAccountId != null && transferToAccountId != null && selectedAccountId != transferToAccountId && !isSaving
                } else {
                    amount.isNotEmpty() && selectedCategoryId != null && selectedAccountId != null && !isSaving
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save_transaction), fontSize = 18.sp, color = Color.Black)
            }
            } // Close Row
        }
    }

    // Bucket warning dialog (Strict)
    bucketWarning?.let { (bucketName, excess) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarnings() },
            title = { Text("$bucketName Cap Reached") },
            text = { Text("This transaction exceeds your $bucketName allocation by ₱${String.format(Locale.getDefault(), "%.2f", excess)}.\n\nStrict limits are enforced. Please adjust the amount or increase your income first.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarnings() }) {
                    Text("OK")
                }
            }
        )
    }

    // Over budget warning dialog
    overBudgetWarning?.let { (excess, isStrict) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarnings() },
            title = { Text("Over Budget") },
            text = { 
                if (isStrict) {
                    Text("This transaction exceeds your budget for this category by ₱${String.format(Locale.getDefault(), "%.2f", excess)}.\n\nStrict budget limits are enforced. You cannot save this transaction.")
                } else {
                    Text("This transaction exceeds your budget for this category by ₱${String.format(Locale.getDefault(), "%.2f", excess)}.\n\nDo you want to save it anyway?")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarnings() }) {
                    Text("OK")
                }
            },
            dismissButton = if (!isStrict) {
                {
                    TextButton(onClick = {
                        viewModel.dismissWarnings()
                        val parsedAmount = amount.toDoubleOrNull()
                        if (parsedAmount != null && selectedCategoryId != null && selectedAccountId != null) {
                            viewModel.saveTransaction(
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
                        }
                    }) {
                        Text("Save Anyway")
                    }
                }
            } else null
        )
    }

    // Template Name Dialog
    if (showTemplateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Save as Template") },
            text = {
                OutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text("Template Name (e.g., Morning Coffee)") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
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
                        Toast.makeText(context, "Template Saved!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = newTemplateName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel")
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
