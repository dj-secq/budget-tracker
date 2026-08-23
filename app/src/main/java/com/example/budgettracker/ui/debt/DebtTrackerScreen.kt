package com.example.budgettracker.ui.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtTrackerScreen(
    viewModel: DebtTrackerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Debt?>(null) }
    var debtToEdit by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Debt?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Debt")
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            val totalOwedToYou = debts.filter { it.type == DebtType.LENT && !it.isPaid }.sumOf { it.amount }
            val totalYouOwe = debts.filter { it.type == DebtType.BORROWED && !it.isPaid }.sumOf { it.amount }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Owed to you", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                        Text(CurrencyUtils.formatAmount(totalOwedToYou), fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("You owe", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                        Text(CurrencyUtils.formatAmount(totalYouOwe), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                    }
                }
            }

            if (debts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No debts tracked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(debts) { debt ->
                        DebtItem(
                            debt = debt,
                            onToggleStatus = {
                                if (!debt.isPaid) {
                                    debtToPay = debt
                                } else {
                                    viewModel.toggleDebtStatus(debt, null)
                                }
                            },
                            onEdit = { debtToEdit = debt },
                            onDelete = { viewModel.deleteDebt(debt) }
                        )
                    }
                }
            }
        }
        
        if (showAddDialog || debtToEdit != null) {
            DebtFormDialog(
                accounts = accounts,
                debtToEdit = debtToEdit,
                onDismiss = { 
                    showAddDialog = false
                    debtToEdit = null
                },
                onSave = { name, amount, type, note, accountId, dueDate, interestRate, startDate ->
                    if (debtToEdit != null) {
                        viewModel.updateDebt(debtToEdit!!, name, amount, type, note, accountId, dueDate, interestRate, startDate)
                    } else {
                        viewModel.addDebt(name, amount, type, note, accountId, dueDate, interestRate, startDate)
                    }
                    showAddDialog = false
                    debtToEdit = null
                }
            )
        }
        
        if (debtToPay != null) {
            PayDebtDialog(
                accounts = accounts,
                debt = debtToPay!!,
                onDismiss = { debtToPay = null },
                onConfirm = { accountId ->
                    viewModel.toggleDebtStatus(debtToPay!!, accountId)
                    debtToPay = null
                }
            )
        }
    }
}

@Composable
fun DebtItem(debt: Debt, onToggleStatus: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleStatus)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val actionText = if (debt.type == DebtType.LENT) "Lent to" else "Borrowed from"
                Text(
                    text = "$actionText ${debt.personName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textDecoration = if (debt.isPaid) TextDecoration.LineThrough else null,
                    color = if (debt.isPaid) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (debt.note.isNotEmpty()) {
                    Text(text = debt.note, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(debt.date))
                val dueStr = debt.dueDate?.let { " • Due: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))}" } ?: ""
                Text(
                    text = "$dateStr$dueStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val color = if (debt.isPaid) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else if (debt.type == DebtType.LENT) EmeraldGreen else MaterialTheme.colorScheme.error
                            
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - debt.date).coerceAtLeast(0)
                val interest = debt.amount * (debt.interestRate / 100.0) * (days / 365.0)
                val totalAmount = debt.amount + interest
                Text(
                    text = CurrencyUtils.formatAmount(totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = color,
                    textDecoration = if (debt.isPaid) TextDecoration.LineThrough else null
                )
                if (debt.interestRate > 0) {
                    Text(
                        text = "Incl. ${debt.interestRate}% interest",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormDialog(
    accounts: List<com.example.budgettracker.data.local.entity.Account>,
    debtToEdit: com.example.budgettracker.data.local.entity.Debt? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, DebtType, String, Long, Long?, Double, Long) -> Unit
) {
    var personName by remember { mutableStateOf(debtToEdit?.personName ?: "") }
    var amountStr by remember { mutableStateOf(debtToEdit?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(debtToEdit?.note ?: "") }
    var type by remember { mutableStateOf(debtToEdit?.type ?: DebtType.LENT) }
    var interestRateStr by remember { mutableStateOf(debtToEdit?.interestRate?.takeIf { it > 0 }?.toString() ?: "") }
    
    var selectedAccount by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(
        accounts.find { it.id == debtToEdit?.accountId } ?: accounts.firstOrNull()
    ) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = debtToEdit?.date ?: System.currentTimeMillis())
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = debtToEdit?.dueDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debtToEdit != null) "Edit Debt" else "Add Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = { interestRateStr = it },
                        label = { Text("Interest %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDatePickerState.selectedDateMillis?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Today",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        modifier = Modifier.weight(1f).clickable { showStartDatePicker = true },
                        trailingIcon = { 
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Select Start Date")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = datePickerState.selectedDateMillis?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "No Due Date",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due Date") },
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        trailingIcon = { 
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Select Due Date")
                            }
                        }
                    )
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == DebtType.LENT, onClick = { type = DebtType.LENT })
                        Text("I Lent")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == DebtType.BORROWED, onClick = { type = DebtType.BORROWED })
                        Text("I Borrowed")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    val interest = interestRateStr.toDoubleOrNull() ?: 0.0
                    val startMillis = startDatePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    if (personName.isNotBlank() && amount != null && amount > 0 && selectedAccount != null) {
                        onSave(personName, amount, type, note, selectedAccount!!.id, datePickerState.selectedDateMillis, interest, startMillis)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDebtDialog(
    accounts: List<com.example.budgettracker.data.local.entity.Account>,
    debt: com.example.budgettracker.data.local.entity.Debt,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(accounts.firstOrNull()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    
    val actionText = if (debt.type == com.example.budgettracker.data.local.entity.DebtType.LENT) "received from" else "paid to"
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - debt.date).coerceAtLeast(0)
    val interest = debt.amount * (debt.interestRate / 100.0) * (days / 365.0)
    val totalAmount = debt.amount + interest

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Paid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select the wallet where the money was $actionText ${debt.personName}:")
                Text("Total to settle: ${com.example.budgettracker.ui.utils.CurrencyUtils.formatAmount(totalAmount)}", fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedAccount != null) {
                        onConfirm(selectedAccount!!.id)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
