package dev.frx.myampere.core

fun statusLabel(status: ChargeStatus): String = when (status) {
    ChargeStatus.CHARGING -> "en charge"
    ChargeStatus.FULL -> "batterie pleine"
    ChargeStatus.DISCHARGING -> "en décharge"
    ChargeStatus.NOT_CHARGING -> "branché, pas de charge"
    ChargeStatus.UNKNOWN -> "en attente de mesure…"
}
