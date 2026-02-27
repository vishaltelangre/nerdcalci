package com.vishaltelangre.nerdcalci.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vishaltelangre.nerdcalci.data.backup.BackupFileInfo
import com.vishaltelangre.nerdcalci.data.backup.BackupLocationMode
import java.util.Date

@Composable
fun RestoreSourceDialog(
    visible: Boolean,
    hasBackupsInCurrentLocation: Boolean,
    currentLocationText: String,
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onChooseDifferentFile: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onUseCurrentLocation,
                    enabled = hasBackupsInCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasBackupsInCurrentLocation) "Use current backup location" else "No backups in current location")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onChooseDifferentFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose a different backup file")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = labelValueText("Current location:", currentLocationText),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RestoreBackupListDialog(
    visible: Boolean,
    currentLocationText: String,
    backups: List<BackupFileInfo>,
    onDismiss: () -> Unit,
    onBackupSelected: (BackupFileInfo) -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = labelValueText("Location:", currentLocationText),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                backups.forEach { backup ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackupSelected(backup) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backup.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Created at: ${formatFriendlyDateTime(backup.lastModified)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RestoreConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm restore") },
        text = { Text("This will overwrite your existing data with the selected backup. This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun labelValueText(label: String, value: String) = buildAnnotatedString {
    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(label)
    }
    append(" ")
    append(value)
}

private fun formatFriendlyDateTime(value: Long): String {
    return DateFormat.format("MMM d, yyyy h:mm a", Date(value)).toString()
}

fun formatBackupLocationText(
    mode: BackupLocationMode,
    customFolderSummary: String
): String {
    return if (mode == BackupLocationMode.APP_STORAGE) {
        "Default app storage"
    } else {
        "Custom folder (${shortenPath(customFolderSummary)})"
    }
}

private fun shortenPath(path: String): String {
    val trimmed = path.trim()
    if (trimmed.isEmpty()) return path

    val segments = trimmed.split("/").filter { it.isNotBlank() }
    if (segments.size > 3) {
        return "${segments[0]}/${segments[1]}/.../${segments.last()}"
    }

    if (trimmed.length > 40) {
        return "${trimmed.take(18)}...${trimmed.takeLast(16)}"
    }

    return trimmed
}
