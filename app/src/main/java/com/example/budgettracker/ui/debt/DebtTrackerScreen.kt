package com.example.budgettracker.ui.debt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.Debt
import com.example.budgettracker.data.local.entity.DebtType
import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.debtDisplayCentavos
import com.example.budgettracker.domain.debtOwedCentavos
import com.example.budgettracker.domain.interestCentavos
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.localNoon
import com.example.budgettracker.domain.parseAnnualPercent
import com.example.budgettracker.domain.pickerUtcMillis
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
    val lastAccountId by viewModel.lastAccountId.collectAsState()
    val settlementAmounts by viewModel.settlementAmounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<Debt?>(null) }
    var debtToEdit by remember { mutableStateOf<Debt?>(null) }
    var debtToDelete by remember { mutableStateOf<Debt?>(null) }
    var debtToUnpay by remember { mutableStateOf<Debt?>(null) }
    val formOpen = showAdd || debtToEdit != null
    val asOf = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                debtToEdit != null -> R.string.edit_debt
                                showAdd -> R.string.add_debt
                                else -> R.string.debt_tracker
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (formOpen) {
                            showAdd = false
                            debtToEdit = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!formOpen) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_debt))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (formOpen) {
            DebtForm(
                accounts = accounts,
                lastAccountId = lastAccountId,
                debtToEdit = debtToEdit,
                modifier = modifier.padding(innerPadding),
                onSave = { name, amount, type, note, accountId, dueDate, interestRate, startDate ->
                    val editing = debtToEdit
                    if (editing != null) {
                        viewModel.updateDebt(editing, name, amount, type, note, accountId, dueDate, interestRate, startDate)
                    } else {
                        viewModel.addDebt(name, amount, type, note, accountId, dueDate, interestRate, startDate)
                    }
                    showAdd = false
                    debtToEdit = null
                }
            )
        } else {
            DebtList(
                debts = debts,
                settlementAmounts = settlementAmounts,
                asOf = asOf,
                modifier = modifier.padding(innerPadding),
                onSettle = { debtToPay = it },
                onUnpay = { debtToUnpay = it },
                onEdit = { debtToEdit = it },
                onDelete = { debtToDelete = it }
            )
        }
    }

    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(stringResource(R.string.delete_debt_title)) },
            text = { Text(stringResource(R.string.delete_debt_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDebt(debt)
                    debtToDelete = null
                }) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    debtToUnpay?.let { debt ->
        val amount = debtDisplayCentavos(
            debt.amount,
            debt.interestRate,
            debt.date,
            asOf,
            settled = true,
            settlementCentavos = debt.settlementTransactionId?.let { settlementAmounts[it] }
        )
        val body = if (debt.type == DebtType.LENT) R.string.mark_unpaid_removed else R.string.mark_unpaid_returned
        AlertDialog(
            onDismissRequest = { debtToUnpay = null },
            title = { Text(stringResource(R.string.mark_unpaid_title)) },
            text = { Text(stringResource(body, CurrencyUtils.formatAmount(amount))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleDebtStatus(debt, null)
                    debtToUnpay = null
                }) { Text(stringResource(R.string.mark_unpaid)) }
            },
            dismissButton = {
                TextButton(onClick = { debtToUnpay = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    debtToPay?.let { debt ->
        PayDebtDialog(
            accounts = accounts,
            debt = debt,
            onDismiss = { debtToPay = null },
            onConfirm = { accountId ->
                viewModel.toggleDebtStatus(debt, accountId)
                debtToPay = null
            }
        )
    }
}

@Composable
private fun DebtList(
    debts: List<Debt>,
    settlementAmounts: Map<Long, Long>,
    asOf: Long,
    onSettle: (Debt) -> Unit,
    onUnpay: (Debt) -> Unit,
    onEdit: (Debt) -> Unit,
    onDelete: (Debt) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val totalOwedToYou = debts.filter { it.type == DebtType.LENT }.sumOf {
            debtOwedCentavos(it.amount, it.interestRate, it.date, asOf, it.isPaid)
        }
        val totalYouOwe = debts.filter { it.type == DebtType.BORROWED }.sumOf {
            debtOwedCentavos(it.amount, it.interestRate, it.date, asOf, it.isPaid)
        }
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
                    Text(
                        stringResource(R.string.owed_to_you),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true
                    )
                    Text(
                        CurrencyUtils.formatAmount(totalOwedToYou),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.you_owe),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true
                    )
                    Text(
                        CurrencyUtils.formatAmount(totalYouOwe),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true
                    )
                }
            }
        }

        if (debts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.no_debts), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(debts, key = { it.id }) { debt ->
                    val settlement = debt.settlementTransactionId?.let { settlementAmounts[it] }
                    DebtRow(
                        debt = debt,
                        display = debtDisplayCentavos(
                            debt.amount,
                            debt.interestRate,
                            debt.date,
                            asOf,
                            debt.isPaid,
                            settlement
                        ),
                        interest = if (debt.isPaid) 0L else interestCentavos(debt.amount, debt.interestRate, debt.date, asOf),
                        onSettle = { onSettle(debt) },
                        onUnpay = { onUnpay(debt) },
                        onEdit = { onEdit(debt) },
                        onDelete = { onDelete(debt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtRow(
    debt: Debt,
    display: Long,
    interest: Long,
    onSettle: () -> Unit,
    onUnpay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val owesYou = debt.type == DebtType.LENT
    val color = when {
        debt.isPaid -> MaterialTheme.colorScheme.onSurfaceVariant
        owesYou -> EmeraldGreen
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(if (owesYou) R.string.owes_you else R.string.you_owe_name, debt.personName),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (debt.isPaid) TextDecoration.LineThrough else null,
                color = if (debt.isPaid) color else MaterialTheme.colorScheme.onSurface,
                softWrap = true
            )
            if (debt.note.isNotEmpty()) {
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
            Text(
                text = stringResource(R.string.debt_started, formatDebtDate(debt.date)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true
            )
            debt.dueDate?.let { due ->
                Text(
                    text = stringResource(R.string.debt_due_on, formatDebtDate(due)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatAmount(display),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                textDecoration = if (debt.isPaid) TextDecoration.LineThrough else null,
                softWrap = true
            )
            if (!debt.isPaid && interest > 0L) {
                Text(
                    text = stringResource(
                        R.string.principal_plus_interest,
                        CurrencyUtils.formatAmount(debt.amount),
                        CurrencyUtils.formatAmount(interest)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
            if (debt.isPaid) {
                Text(
                    text = stringResource(R.string.settled_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!debt.isPaid) {
                    TextButton(
                        onClick = onSettle,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(if (owesYou) R.string.they_paid_me else R.string.i_paid_them))
                    }
                } else {
                    TextButton(
                        onClick = onUnpay,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.mark_unpaid))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_button), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtForm(
    accounts: List<Account>,
    lastAccountId: Long?,
    debtToEdit: Debt?,
    onSave: (String, Long, DebtType, String, Long, Long?, Double, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = debtToEdit?.isPaid == true
    var personName by remember(debtToEdit?.id) { mutableStateOf(debtToEdit?.personName ?: "") }
    var amountStr by remember(debtToEdit?.id) {
        mutableStateOf(debtToEdit?.amount?.let { Money.toInputString(it) } ?: "")
    }
    var note by remember(debtToEdit?.id) { mutableStateOf(debtToEdit?.note ?: "") }
    var type by remember(debtToEdit?.id) { mutableStateOf(debtToEdit?.type ?: DebtType.LENT) }
    var interestRateStr by remember(debtToEdit?.id) {
        mutableStateOf(debtToEdit?.interestRate?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var startMillis by remember(debtToEdit?.id) { mutableStateOf(debtToEdit?.date ?: System.currentTimeMillis()) }
    var dueMillis by remember(debtToEdit?.id) { mutableStateOf(debtToEdit?.dueDate) }
    var selectedAccount by remember(debtToEdit?.id) { mutableStateOf<Account?>(null) }
    var accountMenu by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts, lastAccountId, debtToEdit?.id) {
        if (selectedAccount != null) return@LaunchedEffect
        selectedAccount = if (debtToEdit != null) {
            accounts.find { it.id == debtToEdit.accountId } ?: accounts.firstOrNull()
        } else {
            accounts.find { it.id == lastAccountId } ?: accounts.firstOrNull()
        }
    }

    val amount = Money.parsePesos(amountStr)
    val interest = parseAnnualPercent(interestRateStr)
    val hint = when {
        personName.isBlank() -> stringResource(R.string.person_required)
        !locked && (amount == null || amount <= 0L) -> stringResource(R.string.amount_required)
        !locked && interest == null -> stringResource(R.string.interest_invalid)
        !locked && selectedAccount == null -> stringResource(R.string.choose_wallet)
        else -> null
    }
    val canSave = hint == null && (locked || (amount != null && amount > 0L && selectedAccount != null && interest != null))

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (locked) {
                Text(
                    stringResource(R.string.settled_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
            FilterChip(
                selected = type == DebtType.LENT,
                onClick = { if (!locked) type = DebtType.LENT },
                enabled = !locked,
                label = { Text(stringResource(R.string.they_owe_me), softWrap = true) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
            FilterChip(
                selected = type == DebtType.BORROWED,
                onClick = { if (!locked) type = DebtType.BORROWED },
                enabled = !locked,
                label = { Text(stringResource(R.string.i_owe_them), softWrap = true) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
            if (!locked) {
                Text(
                    debtEffect(type, amount, selectedAccount?.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(stringResource(R.string.person_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amountStr,
                onValueChange = { if (!locked) amountStr = it },
                readOnly = locked,
                label = { Text(stringResource(R.string.amount_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            WalletField(
                accounts = accounts,
                selected = selectedAccount,
                expanded = accountMenu && !locked,
                locked = locked,
                onExpanded = { accountMenu = it },
                onSelect = {
                    selectedAccount = it
                    accountMenu = false
                }
            )
            OutlinedTextField(
                value = interestRateStr,
                onValueChange = { if (!locked) interestRateStr = it },
                readOnly = locked,
                label = { Text(stringResource(R.string.yearly_interest)) },
                supportingText = { Text(stringResource(R.string.yearly_interest_help)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!locked && amount != null && amount > 0L && interest != null && interest > 0.0) {
                val extra = interestCentavos(amount, interest, startMillis, System.currentTimeMillis())
                Text(
                    stringResource(
                        R.string.interest_so_far,
                        CurrencyUtils.formatAmount(extra),
                        CurrencyUtils.formatAmount(amount + extra)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = true
                )
            }
            DateField(
                label = stringResource(R.string.started_label),
                value = formatDebtDate(startMillis),
                enabled = !locked,
                onOpen = { showStartPicker = true }
            )
            DateField(
                label = stringResource(R.string.due_date),
                value = dueMillis?.let { formatDebtDate(it) } ?: stringResource(R.string.no_due_date),
                enabled = true,
                onOpen = { showDuePicker = true }
            )
            if (dueMillis != null) {
                TextButton(onClick = { dueMillis = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.clear_due_date))
                }
            }
            Text(
                stringResource(R.string.debt_reminder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                softWrap = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Button(
            onClick = {
                val editing = debtToEdit
                val savedAmount = if (locked && editing != null) editing.amount else amount ?: return@Button
                val savedInterest = if (locked && editing != null) editing.interestRate else interest ?: return@Button
                val savedType = if (locked && editing != null) editing.type else type
                val savedStart = if (locked && editing != null) editing.date else startMillis
                val savedAccount = if (locked && editing != null) {
                    editing.accountId ?: selectedAccount?.id ?: 0L
                } else {
                    selectedAccount?.id ?: return@Button
                }
                onSave(personName, savedAmount, savedType, note, savedAccount, dueMillis, savedInterest, savedStart)
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = pickerUtcMillis(localDateOf(startMillis)))
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startMillis = localNoon(localDateFromPickerUtc(it)) }
                    showStartPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        ) { DatePicker(state = state) }
    }
    if (showDuePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueMillis?.let { pickerUtcMillis(localDateOf(it)) }
        )
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueMillis = localNoon(localDateFromPickerUtc(it)) }
                    showDuePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDuePicker = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun debtEffect(type: DebtType, amount: Long?, walletName: String?): String {
    val amountText = amount?.let { CurrencyUtils.formatAmount(it) }
    return when {
        type == DebtType.LENT && amountText != null && walletName != null ->
            stringResource(R.string.debt_effect_out, amountText, walletName)
        type == DebtType.BORROWED && amountText != null && walletName != null ->
            stringResource(R.string.debt_effect_in, amountText, walletName)
        type == DebtType.LENT && walletName != null ->
            stringResource(R.string.debt_effect_out_plain, walletName)
        type == DebtType.BORROWED && walletName != null ->
            stringResource(R.string.debt_effect_in_plain, walletName)
        type == DebtType.LENT -> stringResource(R.string.debt_effect_out_unnamed)
        else -> stringResource(R.string.debt_effect_in_unnamed)
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    enabled: Boolean,
    onOpen: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        trailingIcon = {
            if (enabled) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Filled.DateRange, contentDescription = label)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onOpen) else Modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletField(
    accounts: List<Account>,
    selected: Account?,
    expanded: Boolean,
    locked: Boolean,
    onExpanded: (Boolean) -> Unit,
    onSelect: (Account) -> Unit
) {
    if (locked) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.select_wallet),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.wallet)) },
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpanded
    ) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.select_wallet),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.wallet)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = { onSelect(account) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayDebtDialog(
    accounts: List<Account>,
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedAccount by remember(debt.id, accounts) {
        mutableStateOf(accounts.find { it.id == debt.accountId } ?: accounts.firstOrNull())
    }
    var expanded by remember { mutableStateOf(false) }
    val owesYou = debt.type == DebtType.LENT
    val interest = interestCentavos(debt.amount, debt.interestRate, debt.date, System.currentTimeMillis())
    val total = debt.amount + interest
    val walletName = selectedAccount?.name.orEmpty()
    val body = when {
        owesYou && interest > 0L -> stringResource(
            R.string.settle_in,
            CurrencyUtils.formatAmount(total),
            walletName,
            CurrencyUtils.formatAmount(debt.amount),
            CurrencyUtils.formatAmount(interest)
        )
        !owesYou && interest > 0L -> stringResource(
            R.string.settle_out,
            CurrencyUtils.formatAmount(total),
            walletName,
            CurrencyUtils.formatAmount(debt.amount),
            CurrencyUtils.formatAmount(interest)
        )
        owesYou -> stringResource(R.string.settle_in_plain, CurrencyUtils.formatAmount(total), walletName)
        else -> stringResource(R.string.settle_out_plain, CurrencyUtils.formatAmount(total), walletName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (owesYou) R.string.they_paid_me else R.string.i_paid_them)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body, softWrap = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: stringResource(R.string.select_wallet),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.wallet)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedAccount?.let { onConfirm(it.id) } },
                enabled = selectedAccount != null,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(if (owesYou) R.string.they_paid_me else R.string.i_paid_them))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

private fun formatDebtDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
}
