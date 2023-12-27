package ru.mediaattachments.ui.media_attachments.media_notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import ru.mediaattachments.db.medianotes.MediaNotesRepository
import ru.mediaattachments.ui.base.BaseViewModel
import ru.mediaattachments.db.medianotes.DbMediaNotes
import java.io.File

class MediaNotesViewModel(private val mediaNotesInteractor: MediaNotesRepository
    ): BaseViewModel<MediaNotesStates>() {

    private val loadingMap = mutableMapOf<String, Job>()

    fun isMediaNoteLoading(mediaNoteId:String):Boolean{
        return loadingMap.containsKey(mediaNoteId)
    }

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

    fun resetDownloadPercentTest(){
        viewModelScope.launch(Dispatchers.IO){
            try {
                mediaNotesInteractor.getAllMediaNotes().onEach {
                    it.downloadPercent = 0
                    mediaNotesInteractor.updateMediaNote(it)
                }
                withContext(Dispatchers.Main){
                    _state.value = MediaNotesStates.DownloadingStatesReset
                }
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    fun uploadMediaNote(dbNoteId:String){
      if(!loadingMap.containsKey(dbNoteId)) {
          val job = viewModelScope.launch(Dispatchers.IO) {
              try {
                  var percent = mediaNotesInteractor.getMediaNoteById(dbNoteId).uploadPercent
                  while (percent != 100 && isActive) {
                      percent += 1
                      if (percent > 100) percent = 100
                      val note = mediaNotesInteractor.getMediaNoteById(dbNoteId)
                      note.uploadPercent = percent
                      mediaNotesInteractor.updateMediaNote(note)
                      delay(25)
                  }
                  loadingMap.remove(dbNoteId)
              } catch (e: java.lang.Exception) {
                  e.printStackTrace()
              }
          }
          loadingMap.put(dbNoteId, job)
      }
    }

    fun downloadMediaNote(dbNoteId:String){
        if(!loadingMap.containsKey(dbNoteId)) {
            val job = viewModelScope.launch(Dispatchers.IO) {
                try {
                    var percent = mediaNotesInteractor.getMediaNoteById(dbNoteId).downloadPercent
                    while (percent != 100 && isActive) {
                        percent += 1
                        if (percent > 100) percent = 100
                        val note = mediaNotesInteractor.getMediaNoteById(dbNoteId)
                        note.downloadPercent = percent
                        mediaNotesInteractor.updateMediaNote(note)
                        delay(25)
                    }
                    loadingMap.remove(dbNoteId)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            loadingMap.put(dbNoteId, job)
        }
    }

    fun stopDownloading(dbNoteId:String){
        try {
            loadingMap.get(dbNoteId)?.cancel()
            loadingMap.remove(dbNoteId)
        }
        catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    fun getAllMediaNotesLiveData(): LiveData<List<DbMediaNotes>>? {
        if (mediaUris == null) {
            mediaUris = mediaNotesInteractor.getAllMediaNotesLiveData()
        }
        return mediaUris
    }

    fun deleteMediaNotes(dbMediaNotes: List<DbMediaNotes>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                dbMediaNotes.onEach {
                    loadingMap.get(it.id)?.cancel()
                }
                mediaNotesInteractor.removeMediaNotes(dbMediaNotes)
            }
            catch (e:java.lang.Exception){

            }
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