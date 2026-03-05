package jafproductions.jaflist.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jafproductions.jaflist.models.Folder
import jafproductions.jaflist.services.SyncStatus
import jafproductions.jaflist.viewmodels.AppViewModel
import jafproductions.jaflist.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel
) {
    val appData by appViewModel.appData.collectAsState()
    val syncStatus by appViewModel.syncStatus.collectAsState()
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JAFList") },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SyncStatusIcon(syncStatus = syncStatus)
                        IconButton(onClick = { authViewModel.signOut() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        onClick = { navController.navigate("folder/${folder.id}") }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteFolderItem(
    folder: Folder,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {
        FolderItemRow(folder = folder, onClick = onClick)
    }
}

@Composable
fun FolderItemRow(folder: Folder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
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
