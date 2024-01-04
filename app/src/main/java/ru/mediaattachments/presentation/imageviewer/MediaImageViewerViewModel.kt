package ru.mediaattachments.presentation.imageviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.domain.mediaattachments.MediaNotesRepository
import ru.mediaattachments.presentation.base.BaseViewModel

class MediaImageViewerViewModel(
    private val mediaNotesInteractor: MediaNotesRepository,
) : BaseViewModel<MediaImageStates>() {

    private var typedMediaUris: LiveData<List<MediaAttachment>>? = null

    fun deleteMediaNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(id)
                mediaNotesInteractor.removeMediaNotes(listOf(mediaNote))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getImageById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val image = mediaNotesInteractor.getMediaNoteById(id)
                withContext(Dispatchers.Main) {
                    _state.value = MediaImageStates.MediaNoteLoadedState(image)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getMediaUrisByType(mediaType: MediaType): LiveData<List<MediaAttachment>>? {
        if (typedMediaUris == null) {
            typedMediaUris = mediaNotesInteractor.getMediaNotesByType(mediaType)
        }
        return typedMediaUris
    }


}
