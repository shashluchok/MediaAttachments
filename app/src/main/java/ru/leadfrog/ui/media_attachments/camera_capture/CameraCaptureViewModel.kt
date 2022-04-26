package ru.leadfrog.ui.media_attachments.camera_capture

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.leadfrog.db.media_uris.MediaNotesRepository
import ru.leadfrog.ui.base.BaseViewModel
import ru.leadfrog.db.media_uris.DbMediaNotes
import java.io.File
import java.util.*


class CameraCaptureViewModel(
    private val mediaNotesInteractor: MediaNotesRepository
) : BaseViewModel<CameraCaptureStates>() {

    @SuppressLint("Range")
    fun getLastCameraImage() {
        viewModelScope.launch(Dispatchers.IO) {
            var lastImageUri = ""
            var imageCursor: Cursor? = null
            try {
                val columns =
                    arrayOf(MediaStore.Images.Media.DATA)
                val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
                imageCursor = appContext.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        columns,
                        null,
                        null,
                        orderBy
                )
                if (imageCursor != null) {
                    if (imageCursor.moveToNext()) {
                        lastImageUri =
                            imageCursor.getString(
                                    imageCursor.getColumnIndex(
                                            MediaStore.Images.Media.DATA
                                    )
                            )

                    }
                }
            } catch (e: Exception) {
                _state.postValue(CameraCaptureStates.ErrorState("Ошибка при чтении галереи"))
                e.printStackTrace()
            } finally {
                if (imageCursor != null && !imageCursor.isClosed) {
                    imageCursor.close();
                }
                _state.postValue(CameraCaptureStates.UriLoadedState(lastImageUri))
            }
        }
    }

    fun saveVideoToCache( uri: Uri, shardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var cursor: Cursor? = null

            try {
                val fileName =
                        "${createDir(shardId)}/${System.currentTimeMillis()}.mp4"
                launch(Dispatchers.IO) {
                    val f = File(fileName)
                    if (f.exists()) f.delete()
                    val proj = arrayOf(MediaStore.Video.Media.DATA)
                    cursor = appContext.contentResolver.query(uri, proj, null, null, null)
                    val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    cursor!!.moveToFirst()
                    val path = cursor!!.getString(column_index)
                    cursor?.close()


                    val oldFile = File(path)
                    oldFile.copyTo(f)

                    try{
                        oldFile.delete()
                    }
                    catch (e: java.lang.Exception){
                        e.printStackTrace()
                    }

                    val dbMediaUri = DbMediaNotes(
                        id = UUID.randomUUID().toString(),
                        value = fileName.toString(),
                        shardId = shardId,
                        mediaType = "video",
                        order = System.currentTimeMillis()
                    )
                        saveDbMediaNotes(
                            dbMediaUri
                        )

                }.join()
                _state.postValue(CameraCaptureStates.VideoSavedState(fileName))
            } catch (e: java.lang.Exception) {
                _state.postValue(CameraCaptureStates.ErrorState("Ошибка при сохранении файла"))
            } finally {
                cursor?.close()
            }

        }

    }

    private fun saveDbMediaNotes(dbMediaNote: DbMediaNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaNotesInteractor.saveMediaNotes(dbMediaNote)
        }
    }

    private fun createDir(id: String): String {
        val shardIdFolder =
                File(appContext.externalCacheDir, File.separator + id)
        if (!shardIdFolder.exists()) {
            shardIdFolder.mkdirs()
        }
        return shardIdFolder.path

    }

}