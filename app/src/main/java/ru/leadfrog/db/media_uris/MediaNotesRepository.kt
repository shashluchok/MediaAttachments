package ru.leadfrog.db.media_uris

import androidx.lifecycle.LiveData
import ru.leadfrog.db.MediaDatabase

class MediaNotesRepository(
    private val storage: MediaDatabase
){

    fun saveMediaNotes(dbMediaNote: DbMediaNotes) {
        storage.mediaUrisDao().insertMediaNote(dbMediaNote)
    }

    fun getAllMediaNotesByShardId(shardId:String): LiveData<List<DbMediaNotes>> {
        return storage.mediaUrisDao().getAllMediaNotesByShardId(shardId)
    }

    fun getAllMediaNotesLiveData(): LiveData<List<DbMediaNotes>> {
        return storage.mediaUrisDao().getAllMediaNotesLiveData()
    }

    fun getAllMediaNotes(): List<DbMediaNotes> {
        return storage.mediaUrisDao().getAllMediaNotes()
    }

    fun removeMediaNotes(dbMediaNotes: List<DbMediaNotes>) {
        storage.mediaUrisDao().deleteMediaNotes(dbMediaNotes)
    }

    fun removeAllMediaNotes() {
        storage.mediaUrisDao().deleteAllMediaNotes()
    }

    fun getMediaNotesByType(shardId:String, mediaType: String): LiveData<List<DbMediaNotes>> {
        return storage.mediaUrisDao().getMediaNotesByType(shardId,mediaType)
    }

    fun getMediaNoteById(id: String): DbMediaNotes {
        return storage.mediaUrisDao().getMediaNoteById(id = id)
    }

    fun updateMediaNote(dbMediaNote: DbMediaNotes){
        storage.mediaUrisDao().updateMediaNote(dbMediaNote)
    }

}