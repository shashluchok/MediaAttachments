package ru.leadfrog.ui.media_attachments.media_sketch

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.leadfrog.db.media_uris.MediaNotesRepository
import ru.leadfrog.ui.base.BaseViewModel
import ru.leadfrog.db.media_uris.DbMediaNotes
import ru.leadfrog.ui.media_attachments.MediaConstants
import ru.leadfrog.ui.media_attachments.media_notes.MediaNotesStates
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class MediaSketchViewModel(private val mediaNotesInteractor: MediaNotesRepository,
    ): BaseViewModel<MediaNotesStates>() {

    private var mediaUris:LiveData<List<DbMediaNotes>>? = null

    fun updateMediaNote(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.updateMediaNote(dbMediaNote)
            withContext(Dispatchers.Main){
                _state.value = (MediaNotesStates.MediaNoteUpdatedState)
            }
        }
    }

    fun deleteMediaNoteById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(id)
                mediaNotesInteractor.removeMediaNotes(listOf(mediaNote))
                withContext(Dispatchers.Main){
                    _state.value = (MediaNotesStates.MediaNoteRemovedState)
                }
            }
            catch (e:Exception){
                withContext(Dispatchers.Main){
                    _state.value = (MediaNotesStates.ErrorState)
                }
                e.printStackTrace()
            }

        }
    }

    fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
            _state.postValue(MediaNotesStates.MediaNoteSavedState)
        }
    }

    fun getDbMediaNoteById(dbMediaNoteId :String){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(dbMediaNoteId)
                _state.postValue(MediaNotesStates.MediaNoteLoadedState(dbMediaNotes = mediaNote))
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    fun updateSketchMediaNote(sketchByteArray: ByteArray, shardId: String, dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {

            try {

                val oldFile = File(dbMediaNote.value)
                try {
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
                catch (e:java.lang.Exception){
                    e.printStackTrace()
                }

                val mydir = appContext.getDir(MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

                if (!mydir.exists()) {
                    mydir.mkdirs()
                }

                val fileName =
                    "$mydir/${System.currentTimeMillis()}"
                launch(Dispatchers.IO){
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    val fos = FileOutputStream(f)
                    fos.write(sketchByteArray)
                    fos.flush()
                    fos.close()
                }.join()
                val newDbMediaNote = dbMediaNote
                newDbMediaNote.value = fileName
                updateMediaNote(newDbMediaNote)

            } catch (e: java.lang.Exception) {
                _state.postValue(MediaNotesStates.ErrorState)
            }

        }
    }

    fun saveMediaNoteBitmap(sketchByteArray: ByteArray, shardId: String, mediaType: String) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val mydir = appContext.getDir(MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

                if (!mydir.exists()) {
                    mydir.mkdirs()
                }

                val fileName =
                        "$mydir/${System.currentTimeMillis()}"
                launch(Dispatchers.IO){
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    val fos = FileOutputStream(f)
                    fos.write(sketchByteArray)
                    fos.flush()
                    fos.close()
                }.join()
                val dbMediaUri = DbMediaNotes(
                    id = UUID.randomUUID().toString(),
                    shardId = shardId,
                    value = fileName,
                    mediaType = mediaType,
                    order = System.currentTimeMillis(),
                    downloadPercent = 100,
                    uploadPercent = 0
                )
                saveDbMediaNotes(dbMediaUri)

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                _state.postValue(MediaNotesStates.ErrorState)
            }

        }

    }


}