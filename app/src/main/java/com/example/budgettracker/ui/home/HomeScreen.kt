package com.example.budgettracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.res.stringResource
import com.example.budgettracker.R
import com.example.budgettracker.domain.localDateOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.budgettracker.ui.components.BudgetProgressBar
import com.example.budgettracker.ui.components.MonthPicker
import com.example.budgettracker.data.local.entity.Account
import com.example.budgettracker.data.local.entity.AccountType
import com.example.budgettracker.ui.theme.CategoryColors
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.theme.WalletPalette
import com.example.budgettracker.ui.utils.CategoryIconHelper
import com.example.budgettracker.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAssignBudget: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebtTracker: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onOpenWallet: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                expanded = isFabExpanded,
                icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transaction_cd)) },
                text = { Text(stringResource(R.string.new_transaction)) }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 24.dp + innerPadding.calculateBottomPadding()
                )
            ) {
                // Header: Settings and Centered Total Balance
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    imageVector = Icons.Filled.Settings, 
                                    contentDescription = stringResource(R.string.settings_title),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.total_balance),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = CurrencyUtils.formatAmount(uiState.totalBalance),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                                val position = uiState.totalBalance + uiState.owedToYou - uiState.youOwe
                                val positionText = (if (position >= 0L) "+" else "-") +
                                    CurrencyUtils.formatAmount(kotlin.math.abs(position))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.position_line, positionText),
                                    color = if (position < 0L) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    LeftAfterBillsCard(
                        leftCentavos = uiState.leftAfterBills,
                        upcoming = uiState.upcoming,
                        onOpenRecurring = onNavigateToRecurring,
                        onOpenDebt = onNavigateToDebtTracker
                    )
                }

                // Month Picker
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthPicker(
                        currentMonth = uiState.currentMonth,
                        currentYear = uiState.currentYear,
                        onMonthChanged = { m, y -> viewModel.setMonth(m, y) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Income and Expenses Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.monthly_income), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, softWrap = true)
                            Text("+${CurrencyUtils.formatAmount(uiState.totalIncome)}", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, softWrap = true)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.monthly_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, softWrap = true, textAlign = TextAlign.End)
                            Text("-${CurrencyUtils.formatAmount(uiState.totalExpenses)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, softWrap = true, textAlign = TextAlign.End)
                        }
                    }
                    val net = uiState.totalIncome - uiState.totalExpenses
                    val netText = (if (net >= 0L) "+" else "-") + CurrencyUtils.formatAmount(kotlin.math.abs(net))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.net_line, netText),
                        color = if (net >= 0L) EmeraldGreen else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true
                    )
                }

                if (uiState.recent.isEmpty() && uiState.budgetItems.isEmpty() && uiState.totalIncome == 0L && uiState.totalExpenses == 0L) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.empty_month),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.recent.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.recent), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.recent, key = { it.transaction.id }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.transaction.note.ifBlank { row.categoryName }, fontWeight = FontWeight.Medium)
                                Text(row.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${if (row.income) "+" else "-"}${CurrencyUtils.formatAmount(row.transaction.amount)}",
                                color = if (row.income) EmeraldGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Wallets (Accounts)
                if (uiState.accounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(uiState.accounts) { account ->
                                AccountCard(account, onClick = { onOpenWallet(account.id) })
                            }
                        }
                    }
                }

                // Gamification Streaks & Debt Tracker
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Gamification Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.streaks_cd), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.days_logged), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.current_streak, uiState.currentStreak), color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium, softWrap = true)
                                Text(stringResource(R.string.longest_streak, uiState.longestStreak), color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium, softWrap = true)
                            }
                        }

                        // Debt Tracker Shortcut
                        Card(
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToDebtTracker,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MoneyOff, contentDescription = stringResource(R.string.debts_cd), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.debt_tracker), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.owed_to_you), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium)
                                Text(CurrencyUtils.formatAmount(uiState.owedToYou), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.you_owe), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium)
                                Text(CurrencyUtils.formatAmount(uiState.youOwe), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Budget Header with assign icon
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.budgets_header),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        TextButton(onClick = onNavigateToAssignBudget) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.assign))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Budget items with category icons
                items(uiState.budgetItems) { item ->
                    val color = CategoryColors[(item.category.id % CategoryColors.size).toInt()]
                    val today = LocalDate.now()
                    val selected = YearMonth.of(uiState.currentYear, uiState.currentMonth)
                    val paceDay = when {
                        selected.isAfter(YearMonth.from(today)) -> null
                        selected == YearMonth.from(today) -> today.dayOfMonth
                        else -> selected.lengthOfMonth()
                    }
                    BudgetProgressBar(
                        categoryName = item.category.name,
                        spent = item.spent,
                        limit = item.limit,
                        baseColor = color,
                        icon = CategoryIconHelper.getIconForCategory(item.category.name),
                        paceDay = paceDay,
                        paceLength = if (paceDay == null) null else selected.lengthOfMonth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LeftAfterBillsCard(
    leftCentavos: Long,
    upcoming: List<HomeUpcoming>,
    onOpenRecurring: () -> Unit,
    onOpenDebt: () -> Unit
) {
    val dayFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val rows = upcoming.take(5)
    val leftText = (if (leftCentavos < 0L) "-" else "") + CurrencyUtils.formatAmount(kotlin.math.abs(leftCentavos))
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.left_after_bills),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = leftText,
                color = if (leftCentavos < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                softWrap = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.left_after_bills_caption),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                softWrap = true
            )
            if (rows.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.nothing_due_this_month),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = true
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                rows.forEach { row ->
                    val day = dayFormat.format(localDateOf(row.whenMillis))
                    val direction = stringResource(if (row.arrives) R.string.bill_arrives else R.string.bill_leaves)
                    val amount = (if (row.arrives) "+" else "-") + CurrencyUtils.formatAmount(row.amountCentavos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { if (row.opensDebt) onOpenDebt() else onOpenRecurring() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(row.name, fontWeight = FontWeight.Medium, softWrap = true)
                            Text(
                                text = stringResource(R.string.bill_when, day, direction),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        Text(
                            text = amount,
                            color = if (row.arrives) EmeraldGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, onClick: () -> Unit) {
    val contentColor = WalletPalette.contentOn(account.colorArgb)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(account.colorArgb)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.width(160.dp).heightIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).heightIn(min = 68.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when (account.type) {
                        AccountType.CHECKING -> "✓"
                        AccountType.SAVINGS -> "🏦"
                        AccountType.CREDIT -> "💳"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = CurrencyUtils.formatAmount(account.balance),
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                softWrap = true
            )
        }
    }
}
