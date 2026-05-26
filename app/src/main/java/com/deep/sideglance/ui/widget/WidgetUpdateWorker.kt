package com.deep.sideglance.ui.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.util.LocationHelper
import com.deep.sideglance.util.NotificationHelper
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WidgetUpdateWorker", "Starting background widget state update...")
        try {
            val authRepo = ServiceLocator.authRepository
            val statusRepo = ServiceLocator.statusRepository

            // Fetch session
            val user = authRepo.checkUserSession() ?: authRepo.currentUser.value
            if (user == null || user.partnerUid == null) {
                Log.d("WidgetUpdateWorker", "No user logged in or no partner paired. Skipping.")
                return Result.success()
            }

            val partnerUid = user.partnerUid

            // Fetch partner info and status with try-catch and timeout to avoid throwing and failing background worker
            val partnerUser = try {
                withTimeoutOrNull(3000) {
                    statusRepo.getPartnerUser(partnerUid)
                }
            } catch (e: Exception) {
                Log.w("WidgetUpdateWorker", "Failed to fetch partner profile in background", e)
                null
            }

            val partnerStatus = try {
                withTimeoutOrNull(3000) {
                    statusRepo.getPartnerStatus(partnerUid)
                }
            } catch (e: Exception) {
                Log.w("WidgetUpdateWorker", "Failed to fetch partner status in background", e)
                null
            }

            val sharedPrefs = context.getSharedPreferences("sideglance_prefs", Context.MODE_PRIVATE)
            val isLocationOptIn = sharedPrefs.getBoolean("location_opt_in", true)

            // Location check
            val hasPermission = LocationHelper.hasLocationPermission(context)
            var myLat: Double? = null
            var myLng: Double? = null
            if (hasPermission && isLocationOptIn) {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    myLat = loc.first
                    myLng = loc.second
                    statusRepo.updateLocation(loc.first, loc.second)
                }
            } else {
                statusRepo.updateLocation(null, null)
            }

            // Calculate formatted distance
            val distanceStr = LocationHelper.formatDistance(
                myLat, myLng,
                partnerUser?.latitude, partnerUser?.longitude,
                hasPermission && isLocationOptIn
            )

            // Retrieve last status time to check for changes
            val lastStatusTime = try {
                val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(SideGlanceWidget::class.java)
                if (glanceIds.isNotEmpty()) {
                    val store = PreferencesGlanceStateDefinition.getDataStore(context, glanceIds.first().toString())
                    val prefs = store.data.firstOrNull()
                    prefs?.get(androidx.datastore.preferences.core.longPreferencesKey("status_time")) ?: 0L
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }

            // Trigger notification if new Status is updated by partner
            if (partnerStatus != null && !partnerStatus.isExpired && partnerStatus.timestamp > lastStatusTime) {
                val partnerName = partnerUser?.displayName ?: "Partner"
                NotificationHelper.showPartnerStatusNotification(
                    context = context,
                    partnerName = partnerName,
                    statusText = partnerStatus.statusType,
                    detailText = partnerStatus.detail
                )
            }

            // Update widget preferences with only the fetched non-null values, preserving cache if offline
            WidgetStateUpdateHelper.updateWidgetState(
                context = context,
                name = partnerUser?.displayName,
                statusType = partnerStatus?.statusType,
                detail = if (partnerStatus != null) (partnerStatus.detail ?: "") else null,
                time = partnerStatus?.timestamp,
                expiry = partnerStatus?.expirationTime,
                distance = distanceStr
            )

            Log.d("WidgetUpdateWorker", "Widget update successfully completed.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("WidgetUpdateWorker", "Error updating widget state", e)
            return Result.retry()
        }
    }
}
