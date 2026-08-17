package dev.frx.myampere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Monitoring")
            Spacer(Modifier.height(0.dp))
            Switch(checked = running, onCheckedChange = {
                running = it
                SamplerService.setUserEnabled(context, it)
            })
        }
        val ma = sample?.currentMa
        Text(
            text = when {
                unsupported -> "mesure non supportée sur cet appareil"
                ma != null -> "$ma mA"
                else -> "—"
            },
            fontSize = if (unsupported) 20.sp else 56.sp,
            color = when {
                unsupported -> Color.Gray
                (ma ?: 0) >= 0 -> Palette.chargeGreen
                else -> Palette.dischargeRed
            },
        )
        Text(statusLabel(sample?.status ?: ChargeStatus.UNKNOWN))
        Text("min ${min ?: "—"} mA   max ${max ?: "—"} mA")
        Spacer(Modifier.height(16.dp))
        LiveGraph(Modifier.fillMaxWidth().height(160.dp))
        Spacer(Modifier.height(16.dp))
        sample?.let {
            Text("Tension ${it.voltageMv} mV — Temp ${it.tempDeciC / 10.0} °C — Niveau ${it.levelPct} %")
            Text("Santé ${it.health} — Technologie ${it.technology}")
        }
    }
}
