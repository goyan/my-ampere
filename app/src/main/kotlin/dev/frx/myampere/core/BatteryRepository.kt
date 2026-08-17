package dev.frx.myampere.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BatteryRepository {
    private const val RECENT_CAPACITY = 600 // 10 min à 1 s

    /** Lectures consécutives nulles ou à 0 mA avant de considérer la mesure non supportée. */
    private const val UNSUPPORTED_THRESHOLD = 30

    private val _latest = MutableStateFlow<BatterySample?>(null)
    val latest: StateFlow<BatterySample?> = _latest.asStateFlow()

    private val _sessionMin = MutableStateFlow<Int?>(null)
    val sessionMin: StateFlow<Int?> = _sessionMin.asStateFlow()
    private val _sessionMax = MutableStateFlow<Int?>(null)
    val sessionMax: StateFlow<Int?> = _sessionMax.asStateFlow()

    private val _unsupported = MutableStateFlow(false)
    val unsupported: StateFlow<Boolean> = _unsupported.asStateFlow()
    private var consecutiveUnsupported = 0

    private val recent = ArrayDeque<BatterySample>(RECENT_CAPACITY)
    private var pendingDb = mutableListOf<BatterySample>()

    @Synchronized
    fun onSample(sample: BatterySample) {
        consecutiveUnsupported = if (sample.currentMa == 0) consecutiveUnsupported + 1 else 0
        _unsupported.value = consecutiveUnsupported >= UNSUPPORTED_THRESHOLD
        _latest.value = sample
        _sessionMin.value = minOf(_sessionMin.value ?: sample.currentMa, sample.currentMa)
        _sessionMax.value = maxOf(_sessionMax.value ?: sample.currentMa, sample.currentMa)
        if (recent.size == RECENT_CAPACITY) recent.removeFirst()
        recent.addLast(sample)
        pendingDb.add(sample)
    }

    /** read() de BatteryReader a rendu null (courant illisible) : compte comme un tick non supporté. */
    @Synchronized
    fun onUnsupportedSample() {
        consecutiveUnsupported += 1
        _unsupported.value = consecutiveUnsupported >= UNSUPPORTED_THRESHOLD
    }

    @Synchronized fun recentWindow(): List<BatterySample> = recent.toList()

    @Synchronized
    fun drainPendingForDb(): List<BatterySample> {
        val out = pendingDb
        pendingDb = mutableListOf()
        return out
    }

    @Synchronized
    fun resetForTest() {
        _latest.value = null; _sessionMin.value = null; _sessionMax.value = null
        _unsupported.value = false; consecutiveUnsupported = 0
        recent.clear(); pendingDb = mutableListOf()
    }
}
