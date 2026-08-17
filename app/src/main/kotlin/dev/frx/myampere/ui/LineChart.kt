package dev.frx.myampere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

@Composable
fun LineChart(
    points: List<Pair<Long, Int>>,
    modifier: Modifier = Modifier,
    color: Color = Palette.graphBlue,
    onTapSlope: ((Float) -> Unit)? = null,
) {
    var tappedIdx by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier.pointerInput(points) {
            detectTapGestures { offset ->
                if (points.size < 2 || onTapSlope == null) return@detectTapGestures
                val leftMargin = 80f
                val chartWidth = size.width - leftMargin
                val t0 = points.first().first
                val tSpan = (points.last().first - t0).coerceAtLeast(1)
                val tapTs = t0 + ((offset.x - leftMargin) / chartWidth * tSpan).toLong()
                val idx = points.indices
                    .minByOrNull { kotlin.math.abs(points[it].first - tapTs) }
                    ?.coerceIn(1, points.size - 2) ?: return@detectTapGestures
                val dVal = (points[idx + 1].second - points[idx - 1].second).toFloat()
                val dMs = (points[idx + 1].first - points[idx - 1].first).toFloat()
                val slopeMaPerMin = if (dMs > 0f) dVal / dMs * 60_000f else 0f
                tappedIdx = idx
                onTapSlope(slopeMaPerMin)
            }
        }
    ) {
        if (points.size < 2) return@Canvas
        val leftMargin = 80f
        val chartWidth = size.width - leftMargin
        val minV = points.minOf { it.second }.coerceAtMost(0)
        val maxV = points.maxOf { it.second }.coerceAtLeast(0)
        val span = (maxV - minV).coerceAtLeast(1)
        fun y(v: Int) = size.height * (1f - (v - minV).toFloat() / span)

        val labelPaint = android.graphics.Paint().apply {
            textSize = 28f
            this.color = Color.White.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.LEFT
        }
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
        val steps = 4
        for (i in 0..steps) {
            val v = minV + (maxV - minV) * i / steps
            val yPos = y(v)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(leftMargin, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
            drawContext.canvas.nativeCanvas.drawText(
                v.toString(),
                2f,
                yPos + labelPaint.textSize / 3f,
                labelPaint
            )
        }

        drawLine(Color.Gray, Offset(leftMargin, y(0)), Offset(size.width, y(0)), strokeWidth = 1f)

        val t0 = points.first().first
        val t1 = points.last().first
        val tSpan = (t1 - t0).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, (ts, ma) ->
            val x = leftMargin + chartWidth * (ts - t0).toFloat() / tSpan
            if (i == 0) path.moveTo(x, y(ma)) else path.lineTo(x, y(ma))
        }
        drawPath(path, color, style = Stroke(width = 2f))

        tappedIdx?.let { idx ->
            val (ts, ma) = points[idx]
            val x = leftMargin + chartWidth * (ts - t0).toFloat() / tSpan
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f,
            )
            drawCircle(color = color, radius = 8f, center = Offset(x, y(ma)))
        }
    }
}
