package dev.frx.myampere.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizeTest {
    @Test fun `mA passe tel quel`() { assertEquals(510, normalizeCurrentMa(510)) }
    @Test fun `decharge mA negative passe telle quelle`() { assertEquals(-140, normalizeCurrentMa(-140)) }
    @Test fun `microamperes convertis en mA`() { assertEquals(1500, normalizeCurrentMa(1_500_000)) }
    @Test fun `microamperes negatifs convertis`() { assertEquals(-250, normalizeCurrentMa(-250_000)) }
    @Test fun `MIN_VALUE non supporte`() { assertNull(normalizeCurrentMa(Int.MIN_VALUE)) }
    @Test fun `zero est une valeur valide`() { assertEquals(0, normalizeCurrentMa(0)) }
}
