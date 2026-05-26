package com.deep.sideglance.ui.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deep.sideglance.util.NotificationHelper

class StatusExpiredWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.showStatusExpiredNotification(context)
        return Result.success()
    }
}
