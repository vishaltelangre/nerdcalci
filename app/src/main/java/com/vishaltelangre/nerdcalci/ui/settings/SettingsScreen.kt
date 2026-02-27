package com.vishaltelangre.nerdcalci.ui.settings

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.data.backup.BackupFileInfo
import com.vishaltelangre.nerdcalci.data.backup.BackupFrequency
import com.vishaltelangre.nerdcalci.data.backup.BackupLocationMode
import com.vishaltelangre.nerdcalci.ui.components.RestoreBackupListDialog
import com.vishaltelangre.nerdcalci.ui.components.RestoreConfirmDialog
import com.vishaltelangre.nerdcalci.ui.components.RestoreSourceDialog
import com.vishaltelangre.nerdcalci.ui.components.formatBackupLocationText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    autoBackupEnabled: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    backupFrequency: BackupFrequency,
    onBackupFrequencyChange: (BackupFrequency) -> Unit,
    backupLocationMode: BackupLocationMode,
    backupLocationSummary: String,
    onChooseBackupFolder: () -> Unit,
    onUseAppStorageLocation: () -> Unit,
    onBackupNow: () -> Unit,
    onBackupNowAtDifferentLocation: () -> Unit,
    lastBackupAt: Long?,
    availableBackups: List<BackupFileInfo>,
    onRestoreBackup: (BackupFileInfo) -> Unit,
    onRestoreFromDifferentLocation: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showRestoreActionDialog by remember { mutableStateOf(false) }
    var showBackupNowActionDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = packageInfo.longVersionCode
            "v$versionName ($versionCode)"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    val restoreSubtitle = if (availableBackups.isEmpty()) {
        "Use current location or choose a different file"
    } else {
        "Use current location (contains ${availableBackups.size} backups) or choose a different file"
    }
    val currentLocationText = formatBackupLocationText(
        mode = backupLocationMode,
        customFolderSummary = backupLocationSummary
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Theme")

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val options = listOf(
                    Triple("light", "Light", Icons.Default.LightMode),
                    Triple("dark", "Dark", Icons.Default.DarkMode),
                    Triple("system", "System", Icons.Default.DarkMode)
                )

                options.forEachIndexed { index, (value, label, icon) ->
                    SegmentedButton(
                        selected = currentTheme == value,
                        onClick = { onThemeChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = currentTheme == value) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "Data")

            SettingsToggleItem(
                icon = Icons.Default.Backup,
                title = "Automatic backup",
                subtitle = null,
                checked = autoBackupEnabled,
                onCheckedChange = onAutoBackupEnabledChange
            )

            if (autoBackupEnabled) {
                SettingsDropdownItem(
                    icon = Icons.Default.Schedule,
                    title = "Backup frequency",
                    value = if (backupFrequency == BackupFrequency.DAILY) "Daily" else "Weekly",
                    onClick = { showFrequencyDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Backup location",
                    subtitle = currentLocationText,
                    onClick = { showLocationDialog = true }
                )

            }

            Spacer(modifier = Modifier.height(10.dp))
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Back up now",
                subtitle = if (lastBackupAt != null) {
                    "Last backup ${formatRelativeTime(lastBackupAt)}"
                } else {
                    "Create a backup immediately"
                },
                onClick = { showBackupNowActionDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Restore from backup",
                subtitle = restoreSubtitle,
                onClick = { showRestoreActionDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "Help")
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Help",
                subtitle = "View the calculator usage guide and documentation",
                onClick = onHelp
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = appVersion,
                onClick = null
            )

            SettingsItem(
                icon = Icons.Default.Code,
                title = "Source Code",
                subtitle = Constants.SOURCE_CODE_URL.removePrefix("https://"),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SOURCE_CODE_URL))
                    context.startActivity(intent)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Constants.DEVELOPER_NAME,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/vishaltelangre"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = "Buy me a coffee",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SettingsItem(
                icon = Icons.Default.Info,
                title = "License",
                subtitle = Constants.LICENSE,
                onClick = null
            )
        }
    }

    RestoreBackupListDialog(
        visible = showRestoreDialog,
        currentLocationText = currentLocationText,
        backups = availableBackups,
        onDismiss = { showRestoreDialog = false },
        onBackupSelected = { backup -> pendingRestoreBackup = backup }
    )

    RestoreConfirmDialog(
        visible = pendingRestoreBackup != null,
        onDismiss = { pendingRestoreBackup = null },
        onConfirm = {
            val selected = pendingRestoreBackup
            pendingRestoreBackup = null
            showRestoreDialog = false
            if (selected != null) {
                onRestoreBackup(selected)
            }
        }
    )

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Backup location") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Button(
                        onClick = {
                            showLocationDialog = false
                            onUseAppStorageLocation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = backupLocationMode != BackupLocationMode.APP_STORAGE
                    ) {
                        Text(if (backupLocationMode == BackupLocationMode.APP_STORAGE) "Using default app storage" else "Use default app storage")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showLocationDialog = false
                            onChooseBackupFolder()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose custom folder")
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
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text("Backup frequency") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            showFrequencyDialog = false
                            onBackupFrequencyChange(BackupFrequency.DAILY)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (backupFrequency == BackupFrequency.DAILY) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (backupFrequency == BackupFrequency.DAILY) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            showFrequencyDialog = false
                            onBackupFrequencyChange(BackupFrequency.WEEKLY)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (backupFrequency == BackupFrequency.WEEKLY) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (backupFrequency == BackupFrequency.WEEKLY) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showBackupNowActionDialog) {
        AlertDialog(
            onDismissRequest = { showBackupNowActionDialog = false },
            title = { Text("Back up now") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showBackupNowActionDialog = false
                            onBackupNow()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use current backup location")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showBackupNowActionDialog = false
                            onBackupNowAtDifferentLocation()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose different location")
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
                TextButton(onClick = { showBackupNowActionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    RestoreSourceDialog(
        visible = showRestoreActionDialog,
        hasBackupsInCurrentLocation = availableBackups.isNotEmpty(),
        currentLocationText = currentLocationText,
        onDismiss = { showRestoreActionDialog = false },
        onUseCurrentLocation = {
            showRestoreActionDialog = false
            showRestoreDialog = true
        },
        onChooseDifferentFile = {
            showRestoreActionDialog = false
            onRestoreFromDifferentLocation()
        }
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsDropdownItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(value: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        value,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

private fun labelValueText(label: String, value: String) = buildAnnotatedString {
    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(label)
    }
    append(" ")
    append(value)
}
