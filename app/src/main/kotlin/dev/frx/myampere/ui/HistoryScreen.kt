package dev.frx.myampere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.frx.myampere.db.AppDb

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var points by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val rows = AppDb.get(context).sampleDao().range(now - 24L * 3600 * 1000, now)
        points = downsampleForDisplay(rows.map { it.timestampMs to it.currentMa }, 300)
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Dernières 24 h — ${points.size} points")
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
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
            drawPath(path, Color(0xFF1565C0), style = Stroke(width = 2f))
        }
    }
}
