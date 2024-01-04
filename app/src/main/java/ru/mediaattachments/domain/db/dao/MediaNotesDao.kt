package ru.mediaattachments.domain.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType

@Dao
interface MediaNotesDao {

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotesLiveData(): LiveData<List<MediaAttachment>>

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotes(): List<MediaAttachment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMediaNote(dbMediaNote: MediaAttachment): Long

    @Delete
    fun deleteMediaNotes(notes: List<MediaAttachment>)

    @Query("DELETE FROM mediaNotes")
    fun deleteAllMediaNotes()

    @Query("SELECT * FROM mediaNotes WHERE mediaType =:mediaType")
    fun getMediaNotesByType(mediaType: MediaType): LiveData<List<MediaAttachment>>

    @Query("SELECT * FROM mediaNotes WHERE id =:id ")
    fun getMediaNoteById(id: String): MediaAttachment

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMediaNote(dbMediaNote: MediaAttachment)

}