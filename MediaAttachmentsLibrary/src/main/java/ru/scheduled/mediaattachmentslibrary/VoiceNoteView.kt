package ru.scheduled.mediaattachmentslibrary

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.layout_media_sketch.view.*
import kotlinx.android.synthetic.main.layout_visualizer_view.view.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class VoiceNoteView : ConstraintLayout {

    private var currentTotalTime = -1L
    private var currentTouchX = 0f
    private var onSeekBarPointerOn:((isPointerOn:Boolean ) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null
    private var onPlayClick: (() -> Unit)? = null
    private var onRecognizeSpeechClick: (() -> Unit)? = null
    private var mediaPlayer: MediaPlayer? = null
    private var job: Job? = null
    private var audioFile: File? = null

    private var isVisualising: Boolean = false
    private var onLongClick: (() -> Unit)? = null

    var areClicksEnabled = true

    companion object {
        const val PLAYING = "playing"
        const val PAUSED = "paused"
        const val STOPPED = "stopped"
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    fun setOnSeekBarPointerOnCallback(onSeekBarPointerOnCallback: ((isPointerOn:Boolean ) -> Unit)){
        onSeekBarPointerOn = onSeekBarPointerOnCallback
    }

    fun areClicksEnabled(enabled:Boolean){

    }

    fun setOnLongClickCallback(onLongClickCallback: () -> Unit){
        onLongClick = onLongClickCallback
    }

    fun setOnCompleteCallback(onCompleteCallback: () -> Unit) {
        onComplete = onCompleteCallback
    }

    fun setOnPlayClickCallback(onPlayClickCallback: () -> Unit) {
        onPlayClick = onPlayClickCallback
    }

    fun setOnRecognizeSpeechClickCallback(callback: () -> Unit) {
        onRecognizeSpeechClick = callback
    }

    fun cancel() {
        setOnPlayingState(STOPPED)
    }

    fun setRecognizedSpeech(text: String) {
        if (!text.isNullOrEmpty()) {
            media_speech_recognize.visibility = View.VISIBLE
            recognized_speech_tv.text = text
        } else {
            media_speech_recognize.visibility = View.GONE
            recognized_speech_tv.text = ""
        }
    }

    fun setRecognizedSpeechTextVisibility(isVisible: Boolean) {

        if (isVisible) {
            media_speech_recognize.tag = "opened"
            media_speech_recognize.setImageResource(R.drawable.speech_recognizer_close)
            recognized_speech_tv.visibility = View.VISIBLE

        } else {
            media_speech_recognize.tag = "closed"
            media_speech_recognize.setImageResource(R.drawable.speech_recognizer_open)
            recognized_speech_tv.visibility = View.GONE

        }

    }

    fun initVisualizer(
            player: MediaPlayer,
            amplitudes: List<Int>?,
            file: File,
            isCurrentVisualizer: Boolean
    ) {
        val amplitudesList = mutableListOf<Int>().also { list->
            amplitudes?.let{
                list.addAll(it)
            }
        }
        if (mediaPlayer == null)
            mediaPlayer = player
        isVisualising = isCurrentVisualizer
        if (!isVisualising) media_play_iv.setImageResource(R.drawable.media_note_voice_play)
        audioFile = file
        val currentPos = mediaPlayer?.currentPosition ?: -1
        val totalTime = mediaPlayer?.duration ?: -1
        val percentage: Float =
                (currentPos.toFloat() / totalTime.toFloat())
        if ((0f..0.99f).contains(percentage) && isVisualising) {
            if (mediaPlayer?.isPlaying ?: false) {
                setOnPlayingState(playingState = PLAYING)
            }
            val duration = abs((mediaPlayer?.duration ?: 1L).toLong())
            currentTotalTime = duration
            media_duration.text = getFormatTimerString((duration - duration * percentage).toLong())
            media_on_play_visualizer.visualize(amplitudesList = amplitudesList, mCurrentPercentage = percentage)
        } else {
            media_on_play_visualizer.visualize(amplitudesList = amplitudesList)
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
                media.setOnPreparedListener { mp ->
                    val duration = abs(mp.duration.toLong())
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            currentTotalTime = duration
                            media_duration.text = getFormatTimerString(duration)

                        }
                    }

                    media.release()
                }

            }
        }
    }

    private fun setOnPlayingState(playingState: String) {
        media_play_iv.tag = playingState
        when (playingState) {
            PLAYING -> {
                media_play_iv.setImageResource(R.drawable.media_note_voice_pause)
                if (!(mediaPlayer?.isPlaying ?: false)) {

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
                                setOnPlayingState(STOPPED)
                                onComplete?.invoke()

                            })

                        } else {
                            mediaPlayer?.start()
                        }

                    }

                }
                observePlayer()

            }
            STOPPED -> {
                media_play_iv.setImageResource(R.drawable.media_note_voice_play)
                media_on_play_visualizer.stop()
                media_duration.text = getFormatTimerString(currentTotalTime)
                releasePlayer()
                isVisualising = false
            }
            PAUSED -> {
                mediaPlayer?.pause()
                media_play_iv.setImageResource(R.drawable.media_note_voice_play)
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
        media_duration.text =
                getFormatTimerString((currentTotalTime - currentTotalTime * percentage).toLong())
        media_on_play_visualizer.play(percentage)
    }

    private fun getFormatTimerString(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = millis / 1000 / 60
        return String.format("%0$02d:%0$02d", minutes, seconds)
    }

    private fun observePlayer() {
        job = CoroutineScope(Dispatchers.IO).launch {
            currentTotalTime = mediaPlayer?.duration?.toLong() ?: 1L
            while (mediaPlayer?.isPlaying ?: false) {
                withContext(Dispatchers.Main) {
                    val currentTime = mediaPlayer?.currentPosition ?: 0

                    val percentage: Float =
                            currentTime / currentTotalTime.toFloat()
                    if (isVisualising) {
                        if (currentTotalTime != 0L && currentTime.toLong() >= currentTotalTime) {
                            setOnPlayingState(STOPPED)
                        } else {
                            Log.v("ZhoppaVis", "currentTime = $currentTime currentTotalTime = $currentTotalTime")
                            showProgressPercentage(percentage)
                        }
                    }
                }
                delay(1)
            }

        }
    }

    init {
        View.inflate(context, R.layout.layout_visualizer_view, this)
        media_play_iv.tag = STOPPED
        media_on_play_visualizer.visualize(mutableListOf())

        visualizer_cv.setOnLongClickListener {
            onLongClick?.invoke()
            true
        }
        media_play_iv.setOnLongClickListener {
            onLongClick?.invoke()
            true
        }
        media_speech_recognize.setOnLongClickListener {
            onLongClick?.invoke()
            true
        }

        media_speech_recognize.setOnClickListener {
            if(!areClicksEnabled) return@setOnClickListener
            setRecognizedSpeechTextVisibility(media_speech_recognize.tag != "opened")
            onRecognizeSpeechClick?.invoke()
        }

        media_play_iv.setOnClickListener {
            if (mediaPlayer == null || audioFile == null || !areClicksEnabled) return@setOnClickListener
            onPlayClick?.invoke()
            val newState = when (media_play_iv.tag) {
                PLAYING -> {
                    PAUSED
                }
                PAUSED -> {
                    PLAYING
                }
                else -> {
                    PLAYING
                }
            }
            setOnPlayingState(newState)
        }

        media_on_play_visualizer_frame.setOnTouchListener { view, event ->
            if (mediaPlayer == null || audioFile == null || !areClicksEnabled) return@setOnTouchListener false
            if (media_play_iv.tag != STOPPED) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        media_play_iv.setImageResource(R.drawable.media_note_voice_pause)
                        onSeekBarPointerOn?.invoke(true)
                        mediaPlayer?.pause()
                        media_on_play_visualizer.pause()
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


private class Visualizer : View {
    private var mPath: Path? = null
    private var mX: Float = 0f
    private var mY: Float = 0f
    private var mPaint: Paint? = null
    private var insertIdx = 0
    private var vectorsIndiciesCount = 0
    private var playingPaint: Paint? = null
    private var spacing: Float = 0f
    private var currentState: VisualiserStates = VisualiserStates.STOPPED
    private var vectors: FloatArray? = null
    private var maxCountOfLines: Int = 0

    enum class VisualiserStates {
        PLAYING, PAUSED, STOPPED
    }

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

        mX = dpToPx(3)
        mY = 0f
        mPath = Path()
        spacing = dpToPx(3)
        maxCountOfLines = 0
        vectorsIndiciesCount = 0
        insertIdx = 0
        currentState = VisualiserStates.STOPPED
        vectors = FloatArray(0)


        playingPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.defaultActive)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dpToPx(2)
        }
        mPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.defaultNotActive)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dpToPx(2)
        }


    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(resources.getColor(R.color.white), PorterDuff.Mode.MULTIPLY)

        when(currentState){
            VisualiserStates.PLAYING -> {
                canvas.drawPath(mPath!!, mPaint!!)
                if (vectors?.isNotEmpty() ?: false && (vectorsIndiciesCount < vectors?.lastIndex ?: 0) ?: false) {
                    vectors?.let { vectorsList ->

                        val newArr = vectorsList.slice(0..vectorsIndiciesCount).toFloatArray()
                        canvas.drawLines(newArr, playingPaint!!)
                        if (vectorsIndiciesCount == vectorsList.lastIndex) {
                            currentState = VisualiserStates.STOPPED
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

    private fun dpToPx(dp: Int): Float {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        return dp * displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)


    }

    fun play(percentage: Float) {
        currentState = VisualiserStates.PLAYING
        vectors?.let{ vectorsList->
            vectorsIndiciesCount = (vectorsList.lastIndex * percentage).toInt()
        }
        invalidate()
    }

    fun pause(){
        currentState = VisualiserStates.PAUSED
    }

    fun stop(){
        currentState = VisualiserStates.STOPPED
        invalidate()
    }

    fun visualize(amplitudesList: List<Int>, mCurrentPercentage: Float = 0f) {
        try {
            viewTreeObserver.addOnGlobalLayoutListener(
                    object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            initView()
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
                                    mPath!!.moveTo(mX, mHeight.toFloat())
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
                                currentState = VisualiserStates.PLAYING
                                vectors?.let { vectorsList ->
                                    vectorsIndiciesCount = (vectorsList.lastIndex * mCurrentPercentage).toInt()
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

