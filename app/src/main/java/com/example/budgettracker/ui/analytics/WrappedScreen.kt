package com.example.budgettracker.ui.analytics

import android.content.Intent
import com.example.budgettracker.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgettracker.domain.SpendCompare
import com.example.budgettracker.domain.StoryTitle
import com.example.budgettracker.ui.theme.EmeraldGreen
import com.example.budgettracker.ui.utils.CurrencyUtils
import java.time.format.TextStyle
import java.util.Locale

private enum class WrappedPage {
    INTRO, PICTURE, CAPS, ACTIVITY, LARGEST, TOP, QUIET, CLOSE
}

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
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_wrapped))
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
    ) { _ ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val story = uiState.story
            val pages = remember(story, uiState.transactionCount) {
                buildList {
                    add(WrappedPage.INTRO)
                    add(WrappedPage.PICTURE)
                    if (story.capped > 0) add(WrappedPage.CAPS)
                    if (uiState.transactionCount > 0) add(WrappedPage.ACTIVITY)
                    if (story.largest != null) add(WrappedPage.LARGEST)
                    if (story.topCategories.isNotEmpty()) add(WrappedPage.TOP)
                    if (story.daysCounted > 0) add(WrappedPage.QUIET)
                    add(WrappedPage.CLOSE)
                }
            }
            val pagerState = rememberPagerState(pageCount = { pages.size })
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    when (pages[index]) {
                        WrappedPage.INTRO -> IntroPage(uiState)
                        WrappedPage.PICTURE -> BigPicturePage(uiState)
                        WrappedPage.CAPS -> CapsPage(uiState)
                        WrappedPage.ACTIVITY -> ActivityPage(uiState)
                        WrappedPage.LARGEST -> HeavyHitterPage(uiState)
                        WrappedPage.TOP -> TopCategoriesPage(uiState)
                        WrappedPage.QUIET -> SaverPage(uiState)
                        WrappedPage.CLOSE -> VerdictPage(uiState)
                    }
                }
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
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
private fun pageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun titleText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
}

@Composable
fun IntroPage(state: WrappedUiState) {
    pageColumn {
        Text(stringResource(R.string.wrapped_ready), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            stringResource(R.string.wrapped_title, state.monthName),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.wrapped_swipe), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (state.story.monthOpen) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.story_open), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BigPicturePage(state: WrappedUiState) {
    val story = state.story
    pageColumn {
        titleText(stringResource(R.string.wrapped_big_picture))
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.total_income), style = MaterialTheme.typography.bodyLarge)
                Text(CurrencyUtils.formatAmount(state.totalIncome), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.total_spent), style = MaterialTheme.typography.bodyLarge)
                Text(CurrencyUtils.formatAmount(state.totalSpent), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        val netColor = if (story.leftoverCentavos >= 0) EmeraldGreen else MaterialTheme.colorScheme.error
        Text(
            stringResource(R.string.net_savings_line, CurrencyUtils.formatAmount(story.leftoverCentavos)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = netColor,
            textAlign = TextAlign.Center
        )
        val compare = when (story.compare) {
            SpendCompare.UP -> story.comparePercent?.let { stringResource(R.string.story_spent_up, it) }
            SpendCompare.DOWN -> story.comparePercent?.let { stringResource(R.string.story_spent_down, it) }
            SpendCompare.SAME -> stringResource(R.string.story_spent_same)
            SpendCompare.HIDDEN -> null
        }
        if (compare != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(compare, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CapsPage(state: WrappedUiState) {
    val story = state.story
    pageColumn {
        titleText(stringResource(R.string.story_caps_title))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.story_caps, story.insideCap, story.capped),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (story.overCap.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            story.overCap.forEach { over ->
                Text(
                    stringResource(R.string.story_over_by, over.name, CurrencyUtils.formatAmount(over.centavos)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ActivityPage(state: WrappedUiState) {
    val weekday = state.story.busiestWeekday?.getDisplayName(TextStyle.FULL, Locale.getDefault())
    pageColumn {
        titleText(stringResource(R.string.wrapped_activity))
        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.wrapped_made), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${state.transactionCount}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.wrapped_tx_count), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!weekday.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(stringResource(R.string.wrapped_busiest), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(weekday, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun HeavyHitterPage(state: WrappedUiState) {
    val largest = state.story.largest
    pageColumn {
        titleText(stringResource(R.string.wrapped_heavy))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.wrapped_largest), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (largest != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(largest.categoryName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(CurrencyUtils.formatAmount(largest.amountCentavos), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
            if (largest.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\"${largest.note}\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TopCategoriesPage(state: WrappedUiState) {
    pageColumn {
        titleText(stringResource(R.string.wrapped_top))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.wrapped_where), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        state.story.topCategories.forEachIndexed { index, category ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.wrapped_rank, index + 1, category.name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(CurrencyUtils.formatAmount(category.centavos), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun SaverPage(state: WrappedUiState) {
    val story = state.story
    pageColumn {
        titleText(stringResource(R.string.wrapped_saver))
        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.wrapped_you_had), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(EmeraldGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${story.noSpendDays}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.wrapped_no_spend), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.story_days_counted, story.noSpendDays, story.daysCounted),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerdictPage(state: WrappedUiState) {
    val context = LocalContext.current
    val story = state.story
    val title = storyTitle(story.title)
    val shareText = shareText(state, title)
    pageColumn {
        Text(stringResource(R.string.wrapped_verdict), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        if (story.mostlyCategory != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.story_mostly, story.mostlyCategory),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(send, context.getString(R.string.share_month)))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.share_month))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.wrapped_next), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun storyTitle(title: StoryTitle): String = stringResource(
    when (title) {
        StoryTitle.EMPTY -> R.string.story_empty
        StoryTitle.QUIET -> R.string.story_quiet
        StoryTitle.ONE_PURCHASE -> R.string.story_one_purchase
        StoryTitle.SEVERAL_INCOMES -> R.string.story_several_incomes
        StoryTitle.SAVER -> R.string.story_saver
        StoryTitle.AHEAD -> R.string.story_ahead
        StoryTitle.OVER -> R.string.story_over
    }
)

@Composable
private fun shareText(state: WrappedUiState, title: String): String {
    val story = state.story
    val lines = mutableListOf(
        stringResource(R.string.share_month_heading, state.monthName, state.year),
        stringResource(R.string.share_income, CurrencyUtils.formatAmount(state.totalIncome)),
        stringResource(R.string.share_spent, CurrencyUtils.formatAmount(state.totalSpent)),
        stringResource(R.string.share_left, CurrencyUtils.formatAmount(story.leftoverCentavos))
    )
    if (story.capped > 0) {
        lines += stringResource(R.string.story_caps, story.insideCap, story.capped)
    }
    lines += title
    if (story.mostlyCategory != null) {
        lines += stringResource(R.string.story_mostly, story.mostlyCategory)
    }
    return lines.joinToString("\n")
}
