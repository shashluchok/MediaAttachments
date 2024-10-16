package ru.mediaattachments.presentation.widgets.mediatoolbarview

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
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doOnTextChanged
import ru.mediaattachments.utils.animate.please
import kotlinx.coroutines.*
import ru.mediaattachments.R
import ru.mediaattachments.databinding.LayoutMediaToolbarViewBinding
import ru.mediaattachments.utils.hideKeyboard
import ru.mediaattachments.presentation.widgets.tooltip.ToolTip
import ru.mediaattachments.presentation.widgets.voice.VoiceRecorder
import java.io.File

private const val maxAmplitude = 15000
private const val minAmplitude = 1500
private const val minAudioDuration = 1500L
private const val maxAudioDuration = 300000L * 3

private const val animationDuration = 100L
private const val toolbarResetAnimationDuration = 50L
private const val toolbarHeightChangeDelay = 100L

@SuppressLint("ClickableViewAccessibility")
class MediaToolbarView : ConstraintLayout {

    private var binding: LayoutMediaToolbarViewBinding =
        LayoutMediaToolbarViewBinding.inflate(LayoutInflater.from(context), this, true)

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

    private var onSend: ((String) -> Boolean)? = null
    private var onCancelEditting: (() -> Unit)? = null
    private var onStartRecording: (() -> Unit)? = null

    private var onNewToolbarHeight: ((Int) -> Unit)? = null

    private var onOpenCamera: (() -> Unit)? = null
    private var onOpenSketch: (() -> Unit)? = null

    private var onCompleteRecording: ((amplitudesList: List<Int>, filePath: String, duration: Int) -> Unit)? =
        null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun getCurrentText(): String {
        return binding.mediaToolbarDefault.bottomNotesAddTextNoteEt.text.toString()
    }

    fun setNewNoteText(text: String) {
        setUpTextNoteCreationToolbarVisibility(isVisible = true)
        with(binding.mediaToolbarDefault.bottomNotesAddTextNoteEt) {
            setText(text)
            setSelection(text.length)
        }
    }

    fun setOnMediaEditingCancelClickedCallback(callback: () -> Unit) {
        binding.mediaToolbarNoteEdit.cancelTv.setOnClickListener {
            callback.invoke()
            hideMediaEditingToolbar()
        }
    }

    fun setOnMediaCopyClickedCallback(callback: () -> Unit) {
        binding.mediaToolbarNoteEdit.copyIv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnMediaEditClickedCallback(callback: () -> Unit) {
        binding.mediaToolbarNoteEdit.editIv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnMediaDeleteClickedCallback(callback: () -> Unit) {
        binding.mediaToolbarNoteEdit.deleteIv.setOnClickListener {
            callback.invoke()
        }
    }

    fun showMediaEditingToolbar(isCopyable: Boolean, isEditable: Boolean) {
        setEdittingViewsVisibility(areVisible = false)
        with(binding.mediaToolbarNoteEdit) {
            if (root.alpha == 1f) {

                if (isCopyable) {

                    please(duration = 150L) {
                        animate(copyIv) toBe {
                            originalPosition()
                            visible()
                        }
                    }.start()


                } else {
                    please(duration = 150L) {
                        animate(copyIv) toBe {
                            leftOfItsParent()
                            invisible()
                        }
                    }.start()
                }

                if (isEditable) {
                    if (isCopyable) {
                        please(duration = 150L) {
                            animate(editIv) toBe {
                                originalPosition()
                                visible()
                            }
                        }.start()
                    } else {
                        please(duration = 150L) {
                            animate(editIv) toBe {
                                rightOf(deleteIv)
                                visible()
                            }
                        }.start()
                    }
                } else {
                    please(duration = 150L) {
                        animate(editIv) toBe {
                            leftOfItsParent()
                            invisible()
                        }
                    }.start()
                }
            } else {
                if (isCopyable) {

                    please(duration = 0L) {
                        animate(copyIv) toBe {
                            originalPosition()
                            visible()
                        }
                    }.start()


                } else {
                    please(duration = 0L) {
                        animate(copyIv) toBe {
                            leftOfItsParent()
                            invisible()
                        }
                    }.start()
                }

                if (isEditable) {
                    if (isCopyable) {
                        please(duration = 0L) {
                            animate(editIv) toBe {
                                originalPosition()
                                visible()
                            }
                        }.start()
                    } else {
                        please(duration = 0L) {
                            animate(editIv) toBe {
                                rightOf(deleteIv)
                                visible()
                            }
                        }.start()
                    }
                } else {
                    please(duration = 0L) {
                        animate(editIv) toBe {
                            leftOfItsParent()
                            invisible()
                        }
                    }.start()
                }
                please(duration = animationDuration) {
                    animate(binding.mediaToolbarDefault.root) toBe {
                        invisible()
                    }
                }.thenCouldYou(animationDuration) {
                    animate(root) toBe {
                        bottomOfItsParent()
                        visible()
                    }
                }.start()
            }

        }
        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(binding.mediaToolbarDefault.notesToolbarMainCl.height)
        }, toolbarHeightChangeDelay)

    }

    fun hideMediaEditingToolbar() {
        with(binding.mediaToolbarDefault) {
            setEdittingViewsVisibility(areVisible = !bottomNotesAddTextNoteEt.text.isNullOrEmpty())
            if (!bottomNotesAddTextNoteEt.text.isNullOrEmpty()) {
                notesToolbarTextIv.isVisible = false
                notesToolbarCameraIv.isVisible = false
                notesToolbarSketchIv.isVisible = false
                notesVoiceIv.isVisible = false
                notesToolbarVoiceBackgroundIv.isVisible = false
                bottomNotesAddTextNoteSendIv.isVisible = true
                bottomNotesAddTextNoteEt.isVisible = true
                bottomNotesAddTextNoteEt.requestFocus()
                hideKeyboard()
            } else {
                setUpTextNoteCreationToolbarVisibility(isVisible = false)
            }
        }

        please(duration = animationDuration) {
            animate(binding.mediaToolbarNoteEdit.root) toBe {
                originalPosition()
                invisible()
            }
        }.thenCouldYou(animationDuration) {
            animate(binding.mediaToolbarDefault.root) toBe {
                visible()
            }
        }.start()
        Handler(Looper.getMainLooper()).postDelayed({
            onNewToolbarHeight?.invoke(binding.mediaToolbarDefault.notesToolbarMainCl.height)
        }, toolbarHeightChangeDelay)
    }

    fun setOnOpenCameraCallback(callback: () -> Unit) {
        binding.mediaToolbarDefault.notesToolbarCameraIv.visibility = View.VISIBLE
        onOpenCamera = callback
    }

    fun setOnOpenSketchCallback(callback: () -> Unit) {
        binding.mediaToolbarDefault.notesToolbarSketchIv.visibility = View.VISIBLE
        onOpenSketch = callback
    }

    fun setOnStartRecordingCallback(callback: () -> Unit) {
        onStartRecording = callback
    }

    fun setOnCompleteRecordingCallback(callback: (amplitudesList: List<Int>, filePath: String, duration: Int) -> Unit) {
        onCompleteRecording = callback
    }

    fun setOnSendTextCallback(callback: (String) -> Boolean) {
        onSend = callback
    }

    fun setOnCancelEditingTextCallback(callback: () -> Unit) {
        onCancelEditting = callback
    }

    fun setText(text: String) {
        with(binding.mediaToolbarDefault) {

            setUpTextNoteCreationToolbarVisibility(isVisible = true)
            setEdittingViewsVisibility(areVisible = true)
            noteEditingNoteContentTv.text = text
            bottomNotesAddTextNoteEt.apply {
                setText(text)
                setSelection(length())
            }
            val keyboard =
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
            keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
            Handler(Looper.getMainLooper()).postDelayed({
                onNewToolbarHeight?.invoke(binding.mediaToolbarDefault.notesToolbarMainCl.height)
            }, toolbarHeightChangeDelay)

        }
    }

    fun setOnToolBarReadyCallback(callback: (height: Int) -> Unit) {
        with(binding.mediaToolbarDefault) {
            notesToolbarMainCl.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (notesToolbarMainCl.height > 0) {
                        callback.invoke(notesToolbarMainCl.height)
                        notesToolbarMainCl.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
    }

    fun setSwipeToCancelText(text: String) {
        binding.mediaToolbarDefault.swipeToCancelTv.text = text
    }

    fun setMessageInputHint(text: String) {
        binding.mediaToolbarDefault.bottomNotesAddTextNoteEt.hint = text
    }

    fun setEdditingTitleText(text: String) {
        binding.mediaToolbarDefault.noteEditingTitleTv.text = text
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.GONE
        }

    fun stopRecording() {
        isDraggingBlocked = true
        dispatchTouchEvents(
            binding.mediaToolbarDefault.notesVoiceIv,
            listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)
        )
    }

    fun clearEtFocus() {
        binding.mediaToolbarDefault.bottomNotesAddTextNoteEt.clearFocus()
    }

    fun stopEditing(): String {
        with(binding.mediaToolbarDefault) {
            onCancelEditting?.invoke()
            setEdittingViewsVisibility(areVisible = false)
            val text = (bottomNotesAddTextNoteEt?.text ?: "").toString()
            bottomNotesAddTextNoteEt.text?.clear()
            bottomNotesAddTextNoteEt.clearFocus()
            setUpTextNoteCreationToolbarVisibility(isVisible = false)
            hideKeyboard()
            Handler(Looper.getMainLooper()).postDelayed({
                onNewToolbarHeight?.invoke(notesToolbarMainCl.height)
            }, toolbarHeightChangeDelay)
            return text
        }
    }

    fun setOnNewToolbarHeightCallback(callback: (height: Int) -> Unit) {
        onNewToolbarHeight = callback
    }

    init {
        with(binding.mediaToolbarDefault) {

            val toolTip = ToolTip.Builder()
                .setUpViews(
                    targetView = notesVoiceIv,
                    containerView = (context as Activity).window.decorView as ViewGroup
                )
                .setUpDuration(3000)
                .setMargin(4.toPx())
                .setUpText(context.getString(R.string.voice_hint))
                .setUpArrowPosition(arrowPosition = ToolTip.ArrowPosition.BOTTOM_RIGHT).build()

            bottomNotesAddTextNoteEt.setUpOnKeyPreImePressedCallback {
                hideKeyboard()
                bottomNotesAddTextNoteEt.clearFocus()
            }

            notesToolbarSketchIv.setOnClickListener {
                onOpenSketch?.invoke()
            }

            notesToolbarCameraIv.setOnClickListener {
                onOpenCamera?.invoke()
            }


            noteEditingCloseEditingModeIv.setOnClickListener {
                stopEditing()
            }

            bottomNotesAddTextNoteSendIv.setOnClickListener {
                val text =
                    if (bottomNotesAddTextNoteEt.text.isNullOrEmpty()) "" else bottomNotesAddTextNoteEt.text.toString()
                if (onSend?.invoke(text) != false) {
                    setEdittingViewsVisibility(areVisible = false)
                    bottomNotesAddTextNoteEt?.text?.clear()
                    bottomNotesAddTextNoteEt.clearFocus()
                    hideKeyboard()
                    setUpTextNoteCreationToolbarVisibility(isVisible = false)
                }

            }

            notesToolbarTextIv.setOnClickListener {
                setUpTextNoteCreationToolbarVisibility(isVisible = true)
            }

            bottomNotesAddTextNoteEt.doOnTextChanged { text, _, _, _ ->
                if (text?.trim().isNullOrEmpty() && !noteEditingTitleTv.isVisible) {
                    ImageViewCompat.setImageTintList(
                        bottomNotesAddTextNoteSendIv,
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.defaultTextLight
                            )
                        )
                    )
                } else {
                    bottomNotesAddTextNoteSendIv.imageTintList = null
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    onNewToolbarHeight?.invoke(notesToolbarMainCl.height)
                }, toolbarHeightChangeDelay)
            }

            bottomNotesAddTextNoteEt.setOnFocusChangeListener { _, isFocused ->
                if (isFocused) {
                    onNewToolbarHeight?.invoke(notesToolbarMainCl.height)
                }
                if (bottomNotesAddTextNoteEt.text.isNullOrEmpty() && !noteEditingTitleTv.isVisible) {
                    setUpTextNoteCreationToolbarVisibility(isVisible = isFocused)
                }
            }

            notesVoiceIv.setOnTouchListener { v, event ->

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
                                        notesToolbarVoiceBackgroundIv.apply {
                                            animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .alpha(1f)
                                                .duration = animationDuration
                                        }
                                        notesVoiceIv.setImageResource(R.drawable.notes_toolbar_voice_white)
                                        isVoiceBackgroundBeingAnimated = true

                                        CoroutineScope(Dispatchers.IO).launch {
                                            delay(animationDuration)
                                            isVoiceBackgroundBeingAnimated = false
                                            isRecording = true
                                        }
                                        voiceRecorder.startRecord()
                                        voiceRecordingStartMillis = System.currentTimeMillis()

                                        startChronometer()
                                    }
                                }
                            }

                        } else return@setOnTouchListener true

                    }

                    MotionEvent.ACTION_UP -> {
                        if (touchedButNotRecording) {
                            toolTip?.show()
                        }
                        touchedButNotRecording = false
                        job?.cancel()
                        job = null
                        isPointerOn = false
                        if (!stopped) {
                            stopped = true
                            isRecording = false


                            notesVoiceIv.setImageResource(R.drawable.notes_toolbar_voice)
                            notesToolbarVoiceBackgroundIv.apply {
                                animate()
                                    .scaleX(0f)
                                    .scaleY(0f)
                                    .duration = 50
                            }
                            voiceRecorder.stopRecord { file, duration ->

                                val recordDuration =
                                    System.currentTimeMillis() - voiceRecordingStartMillis

                                if (isDraggingBlocked || recordDuration < minAudioDuration) {
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
                            if (initialSwipeToCancelX - (initialIconX - event.rawX) / 2.2.toFloat() <= swipeLeftConstraintX || recordDuration > maxAudioDuration) {
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

            voiceRecorder.amplitude.observe(
                (context as AppCompatActivity),
                androidx.lifecycle.Observer {
                    if (isRecording) {
                        val newAmpl =
                            if (it > maxAmplitude) maxAmplitude else if (it < minAmplitude) (minAmplitude..minAmplitude + 1000).random() else it
                        newAmplitudes.add(newAmpl)

                        if (!isAnimating) {
                            val newScale = if (it > 5000) 5000f else it.toFloat()
                            val additionalScale = newScale / 10000
                            isAnimating = true
                            notesToolbarVoiceBackgroundIv.apply {
                                animate()
                                    .scaleX((voiceCircleInitScale + additionalScale))
                                    .scaleY((voiceCircleInitScale + additionalScale))
                                    .duration = animationDuration
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAnimating = false
                            }, animationDuration)
                        }
                    }

                })

        }
    }

    private fun Int.toPx(): Float {
        return this * Resources.getSystem().displayMetrics.density
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
        with(binding.mediaToolbarDefault) {

            val elapsedRealtime = SystemClock.elapsedRealtime()
            voiceRecordingDurationTv.base = elapsedRealtime
            voiceRecordingDurationTv.setOnChronometerTickListener {
                voiceRecordingVoiceRecordOnIv.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.blink_fade
                    )
                )
            }

            voiceRecordingDurationTv.start()

        }
    }

    private fun stopChronometer() {
        with(binding.mediaToolbarDefault) {
            voiceRecordingDurationTv.setOnChronometerTickListener {}
            voiceRecordingDurationTv.stop()
            val elapsedRealtime: Long = SystemClock.elapsedRealtime()
            voiceRecordingDurationTv.base = elapsedRealtime
        }
    }

    private fun resetToolbarOptionsPositions() {
        with(binding.mediaToolbarDefault) {
            notesVoiceIv.animate()
                .x(initialIconX)
                .setDuration(toolbarResetAnimationDuration)
                .start()
            notesToolbarVoiceBackgroundIv.animate()
                .x(initialIconBackgroundX)
                .setDuration(toolbarResetAnimationDuration)
                .start()
            voiceRecordingSwipeToCancelCl.animate()
                .x(initialSwipeToCancelX)
                .setDuration(toolbarResetAnimationDuration)
                .alpha(1f)
                .start()
        }
    }

    private fun setToolbarOptionsPositionsOnMoveEvent(xPosition: Float) {
        with(binding.mediaToolbarDefault) {
            notesVoiceIv.animate()
                .x(xPosition)
                .setDuration(0)
                .start()
            notesToolbarVoiceBackgroundIv.animate()
                .x(initialIconBackgroundX - initialIconX + xPosition)
                .setDuration(0)
                .start()
            voiceRecordingSwipeToCancelCl.animate()
                .x(initialSwipeToCancelX - (initialIconX - xPosition) / 2.2.toFloat())
                .alpha((initialSwipeToCancelX - (initialIconX - xPosition) / 2.2.toFloat() - swipeLeftConstraintX) / (initialSwipeToCancelX - swipeLeftConstraintX))
                .setDuration(0)
                .start()
        }
    }

    private fun setUpTextNoteCreationToolbarVisibility(isVisible: Boolean) {
        with(binding.mediaToolbarDefault) {
            notesToolbarTextIv.isVisible = !isVisible
            notesToolbarCameraIv.isVisible = !isVisible
            notesToolbarSketchIv.isVisible = !isVisible
            notesVoiceIv.isVisible = !isVisible
            notesToolbarVoiceBackgroundIv.isVisible = !isVisible
            bottomNotesAddTextNoteSendIv.isVisible = isVisible
            bottomNotesAddTextNoteEt.isVisible = isVisible
            if (isVisible) {
                bottomNotesAddTextNoteEt.apply {
                    requestFocus()
                    this.text?.clear()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    val keyboard =
                        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                    keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
                }, 60)

            }
        }
    }

    private fun setToolbarsLayoutListeners() {
        with(binding.mediaToolbarDefault) {
            viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    initialIconX = notesVoiceIv.x
                    initialIconBackgroundX = notesToolbarVoiceBackgroundIv.x
                    rect = Rect(
                        notesToolbarVoiceBackgroundIv.left,
                        notesToolbarVoiceBackgroundIv.top,
                        notesToolbarVoiceBackgroundIv.right,
                        notesToolbarVoiceBackgroundIv.bottom
                    )
                    initialSwipeToCancelX =
                        voiceRecordingSwipeToCancelCl.x
                    swipeLeftConstraintX =
                        voiceRecordingDurationTv.right.toFloat()
                    if (initialSwipeToCancelX != 0f && swipeLeftConstraintX != 0f) {
                        setVoiceRecordingViewsVisibility(areVisible = false)
                        setNotesToolbarViewsVisibility(areVisible = true)
                        viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
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
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.VIBRATE
            ) ==
                    PackageManager.PERMISSION_GRANTED

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

    private fun setEdittingViewsVisibility(areVisible: Boolean) {
        with(binding.mediaToolbarDefault) {
            noteEdittingPencilIv.isVisible = areVisible
            noteEditingTitleTv.isVisible = areVisible
            noteEditingNoteContentTv.isVisible = areVisible
            noteEditingCloseEditingModeIv.isVisible = areVisible
            edittingDivider.isVisible = areVisible
        }
    }

    private fun setVoiceRecordingViewsVisibility(areVisible: Boolean) {
        with(binding.mediaToolbarDefault) {
            voiceRecordingVoiceRecordOnIv.isVisible = areVisible
            voiceRecordingDurationTv.isVisible = areVisible
            voiceRecordingSwipeToCancelCl.isVisible = areVisible
        }
    }

    private fun setNotesToolbarViewsVisibility(areVisible: Boolean) {
        with(binding.mediaToolbarDefault) {
            notesToolbarCameraIv.isVisible = areVisible
            notesToolbarTextIv.isVisible = areVisible
            notesToolbarSketchIv.isVisible = areVisible
        }
    }
}
