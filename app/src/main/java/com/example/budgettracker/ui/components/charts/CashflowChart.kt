package com.example.budgettracker.ui.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer

@Composable
fun CashflowChart(
    incomeData: List<Float>,
    expenseData: List<Float>,
    modifier: Modifier = Modifier
) {
    if (incomeData.isEmpty() || expenseData.isEmpty() || incomeData.size != expenseData.size) return

    val modelProducer = remember(incomeData, expenseData) {
        val incomeEntries = incomeData.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
        val expenseEntries = expenseData.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
        ChartEntryModelProducer(listOf(incomeEntries, expenseEntries))
    }

    Chart(
        chart = columnChart(
            mergeMode = ColumnChart.MergeMode.Grouped
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
