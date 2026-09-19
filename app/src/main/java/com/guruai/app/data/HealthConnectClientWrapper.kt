package com.guruai.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HealthConnectClientWrapper {
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(PERMISSIONS)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTodaySummary(context: Context): String {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val now = Instant.now()
            val range = TimeRangeFilter.between(startOfDay, now)

            val steps = try {
                val response = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))
                response[StepsRecord.COUNT_TOTAL] ?: 0L
            } catch (e: Exception) {
                0L
            }

            val calories = try {
                val response = client.aggregate(AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), range))
                response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }

            val lastHeartRate = try {
                val records = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(now.minus(Duration.ofHours(6)), now))
                ).records
                records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute
            } catch (e: Exception) {
                null
            }

            val sleepHours = try {
                val yesterdayStart = startOfDay.minus(Duration.ofHours(18))
                val records = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(yesterdayStart, now))
                ).records
                val totalMinutes = records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                if (totalMinutes > 0) totalMinutes / 60.0 else null
            } catch (e: Exception) {
                null
            }

            val sb = StringBuilder()
            sb.append("Aaj ke health stats:\n")
            sb.append("Steps: $steps\n")
            if (calories > 0) sb.append("Calories: $calories kcal\n")
            if (lastHeartRate != null) sb.append("Recent heart rate: ${lastHeartRate.toInt()} bpm\n")
            if (sleepHours != null) sb.append("Neend: ${"%.1f".format(sleepHours)} ghante\n")
            sb.toString()
        } catch (e: Exception) {
            "Health data padhne mein dikkat aayi: ${e.message}"
        }
    }
}
