package com.example.budgettracker.ui.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun WealthChart(
    dataPoints: List<Float>,
    monthLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    // Convert data to FloatEntry
    val entries = dataPoints.mapIndexed { index, value -> 
        FloatEntry(x = index.toFloat(), y = value)
    }
    
    val chartEntryModel = remember(dataPoints) { entryModelOf(entries) }
    val formatter = remember(monthLabels) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            monthLabels.getOrNull(value.toInt()) ?: value.toInt().toString()
        }
    }
    val pesoAxis = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ -> wholePesoLabel(value) }
    }

    Chart(
        chart = lineChart(),
        model = chartEntryModel,
        startAxis = rememberStartAxis(valueFormatter = pesoAxis),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
