package dev.frx.myampere.ui

import androidx.compose.foundation.layout.padding
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

@Composable
fun AppRoot() {
    var tab by remember { mutableIntStateOf(0) }
    MyAmpereTheme {
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
