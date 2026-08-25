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
    var editGoalEmoji by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalAmount by remember { mutableStateOf("") }
    var newGoalFrequency by remember { mutableStateOf("") }
    var newGoalContribution by remember { mutableStateOf("") }
    var newGoalEmoji by remember { mutableStateOf("🎯") }

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
                            onFundClick = { 
                                goalToFund = goal 
                                fundAmount = goal.contributionAmount?.let { if (it > 0) it.toString() else "" } ?: ""
                            },
                            onEditClick = {
                                goalToEdit = goal
                                editGoalName = goal.name
                                editGoalAmount = goal.targetAmount.toString()
                                editGoalFrequency = goal.contributionFrequency ?: ""
                                editGoalContribution = goal.contributionAmount?.toString() ?: ""
                                editGoalEmoji = goal.iconName ?: "🎯"
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newGoalEmoji,
                                onValueChange = { newGoalEmoji = it.take(2) },
                                label = { Text("Icon") },
                                modifier = Modifier.weight(0.25f)
                            )
                            OutlinedTextField(
                                value = newGoalName,
                                onValueChange = { newGoalName = it },
                                label = { Text("Goal Name") },
                                modifier = Modifier.weight(0.75f)
                            )
                        }
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
                                    contributionAmount = contrib,
                                    iconName = newGoalEmoji.takeIf { it.isNotBlank() }
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editGoalEmoji,
                                onValueChange = { editGoalEmoji = it.take(2) },
                                label = { Text("Icon") },
                                modifier = Modifier.weight(0.25f)
                            )
                            OutlinedTextField(
                                value = editGoalName,
                                onValueChange = { editGoalName = it },
                                label = { Text("Goal Name") },
                                modifier = Modifier.weight(0.75f)
                            )
                        }
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
                                    contributionAmount = contrib,
                                    iconName = editGoalEmoji.takeIf { it.isNotBlank() }
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
    val isComplete = progress >= 1f
    
    val needsAction = !isComplete && (goal.contributionAmount ?: 0.0) > 0.0 && !goal.contributionFrequency.isNullOrBlank()
    
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
                            modifier = Modifier.fillMaxSize(),
                            color = if (isComplete) Color(0xFFFFD700) else EmeraldGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = goal.iconName ?: "🎯",
                            fontSize = 24.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = goal.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (needsAction) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Due: ${CurrencyUtils.formatAmount(goal.contributionAmount!!)} / ${goal.contributionFrequency}",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isComplete) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Goal Reached! 🎉",
                                color = Color(0xFFD4AF37),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (goal.contributionAmount != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${CurrencyUtils.formatAmount(goal.contributionAmount)} / ${goal.contributionFrequency}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
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
                        text = CurrencyUtils.formatAmount(goal.currentAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isComplete) Color(0xFFD4AF37) else EmeraldGreen
                    )
                    Text(
                        text = "of ${CurrencyUtils.formatAmount(goal.targetAmount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isComplete) {
                    Button(
                        onClick = onFundClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Fund Goal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
