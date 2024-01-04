package ru.mediaattachments.presentation.medialist

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.recyclerview.animators.LandingAnimator
import ru.mediaattachments.domain.api.PreviewApi
import ru.mediaattachments.data.ui.UiMediaAttachment

class MediaListRecyclerView : RecyclerView {

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
        itemAnimator = LandingAnimator(LinearInterpolator()).apply {
            addDuration = 100
            removeDuration = 150
            changeDuration = 300
            moveDuration = 100
        }
    }

    fun releasePlayer() {
        (adapter as? MediaListAdapter)?.releasePlayer()
    }

    fun initRecycler(
        mediaPlayer: MediaPlayer,
        onItemsSelected: (List<UiMediaAttachment>) -> Unit,
        onItemClicked: (UiMediaAttachment) -> Unit,
        onCancelDownloading: (UiMediaAttachment) -> Unit,
        onCancelUploading: (UiMediaAttachment) -> Unit,
        onStartDownloading: (UiMediaAttachment) -> Unit,
        previewApi: PreviewApi? = null
    ) {

        adapter = MediaListAdapter(
            onItemsSelected = onItemsSelected,
            onItemClicked = onItemClicked,
            onCancelDownloading = onCancelDownloading,
            onCancelUploading = onCancelUploading,
            onStartDownloading = onStartDownloading,
            previewApi = previewApi,
            mediaPlayer = mediaPlayer
        )
    }

    fun getSelectedMediaNotes(): List<UiMediaAttachment> {
        return (adapter as MediaListAdapter).getSelectedItems()
    }

    fun setData(data: List<UiMediaAttachment>) {
        if (adapter !is MediaListAdapter) {
            return
        } else {
            (adapter as MediaListAdapter).setData(data)
        }
    }

    fun stopSelecting() {
        (adapter as MediaListAdapter).stopSelecting()
    }

    fun pausePlayer() {
        (adapter as MediaListAdapter).pausePlayer()
    }

}
