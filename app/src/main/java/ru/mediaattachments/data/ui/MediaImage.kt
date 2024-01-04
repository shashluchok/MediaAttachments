package ru.mediaattachments.data.ui

data class MediaImage(
    val id: String,
    val type: ImageItemTypes,
    val filePath: String,
    val imageText: String
)

enum class ImageItemTypes {
    PHOTO, SKETCH
}