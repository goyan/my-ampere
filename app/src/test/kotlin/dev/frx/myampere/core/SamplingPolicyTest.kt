package dev.frx.myampere.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SamplingPolicyTest {
    @Test fun `app visible = 1s`() =
        assertEquals(1_000L, samplingIntervalMs(SamplingConditions(screenOn = true, plugged = false, appVisible = true)))
    @Test fun `widget seul ecran on = 5s`() =
        assertEquals(5_000L, samplingIntervalMs(SamplingConditions(screenOn = true, plugged = true, appVisible = false)))
    @Test fun `ecran off en decharge = stop`() =
        assertNull(samplingIntervalMs(SamplingConditions(screenOn = false, plugged = false, appVisible = false)))
    @Test fun `ecran off en charge = 30s`() =
        assertEquals(30_000L, samplingIntervalMs(SamplingConditions(screenOn = false, plugged = true, appVisible = false)))
    @Test fun `app visible prime sur branche`() =
        assertEquals(1_000L, samplingIntervalMs(SamplingConditions(screenOn = true, plugged = true, appVisible = true)))
    @Test fun `ecran off ignore appVisible residuel`() =
        assertNull(samplingIntervalMs(SamplingConditions(screenOn = false, plugged = false, appVisible = true)))
}
