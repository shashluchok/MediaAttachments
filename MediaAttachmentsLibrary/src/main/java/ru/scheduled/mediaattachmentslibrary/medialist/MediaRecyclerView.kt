package ru.scheduled.mediaattachmentslibrary.medialist

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import ru.scheduled.mediaattachmentslibrary.PreviewApi
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType
import ru.scheduled.mediaattachmentslibrary.data.MediaNote
import ru.scheduled.mediaattachmentslibrary.databinding.ItemMediaNoteBinding
import ru.scheduled.mediaattachmentslibrary.setup
import ru.scheduled.mediaattachmentslibrary.utils.isVisible

class MediaRecyclerView : RecyclerView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?) : super(context!!)

    init {
        layoutManager = ScrollControlLayoutManager(context).also {
            it.stackFromEnd = true
        }
    }

    fun releasePlayer() {
        (adapter as? MediaAdapter)?.releasePlayer()
    }

    fun initRecycler(
        mediaPlayer: MediaPlayer,
        onItemsSelected: (List<MediaNote>) -> Unit,
        onItemClicked: (MediaNote) -> Unit,
        onCancelDownloading: (MediaNote) -> Unit,
        onCancelUploading: (MediaNote) -> Unit,
        onStartDownloading: (MediaNote) -> Unit,
        previewApi: PreviewApi? = null
    ) {

        adapter = MediaAdapter(
            onItemsSelected = onItemsSelected,
            onItemClicked = onItemClicked,
            onCancelDownloading = onCancelDownloading,
            onCancelUploading = onCancelUploading,
            onStartDownloading = onStartDownloading,
            previewApi = previewApi
        ).also {
            it.setMediaPlayer(mediaPlayer)
        }

    }

    fun getSelectedMediaNotes(): List<MediaNote> {
        return (adapter as MediaAdapter).getSelectedItems()
    }

    fun getData(): List<MediaNote> {
        return (adapter as MediaAdapter).getData()
    }

    fun setData(data: List<MediaNote>) {
        if (adapter !is MediaAdapter) {
            return
        } else {
            (adapter as MediaAdapter).setData(data)
            setup(
                items = data,
                bindingClass = ItemMediaNoteBinding::inflate,
            ) { item, binding ->
                val uploadPercent =item.uploadPercent
                val downloadPercent = item.downloadPercent

                val loadingLayoutCl: View?
                val loadingLayoutIv: ImageView?
                val loadingLayoutProgressIndicator: CircularProgressIndicator?

                binding.apply {
                    when(item.mediaType){
                        MediaItemType.TYPE_SKETCH -> {
                            itemMediaNoteSketch.root.apply {
                                isVisible = true

                            }
                        }
                        MediaItemType.TYPE_VOICE -> {
                            itemMediaNoteVoice.root.isVisible = true
                        }
                        MediaItemType.TYPE_PHOTO -> {
                            itemMediaNotePhoto.root.isVisible = true
                        }
                        MediaItemType.TYPE_TEXT -> {
                            itemMediaNoteText.root.isVisible = true
                        }
                    }
                }

            }

        }
    }

    fun stopSelecting() {
        (adapter as MediaAdapter).stopSelecting()
    }

}