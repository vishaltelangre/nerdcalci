package com.vishaltelangre.nerdcalci.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    const val AUTO_BACKUP_WORK_NAME = "nerdcalci_auto_backup"

    fun sync(context: Context, prefs: SharedPreferences = BackupManager.prefs(context)) {
        val settings = BackupManager.readSettings(prefs)
        val workManager = WorkManager.getInstance(context)

        if (!settings.enabled) {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            settings.frequency.intervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
