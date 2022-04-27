package ru.leadfrog.db.media_uris

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.leadfrog.db.media_uris.DbMediaNotes

@Dao
interface MediaUrisDao {

    @Query("SELECT * FROM mediaNotes WHERE shardId = :shardId")
    fun getAllMediaNotesByShardId(shardId: String): LiveData<List<DbMediaNotes>>

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotesLiveData(): LiveData<List<DbMediaNotes>>

    @Query("SELECT * FROM mediaNotes")
    fun getAllMediaNotes(): List<DbMediaNotes>

   /* @Query("SELECT * FROM mediaNotes ORDER BY `order` ASC LIMIT 1")
    fun getFirstMediaNoteByShardId(shardId: String): DbMediaNotes*/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMediaNote(dbMediaNote: DbMediaNotes): Long

    @Delete
    fun deleteMediaNotes(notes: List<DbMediaNotes>)

    @Query("DELETE FROM mediaNotes")
    fun deleteAllMediaNotes()

    @Query("SELECT * FROM mediaNotes WHERE mediaType =:mediaType AND shardId = :shardId")
    fun getMediaNotesByType(shardId:String, mediaType: String): LiveData<List<DbMediaNotes>>

    @Query("SELECT * FROM mediaNotes WHERE id =:id ")
    fun getMediaNoteById(id: String): DbMediaNotes

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMediaNote(dbMediaNote: DbMediaNotes)

}