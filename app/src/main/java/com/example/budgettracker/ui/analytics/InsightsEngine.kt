package com.example.budgettracker.ui.analytics

import com.example.budgettracker.data.local.entity.Category
import com.example.budgettracker.data.local.entity.Transaction
import com.example.budgettracker.data.local.entity.CategoryType

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
        currentMonthTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        categories: List<Category>,
        totalIncome: Double,
        totalExpenses: Double
    ): List<Insight> {
        val insights = mutableListOf<Insight>()
        
        if (currentMonthTransactions.isEmpty()) {
            return listOf(
                Insight("Welcome!", "Start logging transactions to get personalized insights.", InsightType.OBSERVATION)
            )
        }

        // 1. Spending Ratio Warning
        if (totalIncome > 0) {
            val expenseRatio = totalExpenses / totalIncome
            if (expenseRatio > 0.8) {
                insights.add(Insight(
                    "High Spending", 
                    "You've already spent ${(expenseRatio * 100).toInt()}% of your income. Consider slowing down!", 
                    InsightType.WARNING
                ))
            } else if (expenseRatio < 0.5) {
                insights.add(Insight(
                    "Great Saving!", 
                    "You've kept your expenses under 50% of your income. Outstanding work!", 
                    InsightType.PRAISE
                ))
            }
        }

        // 2. Highest Category Observation
        val expenseCategoriesMap = categories.filter { it.type == CategoryType.EXPENSE && it.name != "Withdraw / Transfer Out" }.associateBy { it.id }
        
        val categoryTotals = currentMonthTransactions
            .filter { expenseCategoriesMap.containsKey(it.categoryId) }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            
        val maxCategory = categoryTotals.maxByOrNull { it.value }
        
        if (maxCategory != null && maxCategory.value > 0) {
            val catName = expenseCategoriesMap[maxCategory.key]?.name ?: "Unknown"
            insights.add(Insight(
                "Top Expense", 
                "Your highest expense category this month is '$catName', taking up ${com.example.budgettracker.ui.utils.CurrencyUtils.formatAmount(maxCategory.value)}.", 
                InsightType.OBSERVATION
            ))
        }

        // 3. Month-over-Month Comparison
        if (previousMonthTransactions.isNotEmpty()) {
            val prevTotalExpenses = previousMonthTransactions
                .filter { expenseCategoriesMap.containsKey(it.categoryId) }
                .sumOf { it.amount }
                
            if (totalExpenses < prevTotalExpenses * 0.9) {
                insights.add(Insight(
                    "Trending Down", 
                    "Awesome! You spent less this month compared to the previous month.", 
                    InsightType.PRAISE
                ))
            } else if (totalExpenses > prevTotalExpenses * 1.2) {
                insights.add(Insight(
                    "Trending Up", 
                    "Watch out! Your expenses are 20% higher than last month.", 
                    InsightType.WARNING
                ))
            }
        }

        return insights.ifEmpty { 
            listOf(Insight("On Track", "Your finances look stable. Keep up the good habits!", InsightType.OBSERVATION)) 
        }
    }
}
