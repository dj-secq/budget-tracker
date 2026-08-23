package com.example.budgettracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.theme.CatSoftBlue
import com.example.budgettracker.ui.theme.CatAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagementScreen(
    viewModel: WalletManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val haptic = LocalHapticFeedback.current

    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var includeInTotalBalance by remember { mutableStateOf(true) }
    
    var walletToDelete by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(null) }
    
    var walletToEdit by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIncludeInTotal by remember { mutableStateOf(true) }
    
    val colors = listOf(EmeraldGreen, CatSoftBlue, CatAmber)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Wallets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Text("Add New Wallet", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Wallet Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = balance,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) balance = it },
                    label = { Text("Starting Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Color")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    colors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorIndex = index }
                                .padding(4.dp)
                        ) {
                            if (selectedColorIndex == index) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include in Total Balance")
                    Switch(
                        checked = includeInTotalBalance,
                        onCheckedChange = { includeInTotalBalance = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val parsedBalance = balance.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addWallet(name, parsedBalance, selectedColorIndex, includeInTotalBalance)
                            name = ""
                            balance = ""
                            includeInTotalBalance = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("Add Wallet")
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Existing Wallets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            
            items(accounts) { account ->
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
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(account.colorArgb))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(account.name, fontWeight = FontWeight.Bold)
                                Text(com.example.budgettracker.ui.utils.CurrencyUtils.formatAmount(account.balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!account.includeInTotalBalance) {
                                    Text("Excluded from Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                walletToEdit = account
                                editName = account.name
                                editIncludeInTotal = account.includeInTotalBalance
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { walletToDelete = account }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    walletToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete the wallet \"${account.name}\"? This will NOT delete the transactions associated with it, but the balance will no longer be tracked.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteWallet(account)
                        walletToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Name Dialog
    walletToEdit?.let { account ->
        AlertDialog(
            onDismissRequest = { walletToEdit = null },
            title = { Text("Edit Wallet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Wallet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include in Total Balance")
                        Switch(
                            checked = editIncludeInTotal,
                            onCheckedChange = { editIncludeInTotal = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateWallet(account, editName, editIncludeInTotal)
                        walletToEdit = null
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
