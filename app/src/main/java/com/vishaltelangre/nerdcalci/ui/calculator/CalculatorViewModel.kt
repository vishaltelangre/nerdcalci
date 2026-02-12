package com.vishaltelangre.nerdcalci.ui.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.core.MathEngine
import com.vishaltelangre.nerdcalci.data.local.CalculatorDao
import com.vishaltelangre.nerdcalci.data.local.entities.FileEntity
import com.vishaltelangre.nerdcalci.data.local.entities.LineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class FileSnapshot(
    val lines: List<LineEntity>
)

class CalculatorViewModel(
    private val dao: CalculatorDao,
    private val prefs: SharedPreferences? = null
) : ViewModel() {

    companion object {
        private const val PREF_THEME = "theme"
        private const val DEFAULT_THEME = "system"
    }

    // Theme state - load saved preference or default to "system"
    private val _currentTheme = MutableStateFlow(
        prefs?.getString(PREF_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    )
    val currentTheme: StateFlow<String> = _currentTheme

    fun setTheme(theme: String) {
        _currentTheme.value = theme
        // Persist theme preference
        prefs?.edit()?.putString(PREF_THEME, theme)?.apply()
    }

    // Undo/Redo stacks with max limit per file
    private val undoStacks = mutableMapOf<Long, MutableList<FileSnapshot>>()
    private val redoStacks = mutableMapOf<Long, MutableList<FileSnapshot>>()
    private val maxHistorySize = Constants.MAX_HISTORY_SIZE

    // State flows to notify UI about undo/redo availability
    private val _canUndo = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val canUndo: StateFlow<Map<Long, Boolean>> = _canUndo

    private val _canRedo = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val canRedo: StateFlow<Map<Long, Boolean>> = _canRedo

    val allFiles = dao.getAllFiles()

    fun getLines(fileId: Long): Flow<List<LineEntity>> = dao.getLinesForFile(fileId)

    // Update undo/redo availability for a file
    private fun updateUndoRedoState(fileId: Long) {
        _canUndo.value = _canUndo.value + (fileId to (undoStacks[fileId]?.isNotEmpty() == true))
        _canRedo.value = _canRedo.value + (fileId to (redoStacks[fileId]?.isNotEmpty() == true))
    }

    // Save current state before making changes (used for undo/redo)
    private suspend fun saveStateForUndo(fileId: Long) {
        val currentLines = dao.getLinesForFileSync(fileId)
        val snapshot = FileSnapshot(currentLines.map { it.copy() })

        val undoStack = undoStacks.getOrPut(fileId) { mutableListOf() }
        undoStack.add(snapshot)

        // Limit stack size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }

        // Clear redo stack when new action is performed
        redoStacks[fileId]?.clear()

        updateUndoRedoState(fileId)
    }

    // Undo last action
    fun undo(fileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val undoStack = undoStacks[fileId] ?: return@launch
            if (undoStack.isEmpty()) return@launch

            // Save current state to redo stack
            val currentLines = dao.getLinesForFileSync(fileId)
            val currentSnapshot = FileSnapshot(currentLines.map { it.copy() })
            val redoStack = redoStacks.getOrPut(fileId) { mutableListOf() }
            redoStack.add(currentSnapshot)

            // Restore previous state
            val previousSnapshot = undoStack.removeAt(undoStack.size - 1)
            restoreSnapshot(fileId, previousSnapshot)

            updateUndoRedoState(fileId)
        }
    }

    // Redo last undone action
    fun redo(fileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val redoStack = redoStacks[fileId] ?: return@launch
            if (redoStack.isEmpty()) return@launch

            // Save current state to undo stack
            val currentLines = dao.getLinesForFileSync(fileId)
            val currentSnapshot = FileSnapshot(currentLines.map { it.copy() })
            val undoStack = undoStacks.getOrPut(fileId) { mutableListOf() }
            undoStack.add(currentSnapshot)

            // Restore redo state
            val redoSnapshot = redoStack.removeAt(redoStack.size - 1)
            restoreSnapshot(fileId, redoSnapshot)

            updateUndoRedoState(fileId)
        }
    }

    // Restore a snapshot with minimal UI flashing
    private suspend fun restoreSnapshot(fileId: Long, snapshot: FileSnapshot) {
        val currentLines = dao.getLinesForFileSync(fileId)
        val snapshotLines = snapshot.lines

        // Update existing lines in-place, then handle extras
        val minSize = minOf(currentLines.size, snapshotLines.size)

        // Update existing lines
        for (i in 0 until minSize) {
            val updatedLine = currentLines[i].copy(
                expression = snapshotLines[i].expression,
                result = snapshotLines[i].result,
                sortOrder = snapshotLines[i].sortOrder
            )
            dao.updateLine(updatedLine)
        }

        // If snapshot has more lines, insert the extras
        if (snapshotLines.size > currentLines.size) {
            for (i in minSize until snapshotLines.size) {
                dao.insertLine(snapshotLines[i].copy(id = 0))
            }
        }

        // If current has more lines, delete the extras
        if (currentLines.size > snapshotLines.size) {
            for (i in minSize until currentLines.size) {
                dao.deleteLine(currentLines[i])
            }
        }

        // Recalculate everything
        val allLines = dao.getLinesForFileSync(fileId)
        val calculatedLines = MathEngine.calculate(allLines)
        calculatedLines.forEach { dao.updateLine(it) }
    }

    // Clear undo/redo history for a file
    fun clearHistory(fileId: Long) {
        undoStacks[fileId]?.clear()
        redoStacks[fileId]?.clear()
        updateUndoRedoState(fileId)
    }

    // Format lines with intelligent result display
    private fun formatFileContent(lines: List<LineEntity>): String {
        return lines.joinToString("\n") { line ->
            val expr = line.expression.trim()
            val result = line.result.trim()

            // Don't show result if:
            // - Expression is empty or result is empty/error
            // - It's a comment line (starts with #)
            // - It's a simple assignment like "a = 5" where result is just "5"
            when {
                expr.isEmpty() || result.isBlank() || result == "Err" -> expr
                expr.trimStart().startsWith("#") -> expr // Full comment line
                shouldShowResult(expr, result) -> "$expr # $result"
                else -> expr
            }
        }
    }

    // Determine if result should be shown based on expression complexity
    private fun shouldShowResult(expression: String, result: String): Boolean {
        // Check if expression has operators (indicating computation)
        val hasOperators = expression.any { it in "+-*/%^" }

        // Check if it's a simple assignment like "a = 5"
        val simpleAssignmentRegex = Regex("""^\s*[a-zA-Z][a-zA-Z0-9\s]*\s*=\s*[\d.]+\s*$""")
        if (simpleAssignmentRegex.matches(expression)) {
            // For "a = 5", result is "5", no need to show result
            return false
        }

        // If there are operators or it's a variable reference, show result
        return hasOperators || !expression.contains("=")
    }

    // Update a line and recalculate everything below it
    fun updateLine(updatedLine: LineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save the user's current typing
            dao.updateLine(updatedLine)

            // Fetch all lines for this file to ensure context is correct
            val allLines = dao.getLinesForFileSync(updatedLine.fileId)

            // Run the MathEngine logic
            val calculatedLines = MathEngine.calculate(allLines)

            // Save the results back to the DB so the UI updates
            calculatedLines.forEach { calculatedLine ->
                dao.updateLine(calculatedLine)
            }
        }
    }

    // Create a new file
    fun createNewFile(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileId = dao.insertFile(FileEntity(name = name, lastModified = System.currentTimeMillis()))
            // Start with one empty line
            dao.insertLine(LineEntity(fileId = fileId, sortOrder = 0, expression = "", result = ""))
            // Notify callback with new file ID on main thread
            withContext(Dispatchers.Main) {
                onCreated(fileId)
            }
        }
    }

    // Duplicate an existing file with all its lines
    fun duplicateFile(sourceFileId: Long, newName: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val sourceFile = dao.getFileById(sourceFileId)
            if (sourceFile != null) {
                // Create new file
                val newFileId = dao.insertFile(
                    FileEntity(
                        name = newName,
                        lastModified = System.currentTimeMillis(),
                        isPinned = false
                    )
                )

                // Copy all lines from source file
                val sourceLines = dao.getLinesForFileSync(sourceFileId)
                sourceLines.forEach { sourceLine ->
                    dao.insertLine(
                        LineEntity(
                            fileId = newFileId,
                            sortOrder = sourceLine.sortOrder,
                            expression = sourceLine.expression,
                            result = sourceLine.result
                        )
                    )
                }

                // Notify callback with new file ID on main thread
                withContext(Dispatchers.Main) {
                    onCreated(newFileId)
                }
            }
        }
    }

    fun addLine(fileId: Long, sortOrder: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save state for undo
            saveStateForUndo(fileId)

            val allLines = dao.getLinesForFileSync(fileId)

            // Shift all lines after this position down by 1
            allLines.filter { it.sortOrder >= sortOrder }
                .sortedByDescending { it.sortOrder }
                .forEach { line ->
                    dao.updateLine(line.copy(sortOrder = line.sortOrder + 1))
                }

            // Insert new line at the specified position
            dao.insertLine(LineEntity(fileId = fileId, sortOrder = sortOrder, expression = "", result = ""))
        }
    }

    fun deleteLine(line: LineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save state for undo
            saveStateForUndo(line.fileId)

            // Delete the line
            dao.deleteLine(line)

            // Shift all lines after this position up by 1
            val allLines = dao.getLinesForFileSync(line.fileId)
            allLines.filter { it.sortOrder > line.sortOrder }
                .sortedBy { it.sortOrder }
                .forEach { lineToShift ->
                    dao.updateLine(lineToShift.copy(sortOrder = lineToShift.sortOrder - 1))
                }
        }
    }

    // Clear all lines in a file
    fun clearAllLines(fileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLines = dao.getLinesForFileSync(fileId)
            allLines.forEach { line ->
                dao.deleteLine(line)
            }
            // Create one empty line to start fresh
            dao.insertLine(LineEntity(fileId = fileId, sortOrder = 0, expression = "", result = ""))

            // Clear undo/redo history since everything is cleared
            clearHistory(fileId)
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLines = dao.getLinesForFileSync(fileId)
            allLines.forEach { line ->
                dao.deleteLine(line)
            }
            val file = FileEntity(id = fileId, name = "", lastModified = 0)
            dao.deleteFile(file)
        }
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = dao.getFileById(fileId)
            if (file != null) {
                dao.updateFile(file.copy(name = newName, lastModified = System.currentTimeMillis()))
            }
        }
    }

    // Toggle pin status for a file
    fun togglePinFile(fileId: Long, onMaxPinnedReached: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = dao.getFileById(fileId)
            if (file != null) {
                // If trying to pin and already at max, notify user
                if (!file.isPinned) {
                    val pinnedCount = dao.getPinnedFilesCount()
                    if (pinnedCount >= Constants.MAX_PINNED_FILES) {
                        withContext(Dispatchers.Main) {
                            onMaxPinnedReached()
                        }
                        return@launch
                    }
                }
                dao.updateFile(file.copy(isPinned = !file.isPinned))
            }
        }
    }

    // Export all files to ZIP
    suspend fun exportAllFiles(context: Context, outputUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val filesList = dao.getAllFiles().first()

                if (filesList.isEmpty()) {
                    return@withContext Result.failure(Exception("No files to export"))
                }

                var fileCount = 0

                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOut ->
                        filesList.forEach { file ->
                            val lines = dao.getLinesForFileSync(file.id)
                            val content = formatFileContent(lines)

                            val entry = ZipEntry("${file.name}${Constants.EXPORT_FILE_EXTENSION}")
                            zipOut.putNextEntry(entry)
                            zipOut.write(content.toByteArray())
                            zipOut.closeEntry()
                            fileCount++
                        }
                        zipOut.finish()
                    }
                } ?: return@withContext Result.failure(Exception("Could not open output stream"))

                Result.success("Exported $fileCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Import files from ZIP
    suspend fun importFiles(context: Context, inputUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var fileCount = 0

                val existingFilesList = dao.getAllFiles().first()

                context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry

                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.endsWith(Constants.EXPORT_FILE_EXTENSION)) {
                                val fileName = entry.name.removeSuffix(Constants.EXPORT_FILE_EXTENSION)
                                val content = BufferedReader(InputStreamReader(zipIn)).readText()

                                val expressions = content.lines()
                                    .filter { it.isNotBlank() }
                                    .map { line ->
                                        // Strip result after # (but not if # is the first character - full comment line)
                                        val hashIndex = line.indexOf('#')
                                        if (hashIndex > 0) {
                                            line.substring(0, hashIndex).trim()
                                        } else {
                                            line.trim()
                                        }
                                    }
                                    .filter { it.isNotEmpty() } // Remove empty lines after stripping

                                // Check if file exists
                                val existingFile = existingFilesList.find { it.name == fileName }

                                val fileId = if (existingFile != null) {
                                    // Delete existing lines
                                    val oldLines = dao.getLinesForFileSync(existingFile.id)
                                    oldLines.forEach { dao.deleteLine(it) }
                                    existingFile.id
                                } else {
                                    // Create new file
                                    dao.insertFile(FileEntity(name = fileName, lastModified = System.currentTimeMillis()))
                                }

                                // Insert lines
                                expressions.forEachIndexed { index, expr ->
                                    dao.insertLine(LineEntity(
                                        fileId = fileId,
                                        sortOrder = index,
                                        expression = expr,
                                        result = ""
                                    ))
                                }

                                // Recalculate
                                val allLines = dao.getLinesForFileSync(fileId)
                                val calculatedLines = MathEngine.calculate(allLines)
                                calculatedLines.forEach { dao.updateLine(it) }

                                fileCount++
                            }

                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }

                Result.success("Imported $fileCount file(s)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Copy current file to clipboard with results
    suspend fun copyFileToClipboard(context: Context, fileId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val lines = dao.getLinesForFileSync(fileId)
                val content = formatFileContent(lines)

                withContext(Dispatchers.Main) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("NerdCalci File", content)
                    clipboard.setPrimaryClip(clip)
                }

                Result.success("Copied to clipboard")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
