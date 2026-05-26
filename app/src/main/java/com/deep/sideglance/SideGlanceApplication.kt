package com.deep.sideglance

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.ui.widget.WidgetUpdateWorker
import com.deep.sideglance.util.NotificationHelper
import java.util.concurrent.TimeUnit

class SideGlanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.createNotificationChannel(this)
        
        scheduleWidgetUpdates()
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
