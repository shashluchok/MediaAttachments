package ru.mediaattachments.presentation.imagecrop

import ru.mediaattachments.data.db.mediaattachment.MediaAttachment


sealed class ImageCropStates {
    data class ExistingMediaNoteLoadedState(val mediaNote: MediaAttachment) : ImageCropStates()
    object BitmapSavedState : ImageCropStates()
}