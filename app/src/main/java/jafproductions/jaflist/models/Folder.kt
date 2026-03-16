package jafproductions.jaflist.models

import com.google.gson.annotations.SerializedName

data class Folder(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("items") val items: List<TodoItem> = emptyList(),
    @SerializedName("subfolders") val subfolders: List<Folder> = emptyList()
)
