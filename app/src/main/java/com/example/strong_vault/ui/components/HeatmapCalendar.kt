package com.example.strong_vault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A GitHub-style contribution grid: one column per week, one row per weekday, cell shading is a
 * single sequential hue (the theme's primary) at increasing alpha for increasing set count -
 * never a rainbow, since this only encodes magnitude, not identity.
 */
@Composable
fun HeatmapCalendar(counts: Map<LocalDate, Int>, weeks: Int, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)

    val today = LocalDate.now()
    val startOfThisWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val startDate = startOfThisWeek.minusWeeks((weeks - 1).toLong())

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val cellGap = 2.dp.toPx()
        val cellSize = (size.width / weeks) - cellGap
        val rowHeight = (size.height / 7f)

        for (week in 0 until weeks) {
            for (dayOfWeekIndex in 0 until 7) {
                val date = startDate.plusWeeks(week.toLong()).plusDays(dayOfWeekIndex.toLong())
                if (date.isAfter(today)) continue
                val count = counts[date] ?: 0
                val alpha = if (count == 0) 0f else (0.25f + 0.75f * (count.toFloat() / maxCount))
                val color = if (count == 0) emptyColor else activeColor.copy(alpha = alpha)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(week * (cellSize + cellGap), dayOfWeekIndex * rowHeight),
                    size = Size(cellSize, rowHeight - cellGap),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
        }
    }
}
