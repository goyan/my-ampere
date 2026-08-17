package dev.frx.myampere.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryWindowTest {
    @Test fun `moins que maxPoints = inchange`() {
        val pts = (0L..10L).map { it to it.toInt() }
        assertEquals(pts, downsampleForDisplay(pts, 100))
    }
    @Test fun `reduit a maxPoints environ par pas entier`() {
        val pts = (0L until 1000L).map { it to it.toInt() }
        val out = downsampleForDisplay(pts, 200)
        assertEquals(200, out.size)
        assertEquals(0L, out.first().first)
    }
}
