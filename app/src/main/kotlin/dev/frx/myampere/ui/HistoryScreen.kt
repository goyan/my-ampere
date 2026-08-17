package dev.frx.myampere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.frx.myampere.core.downsampleForDisplay
import dev.frx.myampere.db.AppDb
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pointsCurrent by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }
    var pointsLevel by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }
    var pointsVoltage by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }
    var lastCurrentMa by remember { mutableStateOf<Int?>(null) }
    var lastLevelPct by remember { mutableStateOf<Int?>(null) }
    var lastVoltageMv by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        data class Result(
            val current: List<Pair<Long, Int>>,
            val level: List<Pair<Long, Int>>,
            val voltage: List<Pair<Long, Int>>,
            val lastMa: Int?,
            val lastPct: Int?,
            val lastMv: Int?,
        )
        val r = withContext(Dispatchers.Default) {
            val rows = AppDb.get(context).sampleDao().range(now - 24L * 3600 * 1000, now)
            val last = rows.lastOrNull()
            Result(
                current = downsampleForDisplay(rows.map { it.timestampMs to it.currentMa }, 300),
                level = downsampleForDisplay(rows.map { it.timestampMs to it.levelPct }, 300),
                voltage = downsampleForDisplay(rows.map { it.timestampMs to it.voltageMv }, 300),
                lastMa = last?.currentMa,
                lastPct = last?.levelPct,
                lastMv = last?.voltageMv,
            )
        }
        pointsCurrent = r.current
        pointsLevel = r.level
        pointsVoltage = r.voltage
        lastCurrentMa = r.lastMa
        lastLevelPct = r.lastPct
        lastVoltageMv = r.lastMv
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Dernières 24 h — ${pointsCurrent.size} points",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        val initialSlope: String? = remember(pointsCurrent) {
            if (pointsCurrent.size >= 2) {
                val i = pointsCurrent.size - 2
                val dVal = (pointsCurrent[i + 1].second - pointsCurrent[i].second).toFloat()
                val dMs  = (pointsCurrent[i + 1].first  - pointsCurrent[i].first).toFloat()
                val slope = if (dMs > 0f) dVal / dMs * 60_000f else 0f
                "Pente : ${slope.roundToInt()} mA/min"
            } else null
        }
        var slopeText by remember(pointsCurrent) { mutableStateOf(initialSlope) }
        ChartCard("Courant (mA)", MaterialTheme.colorScheme.primary, lastCurrentMa?.let { "$it mA" }, Modifier.weight(1f)) { color ->
            LineChart(
                pointsCurrent,
                Modifier.fillMaxWidth().weight(1f),
                color,
                onTapSlope = { slope -> slopeText = "Pente : ${slope.roundToInt()} mA/min" },
            )
            slopeText?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        ChartCard("Niveau (%)", MaterialTheme.colorScheme.secondary, lastLevelPct?.let { "$it %" }, Modifier.weight(1f)) { color ->
            LineChart(pointsLevel, Modifier.fillMaxWidth().weight(1f), color)
        }
        ChartCard("Tension (mV)", MaterialTheme.colorScheme.tertiary, lastVoltageMv?.let { "$it mV" }, Modifier.weight(1f)) { color ->
            LineChart(pointsVoltage, Modifier.fillMaxWidth().weight(1f), color)
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    titleColor: Color,
    currentValue: String? = null,
    modifier: Modifier = Modifier,
    chart: @Composable (Color) -> Unit,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
                if (currentValue != null) {
                    Text(
                        currentValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            chart(titleColor)
        }
    }
}
