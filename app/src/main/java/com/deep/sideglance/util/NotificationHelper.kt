package com.deep.sideglance.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "sideglance_channel"
    private const val CHANNEL_NAME = "Sideglance Updates"
    private const val CHANNEL_DESC = "Notifications for partner status changes and status expirations"
    
    private const val SYNC_CHANNEL_ID = "sideglance_sync_channel"
    private const val SYNC_CHANNEL_NAME = "Background Synchronization"
    private const val SYNC_CHANNEL_DESC = "Keeps your widgets and status notifications updated in real time"

    private const val PARTNER_STATUS_NOTIF_ID = 1001
    private const val OWN_STATUS_EXPIRED_NOTIF_ID = 1002
    const val SYNC_NOTIF_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                SYNC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SYNC_CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    fun createSyncNotification(context: Context): android.app.Notification {
        createNotificationChannel(context)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(com.deep.sideglance.R.drawable.ic_launcher_foreground)
            .setContentTitle("SideGlance Sync")
            .setContentText("Listening for partner status changes...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun showPartnerStatusNotification(context: Context, partnerName: String, statusText: String, detailText: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (!detailText.isNullOrBlank()) "$statusText ($detailText)" else statusText
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.deep.sideglance.R.drawable.ic_launcher_foreground)
            .setContentTitle("$partnerName updated their status")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(PARTNER_STATUS_NOTIF_ID, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showStatusExpiredNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.deep.sideglance.R.drawable.ic_launcher_foreground)
            .setContentTitle("Your status expired")
            .setContentText("Tap to update what you are doing!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(OWN_STATUS_EXPIRED_NOTIF_ID, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
