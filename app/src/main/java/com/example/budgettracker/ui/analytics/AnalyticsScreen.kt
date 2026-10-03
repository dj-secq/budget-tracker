package com.example.budgettracker.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.example.budgettracker.R
import com.example.budgettracker.ui.components.BarChartData
import com.example.budgettracker.ui.components.HorizontalBarChart
import com.example.budgettracker.ui.components.MonthPicker
import com.example.budgettracker.ui.theme.CategoryColors
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils
import com.example.budgettracker.ui.components.charts.CashflowChart
import com.example.budgettracker.ui.components.charts.WealthChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ThumbUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToWrapped: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCategoryType by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_analytics), fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp)
        ) {
            MonthPicker(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                onMonthChanged = { m, y -> viewModel.setMonth(m, y) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.hasTransactions) {
                androidx.compose.material3.TextButton(
                    onClick = { onNavigateToWrapped(uiState.currentMonth, uiState.currentYear) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.view_wrapped))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            uiState.insights.forEach { insight ->
                val (icon, color) = when (insight.type) {
                    InsightType.PRAISE -> Icons.Filled.ThumbUp to EmeraldGreen
                    InsightType.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
                    InsightType.OBSERVATION -> Icons.Filled.Lightbulb to MaterialTheme.colorScheme.primary
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(icon, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(insight.title, fontWeight = FontWeight.Bold, color = color)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(insight.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // High-Level Financial Overview
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.income), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatAmount(uiState.totalIncome), fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.expenses), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatAmount(uiState.totalExpenses), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val netBalance = uiState.totalIncome - uiState.totalExpenses
                    val netColor = if (netBalance >= 0) EmeraldGreen else MaterialTheme.colorScheme.error
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.net_balance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${if (netBalance >= 0) "+" else "-"}${CurrencyUtils.formatAmount(Math.abs(netBalance))}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = netColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val expenseRatio = if (uiState.totalIncome > 0L) {
                        (uiState.totalExpenses.toDouble() / uiState.totalIncome.toDouble()).toFloat().coerceIn(0f, 1f)
                    } else if (uiState.totalExpenses > 0L) {
                        1f
                    } else {
                        0f
                    }
                    val expensePercent = stringResource(R.string.progress_percent, (expenseRatio * 100).toInt())
                    LinearProgressIndicator(
                        progress = { expenseRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .semantics {
                                progressBarRangeInfo = ProgressBarRangeInfo(expenseRatio, 0f..1f)
                                contentDescription = expensePercent
                            },
                        color = if (expenseRatio > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val windowEmpty = uiState.monthTrends.isEmpty() || uiState.monthTrends.all {
                it.incomeCentavos == 0L && it.expenseCentavos == 0L
            }
            if (windowEmpty && uiState.totalIncome == 0L && uiState.totalExpenses == 0L) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_data_month),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.add_to_see_analytics),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.budget_rule)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.categories)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text(stringResource(R.string.trends)) }
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedTabIndex == 0) {
                        // Dashboard Cards
                        uiState.bucketStats.forEach { stat ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = stat.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(R.string.goal_amount, CurrencyUtils.formatAmount(stat.goal)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(stringResource(R.string.actual_amount, CurrencyUtils.formatAmount(stat.actual)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val progressRatio = if (stat.goal > 0L) {
                                        (stat.actual.toDouble() / stat.goal.toDouble()).toFloat().coerceIn(0f, 1f)
                                    } else if (stat.actual > 0L) {
                                        1f
                                    } else {
                                        0f
                                    }
                                    val statusColor = if (stat.onTrack) EmeraldGreen else MaterialTheme.colorScheme.error
                                    val statusText = when {
                                        stat.goal <= 0L -> stringResource(R.string.no_cap)
                                        stat.savingsTarget && stat.onTrack -> stringResource(R.string.on_track)
                                        stat.savingsTarget -> stringResource(R.string.short_amount, CurrencyUtils.formatAmount(stat.goal - stat.actual))
                                        stat.onTrack -> stringResource(R.string.within_budget)
                                        else -> stringResource(R.string.over_by, CurrencyUtils.formatAmount(stat.actual - stat.goal))
                                    }
                                    
                                    val bucketPercent = stringResource(R.string.progress_percent, (progressRatio * 100).toInt())
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .semantics {
                                                progressBarRangeInfo = ProgressBarRangeInfo(progressRatio, 0f..1f)
                                                contentDescription = bucketPercent
                                            },
                                        color = statusColor,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else if (selectedTabIndex == 1) {
                        // Categories Tab
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                                SegmentedButton(
                                    selected = selectedCategoryType == 0,
                                    onClick = { selectedCategoryType = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text(stringResource(R.string.expenses))
                                }
                                SegmentedButton(
                                    selected = selectedCategoryType == 1,
                                    onClick = { selectedCategoryType = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text(stringResource(R.string.income))
                                }
                            }

                        val activeCategorySpending = if (selectedCategoryType == 0) uiState.expenseCategorySpending else uiState.incomeCategorySpending
                        val activeTotal = if (selectedCategoryType == 0) uiState.totalExpenses else uiState.totalIncome

                        if (activeCategorySpending.isEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                    text = if (selectedCategoryType == 0) stringResource(R.string.no_expenses_month) else stringResource(R.string.no_income_month),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedCategoryType == 0) stringResource(R.string.expense_breakdown) else stringResource(R.string.income_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val barData = activeCategorySpending.sortedByDescending { it.totalSpent }.map { item ->
                                val color = CategoryColors[(item.category.id % CategoryColors.size).toInt()]
                                val percentage = if (activeTotal > 0L) (item.totalSpent.toDouble() / activeTotal.toDouble()) * 100.0 else 0.0
                                BarChartData(
                                    label = item.category.name,
                                    value = item.totalSpent,
                                    color = color,
                                    percentageText = "${String.format(java.util.Locale.getDefault(), "%.1f", percentage)}%"
                                )
                            }
                            HorizontalBarChart(
                                data = barData,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (selectedTabIndex == 2) {
                        Text(
                            text = stringResource(R.string.cashflow_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CashflowChart(
                            incomeData = uiState.cashflowIncome,
                            expenseData = uiState.cashflowExpense,
                            monthLabels = uiState.cashflowLabels
                        )
                        uiState.averageExpenseCentavos?.let { average ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.typical_month, CurrencyUtils.formatAmount(average)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.monthTrends.forEachIndexed { index, trend ->
                            TrendMonthRow(
                                trend = trend,
                                bold = index == uiState.monthTrends.lastIndex
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (uiState.categoryMoves.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.category_moves_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.categoryMoves.forEach { move ->
                                val up = move.deltaCentavos > 0L
                                Text(
                                    text = stringResource(
                                        if (up) R.string.move_up else R.string.move_down,
                                        move.name,
                                        CurrencyUtils.formatAmount(kotlin.math.abs(move.deltaCentavos))
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (up) MaterialTheme.colorScheme.error else EmeraldGreen,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.leftover_each_month),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.leftover_caption),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val netData = uiState.cashflowIncome.zip(uiState.cashflowExpense) { inc, exp -> inc - exp }
                        WealthChart(dataPoints = netData, monthLabels = uiState.cashflowLabels)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendMonthRow(trend: MonthTrend, bold: Boolean) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    val leftColor = if (trend.leftoverCentavos < 0L) MaterialTheme.colorScheme.error else EmeraldGreen
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trend.label,
            fontWeight = weight,
            style = MaterialTheme.typography.titleMedium,
            softWrap = true
        )
        Text(
            text = stringResource(R.string.trend_income, CurrencyUtils.formatAmount(trend.incomeCentavos)),
            fontWeight = weight,
            style = MaterialTheme.typography.bodyMedium,
            softWrap = true
        )
        Text(
            text = stringResource(R.string.trend_expenses, CurrencyUtils.formatAmount(trend.expenseCentavos)),
            fontWeight = weight,
            style = MaterialTheme.typography.bodyMedium,
            softWrap = true
        )
        Text(
            text = stringResource(R.string.trend_left, CurrencyUtils.formatAmount(trend.leftoverCentavos)),
            fontWeight = weight,
            color = leftColor,
            style = MaterialTheme.typography.bodyMedium,
            softWrap = true
        )
        if (trend.open) {
            Text(
                text = stringResource(R.string.month_still_open),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true
            )
        }
    }
}
