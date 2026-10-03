package com.example.budgettracker.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIconForCategory(categoryName: String, iconName: String? = null): ImageVector {
        iconForKey(iconName)?.let { return it }
        return iconForKey(categoryName.lowercase()) ?: Icons.Filled.List
    }

    fun iconChoices(): List<String> = listOf(
        "groceries", "food", "transport", "rent", "utilities", "health",
        "shopping", "entertainment", "education", "insurance", "investments",
        "gifts", "personal care", "travel", "subscription", "maintenance",
        "savings", "emergency fund", "loan", "loan paid", "lost",
        "salary", "allowance", "bonus", "rent income", "loan received", "loan repaid"
    )

    private fun iconForKey(key: String?): ImageVector? {
        return when (key?.lowercase()) {
            "groceries" -> Icons.Filled.ShoppingCart
            "food", "dining", "restaurant" -> Icons.Filled.Restaurant
            "rent", "housing", "mortgage" -> Icons.Filled.Home
            "entertainment", "fun", "movies" -> Icons.Filled.Movie
            "transport", "car", "gas" -> Icons.Filled.DirectionsCar
            "salary", "income", "paycheck" -> Icons.Filled.AttachMoney
            "utilities", "electric", "water" -> Icons.Filled.Bolt
            "health", "medical", "pharmacy" -> Icons.Filled.LocalHospital
            "shopping", "clothes" -> Icons.Filled.LocalMall
            "education", "school", "tuition" -> Icons.Filled.School
            "insurance", "protection" -> Icons.Filled.Security
            "investments", "stock", "crypto" -> Icons.AutoMirrored.Filled.TrendingUp
            "gifts", "donation", "present" -> Icons.Filled.CardGiftcard
            "personal care", "beauty", "spa" -> Icons.Filled.Face
            "travel", "flight", "vacation" -> Icons.Filled.Flight
            "subscription", "streaming", "gym" -> Icons.Filled.Subscriptions
            "maintenance", "repair", "service" -> Icons.Filled.Build
            "savings", "fund" -> Icons.Filled.Savings
            "emergency fund" -> Icons.Filled.Shield
            "loan", "debt", "repayment", "loan paid" -> Icons.Filled.Payments
            "loan received", "loan repaid" -> Icons.Filled.AccountBalance
            "allowance" -> Icons.Filled.Wallet
            "lost" -> Icons.Filled.SearchOff
            "rent income" -> Icons.Filled.Apartment
            "bonus" -> Icons.Filled.Star
            else -> null
        }
    }
}
