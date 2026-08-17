package dev.frx.myampere.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BatteryRepositoryTest {
    private fun sample(ts: Long, ma: Int) =
        BatterySample(ts, ma, 80, 4000, 300, ChargeStatus.DISCHARGING)

    @Before fun reset() = BatteryRepository.resetForTest()

    @Test fun `latest reflete le dernier sample`() {
        BatteryRepository.onSample(sample(1L, -100))
        BatteryRepository.onSample(sample(2L, -200))
        assertEquals(-200, BatteryRepository.latest.value?.currentMa)
    }

    @Test fun `min max de session`() {
        listOf(-100, 300, -450).forEachIndexed { i, ma -> BatteryRepository.onSample(sample(i.toLong(), ma)) }
        assertEquals(-450, BatteryRepository.sessionMin.value)
        assertEquals(300, BatteryRepository.sessionMax.value)
    }

    @Test fun `drain vide le buffer et le rend`() {
        BatteryRepository.onSample(sample(1L, -100))
        BatteryRepository.onSample(sample(2L, -110))
        assertEquals(2, BatteryRepository.drainPendingForDb().size)
        assertTrue(BatteryRepository.drainPendingForDb().isEmpty())
    }

    @Test fun `recentWindow plafonne a 600`() {
        repeat(700) { BatteryRepository.onSample(sample(it.toLong(), -it)) }
        assertEquals(600, BatteryRepository.recentWindow().size)
        assertEquals(-699, BatteryRepository.recentWindow().last().currentMa)
    }
}
