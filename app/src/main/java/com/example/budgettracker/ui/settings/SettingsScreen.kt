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
import android.net.Uri
import com.example.budgettracker.R
import android.os.Build
import com.example.budgettracker.data.repository.Accent
import com.example.budgettracker.data.repository.ThemeMode
import com.example.budgettracker.ui.lock.DeviceLock
import com.example.budgettracker.ui.utils.label
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
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri) { success ->
                Toast.makeText(context, if (success) context.getString(R.string.export_success) else context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri) { success ->
                Toast.makeText(context, if (success) context.getString(R.string.export_success) else context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(context, uri) { outcome ->
                val message = if (outcome == null || !outcome.headerOk) {
                    context.getString(R.string.import_failed)
                } else {
                    context.getString(R.string.import_csv_result, outcome.added, outcome.skipped, outcome.rejected)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                style = MaterialTheme.typography.titleLarge,
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
                        Text(mode.label())
                    }
                }
            }

            Text(
                text = stringResource(R.string.accent_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Accent.entries.forEachIndexed { index, accent ->
                    SegmentedButton(
                        selected = budgetRule?.accent == accent,
                        onClick = { viewModel.updateAccent(accent) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Accent.entries.size)
                    ) {
                        Text(accent.label())
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dynamic_color), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.dynamic_color_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = budgetRule?.dynamicColor == true,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.general_preferences),
                style = MaterialTheme.typography.titleLarge,
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
                    Text(stringResource(R.string.daily_reminders), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.daily_reminders_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            val reminderChoices = listOf(
                8 to R.string.reminder_morning,
                13 to R.string.reminder_afternoon,
                20 to R.string.reminder_evening
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                reminderChoices.forEachIndexed { index, (hour, label) ->
                    SegmentedButton(
                        selected = (generalPrefs?.reminderHour ?: 20) == hour,
                        onClick = { viewModel.setReminderHour(context, hour) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = reminderChoices.size)
                    ) {
                        Text(stringResource(label))
                    }
                }
            }

            // Rollover Budgets Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.rollover_budgets), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.rollover_budgets_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(stringResource(R.string.strict_limits), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.strict_limits_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = generalPrefs?.strictLimitsEnabled ?: false,
                    onCheckedChange = {
                        generalPrefs?.let { prefs ->
                            viewModel.updateGeneralPreferences(prefs.dailyRemindersEnabled, prefs.rolloverBudgetsEnabled, it)
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_lock_title), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.app_lock_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = generalPrefs?.appLockEnabled == true,
                    onCheckedChange = { enabled ->
                        if (enabled && !DeviceLock.canLock(context)) {
                            Toast.makeText(context, context.getString(R.string.app_lock_unavailable), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setAppLockEnabled(enabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.data_management_header),
                style = MaterialTheme.typography.titleLarge,
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
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = stringResource(R.string.wallets_cd), tint = MaterialTheme.colorScheme.primary)
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
                    Icon(Icons.Filled.Category, contentDescription = stringResource(R.string.categories), tint = MaterialTheme.colorScheme.primary)
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
                    Icon(Icons.Filled.Repeat, contentDescription = stringResource(R.string.recurring_cd), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.manage_recurring), fontWeight = FontWeight.Medium)
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
                    Icon(Icons.Filled.Upload, contentDescription = stringResource(R.string.export_cd), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.export_backup), fontWeight = FontWeight.Medium)
                }
            }

            Card(
                onClick = { csvLauncher.launch("BudgetTrackerTransactions.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = stringResource(R.string.export_csv_cd), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.export_csv), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.export_csv_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(
                onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.import_csv), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.import_csv), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.import_csv_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.import_cd), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.import_restore), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.import_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Card(
                onClick = {
                    viewModel.recalculateBalances { success ->
                        Toast.makeText(
                            context,
                            context.getString(if (success) R.string.recalculate_success else R.string.recalculate_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = stringResource(R.string.recalculate_cd), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.recalculate_balances), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.recalculate_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.check_updates), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.check_updates), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.budgeting_rule_header),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(stringResource(R.string.rule_help), color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Needs Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.needs), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.wants), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.savings), fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = isValid
            ) {
                Text(
                    if (isValid) stringResource(R.string.save_rule) else stringResource(R.string.save_rule_invalid, total),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.replace_all_title)) },
            text = { Text(stringResource(R.string.replace_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    viewModel.importData(context, uri) { success ->
                        Toast.makeText(context, if (success) context.getString(R.string.import_success) else context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}
