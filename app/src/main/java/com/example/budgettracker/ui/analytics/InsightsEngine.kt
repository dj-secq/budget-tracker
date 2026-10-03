package com.example.budgettracker.ui.analytics

import android.content.res.Resources
import com.example.budgettracker.R
import com.example.budgettracker.domain.SpendingTrend
import com.example.budgettracker.domain.spendingDecreasePercent
import com.example.budgettracker.domain.spendingIncreasePercent
import com.example.budgettracker.domain.spendingTrend
import com.example.budgettracker.ui.utils.CurrencyUtils

data class Insight(
    val title: String,
    val message: String,
    val type: InsightType
)

enum class InsightType {
    PRAISE, WARNING, OBSERVATION
}

object InsightsEngine {

    fun generateInsights(
        hasTransactions: Boolean,
        totalIncome: Long,
        totalExpenses: Long,
        topExpenseName: String?,
        topExpenseAmount: Long,
        previousExpenseTotal: Long,
        hasPreviousMonth: Boolean,
        monthOpen: Boolean,
        resources: Resources
    ): List<Insight> {
        if (!hasTransactions) {
            return listOf(
                Insight(
                    resources.getString(R.string.insight_welcome_title),
                    resources.getString(R.string.insight_welcome_body),
                    InsightType.OBSERVATION
                )
            )
        }

        val insights = mutableListOf<Insight>()

        if (totalIncome > 0L) {
            val expenseRatio = totalExpenses.toDouble() / totalIncome
            if (expenseRatio > 0.8) {
                insights.add(
                    Insight(
                        resources.getString(R.string.insight_high_spending_title),
                        resources.getString(R.string.insight_high_spending_body, (expenseRatio * 100).toInt()),
                        InsightType.WARNING
                    )
                )
            } else if (expenseRatio < 0.5) {
                insights.add(
                    Insight(
                        resources.getString(R.string.insight_great_saving_title),
                        resources.getString(R.string.insight_great_saving_body),
                        InsightType.PRAISE
                    )
                )
            }
        }

        if (topExpenseName != null && topExpenseAmount > 0L) {
            insights.add(
                Insight(
                    resources.getString(R.string.insight_top_expense_title),
                    resources.getString(
                        R.string.insight_top_expense_body,
                        topExpenseName,
                        CurrencyUtils.formatAmount(topExpenseAmount)
                    ),
                    InsightType.OBSERVATION
                )
            )
        }

        if (hasPreviousMonth) {
            when (spendingTrend(totalExpenses, previousExpenseTotal, monthOpen)) {
                SpendingTrend.UP -> {
                    val increase = spendingIncreasePercent(totalExpenses, previousExpenseTotal) ?: 0
                    insights.add(
                        Insight(
                            resources.getString(R.string.insight_trending_up_title),
                            resources.getString(R.string.insight_trending_up_body, increase),
                            InsightType.WARNING
                        )
                    )
                }
                SpendingTrend.DOWN -> {
                    val decrease = spendingDecreasePercent(totalExpenses, previousExpenseTotal) ?: 0
                    insights.add(
                        Insight(
                            resources.getString(R.string.insight_trending_down_title),
                            resources.getString(R.string.insight_trending_down_body, decrease),
                            InsightType.PRAISE
                        )
                    )
                }
                SpendingTrend.NONE -> Unit
            }
        }

        return insights.ifEmpty {
            listOf(
                Insight(
                    resources.getString(R.string.insight_on_track_title),
                    resources.getString(R.string.insight_on_track_body),
                    InsightType.OBSERVATION
                )
            )
        }
    }
}
