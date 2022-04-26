package ru.leadfrog.ui.media_attachments.camera_capture.image_crop

import ru.leadfrog.db.media_uris.DbMediaNotes


sealed class ImageCropStates {
    data class ExistingMediaNoteLoadedState(val mediaNote: DbMediaNotes) : ImageCropStates()
    data class ErrorState(val message: String) : ImageCropStates()
    data class BitmapSavedState(val uri: String) : ImageCropStates()
}