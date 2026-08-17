package dev.frx.myampere.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DownsampleTest {
    @Test fun `moins que maxPoints = inchange`() {
        val pts = (0L..10L).map { it to it.toInt() }
        assertEquals(pts, downsampleForDisplay(pts, 100))
    }
    @Test fun `reduit a maxPoints environ par pas entier`() {
        val pts = (0L until 1000L).map { it to it.toInt() }
        val out = downsampleForDisplay(pts, 200)
        assertEquals(200, out.size)
        assertEquals(0L, out.first().first)
        assertEquals(999L, out.last().first)
    }
    @Test fun `conserve le dernier point`() {
        val pts = (0L until 1000L).map { it to it.toInt() }
        val out = downsampleForDisplay(pts, 200)
        assertEquals(200, out.size)
        assertEquals(0L, out.first().first)
        assertEquals(999L, out.last().first)
    }
    @Test fun `bande pathologique size entre max et 2max`() {
        val pts = (0L until 400L).map { it to it.toInt() }
        val out = downsampleForDisplay(pts, 300)
        assertEquals(300, out.size)
        assertEquals(399L, out.last().first)
    }
    @Test fun `garde maxPoints egal a 1`() {
        val pts = (0L until 400L).map { it to it.toInt() }
        assertEquals(listOf(399L to 399), downsampleForDisplay(pts, 1))
    }
    @Test fun `garde maxPoints egal a 0`() {
        val pts = (0L until 400L).map { it to it.toInt() }
        assertEquals(emptyList<Pair<Long, Int>>(), downsampleForDisplay(pts, 0))
    }
}
