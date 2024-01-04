package ru.mediaattachments.presentation.imagecrop

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.domain.mediaattachments.MediaNotesRepository
import ru.mediaattachments.presentation.base.BaseViewModel
import ru.mediaattachments.utils.FileUtils
import ru.mediaattachments.utils.saveBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID



class ImageCropViewModel(
    private val mediaNotesInteractor: MediaNotesRepository
) : BaseViewModel<ImageCropStates>() {

    fun getMediaNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val note = mediaNotesInteractor.getMediaNoteById(id)
                withContext(Dispatchers.Main) {
                    _state.value = ImageCropStates.ExistingMediaNoteLoadedState(mediaNote = note)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteOriginalPhoto(photoPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(photoPath)
                if (file.exists()) file.delete()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, imageNote: String?, existingNoteId: String?) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
               val newFile = FileUtils.createFile(appContext, fileExtension = FileUtils.Extension.JPEG )

                launch(Dispatchers.IO) {
                    newFile.saveBitmap(bitmap)

                    if (existingNoteId != null) {
                        val dbMediaUri =
                            mediaNotesInteractor.getMediaNoteById(existingNoteId).also { it.value }
                        val file = File(dbMediaUri.value)
                        if (file.exists()) file.delete()
                        dbMediaUri.value = newFile.path
                        dbMediaUri.imageNoteText = imageNote ?: ""
                        mediaNotesInteractor.updateMediaNote(dbMediaUri)
                    } else {
                        val dbMediaUri = MediaAttachment(
                            id = UUID.randomUUID().toString(),
                            value = newFile.path,
                            mediaType = MediaType.TYPE_PHOTO,
                            imageNoteText = imageNote ?: "",
                            order = System.currentTimeMillis(),
                            downloadPercent = 100,
                            uploadPercent = 0
                        )
                        saveDbMediaNotes(
                            dbMediaUri
                        )
                    }
                    _state.postValue(ImageCropStates.BitmapSavedState)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun saveDbMediaNotes(dbMediaNote: MediaAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
        }
    }

}