package dev.frx.myampere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.frx.myampere.core.BatteryRepository
import dev.frx.myampere.core.ChargeStatus
import dev.frx.myampere.core.Prefs
import dev.frx.myampere.core.statusLabel
import dev.frx.myampere.service.SamplerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiveScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sample by BatteryRepository.latest.collectAsStateWithLifecycle()
    val min by BatteryRepository.sessionMin.collectAsStateWithLifecycle()
    val max by BatteryRepository.sessionMax.collectAsStateWithLifecycle()
    val unsupported by BatteryRepository.unsupported.collectAsStateWithLifecycle()
    var running by remember { mutableStateOf(SamplerService.userEnabled) }

    LaunchedEffect(Unit) {
        running = withContext(Dispatchers.IO) { Prefs.userEnabled(context) }
    }

    val ma = sample?.currentMa
    val currentColor = when {
        unsupported -> MaterialTheme.colorScheme.outline
        (ma ?: 0) > 0 -> MaterialTheme.colorScheme.primary
        (ma ?: 0) < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Monitoring", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = running, onCheckedChange = {
                    running = it
                    SamplerService.setUserEnabled(context, it)
                })
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when {
                        unsupported -> "mesure non supportée sur cet appareil"
                        ma != null -> "$ma mA"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = currentColor,
                )
                Text(
                    statusLabel(sample?.status ?: ChargeStatus.UNKNOWN),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "min ${min ?: "—"} mA   max ${max ?: "—"} mA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            LiveGraph(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(8.dp)
            )
        }

        sample?.let { s ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        DetailCell("Tension", "${s.voltageMv} mV")
                        DetailCell("Niveau", "${s.levelPct} %")
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        DetailCell("Température", "${s.tempDeciC / 10.0} °C")
                        DetailCell("Santé", s.health)
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Technologie", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    SuggestionChip(onClick = {}, label = { Text(s.technology) })
                }
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
