package com.example.budgettracker.ui.settings

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.CategoryType
import com.example.budgettracker.data.local.entity.ExpenseClassification
import com.example.budgettracker.ui.utils.CategoryIconHelper
import com.example.budgettracker.ui.utils.label
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val deleteBlocked by viewModel.deleteBlocked.collectAsState()
    val haptic = LocalHapticFeedback.current

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    
    var categoryToDelete by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Category?>(null) }
    var categoryToRename by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Category?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameIcon by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.add_new_category), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Type Segmented Button
                TabRow(
                    selectedTabIndex = if (selectedType == CategoryType.EXPENSE) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedType == CategoryType.EXPENSE,
                        onClick = { selectedType = CategoryType.EXPENSE },
                        text = { Text(stringResource(R.string.expense)) }
                    )
                    Tab(
                        selected = selectedType == CategoryType.INCOME,
                        onClick = { selectedType = CategoryType.INCOME },
                        text = { Text(stringResource(R.string.income)) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addCategory(name, selectedType)
                            name = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.add_category))
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(stringResource(R.string.existing_categories), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }

            items(categories) { category ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = CategoryIconHelper.getIconForCategory(category.name, category.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(category.name, fontWeight = FontWeight.Bold)
                                Row {
                                    Text(category.type.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row {
                            TextButton(onClick = {
                                categoryToRename = category
                                renameText = category.name
                                renameIcon = category.iconName
                            }) { Text(stringResource(R.string.rename)) }
                            IconButton(onClick = { categoryToDelete = category }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_button), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion)) },
            text = { Text(stringResource(R.string.delete_category_message, category.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    categoryToRename?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text(stringResource(R.string.rename_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true
                    )
                    Text(stringResource(R.string.icon))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CategoryIconHelper.iconChoices()) { key ->
                            FilterChip(
                                selected = renameIcon == key,
                                onClick = { renameIcon = key },
                                label = { Text(key) },
                                leadingIcon = {
                                    Icon(CategoryIconHelper.getIconForCategory(key, key), contentDescription = null)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCategory(category, renameText, renameIcon)
                        categoryToRename = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    if (deleteBlocked) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteBlocked() },
            title = { Text(stringResource(R.string.cant_delete_category)) },
            text = { Text(stringResource(R.string.cant_delete_category_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDeleteBlocked() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
