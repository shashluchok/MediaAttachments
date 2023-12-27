package ru.mediaattachments.ui.media_attachments.media_image_viewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.db.medianotes.MediaNotesRepository
import ru.mediaattachments.ui.base.BaseViewModel
import ru.mediaattachments.ui.media_attachments.media_notes.MediaNotesStates
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType

class MediaImageViewerViewModel(
    private val mediaNotesInteractor: MediaNotesRepository,
) : BaseViewModel<MediaNotesStates>() {

    private var typedMediaUris: LiveData<List<DbMediaNotes>>? = null

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
                    _state.value = MediaNotesStates.MediaNoteLoadedState(image)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getMediaUrisByType(mediaType: MediaItemType): LiveData<List<DbMediaNotes>>? {
        if (typedMediaUris == null) {
            typedMediaUris = mediaNotesInteractor.getMediaNotesByType(mediaType)
        }
        return typedMediaUris
    }


}
