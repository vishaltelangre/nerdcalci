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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vishaltelangre.nerdcalci.core.Constants

/**
 * Reusable dialog for renaming a file.
 *
 * @param currentName Current file name to show in the text field
 * @param onDismiss Callback when dialog is dismissed without renaming
 * @param onConfirm Callback with new name when rename is confirmed
 */
@Composable
fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var renameText by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    fun confirmRename() {
        if (renameText.isNotBlank()) {
            onConfirm(renameText.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column {
                TextField(
                    value = renameText,
                    onValueChange = { newValue ->
                        // Filter out newlines and limit length
                        val filtered = newValue.replace("\n", "")
                        renameText = if (filtered.length <= Constants.MAX_FILE_NAME_LENGTH) {
                            filtered
                        } else {
                            filtered.take(Constants.MAX_FILE_NAME_LENGTH)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { confirmRename() }
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Text(
                    text = "${renameText.length}/${Constants.MAX_FILE_NAME_LENGTH}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmRename() },
                enabled = renameText.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Auto-focus the text field when dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
