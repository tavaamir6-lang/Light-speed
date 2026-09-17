package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.PingBad
import com.example.ui.theme.PingGood
import com.example.ui.theme.PingMedium
import com.example.ui.theme.PingUntested

@Composable
fun PingBadge(pingMs: Int, modifier: Modifier = Modifier) {
    val (color, text) = when {
        pingMs < 0 && pingMs != -2 -> PingUntested to "—"
        pingMs == -2 -> PingBad to "Timeout"
        pingMs < 180 -> PingGood to "${pingMs}ms"
        pingMs < 450 -> PingMedium to "${pingMs}ms"
        else -> PingBad to "${pingMs}ms"
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = color
        )
    }
}
