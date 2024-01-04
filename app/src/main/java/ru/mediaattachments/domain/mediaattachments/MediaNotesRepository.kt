package ru.mediaattachments.domain.mediaattachments

import androidx.lifecycle.LiveData
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.domain.db.MediaDatabase

class MediaNotesRepository(
    private val storage: MediaDatabase
) {

    fun saveMediaNotes(dbMediaNote: MediaAttachment) {
        storage.mediaNotesDao().insertMediaNote(dbMediaNote)
    }

    fun getAllMediaNotesLiveData(): LiveData<List<MediaAttachment>> {
        return storage.mediaNotesDao().getAllMediaNotesLiveData()
    }

    fun getAllMediaNotes(): List<MediaAttachment> {
        return storage.mediaNotesDao().getAllMediaNotes()
    }

    fun removeMediaNotes(mediaNotes: List<MediaAttachment>) {
        storage.mediaNotesDao().deleteMediaNotes(mediaNotes)
    }

    fun getMediaNotesByType(mediaType: MediaType): LiveData<List<MediaAttachment>> {
        return storage.mediaNotesDao().getMediaNotesByType(mediaType)
    }

    fun getMediaNoteById(id: String): MediaAttachment {
        return storage.mediaNotesDao().getMediaNoteById(id = id)
    }

    fun updateMediaNote(dbMediaNote: MediaAttachment) {
        storage.mediaNotesDao().updateMediaNote(dbMediaNote)
    }

}