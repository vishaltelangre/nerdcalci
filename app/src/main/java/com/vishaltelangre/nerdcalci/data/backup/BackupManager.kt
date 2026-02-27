package com.vishaltelangre.nerdcalci.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.core.MathEngine
import com.vishaltelangre.nerdcalci.data.local.CalculatorDao
import com.vishaltelangre.nerdcalci.data.local.entities.FileEntity
import com.vishaltelangre.nerdcalci.data.local.entities.LineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupFrequency(val prefValue: String, val intervalDays: Long) {
    DAILY("daily", 1L),
    WEEKLY("weekly", 7L);

    companion object {
        fun fromPrefValue(value: String?): BackupFrequency = entries.firstOrNull {
            it.prefValue == value
        } ?: DAILY
    }
}

enum class BackupLocationMode(val prefValue: String) {
    APP_STORAGE("app_storage"),
    CUSTOM_FOLDER("custom_folder");

    companion object {
        fun fromPrefValue(value: String?): BackupLocationMode = entries.firstOrNull {
            it.prefValue == value
        } ?: APP_STORAGE
    }
}

enum class BackupSource {
    APP_STORAGE,
    CUSTOM_FOLDER
}

data class BackupSettings(
    val enabled: Boolean,
    val frequency: BackupFrequency,
    val locationMode: BackupLocationMode,
    val customFolderUri: String?,
    val keepLatestCount: Int
)

data class BackupFileInfo(
    val id: String,
    val displayName: String,
    val lastModified: Long,
    val source: BackupSource,
    val pathOrUri: String
)

object BackupManager {
    const val PREFS_NAME = "nerdcalci_prefs"
    const val PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    const val PREF_AUTO_BACKUP_FREQUENCY = "auto_backup_frequency"
    const val PREF_AUTO_BACKUP_LOCATION_MODE = "auto_backup_location_mode"
    const val PREF_AUTO_BACKUP_CUSTOM_FOLDER_URI = "auto_backup_custom_folder_uri"
    const val PREF_AUTO_BACKUP_KEEP_COUNT = "auto_backup_keep_count"
    const val PREF_LAST_BACKUP_AT = "last_backup_at"

    private const val BACKUP_DIR_NAME = "backups"
    private const val BACKUP_FILE_PREFIX = "nerdcalci_backup_"
    private const val BACKUP_FILE_SUFFIX = ".zip"

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun readSettings(prefs: SharedPreferences): BackupSettings {
        val keepCount = prefs.getInt(PREF_AUTO_BACKUP_KEEP_COUNT, Constants.DEFAULT_BACKUP_KEEP_COUNT).coerceAtLeast(1)
        return BackupSettings(
            enabled = prefs.getBoolean(PREF_AUTO_BACKUP_ENABLED, true),
            frequency = BackupFrequency.fromPrefValue(prefs.getString(PREF_AUTO_BACKUP_FREQUENCY, BackupFrequency.DAILY.prefValue)),
            locationMode = BackupLocationMode.fromPrefValue(
                prefs.getString(PREF_AUTO_BACKUP_LOCATION_MODE, BackupLocationMode.APP_STORAGE.prefValue)
            ),
            customFolderUri = prefs.getString(PREF_AUTO_BACKUP_CUSTOM_FOLDER_URI, null),
            keepLatestCount = keepCount
        )
    }

    suspend fun exportAllFiles(context: Context, dao: CalculatorDao, outputUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val exportedCount = context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    writeBackupZip(dao, outputStream)
                } ?: return@withContext Result.failure(Exception("Could not open output stream"))

                if (exportedCount == 0) {
                    return@withContext Result.failure(Exception("No files to export"))
                }

                Result.success("Exported $exportedCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun importFiles(context: Context, dao: CalculatorDao, inputUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val importedCount = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    importFromZip(dao, inputStream)
                } ?: return@withContext Result.failure(Exception("Could not open input stream"))

                Result.success("Imported $importedCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun backupNow(context: Context, dao: CalculatorDao): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val settings = readSettings(prefs(context))
                val preferredCustom = settings.locationMode == BackupLocationMode.CUSTOM_FOLDER && !settings.customFolderUri.isNullOrBlank()
                val result: Result<String> = if (preferredCustom) {
                    val customResult = writeToCustomFolder(context, dao, settings)
                    if (customResult.isSuccess) {
                        customResult
                    } else {
                        val fallbackResult = writeToAppStorage(context, dao, settings.keepLatestCount)
                        if (fallbackResult.isSuccess) {
                            Result.success("Custom folder unavailable. Saved backup in app storage instead.")
                        } else {
                            fallbackResult
                        }
                    }
                } else {
                    writeToAppStorage(context, dao, settings.keepLatestCount)
                }
                val message = result.getOrNull()
                if (result.isSuccess && message != "No files to back up") {
                    prefs(context).edit().putLong(PREF_LAST_BACKUP_AT, System.currentTimeMillis()).apply()
                }
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun listBackups(context: Context): List<BackupFileInfo> {
        return withContext(Dispatchers.IO) {
            val settings = readSettings(prefs(context))
            val backups = when (settings.locationMode) {
                BackupLocationMode.APP_STORAGE -> listAppStorageBackups(context)
                BackupLocationMode.CUSTOM_FOLDER -> {
                    if (settings.customFolderUri.isNullOrBlank()) {
                        emptyList()
                    } else {
                        listCustomFolderBackups(context, settings.customFolderUri)
                    }
                }
            }
            backups.sortedByDescending { it.lastModified }
        }
    }

    suspend fun restoreFromBackup(context: Context, dao: CalculatorDao, backup: BackupFileInfo): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val importedCount = when (backup.source) {
                    BackupSource.APP_STORAGE -> {
                        FileInputStream(File(backup.pathOrUri)).use { input ->
                            importFromZip(dao, input)
                        }
                    }

                    BackupSource.CUSTOM_FOLDER -> {
                        val uri = Uri.parse(backup.pathOrUri)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            importFromZip(dao, input)
                        } ?: return@withContext Result.failure(Exception("Could not open backup file"))
                    }
                }
                Result.success("Restored $importedCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getBackupDirectory(context: Context): File {
        val directory = File(context.filesDir, BACKUP_DIR_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private suspend fun writeToAppStorage(context: Context, dao: CalculatorDao, keepLatestCount: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val outputFile = File(getBackupDirectory(context), generateBackupFileName())
                val exportedCount = FileOutputStream(outputFile).use { outputStream ->
                    writeBackupZip(dao, outputStream)
                }
                if (exportedCount == 0) {
                    outputFile.delete()
                    return@withContext Result.success("No files to back up")
                }

                enforceAppStorageRetention(context, keepLatestCount)
                Result.success("Backed up $exportedCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun writeToCustomFolder(context: Context, dao: CalculatorDao, settings: BackupSettings): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val folderUri = settings.customFolderUri ?: return@withContext Result.failure(Exception("Custom folder not set"))
                val treeUri = Uri.parse(folderUri)
                val folder = DocumentFile.fromTreeUri(context, treeUri)
                    ?: return@withContext Result.failure(Exception("Custom folder unavailable"))

                val backupFile = folder.createFile(Constants.EXPORT_MIME_TYPE, generateBackupFileName())
                    ?: return@withContext Result.failure(Exception("Could not create backup file"))

                val exportedCount = context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                    writeBackupZip(dao, output)
                } ?: return@withContext Result.failure(Exception("Could not open backup output stream"))

                if (exportedCount == 0) {
                    backupFile.delete()
                    return@withContext Result.success("No files to back up")
                }

                enforceCustomFolderRetention(context, folder, settings.keepLatestCount)
                Result.success("Backed up $exportedCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun listAppStorageBackups(context: Context): List<BackupFileInfo> {
        val files = getBackupDirectory(context)
            .listFiles { file -> file.isFile && file.name.endsWith(BACKUP_FILE_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        return files.map { file ->
            BackupFileInfo(
                id = "app:${file.absolutePath}",
                displayName = file.name,
                lastModified = file.lastModified(),
                source = BackupSource.APP_STORAGE,
                pathOrUri = file.absolutePath
            )
        }
    }

    private fun listCustomFolderBackups(context: Context, folderUri: String): List<BackupFileInfo> {
        return try {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return emptyList()
            folder.listFiles()
                .asSequence()
                .filter { it.isFile && (it.name?.endsWith(BACKUP_FILE_SUFFIX) == true) }
                .sortedByDescending { it.lastModified() }
                .map { file ->
                    BackupFileInfo(
                        id = "custom:${file.uri}",
                        displayName = file.name ?: "backup.zip",
                        lastModified = file.lastModified(),
                        source = BackupSource.CUSTOM_FOLDER,
                        pathOrUri = file.uri.toString()
                    )
                }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun enforceAppStorageRetention(context: Context, keepLatestCount: Int) {
        val keepCount = keepLatestCount.coerceAtLeast(1)
        getBackupDirectory(context)
            .listFiles { file -> file.isFile && file.name.endsWith(BACKUP_FILE_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keepCount)
            ?.forEach { it.delete() }
    }

    private fun enforceCustomFolderRetention(context: Context, folder: DocumentFile, keepLatestCount: Int) {
        val keepCount = keepLatestCount.coerceAtLeast(1)
        folder.listFiles()
            .asSequence()
            .filter { it.isFile && (it.name?.endsWith(BACKUP_FILE_SUFFIX) == true) }
            .sortedByDescending { it.lastModified() }
            .drop(keepCount)
            .forEach { file ->
                try {
                    context.contentResolver.delete(file.uri, null, null)
                } catch (_: Exception) {
                    file.delete()
                }
            }
    }

    private suspend fun writeBackupZip(dao: CalculatorDao, outputStream: OutputStream): Int {
        val filesList = dao.getAllFiles().first()
        var exportedCount = 0

        ZipOutputStream(outputStream).use { zipOut ->
            filesList.forEach { file ->
                val lines = dao.getLinesForFileSync(file.id)
                val content = formatFileContent(lines)

                val entry = ZipEntry("${file.name}${Constants.EXPORT_FILE_EXTENSION}")
                zipOut.putNextEntry(entry)
                zipOut.write(content.toByteArray())
                zipOut.closeEntry()
                exportedCount++
            }
            zipOut.finish()
        }

        return exportedCount
    }

    private suspend fun importFromZip(dao: CalculatorDao, inputStream: InputStream): Int {
        var importedCount = 0
        val existingFilesByName = dao.getAllFiles().first().associateBy { it.name }.toMutableMap()

        ZipInputStream(inputStream).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry

            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(Constants.EXPORT_FILE_EXTENSION)) {
                    val fileName = entry.name.removeSuffix(Constants.EXPORT_FILE_EXTENSION)
                    val content = BufferedReader(InputStreamReader(zipIn)).readText()

                    val expressions = content.lines()
                        .filter { it.isNotBlank() }
                        .map { line ->
                            val hashIndex = line.indexOf('#')
                            if (hashIndex > 0) {
                                line.substring(0, hashIndex).trim()
                            } else {
                                line.trim()
                            }
                        }
                        .filter { it.isNotEmpty() }

                    val existingFile = existingFilesByName[fileName]
                    val fileId = if (existingFile != null) {
                        val oldLines = dao.getLinesForFileSync(existingFile.id)
                        oldLines.forEach { dao.deleteLine(it) }
                        existingFile.id
                    } else {
                        val newFileId = dao.insertFile(
                            FileEntity(
                                name = fileName,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                        existingFilesByName[fileName] = FileEntity(
                            id = newFileId,
                            name = fileName,
                            lastModified = System.currentTimeMillis()
                        )
                        newFileId
                    }

                    expressions.forEachIndexed { index, expr ->
                        dao.insertLine(
                            LineEntity(
                                fileId = fileId,
                                sortOrder = index,
                                expression = expr,
                                result = ""
                            )
                        )
                    }

                    val allLines = dao.getLinesForFileSync(fileId)
                    val calculatedLines = MathEngine.calculate(allLines)
                    calculatedLines.forEach { dao.updateLine(it) }
                    importedCount++
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        return importedCount
    }

    private fun formatFileContent(lines: List<LineEntity>): String {
        return lines.joinToString("\n") { line ->
            val expr = line.expression.trim()
            val result = line.result.trim()

            when {
                expr.isEmpty() || result.isBlank() || result == "Err" -> expr
                expr.trimStart().startsWith("#") -> expr
                shouldShowResult(expr) -> "$expr # $result"
                else -> expr
            }
        }
    }

    private fun shouldShowResult(expression: String): Boolean {
        val hasOperators = expression.any { it in "+-*/%^" }
        val simpleAssignmentRegex = Regex("""^\s*[a-zA-Z][a-zA-Z0-9\s]*\s*=\s*[\d.]+\s*$""")
        if (simpleAssignmentRegex.matches(expression)) {
            return false
        }
        return hasOperators || !expression.contains("=")
    }

    private fun generateBackupFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val timestamp = formatter.format(Date())
        return "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_SUFFIX"
    }
}
