package ru.mediaattachments.db.medianotes

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType

@Dao
interface MediaNotesDao {

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotesLiveData(): LiveData<List<DbMediaNotes>>

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotes(): List<DbMediaNotes>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMediaNote(dbMediaNote: DbMediaNotes): Long

    @Delete
    fun deleteMediaNotes(notes: List<DbMediaNotes>)

    @Query("DELETE FROM mediaNotes")
    fun deleteAllMediaNotes()

    @Query("SELECT * FROM mediaNotes WHERE mediaType =:mediaType")
    fun getMediaNotesByType(mediaType: MediaItemType): LiveData<List<DbMediaNotes>>

    @Query("SELECT * FROM mediaNotes WHERE id =:id ")
    fun getMediaNoteById(id: String): DbMediaNotes

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMediaNote(dbMediaNote: DbMediaNotes)

}