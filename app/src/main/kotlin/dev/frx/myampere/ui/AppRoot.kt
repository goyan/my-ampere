package dev.frx.myampere.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

fun downsampleForDisplay(samples: List<Pair<Long, Int>>, maxPoints: Int): List<Pair<Long, Int>> {
    if (samples.size <= maxPoints) return samples
    val step = samples.size / maxPoints
    return samples.filterIndexed { i, _ -> i % step == 0 }.take(maxPoints)
}

@Composable
fun AppRoot() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Scaffold(topBar = {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Live") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historique") })
            }
        }) { padding ->
            when (tab) {
                0 -> LiveScreen(Modifier.padding(padding))
                1 -> HistoryScreen(Modifier.padding(padding))
            }
        }
    }
}
