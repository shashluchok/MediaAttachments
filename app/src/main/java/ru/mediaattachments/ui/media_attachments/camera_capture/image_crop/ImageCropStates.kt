package ru.mediaattachments.ui.media_attachments.camera_capture.image_crop

import ru.mediaattachments.db.medianotes.DbMediaNotes


sealed class ImageCropStates {
    data class ExistingMediaNoteLoadedState(val mediaNote: DbMediaNotes) : ImageCropStates()
    data class BitmapSavedState(val uri: String) : ImageCropStates()
}