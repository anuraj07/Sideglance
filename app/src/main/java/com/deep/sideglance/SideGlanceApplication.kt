package com.deep.sideglance

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.ui.widget.WidgetUpdateWorker
import com.deep.sideglance.util.NotificationHelper
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.deep.sideglance.data.service.SideGlanceSyncService

class SideGlanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.createNotificationChannel(this)
        
        scheduleWidgetUpdates()

        // Start background sync service if already logged in and paired
        MainScope().launch {
            val authRepo = ServiceLocator.authRepository
            val user = authRepo.checkUserSession() ?: authRepo.currentUser.value
            if (user != null && user.partnerUid != null) {
                SideGlanceSyncService.start(this@SideGlanceApplication)
            }
        }
    }

    private fun scheduleWidgetUpdates() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SideGlanceWidgetUpdateWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
