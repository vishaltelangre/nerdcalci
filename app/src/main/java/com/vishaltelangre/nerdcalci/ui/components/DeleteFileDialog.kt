package com.vishaltelangre.nerdcalci.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Reusable confirmation dialog for deleting a file.
 *
 * @param fileName Name of the file to be deleted (shown in the warning message)
 * @param onDismiss Callback when dialog is dismissed without deleting
 * @param onConfirm Callback when user confirms deletion
 */
@Composable
fun DeleteFileDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete file?") },
        text = {
            Text("This will permanently delete \"$fileName\" and all its contents. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
