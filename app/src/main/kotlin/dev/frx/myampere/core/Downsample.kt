package dev.frx.myampere.core

fun downsampleForDisplay(samples: List<Pair<Long, Int>>, maxPoints: Int): List<Pair<Long, Int>> {
    if (maxPoints <= 1) return samples.takeLast(maxPoints.coerceAtLeast(0))
    if (samples.size <= maxPoints) return samples
    return List(maxPoints) { i -> samples[i * (samples.size - 1) / (maxPoints - 1)] }
}
