package dev.frx.myampere.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SampleEntity>)

    @Query("SELECT * FROM samples WHERE timestampMs BETWEEN :fromMs AND :toMs ORDER BY timestampMs")
    suspend fun range(fromMs: Long, toMs: Long): List<SampleEntity>

    @Query("SELECT timestampMs FROM samples WHERE timestampMs BETWEEN :fromMs AND :toMs ORDER BY timestampMs")
    suspend fun timestampsBetween(fromMs: Long, toMs: Long): List<Long>

    @Query("DELETE FROM samples WHERE timestampMs IN (:ts)")
    suspend fun deleteByTimestamps(ts: List<Long>)

    @Query("DELETE FROM samples WHERE timestampMs < :cutoffMs")
    suspend fun deleteBefore(cutoffMs: Long)
}
