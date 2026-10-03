package com.example.budgettracker.ui.settings

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.domain.Money
import com.example.budgettracker.ui.theme.WalletPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagementScreen(
    viewModel: WalletManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val deleteBlocked by viewModel.deleteBlocked.collectAsState()
    val haptic = LocalHapticFeedback.current

    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var includeInTotalBalance by remember { mutableStateOf(true) }
    
    var walletToDelete by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(null) }
    
    var walletToEdit by remember { mutableStateOf<com.example.budgettracker.data.local.entity.Account?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIncludeInTotal by remember { mutableStateOf(true) }
    
    val colors = WalletPalette.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_wallets_title)) },
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
                Text(stringResource(R.string.add_wallet), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.wallet_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = balance,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) balance = it },
                    label = { Text(stringResource(R.string.starting_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.color))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(colors.size) { index ->
                        val color = colors[index]
                        val colorLabel = stringResource(R.string.color_cd, index + 1)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = colorLabel }
                                .clickable { selectedColorIndex = index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
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
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.include_in_total))
                    Switch(
                        checked = includeInTotalBalance,
                        onCheckedChange = { includeInTotalBalance = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val parsedBalance = if (balance.isBlank()) 0L else Money.parsePesos(balance)
                        if (name.isNotBlank() && parsedBalance != null) {
                            viewModel.addWallet(name, parsedBalance, selectedColorIndex, includeInTotalBalance)
                            name = ""
                            balance = ""
                            includeInTotalBalance = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.add_wallet_button))
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(stringResource(R.string.existing_wallets), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
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
                                    Text(stringResource(R.string.excluded_from_total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                walletToEdit = account
                                editName = account.name
                                editIncludeInTotal = account.includeInTotalBalance
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { walletToDelete = account }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_button), tint = MaterialTheme.colorScheme.error)
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
            title = { Text(stringResource(R.string.confirm_deletion)) },
            text = { Text(stringResource(R.string.delete_wallet_message, account.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteWallet(account)
                        walletToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (deleteBlocked) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteBlocked() },
            title = { Text(stringResource(R.string.cant_delete_wallet)) },
            text = { Text(stringResource(R.string.cant_delete_wallet_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDeleteBlocked() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Edit Name Dialog
    walletToEdit?.let { account ->
        AlertDialog(
            onDismissRequest = { walletToEdit = null },
            title = { Text(stringResource(R.string.edit_wallet)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.wallet_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.include_in_total))
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
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToEdit = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}
