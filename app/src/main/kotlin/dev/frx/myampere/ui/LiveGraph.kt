package dev.frx.myampere.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.frx.myampere.core.BatteryRepository

@Composable
fun LiveGraph(modifier: Modifier = Modifier) {
    BatteryRepository.latest.collectAsStateWithLifecycle().value // déclenche la recomposition
    val points = BatteryRepository.recentWindow().map { it.timestampMs to it.currentMa }
    LineChart(points, modifier)
}
