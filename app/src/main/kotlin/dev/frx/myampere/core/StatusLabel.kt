package dev.frx.myampere.core

fun statusLabel(status: ChargeStatus): String = when (status) {
    ChargeStatus.CHARGING -> "en charge"
    ChargeStatus.FULL -> "batterie pleine"
    ChargeStatus.DISCHARGING -> "en décharge"
    ChargeStatus.NOT_CHARGING -> "branché, pas de charge"
    ChargeStatus.UNKNOWN -> "en attente de mesure…"
}

/** Variante compacte pour le widget 1x1 (le libellé long ne tient pas dans une cellule). */
fun statusLabelShort(status: ChargeStatus): String = when (status) {
    ChargeStatus.CHARGING -> "charge"
    ChargeStatus.FULL -> "plein"
    ChargeStatus.DISCHARGING -> "décharge"
    ChargeStatus.NOT_CHARGING -> "branché"
    ChargeStatus.UNKNOWN -> "—"
}
