package ru.mediaattachments

import android.view.View
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.scheduled.mediaattachmentslibrary.data.MediaNote
import ru.scheduled.mediaattachmentslibrary.data.MediaNoteStatus

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

fun MediaNote.toDbMediaNote(): DbMediaNotes {
    return DbMediaNotes(
        id = id,
        value = value,
        mediaType = mediaType,
        order = createdAtTimeStamp,
        recognizedSpeechText = recognizedSpeechText,
        imageNoteText = imageNoteText,
        voiceAmplitudesList = voiceAmplitudesList,
        uploadPercent = uploadPercent,
        downloadPercent = downloadPercent
    )
}

fun DbMediaNotes.toMediaNote(): MediaNote {
    return MediaNote(
        id = id,
        value = value,
        mediaType = mediaType,
        createdAtTimeStamp = order,
        recognizedSpeechText = recognizedSpeechText,
        imageNoteText = imageNoteText,
        voiceAmplitudesList = voiceAmplitudesList ?: listOf(),
        uploadPercent = uploadPercent,
        downloadPercent = downloadPercent,
        updatedAtTimeStamp = 0,
        status = when {
            downloadPercent in 0 until 100 -> {
                MediaNoteStatus.downloading
            }

            uploadPercent in 0 until 100 -> {
                MediaNoteStatus.uploading
            }

            else -> MediaNoteStatus.synchronized
        },
        previewKey = "32123213123123"
    )
}