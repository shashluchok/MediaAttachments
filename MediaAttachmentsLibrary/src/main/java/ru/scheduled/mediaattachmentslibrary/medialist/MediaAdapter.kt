package ru.scheduled.mediaattachmentslibrary.medialist

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import ru.scheduled.mediaattachmentslibrary.*
import ru.scheduled.mediaattachmentslibrary.data.MediaNote
import ru.scheduled.mediaattachmentslibrary.data.MediaNoteStatus
import ru.scheduled.mediaattachmentslibrary.widgets.ShimmerImage

private const val removeAnimationScaleX = 1.5F
private const val removeAnimationScaleY = 1.5F
private const val animationDuration = 150L

class MediaAdapter(
    private val onItemsSelected: (List<MediaNote>) -> Unit,
    private val onItemClicked: (MediaNote) -> Unit,
    private val onStartDownloading: (MediaNote) -> Unit,
    private val onCancelDownloading: (MediaNote) -> Unit,
    private val onCancelUploading: (MediaNote) -> Unit,
    private val previewApi: PreviewApi?
) : RecyclerView.Adapter<MediaAdapter.NotesViewHolder>() {

    inner class NotesViewHolder(v: View) : RecyclerView.ViewHolder(v), AnimateViewHolder {
        override fun animateAddImpl(
            holder: RecyclerView.ViewHolder,
            listener: Animator.AnimatorListener
        ) {
            itemView.animate().apply {
                alpha(1f)
                duration = animationDuration
                setListener(listener)
            }.start()
        }

        override fun animateRemoveImpl(
            holder: RecyclerView.ViewHolder,
            listener: Animator.AnimatorListener
        ) {
            itemView.animate().apply {
                alpha(0f)
                    .scaleX(removeAnimationScaleX)
                    .scaleY(removeAnimationScaleY)
                duration = animationDuration
                interpolator = interpolator
            }.start()
        }

        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            itemView.alpha = 0f
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder) {}

    }

    private val previews = mutableMapOf<String, ByteArray>()

    private var mediaPlayer: MediaPlayer? = null

    private val mediaList = mutableListOf<MediaNote>()

    private var currentVoice = -1
    private var currentHolder: View? = null

    private var recognizedSpeechPositions = mutableListOf<Int>()

    private lateinit var parentRecyclerView: RecyclerView

    private var isSelecting = false

    companion object {
        const val TYPE_SKETCH = 0
        const val TYPE_TEXT = 1
        const val TYPE_VOICE = 2
        const val TYPE_PHOTO = 3
    }

    private val selectedNotes = mutableListOf<MediaNote>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }

    fun getData(): List<MediaNote> {
        return mediaList
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val layoutId = when (viewType) {
            TYPE_SKETCH -> R.layout.item_media_note_sketch
            TYPE_VOICE -> R.layout.item_voice
            TYPE_PHOTO -> R.layout.item_media_note_photo
            TYPE_TEXT -> R.layout.item_media_note_text
            else -> R.layout.item_voice
        }

        val layout = LayoutInflater.from(parent.context).inflate(
            layoutId,
            parent,
            false
        )

        return NotesViewHolder(layout)
    }

/*    override fun getItemViewType(position: Int): Int {
        return when (mediaList[position].mediaType) {
            MediaRecyclerView.MediaItemTypes.TYPE_SKETCH -> TYPE_SKETCH
            MediaRecyclerView.MediaItemTypes.TYPE_VOICE -> TYPE_VOICE
            MediaRecyclerView.MediaItemTypes.TYPE_PHOTO -> TYPE_PHOTO
            MediaRecyclerView.MediaItemTypes.TYPE_TEXT -> TYPE_TEXT
        }
    }*/

    fun getSelectedItems(): List<MediaNote> {
        return selectedNotes
    }

    fun stopSelecting() {
        selectedNotes.clear()
        onSelecting(false)
    }

    fun setMediaPlayer(mediaPlayer: MediaPlayer) {
        this.mediaPlayer = mediaPlayer
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: NotesViewHolder, s: Int) {
        val position = holder.bindingAdapterPosition
        val contentView: View?= null
        val checkBox: ImageView?= null
        val selectionView: View?= null
        var shimmerImage: ShimmerImage? = null
        val viewToSetOnClickListener: View?= null
        val loadingLayoutCl: View?= null
        val loadingLayoutIv: ImageView?= null
        val loadingLayoutProgressIndicator: CircularProgressIndicator?= null

        val uploadPercent = mediaList[position].uploadPercent
        val downloadPercent = mediaList[position].downloadPercent

        when (getItemViewType(position)) {

            TYPE_SKETCH -> {
                /*loadingLayoutCl = holder.itemView.loadingLayoutSketch
                loadingLayoutIv = holder.itemView.loadingLayoutIv
                loadingLayoutProgressIndicator = holder.itemView.loadingLayoutProgress
                viewToSetOnClickListener = holder.itemView.item_media_note_sketch_cv
                selectionView = holder.itemView.selection_view_sketch
                checkBox = holder.itemView.note_checkbox_sketch
                contentView = holder.itemView.item_media_note_sketch_cv
                shimmerImage = holder.itemView.lf_shimmer_image*/
            }

            TYPE_VOICE -> {
              /*  loadingLayoutCl = holder.itemView.loadingLayoutVoiceCl
                loadingLayoutIv = holder.itemView.loadingLayoutIv
                loadingLayoutProgressIndicator = holder.itemView.loadingLayoutProgress
                viewToSetOnClickListener = null
                selectionView = holder.itemView.selection_view_voice
                checkBox = holder.itemView.note_checkbox_voice
                contentView = holder.itemView.voiceNoteCl


                holder.itemView.visualizer_view.apply {


                    mediaPlayer?.let { player ->
                        initVisualizer(
                            player = player,
                            amplitudes = mediaList[position].voiceAmplitudesList,
                            file = File(mediaList[position].value),
                            isCurrentVisualizer = currentVoice == position,
                            duration = mediaList[position].audioDuration ?: 0,
                            isActive = when (mediaList[position].status) {
                                MediaNoteStatus.synchronized -> {
                                    downloadPercent == 100
                                }
                                else -> {
                                    false
                                }

                            }
                        )
                    }


                    setOnSeekBarPointerOnCallback { isPointerOn ->
                        setParentRecyclerDraggability(!isPointerOn)
                    }
                    setOnCompleteCallback {
                        currentVoice = -1
                    }

                    setOnPlayClickCallback {
                        if (currentVoice != position) {
                            currentHolder?.let { holder ->
                                holder.visualizer_view.cancel()
                            }

                        }
                        if (!(mediaPlayer?.isPlaying ?: false) && currentVoice != position) {
                            releasePlayer()
                            currentHolder = holder.itemView
                            currentVoice = position

                        }

                    }
                }*/

            }

            TYPE_PHOTO -> {
               /* loadingLayoutCl = holder.itemView.loadingLayout
                loadingLayoutIv = holder.itemView.loadingLayout.loadingLayoutIv
                loadingLayoutProgressIndicator = holder.itemView.loadingLayout.loadingLayoutProgress
                viewToSetOnClickListener = holder.itemView.item_media_note_photo_cv
                selectionView = holder.itemView.selection_view_photo
                contentView = holder.itemView.item_media_note_photo_cv
                checkBox = holder.itemView.note_checkbox_photo
                shimmerImage = holder.itemView.lf_shimmer_image_photo

                if (!mediaList[position].imageNoteText.isNullOrEmpty()) {
                    holder.itemView.media_note_photo_tv.apply {
                        text = mediaList[position].imageNoteText
                        visibility = View.VISIBLE
                    }
                }*/
            }

            TYPE_TEXT -> {
               /* loadingLayoutCl = null
                loadingLayoutIv = null
                loadingLayoutProgressIndicator = null
                viewToSetOnClickListener = null
                holder.itemView.textUploadingIv.isVisible = uploadPercent in 0 until 100
                selectionView = holder.itemView.selection_view_text
                checkBox = holder.itemView.note_checkbox_text
                contentView = holder.itemView.note_text_cv
                holder.itemView.item_media_note_text_tv.setText(mediaList[position].value)*/
            }

            else -> {
               /* loadingLayoutCl = null
                loadingLayoutIv = null
                loadingLayoutProgressIndicator = null
                viewToSetOnClickListener = null
                checkBox = null
                contentView = null
                selectionView = null*/
            }
        }




        when (mediaList[position].status) {
            MediaNoteStatus.uploading -> {
                shimmerImage?.apply {
                    stopShimmer()
                    loadImage(mediaList[position].value)
                }
            }

            MediaNoteStatus.downloading -> {
                shimmerImage?.apply {

                    if (previews.get(mediaList[position].id) != null) {
                        stopShimmer()
                    } else {
                        previewApi?.let {
                            mediaList[position].previewKey?.let { key ->
                                loadPreview(
                                    it, key,
                                    onPreviewImageByteArrayLoaded = {
                                        stopShimmer()
                                        previews.put(mediaList[position].id, it)
                                    },
                                    previousPreview = previews.get(mediaList[position].id)
                                )
                            }
                        }
                        startShimmer()
                    }

                    setAfterEffect(afterEffect = ShimmerImage.AfterEffect.BLUR)
                }
            }

            MediaNoteStatus.synchronized -> {
                shimmerImage?.apply {

                    if (downloadPercent == 100 && uploadPercent == 100) {
                        stopShimmer()
                        removeAfterEffects()
                        loadImage(mediaList[position].value)
                    } else if (uploadPercent == 100) {
                        stopShimmer()
                        stopLoadingPreview()
                    }

                }

            }
        }

        if (uploadPercent == 100) {

            if (downloadPercent in 0 until 100) {

                loadingLayoutCl?.visibility = View.VISIBLE

                if (downloadPercent == 0) {
                    if (mediaList[position].status == MediaNoteStatus.synchronized) {
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

                val imageId = if (mediaList[position].status == MediaNoteStatus.synchronized) {
                    R.drawable.download
                } else R.drawable.close_new

                loadingLayoutIv?.apply {
                    setImageResource(imageId)
                    setOnClickListener {
                        try {
                            if (mediaList[position].status == MediaNoteStatus.synchronized) {
                                onStartDownloading.invoke(mediaList[position])
//                            mediaList[position].isLoadingStopped = false
                            } else {
                                onCancelDownloading.invoke(mediaList[position])
                                mediaList[position].isLoadingStopped = true


                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val imageId =
                            if (mediaList[position].status == MediaNoteStatus.synchronized) {
                                R.drawable.download
                            } else R.drawable.close_new
                        setImageResource(imageId)
                    }
                }

            } else if (downloadPercent == 100) {
                loadingLayoutCl?.visibility = View.GONE
                mediaList[position].isLoadingStopped = true
            }

        } else {
            if (uploadPercent in 0 until 100) {

                if (uploadPercent == 0) {
                    loadingLayoutProgressIndicator?.isIndeterminate = true
                    loadingLayoutProgressIndicator?.progress = 5
                } else {
                    loadingLayoutProgressIndicator?.isIndeterminate = false
                    loadingLayoutProgressIndicator?.progress = uploadPercent
                }


                loadingLayoutCl?.visibility = View.VISIBLE
                val imageId = R.drawable.close_new

                loadingLayoutIv?.apply {
                    setImageResource(imageId)
                    setOnClickListener {
                        try {
                            onCancelUploading.invoke(mediaList[position])
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (uploadPercent == 100) {
                loadingLayoutCl?.visibility = View.GONE
                mediaList[position].isLoadingStopped = true
            }
        }




        viewToSetOnClickListener?.setOnClickListener {
            try {
                if (mediaList[position].downloadPercent == 100 && mediaList[position].uploadPercent == 100) {
                    onItemClicked.invoke(mediaList[position])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        selectionView?.isVisible = isSelecting

        if (isSelecting) {

            releasePlayer()
            if (uploadPercent == 100) {
                if (selectedNotes.contains(mediaList[position])) {
                    checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)
                    holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))

                } else {
                    checkBox?.setImageResource(R.drawable.checkbox_to_checked)
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                }
                contentView?.apply {
                    animate().x(dpToPx(48)).duration = 0
                }

                checkBox?.apply {
                    animate().x(dpToPx(12)).duration = 0
                }
            }
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            contentView?.apply {
                animate().x(dpToPx(12)).duration = 0
            }

            checkBox?.apply {
                animate().x(dpToPx(-24)).duration = 0
            }
        }
        /*if (contentView == holder.itemView.voiceNoteCl) {
            holder.itemView.visualizer_view.setOnLongClickCallback {
                if (!isSelecting && mediaList[position].uploadPercent == 100) {
                    onSelecting(true)
                    holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))
                    selectedNotes.add(mediaList[position])
                    onItemsSelected.invoke(selectedNotes)
                    checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)
                }
            }
        } else {

            contentView?.setOnLongClickListener {
                if (!isSelecting && mediaList[position].uploadPercent == 100) {
                    onSelecting(true)
                    holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))
                    selectedNotes.add(mediaList[position])
                    onItemsSelected.invoke(selectedNotes)
                    checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)
                }
                true
            }

        }*/

        selectionView?.setOnClickListener {
            try {

                if (isSelecting && mediaList[position].uploadPercent == 100) {
                    if (selectedNotes.contains(mediaList[position])) {
                        selectedNotes.remove(mediaList[position])
                        checkBox?.setImageResource(R.drawable.checkbox_to_checked)
                        if (selectedNotes.isEmpty()) {
                            onSelecting(false)
                        }
                        onItemsSelected.invoke(selectedNotes)
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))
                        selectedNotes.add(mediaList[position])
                        onItemsSelected.invoke(selectedNotes)
                        checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)

                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }


    override fun getItemCount(): Int {
        return mediaList.size
    }

    fun setData(newData: List<MediaNote>) {
        val oldList = mutableListOf<MediaNote>().also {
            it.addAll(mediaList)
        }

        when {
            oldList.size == 0 || newData.size == 0 -> {
                mediaList.clear()
                mediaList.addAll(newData)
                notifyDataSetChanged()
            }

            newData.size < oldList.size -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    oldList.onEach {
                        val ind = mediaList.indexOf(it)
                        if (!newData.contains(it) && !newData.map { it.id }
                                .contains(it.id) && ind >= 0) {
                            mediaList.removeAt(ind)
                            notifyItemRemoved(ind)
                        }
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyItemRangeChanged(0, itemCount)
                    }, 100)
                }, 200)
            }

            newData.size > oldList.size -> {
                mediaList.clear()
                mediaList.addAll(newData)
                newData.onEach {
                    val ind = newData.indexOf(it)
                    if (!oldList.contains(it) && !oldList.map { it.id }.contains(it.id) && ind >= 0)
                        notifyItemInserted(newData.indexOf(it))
                }
                parentRecyclerView.scrollToPosition(mediaList.lastIndex)

            }

            newData.size == oldList.size -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    newData.onEach {
                        if (!oldList.contains(it)) {
                            var wasChanged = false
                            if (mediaList.isNotEmpty()) {
                                mediaList[newData.indexOf(it)].apply {

                                    if (it.value != value
                                        || it.downloadPercent != downloadPercent
                                        || it.uploadPercent != uploadPercent
                                        || it.recognizedSpeechText != recognizedSpeechText
                                        || it.status != status
                                        || it.imageNoteText != imageNoteText
                                    ) wasChanged = true

                                    value = it.value
                                    status = it.status
                                    recognizedSpeechText = it.recognizedSpeechText
                                    downloadPercent = it.downloadPercent
                                    uploadPercent = it.uploadPercent
                                    imageNoteText = it.imageNoteText
                                }
                                if (wasChanged) notifyItemChanged(newData.indexOf(it))
                            }
                        }
                    }
                }, 200)

            }
        }
        releasePlayer()

    }

    private fun setParentRecyclerDraggability(isDraggable: Boolean) {
        (parentRecyclerView.layoutManager as ScrollControlLayoutManager).setScrollEnabled(
            isDraggable
        )
    }

    private fun dpToPx(dp: Int): Float {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        return dp * displayMetrics.density
    }

    fun releasePlayer() {
       /* currentHolder?.let { holder ->
            holder.visualizer_view.cancel()
        }
        currentHolder = null
        currentVoice = -1*/
    }


    private fun onSelecting(isSelecting: Boolean) {
        this.isSelecting = isSelecting

        val firstVisibleItemPosition =
            (parentRecyclerView.layoutManager as ScrollControlLayoutManager).findFirstVisibleItemPosition()
        val visibleItemsSize = parentRecyclerView.childCount

        val listOfViews = mutableListOf<Triple<ImageView?, View?, View?>>()

        for (i in firstVisibleItemPosition until firstVisibleItemPosition + visibleItemsSize) {
            val checkBoxM: ImageView?
            val contentViewM: View?
            val selectionViewM: View?
            parentRecyclerView.findViewHolderForAdapterPosition(i)?.itemView?.let {
                if (mediaList[i].uploadPercent == 100) {
                   /* when (mediaList[i].mediaType) {
                        MediaRecyclerView.MediaItemTypes.TYPE_SKETCH -> {
                            checkBoxM = it.note_checkbox_sketch
                            contentViewM = it.item_media_note_sketch_cv
                            selectionViewM = it.selection_view_sketch
                        }

                        MediaRecyclerView.MediaItemTypes.TYPE_VOICE -> {
                            checkBoxM = it.note_checkbox_voice
                            contentViewM = it.voiceNoteCl
                            selectionViewM = it.selection_view_voice
                        }

                        MediaRecyclerView.MediaItemTypes.TYPE_PHOTO -> {
                            checkBoxM = it.note_checkbox_photo
                            contentViewM = it.item_media_note_photo_cv
                            selectionViewM = it.selection_view_photo
                        }

                        MediaRecyclerView.MediaItemTypes.TYPE_TEXT -> {
                            checkBoxM = it.note_checkbox_text
                            contentViewM = it.note_text_cv
                            selectionViewM = it.selection_view_text
                        }


                    }*/
                   /* if (!isSelecting) {
                        it.setBackgroundColor(Color.TRANSPARENT)
                    }
                    listOfViews.add(Triple(checkBoxM, contentViewM, selectionViewM))*/
                }
            }
        }

        listOfViews.onEach {
            val contentOffset: Int
            val checkBoxOffset: Int



            if (isSelecting) {
                contentOffset = 48
                checkBoxOffset = 12

            } else {
                contentOffset = 12
                checkBoxOffset = -24
                it.first?.setImageResource(R.drawable.checkbox_to_checked)
            }
            it.second?.apply {
                animate().x(dpToPx(contentOffset)).duration = 100
            }
            it.first?.apply {
                animate().x(dpToPx(checkBoxOffset)).duration = 100
            }
            it.third?.isVisible = isSelecting
        }

        for (i in 0 until itemCount) {
            if (!(firstVisibleItemPosition until firstVisibleItemPosition + visibleItemsSize).contains(
                    i
                )
            ) {
                notifyItemChanged(i)
            }
        }
    }
}
