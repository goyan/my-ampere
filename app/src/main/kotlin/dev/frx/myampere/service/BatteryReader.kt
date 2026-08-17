package dev.frx.myampere.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.frx.myampere.core.BatterySample
import dev.frx.myampere.core.ChargeStatus
import dev.frx.myampere.core.normalizeCurrentMa

class BatteryReader(private val context: Context) {
    private val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    @Volatile private var hasRead = false
    @Volatile private var lastPlugged = false

    fun read(nowMs: Long): BatterySample? {
        val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val ma = normalizeCurrentMa(raw) ?: return null
        val sticky: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val voltage = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val status = when (sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargeStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeStatus.NOT_CHARGING
            else -> ChargeStatus.UNKNOWN
        }
        val health = when (sticky?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "bonne"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "surchauffe"
            BatteryManager.BATTERY_HEALTH_DEAD -> "morte"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "surtension"
            BatteryManager.BATTERY_HEALTH_COLD -> "froide"
            else -> "inconnue"
        }
        val technology = sticky?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        lastPlugged = (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        hasRead = true
        return BatterySample(nowMs, ma, pct, voltage, temp, status, health, technology)
    }

    /** Rend l'état "branché" dérivé de la dernière lecture (aucun nouvel appel binder).
     *  Fallback en lecture directe si aucun [read] n'a encore eu lieu. */
    fun isPlugged(): Boolean {
        if (!hasRead) {
            val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        }
        return lastPlugged
    }
}
