package ru.mediaattachments.presentation.medialist

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import ru.mediaattachments.domain.api.PreviewApi
import ru.mediaattachments.R
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.data.ui.MediaNoteStatus
import ru.mediaattachments.data.ui.UiMediaAttachment
import ru.mediaattachments.databinding.ItemMediaNoteBinding
import ru.mediaattachments.presentation.widgets.shimmerimage.ShimmerImage
import ru.mediaattachments.utils.*
import java.io.File

private const val selectedItemColor = "#E3F5FF"
private const val dateFormat = "HH:mm | dd.MM.yyyy"
private const val animationDuration = 50L
private const val loadingOffset = 12

class MediaListAdapter(
    private val onItemsSelected: (List<UiMediaAttachment>) -> Unit,
    private val onItemClicked: (UiMediaAttachment) -> Unit,
    private val onStartDownloading: (UiMediaAttachment) -> Unit,
    private val onCancelDownloading: (UiMediaAttachment) -> Unit,
    private val onCancelUploading: (UiMediaAttachment) -> Unit,
    private val mediaPlayer: MediaPlayer,
    private val previewApi: PreviewApi?
) : RecyclerView.Adapter<MediaListAdapter.NotesViewHolder>() {

    class NotesViewHolder(val binding: ItemMediaNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding =
            ItemMediaNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesViewHolder(binding)
    }


    private val previews = mutableMapOf<String, ByteArray>()

    private val mediaList = mutableListOf<UiMediaAttachment>()

    private var currentVoice = -1
    private var currentHolder: NotesViewHolder? = null


    private lateinit var parentRecyclerView: RecyclerView

    private val selectedNotes = mutableListOf<UiMediaAttachment>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }

    fun getSelectedItems(): List<UiMediaAttachment> {
        return selectedNotes
    }

    fun stopSelecting() {
        onSelecting(null)
    }

    fun pausePlayer() {
        mediaPlayer.pause()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val isSelecting = selectedNotes.isNotEmpty()

        val contentView: CardView = holder.binding.contentCardView
        val checkBox: MaterialCheckBox = holder.binding.selectionCheckbox
        var loadingLayoutCl: View? = holder.binding.loadingLayout.root
        var loadingLayoutIv: ImageView? = holder.binding.loadingLayout.loadingLayoutIv
        var loadingLayoutProgressIndicator: CircularProgressIndicator? =
            holder.binding.loadingLayout.loadingLayoutProgress
        val uploadPercent = mediaList[position].uploadPercent
        val downloadPercent = mediaList[position].downloadPercent
        val shimmerImage = holder.binding.shimmerImage

        holder.binding.apply {
            itemMediaNoteTextTv.isVisible = false
            textContent.isVisible = false
            visualizerView.isVisible = false
            shimmerImage.isVisible = false
            loadingLayoutCl?.isVisible = false
            selectionLayout.isVisible = false
        }

        holder.binding.selectionLayout.apply {
            isVisible = isSelecting
            setOnClickListener {
                onSelecting(mediaList[position])
            }
        }

        holder.binding.contentTimestamp.text =
            mediaList[position].createdAtTimeStamp.convertToDate(dateFormat)

        when (mediaList[position].mediaType) {

            MediaType.TYPE_SKETCH, MediaType.TYPE_PHOTO -> {

                shimmerImage.isVisible = true

                contentView.setOnClickListener {
                    if (mediaList[position].downloadPercent == 100 && mediaList[position].uploadPercent == 100) {
                        onItemClicked.invoke(mediaList[position])
                    }
                }

                if (mediaList[position].imageNoteText.isNotEmpty()) {
                    holder.binding.textContent.apply {
                        text = mediaList[position].imageNoteText
                        isVisible = true
                    }
                }

                when (mediaList[position].status) {
                    MediaNoteStatus.uploading -> {
                        shimmerImage.apply {
                            stopShimmer()
                            loadImage(mediaList[position].value)
                        }
                    }

                    MediaNoteStatus.downloading, MediaNoteStatus.waiting_download -> {
                        shimmerImage.apply {

                            if (previews[mediaList[position].id] != null) {
                                stopShimmer()
                            } else {
                                previewApi?.let {
                                    mediaList[position].previewKey?.let { key ->
                                        loadPreview(
                                            it, key,
                                            onPreviewImageByteArrayLoaded = {
                                                stopShimmer()
                                                previews[mediaList[position].id] = it
                                            },
                                            previousPreview = previews[mediaList[position].id]
                                        )
                                    }
                                }
                                startShimmer()
                            }
                            setAfterEffect(afterEffect = ShimmerImage.AfterEffect.BLUR)
                        }
                    }

                    MediaNoteStatus.synchronized -> {
                        shimmerImage.apply {
                            stopShimmer()
                            removeAfterEffects()
                            loadImage(mediaList[position].value)
                        }

                    }
                }

            }

            MediaType.TYPE_VOICE -> {
                contentView.setOnClickListener{}
                holder.binding.contentLayout.apply {
                    clearConstraints(
                        viewId = R.id.loadingLayout,
                        ConstraintSet.END,
                        ConstraintSet.BOTTOM
                    )
                    newConstraint(
                        R.id.loadingLayout,
                        ConstraintSet.START,
                        R.id.contentLayout,
                        ConstraintSet.START,
                        loadingOffset.toPx().toInt()
                    )
                    newConstraint(
                        R.id.loadingLayout,
                        ConstraintSet.TOP,
                        R.id.contentLayout,
                        ConstraintSet.TOP,
                        loadingOffset.toPx().toInt()
                    )
                }

                holder.binding.visualizerView.apply {
                    isVisible = true
                    initVisualizer(
                        player = mediaPlayer,
                        amplitudes = mediaList[position].voiceAmplitudesList,
                        file = File(mediaList[position].value),
                        isCurrentVisualizer = currentVoice == position,
                        duration = mediaList[position].audioDuration ?: 0,
                        isActive = mediaList[position].status == MediaNoteStatus.synchronized && downloadPercent == 100
                    )

                    setOnSeekBarPointerOnCallback { isPointerOn ->
                        setParentRecyclerDraggability(!isPointerOn)
                    }
                    setOnCompleteCallback {
                        currentVoice = -1
                    }

                    setOnPlayClickCallback {
                        if (currentVoice != position) {
                            currentHolder?.let { holder ->
                                holder.binding.visualizerView.cancel()
                            }
                        }
                        if (!mediaPlayer.isPlaying && currentVoice != position) {
                            releasePlayer()
                            currentHolder = holder
                            currentVoice = position

                        }

                    }
                }

            }


            MediaType.TYPE_TEXT -> {
                contentView.setOnClickListener{}
                loadingLayoutCl = null
                loadingLayoutIv = null
                loadingLayoutProgressIndicator = null
                holder.binding.itemMediaNoteTextTv.isVisible = true
                holder.binding.textUploadingIv.isVisible = uploadPercent in 0 until 100
                holder.binding.itemMediaNoteTextTv.text = mediaList[position].value
            }
        }


        if (uploadPercent == 100) {

            if (downloadPercent in 0 until 100) {

                loadingLayoutCl?.visibility = View.VISIBLE

                if (downloadPercent == 0) {
                    if (mediaList[position].status == MediaNoteStatus.waiting_download) {
                        loadingLayoutProgressIndicator?.isIndeterminate = false
                        loadingLayoutProgressIndicator?.progress = 0
                    } else {
                        loadingLayoutProgressIndicator?.isIndeterminate = true
                        loadingLayoutProgressIndicator?.progress = 5
                    }

                } else {
                    loadingLayoutProgressIndicator?.isIndeterminate = false
                    loadingLayoutProgressIndicator?.progress = downloadPercent
                }

                loadingLayoutIv?.apply {
                    val imageId =
                        if (mediaList[position].status == MediaNoteStatus.waiting_download) {
                            R.drawable.download
                        } else R.drawable.close_new
                    setImageResource(imageId)
                    setOnClickListener {
                        if (mediaList[position].status == MediaNoteStatus.waiting_download) {
                            onStartDownloading.invoke(mediaList[position])
                        } else {
                            onCancelDownloading.invoke(mediaList[position])
                        }
                    }
                }

            } else if (downloadPercent == 100) {
                loadingLayoutCl?.visibility = View.GONE
            }

        } else {
            loadingLayoutCl?.isVisible = true

            if (uploadPercent == 0) {
                loadingLayoutProgressIndicator?.isIndeterminate = true
                loadingLayoutProgressIndicator?.progress = 5
            } else {
                loadingLayoutProgressIndicator?.isIndeterminate = false
                loadingLayoutProgressIndicator?.progress = uploadPercent
            }


            loadingLayoutIv?.apply {
                setImageResource(R.drawable.close_new)
                setOnClickListener {
                    onCancelUploading.invoke(mediaList[position])
                }
            }
        }

        if (isSelecting) {
            if (mediaList[position].status == MediaNoteStatus.synchronized) {
                if (selectedNotes.contains(mediaList[position])) {
                    checkBox.isChecked = true
                    holder.itemView.setBackgroundColor(Color.parseColor(selectedItemColor))

                } else {
                    checkBox.isChecked = false
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                }
                contentView.animate().x(54.toPx()).duration = animationDuration
                checkBox.animate().x(12.toPx()).duration = animationDuration
            }
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            contentView.animate().x(12.toPx()).duration = animationDuration
            checkBox.animate().x((-24).toPx()).duration = animationDuration
        }
        if (mediaList[position].mediaType == MediaType.TYPE_VOICE) {
            holder.binding.visualizerView.setOnLongClickCallback {
                onSelecting(mediaList[position])
            }
        } else {
            contentView.setOnLongClickListener {
                onSelecting(mediaList[position])
                true
            }

        }

    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    fun setData(newData: List<UiMediaAttachment>) {
        val gameDiffUtilCallback = DiffUtilCallback(
            mediaList,
            newData
        )
        val diffCallback = DiffUtil.calculateDiff(gameDiffUtilCallback)
        val isNewItem = newData.size > mediaList.size
        mediaList.clear()
        mediaList.addAll(newData)
        diffCallback.dispatchUpdatesTo(this)
        releasePlayer()
        if(isNewItem){
            parentRecyclerView.smoothScrollToPosition(mediaList.size - 1)
        }
    }

    private fun setParentRecyclerDraggability(isDraggable: Boolean) {
        (parentRecyclerView.layoutManager as ScrollControlLayoutManager).setScrollEnabled(
            isDraggable
        )
    }

    fun releasePlayer() {
        currentHolder?.binding?.visualizerView?.cancel()
        currentHolder = null
        currentVoice = -1
    }


    private fun onSelecting(uiMediaAttachment: UiMediaAttachment?) {
        if (uiMediaAttachment != null) {
            if (uiMediaAttachment.status != MediaNoteStatus.synchronized) {
                return
            }
            selectedNotes.addOrRemove(uiMediaAttachment)
        } else selectedNotes.clear()

        notifyItemRangeChanged(
            0,
            itemCount
        )
        onItemsSelected.invoke(selectedNotes)
    }

}

private class DiffUtilCallback(
    private val oldItems: List<UiMediaAttachment>,
    private val newItems: List<UiMediaAttachment>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldItems.size
    override fun getNewListSize(): Int = newItems.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].id == newItems[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].value == newItems[newItemPosition].value
                && oldItems[oldItemPosition].mediaType == newItems[newItemPosition].mediaType
                && oldItems[oldItemPosition].downloadPercent  == newItems[newItemPosition].downloadPercent
                && oldItems[oldItemPosition].uploadPercent == newItems[newItemPosition].uploadPercent
                && oldItems[oldItemPosition].status == newItems[newItemPosition].status
                && oldItems[oldItemPosition].isSelected == newItems[newItemPosition].isSelected
                && oldItems[oldItemPosition].imageNoteText == newItems[newItemPosition].imageNoteText
    }
}
