package com.example.budgettracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.atan2

data class PieChartData(
    val label: String,
    val value: Long,
    val color: Color
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    donutHoleRatio: Float = 0.5f,
    onSliceClick: ((PieChartData) -> Unit)? = null
) {
    val total = data.sumOf { it.value }.toFloat()
    val animatedProgress = remember { Animatable(0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val x = tapOffset.x - center.x
                    val y = tapOffset.y - center.y
                    
                    val angleInDegrees = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                    var normalizedAngle = angleInDegrees + 90f
                    if (normalizedAngle < 0) normalizedAngle += 360f

                    var currentStart = 0f
                    var foundIndex: Int? = null
                    for (i in data.indices) {
                        val sweep = if (total > 0) (data[i].value.toFloat() / total) * 360f else 0f
                        if (normalizedAngle >= currentStart && normalizedAngle <= currentStart + sweep) {
                            foundIndex = i
                            break
                        }
                        currentStart += sweep
                    }

                    if (foundIndex != null) {
                        selectedIndex = if (selectedIndex == foundIndex) null else foundIndex
                        if (selectedIndex != null) {
                            onSliceClick?.invoke(data[foundIndex])
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseStrokeWidth = (size.width / 2) * (1f - donutHoleRatio)
            val outerRadius = size.width / 2
            
            var startAngle = -90f

            data.forEachIndexed { index, slice ->
                val sweepAngle = if (total > 0) (slice.value.toFloat() / total) * 360f else 0f
                val animatedSweep = sweepAngle * animatedProgress.value
                
                // Increase stroke width if selected
                val strokeWidth = if (index == selectedIndex) baseStrokeWidth * 1.2f else baseStrokeWidth
                val radius = outerRadius - strokeWidth / 2
                val sizeToDraw = Size(radius * 2, radius * 2)
                
                // Calculate center offset so it stays centered even when stroke increases
                val offset = (outerRadius - radius)
                val topLeft = Offset(offset, offset)

                if (animatedSweep > 0) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = sizeToDraw,
                        style = Stroke(width = strokeWidth)
                    )
                }
                startAngle += sweepAngle
            }
            
            // Draw empty state if no data
            if (total == 0f || data.isEmpty()) {
                val radius = outerRadius - baseStrokeWidth / 2
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f * animatedProgress.value,
                    useCenter = false,
                    topLeft = Offset(baseStrokeWidth / 2, baseStrokeWidth / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = baseStrokeWidth)
                )
            }
        }
    }
}
