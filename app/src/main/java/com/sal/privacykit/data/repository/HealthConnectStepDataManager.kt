package com.sal.privacykit.data.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.deleteRecords
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import com.sal.privacykit.data.model.DeterministicStepCount
import com.sal.privacykit.data.prefs.AppPreferences
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Replaces PrivacyKit Lite's own recent step records when the active profile changes.
 *
 * Health Connect intentionally limits time-range deletion to records owned by this app.
 * Records written by other apps are not removed.
 */
class HealthConnectStepDataManager(
    context: Context,
    private val appPreferences: AppPreferences,
) {
    private val appContext = context.applicationContext

    suspend fun replaceRecentSteps(profileId: Long) {
        if (profileId <= 0L) return

        if (HealthConnectClient.getSdkStatus(appContext) != HealthConnectClient.SDK_AVAILABLE) {
            Log.i(TAG, "Health Connect is not available; skipping step record replacement")
            return
        }

        val client = HealthConnectClient.getOrCreate(appContext)
        val writePermission = HealthPermission.getWritePermission(StepsRecord::class)
        if (writePermission !in client.permissionController.getGrantedPermissions()) {
            Log.w(TAG, "WRITE_STEPS permission is not granted; skipping step record replacement")
            return
        }

        val now = Instant.now()
        val deleteStart = now.minus(DELETE_WINDOW)
        client.deleteRecords<StepsRecord>(
            timeRangeFilter = TimeRangeFilter.between(deleteStart, now),
        )

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val minSteps = appPreferences.stepCountMin.first()
        val maxSteps = appPreferences.stepCountMax.first()

        val todayStart = today.atStartOfDay(zone).toInstant()
        val todayEnd = maxOf(now, todayStart.plusSeconds(1))
        val yesterdayStart = yesterday.atTime(12, 0).atZone(zone).toInstant()
        val yesterdayEnd = yesterdayStart.plusSeconds(60)

        val records = listOf(
            createRecord(profileId, today, todayStart, todayEnd, zone, minSteps, maxSteps),
            createRecord(profileId, yesterday, yesterdayStart, yesterdayEnd, zone, minSteps, maxSteps),
        )

        client.insertRecords(records)
        Log.i(TAG, "Replaced recent step records for profile=$profileId, dates=$yesterday,$today")
    }

    private fun createRecord(
        profileId: Long,
        date: LocalDate,
        startTime: Instant,
        endTime: Instant,
        zone: ZoneId,
        minSteps: Long,
        maxSteps: Long,
    ): StepsRecord {
        val count = DeterministicStepCount.forProfileDate(
            profileId = profileId,
            date = date,
            min = minSteps,
            max = maxSteps,
        )
        return StepsRecord(
            count = count,
            startTime = startTime,
            endTime = endTime,
            startZoneOffset = startTime.atZone(zone).offset,
            endZoneOffset = endTime.atZone(zone).offset,
            metadata = Metadata.manualEntry(),
        )
    }

    companion object {
        private const val TAG = "HealthConnectStepData"
        private val DELETE_WINDOW: Duration = Duration.ofHours(36)
    }
}
