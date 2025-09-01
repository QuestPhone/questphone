package neth.iecal.questphone.core.utils.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import nethical.questphone.data.quest.health.HealthTaskType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class HealthConnectManager(private val context: Context) {
    companion object {
        val requiredPermissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class)
        )
    }

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val permissionController by lazy { healthConnectClient.permissionController }


    suspend fun isAvailable(): Boolean = suspendCoroutine { continuation ->
        try {
            val providerPackageName = "com.google.android.apps.healthdata" // Define the provider package name
            val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)

            when (availabilityStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    continuation.resume(true)
                }
                HealthConnectClient.SDK_UNAVAILABLE -> {
                    continuation.resume(false)
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    // Optionally redirect to package installer
                    val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setPackage("com.android.vending")
                            data = Uri.parse(uriString)
                            putExtra("overlay", true)
                            putExtra("callerId", context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK // Add this since it's called from a suspend function
                        }
                    )
                    continuation.resume(false)
                }
                else -> {
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking availability", e)
            continuation.resume(false)
        }
    }
    suspend fun hasAllPermissions(): Boolean =
        permissionController.getGrantedPermissions().containsAll(requiredPermissions)

    private fun getFullDayTimeRange(): TimeRangeFilter {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        return TimeRangeFilter.between(startOfDay, startOfTomorrow)
    }


    suspend fun getTodayHealthData(type: HealthTaskType): Double {
        if (!hasAllPermissions()) {
            throw SecurityException("Required Health Connect permissions not granted")
        }

        val timeRangeFilter = getFullDayTimeRange()
        return when (type) {
            HealthTaskType.STEPS -> {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                aggregateResponse[StepsRecord.COUNT_TOTAL]?.toDouble() ?: 0.0
            }
            HealthTaskType.CALORIES -> {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            }
            HealthTaskType.DISTANCE -> {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            }
            HealthTaskType.SLEEP -> readRecords<SleepSessionRecord>(timeRangeFilter)
                .sumOf {
                    println("Sleep session: start=${it.startTime}, end=${it.endTime}")
                    Duration.between(it.startTime, it.endTime).toHours().toDouble()
                }
            HealthTaskType.WATER_INTAKE -> {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                aggregateResponse[HydrationRecord.VOLUME_TOTAL]?.inMilliliters ?: 0.0
            }
        }
    }

    private suspend inline fun <reified T : Record> readRecords(timeRangeFilter: TimeRangeFilter): List<T> {
        val request = ReadRecordsRequest(recordType = T::class, timeRangeFilter = timeRangeFilter)
        return healthConnectClient.readRecords(request).records
    }
}
