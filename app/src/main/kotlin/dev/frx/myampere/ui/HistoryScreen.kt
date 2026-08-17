package dev.frx.myampere.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.frx.myampere.core.downsampleForDisplay
import dev.frx.myampere.db.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var points by remember { mutableStateOf<List<Pair<Long, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        points = withContext(Dispatchers.Default) {
            val rows = AppDb.get(context).sampleDao().range(now - 24L * 3600 * 1000, now)
            downsampleForDisplay(rows.map { it.timestampMs to it.currentMa }, 300)
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Dernières 24 h — ${points.size} points")
        LineChart(points, Modifier.fillMaxWidth().height(220.dp))
    }
}
