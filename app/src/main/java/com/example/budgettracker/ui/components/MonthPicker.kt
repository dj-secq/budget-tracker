package com.example.budgettracker.ui.components

import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun MonthPicker(
    currentMonth: Int,
    currentYear: Int,
    onMonthChanged: (month: Int, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            var m = currentMonth - 1
            var y = currentYear
            if (m < 1) {
                m = 12
                y -= 1
            }
            onMonthChanged(m, y)
        }) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.previous_month))
        }

        val monthName = DateFormatSymbols().months[currentMonth - 1]
        Text(
            text = "$monthName $currentYear",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val now = Calendar.getInstance()
        val isCurrentOrFuture = currentYear > now.get(Calendar.YEAR) || 
            (currentYear == now.get(Calendar.YEAR) && currentMonth >= now.get(Calendar.MONTH) + 1)

        IconButton(
            onClick = {
                var m = currentMonth + 1
                var y = currentYear
                if (m > 12) {
                    m = 1
                    y += 1
                }
                onMonthChanged(m, y)
            },
            enabled = !isCurrentOrFuture // Prevent navigating into the future
        ) {
            Icon(
                Icons.Filled.ChevronRight, 
                contentDescription = stringResource(R.string.next_month),
                tint = if (isCurrentOrFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
