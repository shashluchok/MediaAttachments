package ru.scheduled.mediaattachmentslibrary.data

data class ImageItem(
    val id: String,
    val type: ImageItemTypes,
    val filePath: String,
    val imageText: String
)

enum class ImageItemTypes {
    PHOTO, SKETCH
}