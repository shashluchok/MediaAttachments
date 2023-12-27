package ru.mediaattachments.ui.media_attachments.media_sketch

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.db.medianotes.MediaNotesRepository
import ru.mediaattachments.ui.base.BaseViewModel
import ru.mediaattachments.ui.media_attachments.MediaConstants
import ru.mediaattachments.ui.media_attachments.media_notes.MediaNotesStates
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaSketchViewModel(
    private val mediaNotesInteractor: MediaNotesRepository,
) : BaseViewModel<MediaNotesStates>() {
    fun deleteMediaNoteById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaNote = mediaNotesInteractor.getMediaNoteById(id)
                mediaNotesInteractor.removeMediaNotes(listOf(mediaNote))
                withContext(Dispatchers.Main) {
                    _state.value = (MediaNotesStates.MediaNoteRemovedState)
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
                _state.postValue(MediaNotesStates.MediaNoteLoadedState(dbMediaNote = mediaNote))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSketchMediaNote(sketchByteArray: ByteArray, dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {

            try {

                val oldFile = File(dbMediaNote.value)
                try {
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val mydir = appContext.getDir(
                    MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY,
                    Context.MODE_PRIVATE
                )

                if (!mydir.exists()) {
                    mydir.mkdirs()
                }

                val fileName =
                    "$mydir/${System.currentTimeMillis()}"
                launch(Dispatchers.IO) {
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    FileOutputStream(f).use {
                        it.write(sketchByteArray)
                    }
                }.join()
                dbMediaNote.value = fileName
                updateMediaNote(dbMediaNote)

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun saveMediaNoteBitmap(sketchByteArray: ByteArray, mediaType: MediaItemType) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val mydir = appContext.getDir(
                    MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY,
                    Context.MODE_PRIVATE
                )

                if (!mydir.exists()) {
                    mydir.mkdirs()
                }

                val fileName =
                    "$mydir/${System.currentTimeMillis()}"
                launch(Dispatchers.IO) {
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    FileOutputStream(f).use {
                        it.write(sketchByteArray)
                    }
                }.join()
                val dbMediaUri = DbMediaNotes(
                    id = UUID.randomUUID().toString(),
                    value = fileName,
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

    private fun updateMediaNote(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.updateMediaNote(dbMediaNote)
            withContext(Dispatchers.Main) {
                _state.value = (MediaNotesStates.MediaNoteUpdatedState)
            }
        }
    }

    private fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
            _state.postValue(MediaNotesStates.MediaNoteSavedState)
        }
    }

}