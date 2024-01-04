package ru.mediaattachments.presentation.sketch

import ru.mediaattachments.data.db.mediaattachment.MediaAttachment


sealed class MediaSketchStates {
    object MediaNoteRemovedState: MediaSketchStates()
    object MediaNoteSavedState: MediaSketchStates()
    object MediaNoteUpdatedState: MediaSketchStates()
    data class MediaNoteLoadedState(val dbMediaNote: MediaAttachment): MediaSketchStates()
}