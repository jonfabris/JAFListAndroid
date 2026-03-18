package jafproductions.jaflist.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import jafproductions.jaflist.models.Folder
import jafproductions.jaflist.services.SyncStatus
import jafproductions.jaflist.ui.theme.JAFListTheme
import jafproductions.jaflist.viewmodels.AppViewModel
import jafproductions.jaflist.viewmodels.AuthViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel
) {
    val appData by appViewModel.appData.collectAsState()
    val syncStatus by appViewModel.syncStatus.collectAsState()
    val lastSyncDate by appViewModel.lastSyncDate.collectAsState()
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var renameFolderText by remember { mutableStateOf("") }
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    val lastSyncText = lastSyncDate?.let { ms ->
        val formatter = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault())
        "Last synced: ${formatter.format(java.util.Date(ms))}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JAFList") },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SyncStatusIcon(syncStatus = syncStatus)
                        IconButton(onClick = { showSignOutConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                        IconButton(onClick = { navController.navigate("restore") }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Restore Backup"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        newFolderName = ""
                        showAddFolderDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Folder"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (appData.folders.isEmpty()) {
                    item {
                        Text(
                            text = "No folders yet",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(
                        items = appData.folders,
                        key = { it.id }
                    ) { folder ->
                        SwipeToDeleteFolderItem(
                            folder = folder,
                            onDelete = { appViewModel.deleteFolder(folder.id) },
                            onRename = {
                                renamingFolderId = folder.id
                                renameFolderText = folder.name
                            },
                            onClick = { navController.navigate("folder/${folder.id}") }
                        )
                    }
                }
            }

            if (lastSyncText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lastSyncText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                appViewModel.addFolder(newFolderName.trim())
                                showAddFolderDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (renamingFolderId != null) {
            AlertDialog(
                onDismissRequest = { renamingFolderId = null },
                title = { Text("Rename Folder") },
                text = {
                    OutlinedTextField(
                        value = renameFolderText,
                        onValueChange = { renameFolderText = it },
                        label = { Text("Folder Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = renamingFolderId
                            if (id != null && renameFolderText.isNotBlank()) {
                                appViewModel.renameFolder(id, renameFolderText.trim())
                            }
                            renamingFolderId = null
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renamingFolderId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSignOutConfirmation) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmation = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutConfirmation = false
                        authViewModel.signOut()
                    }) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private const val FOLDER_BUTTON_WIDTH_DP = 80f
private const val FOLDER_REVEAL_DP = FOLDER_BUTTON_WIDTH_DP * 2
private const val FOLDER_REVEAL_THRESHOLD_RAW = FOLDER_REVEAL_DP * 3f

@Composable
fun SwipeToDeleteFolderItem(
    folder: Folder,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = tween(durationMillis = 150),
        label = "folder_swipe_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // Background action buttons anchored to the right
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(48.dp)
        ) {
            // Rename (orange)
            Box(
                modifier = Modifier
                    .width(FOLDER_BUTTON_WIDTH_DP.dp)
                    .height(48.dp)
                    .background(Color(0xFFE65100))
                    .clickable {
                        rawOffsetX = 0f
                        onRename()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = Color.White
                )
            }
            // Delete (red)
            Box(
                modifier = Modifier
                    .width(FOLDER_BUTTON_WIDTH_DP.dp)
                    .height(48.dp)
                    .background(Color(0xFFD32F2F))
                    .clickable {
                        rawOffsetX = 0f
                        showDeleteConfirmation = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }

        // Foreground sliding content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(folder.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val revealed = -rawOffsetX > FOLDER_REVEAL_THRESHOLD_RAW / 2f
                            rawOffsetX = if (revealed) -FOLDER_REVEAL_THRESHOLD_RAW else 0f
                        },
                        onDragCancel = { rawOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffsetX = (rawOffsetX + dragAmount).coerceIn(-FOLDER_REVEAL_THRESHOLD_RAW, 0f)
                        }
                    )
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            FolderItemRow(folder = folder, onClick = onClick)
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Folder") },
            text = { Text("Delete \"${folder.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FolderItemRow(folder: Folder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SyncStatusIcon(syncStatus: SyncStatus) {
    when (syncStatus) {
        SyncStatus.IDLE -> { /* no icon */ }
        SyncStatus.SYNCING -> {
            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Syncing",
                tint = Color(0xFF1976D2),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        SyncStatus.SYNCED -> {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Synced",
                tint = Color(0xFF388E3C),
                modifier = Modifier.size(24.dp)
            )
        }
        SyncStatus.OFFLINE -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = Color(0xFFF57C00),
                modifier = Modifier.size(24.dp)
            )
        }
        SyncStatus.ERROR -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Error",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "SyncStatusIcon - All States")
@Composable
private fun SyncStatusIconPreview() {
    JAFListTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SyncStatusIcon(SyncStatus.IDLE)
            SyncStatusIcon(SyncStatus.SYNCING)
            SyncStatusIcon(SyncStatus.SYNCED)
            SyncStatusIcon(SyncStatus.OFFLINE)
            SyncStatusIcon(SyncStatus.ERROR)
        }
    }
}

@Preview(showBackground = true, name = "FolderItemRow")
@Composable
private fun FolderItemRowPreview() {
    JAFListTheme(dynamicColor = false) {
        FolderItemRow(folder = Folder(id = "1", name = "Groceries"), onClick = {})
    }
}

@Preview(showBackground = true, name = "SwipeToDeleteFolderItem")
@Composable
private fun SwipeToDeleteFolderItemPreview() {
    JAFListTheme(dynamicColor = false) {
        SwipeToDeleteFolderItem(
            folder = Folder(id = "1", name = "Groceries"),
            onDelete = {}, onRename = {}, onClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "MainScreen - With Folders", showSystemUi = true)
@Composable
private fun MainScreenPreview() {
    val folders = listOf(
        Folder(id = "1", name = "Groceries"),
        Folder(id = "2", name = "Work Tasks"),
        Folder(id = "3", name = "Home Improvement"),
    )
    JAFListTheme(dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("JAFList") },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SyncStatusIcon(SyncStatus.SYNCED)
                            IconButton(onClick = {}) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = "Sign Out")
                            }
                            IconButton(onClick = {}) {
                                Icon(imageVector = Icons.Default.History, contentDescription = "Restore Backup")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Folder")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(folders, key = { it.id }) { folder ->
                        SwipeToDeleteFolderItem(folder = folder, onDelete = {}, onRename = {}, onClick = {})
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Last synced: Mar 18, 2026 at 9:00 AM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "MainScreen - Empty", showSystemUi = true)
@Composable
private fun MainScreenEmptyPreview() {
    JAFListTheme(dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("JAFList") },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Folder")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "No folders yet",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
