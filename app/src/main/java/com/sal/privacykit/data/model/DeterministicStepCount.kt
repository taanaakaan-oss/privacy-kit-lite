package com.sal.privacykit.data.model

import java.time.LocalDate
import java.util.Random

/**
 * Produces stable synthetic step totals for UI and integration testing only.
 * This helper does not write to Health Connect or alter real aggregation results.
 */
object DeterministicStepCount {
    fun forProfileDate(profileId: Long, date: LocalDate, min: Long = 12000L, max: Long = 15000L): Long {
        require(profileId >= 0L) { "Profile ID must be non-negative" }
        require(min >= 0L) { "Minimum steps must be non-negative" }
        require(max >= min) { "Maximum steps must be greater than or equal to minimum steps" }
        require(max <= Int.MAX_VALUE.toLong()) { "Maximum steps is too large" }

        val seedInput = "$profileId|$date"
        var seed = -3750763034362895579L // FNV-1a offset basis
        seedInput.toByteArray(Charsets.UTF_8).forEach { byte ->
            seed = (seed xor (byte.toLong() and 0xffL)) * 1099511628211L
        }
        val width = max - min + 1L
        val offset = if (width == 1L) 0L else Random(seed).nextInt(width.toInt()).toLong()
        return min + offset
    }
}
