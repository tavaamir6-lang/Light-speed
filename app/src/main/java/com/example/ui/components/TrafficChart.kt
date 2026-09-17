package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrafficStat
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.max

@Composable
fun TrafficChart(
    stats: List<TrafficStat>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDarkSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Consumption",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(NeonCyan, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.size(8.dp).background(NeonEmerald, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (stats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No traffic recorded yet. Connect to VPN to track usage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            val chartStats = stats.take(7).reversed()
            val maxBytes = max(1024L * 1024L, chartStats.maxOfOrNull { it.downloadBytes + it.uploadBytes } ?: 1L)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 30f // bottom reserve for labels

                // Draw background horizontal gridlines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = canvasHeight * (i.toFloat() / gridLines)
                    drawLine(
                        color = CyberCardBorder.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }

                val barSlotWidth = canvasWidth / chartStats.size
                val barWidth = (barSlotWidth * 0.45f).coerceAtMost(32.dp.toPx())

                chartStats.forEachIndexed { index, stat ->
                    val centerX = (index * barSlotWidth) + (barSlotWidth / 2)
                    val left = centerX - (barWidth / 2)

                    val total = stat.downloadBytes + stat.uploadBytes
                    val totalHeight = (total.toFloat() / maxBytes.toFloat() * canvasHeight).coerceAtLeast(4f)
                    val downRatio = if (total > 0) stat.downloadBytes.toFloat() / total.toFloat() else 0.5f
                    val downHeight = totalHeight * downRatio
                    val upHeight = totalHeight - downHeight

                    val topY = canvasHeight - totalHeight

                    // Download bar portion (NeonCyan)
                    if (downHeight > 0) {
                        drawRoundRect(
                            color = NeonCyan,
                            topLeft = Offset(left, canvasHeight - downHeight),
                            size = Size(barWidth, downHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Upload bar portion (NeonEmerald on top)
                    if (upHeight > 0) {
                        drawRoundRect(
                            color = NeonEmerald,
                            topLeft = Offset(left, topY),
                            size = Size(barWidth, upHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Date label at bottom
                    val shortDate = if (stat.dateString.length >= 5) {
                        stat.dateString.takeLast(5)
                    } else {
                        stat.dateString
                    }
                    val textLayout = textMeasurer.measure(
                        text = shortDate,
                        style = TextStyle(color = TextMuted, fontSize = 9.sp)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(centerX - (textLayout.size.width / 2), canvasHeight + 8f)
                    )
                }
            }
        }
    }
}
