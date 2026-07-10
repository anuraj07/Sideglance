package com.deep.sideglance.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.ui.widget.WidgetStateUpdateHelper
import com.deep.sideglance.util.LocationHelper
import com.deep.sideglance.util.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class SideGlanceSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private var partnerSyncJob: Job? = null
    private var lastPartnerStatusTimestamp: Long? = null

    companion object {
        private const val TAG = "SideGlanceSyncService"
        const val ACTION_START = "com.deep.sideglance.action.START_SYNC"
        const val ACTION_STOP = "com.deep.sideglance.action.STOP_SYNC"

        fun start(context: Context) {
            val intent = Intent(context, SideGlanceSyncService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sync service: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SideGlanceSyncService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop sync service: ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Sync Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        // Start Foreground Service
        startServiceInForeground()

        // Start observing authentication state and partner info
        observePartnerData()

        return START_STICKY
    }

    private fun startServiceInForeground() {
        val notification = NotificationHelper.createSyncNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.SYNC_NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.SYNC_NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun observePartnerData() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val authRepo = ServiceLocator.authRepository
            val statusRepo = ServiceLocator.statusRepository

            // Observe the user state
            authRepo.currentUser.collect { currentUser ->
                partnerSyncJob?.cancel()

                if (currentUser == null || currentUser.partnerUid == null) {
                    Log.d(TAG, "No user logged in or no partner paired. Stopping listeners.")
                    lastPartnerStatusTimestamp = null
                    // Reset widget state to default when unpaired/logged out
                    WidgetStateUpdateHelper.updateWidgetState(
                        context = this@SideGlanceSyncService,
                        name = "Partner",
                        statusType = "Offline",
                        detail = "",
                        time = 0L,
                        expiry = 0L,
                        distance = "infinity"
                    )
                    return@collect
                }

                val partnerUid = currentUser.partnerUid
                Log.d(TAG, "Starting sync listeners for partner: $partnerUid")

                // Launch a separate coroutine for this partner's updates
                partnerSyncJob = launch {
                    combine(
                        statusRepo.observePartnerUser(partnerUid),
                        statusRepo.observePartnerStatus(partnerUid)
                    ) { partnerUser, partnerStatus ->
                        Pair(partnerUser, partnerStatus)
                    }.distinctUntilChanged()
                        .collect { (partnerUser, partnerStatus) ->
                            processUpdate(partnerUser, partnerStatus)
                        }
                }
            }
        }
    }

    private suspend fun processUpdate(partnerUser: User?, partnerStatus: UserStatus?) {
        Log.d(TAG, "Processing sync update. PartnerUser: ${partnerUser?.displayName}, PartnerStatus: ${partnerStatus?.statusType}")

        // Retrieve my location to calculate distance
        val sharedPrefs = getSharedPreferences("sideglance_prefs", Context.MODE_PRIVATE)
        val isLocationOptIn = sharedPrefs.getBoolean("location_opt_in", true)
        val hasPermission = LocationHelper.hasLocationPermission(this)

        var myLat: Double? = null
        var myLng: Double? = null
        if (hasPermission && isLocationOptIn) {
            val loc = LocationHelper.getCurrentLocation(this)
            if (loc != null) {
                myLat = loc.first
                myLng = loc.second
            }
        }

        // Calculate distance
        val distanceStr = LocationHelper.formatDistance(
            myLat, myLng,
            partnerUser?.latitude, partnerUser?.longitude,
            hasPermission && isLocationOptIn
        )

        // Notification logic
        if (partnerStatus != null && !partnerStatus.isExpired) {
            val currentTimestamp = partnerStatus.timestamp
            if (lastPartnerStatusTimestamp != null && currentTimestamp > lastPartnerStatusTimestamp!!) {
                val partnerName = partnerUser?.displayName ?: "Partner"
                NotificationHelper.showPartnerStatusNotification(
                    context = this,
                    partnerName = partnerName,
                    statusText = partnerStatus.statusType,
                    detailText = partnerStatus.detail
                )
            }
            lastPartnerStatusTimestamp = currentTimestamp
        } else {
            lastPartnerStatusTimestamp = null
        }

        // Update Glance Widget
        WidgetStateUpdateHelper.updateWidgetState(
            context = this,
            name = partnerUser?.displayName,
            statusType = partnerStatus?.statusType ?: "Offline",
            detail = partnerStatus?.detail ?: "",
            time = partnerStatus?.timestamp,
            expiry = partnerStatus?.expirationTime,
            distance = distanceStr
        )
    }

    private fun stopForegroundService() {
        Log.d(TAG, "Stopping Sync Service")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Sync Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved called - service will persist because stopWithTask is false and it is sticky")
    }
}
