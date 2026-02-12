package com.vishaltelangre.nerdcalci.core

/**
 * Application-wide constants
 *
 * Note:
 * - App name is defined in res/values/strings.xml as "app_name"
 * - App version is defined in app/build.gradle.kts as "versionName" and "versionCode"
 */
object Constants {
    // Database
    const val DATABASE_NAME = "calci-db"

    // Developer & Legal (not in standard Android files)
    const val SOURCE_CODE_URL = "https://github.com/vishaltelangre/nerdcalci"
    const val DEVELOPER_NAME = "Vishal Telangre"
    const val LICENSE = "GNU General Public License v3.0"

    // File management
    const val MAX_FILE_NAME_LENGTH = 50
    const val MAX_PINNED_FILES = 10

    // Undo/Redo
    const val MAX_HISTORY_SIZE = 30

    // Export/Import
    const val EXPORT_FILE_EXTENSION = ".nerdcalci"
    const val EXPORT_MIME_TYPE = "application/zip"
}
