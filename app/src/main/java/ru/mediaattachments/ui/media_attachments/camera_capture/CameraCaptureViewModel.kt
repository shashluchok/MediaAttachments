package ru.mediaattachments.ui.media_attachments.camera_capture

import android.annotation.SuppressLint
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.mediaattachments.ui.base.BaseViewModel
import java.util.*


class CameraCaptureViewModel : BaseViewModel<CameraCaptureStates>() {

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
                e.printStackTrace()
            } finally {
                if (imageCursor != null && !imageCursor.isClosed) {
                    imageCursor.close();
                }
                _state.postValue(CameraCaptureStates.UriLoadedState(lastImageUri))
            }
        }
    }

}