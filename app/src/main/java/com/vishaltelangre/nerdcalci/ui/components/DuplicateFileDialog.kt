package com.vishaltelangre.nerdcalci.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vishaltelangre.nerdcalci.core.Constants

/**
 * Reusable dialog for duplicating a file with a new name.
 *
 * @param originalName Original file name to duplicate from
 * @param onDismiss Callback when dialog is dismissed without duplicating
 * @param onConfirm Callback with new name when duplicate is confirmed
 */
@Composable
fun DuplicateFileDialog(
    originalName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val suggestedName = "Copy of $originalName".take(Constants.MAX_FILE_NAME_LENGTH)
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = suggestedName,
                selection = TextRange(0, suggestedName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    fun confirmDuplicate() {
        val trimmedText = textFieldValue.text.trim()
        if (trimmedText.isNotBlank()) {
            onConfirm(trimmedText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate File") },
        text = {
            Column {
                TextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        // Filter out newlines and limit length
                        val filtered = newValue.text.replace("\n", "")
                        val finalText = if (filtered.length <= Constants.MAX_FILE_NAME_LENGTH) {
                            filtered
                        } else {
                            filtered.take(Constants.MAX_FILE_NAME_LENGTH)
                        }
                        textFieldValue = newValue.copy(
                            text = finalText,
                            selection = TextRange(newValue.selection.start.coerceAtMost(finalText.length))
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { confirmDuplicate() }
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Text(
                    text = "${textFieldValue.text.length}/${Constants.MAX_FILE_NAME_LENGTH}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmDuplicate() },
                enabled = textFieldValue.text.isNotBlank()
            ) {
                Text("Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Auto-focus the text field with text selected when dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
