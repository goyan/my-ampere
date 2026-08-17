package dev.frx.myampere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.frx.myampere.core.downsampleForDisplay
import dev.frx.myampere.db.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pointsCurrent by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }
    var pointsLevel by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }
    var pointsVoltage by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val (current, level, voltage) = withContext(Dispatchers.Default) {
            val rows = AppDb.get(context).sampleDao().range(now - 24L * 3600 * 1000, now)
            Triple(
                downsampleForDisplay(rows.map { it.timestampMs to it.currentMa }, 300),
                downsampleForDisplay(rows.map { it.timestampMs to it.levelPct }, 300),
                downsampleForDisplay(rows.map { it.timestampMs to it.voltageMv }, 300),
            )
        }
        pointsCurrent = current
        pointsLevel = level
        pointsVoltage = voltage
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Dernières 24 h — ${pointsCurrent.size} points",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        ChartCard("Courant (mA)", MaterialTheme.colorScheme.primary) { color ->
            LineChart(pointsCurrent, Modifier.fillMaxWidth().height(220.dp), color)
        }
        ChartCard("Niveau (%)", MaterialTheme.colorScheme.secondary) { color ->
            LineChart(pointsLevel, Modifier.fillMaxWidth().height(180.dp), color)
        }
        ChartCard("Tension (mV)", MaterialTheme.colorScheme.tertiary) { color ->
            LineChart(pointsVoltage, Modifier.fillMaxWidth().height(180.dp), color)
        }
    }
}

@Composable
private fun ChartCard(title: String, titleColor: Color, chart: @Composable (Color) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
            Spacer(Modifier.height(8.dp))
            chart(titleColor)
        }
    }
}
