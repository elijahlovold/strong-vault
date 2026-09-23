package com.example.strong_vault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * A minimal single-series progression line: one hue, thin stroke, rounded caps, no gridlines
 * beyond a single recessive baseline - deliberately not a dual-axis or multi-series chart.
 */
@Composable
fun SimpleLineChart(values: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val topPadding = 12f
        val bottomPadding = 12f
        val chartHeight = size.height - topPadding - bottomPadding

        drawLine(
            color = baselineColor,
            start = Offset(0f, size.height - bottomPadding),
            end = Offset(size.width, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )

        if (values.size < 2) {
            if (values.size == 1) {
                drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f))
            }
            return@Canvas
        }

        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        fun pointFor(index: Int): Offset {
            val normalized = (values[index] - minValue) / range
            val y = topPadding + (1f - normalized) * chartHeight
            return Offset(index * stepX, y)
        }

        for (i in 0 until values.size - 1) {
            drawLine(
                color = lineColor,
                start = pointFor(i),
                end = pointFor(i + 1),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // Direct-label only the endpoints (first/last), not every point.
        drawCircle(lineColor, radius = 4.dp.toPx(), center = pointFor(0))
        drawCircle(lineColor, radius = 4.dp.toPx(), center = pointFor(values.size - 1))
    }
}
