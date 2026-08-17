package dev.frx.myampere.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetGateTest {
    private val base = WidgetGateState(lastPushedMa = 200, lastPushMs = 0L)

    @Test fun `ecran off = jamais`() = assertFalse(shouldPushWidget(base, 900, 1_000L, screenOn = false))
    @Test fun `premier push toujours`() =
        assertTrue(shouldPushWidget(WidgetGateState(null, 0L), 100, 1_000L, screenOn = true))
    @Test fun `delta sous seuil et recent = non`() = assertFalse(shouldPushWidget(base, 230, 5_000L, screenOn = true))
    @Test fun `delta 50 mA et plus = oui`() = assertTrue(shouldPushWidget(base, 250, 5_000L, screenOn = true))
    @Test fun `flip de signe = oui meme petit delta`() = assertTrue(shouldPushWidget(base, -10, 1_000L, screenOn = true))
    @Test fun `10s ecoulees = oui meme sans delta`() = assertTrue(shouldPushWidget(base, 200, 10_000L, screenOn = true))
}
