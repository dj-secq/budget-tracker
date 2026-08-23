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
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.SavingsGoal
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    var goalToFund by remember { mutableStateOf<SavingsGoal?>(null) }
    var fundAmount by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var editGoalName by remember { mutableStateOf("") }
    var editGoalAmount by remember { mutableStateOf("") }
    var editGoalFrequency by remember { mutableStateOf("") } // Weekly, Monthly
    var editGoalContribution by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalAmount by remember { mutableStateOf("") }
    var newGoalFrequency by remember { mutableStateOf("") }
    var newGoalContribution by remember { mutableStateOf("") }

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
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
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
                        "No goals yet. Add one to start saving!", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(goals) { goal ->
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
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    ) {
                        GoalCard(
                            goal = goal,
                            onFundClick = { goalToFund = goal },
                            onEditClick = {
                                goalToEdit = goal
                                editGoalName = goal.name
                                editGoalAmount = goal.targetAmount.toString()
                                editGoalFrequency = goal.contributionFrequency ?: ""
                                editGoalContribution = goal.contributionAmount?.toString() ?: ""
                            }
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
                title = { Text("Add Savings Goal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = newGoalName,
                            onValueChange = { newGoalName = it },
                            label = { Text("Goal Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newGoalAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) newGoalAmount = it },
                            label = { Text("Target Amount") },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newGoalFrequency,
                            onValueChange = { newGoalFrequency = it },
                            label = { Text("Frequency (e.g. Weekly, Monthly)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newGoalContribution,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) newGoalContribution = it },
                            label = { Text("Contribution Amount (Optional)") },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = newGoalAmount.toDoubleOrNull()
                            val contrib = newGoalContribution.toDoubleOrNull()
                            if (newGoalName.isNotBlank() && amount != null && amount > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.addGoal(
                                    name = newGoalName, 
                                    targetAmount = amount, 
                                    frequency = newGoalFrequency.takeIf { it.isNotBlank() },
                                    contributionAmount = contrib
                                )
                                showAddGoalDialog = false
                                newGoalName = ""
                                newGoalAmount = ""
                                newGoalFrequency = ""
                                newGoalContribution = ""
                            }
                        },
                        enabled = newGoalName.isNotBlank() && newGoalAmount.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
                        Text("Cancel")
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
                title = { Text("Fund ${goal.name}") },
                text = {
                    Column {
                        Text("How much would you like to contribute to this goal?")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fundAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) fundAmount = it },
                            label = { Text("Amount") },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("From Account", fontWeight = FontWeight.Bold)
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
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = fundAmount.toDoubleOrNull()
                            if (amount != null && amount > 0 && selectedAccountId != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.fundGoal(goal, amount, selectedAccountId!!)
                                goalToFund = null
                                fundAmount = ""
                                selectedAccountId = null
                            }
                        },
                        enabled = fundAmount.isNotEmpty() && selectedAccountId != null
                    ) {
                        Text("Fund", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        goalToFund = null
                        fundAmount = ""
                        selectedAccountId = null
                    }) {
                        Text("Cancel")
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
                title = { Text("Edit Savings Goal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = editGoalName,
                            onValueChange = { editGoalName = it },
                            label = { Text("Goal Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGoalAmount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editGoalAmount = it },
                            label = { Text("Target Amount") },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGoalFrequency,
                            onValueChange = { editGoalFrequency = it },
                            label = { Text("Frequency (e.g. Weekly, Monthly)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGoalContribution,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editGoalContribution = it },
                            label = { Text("Contribution Amount (Optional)") },
                            prefix = { Text("₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = editGoalAmount.toDoubleOrNull()
                            val contrib = editGoalContribution.toDoubleOrNull()
                            if (editGoalName.isNotBlank() && amount != null && amount > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateGoal(
                                    goal = goal,
                                    name = editGoalName,
                                    targetAmount = amount,
                                    targetDate = goal.targetDate,
                                    frequency = editGoalFrequency.takeIf { it.isNotBlank() },
                                    contributionAmount = contrib
                                )
                                goalToEdit = null
                            }
                        },
                        enabled = editGoalName.isNotBlank() && editGoalAmount.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { goalToEdit = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: SavingsGoal,
    onFundClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = onFundClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Fund", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyUtils.formatAmount(goal.currentAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = EmeraldGreen
                )
                Text(
                    text = "Goal: ${CurrencyUtils.formatAmount(goal.targetAmount)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (goal.contributionAmount != null && !goal.contributionFrequency.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reminder: ${CurrencyUtils.formatAmount(goal.contributionAmount)} / ${goal.contributionFrequency}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .background(EmeraldGreen)
                )
            }
        }
    }
}
