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
    var tappedX by remember { mutableStateOf<Float?>(null) }

    val minV = remember(points) { if (points.isEmpty()) 0 else points.minOf { it.second }.coerceAtMost(0) }
    val maxV = remember(points) { if (points.isEmpty()) 1 else points.maxOf { it.second }.coerceAtLeast(0) }
    val span = remember(points) { (maxV - minV).coerceAtLeast(1) }
    val t0 = remember(points) { if (points.isEmpty()) 0L else points.first().first }
    val tSpan = remember(points) { if (points.size < 2) 1L else (points.last().first - points.first().first).coerceAtLeast(1) }

    Canvas(
        modifier.pointerInput(points) {
            detectTapGestures { offset ->
                if (points.size < 2 || onTapSlope == null) return@detectTapGestures
                val leftMargin = 80f
                val chartWidth = size.width - leftMargin
                val tapTs = t0 + ((offset.x - leftMargin) / chartWidth * tSpan).toLong()
                val idx = points.indexOfLast { it.first <= tapTs }.coerceIn(0, points.size - 2)
                val dVal = (points[idx + 1].second - points[idx].second).toFloat()
                val dMs = (points[idx + 1].first - points[idx].first).toFloat()
                val slopeMaPerMin = if (dMs > 0f) dVal / dMs * 60_000f else 0f
                tappedX = offset.x
                onTapSlope(slopeMaPerMin)
            }
        }
    ) {
        if (points.size < 2) return@Canvas
        val leftMargin = 80f
        val chartWidth = size.width - leftMargin
        fun yf(v: Float) = size.height * (1f - (v - minV) / span)
        fun y(v: Int) = yf(v.toFloat())

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

        val path = Path()
        points.forEachIndexed { i, (ts, ma) ->
            val x = leftMargin + chartWidth * (ts - t0).toFloat() / tSpan
            if (i == 0) path.moveTo(x, y(ma)) else path.lineTo(x, y(ma))
        }
        drawPath(path, color, style = Stroke(width = 2f))

        tappedX?.let { x ->
            val tapTs = t0 + ((x - leftMargin) / chartWidth * tSpan).toLong()
            val idx = points.indexOfLast { it.first <= tapTs }.coerceIn(0, points.size - 2)
            val dMs = (points[idx + 1].first - points[idx].first).toFloat()
            val t = if (dMs > 0f) (tapTs - points[idx].first).toFloat() / dMs else 0f
            val interpolatedVal = points[idx].second + (points[idx + 1].second - points[idx].second) * t
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f,
            )
            drawCircle(color = color, radius = 8f, center = Offset(x, yf(interpolatedVal)))
        }
    }
}
