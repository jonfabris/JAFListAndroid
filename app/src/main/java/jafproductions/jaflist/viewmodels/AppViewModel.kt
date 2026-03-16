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
        val newFolder = Folder(id = UUID.randomUUID().toString(), name = name)
        val current = appData.value
        dataStore.save(current.copy(folders = current.folders + newFolder, lastModified = Date()))
    }

    fun deleteFolder(folderId: String) {
        val current = appData.value
        dataStore.save(current.copy(folders = current.folders.filter { it.id != folderId }, lastModified = Date()))
    }

    // MARK: - Subfolder Operations

    fun addSubfolder(parentFolderId: String, name: String) {
        val newSubfolder = Folder(id = UUID.randomUUID().toString(), name = name)
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == parentFolderId) folder.copy(subfolders = folder.subfolders + newSubfolder)
            else folder
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    fun deleteSubfolder(parentFolderId: String, subfolderId: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == parentFolderId) folder.copy(subfolders = folder.subfolders.filter { it.id != subfolderId })
            else folder
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    fun renameFolder(folderId: String, newName: String) {
        val current = appData.value
        val updatedFolders = current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(name = newName)
            } else {
                folder.copy(subfolders = folder.subfolders.map { sub ->
                    if (sub.id == folderId) sub.copy(name = newName) else sub
                })
            }
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    // MARK: - Item Operations

    /**
     * Applies [transform] to the items list of whichever folder (top-level or subfolder)
     * matches [folderId], returning the updated top-level folders list.
     */
    private fun transformFolderItems(
        current: AppData,
        folderId: String,
        transform: (List<TodoItem>) -> List<TodoItem>
    ): List<Folder> {
        return current.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(items = transform(folder.items))
            } else {
                folder.copy(subfolders = folder.subfolders.map { sub ->
                    if (sub.id == folderId) sub.copy(items = transform(sub.items)) else sub
                })
            }
        }
    }

    fun addItem(folderId: String, text: String) {
        val newItem = TodoItem(id = UUID.randomUUID().toString(), text = text)
        val current = appData.value
        val updatedFolders = transformFolderItems(current, folderId) { it + newItem }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    fun addChildItem(folderId: String, parentItemId: String, text: String) {
        val newItem = TodoItem(id = UUID.randomUUID().toString(), text = text)
        val current = appData.value
        val updatedFolders = transformFolderItems(current, folderId) { items ->
            addChildToItems(items, parentItemId, newItem)
        }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun addChildToItems(items: List<TodoItem>, parentId: String, newChild: TodoItem): List<TodoItem> {
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
        val updatedFolders = transformFolderItems(current, folderId) { toggleCompletionInItems(it, itemId) }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun toggleCompletionInItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) item.copy(isCompleted = !item.isCompleted)
            else item.copy(children = toggleCompletionInItems(item.children, itemId))
        }
    }

    fun toggleItemExpansion(folderId: String, itemId: String) {
        val current = appData.value
        val updatedFolders = transformFolderItems(current, folderId) { toggleExpansionInItems(it, itemId) }
        // UI state only — do NOT update lastModified or sync to cloud
        dataStore.saveLocalOnly(current.copy(folders = updatedFolders))
    }

    private fun toggleExpansionInItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) item.copy(isExpanded = !item.isExpanded)
            else item.copy(children = toggleExpansionInItems(item.children, itemId))
        }
    }

    fun deleteItem(folderId: String, itemId: String) {
        val current = appData.value
        val updatedFolders = transformFolderItems(current, folderId) { deleteFromItems(it, itemId) }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun deleteFromItems(items: List<TodoItem>, itemId: String): List<TodoItem> {
        return items
            .filter { it.id != itemId }
            .map { item -> item.copy(children = deleteFromItems(item.children, itemId)) }
    }

    fun editItem(folderId: String, itemId: String, newText: String) {
        val current = appData.value
        val updatedFolders = transformFolderItems(current, folderId) { editInItems(it, itemId, newText) }
        dataStore.save(current.copy(folders = updatedFolders, lastModified = Date()))
    }

    private fun editInItems(items: List<TodoItem>, itemId: String, newText: String): List<TodoItem> {
        return items.map { item ->
            if (item.id == itemId) item.copy(text = newText)
            else item.copy(children = editInItems(item.children, itemId, newText))
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
