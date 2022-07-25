package ru.scheduled.mediaattachmentslibrary

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Rect
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doOnTextChanged
import com.github.florent37.kotlin.pleaseanimate.please
import kotlinx.android.synthetic.main.layout_media_toolbar_default.view.*
import kotlinx.android.synthetic.main.layout_media_toolbar_note_edit.view.*
import kotlinx.android.synthetic.main.layout_media_toolbar_view.view.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ClickableViewAccessibility")
class MediaToolbarView : ConstraintLayout {

    private val voiceRecorder: VoiceRecorder = VoiceRecorder(context)

    private var initialIconX: Float = 0f
    private var initialIconBackgroundX: Float = 0f
    private var initialSwipeToCancelX: Float = 0f
    private var swipeLeftConstraintX: Float = 0f
    private var rect: Rect? = null
    private var voiceCircleInitScale = 1f
    private var isRecording = false
    private var isAnimating = false
    private var isDraggingBlocked = false
    private var isVoiceBackgroundBeingAnimated = false
    private var voiceRecordingStartMillis = -1L
    private val newAmplitudes = mutableListOf<Int>()
    private val amplitudesList = mutableListOf<Pair<MutableList<Int>, String>>()

    private var isPointerOn = false

    private var job: Job? = null

    private var touchedButNotRecording = false

    private var stopped = true

    private var toolTip:ToolTip? = null

    companion object {
        const val MAX_AMPLITUDE = 15000
        const val MIN_AMPLITUDE = 1500
        const val MIN_DURATION = 1500L

    }

    private var onSend: ((String) -> Boolean)? = null
    private var onCancelEditting: (() -> Unit)? = null
    private var onStartRecording: (() -> Unit)? = null

    private var onRecognizedSpeech: ((String) -> Unit)? = null

    private var onMediaEditingCancel: (() -> Unit)? = null
    private var onMediaCopy: (() -> Unit)? = null
    private var onMediaEdit: (() -> Unit)? = null
    private var onMediaDelete: (() -> Unit)? = null

    private var onNewToolbarHeight: ((Int) -> Unit)? = null

    private var onOpenCamera: (() -> Unit)? = null
    private var onOpenSketch: (() -> Unit)? = null

    private var onCompleteRecording: ((amplitudesList: List<Int>, filePath: String, duration:Int) -> Unit)? =
        null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    fun getCurrentText():String {
        return bottom_notes_add_text_note_et.text.toString()
    }

    fun setNewNoteText(text: String){
        setUpTextNoteCreationToolbarVisibility(isVisible = true)
        bottom_notes_add_text_note_et.setText(text)
    }

    fun setOnMediaEditingCancelClickedCallback(callback: () -> Unit) {
        cancel_tv.setOnClickListener {
            callback.invoke()
            hideMediaEditingToolbar()
        }
    }

    fun setOnMediaCopyClickedCallback(callback: () -> Unit) {
        copy_iv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnMediaEditClickedCallback(callback: () -> Unit) {
        edit_iv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnSpeechRecognizedCallback(callback: (recognizedSpeech:String) -> Unit) {
        onRecognizedSpeech = callback
    }

    fun setOnMediaDeleteClickedCallback(callback: () -> Unit) {
        delete_iv.setOnClickListener {
            callback.invoke()
        }
    }

    fun showMediaEditingToolbar(isCopyable: Boolean, isEditable: Boolean) {
        setEdittingViewsVisibility(areVisible = false)
        if(media_toolbar_note_edit.alpha == 1f) {

            if (isCopyable) {

                please(duration = 150L) {
                    animate(copy_iv) toBe {
                        originalPosition()
                        visible()
                    }
                }.start()


            } else {
                please(duration = 150L) {
                    animate(copy_iv) toBe {
                        leftOfItsParent()
                        invisible()
                    }
                }.start()
            }

            if (isEditable) {
                if (isCopyable) {
                    please(duration = 150L) {
                        animate(edit_iv) toBe {
                            originalPosition()
                            visible()
                        }
                    }.start()
                } else {
                    please(duration = 150L) {
                        animate(edit_iv) toBe {
                            rightOf(delete_iv)
                            visible()
                        }
                    }.start()
                }
            } else {
                please(duration = 150L) {
                    animate(edit_iv) toBe {
                        leftOfItsParent()
                        invisible()
                    }
                }.start()
            }
        }
        else {
            if (isCopyable) {

                please(duration = 0L) {
                    animate(copy_iv) toBe {
                        originalPosition()
                        visible()
                    }
                }.start()


            } else {
                please(duration = 0L) {
                    animate(copy_iv) toBe {
                        leftOfItsParent()
                        invisible()
                    }
                }.start()
            }

            if (isEditable) {
                if (isCopyable) {
                    please(duration = 0L) {
                        animate(edit_iv) toBe {
                            originalPosition()
                            visible()
                        }
                    }.start()
                } else {
                    please(duration = 0L) {
                        animate(edit_iv) toBe {
                            rightOf(delete_iv)
                            visible()
                        }
                    }.start()
                }
            } else {
                please(duration = 0L) {
                    animate(edit_iv) toBe {
                        leftOfItsParent()
                        invisible()
                    }
                }.start()
            }
            please(duration = 100L) {
                animate(media_toolbar_default) toBe {
                    invisible()
                }
            }.thenCouldYou(100L) {
                animate(media_toolbar_note_edit) toBe {
                    bottomOfItsParent()
                    visible()
                }
            }.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
        }, 30)

    }

    fun hideMediaEditingToolbar() {
        setEdittingViewsVisibility(areVisible = !bottom_notes_add_text_note_et.text.isNullOrEmpty())
        if(!bottom_notes_add_text_note_et.text.isNullOrEmpty()){
            notes_toolbar_text_iv.isVisible = false
            notes_toolbar_camera_iv.isVisible = false
            notes_toolbar_sketch_iv.isVisible = false
            notes_voice_iv.isVisible = false
            notes_toolbar_voice_background_iv.isVisible = false
            bottom_notes_add_text_note_send_iv.isVisible = true
            bottom_notes_add_text_note_et.isVisible = true
                bottom_notes_add_text_note_et.apply {
                    requestFocus()
                }
                val keyboard =
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
        else {
            setUpTextNoteCreationToolbarVisibility(isVisible = false)
        }
        please(duration = 100L) {
            animate(media_toolbar_note_edit) toBe {
                originalPosition()
                invisible()
            }
        }.thenCouldYou(100L) {
            animate(media_toolbar_default) toBe {
                visible()
            }
        }.start()
        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
        }, 30)

    }

    fun setOnOpenCameraCallback(callback: () -> Unit) {
        notes_toolbar_camera_iv.visibility = View.VISIBLE
        onOpenCamera = callback
    }

    fun setOnOpenSketchCallback(callback: () -> Unit) {
        notes_toolbar_sketch_iv.visibility = View.VISIBLE
        onOpenSketch = callback
    }

    fun setOnStartRecordingCallback(callback: () -> Unit) {
        onStartRecording = callback
    }

    fun setOnCompleteRecordingCallback(callback: (amplitudesList: List<Int>, filePath: String, duration:Int) -> Unit) {
        onCompleteRecording = callback
    }

    fun setOnSendTextCallback(callback: (String) -> Boolean) {
        onSend = callback
    }

    fun setOnCancelEditingTextCallback(callback: () -> Unit) {
        onCancelEditting = callback
    }

    private fun setEdittingViewsVisibility(areVisible: Boolean) {
        noteEdittingPencilIv.isVisible = areVisible
        note_editing_title_tv.isVisible = areVisible
        note_editing_note_content_tv.isVisible = areVisible
        note_editing_close_editing_mode_iv.isVisible = areVisible
        editting_divider.isVisible = areVisible

    }

    private fun setVoiceRecordingViewsVisibility(areVisible: Boolean) {
        voice_recording_voice_record_on_iv.isVisible = areVisible
        voice_recording_duration_tv.isVisible = areVisible
        voice_recording_swipe_to_cancel_cl.isVisible = areVisible
    }

    private fun setNotesToolbarViewsVisibility(areVisible: Boolean) {
        notes_toolbar_camera_iv.isVisible = areVisible
        notes_toolbar_text_iv.isVisible = areVisible
        notes_toolbar_sketch_iv.isVisible = areVisible
    }


    fun setText(text: String) {
        setUpTextNoteCreationToolbarVisibility(isVisible = true)
        setEdittingViewsVisibility(areVisible = true)
        note_editing_note_content_tv.text = text
        bottom_notes_add_text_note_et.apply {
            this.setText(text)
            setSelection(length())
        }
        val keyboard =
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
        keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
        }, 30)
    }

    fun setOnToolBarReadyCallback(callback: (height: Int) -> Unit) {
        notes_toolbar_main_cl.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (notes_toolbar_main_cl.height > 0) {
                    callback.invoke(notes_toolbar_main_cl.height)
                    notes_toolbar_main_cl.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    fun setSwipeToCancelText(text: String) {
        swipeToCancelTv.text = text
    }

    fun setMessageInputHint(text: String) {
        bottom_notes_add_text_note_et.hint = text
    }

    fun setEdditingTitleText(text: String) {
        note_editing_title_tv.text = text
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.GONE
        }

    fun stopRecording(){
        isDraggingBlocked = true
        dispatchTouchEvents(
            notes_voice_iv,
            listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)
        )
    }

    fun stopEditing(){
        onCancelEditting?.invoke()
        setEdittingViewsVisibility(areVisible = false)
        bottom_notes_add_text_note_et?.text?.clear()
        bottom_notes_add_text_note_et.clearFocus()
        setUpTextNoteCreationToolbarVisibility(isVisible = false)
        hideKeyboard()
        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
        }, 30)
    }

    fun setOnNewToolbarHeightCallback(callback: (height: Int) -> Unit){
        onNewToolbarHeight = callback
    }

    init {
        View.inflate(context, R.layout.layout_media_toolbar_view, this)

        val toolTip = ToolTip.Builder()
            .setUpViews(
                targetView = notes_voice_iv,
                containerView = (context as Activity).window.decorView as ViewGroup
            )
            .setUpDuration(3000)
            .setMargin(4.toPx())
            .setUpText(context.getString(R.string.voice_hint))
            .setUpArrowPosition(arrowPosition = ToolTip.ArrowPosition.BOTTOM_RIGHT).build()

        bottom_notes_add_text_note_et.setUpOnKeyPreImePressedCallback {
            hideKeyboard()
            bottom_notes_add_text_note_et.clearFocus()
        }

        notes_toolbar_sketch_iv.setOnClickListener {
            onOpenSketch?.invoke()
        }

        notes_toolbar_camera_iv.setOnClickListener {
            onOpenCamera?.invoke()
        }


        note_editing_close_editing_mode_iv.setOnClickListener {
            stopEditing()
        }

        bottom_notes_add_text_note_send_iv.setOnClickListener {
            val text =
                if (bottom_notes_add_text_note_et.text.isNullOrEmpty()) "" else bottom_notes_add_text_note_et.text.toString()
                if(onSend?.invoke(text) != false){
                    setEdittingViewsVisibility(areVisible = false)
                    bottom_notes_add_text_note_et?.text?.clear()
                    bottom_notes_add_text_note_et.clearFocus()
                    hideKeyboard()
                    setUpTextNoteCreationToolbarVisibility(isVisible = false)
                }

        }

        notes_toolbar_text_iv.setOnClickListener {
            setUpTextNoteCreationToolbarVisibility(isVisible = true)
        }

        bottom_notes_add_text_note_et.doOnTextChanged { text, _, _, _ ->
            if (text?.trim().isNullOrEmpty() && !note_editing_title_tv.isVisible) {
                ImageViewCompat.setImageTintList(
                    bottom_notes_add_text_note_send_iv,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.defaultTextLight
                        )
                    )
                )
            } else {
                bottom_notes_add_text_note_send_iv.imageTintList = null
            }
            Handler(Looper.getMainLooper()).postDelayed({
                onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
            }, 30)
        }

        bottom_notes_add_text_note_et.setOnFocusChangeListener { _, isFocused ->
            if(isFocused){
                onNewToolbarHeight?.invoke(notes_toolbar_main_cl.height)
            }
            if (bottom_notes_add_text_note_et.text.isNullOrEmpty() && !note_editing_title_tv.isVisible) {
                setUpTextNoteCreationToolbarVisibility(isVisible = isFocused)
            }
        }

        notes_voice_iv.setOnTouchListener { v, event ->

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    isPointerOn = true
                    isDraggingBlocked = true
                    if (checkRecordAudioPermission()) {
                        touchedButNotRecording = true
                        job?.cancel()
                        job = CoroutineScope(Dispatchers.IO).launch {
                            delay(200)
                            withContext(Dispatchers.Main) {
                                if (isPointerOn) {
                                    toolTip?.hide()
                                    touchedButNotRecording = false
                                    onStartRecording?.invoke()
                                    stopped = false
                                    if (checkVibrationPermission()) {
                                        val vibe =
                                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibe?.vibrate(
                                                VibrationEffect.createOneShot(
                                                    100,
                                                    VibrationEffect.DEFAULT_AMPLITUDE
                                                )
                                            )
                                        } else vibe?.vibrate(100);
                                    }
                                    setEdittingViewsVisibility(areVisible = false)
                                    setNotesToolbarViewsVisibility(areVisible = false)
                                    setVoiceRecordingViewsVisibility(areVisible = true)
                                    isDraggingBlocked = false
                                    notes_toolbar_voice_background_iv.apply {
                                        animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .alpha(1f)
                                            .duration = 100
                                    }
                                    notes_voice_iv.setImageResource(R.drawable.notes_toolbar_voice_white)
                                    isVoiceBackgroundBeingAnimated = true

                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(100)
                                        isVoiceBackgroundBeingAnimated = false
                                        isRecording = true
                                    }
                                    Log.v("MediaToolbar", "Started recording")
                                    voiceRecorder.startRecord()
                                    voiceRecordingStartMillis = System.currentTimeMillis()

                                    startChronometer()
                                }
                            }
                        }

                    } else return@setOnTouchListener true

                }
                MotionEvent.ACTION_UP -> {
                    if(touchedButNotRecording){
                        toolTip?.show()
                    }
                    touchedButNotRecording = false
                    job?.cancel()
                    job = null
                    isPointerOn = false
                    if (!stopped) {
                        stopped = true
                        isRecording = false


                        notes_voice_iv.setImageResource(R.drawable.notes_toolbar_voice)
                        notes_toolbar_voice_background_iv.apply {
                            animate()
                                .scaleX(0f)
                                .scaleY(0f)
                                .duration = 50
                        }
                        voiceRecorder.stopRecord { file, duration ->
                            Log.v("MediaToolbar", "Stopped recording, File = $file")

                            val recordDuration =
                                System.currentTimeMillis() - voiceRecordingStartMillis

                            if (isDraggingBlocked || recordDuration < MIN_DURATION) {
                                deleteFile(file)
                            } else {
                                (context as Activity).window.setFlags(
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                )
                                Handler(Looper.getMainLooper()).postDelayed({
                                    (context as Activity).window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                }, 500)

                                val tempList = mutableListOf<Int>()
                                tempList.addAll(newAmplitudes)
                                amplitudesList.add(Pair(tempList, file))
                                onCompleteRecording?.invoke(
                                    amplitudesList[0].first,
                                    amplitudesList[0].second,
                                    duration
                                )

                                amplitudesList.removeAt(0)
                                newAmplitudes.clear()

                            }
                        }
                        resetToolbarOptionsPositions()
                        voiceRecordingStartMillis = -1
                        setVoiceRecordingViewsVisibility(areVisible = false)
                        setEdittingViewsVisibility(areVisible = false)
                        setNotesToolbarViewsVisibility(areVisible = true)
                        stopChronometer()

                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val recordDuration = System.currentTimeMillis() - voiceRecordingStartMillis
                    if (!isDraggingBlocked) {

                        if (event.rawX < initialIconX) {
                            setToolbarOptionsPositionsOnMoveEvent(event.rawX)
                        }
                        if (initialSwipeToCancelX - (initialIconX - event.rawX) / 2.2.toFloat() <= swipeLeftConstraintX || recordDuration > 300000L) {
                            isDraggingBlocked = true
                            dispatchTouchEvents(
                                v,
                                listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)
                            )


                        }

                    }
                }

            }

            true
        }
        setVoiceRecordingViewsVisibility(areVisible = true)
        setNotesToolbarViewsVisibility(areVisible = false)
        setToolbarsLayoutListeners()


        voiceRecorder.mediaNotesWithText.observe(
            (context as AppCompatActivity),
            androidx.lifecycle.Observer {
                onRecognizedSpeech?.invoke(it)
               /* if (amplitudesList.isNotEmpty()) {
                    onCompleteRecording?.invoke(
                        amplitudesList[0].first,
                        it,
                        amplitudesList[0].second
                    )
                    amplitudesList.removeAt(0)
                }*/

            })

        voiceRecorder.amplitude.observe(
            (context as AppCompatActivity),
            androidx.lifecycle.Observer {
                if (isRecording) {
                    val newAmpl =
                        if (it > MAX_AMPLITUDE) 15000 else if (it < MIN_AMPLITUDE) (MIN_AMPLITUDE..MIN_AMPLITUDE + 1000).random() else it
                    newAmplitudes.add(newAmpl)

                    if (!isAnimating) {
                        val newScale = if (it > 5000) 5000f else it.toFloat()
                        val additionalScale = newScale / 10000
                        isAnimating = true
                        notes_toolbar_voice_background_iv.apply {
                            animate()
                                .scaleX((voiceCircleInitScale + additionalScale))
                                .scaleY((voiceCircleInitScale + additionalScale))
                                .duration = 100
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            isAnimating = false
                        }, 100)
                    }
                }

            })

    }

    private  fun Int.toPx():Float {
        return this *  Resources.getSystem().displayMetrics.density
    }

    private fun dispatchTouchEvents(v: View, events: List<Int>) {
        for (e in events) {
            v.dispatchTouchEvent(
                MotionEvent.obtain(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    e,
                    0f,
                    0f,
                    0
                )
            )
        }
    }


    private fun startChronometer() {
        val elapsedRealtime = SystemClock.elapsedRealtime()
        voice_recording_duration_tv.base = elapsedRealtime
        voice_recording_duration_tv.setOnChronometerTickListener {
            voice_recording_voice_record_on_iv.startAnimation(
                AnimationUtils.loadAnimation(
                    context,
                    R.anim.blink_fade
                )
            )
        }

        voice_recording_duration_tv.start()
    }

    private fun stopChronometer() {
        voice_recording_duration_tv.setOnChronometerTickListener {

        }
        voice_recording_duration_tv.stop()
        val elapsedRealtime: Long = SystemClock.elapsedRealtime()
        voice_recording_duration_tv.base = elapsedRealtime
    }

    private fun resetToolbarOptionsPositions() {
        notes_voice_iv.animate()
            .x(initialIconX)
            .setDuration(300)
            .start()
        notes_toolbar_voice_background_iv.animate()
            .x(initialIconBackgroundX)
            .setDuration(300)
            .start()
        voice_recording_swipe_to_cancel_cl.animate()
            .x(initialSwipeToCancelX)
            .setDuration(300)
            .alpha(1f)
            .start()
    }

    private fun setToolbarOptionsPositionsOnMoveEvent(xPostion: Float) {
        notes_voice_iv.animate()
            .x(xPostion)
            .setDuration(0)
            .start()
        notes_toolbar_voice_background_iv.animate()
            .x(initialIconBackgroundX - initialIconX + xPostion)
            .setDuration(0)
            .start()
        voice_recording_swipe_to_cancel_cl.animate()
            .x(initialSwipeToCancelX - (initialIconX - xPostion) / 2.2.toFloat())
            .alpha((initialSwipeToCancelX - (initialIconX - xPostion) / 2.2.toFloat() - swipeLeftConstraintX) / (initialSwipeToCancelX - swipeLeftConstraintX))
            .setDuration(0)
            .start()
    }

    private fun setUpTextNoteCreationToolbarVisibility(isVisible: Boolean) {
        notes_toolbar_text_iv.isVisible = !isVisible
        notes_toolbar_camera_iv.isVisible = !isVisible
        notes_toolbar_sketch_iv.isVisible = !isVisible
        notes_voice_iv.isVisible = !isVisible
        notes_toolbar_voice_background_iv.isVisible = !isVisible
        bottom_notes_add_text_note_send_iv.isVisible = isVisible
        bottom_notes_add_text_note_et.isVisible = isVisible
        if (isVisible) {
            bottom_notes_add_text_note_et.apply {
                requestFocus()
                this.text?.clear()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                val keyboard =
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
            },60)

        }
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun setToolbarsLayoutListeners() {
        viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                initialIconX = notes_voice_iv.x
                initialIconBackgroundX = notes_toolbar_voice_background_iv.x
                rect = Rect(
                    notes_toolbar_voice_background_iv.left,
                    notes_toolbar_voice_background_iv.top,
                    notes_toolbar_voice_background_iv.right,
                    notes_toolbar_voice_background_iv.bottom
                )
                initialSwipeToCancelX =
                    voice_recording_swipe_to_cancel_cl.x
                swipeLeftConstraintX =
                    voice_recording_duration_tv.right.toFloat()
                if (initialSwipeToCancelX != 0f && swipeLeftConstraintX != 0f) {
                    setVoiceRecordingViewsVisibility(areVisible = false)
                    setNotesToolbarViewsVisibility(areVisible = true)
                    viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            }
        })


    }

    private fun checkRecordAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                (context as Activity).requestPermissions(
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    184567
                )
                false
            }

        } else {
            true
        }

    }

    private fun checkVibrationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.VIBRATE
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                false
            }

        } else {
            true
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        voiceRecorder.releaseRecorder()
    }

    private fun deleteFile(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(path)
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


 class CustomEditText : androidx.appcompat.widget.AppCompatEditText {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
            context!!,
            attrs,
            defStyle
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}
    constructor(context: Context?) : super(context!!) {}

    private var onKeyPreImePressed: (() -> Unit)? = null

    fun setUpOnKeyPreImePressedCallback(callback: () -> Unit) {
        onKeyPreImePressed = callback
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP
        ) {
            onKeyPreImePressed?.invoke()
            false
        } else super.dispatchKeyEvent(event)
    }
}