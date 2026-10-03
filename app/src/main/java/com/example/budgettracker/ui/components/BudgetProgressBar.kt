package com.example.budgettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgettracker.R
import com.example.budgettracker.domain.paceDeltaCentavos
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils

@Composable
fun BudgetProgressBar(
    categoryName: String,
    spent: Long,
    limit: Long,
    baseColor: Color,
    icon: ImageVector? = null,
    paceDay: Int? = null,
    paceLength: Int? = null,
    modifier: Modifier = Modifier
) {
    val progress = if (limit > 0L) (spent.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (limit > 0L) ((spent * 100L) / limit).toInt() else 0
    val percentLabel = stringResource(R.string.progress_percent, percent)
    val isOverBudget = spent > limit
    
    // Choose colors
    val activeColor = if (isOverBudget) MaterialTheme.colorScheme.error else baseColor
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = categoryName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            
                val remaining = limit - spent
                if (isOverBudget) {
                    Text(
                        text = stringResource(R.string.over_amount, CurrencyUtils.formatAmount(kotlin.math.abs(remaining))),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(R.string.left_amount, CurrencyUtils.formatAmount(remaining)),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldGreen
                    )
                }
            }
        
            Spacer(modifier = Modifier.height(8.dp))
        
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(trackColor)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        contentDescription = percentLabel
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .background(activeColor)
                )
            }
        
            Spacer(modifier = Modifier.height(4.dp))
        
            Text(
                text = stringResource(
                    R.string.spent_of,
                    CurrencyUtils.formatAmount(spent),
                    CurrencyUtils.formatAmount(limit)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val pace = if (paceDay != null && paceLength != null) {
                paceDeltaCentavos(spent, limit, paceDay, paceLength)
            } else {
                null
            }
            if (pace != null) {
                val paceText = when {
                    pace > 0L -> stringResource(R.string.pace_over, CurrencyUtils.formatAmount(pace))
                    pace < 0L -> stringResource(R.string.pace_under, CurrencyUtils.formatAmount(kotlin.math.abs(pace)))
                    else -> stringResource(R.string.pace_on)
                }
                Text(
                    text = paceText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pace > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
        }
    }
}
