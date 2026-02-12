package com.vishaltelangre.nerdcalci.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vishaltelangre.nerdcalci.data.local.entities.FileEntity
import com.vishaltelangre.nerdcalci.data.local.entities.LineEntity

/**
 * Room database for NerdCalci app.
 *
 * Accessed through CalculatorDao which provides all CRUD operations.
 */
@Database(
    entities = [FileEntity::class, LineEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculatorDao(): CalculatorDao
}
