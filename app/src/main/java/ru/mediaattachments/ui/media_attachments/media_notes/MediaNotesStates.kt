package ru.mediaattachments.ui.media_attachments.media_notes

import ru.mediaattachments.db.medianotes.DbMediaNotes


sealed class MediaNotesStates {
    object MediaNoteRemovedState: MediaNotesStates()
    object MediaNoteSavedState: MediaNotesStates()
    object MediaNoteUpdatedState: MediaNotesStates()
    data class MediaNoteLoadedState(val dbMediaNote: DbMediaNotes): MediaNotesStates()
    object DownloadingStatesReset: MediaNotesStates()
}