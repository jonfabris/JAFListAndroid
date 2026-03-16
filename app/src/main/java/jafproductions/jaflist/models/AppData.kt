package jafproductions.jaflist.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class AppData(
    @SerializedName("folders") val folders: List<Folder> = emptyList(),
    @SerializedName("lastModified") val lastModified: Date = Date()
) {
    companion object {
        val empty get() = AppData(folders = emptyList(), lastModified = Date(0))
    }
}
