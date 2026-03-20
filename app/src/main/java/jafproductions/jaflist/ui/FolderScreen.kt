package jafproductions.jaflist.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import jafproductions.jaflist.models.Folder
import jafproductions.jaflist.models.TodoItem
import jafproductions.jaflist.ui.theme.JAFListTheme
import jafproductions.jaflist.viewmodels.AppViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: String,
    parentFolderId: String? = null,
    appViewModel: AppViewModel,
    navController: NavController
) {
    val appData by appViewModel.appData.collectAsState()

    val folder = if (parentFolderId != null) {
        appData.folders.find { it.id == parentFolderId }?.subfolders?.find { it.id == folderId }
    } else {
        appData.folders.find { it.id == folderId }
    }

    if (folder == null) {
        navController.popBackStack()
        return
    }

    val visibleItems = remember(folder.items) {
        flatVisibleItems(folder.items, depth = 0)
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var showAddSubfolderDialog by remember { mutableStateOf(false) }
    var newSubfolderName by remember { mutableStateOf("") }
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var renameFolderText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Subfolder section — only shown for top-level folders
            if (parentFolderId == null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Folders",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                newSubfolderName = ""
                                showAddSubfolderDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subfolder",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                items(
                    items = folder.subfolders,
                    key = { "sub_${it.id}" }
                ) { subfolder ->
                    SubfolderItemRow(
                        subfolder = subfolder,
                        onDelete = { appViewModel.deleteSubfolder(folderId, subfolder.id) },
                        onRename = {
                            renamingFolderId = subfolder.id
                            renameFolderText = subfolder.name
                        },
                        onClick = { navController.navigate("subfolder/$folderId/${subfolder.id}") }
                    )
                }

            }

            item {
                if (parentFolderId == null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            newItemText = ""
                            showAddItemDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            items(
                items = visibleItems,
                key = { (item, _) -> item.id }
            ) { (item, depth) ->
                TodoItemRow(
                    item = item,
                    folderId = folderId,
                    depth = depth,
                    appViewModel = appViewModel
                )
            }
        }

        // Add Item dialog
        if (showAddItemDialog) {
            Dialog(onDismissRequest = { showAddItemDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(text = "New Item", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        OutlinedTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            label = { Text("Item Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    if (newItemText.isNotBlank()) {
                                        appViewModel.addItem(folderId, newItemText.trim())
                                        newItemText = ""
                                    }
                                }
                            ) { Text("Save & Add") }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    if (newItemText.isNotBlank()) {
                                        appViewModel.addItem(folderId, newItemText.trim())
                                        showAddItemDialog = false
                                    }
                                }
                            ) { Text("Add") }
                        }
                    }
                }
            }
        }

        // Add Subfolder dialog
        if (showAddSubfolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddSubfolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = newSubfolderName,
                        onValueChange = { newSubfolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newSubfolderName.isNotBlank()) {
                                appViewModel.addSubfolder(folderId, newSubfolderName.trim())
                                showAddSubfolderDialog = false
                            }
                        }
                    ) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubfolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Rename dialog (used for both current folder and subfolders)
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
                    ) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { renamingFolderId = null }) { Text("Cancel") }
                }
            )
        }
    }
}

private const val SUBFOLDER_BUTTON_WIDTH_DP = 80f
private const val SUBFOLDER_REVEAL_DP = SUBFOLDER_BUTTON_WIDTH_DP * 2
private const val SUBFOLDER_REVEAL_THRESHOLD_RAW = SUBFOLDER_REVEAL_DP * 3f

@Composable
fun SubfolderItemRow(
    subfolder: Folder,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = tween(durationMillis = 150),
        label = "subfolder_swipe_offset"
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
                    .width(SUBFOLDER_BUTTON_WIDTH_DP.dp)
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
                    .width(SUBFOLDER_BUTTON_WIDTH_DP.dp)
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
                .pointerInput(subfolder.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val revealed = -rawOffsetX > SUBFOLDER_REVEAL_THRESHOLD_RAW / 2f
                            rawOffsetX = if (revealed) -SUBFOLDER_REVEAL_THRESHOLD_RAW else 0f
                        },
                        onDragCancel = { rawOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffsetX = (rawOffsetX + dragAmount).coerceIn(-SUBFOLDER_REVEAL_THRESHOLD_RAW, 0f)
                        }
                    )
                },
            color = MaterialTheme.colorScheme.surface
        ) {
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
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = subfolder.name,
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
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete \"${subfolder.name}\"?") },
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

fun flatVisibleItems(
    items: List<TodoItem>,
    depth: Int
): List<Pair<TodoItem, Int>> {
    val result = mutableListOf<Pair<TodoItem, Int>>()
    for (item in items) {
        result.add(Pair(item, depth))
        if (item.isExpanded && item.children.isNotEmpty()) {
            result.addAll(flatVisibleItems(item.children, depth + 1))
        }
    }
    return result
}

@Preview(showBackground = true, name = "SubfolderItemRow")
@Composable
private fun SubfolderItemRowPreview() {
    JAFListTheme(dynamicColor = false) {
        SubfolderItemRow(
            subfolder = Folder(id = "1", name = "Vegetables"),
            onDelete = {}, onRename = {}, onClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "FolderScreen - With Items", showSystemUi = true)
@Composable
private fun FolderScreenPreview() {
    val subfolder = Folder(id = "sub1", name = "Vegetables")
    val items = listOf(
        TodoItem(id = "i1", text = "Bread"),
        TodoItem(id = "i2", text = "Butter", isCompleted = true),
        TodoItem(
            id = "i3", text = "Fruit basket", isExpanded = true,
            children = listOf(
                TodoItem(id = "i4", text = "Apples"),
                TodoItem(id = "i5", text = "Bananas"),
            )
        ),
    )
    val folder = Folder(id = "f1", name = "Groceries", subfolders = listOf(subfolder), items = items)
    val visibleItems = flatVisibleItems(folder.items, depth = 0)

    JAFListTheme(dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(folder.name) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Folders",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subfolder",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                items(folder.subfolders, key = { "sub_${it.id}" }) { sub ->
                    SubfolderItemRow(subfolder = sub, onDelete = {}, onRename = {}, onClick = {})
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Items",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Item",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                items(visibleItems, key = { (item, depth) -> "${item.id}_$depth" }) { (item, depth) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(start = (depth * 20).dp + 16.dp, end = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = item.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
