package com.example.budgettracker.ui.components.charts

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

fun wholePesoLabel(pesos: Float): String {
    val number = NumberFormat.getIntegerInstance(Locale("en", "PH"))
    return "₱" + number.format(pesos.roundToLong())
}

@Composable
fun CashflowChart(
    incomeData: List<Float>,
    expenseData: List<Float>,
    monthLabels: List<String>,
    modifier: Modifier = Modifier
) {
    if (incomeData.isEmpty() || expenseData.isEmpty() || incomeData.size != expenseData.size) return

    val incomeColumn = lineComponent(color = EmeraldGreen, thickness = 8.dp)
    val expenseColumn = lineComponent(color = MaterialTheme.colorScheme.error, thickness = 8.dp)
    val modelProducer = remember(incomeData, expenseData) {
        val incomeEntries = incomeData.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
        val expenseEntries = expenseData.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
        ChartEntryModelProducer(listOf(incomeEntries, expenseEntries))
    }
    val formatter = remember(monthLabels) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            monthLabels.getOrNull(value.toInt()) ?: ""
        }
    }
    val pesoAxis = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ -> wholePesoLabel(value) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = EmeraldGreen, label = stringResource(R.string.income))
            LegendDot(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.expenses))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Chart(
            chart = columnChart(
                columns = listOf(incomeColumn, expenseColumn),
                mergeMode = ColumnChart.MergeMode.Grouped
            ),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(valueFormatter = pesoAxis),
            bottomAxis = rememberBottomAxis(valueFormatter = formatter),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
