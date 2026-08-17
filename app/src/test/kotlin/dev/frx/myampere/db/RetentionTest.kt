package dev.frx.myampere.db

import org.junit.Assert.assertEquals
import org.junit.Test

class RetentionTest {
    @Test fun `garde le premier de chaque bucket`() {
        val ts = listOf(0L, 10_000L, 59_000L, 60_000L, 61_000L, 125_000L)
        // buckets 60s: [0,10k,59k] -> garde 0 ; [60k,61k] -> garde 60k ; [125k] -> garde 125k
        assertEquals(listOf(10_000L, 59_000L, 61_000L), selectDownsampleDeletions(ts, 60_000L))
    }
    @Test fun `liste vide = rien`() = assertEquals(emptyList<Long>(), selectDownsampleDeletions(emptyList(), 60_000L))
    @Test fun `deja downsample = rien a supprimer`() =
        assertEquals(emptyList<Long>(), selectDownsampleDeletions(listOf(0L, 60_000L, 120_000L), 60_000L))
}
