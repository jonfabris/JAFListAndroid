package jafproductions.jaflist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import jafproductions.jaflist.models.TodoItem
import jafproductions.jaflist.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: String,
    appViewModel: AppViewModel,
    navController: NavController
) {
    val appData by appViewModel.appData.collectAsState()
    val folder = appData.folders.find { it.id == folderId }

    if (folder == null) {
        navController.popBackStack()
        return
    }

    val visibleItems = remember(folder.items) {
        flatVisibleItems(folder.items, depth = 0)
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }

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
                actions = {
                    IconButton(onClick = {
                        newItemText = ""
                        showAddItemDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item"
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
                        Text(
                            text = "New Item",
                            style = MaterialTheme.typography.headlineSmall
                        )
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
                            TextButton(onClick = { showAddItemDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    if (newItemText.isNotBlank()) {
                                        appViewModel.addItem(folderId, newItemText.trim())
                                        showAddItemDialog = false
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
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
