package com.example.budgettracker.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.data.local.entity.countsAsExpense
import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.localDateFromPickerUtc
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.domain.localNoon
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val fundError by viewModel.fundError.collectAsState()
    val haptic = LocalHapticFeedback.current
    val expenseCategories = categories.filter { it.countsAsExpense() }
    
    var goalToFund by remember { mutableStateOf<SavingsGoal?>(null) }
    var fundAmount by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var countAsSpending by remember { mutableStateOf(false) }
    var spendCategoryId by remember { mutableStateOf<Long?>(null) }
    var goalToUnfund by remember { mutableStateOf<SavingsGoal?>(null) }
    
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var editGoalName by remember { mutableStateOf("") }
    var editGoalAmount by remember { mutableStateOf("") }
    var editGoalFrequency by remember { mutableStateOf("") } // Weekly, Monthly
    var editGoalContribution by remember { mutableStateOf("") }
    var editGoalEmoji by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalAmount by remember { mutableStateOf("") }
    var newGoalFrequency by remember { mutableStateOf("") }
    var newGoalContribution by remember { mutableStateOf("") }
    var newGoalEmoji by remember { mutableStateOf("🎯") }
    var newGoalDate by remember { mutableStateOf<Long?>(null) }
    var editGoalDate by remember { mutableStateOf<Long?>(null) }
    var pickingDate by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_goals), fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_goal))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (goals.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_goals), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(goals, key = { it.goal.id }) { progress ->
                    val goal = progress.goal
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                goalToDelete = goal
                                false // Don't dismiss immediately, wait for confirmation
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
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_button), tint = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    ) {
                        GoalCard(
                            goal = goal,
                            savedCentavos = progress.savedCentavos,
                            onFundClick = {
                                goalToFund = goal
                                fundAmount = goal.contributionAmount?.let { if (it > 0L) Money.toInputString(it) else "" } ?: ""
                                countAsSpending = false
                                spendCategoryId = null
                            },
                            onEditClick = {
                                goalToEdit = goal
                                editGoalName = goal.name
                                editGoalAmount = Money.toInputString(goal.targetAmount)
                                editGoalFrequency = goal.contributionFrequency ?: ""
                                editGoalContribution = goal.contributionAmount?.let { Money.toInputString(it) } ?: ""
                                editGoalEmoji = goal.iconName ?: "🎯"
                                editGoalDate = goal.targetDate
                            },
                            onUnfundClick = { goalToUnfund = goal }
                        )
                    }
                }
            }
        }

        // Add Goal Dialog
        if (showAddGoalDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddGoalDialog = false
                    newGoalName = ""
                    newGoalAmount = ""
                    newGoalFrequency = ""
                    newGoalContribution = ""
                },
                title = { Text(stringResource(R.string.add_savings_goal)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newGoalEmoji,
                                onValueChange = { newGoalEmoji = it.take(2) },
                                label = { Text(stringResource(R.string.icon)) },
                                modifier = Modifier.weight(0.25f)
                            )
                            OutlinedTextField(
                                value = newGoalName,
                                onValueChange = { newGoalName = it },
                                label = { Text(stringResource(R.string.goal_name)) },
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                        OutlinedTextField(
                            value = newGoalAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) newGoalAmount = it },
                            label = { Text(stringResource(R.string.target_amount)) },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newGoalFrequency,
                            onValueChange = { newGoalFrequency = it },
                            label = { Text(stringResource(R.string.frequency_example)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newGoalContribution,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) newGoalContribution = it },
                            label = { Text(stringResource(R.string.contribution_optional)) },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { pickingDate = "new" }) {
                            Text(newGoalDate?.let { stringResource(R.string.target_on, goalDay(it)) } ?: stringResource(R.string.set_target_date))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = Money.parsePesos(newGoalAmount)
                            val contrib = if (newGoalContribution.isBlank()) null else Money.parsePesos(newGoalContribution)
                            if (newGoalName.isNotBlank() && amount != null && amount > 0L && (newGoalContribution.isBlank() || contrib != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.addGoal(
                                    name = newGoalName,
                                    targetAmount = amount,
                                    targetDate = newGoalDate,
                                    frequency = newGoalFrequency.takeIf { it.isNotBlank() },
                                    contributionAmount = contrib,
                                    iconName = newGoalEmoji.takeIf { it.isNotBlank() }
                                )
                                showAddGoalDialog = false
                                newGoalName = ""
                                newGoalAmount = ""
                                newGoalFrequency = ""
                                newGoalContribution = ""
                                newGoalDate = null
                            }
                        },
                        enabled = newGoalName.isNotBlank() && newGoalAmount.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddGoalDialog = false
                        newGoalName = ""
                        newGoalAmount = ""
                        newGoalFrequency = ""
                        newGoalContribution = ""
                    }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        // Funding Dialog
        goalToFund?.let { goal ->
            AlertDialog(
                onDismissRequest = { 
                    goalToFund = null
                    fundAmount = ""
                    selectedAccountId = null
                },
                title = { Text(stringResource(R.string.fund_title, goal.name)) },
                text = {
                    Column {
                        Text(stringResource(R.string.fund_help))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fundAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) fundAmount = it },
                            label = { Text(stringResource(R.string.amount_label)) },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.from_account), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accounts) { account ->
                                val isSelected = selectedAccountId == account.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAccountId = account.id },
                                    label = { Text(account.name) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.count_as_spending), modifier = Modifier.weight(1f))
                            Switch(checked = countAsSpending, onCheckedChange = { countAsSpending = it })
                        }
                        if (countAsSpending) {
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(expenseCategories) { category ->
                                    FilterChip(
                                        selected = spendCategoryId == category.id,
                                        onClick = { spendCategoryId = category.id },
                                        label = { Text(category.name) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = Money.parsePesos(fundAmount)
                            val spend = if (countAsSpending) spendCategoryId else null
                            if (amount != null && amount > 0L && selectedAccountId != null && (!countAsSpending || spend != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.fundGoal(goal, amount, selectedAccountId!!, spend)
                                goalToFund = null
                                fundAmount = ""
                                selectedAccountId = null
                                countAsSpending = false
                                spendCategoryId = null
                            }
                        },
                        enabled = fundAmount.isNotEmpty() && selectedAccountId != null && (!countAsSpending || spendCategoryId != null)
                    ) {
                        Text(stringResource(R.string.fund), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        goalToFund = null
                        fundAmount = ""
                        selectedAccountId = null
                    }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }
        
        // Delete Confirmation Dialog
        goalToDelete?.let { goal ->
            AlertDialog(
                onDismissRequest = { goalToDelete = null },
                title = { Text(stringResource(R.string.confirm_deletion)) },
                text = { Text(stringResource(R.string.delete_confirmation_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteGoal(goal)
                            goalToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { goalToDelete = null }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        // Edit Goal Dialog
        goalToEdit?.let { goal ->
            AlertDialog(
                onDismissRequest = { 
                    goalToEdit = null
                },
                title = { Text(stringResource(R.string.edit_savings_goal)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editGoalEmoji,
                                onValueChange = { editGoalEmoji = it.take(2) },
                                label = { Text(stringResource(R.string.icon)) },
                                modifier = Modifier.weight(0.25f)
                            )
                            OutlinedTextField(
                                value = editGoalName,
                                onValueChange = { editGoalName = it },
                                label = { Text(stringResource(R.string.goal_name)) },
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                        OutlinedTextField(
                            value = editGoalAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editGoalAmount = it },
                            label = { Text(stringResource(R.string.target_amount)) },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGoalFrequency,
                            onValueChange = { editGoalFrequency = it },
                            label = { Text(stringResource(R.string.frequency_example)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGoalContribution,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editGoalContribution = it },
                            label = { Text(stringResource(R.string.contribution_optional)) },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { pickingDate = "edit" }) {
                            Text(editGoalDate?.let { stringResource(R.string.target_on, goalDay(it)) } ?: stringResource(R.string.set_target_date))
                        }
                        if (editGoalDate != null) {
                            TextButton(onClick = { editGoalDate = null }) { Text(stringResource(R.string.clear_target_date)) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = Money.parsePesos(editGoalAmount)
                            val contrib = if (editGoalContribution.isBlank()) null else Money.parsePesos(editGoalContribution)
                            if (editGoalName.isNotBlank() && amount != null && amount > 0L && (editGoalContribution.isBlank() || contrib != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateGoal(
                                    goal = goal,
                                    name = editGoalName,
                                    targetAmount = amount,
                                    targetDate = editGoalDate,
                                    frequency = editGoalFrequency.takeIf { it.isNotBlank() },
                                    contributionAmount = contrib,
                                    iconName = editGoalEmoji.takeIf { it.isNotBlank() }
                                )
                                goalToEdit = null
                            }
                        },
                        enabled = editGoalName.isNotBlank() && editGoalAmount.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { goalToEdit = null }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        goalToUnfund?.let { goal ->
            AlertDialog(
                onDismissRequest = { goalToUnfund = null },
                title = { Text(stringResource(R.string.remove_contribution_title)) },
                text = { Text(stringResource(R.string.remove_contribution_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unfundLatest(goal)
                        goalToUnfund = null
                    }) { Text(stringResource(R.string.remove)) }
                },
                dismissButton = {
                    TextButton(onClick = { goalToUnfund = null }) { Text(stringResource(R.string.cancel_button)) }
                }
            )
        }

        fundError?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearFundError() },
                title = { Text(stringResource(R.string.fund_failed)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearFundError() }) { Text(stringResource(R.string.ok)) }
                }
            )
        }

        if (pickingDate != null) {
            val pickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { pickingDate = null },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { selected ->
                            val noon = localNoon(localDateFromPickerUtc(selected))
                            if (pickingDate == "edit") editGoalDate = noon else newGoalDate = noon
                        }
                        pickingDate = null
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pickingDate = null }) { Text(stringResource(R.string.cancel_button)) }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

private val goalDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun goalDay(millis: Long): String = localDateOf(millis).format(goalDayFormat)

@Composable
fun GoalCard(
    goal: SavingsGoal,
    savedCentavos: Long,
    onFundClick: () -> Unit,
    onEditClick: () -> Unit,
    onUnfundClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0L) {
        (savedCentavos.toDouble() / goal.targetAmount.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val isComplete = progress >= 1f
    val percentLabel = stringResource(R.string.progress_percent, (progress * 100).toInt())
    
    val needsAction = !isComplete && (goal.contributionAmount ?: 0L) > 0L && !goal.contributionFrequency.isNullOrBlank()
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                                    contentDescription = percentLabel
                                },
                            color = if (isComplete) Color(0xFFFFD700) else EmeraldGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = goal.iconName ?: "🎯",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = goal.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        goal.targetDate?.let { date ->
                            Text(
                                text = stringResource(R.string.by_date, goalDay(date)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (needsAction) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.due_contribution,
                                        CurrencyUtils.formatAmount(goal.contributionAmount!!),
                                        goal.contributionFrequency ?: ""
                                    ),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isComplete) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.goal_reached),
                                color = Color(0xFFD4AF37),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (goal.contributionAmount != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.contribution_plan,
                                    CurrencyUtils.formatAmount(goal.contributionAmount ?: 0L),
                                    goal.contributionFrequency ?: ""
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_goal), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = CurrencyUtils.formatAmount(savedCentavos),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isComplete) Color(0xFFD4AF37) else EmeraldGreen
                    )
                    Text(
                        text = stringResource(R.string.of_target, CurrencyUtils.formatAmount(goal.targetAmount)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedCentavos > 0L) {
                        TextButton(onClick = onUnfundClick) { Text(stringResource(R.string.unfund)) }
                    }
                    if (!isComplete) {
                        Button(
                            onClick = onFundClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(stringResource(R.string.fund_goal), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
