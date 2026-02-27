package com.vishaltelangre.nerdcalci.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Room
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.data.local.AppDatabase
import com.vishaltelangre.nerdcalci.data.local.DatabaseMigrations

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = BackupManager.readSettings(BackupManager.prefs(applicationContext))
        if (!settings.enabled) {
            return Result.success()
        }

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()

        return try {
            BackupManager.backupNow(applicationContext, db.calculatorDao())
            Result.success()
        } catch (_: Exception) {
            Result.success()
        } finally {
            db.close()
        }
    }
}
