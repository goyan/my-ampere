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
import dev.frx.myampere.service.SamplerService

private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

@Composable
fun LiveScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sample by BatteryRepository.latest.collectAsStateWithLifecycle()
    val min by BatteryRepository.sessionMin.collectAsStateWithLifecycle()
    val max by BatteryRepository.sessionMax.collectAsStateWithLifecycle()
    var running by remember { mutableStateOf(SamplerService.userEnabled) }

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
                SamplerService.userEnabled = it
                if (it) SamplerService.start(context) else SamplerService.stop(context)
            })
        }
        val ma = sample?.currentMa
        Text(
            text = if (ma != null) "$ma mA" else "—",
            fontSize = 56.sp,
            color = if ((ma ?: 0) >= 0) Green else Red,
        )
        Text(when (sample?.status) {
            ChargeStatus.CHARGING -> "en charge"
            ChargeStatus.FULL -> "batterie pleine"
            ChargeStatus.DISCHARGING -> "en décharge"
            ChargeStatus.NOT_CHARGING -> "branché, pas de charge"
            else -> "en attente de mesure…"
        })
        Text("min ${min ?: "—"} mA   max ${max ?: "—"} mA")
        Spacer(Modifier.height(16.dp))
        LiveGraph(Modifier.fillMaxWidth().height(160.dp))
        Spacer(Modifier.height(16.dp))
        sample?.let {
            Text("Tension ${it.voltageMv} mV — Temp ${it.tempDeciC / 10.0} °C — Niveau ${it.levelPct} %")
        }
    }
}
