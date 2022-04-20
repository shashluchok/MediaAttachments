package ru.scheduled.mediaattachmentslibrary.adapters

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaPlayer
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import kotlinx.android.synthetic.main.item_media_note_photo.view.*
import kotlinx.android.synthetic.main.item_media_note_sketch.view.*
import kotlinx.android.synthetic.main.item_media_note_text.view.*
import kotlinx.android.synthetic.main.item_voice.view.*
import ru.scheduled.mediaattachmentslibrary.CustomLinearLayoutManager
import ru.scheduled.mediaattachmentslibrary.MediaRecyclerView
import ru.scheduled.mediaattachmentslibrary.R
import java.io.File
import java.lang.Exception
import kotlin.math.abs


class MediaAdapter(
    private val onItemsSelected: (List<MediaRecyclerView.MediaNote>) -> Unit,
    private val onItemClicked: (MediaRecyclerView.MediaNote)->Unit
) : RecyclerView.Adapter<MediaAdapter.NotesViewHolder>() {

   inner class NotesViewHolder(v: View) : RecyclerView.ViewHolder(v), AnimateViewHolder {
        override fun animateAddImpl(
            holder: RecyclerView.ViewHolder,
            listener: Animator.AnimatorListener
        ) {
            itemView.animate().apply {
                alpha(1f)
                duration = 100
                setListener(listener)
            }.start()
        }

        override fun animateRemoveImpl(
            holder: RecyclerView.ViewHolder,
            listener: Animator.AnimatorListener
        ) {
            itemView.animate().apply {
                alpha(0f)
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                duration = 150
                interpolator = interpolator
            }.start()
        }

        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            itemView.setAlpha(0f)
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder) {

        }

    }

    private lateinit var mContext: Context

    private var mediaPlayer: MediaPlayer? = null

    private val mediaList = mutableListOf<MediaRecyclerView.MediaNote>()

    private var currentVoice = -1
    private var currentHolder: View? = null

    private var recognizedSpeechPositions = mutableListOf<Int>()

    private lateinit var parentRecyclerView: RecyclerView

    private var isSelecting = false

    private val selectedNotes = mutableListOf<MediaRecyclerView.MediaNote>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
        mContext = recyclerView.context
    }

    companion object {
        const val TYPE_SKETCH = 0
        const val TYPE_TEXT = 1
        const val TYPE_VOICE = 2
        const val TYPE_PHOTO = 3
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

        val holder = NotesViewHolder(layout)

        return holder
    }

    override fun getItemViewType(position: Int): Int {
        return when (mediaList[position].mediaType) {
            MediaRecyclerView.MediaItemTypes.TYPE_SKETCH -> TYPE_SKETCH
            MediaRecyclerView.MediaItemTypes.TYPE_VOICE -> TYPE_VOICE
            MediaRecyclerView.MediaItemTypes.TYPE_PHOTO -> TYPE_PHOTO
            MediaRecyclerView.MediaItemTypes.TYPE_TEXT -> TYPE_TEXT
        }
    }

    fun getSelectedItems(): List<MediaRecyclerView.MediaNote>{
        return selectedNotes
    }

    fun stopSelecting(){
        selectedNotes.clear()
        onSelecting(false)
    }

    fun setMediaPlayer(mediaPlayer: MediaPlayer) {
        this.mediaPlayer = mediaPlayer
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: NotesViewHolder, s: Int) {
        val position = holder.bindingAdapterPosition
        val contentView: View?
        val checkBox: ImageView?
        val selectionView: View?
        var viewToSetOnClickListener:View? = null

        when (getItemViewType(position)) {

            TYPE_SKETCH -> {
                viewToSetOnClickListener = holder.itemView.item_media_note_sketch_cv
                selectionView = holder.itemView.selection_view_sketch
                checkBox = holder.itemView.note_checkbox_sketch
                contentView = holder.itemView.item_media_note_sketch_cv
                Glide.with(mContext)
                    .load(mediaList[position].value)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                     .into(holder.itemView.media_note_sketch_iv)

            }
            TYPE_VOICE -> {
                viewToSetOnClickListener = null
                selectionView = holder.itemView.selection_view_voice
                checkBox = holder.itemView.note_checkbox_voice
                contentView = holder.itemView.visualizer_view


                holder.itemView.visualizer_view.apply {

                    mediaPlayer?.let { player ->
                        initVisualizer(
                                player = player,
                                amplitudes = mediaList[position].voiceAmplitudesList,
                                file = File(mediaList[position].value),
                                isCurrentVisualizer = currentVoice == position
                        )
                    }


                    setOnSeekBarPointerOnCallback { isPointerOn ->
                        setParentRecyclerDraggability(!isPointerOn)
                    }
                    setOnCompleteCallback {
                        currentVoice = -1
                    }

                    setRecognizedSpeech(mediaList[position].recognizedSpeechText)
                    setRecognizedSpeechTextVisibility(recognizedSpeechPositions.contains(position))


                    setOnRecognizeSpeechClickCallback {
                        if (!recognizedSpeechPositions.contains(position)) {
                            recognizedSpeechPositions.add(position)

                        } else {
                            recognizedSpeechPositions.remove(position)
                        }
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
                }

            }
            TYPE_PHOTO -> {
                viewToSetOnClickListener = holder.itemView.item_media_note_photo_cv
                selectionView = holder.itemView.selection_view_photo
                contentView = holder.itemView.item_media_note_photo_cv
                checkBox = holder.itemView.note_checkbox_photo
                Glide.with(mContext).load(mediaList[position].value)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(holder.itemView.media_note_photo_iv)

                if (!mediaList[position].imageNoteText.isNullOrEmpty()) {
                    holder.itemView.media_note_photo_tv.apply {
                        text = mediaList[position].imageNoteText
                        visibility = View.VISIBLE
                    }
                }
            }
            TYPE_TEXT -> {
                viewToSetOnClickListener = null
                selectionView=  holder.itemView.selection_view_text
                checkBox = holder.itemView.note_checkbox_text
                contentView = holder.itemView.note_text_cv
                holder.itemView.item_media_note_text_tv.setText(mediaList[position].value)
            }
            else -> {
                viewToSetOnClickListener = null
                checkBox = null
                contentView = null
                selectionView = null
            }
        }

        viewToSetOnClickListener?.setOnClickListener{
            onItemClicked.invoke(mediaList[position])
        }

        selectionView?.isVisible = isSelecting

        if (isSelecting) {

            releasePlayer()
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
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            contentView?.apply {
                animate().x(dpToPx(12)).duration = 0
            }
            checkBox?.apply {
                animate().x(dpToPx(-24)).duration = 0
            }
        }
        if (contentView == holder.itemView.visualizer_view) {
            holder.itemView.visualizer_view.setOnLongClickCallback {
                if (!isSelecting) {
                    onSelecting(true)
                    holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))
                    selectedNotes.add(mediaList[position])
                    onItemsSelected.invoke(selectedNotes)
                    checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)
                }
            }
        } else {

            contentView?.setOnLongClickListener {
                if (!isSelecting) {
                    onSelecting(true)
                    holder.itemView.setBackgroundColor(Color.parseColor("#E3F5FF"))
                    selectedNotes.add(mediaList[position])
                    onItemsSelected.invoke(selectedNotes)
                    checkBox?.setImageResource(R.drawable.checkbox_to_unchecked)
                }
                true
            }

        }

       selectionView?.setOnClickListener {
           if (isSelecting) {
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

       }

    }


    override fun getItemCount(): Int {
        return mediaList.size
    }

    fun setData(newData: List<MediaRecyclerView.MediaNote>) {
        val oldList = mutableListOf<ru.scheduled.mediaattachmentslibrary.MediaRecyclerView.MediaNote>().also{
            it.addAll(mediaList)
        }

        when {
            oldList.size == 0 || newData.size == 0-> {
                mediaList.clear()
                mediaList.addAll(newData)
                notifyDataSetChanged()
            }
            newData.size < oldList.size -> {
                try {
                    oldList.onEach {
                        val ind = mediaList.indexOf(it)
                        if (!newData.contains(it)) {
                            mediaList.removeAt(ind)
                            notifyItemRemoved(ind)
                            notifyItemRangeChanged(ind, itemCount)
                        }
                    }
                    notifyItemRangeChanged(0, itemCount)
                }
                catch (e:Exception) {
                    notifyDataSetChanged()
                }
            }
            newData.size > oldList.size -> {
                mediaList.clear()
                mediaList.addAll(newData)
                newData.onEach {
                    if(!oldList.contains(it))
                        notifyItemInserted(newData.indexOf(it))
                }
                parentRecyclerView.scrollToPosition(mediaList.lastIndex)
            }
            newData.size == oldList.size -> {

                newData.onEach {
                    if(!oldList.contains(it))
                        notifyItemChanged(newData.indexOf(it))
                }
            }
        }
        releasePlayer()

    }

    private fun setParentRecyclerDraggability(isDraggable: Boolean) {
        (parentRecyclerView.layoutManager as CustomLinearLayoutManager).setScrollEnabled(isDraggable)
    }

    private fun dpToPx(dp: Int): Float {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        return dp * displayMetrics.density
    }

    fun releasePlayer() {
        currentHolder?.let { holder ->
            holder.visualizer_view.cancel()
        }
        currentHolder = null
        currentVoice = -1
    }

    private fun onSelecting(isSelecting: Boolean) {
        this.isSelecting = isSelecting

        val firstVisibleItemPosition =
                (parentRecyclerView.layoutManager as CustomLinearLayoutManager).findFirstVisibleItemPosition()
        val visibleItemsSize = parentRecyclerView.childCount

        val listOfViews = mutableListOf<Triple<ImageView?,View?, View?>>()

        for (i in firstVisibleItemPosition until firstVisibleItemPosition + visibleItemsSize) {
            val checkBoxM: ImageView?
            val contentViewM: View?
            val selectionViewM: View?
            parentRecyclerView.findViewHolderForAdapterPosition(i)?.itemView?.let {
                when (mediaList[i].mediaType) {
                    MediaRecyclerView.MediaItemTypes.TYPE_SKETCH -> {
                        checkBoxM = it.note_checkbox_sketch
                        contentViewM = it.item_media_note_sketch_cv
                        selectionViewM = it.selection_view_sketch
                    }
                    MediaRecyclerView.MediaItemTypes.TYPE_VOICE -> {
                        checkBoxM = it.note_checkbox_voice
                        contentViewM = it.visualizer_view
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


                }
                if(!isSelecting){
                    it.setBackgroundColor(Color.TRANSPARENT)
                }
                listOfViews.add(Triple(checkBoxM,contentViewM,selectionViewM))
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
            if (!(firstVisibleItemPosition until firstVisibleItemPosition+visibleItemsSize).contains(i)) {
                notifyItemChanged(i)
            }
        }

    }

}
