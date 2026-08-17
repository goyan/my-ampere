package dev.frx.myampere.core

enum class ChargeStatus { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

data class BatterySample(
    val timestampMs: Long,
    val currentMa: Int, // convention: charge > 0, décharge < 0
    val levelPct: Int,
    val voltageMv: Int,
    val tempDeciC: Int,
    val status: ChargeStatus,
)
