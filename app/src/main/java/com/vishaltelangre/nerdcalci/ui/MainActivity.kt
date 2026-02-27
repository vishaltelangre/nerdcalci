package com.vishaltelangre.nerdcalci.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.vishaltelangre.nerdcalci.core.Constants
import com.vishaltelangre.nerdcalci.data.backup.AutoBackupScheduler
import com.vishaltelangre.nerdcalci.data.backup.BackupFileInfo
import com.vishaltelangre.nerdcalci.data.backup.BackupLocationMode
import com.vishaltelangre.nerdcalci.data.backup.BackupManager
import com.vishaltelangre.nerdcalci.data.local.AppDatabase
import com.vishaltelangre.nerdcalci.data.local.DatabaseMigrations
import com.vishaltelangre.nerdcalci.di.CalculatorViewModelFactory
import com.vishaltelangre.nerdcalci.ui.calculator.CalculatorScreen
import com.vishaltelangre.nerdcalci.ui.calculator.CalculatorViewModel
import com.vishaltelangre.nerdcalci.ui.components.formatBackupLocationText
import com.vishaltelangre.nerdcalci.ui.components.RestoreBackupListDialog
import com.vishaltelangre.nerdcalci.ui.components.RestoreConfirmDialog
import com.vishaltelangre.nerdcalci.ui.components.RestoreSourceDialog
import com.vishaltelangre.nerdcalci.ui.help.HelpScreen
import com.vishaltelangre.nerdcalci.ui.home.HomeScreen
import com.vishaltelangre.nerdcalci.ui.settings.SettingsScreen
import com.vishaltelangre.nerdcalci.ui.theme.NerdCalciTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, Constants.DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()

        val prefs = getSharedPreferences(BackupManager.PREFS_NAME, MODE_PRIVATE)
        AutoBackupScheduler.sync(applicationContext, prefs)

        val viewModel: CalculatorViewModel by viewModels {
            CalculatorViewModelFactory(db.calculatorDao(), prefs)
        }

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val isDarkTheme = when (currentTheme) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            NerdCalciTheme(darkTheme = isDarkTheme) {
                // Update system bar appearance to match theme
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as ComponentActivity).window
                        // Use WindowInsetsController for system bar appearance
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !isDarkTheme
                            isAppearanceLightNavigationBars = !isDarkTheme
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    CalculatorNavHost(viewModel)
                }
            }
        }
    }
}

@Composable
fun CalculatorNavHost(viewModel: CalculatorViewModel, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val backupLocationMode by viewModel.backupLocationMode.collectAsState()
    val customBackupFolderUri by viewModel.customBackupFolderUri.collectAsState()
    val availableBackups by viewModel.availableBackups.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    val customBackupFolderSummary = remember(customBackupFolderUri) {
        customBackupFolderUri?.let { uriString ->
            val parsed = Uri.parse(uriString)
            Uri.decode(parsed.lastPathSegment ?: "")
                .ifBlank { "Custom folder selected" }
        } ?: "Not selected"
    }
    val currentLocationText = formatBackupLocationText(
        mode = backupLocationMode,
        customFolderSummary = customBackupFolderSummary
    )
    var showHomeRestoreActionDialog by remember { mutableStateOf(false) }
    var showHomeRestoreListDialog by remember { mutableStateOf(false) }
    var pendingHomeRestoreBackup by remember { mutableStateOf<BackupFileInfo?>(null) }

    // Export launcher - creates a one-off ZIP file at user-chosen location
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(Constants.EXPORT_MIME_TYPE)
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val result = viewModel.exportAllFiles(context, it)
                result.onSuccess { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Import launcher - opens a ZIP file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val result = viewModel.importFiles(context, it)
                result.onSuccess { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Import failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (_: SecurityException) {
            }
            viewModel.setCustomBackupFolder(context, it)
            Toast.makeText(context, "Custom backup folder saved", Toast.LENGTH_SHORT).show()
        }
    }

    // Reusable slide animations for detail screens
    val slideInFromRight = slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it })
    val slideOutToLeft = slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it / 3 })
    val slideInFromLeft = slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it / 3 })
    val slideOutToRight = slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it })

    NavHost(navController = navController, startDestination = "home") {
        // Home Screen
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onFileClick = { fileId -> navController.navigate("editor/$fileId") },
                onSettingsClick = { navController.navigate("settings") },
                onHelpClick = { navController.navigate("help") },
                onRestoreClick = {
                    viewModel.refreshBackups(context)
                    showHomeRestoreActionDialog = true
                }
            )
        }

        // Calculator Editor Screen
        composable(
            "editor/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.LongType }),
            enterTransition = { slideInFromRight },
            exitTransition = { slideOutToLeft },
            popEnterTransition = { slideInFromLeft },
            popExitTransition = { slideOutToRight }
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getLong("fileId") ?: 0L
            CalculatorScreen(
                fileId = fileId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onHelp = { navController.navigate("help") },
                onNavigateToFile = { newFileId ->
                    navController.navigate("editor/$newFileId") {
                        popUpTo("editor/$fileId") { inclusive = true }
                    }
                }
            )
        }

        // Settings Screen
        composable(
            "settings",
            enterTransition = { slideInFromRight },
            exitTransition = { slideOutToLeft },
            popEnterTransition = { slideInFromLeft },
            popExitTransition = { slideOutToRight }
        ) {
            LaunchedEffect(Unit) {
                viewModel.refreshBackups(context)
            }
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = { theme -> viewModel.setTheme(theme) },
                autoBackupEnabled = autoBackupEnabled,
                onAutoBackupEnabledChange = { enabled -> viewModel.setAutoBackupEnabled(context, enabled) },
                backupFrequency = backupFrequency,
                onBackupFrequencyChange = { frequency -> viewModel.setBackupFrequency(context, frequency) },
                backupLocationMode = backupLocationMode,
                backupLocationSummary = customBackupFolderSummary,
                onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                onUseAppStorageLocation = { viewModel.setBackupLocationToAppStorage(context) },
                onBackupNow = {
                    coroutineScope.launch {
                        val result = viewModel.backupNow(context)
                        result.onSuccess { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(context, "Backup failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onBackupNowAtDifferentLocation = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(Date())
                    val filename = "nerdcalci_backup_$timestamp.zip"
                    exportLauncher.launch(filename)
                },
                lastBackupAt = lastBackupAt,
                availableBackups = availableBackups,
                onRestoreBackup = { backup ->
                    coroutineScope.launch {
                        val result = viewModel.restoreFromBackup(context, backup)
                        result.onSuccess { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }.onFailure { error ->
                            Toast.makeText(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onRestoreFromDifferentLocation = { importLauncher.launch(arrayOf("application/zip")) },
                onHelp = { navController.navigate("help") },
                onBack = { navController.popBackStack() }
            )
        }

        // Help Screen
        composable(
            "help",
            enterTransition = { slideInFromRight },
            exitTransition = { slideOutToLeft },
            popEnterTransition = { slideInFromLeft },
            popExitTransition = { slideOutToRight }
        ) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
    }

    RestoreSourceDialog(
        visible = showHomeRestoreActionDialog,
        hasBackupsInCurrentLocation = availableBackups.isNotEmpty(),
        currentLocationText = currentLocationText,
        onDismiss = { showHomeRestoreActionDialog = false },
        onUseCurrentLocation = {
            showHomeRestoreActionDialog = false
            showHomeRestoreListDialog = true
        },
        onChooseDifferentFile = {
            showHomeRestoreActionDialog = false
            importLauncher.launch(arrayOf("application/zip"))
        }
    )

    RestoreBackupListDialog(
        visible = showHomeRestoreListDialog,
        currentLocationText = currentLocationText,
        backups = availableBackups,
        onDismiss = { showHomeRestoreListDialog = false },
        onBackupSelected = { backup -> pendingHomeRestoreBackup = backup }
    )

    RestoreConfirmDialog(
        visible = pendingHomeRestoreBackup != null,
        onDismiss = { pendingHomeRestoreBackup = null },
        onConfirm = {
            val selected = pendingHomeRestoreBackup
            pendingHomeRestoreBackup = null
            showHomeRestoreListDialog = false
            if (selected != null) {
                coroutineScope.launch {
                    val result = viewModel.restoreFromBackup(context, selected)
                    result.onSuccess { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }.onFailure { error ->
                        Toast.makeText(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
}
