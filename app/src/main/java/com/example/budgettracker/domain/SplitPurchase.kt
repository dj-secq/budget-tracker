package com.example.budgettracker.domain

/** True when every part is a positive share and the parts add up to [totalCentavos]. */
fun splitPartsMatch(totalCentavos: Long, partCentavos: List<Long>): Boolean {
    return totalCentavos > 0L &&
        partCentavos.size >= 2 &&
        partCentavos.all { it > 0L } &&
        partCentavos.sum() == totalCentavos
}
