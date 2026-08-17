package dev.frx.myampere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** Graphe (timestamp, courant en mA) partagé par LiveGraph et HistoryScreen :
 *  min/max clampés à 0, ligne zéro grise, tracé bleu. */
@Composable
fun LineChart(points: List<Pair<Long, Int>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val minV = points.minOf { it.second }.coerceAtMost(0)
        val maxV = points.maxOf { it.second }.coerceAtLeast(0)
        val span = (maxV - minV).coerceAtLeast(1)
        fun y(ma: Int) = size.height * (1f - (ma - minV).toFloat() / span)
        drawLine(Color.Gray, Offset(0f, y(0)), Offset(size.width, y(0)), strokeWidth = 1f)
        val t0 = points.first().first
        val t1 = points.last().first
        val tSpan = (t1 - t0).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, (ts, ma) ->
            val x = size.width * (ts - t0).toFloat() / tSpan
            if (i == 0) path.moveTo(x, y(ma)) else path.lineTo(x, y(ma))
        }
        drawPath(path, Palette.graphBlue, style = Stroke(width = 2f))
    }
}
