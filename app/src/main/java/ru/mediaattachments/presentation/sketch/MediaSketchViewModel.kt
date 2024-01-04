package ru.mediaattachments.presentation.sketch

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.domain.mediaattachments.MediaNotesRepository
import ru.mediaattachments.presentation.base.BaseViewModel
import ru.mediaattachments.utils.FileUtils
import ru.mediaattachments.utils.saveByteArray
import java.io.File
import java.util.UUID

class MediaSketchViewModel(
    private val mediaNotesInteractor: MediaNotesRepository,
) : BaseViewModel<MediaSketchStates>() {
    fun deleteMediaNoteById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(id)
                mediaNotesInteractor.removeMediaNotes(listOf(mediaNote))
                withContext(Dispatchers.Main) {
                    _state.value = (MediaSketchStates.MediaNoteRemovedState)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun getDbMediaNoteById(dbMediaNoteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(dbMediaNoteId)
                _state.postValue(MediaSketchStates.MediaNoteLoadedState(dbMediaNote = mediaNote))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSketchMediaNote(sketchByteArray: ByteArray, dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {

            try {

                File(dbMediaNote.value).delete()

                val file =
                    FileUtils.createFile(appContext, fileExtension = FileUtils.Extension.JPEG)

                file.saveByteArray(sketchByteArray)
                dbMediaNote.value = file.path
                updateMediaNote(dbMediaNote)

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun saveMediaNoteBitmap(sketchByteArray: ByteArray, mediaType: MediaType) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val file =
                    FileUtils.createFile(appContext, fileExtension = FileUtils.Extension.JPEG)
                file.saveByteArray(sketchByteArray)
                val dbMediaUri = MediaAttachment(
                    id = UUID.randomUUID().toString(),
                    value = file.path,
                    mediaType = mediaType,
                    order = System.currentTimeMillis(),
                    downloadPercent = 100,
                    uploadPercent = 0
                )
                saveDbMediaNotes(dbMediaUri)

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun updateMediaNote(dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.updateMediaNote(dbMediaNote)
            withContext(Dispatchers.Main) {
                _state.value = (MediaSketchStates.MediaNoteUpdatedState)
            }
        }
    }

    private fun saveDbMediaNotes(dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
            _state.postValue(MediaSketchStates.MediaNoteSavedState)
        }
    }

}