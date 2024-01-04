package ru.mediaattachments.data.ui

import ru.mediaattachments.data.db.mediaattachment.MediaType

data class UiMediaAttachment(
    val id:String,
    val mediaType: MediaType,
    var value: String,
    val voiceAmplitudesList: List<Int>,
    var imageNoteText: String,
    val createdAtTimeStamp:Long,
    var updatedAtTimeStamp:Long,
    var downloadPercent:Int,
    var uploadPercent:Int,
    var status: MediaNoteStatus,
    var previewKey:String? = null,
    var audioDuration:Int? = null,
    var isSelected:Boolean = false
)

enum class MediaNoteStatus {
    uploading,
    downloading,
    waiting_download,
    synchronized
}