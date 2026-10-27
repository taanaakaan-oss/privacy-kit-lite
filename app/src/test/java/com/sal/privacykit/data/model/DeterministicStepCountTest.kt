package com.sal.privacykit.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeterministicStepCountTest {
    @Test
    fun sameProfileAndDateProducesSameValue() {
        val date = LocalDate.of(2026, 10, 27)
        assertEquals(
            DeterministicStepCount.forProfileDate(42L, date),
            DeterministicStepCount.forProfileDate(42L, date),
        )
    }

    @Test
    fun generatedValueIsWithinConfiguredRange() {
        val value = DeterministicStepCount.forProfileDate(
            profileId = 42L,
            date = LocalDate.of(2026, 10, 27),
            min = 12000L,
            max = 15000L,
        )
        assertTrue(value in 12000L..15000L)
    }

    @Test
    fun equalBoundsReturnThatBound() {
        assertEquals(
            12345L,
            DeterministicStepCount.forProfileDate(42L, LocalDate.of(2026, 10, 27), 12345L, 12345L),
        )
    }
}
