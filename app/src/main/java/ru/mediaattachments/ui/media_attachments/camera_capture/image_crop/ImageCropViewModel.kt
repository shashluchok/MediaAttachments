package ru.mediaattachments.ui.media_attachments.camera_capture.image_crop

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.db.medianotes.MediaNotesRepository
import ru.mediaattachments.ui.base.BaseViewModel
import ru.mediaattachments.ui.media_attachments.MediaConstants
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID


private const val dateFormat = "HHmmssddMMyyyy"

private const val imageExtension = ".jpg"
private const val imageMimeType = "image/jpeg"
private const val imageFolderPath = "DCIM/MediaAttachments"
private const val imageQuality = 90

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

    fun saveBitmapToGallery(bitmap: Bitmap, imageNote: String?, existingNoteId: String?) {
        CoroutineScope(Dispatchers.IO).launch {

            val contentValues =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    ContentValues().apply {
                        val name = SimpleDateFormat(
                            dateFormat,
                            Locale.US
                        ).format(System.currentTimeMillis()) + imageExtension
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, imageMimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, imageFolderPath)
                    }
                } else {
                    ContentValues().apply {
                        put(
                            MediaStore.Images.Media.TITLE,
                            SimpleDateFormat(dateFormat, Locale.US).format(
                                System.currentTimeMillis()
                            )
                        )
                        put(
                            MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat(
                                dateFormat,
                                Locale.US
                            ).format(System.currentTimeMillis())
                        )
                        put(
                            MediaStore.Images.Media.DESCRIPTION, SimpleDateFormat(
                                dateFormat,
                                Locale.US
                            ).format(System.currentTimeMillis())
                        )
                        put(MediaStore.Images.Media.MIME_TYPE, imageExtension)
                        put(
                            MediaStore.Images.Media.DATE_ADDED,
                            System.currentTimeMillis() / 1000
                        )
                    }

                }

            launch(Dispatchers.IO) {
                try {
                    val url = appContext.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (url != null) {
                        appContext.contentResolver.openOutputStream(url)?.use { imageOut ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, imageOut);
                        }

                        if (existingNoteId != null) {
                            val dbMediaUri = mediaNotesInteractor.getMediaNoteById(existingNoteId)
                                .also { it.value }
                            val file = File(dbMediaUri.value)
                            if (file.exists()) file.delete()
                            dbMediaUri.value = url.toString()
                            dbMediaUri.imageNoteText = imageNote ?: ""
                            mediaNotesInteractor.updateMediaNote(dbMediaUri)
                        } else {
                            val dbMediaUri = DbMediaNotes(
                                id = UUID.randomUUID().toString(),
                                value = url.toString(),
                                mediaType = MediaItemType.TYPE_PHOTO,
                                imageNoteText = imageNote ?: "",
                                order = System.currentTimeMillis(),
                                downloadPercent = 100,
                                uploadPercent = 0
                            )
                            saveDbMediaNotes(
                                dbMediaUri
                            )
                        }

                        _state.postValue(ImageCropStates.BitmapSavedState(url.toString()))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, imageNote: String?, existingNoteId: String?) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val dir = appContext.getDir(
                    MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY,
                    Context.MODE_PRIVATE
                )

                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val fileName =
                    "$dir/${System.currentTimeMillis()}$imageExtension"
                launch(Dispatchers.IO) {
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    val fos = FileOutputStream(f)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
                    val image = stream.toByteArray()
                    fos.write(image)
                    fos.flush()
                    fos.close()

                    if (existingNoteId != null) {
                        val dbMediaUri =
                            mediaNotesInteractor.getMediaNoteById(existingNoteId).also { it.value }
                        val file = File(dbMediaUri.value)
                        if (file.exists()) file.delete()
                        dbMediaUri.value = fileName
                        dbMediaUri.imageNoteText = imageNote ?: ""
                        mediaNotesInteractor.updateMediaNote(dbMediaUri)
                    } else {
                        val dbMediaUri = DbMediaNotes(
                            id = UUID.randomUUID().toString(),
                            value = fileName,
                            mediaType = MediaItemType.TYPE_PHOTO,
                            imageNoteText = imageNote ?: "",
                            order = System.currentTimeMillis(),
                            downloadPercent = 100,
                            uploadPercent = 0
                        )
                        saveDbMediaNotes(
                            dbMediaUri
                        )
                    }

                }.join()
                _state.postValue(ImageCropStates.BitmapSavedState(fileName))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
        }
    }

}