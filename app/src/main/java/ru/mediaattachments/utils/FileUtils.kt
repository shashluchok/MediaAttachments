package ru.mediaattachments.utils

import android.content.Context
import java.io.File


object FileUtils {

    enum class Extension(val extensionString: String) {
        JPEG(".jpg"),
        MP3(".mp3")
    }

    fun createFile(context: Context, fileExtension: Extension): File {

        val dir = context.getDir(
            MediaConstants.INTERNAL_DIRECTORY,
            Context.MODE_PRIVATE
        )

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val fileName =
            "$dir/${System.currentTimeMillis()}${fileExtension.extensionString}"
        return File(fileName)
    }
}