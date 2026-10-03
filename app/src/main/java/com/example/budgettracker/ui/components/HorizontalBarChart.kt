package com.example.budgettracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarChartData(
    val label: String,
    val value: Long,
    val color: Color,
    val percentageText: String? = null
)

@Composable
fun HorizontalBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    val maxVal = data.maxOfOrNull { it.value } ?: 0L
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        data.forEach { item ->
            val fraction = if (maxVal > 0L) (item.value.toDouble() / maxVal.toDouble()).toFloat() else 0f
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Row {
                        Text(text = com.example.budgettracker.ui.utils.CurrencyUtils.formatAmount(item.value), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.percentageText != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "(${item.percentageText})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    
                    // Background Track
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.15f),
                        size = size,
                        cornerRadius = cornerRadius
                    )
                    
                    // Animated Foreground Bar
                    if (fraction > 0f) {
                        drawRoundRect(
                            color = item.color,
                            size = Size(size.width * fraction * animatedProgress.value, size.height),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }
    }
}
