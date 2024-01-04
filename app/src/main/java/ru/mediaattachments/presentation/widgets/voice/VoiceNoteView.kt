package ru.mediaattachments.presentation.widgets.voice

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.*
import ru.mediaattachments.R
import ru.mediaattachments.databinding.LayoutVisualizerViewBinding
import ru.mediaattachments.utils.toPx
import java.io.File
import java.io.IOException
import kotlin.math.abs

private enum class VoiceRecordingState {
    PLAYING, PAUSED, STOPPED
}

@SuppressLint("ClickableViewAccessibility")
class VoiceNoteView : ConstraintLayout {

    private var binding: LayoutVisualizerViewBinding =
        LayoutVisualizerViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var currentTotalTime = -1L
    private var currentTouchX = 0f
    private var onSeekBarPointerOn: ((isPointerOn: Boolean) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null
    private var onPlayClick: (() -> Unit)? = null
    private var mediaPlayer: MediaPlayer? = null
    private var job: Job? = null
    private var audioFile: File? = null

    private var isVisualising: Boolean = false
    private var onLongClick: (() -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        with(binding) {

            mediaPlayIv.tag = VoiceRecordingState.STOPPED.name
            mediaOnPlayVisualizer.visualize(mutableListOf())

            visualizerCv.setOnLongClickListener {
                onLongClick?.invoke()
                true
            }
            mediaPlayIv.setOnLongClickListener {
                onLongClick?.invoke()
                true
            }

            mediaPlayIv.setOnClickListener {
                if (mediaPlayer == null || audioFile == null) return@setOnClickListener
                onPlayClick?.invoke()
                val newVoiceRecordingState = when (mediaPlayIv.tag) {
                    VoiceRecordingState.PLAYING -> {
                        VoiceRecordingState.PAUSED
                    }

                    VoiceRecordingState.PAUSED -> {
                        VoiceRecordingState.PLAYING
                    }

                    else -> {
                        VoiceRecordingState.PLAYING
                    }
                }
                setOnPlayingVoiceRecordingState(newVoiceRecordingState)
            }

            mediaOnPlayVisualizerFrame.setOnTouchListener { view, event ->
                if (mediaPlayer == null || audioFile == null) return@setOnTouchListener false
                if (mediaPlayIv.tag != VoiceRecordingState.STOPPED.name) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            mediaPlayIv.setImageResource(R.drawable.media_note_voice_pause)
                            onSeekBarPointerOn?.invoke(true)
                            mediaPlayer?.pause()
                            mediaOnPlayVisualizer.pause()
                            val location = IntArray(2)
                            view.getLocationOnScreen(location)
                            val viewX = location[0]
                            currentTouchX = event.rawX - viewX
                            if (currentTouchX < 0) currentTouchX = 0f
                            var percentage: Float = currentTouchX / (view.width)
                            if (percentage >= 1) percentage = 1f
                            showProgressPercentage(percentage)
                        }

                        MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (currentTouchX < 0) currentTouchX = 0f
                            var playFromPositionPercent: Float = currentTouchX / (view.width)
                            if (playFromPositionPercent >= 1) playFromPositionPercent = 1f
                            onSeekBarPointerOn?.invoke(false)
                            mediaPlayer?.seekTo((currentTotalTime * playFromPositionPercent).toInt())
                            mediaPlayer?.start()
                            observePlayer()
                        }

                        MotionEvent.ACTION_MOVE -> {

                            val location = IntArray(2)
                            view.getLocationOnScreen(location)
                            val viewX = location[0]
                            currentTouchX = event.rawX - viewX
                            if (currentTouchX < 0) currentTouchX = 0f
                            var percentage: Float = currentTouchX / (view.width)
                            if (percentage >= 1) percentage = 0.99f
                            showProgressPercentage(percentage)
                        }
                    }

                }
                true
            }

        }
    }

    fun setOnSeekBarPointerOnCallback(onSeekBarPointerOnCallback: ((isPointerOn: Boolean) -> Unit)) {
        onSeekBarPointerOn = onSeekBarPointerOnCallback
    }

    fun setOnLongClickCallback(onLongClickCallback: () -> Unit) {
        onLongClick = onLongClickCallback
    }

    fun setOnCompleteCallback(onCompleteCallback: () -> Unit) {
        onComplete = onCompleteCallback
    }

    fun setOnPlayClickCallback(onPlayClickCallback: () -> Unit) {
        onPlayClick = onPlayClickCallback
    }

    fun cancel() {
        setOnPlayingVoiceRecordingState(VoiceRecordingState.STOPPED)
    }

    fun initVisualizer(
        player: MediaPlayer,
        amplitudes: List<Int>?,
        file: File,
        isCurrentVisualizer: Boolean,
        duration: Int,
        isActive: Boolean
    ) {
        with(binding) {

            val amplitudesList = mutableListOf<Int>().also { list ->
                amplitudes?.let {
                    list.addAll(it)
                }
            }
            if (mediaPlayer == null)
                mediaPlayer = player
            isVisualising = isCurrentVisualizer
            if (!isVisualising) mediaPlayIv.setImageResource(R.drawable.media_note_voice_play)
            audioFile = file
            val currentPos = mediaPlayer?.currentPosition ?: -1
            val totalTime = mediaPlayer?.duration ?: -1
            val percentage: Float =
                (currentPos.toFloat() / totalTime.toFloat())
            if ((0f..0.99f).contains(percentage) && isVisualising) {
                if (mediaPlayer?.isPlaying == true) {
                    setOnPlayingVoiceRecordingState(playingVoiceRecordingState = VoiceRecordingState.PLAYING)
                }
                val duration = abs((mediaPlayer?.duration ?: 1L).toLong())
                currentTotalTime = duration
                mediaDuration.text =
                    getFormatTimerString((duration - duration * percentage).toLong())
                mediaOnPlayVisualizer.visualize(
                    amplitudesList = amplitudesList,
                    mCurrentPercentage = percentage
                )
            } else {
                mediaOnPlayVisualizer.visualize(
                    amplitudesList = amplitudesList,
                    isActive = isActive
                )
                CoroutineScope(Dispatchers.IO).launch {
                    val media = MediaPlayer().apply {

                        try {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(
                                context, Uri.fromFile(audioFile)
                            )
                            prepare()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    if (duration != 0) {
                        media.release()
                        currentTotalTime = (duration.toLong() * 1000)
                        withContext(Dispatchers.Main) {
                            mediaDuration.text = getFormatTimerString(currentTotalTime)
                        }
                    } else {
                        media.setOnPreparedListener { mp ->
                            val duration = abs(mp.duration.toLong())
                            CoroutineScope(Dispatchers.IO).launch {
                                withContext(Dispatchers.Main) {
                                    currentTotalTime = duration
                                    mediaDuration.text = getFormatTimerString(duration)

                                }
                            }

                            media.release()
                        }
                    }
                }
            }

        }
    }

    private fun setOnPlayingVoiceRecordingState(playingVoiceRecordingState: VoiceRecordingState) {
        with(binding) {

            mediaPlayIv.tag = playingVoiceRecordingState
            when (playingVoiceRecordingState) {
                VoiceRecordingState.PLAYING -> {
                    mediaPlayIv.setImageResource(R.drawable.media_note_voice_pause)
                    if (mediaPlayer?.isPlaying != true) {

                        if (isVisualising) {
                            mediaPlayer?.start()
                        } else {
                            isVisualising = true
                            if (job == null) {
                                mediaPlayer?.apply {

                                    try {
                                        reset()
                                        setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .build()
                                        )
                                        setDataSource(
                                            context,
                                            Uri.fromFile(audioFile)
                                        )
                                        prepare()
                                        start()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }

                                }

                                mediaPlayer?.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                                    setOnPlayingVoiceRecordingState(VoiceRecordingState.STOPPED)
                                    onComplete?.invoke()

                                })

                            } else {
                                mediaPlayer?.start()
                            }

                        }

                    }
                    observePlayer()
                }

                VoiceRecordingState.STOPPED -> {
                    mediaPlayIv.setImageResource(R.drawable.media_note_voice_play)
                    mediaOnPlayVisualizer.stop()
                    mediaDuration.text = getFormatTimerString(currentTotalTime)
                    releasePlayer()
                    isVisualising = false
                }

                VoiceRecordingState.PAUSED -> {
                    mediaPlayer?.pause()
                    mediaPlayIv.setImageResource(R.drawable.media_note_voice_play)
                }
            }

        }

    }

    private fun releasePlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        job?.cancel()
        job = null
    }

    private fun showProgressPercentage(percentage: Float) {
        with(binding) {
            mediaDuration.text =
                getFormatTimerString((currentTotalTime - currentTotalTime * percentage).toLong())
            mediaOnPlayVisualizer.play(percentage)
        }
    }

    private fun getFormatTimerString(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = millis / 1000 / 60
        return String.format("%0$02d:%0$02d", minutes, seconds)
    }

    private fun observePlayer() {
        job = CoroutineScope(Dispatchers.IO).launch {
            currentTotalTime = mediaPlayer?.duration?.toLong() ?: 1L
            while (mediaPlayer?.isPlaying == true) {
                withContext(Dispatchers.Main) {
                    val currentTime = mediaPlayer?.currentPosition ?: 0

                    val percentage: Float =
                        currentTime / currentTotalTime.toFloat()
                    if (isVisualising) {
                        if (currentTotalTime != 0L && currentTime.toLong() >= currentTotalTime) {
                            setOnPlayingVoiceRecordingState(VoiceRecordingState.STOPPED)
                        } else {
                            showProgressPercentage(percentage)
                        }
                    }
                }
                delay(1)
            }

        }
    }

}

class Visualizer : View {
    private var mPath: Path? = null
    private var mX: Float = 0f
    private var mY: Float = 0f
    private var mPaint: Paint? = null
    private var insertIdx = 0
    private var vectorsIndiciesCount = 0
    private var playingPaint: Paint? = null
    private var spacing: Float = 0f
    private var currentVoiceRecordingState: VoiceRecordingState = VoiceRecordingState.STOPPED
    private var vectors: FloatArray? = null
    private var maxCountOfLines: Int = 0

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    fun initView() {

        mX = 3.toPx()
        mY = 0f
        mPath = Path()
        spacing = 3.toPx()
        maxCountOfLines = 0
        vectorsIndiciesCount = 0
        insertIdx = 0
        currentVoiceRecordingState = VoiceRecordingState.STOPPED
        vectors = FloatArray(0)


        playingPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.defaultActiveVoice)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.toPx()
        }
        mPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.defaultNotActiveVoice)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.toPx()
        }


    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(resources.getColor(R.color.lib_white), PorterDuff.Mode.MULTIPLY)

        when (currentVoiceRecordingState) {
            VoiceRecordingState.PLAYING -> {
                canvas.drawPath(mPath!!, mPaint!!)
                if (vectors?.isNotEmpty() == true && (vectorsIndiciesCount < vectors?.lastIndex ?: 0)) {
                    vectors?.let { vectorsList ->

                        val newArr = vectorsList.slice(0..vectorsIndiciesCount).toFloatArray()
                        canvas.drawLines(newArr, playingPaint!!)
                        if (vectorsIndiciesCount == vectorsList.lastIndex) {
                            currentVoiceRecordingState = VoiceRecordingState.STOPPED
                            invalidate()
                        }
                    }
                }
            }

            else -> {
                canvas.drawPath(mPath!!, playingPaint!!)
            }
        }

    }

    fun play(percentage: Float) {
        currentVoiceRecordingState = VoiceRecordingState.PLAYING
        vectors?.let { vectorsList ->
            vectorsIndiciesCount = (vectorsList.lastIndex * percentage).toInt()
        }
        invalidate()
    }

    fun pause() {
        currentVoiceRecordingState = VoiceRecordingState.PAUSED
    }

    fun stop() {
        currentVoiceRecordingState = VoiceRecordingState.STOPPED
        invalidate()
    }

    fun visualize(
        amplitudesList: List<Int>,
        mCurrentPercentage: Float = 0f,
        isActive: Boolean = true
    ) {
        try {
            viewTreeObserver.addOnGlobalLayoutListener(
                object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        initView()
                        if (isActive) {
                            playingPaint?.apply {
                                color = resources.getColor(R.color.defaultActiveVoice)
                            }
                        } else {
                            playingPaint?.apply {
                                color = resources.getColor(R.color.defaultNotActiveVoice)
                            }
                        }
                        val mWidth = width
                        val mHeight = height


                        val amplitudes = mutableListOf<Int>()
                        amplitudes.addAll(amplitudesList)

                        maxCountOfLines = (mWidth / (spacing)).toInt()
                        vectors = FloatArray(maxCountOfLines * 4)
                        if (!amplitudesList.isNullOrEmpty()) {
                            if (amplitudes.size > maxCountOfLines) {
                                val extra = amplitudes.size % maxCountOfLines
                                if (extra > maxCountOfLines / 2) {
                                    for (i in 1..(maxCountOfLines - extra)) {
                                        amplitudes.add(0, 1600)
                                    }
                                } else {
                                    for (i in 1..extra) {
                                        amplitudes.remove((amplitudes.minOrNull()))
                                    }
                                }
                                val factor = amplitudes.size / maxCountOfLines
                                if (factor != 1) {
                                    val tempList = mutableListOf<Int>()
                                    for (ind in 0..amplitudes.lastIndex) {

                                        if (ind % factor == 0) {
                                            tempList.add(amplitudes[ind])
                                        }

                                    }
                                    amplitudes.clear()
                                    amplitudes.addAll(tempList)

                                }

                            } else {
                                val numberOfLinesToAdd = maxCountOfLines - amplitudes.size
                                for (num in 0 until numberOfLinesToAdd) {
                                    if (num % 2 == 0) {
                                        amplitudes.add(1600)
                                    } else {
                                        amplitudes.add(0, 1600)
                                    }

                                }
                            }
                        } else {
                            for (num in 0..maxCountOfLines) {
                                amplitudes.add(4000)
                            }
                        }

                        for (i in 0 until amplitudes.size) {
                            if (mX + spacing < mWidth) {
                                val amplitude = amplitudes[i]
                                mY = mHeight * amplitude / 17000.toFloat()
                                mPath!!.moveTo(mX, mHeight.toFloat() - 1.toPx())
                                mPath!!.lineTo(mX, mHeight.toFloat() - mY)
                                var newVectorIndex: Int = insertIdx * 4
                                vectors?.let { vectorsList ->
                                    vectorsList[newVectorIndex++] = mX // x0
                                    vectorsList[newVectorIndex++] = mHeight.toFloat() // y0
                                    vectorsList[newVectorIndex++] = mX // x1
                                    vectorsList[newVectorIndex] = mHeight.toFloat() - mY //y1
                                    if (insertIdx + 1 < maxCountOfLines) insertIdx++
                                }

                                if (i != amplitudes.size - 1) {
                                    mX += spacing
                                }
                            }

                        }
                        if (mCurrentPercentage != 0f) {
                            currentVoiceRecordingState = VoiceRecordingState.PLAYING
                            vectors?.let { vectorsList ->
                                vectorsIndiciesCount =
                                    (vectorsList.lastIndex * mCurrentPercentage).toInt()
                            }
                        }
                        invalidate()
                        viewTreeObserver.removeOnGlobalLayoutListener(this)

                    }
                })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

