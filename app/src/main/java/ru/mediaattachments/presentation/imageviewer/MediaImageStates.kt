package ru.mediaattachments.presentation.imageviewer

import ru.mediaattachments.data.db.mediaattachment.MediaAttachment


sealed class MediaImageStates {
    data class MediaNoteLoadedState(val dbMediaNote: MediaAttachment): MediaImageStates()
}