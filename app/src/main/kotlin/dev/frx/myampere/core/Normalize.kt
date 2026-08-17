package dev.frx.myampere.core

// Sonde SM-G985F 2026-08-17 : ce device rend des mA (API documente µA)
private const val MICROAMP_THRESHOLD = 10_000

fun normalizeCurrentMa(raw: Int): Int? = when {
    raw == Int.MIN_VALUE -> null
    kotlin.math.abs(raw) > MICROAMP_THRESHOLD -> raw / 1000
    else -> raw
}
