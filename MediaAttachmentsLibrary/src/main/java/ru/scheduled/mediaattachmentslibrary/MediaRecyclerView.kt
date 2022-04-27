package ru.scheduled.mediaattachmentslibrary

import android.content.Context
import android.media.MediaPlayer
import android.os.Parcelable
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import ru.scheduled.mediaattachmentslibrary.adapters.MediaAdapter

class MediaRecyclerView : RecyclerView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?) : super(context!!)

    enum class MediaItemTypes {
        TYPE_SKETCH,
        TYPE_VOICE,
        TYPE_PHOTO,
        TYPE_TEXT,
//        TYPE_VIDEO
    }

    init {
        this.layoutManager = CustomLinearLayoutManager(context).also {
            it.stackFromEnd = true
        }

    }

    fun releasePlayer() {
        if (this.adapter !is MediaAdapter) {
            return
        }
        (this.adapter as MediaAdapter).releasePlayer()
    }

    fun initRecycler(
        mediaPlayer: MediaPlayer,
        onItemsSelected: (List<MediaNote>) -> Unit,
        onItemClicked: (MediaNote) -> Unit,
        onCancelDownloading: (MediaNote) -> Unit,
        onCancelUploading: (MediaNote) -> Unit,
        onStartDownloading: (MediaNote) -> Unit
    ) {
        this.adapter = MediaAdapter(
            onItemsSelected = onItemsSelected,
            onItemClicked = onItemClicked,
            onCancelDownloading = onCancelDownloading ,
            onCancelUploading = onCancelUploading,
            onStartDownloading = onStartDownloading
        ).also {
            it.setMediaPlayer(mediaPlayer)
        }

    }

    fun getSelectedMediaNotes(): List<MediaNote>{
        return (this.adapter as MediaAdapter).getSelectedItems()
    }

    fun getData():List<MediaNote>{
        return (this.adapter as MediaAdapter).getData()
    }

    fun setData(data: List<MediaNote>) {
        if (this.adapter !is MediaAdapter) {
            return
        } else {
            (this.adapter as MediaAdapter).setData(data)
        }
    }

    fun stopSelecting(){
        (this.adapter as MediaAdapter).stopSelecting()
    }

    @Parcelize
    data class MediaNote(
        val id:String,
        val mediaType: MediaRecyclerView.MediaItemTypes,
        var value: String,
        var recognizedSpeechText: String,
        val voiceAmplitudesList: List<Int>,
        val imageNoteText: String,
        val timeStamp:Long,
        var isChosen:Boolean = false,
        var downloadPercent:Int,
        var uploadPercent:Int,
        var isLoadingStopped:Boolean = true

    ):Parcelable
}