package ru.leadfrog.ui.media_attachments.camera_capture.image_crop

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.leadfrog.db.media_uris.MediaNotesRepository
import ru.leadfrog.ui.base.BaseViewModel
import ru.leadfrog.db.media_uris.DbMediaNotes
import ru.leadfrog.ui.media_attachments.MediaConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageCropViewModel(
    private val mediaNotesInteractor: MediaNotesRepository
) : BaseViewModel<ImageCropStates>() {

    fun getMediaNote(id:String){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val note = mediaNotesInteractor.getMediaNoteById(id)
                withContext(Dispatchers.Main){
                    _state.value = ImageCropStates.ExistingMediaNoteLoadedState(mediaNote = note)
                }
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    fun deleteOriginalPhoto(photoPath:String){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val file = File(photoPath)
                if(file.exists()) file.delete()
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap, shardId: String, imageNote: String?, existingNoteId:String?) {
        CoroutineScope(Dispatchers.IO).launch {

            val contentValues =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        ContentValues().apply {
                            val name = SimpleDateFormat(
                                    "HHmmssddMMyyyy",
                                    Locale.US
                            ).format(System.currentTimeMillis()) + ".jpg"
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/LeadFrog")
                    }
                } else {
                    ContentValues().apply {
                        put(
                                MediaStore.Images.Media.TITLE,
                                SimpleDateFormat("HHmmssddMMyyyy", Locale.US).format(
                                        System.currentTimeMillis()
                                )
                        )
                        put(
                                MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat(
                                "HHmmssddMMyyyy",
                                Locale.US
                        ).format(System.currentTimeMillis())
                        )
                        put(
                                MediaStore.Images.Media.DESCRIPTION, SimpleDateFormat(
                                "HHmmssddMMyyyy",
                                Locale.US
                        ).format(System.currentTimeMillis())
                        )
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                                MediaStore.Images.Media.DATE_ADDED,
                                System.currentTimeMillis() / 1000
                        )
                    }

                }

            var imageOut: OutputStream? = null
            launch(Dispatchers.IO) {
                try {
                    val url = appContext.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    )

                    if (url != null) {
                        imageOut = appContext.contentResolver.openOutputStream(url);
                        imageOut.use { imageOut ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, imageOut);
                        }

                        if(existingNoteId!=null){
                            val dbMediaUri =   mediaNotesInteractor.getMediaNoteById(existingNoteId).also { it.value }
                            val file = File(dbMediaUri.value)
                            if(file.exists()) file.delete()
                            dbMediaUri.value = url.toString()
                            dbMediaUri.imageNoteText = imageNote?:""
                            mediaNotesInteractor.updateMediaNote(dbMediaUri)
                        }
                        else {
                            val dbMediaUri =  DbMediaNotes(
                                id = UUID.randomUUID().toString(),
                                shardId = shardId,
                                value = url.toString(),
                                mediaType = "photo",
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
                    _state.postValue(ImageCropStates.ErrorState("Ошибка при сохранении файла"))
                } finally {
                    imageOut?.flush()
                    imageOut?.close()
                }
            }
        }

    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, shardId: String, imageNote: String?,existingNoteId:String?) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val mydir = appContext.getDir(MediaConstants.MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

                if (!mydir.exists()) {
                    mydir.mkdirs()
                }

                val fileName =
                        "$mydir/${System.currentTimeMillis()}.jpeg"
                launch(Dispatchers.IO) {
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    f.createNewFile()
                    val fos = FileOutputStream(f)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val image = stream.toByteArray()
                    fos.write(image)
                    fos.flush()
                    fos.close()

                    if(existingNoteId!=null){
                        val dbMediaUri =   mediaNotesInteractor.getMediaNoteById(existingNoteId).also { it.value }
                        val file = File(dbMediaUri.value)
                        if(file.exists()) file.delete()
                        dbMediaUri.value = fileName.toString()
                        dbMediaUri.imageNoteText = imageNote?:""
                        mediaNotesInteractor.updateMediaNote(dbMediaUri)
                    }
                    else {
                        val dbMediaUri = DbMediaNotes(
                            id = UUID.randomUUID().toString(),
                            shardId = shardId,
                            value = fileName.toString(),
                            mediaType = "photo",
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
                _state.postValue(ImageCropStates.ErrorState("Ошибка при сохранении файла"))
            }

        }

    }

    private fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
        }
    }


}