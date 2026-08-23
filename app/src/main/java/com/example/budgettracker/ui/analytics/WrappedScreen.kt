package com.example.budgettracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    month: Int,
    year: Int,
    viewModel: WrappedViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(month, year) {
        viewModel.loadWrappedData(month, year)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Wrapped")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { 5 })
            
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> IntroPage(uiState)
                        1 -> BigPicturePage(uiState)
                        2 -> HeavyHitterPage(uiState)
                        3 -> TopCategoriesPage(uiState)
                        4 -> VerdictPage(uiState)
                    }
                }
                
                // Page Indicator
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntroPage(state: WrappedUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ready for your", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("${state.monthName} Wrapped?", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Swipe to see how you did.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BigPicturePage(state: WrappedUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The Big Picture", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Income", fontSize = 16.sp)
                Text(CurrencyUtils.formatAmount(state.totalIncome), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Spent", fontSize = 16.sp)
                Text(CurrencyUtils.formatAmount(state.totalSpent), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        val netColor = if (state.netSavings >= 0) EmeraldGreen else MaterialTheme.colorScheme.error
        Text("Net Savings: ${CurrencyUtils.formatAmount(state.netSavings)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = netColor)
    }
}

@Composable
fun HeavyHitterPage(state: WrappedUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The Heavy Hitter", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your largest single expense was...", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        if (state.largestExpense != null) {
            Text(state.largestExpenseCategory, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(CurrencyUtils.formatAmount(state.largestExpense.amount), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
            if (state.largestExpense.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\"${state.largestExpense.note}\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("Nothing!", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TopCategoriesPage(state: WrappedUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Top Categories", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Where did your money go?", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (state.topCategories.isEmpty()) {
            Text("No expenses this month!", fontSize = 20.sp)
        } else {
            state.topCategories.forEachIndexed { index, pair ->
                val medal = when(index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "" }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$medal ${pair.first}", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(CurrencyUtils.formatAmount(pair.second), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun VerdictPage(state: WrappedUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The Verdict", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(state.verdict, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Text("See you next month!", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
