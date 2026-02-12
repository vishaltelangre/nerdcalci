package com.vishaltelangre.nerdcalci.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.data.local.entities.LineEntity
import com.vishaltelangre.nerdcalci.ui.components.DeleteFileDialog
import com.vishaltelangre.nerdcalci.ui.components.DuplicateFileDialog
import com.vishaltelangre.nerdcalci.ui.components.RenameFileDialog
import com.vishaltelangre.nerdcalci.ui.theme.FiraCodeFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Syntax highlighting colors (dark theme) - High contrast for readability
private val NumberColorDark = Color(0xFFFFD54F) // Bright yellow for numbers
private val VariableColorDark = Color(0xFF64FFDA) // Bright cyan/teal for variables
private val OperatorColorDark = Color.White // Pure white for operators (=, +, -, etc)
private val PercentColorDark = Color(0xFFFFAB40) // Bright amber for percentages
private val CommentColorDark = Color(0xFF607D8B) // Muted blue-gray for comments (dimmer)

// Syntax highlighting colors (light theme)
private val NumberColorLight = Color(0xFF09885A)
private val VariableColorLight = Color(0xFF001080)
private val OperatorColorLight = Color(0xFF000000)
private val PercentColorLight = Color(0xFFA31515)
private val CommentColorLight = Color(0xFF5A7A5A) // Dimmer but readable green

// Apply syntax highlighting
/**
 * Apply syntax highlighting to calculator expressions.
 *
 * Colors adapt to dark/light theme for optimal readability.
 */
private fun applySyntaxHighlighting(
    text: String,
    numberColor: Color,
    variableColor: Color,
    operatorColor: Color,
    percentColor: Color,
    commentColor: Color,
    defaultColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val char = text[i]
            when {
                // Comments: everything from # to end of string
                char == '#' -> {
                    withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i))
                    }
                    break
                }
                char.isDigit() || (char == '.' && i + 1 < text.length && text[i + 1].isDigit()) -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] == '.')) i++
                    withStyle(SpanStyle(color = numberColor)) { append(text.substring(start, i)) }
                    continue
                }
                char.isLetter() -> {
                    val start = i
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    withStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.Bold)) { append(text.substring(start, i)) }
                    continue
                }
                char == '%' -> withStyle(SpanStyle(color = percentColor)) { append(char) }
                char in "+-*/^()=×÷" -> withStyle(SpanStyle(color = operatorColor)) { append(char) }
                else -> withStyle(SpanStyle(color = defaultColor)) { append(char) }
            }
            i++
        }
    }
}

/**
 * Extract variable names from calculator expressions for autocomplete.
 *
 * Parses assignment statements (e.g., "price = 100") and extracts the variable name.
 * Supports both underscore and space-separated names:
 * - "monthly_salary = 5000" → "monthly_salary"
 * - "monthly salary = 5000" → "monthly salary"
 *
 * @return Set of variable names defined in the file
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun extractVariables(lines: List<LineEntity>): Set<String> {
    val variables = mutableSetOf<String>()
    lines.forEach { line ->
        // Strip comments first
        val hashIndex = line.expression.indexOf('#')
        val exprWithoutComment = if (hashIndex >= 0) {
            line.expression.substring(0, hashIndex).trim()
        } else {
            line.expression
        }

        // Match variable assignments: "var name = ..." (can include spaces)
        val assignmentRegex = Regex("""^\s*([a-zA-Z][a-zA-Z0-9\s]*?)\s*=""")
        assignmentRegex.find(exprWithoutComment)?.groupValues?.get(1)?.let {
            variables.add(it.trim())
        }
    }
    return variables
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Main calculator editor screen.
 *
 * @param fileId ID of the file to edit
 * @param viewModel ViewModel managing calculator state and operations
 * @param onBack Callback when back button is pressed
 * @param onHelp Callback when help button is pressed
 * @param onNavigateToFile Callback when navigating to a different file (used for duplicate)
 */
@Composable
fun CalculatorScreen(
    fileId: Long,
    viewModel: CalculatorViewModel,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onNavigateToFile: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lines by viewModel.getLines(fileId).collectAsState(initial = emptyList())
    val files by viewModel.allFiles.collectAsState(initial = emptyList())
    val canUndoMap by viewModel.canUndo.collectAsState()
    val canRedoMap by viewModel.canRedo.collectAsState()
    val canUndo = canUndoMap[fileId] ?: false
    val canRedo = canRedoMap[fileId] ?: false
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val fileName = files.find { it.id == fileId }?.name ?: "Editor"

    // Track which line should be focused and cursor position
    var focusLineId by remember { mutableStateOf<Long?>(null) }
    var focusCursorPosition by remember { mutableStateOf<Int?>(null) }

    // Track which line is currently focused by the user
    var currentlyFocusedLineId by remember { mutableStateOf<Long?>(null) }

    // Track toolbar text insertion requests (used for inserting symbols using custom keyboard shortcuts)
    var insertTextRequest by remember { mutableStateOf<Pair<Long, String>?>(null) }

    // Check if keyboard is visible
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(density) > 0

    // Extract all defined variables for autocomplete
    val availableVariables = remember(lines) { extractVariables(lines) }

    // Theme-aware colors - respect app theme setting, not system
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDarkTheme = when (currentTheme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val numberColor = if (isDarkTheme) NumberColorDark else NumberColorLight
    val variableColor = if (isDarkTheme) VariableColorDark else VariableColorLight
    val operatorColor = if (isDarkTheme) OperatorColorDark else OperatorColorLight
    val percentColor = if (isDarkTheme) PercentColorDark else PercentColorLight
    val commentColor = if (isDarkTheme) CommentColorDark else CommentColorLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = fileName,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.undo(fileId) },
                    enabled = canUndo
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.redo(fileId) },
                    enabled = canRedo
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Help") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onHelp()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Rename File") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate File") },
                            leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDuplicateDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy File Content") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    val result = viewModel.copyFileToClipboard(context, fileId)
                                    result.onSuccess { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }.onFailure { error ->
                                        Toast.makeText(context, "Copy failed: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Clear File") },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showClearConfirmDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete File") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f)) {
            // Full-height vertical dividers as background
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(50.dp))
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.weight(1f))
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.width(120.dp))
            }

            // LazyColumn with lines on top
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(lines) { index, line ->
                    LineRow(
                        lineNumber = index + 1,
                        line = line,
                        availableVariables = availableVariables,
                        shouldFocus = focusLineId == line.id,
                        focusCursorPos = if (focusLineId == line.id) focusCursorPosition else null,
                        insertTextRequest = if (insertTextRequest?.first == line.id) insertTextRequest?.second else null,
                        onInsertHandled = { insertTextRequest = null },
                        numberColor = numberColor,
                        variableColor = variableColor,
                        operatorColor = operatorColor,
                        percentColor = percentColor,
                        commentColor = commentColor,
                        isDarkTheme = isDarkTheme,
                        onFocused = {
                            currentlyFocusedLineId = line.id
                            focusLineId = null
                            focusCursorPosition = null
                        },
                        onBlur = {
                            if (currentlyFocusedLineId == line.id) {
                                currentlyFocusedLineId = null
                            }
                        },
                        onValueChange = { newValue ->
                            viewModel.updateLine(line.copy(expression = newValue))
                        },
                        onEnter = {
                            viewModel.addLine(fileId, line.sortOrder + 1)
                            // Focus will be handled by auto-focus in LineRow for new empty lines
                        },
                        onDelete = {
                            if (lines.size > 1) {
                                // Focus previous line at end
                                val prevIndex = index - 1
                                val prevLine = lines.getOrNull(prevIndex)
                                if (prevLine != null) {
                                    val prevLineNumber = prevIndex + 1 // Convert 0-based index to 1-based line number
                                    focusLineId = prevLine.id
                                    // For empty lines: line 1 has no leading space (pos 0), others at pos 1
                                    focusCursorPosition = if (prevLine.expression.isEmpty()) {
                                        if (prevLineNumber == 1) 0 else 1
                                    } else {
                                        prevLine.expression.length
                                    }
                                }
                                viewModel.deleteLine(line)
                            }
                        },
                        onNavigateUp = {
                            if (index > 0) {
                                val prevIndex = index - 1
                                val prevLine = lines[prevIndex]
                                val prevLineNumber = prevIndex + 1
                                focusLineId = prevLine.id
                                // For empty lines: line 1 has no leading space (pos 0), others at pos 1
                                focusCursorPosition = if (prevLine.expression.isEmpty()) {
                                    if (prevLineNumber == 1) 0 else 1
                                } else {
                                    prevLine.expression.length
                                }
                            }
                        },
                        onNavigateDown = {
                            if (index < lines.size - 1) {
                                val nextIndex = index + 1
                                val nextLine = lines[nextIndex]
                                val nextLineNumber = nextIndex + 1
                                focusLineId = nextLine.id
                                // For empty lines: line 1 has no leading space (pos 0), others at pos 1
                                focusCursorPosition = if (nextLine.expression.isEmpty()) {
                                    if (nextLineNumber == 1) 0 else 1
                                } else {
                                    nextLine.expression.length
                                }
                            }
                        }
                    )
                    if (index < lines.size - 1) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        }

        // Keyboard toolbar with common symbols - only show when keyboard is visible
        // Position it at the bottom of the Box, above the keyboard
        if (isKeyboardVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
            ) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val symbols = listOf(".", "+", "-", "×", "÷", "=", "%", "(", ")", "#")
                    symbols.forEach { symbol ->
                        Surface(
                            onClick = {
                                // Trigger insertion for the currently focused line
                                currentlyFocusedLineId?.let { lineId ->
                                    insertTextRequest = Pair(lineId, symbol)
                                }
                            },
                            modifier = Modifier
                                .width(44.dp)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symbol,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FiraCodeFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename File dialog
    if (showRenameDialog) {
        RenameFileDialog(
            currentName = fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameFile(fileId, newName.take(Constants.MAX_FILE_NAME_LENGTH))
                showRenameDialog = false
            }
        )
    }

    // Duplicate File dialog
    if (showDuplicateDialog) {
        DuplicateFileDialog(
            originalName = fileName,
            onDismiss = { showDuplicateDialog = false },
            onConfirm = { newName ->
                viewModel.duplicateFile(fileId, newName.take(Constants.MAX_FILE_NAME_LENGTH)) { newFileId ->
                    onNavigateToFile(newFileId)
                }
                showDuplicateDialog = false
            }
        )
    }

    // Clear All confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear all lines?") },
            text = { Text("This will delete all lines in this file. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllLines(fileId)
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete File confirmation dialog
    if (showDeleteConfirmDialog) {
        DeleteFileDialog(
            fileName = fileName,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteFile(fileId)
                onBack()
            }
        )
    }
}

@Composable
private fun LineRow(
    lineNumber: Int,
    line: LineEntity,
    availableVariables: Set<String>,
    shouldFocus: Boolean,
    focusCursorPos: Int?,
    insertTextRequest: String?,
    onInsertHandled: () -> Unit,
    numberColor: Color,
    variableColor: Color,
    operatorColor: Color,
    percentColor: Color,
    commentColor: Color,
    isDarkTheme: Boolean,
    onFocused: () -> Unit,
    onBlur: () -> Unit,
    onValueChange: (String) -> Unit,
    onEnter: () -> Unit,
    onDelete: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit
) {
    // Add leading space for backspace detection trick (but not for first line)
    val displayText = if (line.expression.isEmpty() && lineNumber > 1) " " else line.expression
    val defaultTextColor = MaterialTheme.colorScheme.onSurface

    var textFieldValue by remember(line.id) {
        mutableStateOf(TextFieldValue(
            annotatedString = applySyntaxHighlighting(
                displayText,
                numberColor,
                variableColor,
                operatorColor,
                percentColor,
                commentColor,
                defaultTextColor
            )
        ))
    }
    var previousSelection by remember { mutableStateOf(textFieldValue.selection) }
    var isFocused by remember { mutableStateOf(false) }
    var showDeleteIcon by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Autocomplete suggestions
    val currentWord = remember(textFieldValue.text, textFieldValue.selection) {
        val cursorPos = textFieldValue.selection.start
        val text = textFieldValue.text
        if (cursorPos > 0) {
            // Check if cursor is inside a comment (after #)
            val beforeCursor = text.substring(0, cursorPos)
            val hashIndex = beforeCursor.indexOf('#')
            if (hashIndex >= 0) {
                // Cursor is inside a comment, don't suggest
                ""
            } else {
                // Find the word before cursor
                val wordStart = beforeCursor.lastIndexOfAny(charArrayOf(' ', '+', '-', '*', '/', '×', '÷', '(', ')', '=', ',')) + 1
                beforeCursor.substring(wordStart)
            }
        } else ""
    }

    val suggestions = remember(currentWord, availableVariables) {
        if (currentWord.isNotEmpty() && currentWord.all { it.isLetterOrDigit() || it == '_' }) {
            availableVariables.filter {
                it.startsWith(currentWord, ignoreCase = true) && it != currentWord
            }.sorted()
        } else emptyList()
    }

    // Show delete icon after a delay when line loses focus
    LaunchedEffect(isFocused, textFieldValue.text) {
        val isEmpty = textFieldValue.text.trim().isEmpty()
        if (!isFocused && isEmpty && lineNumber > 1) {
            delay(500) // 500ms delay
            if (!isFocused) { // Check again in case focus changed during delay
                showDeleteIcon = true
            }
        } else {
            showDeleteIcon = false
        }
    }

    // Sync with database updates
    LaunchedEffect(line.expression) {
        if (textFieldValue.text.trim() != line.expression) {
            textFieldValue = TextFieldValue(
                annotatedString = applySyntaxHighlighting(
                    displayText,
                    numberColor,
                    variableColor,
                    operatorColor,
                    percentColor,
                    commentColor,
                    defaultTextColor
                ),
                selection = textFieldValue.selection
            )
        }
    }

    // Auto-focus empty lines:
    // - Line 1: if it's the only line and empty (on file open)
    // - Other lines: newly created empty lines (but not if we're focusing programmatically)
    LaunchedEffect(line.id, line.expression) {
        if (line.expression.isEmpty() && !shouldFocus) {
            if (lineNumber == 1) {
                // Auto-focus first line only if it's the only line
                // Check will be done by parent, but we'll focus anyway for UX
                delay(50)
                focusRequester.requestFocus()
                // No leading space on line 1, cursor at position 0
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0)
                )
            } else if (lineNumber > 1) {
                // Auto-focus newly created lines after line 1
                delay(50)
                focusRequester.requestFocus()
                // Position cursor after the leading space
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(1)
                )
            }
        }
    }

    // Handle programmatic focus requests (from navigation or deletion)
    LaunchedEffect(shouldFocus, focusCursorPos) {
        if (shouldFocus && focusCursorPos != null) {
            delay(150) // Delay to ensure UI is stable and other effects don't interfere
            focusRequester.requestFocus()

            // Set cursor position - focusCursorPos is the desired position in the expression
            // We need to map this to the displayText position
            val actualPos = if (line.expression.isEmpty()) {
                // Line 1 has no leading space, others have space at position 0
                if (lineNumber == 1) 0 else 1
            } else {
                focusCursorPos.coerceIn(0, textFieldValue.text.length) // Ensure within bounds
            }

            textFieldValue = textFieldValue.copy(
                selection = TextRange(actualPos)
            )
            // onFocused() will be called by onFocusChanged when focus is gained
        }
    }

    // Handle toolbar text insertion
    LaunchedEffect(insertTextRequest) {
        if (insertTextRequest != null) {
            val currentSelection = textFieldValue.selection
            val currentText = textFieldValue.text
            val cursorPosition = currentSelection.start

            // Insert the text at cursor position
            val newText = currentText.substring(0, cursorPosition) + insertTextRequest + currentText.substring(cursorPosition)
            val newCursorPosition = cursorPosition + insertTextRequest.length

            textFieldValue = TextFieldValue(
                annotatedString = applySyntaxHighlighting(
                    newText,
                    numberColor,
                    variableColor,
                    operatorColor,
                    percentColor,
                    commentColor,
                    defaultTextColor
                ),
                selection = TextRange(newCursorPosition)
            )

            val trimmedText = newText.trim()
            if (trimmedText != line.expression) {
                onValueChange(trimmedText)
            }

            onInsertHandled()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.Top
    ) {
        // Line number area with delete icon
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp)
        ) {
            // Delete icon at far left (shows after delay)
            if (showDeleteIcon) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(0.dp)
                        .width(18.dp)
                        .height(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete line",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }

            // Line number at right
            Text(
                text = "$lineNumber",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FiraCodeFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
            )
        }

        // Vertical divider after line gutter
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Editor with wrapping and autocomplete
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column {
                BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val filteredText = newValue.text.replace("\n", "")

                    // Handle Enter key
                    if (newValue.text.contains("\n")) {
                        onEnter()
                        return@BasicTextField
                    }

                    // Detect backspace deleting the leading space (empty line deletion)
                    if (filteredText.isEmpty() && lineNumber > 1) {
                        onDelete()
                        return@BasicTextField
                    }

                    // Strip leading space if user added real content
                    val actualText = if (filteredText.startsWith(" ") && filteredText.length > 1) {
                        filteredText.substring(1)
                    } else if (filteredText == " ") {
                        "" // Just the space, treat as empty
                    } else {
                        filteredText
                    }

                    // Re-add leading space if text becomes empty (but not for line 1)
                    val displayText = if (actualText.isEmpty() && lineNumber > 1) " " else actualText

                    previousSelection = newValue.selection
                    textFieldValue = newValue.copy(
                        annotatedString = applySyntaxHighlighting(
                            displayText,
                            numberColor,
                            variableColor,
                            operatorColor,
                            percentColor,
                            commentColor,
                            defaultTextColor
                        )
                    )
                    onValueChange(actualText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onFocused()
                        } else {
                            onBlur()
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        // Only handle KEY_DOWN to avoid double triggering
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    // Only navigate if cursor is at the start
                                    // Line 1: position 0, Others: position 0 or 1 (before/on leading space)
                                    val atStart = if (lineNumber == 1) {
                                        textFieldValue.selection.start == 0
                                    } else {
                                        textFieldValue.selection.start <= 1
                                    }
                                    if (atStart && !shouldFocus) {
                                        onNavigateUp()
                                        true // Consume the event
                                    } else {
                                        false
                                    }
                                }
                                Key.DirectionDown -> {
                                    // Only navigate if cursor is at the end
                                    if (textFieldValue.selection.start >= textFieldValue.text.length && !shouldFocus) {
                                        onNavigateDown()
                                        true // Consume the event
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FiraCodeFamily
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onEnter() }),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.trim().isEmpty() && lineNumber == 1) {
                        Text(
                            "Type here...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FiraCodeFamily
                            )
                        )
                    }
                    innerTextField()
                }
            )

            // Autocomplete suggestions dropdown
            if (suggestions.isNotEmpty() && isFocused) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 4.dp)
                ) {
                    suggestions.take(5).forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Replace current word with suggestion
                                    val cursorPos = textFieldValue.selection.start
                                    val text = textFieldValue.text
                                    val beforeCursor = text.substring(0, cursorPos)
                                    val wordStart = beforeCursor.lastIndexOfAny(
                                        charArrayOf(' ', '+', '-', '*', '/', '×', '÷', '(', ')', '=', ',')
                                    ) + 1
                                    val newText = text.substring(0, wordStart) + suggestion + text.substring(cursorPos)
                                    val newCursorPos = wordStart + suggestion.length

                                    textFieldValue = TextFieldValue(
                                        annotatedString = applySyntaxHighlighting(
                                            newText,
                                            numberColor,
                                            variableColor,
                                            operatorColor,
                                            percentColor,
                                            commentColor,
                                            defaultTextColor
                                        ),
                                        selection = TextRange(newCursorPos)
                                    )
                                    onValueChange(newText)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FiraCodeFamily),
                            color = variableColor
                        )
                    }
                }
            }
            }
        }

        // Vertical divider before result gutter
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Result
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val resultColor = if (line.result == "Err") {
                MaterialTheme.colorScheme.error
            } else {
                com.vishaltelangre.nerdcalci.ui.theme.ResultSuccess
            }
            Text(
                text = line.result,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FiraCodeFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = resultColor
            )
        }
    }
}
