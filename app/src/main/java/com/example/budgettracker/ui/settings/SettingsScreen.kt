package com.example.budgettracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.budgettracker.R
import com.example.budgettracker.data.repository.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetRule by viewModel.budgetRule.collectAsState()
    val generalPrefs by viewModel.generalPrefs.collectAsState()

    var needs by remember(budgetRule) { mutableFloatStateOf(budgetRule?.needsPercent?.toFloat() ?: 50f) }
    var wants by remember(budgetRule) { mutableFloatStateOf(budgetRule?.wantsPercent?.toFloat() ?: 30f) }
    var savings by remember(budgetRule) { mutableFloatStateOf(budgetRule?.savingsPercent?.toFloat() ?: 20f) }

    val total = (needs + wants + savings).roundToInt()
    val isValid = total == 100

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri) { success ->
                Toast.makeText(context, if (success) "Export successful!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri) { success ->
                Toast.makeText(context, if (success) "Import successful!" else "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.appearance_header),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Theme Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = budgetRule?.themeMode == mode,
                        onClick = { viewModel.updateThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                    ) {
                        Text(mode.name.lowercase().capitalize())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "General Preferences",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Daily Reminders Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Reminders", fontWeight = FontWeight.Medium)
                    Text("Notify me at 8 PM if I haven't logged anything", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = generalPrefs?.dailyRemindersEnabled ?: false,
                    onCheckedChange = { 
                        generalPrefs?.let { prefs ->
                            viewModel.updateGeneralPreferences(it, prefs.rolloverBudgetsEnabled, prefs.strictLimitsEnabled)
                        }
                    }
                )
            }

            // Rollover Budgets Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rollover Budgets", fontWeight = FontWeight.Medium)
                    Text("Carry over unspent budget to the next month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = generalPrefs?.rolloverBudgetsEnabled ?: false,
                    onCheckedChange = {
                        generalPrefs?.let { prefs ->
                            viewModel.updateGeneralPreferences(prefs.dailyRemindersEnabled, it, prefs.strictLimitsEnabled)
                        }
                    }
                )
            }

            // Strict Limits Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strict Budget Limits", fontWeight = FontWeight.Medium)
                    Text("Prevent saving a transaction if it exceeds budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = generalPrefs?.strictLimitsEnabled ?: true,
                    onCheckedChange = {
                        generalPrefs?.let { prefs ->
                            viewModel.updateGeneralPreferences(prefs.dailyRemindersEnabled, prefs.rolloverBudgetsEnabled, it)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.data_management_header),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Wallet Management Card
            Card(
                onClick = onNavigateToWallets,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Wallets", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.manage_wallets), fontWeight = FontWeight.Medium)
                }
            }

            // Category Management Card
            Card(
                onClick = onNavigateToCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Category, contentDescription = "Categories", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.manage_categories), fontWeight = FontWeight.Medium)
                }
            }

            // Recurring Transactions Card
            Card(
                onClick = onNavigateToRecurring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Repeat, contentDescription = "Recurring Transactions", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Manage Recurring Transactions", fontWeight = FontWeight.Medium)
                }
            }

            // Export Data Card
            Card(
                onClick = { exportLauncher.launch("BudgetTrackerBackup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = "Export Data", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Export Data (Backup)", fontWeight = FontWeight.Medium)
                }
            }

            // Import Data Card
            Card(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Import Data", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Import Data (Restore)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        Text("Warning: Replaces all existing data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Check for Updates Card
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Card(
                onClick = { uriHandler.openUri("https://github.com/KatsuoSaito/BudgetTracker/releases") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Check for Updates", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Check for Updates", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.budgeting_rule_header),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text("Adjust your target allocations. Must equal 100%.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Needs Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Needs", fontWeight = FontWeight.Bold)
                    Text("${needs.roundToInt()}%")
                }
                Slider(
                    value = needs,
                    onValueChange = { needs = it },
                    valueRange = 0f..100f,
                    steps = 100
                )
            }

            // Wants Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wants", fontWeight = FontWeight.Bold)
                    Text("${wants.roundToInt()}%")
                }
                Slider(
                    value = wants,
                    onValueChange = { wants = it },
                    valueRange = 0f..100f,
                    steps = 100
                )
            }

            // Savings Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Savings", fontWeight = FontWeight.Bold)
                    Text("${savings.roundToInt()}%")
                }
                Slider(
                    value = savings,
                    onValueChange = { savings = it },
                    valueRange = 0f..100f,
                    steps = 100
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.updateRule(needs.roundToInt(), wants.roundToInt(), savings.roundToInt()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isValid
            ) {
                Text(
                    if (isValid) "Save Rule" else "Total is $total%, must be 100%",
                    fontSize = 16.sp
                )
            }
        }
    }
}
