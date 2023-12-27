package ru.mediaattachments.db.medianotes

import androidx.lifecycle.LiveData
import ru.mediaattachments.db.MediaDatabase
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType

class MediaNotesRepository(
    private val storage: MediaDatabase
) {

    fun saveMediaNotes(dbMediaNote: DbMediaNotes) {
        storage.mediaNotesDao().insertMediaNote(dbMediaNote)
    }

    fun getAllMediaNotesLiveData(): LiveData<List<DbMediaNotes>> {
        return storage.mediaNotesDao().getAllMediaNotesLiveData()
    }

    fun getAllMediaNotes(): List<DbMediaNotes> {
        return storage.mediaNotesDao().getAllMediaNotes()
    }

    fun removeMediaNotes(dbMediaNotes: List<DbMediaNotes>) {
        storage.mediaNotesDao().deleteMediaNotes(dbMediaNotes)
    }

    fun removeAllMediaNotes() {
        storage.mediaNotesDao().deleteAllMediaNotes()
    }

    fun getMediaNotesByType(mediaType: MediaItemType): LiveData<List<DbMediaNotes>> {
        return storage.mediaNotesDao().getMediaNotesByType(mediaType)
    }

    fun getMediaNoteById(id: String): DbMediaNotes {
        return storage.mediaNotesDao().getMediaNoteById(id = id)
    }

    fun updateMediaNote(dbMediaNote: DbMediaNotes) {
        storage.mediaNotesDao().updateMediaNote(dbMediaNote)
    }

}