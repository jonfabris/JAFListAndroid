package jafproductions.jaflist.models

import com.google.gson.annotations.SerializedName

data class TodoItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("isExpanded") val isExpanded: Boolean = false,
    @SerializedName("children") val children: List<TodoItem> = emptyList()
)
