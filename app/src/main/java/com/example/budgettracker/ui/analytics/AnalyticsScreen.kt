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
import com.example.budgettracker.R
import com.example.budgettracker.ui.components.BarChartData
import com.example.budgettracker.ui.components.HorizontalBarChart
import com.example.budgettracker.ui.components.MonthPicker
import com.example.budgettracker.ui.components.PieChart
import com.example.budgettracker.ui.components.PieChartData
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
            
            androidx.compose.material3.Button(
                onClick = { onNavigateToWrapped(uiState.currentMonth, uiState.currentYear) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Monthly Wrapped")
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Insight Card
            if (uiState.insights.isNotEmpty()) {
                val insight = uiState.insights.first()
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
                            Text(insight.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                            Text("Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatAmount(uiState.totalIncome), fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatAmount(uiState.totalExpenses), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val netBalance = uiState.totalIncome - uiState.totalExpenses
                    val netColor = if (netBalance >= 0) EmeraldGreen else MaterialTheme.colorScheme.error
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Balance", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${if (netBalance >= 0) "+" else "-"}${CurrencyUtils.formatAmount(Math.abs(netBalance))}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = netColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val expenseRatio = if (uiState.totalIncome > 0) (uiState.totalExpenses / uiState.totalIncome).toFloat().coerceIn(0f, 1f) else if (uiState.totalExpenses > 0) 1f else 0f
                    LinearProgressIndicator(
                        progress = { expenseRatio },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (expenseRatio > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.totalExpenses == 0.0 && uiState.totalIncome == 0.0) {
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
                        "No data this month",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                    Text(
                        "Add transactions to see analytics",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
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
                        text = { Text("Budget Rule") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Categories") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Trends") }
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
                                        Text("Goal: ${CurrencyUtils.formatAmount(stat.goal)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Actual: ${CurrencyUtils.formatAmount(stat.actual)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val progressRatio = if (stat.goal > 0) (stat.actual / stat.goal).toFloat().coerceIn(0f, 1f) else if (stat.actual > 0) 1f else 0f
                                    val isUnderBudget = stat.net >= 0
                                    
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = if (isUnderBudget) EmeraldGreen else MaterialTheme.colorScheme.error,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Net: ${if (isUnderBudget) "+" else "-"}${CurrencyUtils.formatAmount(Math.abs(stat.net))}",
                                        color = if (isUnderBudget) EmeraldGreen else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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
                                    Text("Expenses")
                                }
                                SegmentedButton(
                                    selected = selectedCategoryType == 1,
                                    onClick = { selectedCategoryType = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("Income")
                                }
                            }

                        val activeCategorySpending = if (selectedCategoryType == 0) uiState.expenseCategorySpending else uiState.incomeCategorySpending
                        val activeTotal = if (selectedCategoryType == 0) uiState.totalExpenses else uiState.totalIncome

                        if (activeCategorySpending.isEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                    text = if (selectedCategoryType == 0) "No expenses this month." else "No income this month.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        } else {
                            // Pie Chart for spending breakdown
                            Spacer(modifier = Modifier.height(8.dp))
                            val titleText = if (selectedCategoryType == 0) "Expense Breakdown" else "Income Breakdown"
                            Text(
                                    text = titleText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val pieData = activeCategorySpending.sortedByDescending { it.totalSpent }.map { item ->
                                    val color = CategoryColors[(item.category.id % CategoryColors.size).toInt()]
                                    PieChartData(
                                        label = item.category.name,
                                        value = item.totalSpent,
                                        color = color
                                    )
                                }
                                
                                PieChart(
                                    data = pieData,
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .padding(vertical = 8.dp)
                                )
                                
                                // Legend
                                Spacer(modifier = Modifier.height(16.dp))
                                pieData.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(end = 0.dp)
                                        ) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawCircle(color = item.color)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyUtils.formatAmount(item.value),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            
                                Spacer(modifier = Modifier.height(32.dp))
                                val barTitle = if (selectedCategoryType == 0) "Spending by Category" else "Income by Category"
                                Text(
                                    text = barTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val barData = activeCategorySpending.sortedByDescending { it.totalSpent }.map { item ->
                                    val color = CategoryColors[(item.category.id % CategoryColors.size).toInt()]
                                    val percentage = if (activeTotal > 0) (item.totalSpent / activeTotal) * 100 else 0.0
                                    BarChartData(
                                        label = item.category.name,
                                        value = item.totalSpent,
                                        color = color,
                                        percentageText = "${String.format(java.util.Locale.getDefault(), "%.1f", percentage)}%"
                                    )
                                }

                                HorizontalBarChart(
                                    data = barData,
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                )
                        }
                    } else if (selectedTabIndex == 2) {
                        // Trends Tab (Vico Charts)
                            Text(
                                text = "Cashflow (Last 6 Months)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            CashflowChart(
                                incomeData = uiState.cashflowIncome,
                                expenseData = uiState.cashflowExpense
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text(
                                text = "Net Savings Trend",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val netData = uiState.cashflowIncome.zip(uiState.cashflowExpense) { inc, exp -> inc - exp }
                            WealthChart(dataPoints = netData)
                    }
                }
            }
        }
    }
}
