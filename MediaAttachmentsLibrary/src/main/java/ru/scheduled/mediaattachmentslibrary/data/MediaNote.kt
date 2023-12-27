package ru.scheduled.mediaattachmentslibrary.data

data class MediaNote(
    val id:String,
    val mediaType: MediaItemType,
    var value: String,
    var recognizedSpeechText: String,
    val voiceAmplitudesList: List<Int>,
    var imageNoteText: String,
    val createdAtTimeStamp:Long,
    var updatedAtTimeStamp:Long,
    var isChosen:Boolean = false,
    var downloadPercent:Int,
    var uploadPercent:Int,
    var isLoadingStopped:Boolean = true,
    var status: MediaNoteStatus,
    var previewKey:String? = null,
    var audioDuration:Int? = null
)

enum class MediaNoteStatus {
    uploading,
    downloading,
    synchronized
}