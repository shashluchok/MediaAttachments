package ru.scheduled.mediaattachmentslibrary

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.animate
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.camera_capture_view.view.*
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


class CameraCaptureView : ConstraintLayout {

    private val MEDIA_NOTES_INTERNAL_DIRECTORY = "media_attachments"

    private var lastRotation = 0
    private var cam: Camera? = null
    private var isFlashLightOn = false
    private var lastCameraImageUri: String? = null
    private var backCamera = CameraSelector.DEFAULT_BACK_CAMERA
    private var frontCamera = CameraSelector.DEFAULT_FRONT_CAMERA
    private var isBackCameraOn = false
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var isVideoCaptureEnabled = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var isLessThanLongClick = false
    private var isPointerOn = false
    private var initTimeInMillis = 0L
    private var isDraggingBlocked = true
    private var currentOrientationIsPortrait = true
    private var mOrientationListener: OrientationEventListener? = null
    private var initialIconX: Float = 0f

    private var animJob: Job? = null

    private var analyzer: AnalyzerM? = null

    private var initCenterX = 0f
    private var initCenterY = 0f
    private var initScale = 0f

    private var currentSavedVideoUri: Uri? = null

    private var onVideoSaved: ((Uri) -> Unit)? = null
    private var onImageSaved: ((uri: Uri) -> Unit)? = null
    private var onPhotoClicked: (() -> Unit)? = null

    private var saveDestination: SaveLocation = SaveLocation.INTERNAL_STORAGE

    enum class SaveLocation {
        GALLERY, INTERNAL_STORAGE
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private fun stopMoves() {
        animJob?.cancel()
        animJob = null
    }

    private fun flash() {
        photoFlashView.visibility = View.VISIBLE
        photoFlashView.apply {
            alpha = 1f
            animate().alpha(0f).setDuration(900)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            photoFlashView.visibility = View.GONE
        }, 900)
    }


    init {
        View.inflate(context, R.layout.camera_capture_view, this)
        if (isEmulator()) {
            flashlight_container.isVisible = false
            previous_photo_container.isVisible = false
            change_camera_iv.isVisible = false
            preview_view2.isVisible = false
            mockedCl.isVisible = true
            notClickableFilterView.visibility = View.VISIBLE
            mockedDataIv.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (mockedCl.width > 0 && mockedCl.height > 0) {
                        initCenterX = mockedCl.x + mockedCl.width / 2
                        initCenterY = mockedCl.y + mockedCl.height / 2
                        initScale = 1f
                        simulateMoves(mockedDataIv)
                        mockedDataIv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }

            })
            initEmulatedCamera()

            photo_video_capture_icon.setOnClickListener {
                stopMoves()
                notClickableFilterView.visibility = View.VISIBLE
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        blurView.visibility = View.VISIBLE
                        blurView.alpha = 1f
                    }

                    withContext(Dispatchers.Main) {
                        mockedDataIv.apply {
                            animate()
                                .scaleX(1.15f)
                                .scaleY(1.15f)
                                .duration = 400
                        }

                        for (i in 1..4) {
                            delay(100)
                            withContext(Dispatchers.Main) {
                                blurView.setBlurEnabled(false)
                                blurView.setBlurRadius((1 + i).toFloat())
                                blurView.setBlurEnabled(true)

                            }
                        }

                    }
                    withContext(Dispatchers.Main) {
                        mockedDataIv.apply {
                            animate()
                                .scaleX(1.08f)
                                .scaleY(1.08f)
                                .duration = 250
                        }
                        for (i in 1..9) {
                            delay(30)
                            withContext(Dispatchers.Main) {
                                blurView.setBlurEnabled(false)
                                blurView.setBlurRadius((10 - i).toFloat())
                                blurView.setBlurEnabled(true)
                            }
                        }
                    }
                    blurView.setBlurEnabled(false)
                    delay(300)

                    withContext(Dispatchers.Main) {
//                showLoader()
                        takeEmulatedPhoto()
                        blurView.visibility = View.GONE
                        notClickableFilterView.visibility = View.GONE
                    }

                }

            }

        } else {
            setUpOrientationChangeListener()
            setUpCamera()
            setLastGalleryImage()
        }


    }

    private fun takeEmulatedPhoto() {
        flash()


        mockedCl.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mockedCl.height != 0 && mockedCl.width != 0) {
                    mockedCl.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    )
                    val bitmap = setViewToBitmapImage(mockedCl)
                    val mydir = context?.getDir("photos", Context.MODE_PRIVATE)

                    if (!(mydir?.exists() ?: false)) {
                        mydir?.mkdirs()
                    }

                    val photoFile = File(
                        mydir,
                        SimpleDateFormat(
                            "HHmmssddMMyyyy",
                            Locale.US
                        ).format(System.currentTimeMillis()) + ".jpg"
                    )

                    GlobalScope.launch(Dispatchers.IO) {
                        if (photoFile.exists()) photoFile.delete()
                        photoFile.createNewFile()
                        val fos = FileOutputStream(photoFile)
                        val stream = ByteArrayOutputStream()
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val image = stream.toByteArray()
                        fos.write(image)
                        fos.flush()
                        fos.close()
                        withContext(Dispatchers.Main) {
                            onImageSaved?.invoke(Uri.fromFile(photoFile))
                        }
                    }

                }
            }
        })


    }

    private fun setViewToBitmapImage(view: View): Bitmap? {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null)
            bgDrawable.draw(canvas) else
            canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedBitmap
    }

    private fun simulateMoves(animatedView: View) {
        val constraint = 70
        val scaleConstraint = 0.2
        val delayMin = 1000
        val delayMax = 1500
        animJob = GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {

                val currentCenterX = animatedView.x + animatedView.width / 2
                val currentCenterY = animatedView.y + animatedView.height / 2
                val currentScale = animatedView.scaleX

                val currentDiffX = currentCenterX - initCenterX
                val currentDiffY = currentCenterY - initCenterY
                val currentDiffScale = currentScale - initScale

                val delayRand = kotlin.random.Random.nextInt(delayMin, delayMax)

                var isDownScale = Random().nextBoolean()
                if (currentDiffScale <= initScale - constraint)
                    isDownScale = false
                if (currentDiffScale >= constraint + initScale)
                    isDownScale = true


                var isHorizontalLeft = Random().nextBoolean()
                if (currentDiffX <= -constraint)
                    isHorizontalLeft = false
                if (currentDiffX >= constraint)
                    isHorizontalLeft = true

                var isVerticalTop = Random().nextBoolean()

                if (currentDiffY <= -constraint)
                    isVerticalTop = false

                if (currentDiffY >= constraint)
                    isVerticalTop = true

                var scaleMove = kotlin.random.Random.nextDouble(0.0, 0.01)
                if (scaleConstraint - abs(currentDiffScale.toDouble()).toInt() < scaleMove) {
                    scaleMove = scaleConstraint - abs(currentDiffScale.toDouble()).toInt()
                }

                if (isDownScale) scaleMove *= -1


                var moveVertical = kotlin.random.Random.nextInt(2, 3)
                if (constraint - abs(currentDiffY.toDouble()).toInt() < moveVertical) {
                    moveVertical = constraint - abs(currentDiffY.toDouble()).toInt()
                }
                if (isVerticalTop) moveVertical *= -1

                var moveHorizontal = kotlin.random.Random.nextInt(2, 3)
                if (constraint - abs(currentDiffX.toDouble()).toInt() < moveHorizontal) {
                    moveHorizontal = constraint - abs(currentDiffX.toDouble()).toInt()
                }
                if (isHorizontalLeft) moveHorizontal *= -1

                withContext(Dispatchers.Main) {
                    animatedView.apply {
                        animate()
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .x(moveHorizontal.toFloat())
                            .y(moveVertical.toFloat())
                            .scaleX(initScale + scaleMove.toFloat())
                            .scaleY(initScale + scaleMove.toFloat())
                            .duration = delayRand.toLong()
                    }
                }
                delay((delayRand / 1.15).toLong())

            }
        }
    }

    private fun initEmulatedCamera() {

        val decorView = camera_fragment_main_layout
        val rootView =
            (mockedCl as ViewGroup)
        val windowBackground = decorView.background
        val radius = 10f
        blurView.setupWith(rootView)
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(context))
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)

        GlobalScope.launch(Dispatchers.IO) {
            delay(1100)
            for (i in 1..9) {
                delay(7)
                withContext(Dispatchers.Main) {
                    blurView.setBlurEnabled(false)
                    blurView.setBlurRadius((10 - i).toFloat())
                    blurView.setBlurEnabled(true)
                }

            }
            withContext(Dispatchers.Main) {
                blurView.apply {
                    animate().alpha(0f).duration = 50
                }
            }
            delay(60)
            withContext(Dispatchers.Main) {
                blurView.visibility = View.GONE
                blurView.alpha = 1f
                blurView.setBlurEnabled(true)
                blurView.setBlurRadius((1).toFloat())
            }

            delay(200)

            withContext(Dispatchers.Main) {
                blurView.setBlurEnabled(false)

            }
            delay(1000)
            withContext(Dispatchers.Main) {
                notClickableFilterView.visibility = View.GONE
            }
        }

    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    fun setOnPhotoSavedCallback(callback: (uri: Uri) -> Unit) {
        onImageSaved = callback
    }

    fun setOnPhotoClickedCallback(callback: () -> Unit) {
        onPhotoClicked = callback
    }

    fun setSaveLocation(location: SaveLocation) {
        saveDestination = location
    }

    fun setOnVideoSavedCallback(callback: (Uri) -> Unit) {
        (isVideoCaptureEnabled) = true
        onVideoSaved = callback
    }

    fun setOnGalleryClickedCallback(callback: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            previous_photo_container?.setOnClickListener {
                if (previous_photo_iv.visibility == View.VISIBLE) {
                    callback.invoke()
                }
            }
        }, 300)

    }

    fun setOnCloseClickedCallback(callback: () -> Unit) {
        close_container.setOnClickListener {
            callback.invoke()
        }
    }


    @SuppressLint("Range")
    private fun setLastGalleryImage() {
        var lastImageUri = ""
        var imageCursor: Cursor? = null
        try {
            val columns =
                arrayOf(MediaStore.Images.Media.DATA)
            val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
            imageCursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                orderBy
            )
            if (imageCursor != null) {
                if (imageCursor?.moveToNext() ?: false) {
                    lastImageUri =
                        imageCursor!!.getString(
                            imageCursor!!.getColumnIndex(
                                MediaStore.Images.Media.DATA
                            )
                        )

                }
            }

            if (!lastImageUri.isNullOrEmpty()) {
                previous_photo_no_photo_iv.visibility = View.GONE
                previous_photo_iv.visibility = View.VISIBLE
                Glide.with(this).load(lastImageUri)
                    .transition(DrawableTransitionOptions.withCrossFade(175))
                    .into(previous_photo_iv)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            if (imageCursor != null && !imageCursor!!.isClosed) {
                imageCursor!!.close();
            }
        }
    }

    private fun setUpCamera() {
        CoroutineScope(Dispatchers.Default).launch {
            delay(500)
            withContext(Dispatchers.Main) {
                initListeners()
                setPhotoVideoCaptureButtonLayoutListener()
                if (checkCameraPermissionGranted()) {
                    startCamera(backCamera)
                    isBackCameraOn = true
                }
            }
        }
    }

    private fun setPhotoVideoCaptureButtonLayoutListener() {
        viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                initialIconX = photo_video_capture_icon.x
                if (initialIconX != 0f) {
                    viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            }
        })

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {

        flashlight_container.setOnClickListener {
            if (cam != null && cam!!.cameraInfo.hasFlashUnit()) {
                if (!isFlashLightOn) {
                    isFlashLightOn = true
                    flashlight_icon.setImageResource(R.drawable.flashlight_on)
                } else {
                    isFlashLightOn = false
                    flashlight_icon.setImageResource(R.drawable.flashlight_off)
                }
                imageCapture?.flashMode = if (isFlashLightOn) {
                    ImageCapture.FLASH_MODE_ON
                } else ImageCapture.FLASH_MODE_OFF
            }
        }


        change_camera_iv.setOnClickListener {
            if (checkCameraPermissionGranted()) {
                isBackCameraOn = if (isBackCameraOn) {
                    startCamera(frontCamera)
                    flashlight_container.visibility = View.GONE
                    false
                } else {
                    startCamera(backCamera)
                    flashlight_container.visibility = View.VISIBLE
                    true
                }
            }
        }


        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = cam!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                cam!!.cameraControl.setZoomRatio(scale)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, listener)
        preview_view2.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener false
        }


        if (isEmulator()) {

        } else {
            if (isVideoCaptureEnabled) {
                photo_video_capture_icon.setOnTouchListener { v, event ->
                    if (checkCameraPermissionGranted() && checkRecordAudioPermission()) {

                        when (event.action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_DOWN -> {
                                isLessThanLongClick = true
                                isPointerOn = true
                                initTimeInMillis = System.currentTimeMillis()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isLessThanLongClick = false
                                    if (isPointerOn) {
                                        vibrate()
                                        isDraggingBlocked = false
                                        Glide.with(this).load(R.drawable.capture_video_button)
                                            .into(photo_video_capture_icon)
                                        startRecording()
                                        startChronometer()
                                        changeInterfaceOnVideoRecording(recording = true)
                                    }
                                }, 1000)
                            }

                            MotionEvent.ACTION_UP -> {
                                if (isLessThanLongClick) {
                                    onPhotoClicked?.invoke()
                                    disableUserInteraction()
                                    takePhoto()
                                }
                                if (isPointerOn) {
                                    stopRecording()
                                    stopChronometer()
                                    changeInterfaceOnVideoRecording(recording = false)
                                    resetVideoCaptureButtonPositions()
                                    Glide.with(this).load(R.drawable.capture_photo_button)
                                        .into(photo_video_capture_icon)
                                    if (isDraggingBlocked) {
                                        currentSavedVideoUri?.let {
                                            deleteFile(it.toString())
                                        }
                                    } else {
                                        disableUserInteraction()
                                        CoroutineScope(Dispatchers.IO).launch {
                                            while (currentSavedVideoUri == null) {
                                                delay(100)
                                            }
                                            withContext(Dispatchers.Main) {
                                                currentSavedVideoUri?.let {
                                                    onVideoSaved?.invoke(it)
//                                                viewModel.saveVideoToCache(it,shardId)
                                                    currentSavedVideoUri = null
                                                }

                                            }
                                        }
                                    }

                                }
                                isPointerOn = false
                                isLessThanLongClick = false
                                resetVideoCaptureButtonPositions()
                                isDraggingBlocked = true
                            }

                            MotionEvent.ACTION_MOVE -> {
                                if (!isDraggingBlocked) {

                                    if (!setPhotoVideoCaptureButtonPositionOnMoveEvent(event.rawX)) {
                                        isDraggingBlocked = true
                                        dispatchTouchEvents(
                                            v,
                                            listOf(
                                                MotionEvent.ACTION_UP,
                                                MotionEvent.ACTION_POINTER_UP
                                            )
                                        )
                                    }

                                }
                            }

                        }
                    }

                    true
                }
            } else {
                photo_video_capture_icon.setOnClickListener {
                    onPhotoClicked?.invoke()
                    disableUserInteraction()
                    takePhoto()
                }
            }
        }
    }

    private fun exifToDegrees(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val metadata = ImageCapture.Metadata()

        val bitmap = preview_view2.bitmap
        photo_preview_iv.visibility = View.VISIBLE
        Glide.with(context).load(bitmap).into(photo_preview_iv)
        preview_view2.visibility = View.GONE
        var photoFile: File? = null
        if (!isBackCameraOn) {
            metadata.isReversedHorizontal = true
        }
        val outputOptions: ImageCapture.OutputFileOptions

        if (saveDestination == SaveLocation.GALLERY) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

                val contentValues = ContentValues().apply {
                    val name = SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis()) + ".jpg"
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Images/LeadFrog")
                }
                outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).setMetadata(metadata).build()


            } else {
                val values = ContentValues()
                values.put(
                    MediaStore.Images.Media.TITLE,
                    SimpleDateFormat("HHmmssddMMyyyy", Locale.US).format(
                        System.currentTimeMillis()
                    )
                )
                values.put(
                    MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis())
                )
                values.put(
                    MediaStore.Images.Media.DESCRIPTION, SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis())
                )
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).setMetadata(
                    metadata
                ).build()
            }

        } else {
            val mydir = context.getDir(MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

            if (!mydir.exists()) {
                mydir.mkdirs()
            }

            photoFile = File(
                mydir,
                SimpleDateFormat(
                    "HHmmssddMMyyyy",
                    Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )

            outputOptions = ImageCapture.OutputFileOptions.Builder(
                photoFile
            ).setMetadata(
                metadata
            ).build()


        }

        if (!isBackCameraOn) {
            enableUserInteraction()
            val bitmap = preview_view2.bitmap
            val fos = FileOutputStream(photoFile)
            val stream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageBytes = stream.toByteArray()
            fos.write(imageBytes)
            fos.flush()
            fos.close()
            onImageSaved?.invoke(Uri.fromFile(photoFile))
        } else {

            var flag = false


            GlobalScope.launch(Dispatchers.IO) {
                var mbitmap: Bitmap? = null
                if (isFlashLightOn) {
                    delay(700)
                }
                withContext(Dispatchers.Main) {
                    mbitmap = preview_view2.bitmap
                }
                while (!flag) {
                    delay(100)
                }
                withContext(Dispatchers.Main) {
                    photoFile?.let {
                        if (photoFile.exists()) photoFile.delete()
                    }

                    val fos = FileOutputStream(photoFile)
                    val stream = ByteArrayOutputStream()
                    mbitmap?.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val imageBytes = stream.toByteArray()
                    fos.write(imageBytes)
                    fos.flush()
                    fos.close()
                    onImageSaved?.invoke(Uri.fromFile(photoFile))
                }
            }


            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        enableUserInteraction()
                        flag = true
                    }

                    override fun onError(exception: ImageCaptureException) {
                        enableUserInteraction()
                        exception.printStackTrace()
                    }

                })
        }


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

    private fun changeInterfaceOnVideoRecording(recording: Boolean) {
        if (recording) {
            flashlight_container.visibility = View.GONE
            close_container.visibility = View.GONE
            change_camera_iv.visibility = View.GONE
            previous_photo_container.visibility = View.GONE
            video_recording_swipe_to_cancel_cl.visibility = View.VISIBLE
        } else {
            flashlight_container.visibility = View.VISIBLE
            close_container.visibility = View.VISIBLE
            change_camera_iv.visibility = View.VISIBLE
            previous_photo_container.visibility = View.VISIBLE
            video_recording_swipe_to_cancel_cl.visibility = View.GONE
        }
    }

    @SuppressLint("RestrictedApi")
    private fun stopRecording() {
        videoCapture?.stopRecording()
    }

    private fun resetVideoCaptureButtonPositions() {
        photo_video_capture_icon.animate()
            .x(initialIconX)
            .setDuration(200)
            .start()
        video_recording_swipe_to_cancel_cl.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun setPhotoVideoCaptureButtonPositionOnMoveEvent(xPostion: Float): Boolean {
        if (xPostion > initialIconX / 2) {
            val newButtonPosition: Float =
                if ((initialIconX) - (initialIconX - xPostion) / 1.6 > initialIconX) initialIconX
                else (initialIconX) - (initialIconX - xPostion) / 1.5.toFloat()
            photo_video_capture_icon.animate()
                .x(newButtonPosition.toFloat())
                .setDuration(0)
                .start()
            val newScale = newButtonPosition / initialIconX
            video_recording_swipe_to_cancel_cl.animate()
                .scaleX(newScale)
                .scaleY(newScale)
                .alpha(newScale)
                .setDuration(0)
                .start()
            return true
        }
        return false
    }

    private fun startChronometer() {
        video_recording_timer_cl.visibility = View.VISIBLE

        val elapsedRealtime = SystemClock.elapsedRealtime()
        video_recording_duration_tv.base = elapsedRealtime
        video_recording_duration_tv.setOnChronometerTickListener {
            video_recording_video_record_on_iv.startAnimation(
                AnimationUtils.loadAnimation(
                    context,
                    R.anim.blink_fade
                )
            )
        }
        video_recording_duration_tv.start()
    }

    private fun stopChronometer() {
        video_recording_timer_cl.visibility = View.GONE
        video_recording_duration_tv.stop()
        val elapsedRealtime: Long = SystemClock.elapsedRealtime()
        video_recording_duration_tv.base = elapsedRealtime
    }

    private fun disableUserInteraction() {
        (context as Activity).window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun startCamera(cameraSelector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        disableUserInteraction()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            enableUserInteraction()
        }, 500)

        cameraProviderFuture.addListener(Runnable {

            cameraProvider = cameraProviderFuture.get()
            if (cameraProvider != null) {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(preview_view2.surfaceProvider)
                    }


                videoCapture = VideoCapture.Builder().build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val executor = ContextCompat.getMainExecutor(context)
                analyzer = AnalyzerM()

                val cameraSelector = cameraSelector

                if (!cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    change_camera_iv.visibility = View.GONE
                }
                cameraProvider!!.unbindAll()

                if (isVideoCaptureEnabled) {
                    try {
                        cam = cameraProvider!!.bindToLifecycle(
                            context as AppCompatActivity,
                            cameraSelector,
                            preview,
                            imageCapture,
                            videoCapture
                        )


                    } catch (exc: Exception) {
                        cam = cameraProvider!!.bindToLifecycle(
                            context as AppCompatActivity, cameraSelector, preview, imageCapture
                        )
                    }
                } else {
                    analyzer?.let {
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(preview_view2.width, preview_view2.height))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    executor, analyzer!!
                                )
                            }
                        cam = cameraProvider!!.bindToLifecycle(
                            context as AppCompatActivity,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                            imageCapture
                        )
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
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
                    1993993399
                )
                false
            }

        } else {
            true
        }

    }

    private fun checkCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableUserInteraction() {
        (context as Activity).window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @SuppressLint("RestrictedApi")
    private fun startRecording() {
        if (videoCapture == null) return
        val metadata = VideoCapture.Metadata()

        val outputOptions: VideoCapture.OutputFileOptions

        if (saveDestination == CameraCaptureView.SaveLocation.GALLERY) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

                val contentValues = ContentValues().apply {
                    val name = SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis()) + ".mp4"
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/LeadFrog")
                }
                outputOptions = VideoCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).setMetadata(metadata).build()


            } else {
                val values = ContentValues()
                values.put(
                    MediaStore.Video.Media.TITLE,
                    SimpleDateFormat("HHmmssddMMyyyy", Locale.US).format(
                        System.currentTimeMillis()
                    )
                )
                values.put(
                    MediaStore.Video.Media.DISPLAY_NAME, SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis())
                )
                values.put(
                    MediaStore.Video.Media.DESCRIPTION, SimpleDateFormat(
                        "HHmmssddMMyyyy",
                        Locale.US
                    ).format(System.currentTimeMillis())
                )
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                outputOptions = VideoCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                ).setMetadata(
                    metadata
                ).build()
            }

        } else {
            val mydir = context.getDir(MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

            if (!mydir.exists()) {
                mydir.mkdirs()
            }

            val videoFile = File(
                mydir,
                SimpleDateFormat(
                    "HHmmssddMMyyyy",
                    Locale.US
                ).format(System.currentTimeMillis()) + ".mp4"
            )

            outputOptions = VideoCapture.OutputFileOptions.Builder(
                videoFile
            ).setMetadata(
                metadata
            ).build()
        }

        videoCapture?.startRecording(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    enableUserInteraction()
                    cause?.printStackTrace()
                }

                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    enableUserInteraction()
                    currentSavedVideoUri = outputFileResults.savedUri
                }
            })
    }

    private fun getTransparentPercentage(num: Int): String {
        return when (num) {
            100 -> "FF"
            99 -> "FC"
            98 -> "FA"
            97 -> "F7"
            96 -> "F5"
            95 -> "F2"
            94 -> "F0"
            93 -> "ED"
            92 -> "EB"
            91 -> "E8"
            90 -> "E6"
            89 -> "E3"
            88 -> "E0"
            87 -> "DE"
            86 -> "DB"
            85 -> "D9"
            84 -> "D6"
            83 -> "D4"
            82 -> "D1"
            81 -> "CF"
            80 -> "CC"
            79 -> "C9"
            78 -> "C7"
            77 -> "C4"
            76 -> "C2"
            75 -> "BF"
            74 -> "BD"
            73 -> "BA"
            72 -> "B8"
            71 -> "B5"
            70 -> "B3"
            69 -> "B0"
            68 -> "AD"
            67 -> "AB"
            66 -> "A8"
            65 -> "A6"
            64 -> "A3"
            63 -> "A1"
            62 -> "9E"
            61 -> "9C"
            60 -> "99"
            59 -> "96"
            58 -> "94"
            57 -> "91"
            56 -> "8F"
            55 -> "8C"
            54 -> "8A"
            53 -> "87"
            52 -> "85"
            51 -> "82"
            50 -> "80"
            49 -> "7D"
            48 -> "7A"
            47 -> "78"
            46 -> "75"
            45 -> "73"
            44 -> "70"
            43 -> "6E"
            42 -> "6B"
            41 -> "69"
            40 -> "66"
            39 -> "63"
            38 -> "61"
            37 -> "5E"
            36 -> "5C"
            35 -> "59"
            34 -> "57"
            33 -> "54"
            32 -> "52"
            31 -> "4F"
            30 -> "4D"
            29 -> "4A"
            28 -> "47"
            27 -> "45"
            26 -> "42"
            25 -> "40"
            24 -> "3D"
            23 -> "3B"
            22 -> "38"
            21 -> "36"
            20 -> "33"
            19 -> "30"
            18 -> "2E"
            17 -> "2B"
            16 -> "29"
            15 -> "26"
            14 -> "24"
            13 -> "21"
            12 -> "1F"
            11 -> "1C"
            10 -> "1A"
            9 -> "17"
            8 -> "14"
            7 -> "12"
            6 -> "0F"
            5 -> "0D"
            4 -> "0A"
            3 -> "08"
            2 -> "05"
            1 -> "03"
            0 -> "00"
            else -> "00"
        }
    }

    private fun setUpOrientationChangeListener() {
        val mOrientationListener: OrientationEventListener = object : OrientationEventListener(
            context
        ) {
            override fun onOrientationChanged(orientation: Int) {


                if (camera_fragment_main_layout != null && change_camera_iv != null) {
                    when (orientation) {
                        0 -> {
                            imageCapture?.targetRotation = Surface.ROTATION_0
                        }

                        90 -> {
                            imageCapture?.targetRotation = Surface.ROTATION_270
                        }

                        180 -> {
                            imageCapture?.targetRotation = Surface.ROTATION_180
                        }

                        270 -> {
                            imageCapture?.targetRotation = Surface.ROTATION_90
                        }
                    }
                    if (orientation == 0 || orientation == 180) {
                        val animator = ValueAnimator.ofFloat(change_camera_iv.rotation, 0f)


                        animator.interpolator = OvershootInterpolator()
                        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                            override fun onAnimationUpdate(animation: ValueAnimator?) {
                                val newAngle = animation?.animatedValue as Float
                                if (camera_fragment_main_layout != null && change_camera_iv != null) {
                                    change_camera_iv.rotation = newAngle
                                }
                            }


                        })
                        animator.duration = 500
                        animator.start()

                        flashlight_icon?.rotation = 360 - orientation.toFloat()
                        previous_photo_iv?.rotation = 360 - orientation.toFloat()

                        if (lastRotation != orientation) {

                            video_recording_swipe_to_cancel_tv?.apply {
                                alpha = 0f
                                text = getSwipeToCancelTextFromCurrentRotation(360 - orientation)
                                animate()
                                    .alpha(1f)
                                    .duration = 300
                            }
                        }
                        lastRotation = orientation


                    } else if (orientation == 90 || orientation == 270) {
                        val animator = ValueAnimator.ofFloat(change_camera_iv.rotation, 90f)
                        animator.interpolator = OvershootInterpolator()
                        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                            override fun onAnimationUpdate(animation: ValueAnimator?) {
                                val newAngle = animation?.animatedValue as Float
                                change_camera_iv.rotation = newAngle
                            }


                        })
                        animator.duration = 500
                        animator.start()

                        previous_photo_iv.rotation = 360 - orientation.toFloat()
                        flashlight_icon.rotation = 360 - orientation.toFloat()
                        if (lastRotation != orientation) {
                            video_recording_swipe_to_cancel_tv.apply {
                                alpha = 0f
                                text = getSwipeToCancelTextFromCurrentRotation(360 - orientation)
                                animate()
                                    .alpha(1f)
                                    .duration = 300
                            }
                        }

                        lastRotation = orientation

                    }

                }
            }
        }

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        }
    }

    fun deleteFile(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(path)
                file.delete()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun vibrate() {
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
            } else vibe?.vibrate(100)
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

    private fun getSwipeToCancelTextFromCurrentRotation(currentRotation: Int): String {
        val swipeToCancelText: String = when (currentRotation) {
            0 -> context.resources.getString(R.string.defaultSwipeLeftToCancel)
            90 -> context.resources.getString(R.string.defaultSwipeBottomToCancel)
            180 -> context.resources.getString(R.string.defaultSwipeRightToCancel)
            270 -> context.resources.getString(R.string.defaultSwipeTopToCancel)
            else -> context.resources.getString(R.string.defaultSwipeLeftToCancel)
        }
        return swipeToCancelText
    }

    class AnalyzerM : ImageAnalysis.Analyzer {

        private var rotation: Int = 0

        fun getRotation(): Int {
            return rotation
        }

        override fun analyze(imageProxy: ImageProxy) {
            rotation = imageProxy.imageInfo.rotationDegrees
            Log.v("AnalyzerM", "rotation = $rotation")
        }


    }
}