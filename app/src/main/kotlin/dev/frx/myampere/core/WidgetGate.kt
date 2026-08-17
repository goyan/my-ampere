package dev.frx.myampere.core

const val WIDGET_DELTA_MA = 50
const val WIDGET_MAX_AGE_MS = 10_000L

data class WidgetGateState(val lastPushedMa: Int?, val lastPushMs: Long)

fun shouldPushWidget(state: WidgetGateState, currentMa: Int, nowMs: Long, screenOn: Boolean): Boolean {
    if (!screenOn) return false
    val last = state.lastPushedMa ?: return true
    val flipped = (last >= 0) != (currentMa >= 0)
    val bigDelta = kotlin.math.abs(currentMa - last) >= WIDGET_DELTA_MA
    val stale = nowMs - state.lastPushMs >= WIDGET_MAX_AGE_MS
    return flipped || bigDelta || stale
}
