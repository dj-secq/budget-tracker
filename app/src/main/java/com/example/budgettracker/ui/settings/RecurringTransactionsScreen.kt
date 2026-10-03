package com.example.budgettracker.ui.settings

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Frequency
import com.example.budgettracker.data.local.entity.RecurringTransaction
import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.endOfLocalDay
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.localNoon
import com.example.budgettracker.ui.theme.CategoryColors
import com.example.budgettracker.ui.utils.CurrencyUtils
import com.example.budgettracker.ui.utils.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ruleDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatDay(millis: Long): String = localDateOf(millis).format(ruleDate)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: RecurringTransactionsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.recurringTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringTransaction?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_cd)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_recurring))
            }
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.no_recurring),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.recurringTransaction.id }) { item ->
                    RecurringCard(
                        item = item,
                        onEdit = {
                            editing = item.recurringTransaction
                            showEditor = true
                        },
                        onDelete = { pendingDelete = item.recurringTransaction },
                        onPauseChange = { paused -> viewModel.setPaused(item.recurringTransaction, paused) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        RecurringEditorDialog(
            rule = editing,
            accounts = accounts,
            categories = categories,
            onDismiss = { showEditor = false },
            onSave = { accountId, categoryId, amount, note, frequency, start, endDate ->
                val current = editing
                if (current == null) {
                    viewModel.addRule(accountId, categoryId, amount, note, frequency, start, endDate)
                } else {
                    viewModel.updateRule(current, accountId, categoryId, amount, note, frequency, endDate)
                }
                showEditor = false
            }
        )
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_recurring_title)) },
            text = { Text(stringResource(R.string.delete_recurring_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecurringTransaction(rule)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }
}

@Composable
private fun RecurringCard(
    item: RecurringTransactionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPauseChange: (Boolean) -> Unit
) {
    val rule = item.recurringTransaction
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val categoryName = item.category?.name ?: stringResource(R.string.unknown)
                val color = item.category?.let { CategoryColors[(it.id % CategoryColors.size).toInt()] }
                    ?: MaterialTheme.colorScheme.surfaceVariant
                Surface(color = color, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = categoryName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = rule.frequency.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (rule.paused) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(stringResource(R.string.paused), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatAmount(rule.amount),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            if (rule.note.isNotBlank()) {
                Text(rule.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.account?.let {
                Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (rule.paused) stringResource(R.string.paused) else stringResource(R.string.next_run, formatDay(rule.nextRunTime)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            rule.endDate?.let {
                Text(
                    text = stringResource(R.string.ends_on, formatDay(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pause), modifier = Modifier.weight(1f))
                Switch(checked = rule.paused, onCheckedChange = onPauseChange)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_button), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorDialog(
    rule: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (accountId: Long, categoryId: Long, amount: Long, note: String, frequency: Frequency, start: Long, endDate: Long?) -> Unit
) {
    var amountText by remember(rule?.id) {
        mutableStateOf(rule?.let { Money.toInputString(it.amount) } ?: "")
    }
    var note by remember(rule?.id) { mutableStateOf(rule?.note ?: "") }
    var accountId by remember(rule?.id) { mutableStateOf(rule?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember(rule?.id) { mutableStateOf(rule?.categoryId ?: categories.firstOrNull()?.id) }
    var frequency by remember(rule?.id) { mutableStateOf(rule?.frequency ?: Frequency.MONTHLY) }
    var start by remember(rule?.id) { mutableLongStateOf(rule?.startDate ?: localNoon(LocalDate.now())) }
    var endDate by remember(rule?.id) { mutableStateOf(rule?.endDate) }
    var picking by remember { mutableStateOf<String?>(null) }
    val parsed = Money.parsePesos(amountText)
    val canSave = parsed != null && parsed > 0L && accountId != null && categoryId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) stringResource(R.string.add_recurring) else stringResource(R.string.edit_recurring)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amountText = it },
                    label = { Text(stringResource(R.string.amount_label)) },
                    prefix = { Text("₱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_short)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.wallet), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        FilterChip(
                            selected = accountId == account.id,
                            onClick = { accountId = account.id },
                            label = { Text(account.name) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                Text(stringResource(R.string.category_label), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) }
                        )
                    }
                }
                Text(stringResource(R.string.repeats), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Frequency.entries) { item ->
                        FilterChip(
                            selected = frequency == item,
                            onClick = { frequency = item },
                            label = { Text(item.label()) }
                        )
                    }
                }
                if (rule == null) {
                    TextButton(onClick = { picking = "start" }) {
                        Text(stringResource(R.string.starts_on, formatDay(start)))
                    }
                }
                TextButton(onClick = { picking = "end" }) {
                    Text(endDate?.let { stringResource(R.string.ends_on, formatDay(it)) } ?: stringResource(R.string.no_end_date))
                }
                if (endDate != null) {
                    TextButton(onClick = { endDate = null }) { Text(stringResource(R.string.clear_end_date)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val amount = parsed ?: return@TextButton
                    val account = accountId ?: return@TextButton
                    val category = categoryId ?: return@TextButton
                    onSave(account, category, amount, note, frequency, start, endDate)
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )

    if (picking != null) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected != null) {
                        val day = localDateFromPickerUtc(selected)
                        if (picking == "start") {
                            val noon = localNoon(day)
                            start = if (day == LocalDate.now() && noon > System.currentTimeMillis()) {
                                System.currentTimeMillis()
                            } else {
                                noon
                            }
                        } else {
                            endDate = endOfLocalDay(day)
                        }
                    }
                    picking = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
