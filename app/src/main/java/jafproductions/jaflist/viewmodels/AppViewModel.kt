package jafproductions.jaflist.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jafproductions.jaflist.models.AppData
import jafproductions.jaflist.models.Folder
import jafproductions.jaflist.models.TodoItem
import jafproductions.jaflist.services.DataStore
import jafproductions.jaflist.services.SyncStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = DataStore(application)

    val appData: StateFlow<AppData> = dataStore.appData
    val syncStatus: StateFlow<SyncStatus> = dataStore.syncStatus

    fun initializeCloud() {
        viewModelScope.launch {
            dataStore.initializeCloud()
        }
    }

    // MARK: - Folder Operations

    fun addFolder(name: String) {
        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = name,
            items = emptyList()
        )
        val current = appData.value
        dataStore.save(current.copy(folders = current.folders + newFolder, lastModified = Date()))
    }

    fun deleteFolder(folderId: String) {
        val current = appData.value
        dataStore.save(current.copy(folders = current.folders.filter { it.id != folderId }, lastModified = Date()))
    }

    // MARK: - Item Operations

    fun addItem(folderId: String, text: String) {
        val newItem = TodoItem(
            id = UUID.randomUUID().toString(),
            text = text,
            isCompleted = false,
            isExpanded = false,
            children = emptyList()
        )
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = folder.items + newItem)
            } else {
                folder
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    fun addChildItem(folderId: String, parentItemId: String, text: String) {
        val newItem = TodoItem(
            id = UUID.randomUUID().toString(),
            text = text,
            isCompleted = false,
            isExpanded = false,
            children = emptyList()
        )
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = addChildToItems(folder.items, parentItemId, newItem))
            } else {
                folder
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun addChildToItems(
        items: List<TodoItem>,
        parentId: String,
        newChild: TodoItem
    ): List<TodoItem> {
        return items.map { item ->
            if (item.id == parentId) {
                item.copy(children = item.children + newChild, isExpanded = true)
            } else {
                item.copy(children = addChildToItems(item.children, parentId, newChild))
            }
        }
    }

    fun toggleItemCompletion(folderId: String, itemId: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = toggleCompletionInItems(folder.items, itemId))
            } else {
                folder
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun toggleCompletionInItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) {
                item.copy(isCompleted = !item.isCompleted)
            } else {
                item.copy(children = toggleCompletionInItems(item.children, itemId))
            }
        }
    }

    fun toggleItemExpansion(folderId: String, itemId: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = toggleExpansionInItems(folder.items, itemId))
            } else {
                folder
            }
        }
        // UI state only — do NOT update lastModified or sync to cloud
        dataStore.saveLocalOnly(current.copy(folders = updatedFolders))
    }

    private fun toggleExpansionInItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) {
                item.copy(isExpanded = !item.isExpanded)
            } else {
                item.copy(children = toggleExpansionInItems(item.children, itemId))
            }
        }
    }

    fun deleteItem(folderId: String, itemId: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = deleteFromItems(folder.items, itemId))
            } else {
                folder
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun deleteFromItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items
            .filter { it.id != itemId }
            .map { item ->
                item.copy(children = deleteFromItems(item.children, itemId))
            }
    }

    fun editItem(folderId: String, itemId: String, newText: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = editInItems(folder.items, itemId, newText))
            } else {
                folder
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun editInItems(items: List<TodoItem>, itemId: String, newText: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) {
                item.copy(text = newText)
            } else {
                item.copy(children = editInItems(item.children, itemId, newText))
            }
        }
    }

    fun saveOnBackground() {
        dataStore.saveImmediately(appData.value)
    }

    override fun onCleared() {
        super.onCleared()
        dataStore.close()
    }
}
