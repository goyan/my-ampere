package dev.frx.myampere.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.frx.myampere.core.BatterySample

@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey val timestampMs: Long,
    val currentMa: Int,
    val levelPct: Int,
)

fun BatterySample.toEntity() = SampleEntity(timestampMs, currentMa, levelPct)
