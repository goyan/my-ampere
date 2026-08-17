package dev.frx.myampere.core

data class SamplingConditions(
    val screenOn: Boolean,
    val plugged: Boolean,
    val appVisible: Boolean,
)

const val INTERVAL_APP_MS = 1_000L
const val INTERVAL_WIDGET_MS = 5_000L
const val INTERVAL_CHARGING_OFF_MS = 30_000L

fun samplingIntervalMs(c: SamplingConditions): Long? = when {
    !c.screenOn && !c.plugged -> null
    !c.screenOn -> INTERVAL_CHARGING_OFF_MS
    c.appVisible -> INTERVAL_APP_MS
    else -> INTERVAL_WIDGET_MS
}
