package ru.leadfrog.ui.media_attachments.media_notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.leadfrog.db.media_uris.MediaNotesRepository
import ru.leadfrog.ui.base.BaseViewModel
import ru.leadfrog.db.media_uris.DbMediaNotes
import ru.leadfrog.ui.media_attachments.media_notes.MediaNotesStates
import java.io.File

class MediaNotesViewModel(private val mediaNotesInteractor: MediaNotesRepository
    ): BaseViewModel<MediaNotesStates>() {

    private var mediaUris:LiveData<List<DbMediaNotes>>? = null

    fun updateMediaNote(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.updateMediaNote(dbMediaNote)
            _state.postValue(MediaNotesStates.MediaNoteUpdatedState)
        }
    }

    fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
            _state.postValue(MediaNotesStates.MediaNoteSavedState)
        }
    }

    fun updateVoiceNoteWithRecognizedSpeech(text: String, noteId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(100)
                val dbMediaNote = mediaNotesInteractor.getMediaNoteById(noteId)
                dbMediaNote.recognizedSpeechText = text
                mediaNotesInteractor.updateMediaNote(dbMediaNote)
                _state.postValue(MediaNotesStates.MediaNoteSavedState)

            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }

        }
    }

    fun getAllDbMediaNotesByShardId(shardId: String): LiveData<List<DbMediaNotes>>? {
        if (mediaUris == null) {
            mediaUris = mediaNotesInteractor.getAllMediaNotesByShardId(shardId)
        }
        return mediaUris
    }

    fun deleteMediaNotes(dbMediaNotes: List<DbMediaNotes>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.removeMediaNotes(dbMediaNotes)
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                file.delete()
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun deleteAllMediaNotes(){
        viewModelScope.launch(Dispatchers.IO)  {
            mediaNotesInteractor.removeAllMediaNotes()
        }
    }

}