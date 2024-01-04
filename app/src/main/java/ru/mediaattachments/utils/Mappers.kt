package ru.mediaattachments.utils

import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.ui.MediaNoteStatus
import ru.mediaattachments.data.ui.UiMediaAttachment

fun UiMediaAttachment.toDbMediaAttachment(): MediaAttachment {
    return MediaAttachment(
        id = id,
        value = value,
        mediaType = mediaType,
        order = createdAtTimeStamp,
        imageNoteText = imageNoteText,
        voiceAmplitudesList = voiceAmplitudesList,
        uploadPercent = uploadPercent,
        downloadPercent = downloadPercent
    )
}

fun MediaAttachment.toUiMediaAttachment(): UiMediaAttachment {
    return UiMediaAttachment(
        id = id,
        value = value,
        mediaType = mediaType,
        createdAtTimeStamp = order,
        imageNoteText = imageNoteText,
        voiceAmplitudesList = voiceAmplitudesList ?: listOf(),
        uploadPercent = uploadPercent,
        downloadPercent = downloadPercent,
        updatedAtTimeStamp = 0,
        status = when {
            downloadPercent == 0 && uploadPercent == 100 -> {
                MediaNoteStatus.waiting_download
            }
            downloadPercent in 0 until 100 -> {
                MediaNoteStatus.downloading
            }

            uploadPercent in 0 until 100 -> {
                MediaNoteStatus.uploading
            }

            else -> MediaNoteStatus.synchronized
        },
    )
}