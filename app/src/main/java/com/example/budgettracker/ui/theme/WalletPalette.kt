package com.example.budgettracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

object WalletPalette {
    val colors = listOf(
        EmeraldGreen,
        CatSoftBlue,
        CatAmber,
        CatCoral,
        CatTeal,
        CatIndigo,
        CatPurple,
        OceanBlue,
        SunsetOrange
    )

    fun argbAt(index: Int): Int = colors[index.mod(colors.size)].toArgb()

    /** Black on light swatches, white on the rest. */
    fun contentOn(argb: Int): Color = if (Color(argb).luminance() > 0.55f) Color.Black else Color.White
}
