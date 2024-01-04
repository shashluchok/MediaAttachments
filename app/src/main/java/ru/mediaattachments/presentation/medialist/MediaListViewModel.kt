package ru.mediaattachments.presentation.medialist

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.domain.mediaattachments.MediaNotesRepository
import ru.mediaattachments.presentation.base.BaseViewModel

private val loadingIterationInterval = 1..40
private const val loadingIterationDelay = 500L

class MediaListViewModel(
    private val mediaNotesInteractor: MediaNotesRepository
) : BaseViewModel<MediaListStates>() {

    private val loadingMap = mutableMapOf<String, Job>()
    private var mediaUris: LiveData<List<MediaAttachment>>? = null

    fun updateMediaNote(dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.updateMediaNote(dbMediaNote)
        }
    }

    fun saveDbMediaNotes(dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
        }
    }

    fun resetDownloadPercentTest() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.getAllMediaNotes().onEach {
                it.downloadPercent = 0
                mediaNotesInteractor.updateMediaNote(it)
            }
            withContext(Dispatchers.Main) {
                _state.value = MediaListStates.DownloadingStatesReset
            }

        }
    }

    fun uploadMediaNote(id: String) {
        if (!loadingMap.containsKey(id)) {
            val job = viewModelScope.launch(Dispatchers.IO) {
                var percent = mediaNotesInteractor.getMediaNoteById(id).uploadPercent
                while (percent != 100 && isActive) {
                    percent += loadingIterationInterval.random()
                    if (percent > 100) percent = 100
                    val note = mediaNotesInteractor.getMediaNoteById(id)
                    note.uploadPercent = percent
                    mediaNotesInteractor.updateMediaNote(note)
                    delay(loadingIterationDelay)
                }
                loadingMap.remove(id)
            }
            loadingMap[id] = job
        }
    }

    fun downloadMediaNote(id: String) {
        if (!loadingMap.containsKey(id)) {
            val job = viewModelScope.launch(Dispatchers.IO) {
                var percent = mediaNotesInteractor.getMediaNoteById(id).downloadPercent
                while (percent != 100 && isActive) {
                    percent += loadingIterationInterval.random()
                    if (percent > 100) percent = 100
                    val note = mediaNotesInteractor.getMediaNoteById(id)
                    note.downloadPercent = percent
                    mediaNotesInteractor.updateMediaNote(note)
                    delay(loadingIterationDelay)
                }
                loadingMap.remove(id)
            }
            loadingMap[id] = job
        }
    }

    fun stopDownloading(id: String) {
        loadingMap[id]?.cancel()
        loadingMap.remove(id)
        viewModelScope.launch(Dispatchers.IO) {
            val mediaNote = mediaNotesInteractor.getMediaNoteById(id)
            mediaNote.downloadPercent = 0
            updateMediaNote(mediaNote)
        }
    }

    fun getAllMediaNotesLiveData(): LiveData<List<MediaAttachment>>? {
        if (mediaUris == null) {
            mediaUris = mediaNotesInteractor.getAllMediaNotesLiveData()
        }
        return mediaUris
    }

    fun deleteMediaNotes(vararg id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            id.onEach {
                loadingMap[it]?.cancel()
            }
            mediaUris?.value?.filter { id.contains(it.id) }?.let {
                mediaNotesInteractor.removeMediaNotes(it)
            }
        }
    }

}