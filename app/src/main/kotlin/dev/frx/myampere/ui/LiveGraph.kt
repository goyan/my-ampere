package dev.frx.myampere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.frx.myampere.core.BatteryRepository

@Composable
fun LiveGraph(modifier: Modifier = Modifier) {
    val tick by BatteryRepository.latest.collectAsStateWithLifecycle() // déclenche la recomposition
    Canvas(modifier) {
        val data = BatteryRepository.recentWindow()
        if (tick == null || data.size < 2) return@Canvas
        val minV = data.minOf { it.currentMa }.coerceAtMost(0)
        val maxV = data.maxOf { it.currentMa }.coerceAtLeast(0)
        val span = (maxV - minV).coerceAtLeast(1)
        fun y(ma: Int) = size.height * (1f - (ma - minV).toFloat() / span)
        // ligne zéro
        drawLine(Color.Gray, Offset(0f, y(0)), Offset(size.width, y(0)), strokeWidth = 1f)
        val stepX = size.width / (data.size - 1)
        val path = Path()
        data.forEachIndexed { i, s ->
            val x = i * stepX
            if (i == 0) path.moveTo(x, y(s.currentMa)) else path.lineTo(x, y(s.currentMa))
        }
        drawPath(path, Color(0xFF1565C0), style = Stroke(width = 3f))
    }
}
