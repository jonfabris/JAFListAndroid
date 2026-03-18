package jafproductions.jaflist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jafproductions.jaflist.services.BackupInfo
import jafproductions.jaflist.ui.theme.JAFListTheme
import jafproductions.jaflist.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val backups = remember { appViewModel.availableBackups() }
    RestoreBackupScreenContent(
        backups = backups,
        onRestore = { backup ->
            appViewModel.restore(backup)
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreBackupScreenContent(
    backups: List<BackupInfo>,
    onRestore: (BackupInfo) -> Unit,
    onBack: () -> Unit
) {
    var confirmingBackup by remember { mutableStateOf<BackupInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (backups.isEmpty()) {
            Text(
                text = "No backups yet. Backups are created automatically once a week.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(backups, key = { it.id }) { backup ->
                    ListItem(
                        headlineContent = { Text(backup.displayName) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { confirmingBackup = backup }
                    )
                }
            }
        }
    }

    confirmingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { confirmingBackup = null },
            title = { Text("Restore this backup?") },
            text = {
                Text("Your current data will be replaced with the backup from ${backup.displayName}. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestore(backup)
                        confirmingBackup = null
                    }
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingBackup = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "RestoreBackupScreen - Empty", showSystemUi = true)
@Composable
private fun RestoreBackupEmptyPreview() {
    JAFListTheme(dynamicColor = false) {
        RestoreBackupScreenContent(backups = emptyList(), onRestore = {}, onBack = {})
    }
}

@Preview(showBackground = true, name = "RestoreBackupScreen - With Backups", showSystemUi = true)
@Composable
private fun RestoreBackupWithBackupsPreview() {
    val now = java.util.Date()
    val week = 7L * 24 * 60 * 60 * 1000
    val backups = listOf(
        BackupInfo(file = java.io.File("/preview/backup1.json"), date = java.util.Date(now.time)),
        BackupInfo(file = java.io.File("/preview/backup2.json"), date = java.util.Date(now.time - week)),
        BackupInfo(file = java.io.File("/preview/backup3.json"), date = java.util.Date(now.time - week * 2)),
    )
    JAFListTheme(dynamicColor = false) {
        RestoreBackupScreenContent(backups = backups, onRestore = {}, onBack = {})
    }
}
