package com.example.budgettracker.ui.assign

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.domain.Money
import com.example.budgettracker.ui.components.MonthPicker

import com.example.budgettracker.ui.utils.CategoryIconHelper
import com.example.budgettracker.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignBudgetScreen(
    viewModel: AssignBudgetViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var categoryToEdit by remember { mutableStateOf<Long?>(null) }
    var editAmount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assign_budget)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header — Month picker only
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MonthPicker(
                    currentMonth = uiState.currentMonth,
                    currentYear = uiState.currentYear,
                    onMonthChanged = { m, y -> viewModel.setMonth(m, y) }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.budgetItems) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                categoryToEdit = item.category.id
                                editAmount = if (item.base > 0L) Money.toInputString(item.base) else ""
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIconForCategory(item.category.name),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(item.category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, softWrap = true)
                                    Text(stringResource(R.string.spent_amount, CurrencyUtils.formatAmount(item.spent)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                                    if (item.suggestedBase != null) {
                                        Text(
                                            stringResource(R.string.suggested_base, CurrencyUtils.formatAmount(item.suggestedBase)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            softWrap = true
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.base_amount, CurrencyUtils.formatAmount(item.base)), fontWeight = FontWeight.Bold)
                                if (uiState.rolloverEnabled) {
                                    Text(
                                        stringResource(R.string.rollover_amount, CurrencyUtils.formatAmount(item.rollover)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(R.string.effective_amount, CurrencyUtils.formatAmount(item.effective)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    categoryToEdit?.let { categoryId ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(stringResource(R.string.assign_budget)) },
            text = {
                val suggestion = uiState.budgetItems.find { it.category.id == categoryId }?.suggestedBase
                Column {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editAmount = it },
                        label = { Text(stringResource(R.string.amount_label)) },
                        prefix = { Text("₱") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (suggestion != null) {
                        TextButton(onClick = { editAmount = Money.toInputString(suggestion) }) {
                            Text(stringResource(R.string.use_suggested, CurrencyUtils.formatAmount(suggestion)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = if (editAmount.isBlank()) 0L else Money.parsePesos(editAmount)
                    if (amount != null) {
                        viewModel.updateBudgetLimit(categoryId, amount)
                        categoryToEdit = null
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}
