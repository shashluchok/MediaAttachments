package ru.mediaattachments.presentation.widgets.cameracapture

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Size
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.*
import ru.mediaattachments.databinding.CameraCaptureViewBinding
import ru.mediaattachments.utils.FileUtils
import ru.mediaattachments.utils.saveBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Random
import kotlin.math.abs

private const val dateFormat = "HHmmssddMMyyyy"

private const val imageExtension = ".jpg"
private const val imageMimeType = "image/jpeg"
private const val imageFolderPath = "Images/MediaAttachments"
private const val imageQuality = 90

private const val mockedDataAnimationDuration = 300L
private const val mockedImagesPath = "photos"

private const val normalDelay = 300L
private const val longDelay = 1000L

private const val orientationAnimationDuration = 500L
private const val flashDuration = 1000L

class CameraCaptureView : ConstraintLayout {

    private var binding: CameraCaptureViewBinding =
        CameraCaptureViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var cam: Camera? = null
    private var backCamera = CameraSelector.DEFAULT_BACK_CAMERA
    private var frontCamera = CameraSelector.DEFAULT_FRONT_CAMERA
    private var isBackCameraOn = false
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var animJob: Job? = null

    private var initCenterX = 0f
    private var initCenterY = 0f
    private var initScale = 0f

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

    init {
        with(binding) {

            if (isEmulator()) {
                previousPhotoContainer.isVisible = false
                changeCameraIv.isVisible = false
                previewView.isVisible = false
                mockedCl.isVisible = true
                notClickableFilterView.visibility = VISIBLE
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

                photoCaptureIcon.setOnClickListener {
                    stopMoves()
                    notClickableFilterView.visibility = VISIBLE
                    GlobalScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            blurView.visibility = VISIBLE
                            blurView.alpha = 1f
                        }

                        withContext(Dispatchers.Main) {
                            mockedDataIv.apply {
                                animate()
                                    .scaleX(1.15f)
                                    .scaleY(1.15f)
                                    .duration = mockedDataAnimationDuration
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
                                    .duration = mockedDataAnimationDuration
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
                        delay(normalDelay)

                        withContext(Dispatchers.Main) {
                            takeEmulatedPhoto()
                            blurView.visibility = GONE
                            notClickableFilterView.visibility = GONE
                        }

                    }

                }

            } else {
                setUpOrientationChangeListener()
                setUpCamera()
                setLastGalleryImage()
            }

        }
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

    fun setOnGalleryClickedCallback(callback: () -> Unit) {
        with(binding) {

            Handler(Looper.getMainLooper()).postDelayed({
                previousPhotoContainer.setOnClickListener {
                    if (previousPhotoIv.visibility == VISIBLE) {
                        callback.invoke()
                    }
                }
            }, normalDelay)

        }
    }

    fun setOnCloseClickedCallback(callback: () -> Unit) {
        binding.closeContainer.setOnClickListener {
            callback.invoke()
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

    private fun takeEmulatedPhoto() {
        with(binding) {

            flash()
            mockedCl.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (mockedCl.height != 0 && mockedCl.width != 0) {
                        mockedCl.viewTreeObserver.removeOnGlobalLayoutListener(
                            this
                        )
                        val bitmap = setViewToBitmapImage(mockedCl) ?: return
                        val photoFile =
                            FileUtils.createFile(context, fileExtension = FileUtils.Extension.JPEG)

                        GlobalScope.launch(Dispatchers.IO) {
                            photoFile.saveBitmap(bitmap)
                            withContext(Dispatchers.Main) {
                                onImageSaved?.invoke(Uri.fromFile(photoFile))
                            }
                        }

                    }
                }
            })

        }

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
        with(binding) {

            val decorView = cameraFragmentMainLayout
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
                delay(longDelay)
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
                    blurView.visibility = GONE
                    blurView.alpha = 1f
                    blurView.setBlurEnabled(true)
                    blurView.setBlurRadius((1).toFloat())
                }

                delay(normalDelay)

                withContext(Dispatchers.Main) {
                    blurView.setBlurEnabled(false)

                }
                delay(longDelay)
                withContext(Dispatchers.Main) {
                    notClickableFilterView.visibility = GONE
                }
            }

        }
    }

    @SuppressLint("Range")
    private fun setLastGalleryImage() {
        with(binding) {

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
                    if (imageCursor.moveToNext()) {
                        lastImageUri =
                            imageCursor.getString(
                                imageCursor.getColumnIndex(
                                    MediaStore.Images.Media.DATA
                                )
                            )

                    }
                }

                if (!lastImageUri.isNullOrEmpty()) {
                    previousPhotoNoPhotoIv.visibility = GONE
                    previousPhotoIv.visibility = VISIBLE
                    Glide.with(this@CameraCaptureView).load(lastImageUri)
                        .transition(DrawableTransitionOptions.withCrossFade(175))
                        .into(previousPhotoIv)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                if (imageCursor != null && !imageCursor.isClosed) {
                    imageCursor.close();
                }
            }

        }
    }

    private fun setUpCamera() {
        CoroutineScope(Dispatchers.Default).launch {
            delay(normalDelay)
            withContext(Dispatchers.Main) {
                initListeners()
                if (checkCameraPermissionGranted()) {
                    startCamera(backCamera)
                    isBackCameraOn = true
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        with(binding) {
            changeCameraIv.setOnClickListener {
                if (checkCameraPermissionGranted()) {
                    isBackCameraOn = if (isBackCameraOn) {
                        startCamera(frontCamera)
                        false
                    } else {
                        startCamera(backCamera)
                        true
                    }
                }
            }


            val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    cam?.let {
                        val scale =
                            (it.cameraInfo.zoomState.value?.zoomRatio ?: 1F) * detector.scaleFactor
                        it.cameraControl.setZoomRatio(scale)
                    }
                    return true
                }
            }

            val scaleGestureDetector = ScaleGestureDetector(context, listener)
            previewView.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener false
            }

            if (isEmulator().not()) {
                photoCaptureIcon.setOnClickListener {
                    onPhotoClicked?.invoke()
                    takePhoto()
                }
            }
        }
    }

    private fun takePhoto() {
        with(binding) {

            val imageCapture = imageCapture ?: return

            val metadata = ImageCapture.Metadata()

            val bitmap = previewView.bitmap
            photoPreviewIv.visibility = VISIBLE
            Glide.with(context).load(bitmap).into(photoPreviewIv)
            previewView.visibility = GONE
            val photoFile = FileUtils.createFile(context, fileExtension = FileUtils.Extension.JPEG)
            if (!isBackCameraOn) {
                metadata.isReversedHorizontal = true
            }
            val outputOptions: ImageCapture.OutputFileOptions

            if (saveDestination == SaveLocation.GALLERY) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val contentValues = ContentValues().apply {
                        val name = SimpleDateFormat(
                            dateFormat,
                            Locale.US
                        ).format(System.currentTimeMillis()) + imageExtension
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, imageMimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, imageFolderPath)
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
                        SimpleDateFormat(dateFormat, Locale.US).format(
                            System.currentTimeMillis()
                        )
                    )
                    values.put(
                        MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat(
                            dateFormat,
                            Locale.US
                        ).format(System.currentTimeMillis())
                    )
                    values.put(
                        MediaStore.Images.Media.DESCRIPTION, SimpleDateFormat(
                            dateFormat,
                            Locale.US
                        ).format(System.currentTimeMillis())
                    )
                    values.put(MediaStore.Images.Media.MIME_TYPE, imageMimeType)
                    values.put(
                        MediaStore.Images.Media.DATE_ADDED,
                        System.currentTimeMillis() / 1000
                    )

                    outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ).setMetadata(
                        metadata
                    ).build()
                }

            } else {
                outputOptions = ImageCapture.OutputFileOptions.Builder(
                    photoFile
                ).setMetadata(
                    metadata
                ).build()

            }

            if (!isBackCameraOn) {
                val bitmap = previewView.bitmap
                val fos = FileOutputStream(photoFile)
                val stream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
                val imageBytes = stream.toByteArray()
                fos.use {
                    it.write(imageBytes)
                }
                onImageSaved?.invoke(Uri.fromFile(photoFile))
            } else {
                var isProcessing = false
                GlobalScope.launch(Dispatchers.IO) {
                    var mbitmap: Bitmap?
                    withContext(Dispatchers.Main) {
                        mbitmap = previewView.bitmap
                    }
                    while (!isProcessing) {
                        delay(100)
                    }
                    withContext(Dispatchers.Main) {
                        launch(Dispatchers.IO) {
                            mbitmap?.let {
                                photoFile.saveBitmap(it)
                            }
                            withContext(Dispatchers.Main) {
                                onImageSaved?.invoke(Uri.fromFile(photoFile))
                            }
                        }
                    }
                }

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            isProcessing = true
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                        }
                    })
            }

        }
    }

    private fun startCamera(cameraSelector: CameraSelector) {
        with(binding) {

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener(Runnable {

                cameraProvider = cameraProviderFuture.get()
                if (cameraProvider != null) {
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    if (!cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        changeCameraIv.visibility = GONE
                    }
                    cameraProvider!!.unbindAll()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(previewView.width, previewView.height))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    cam = cameraProvider!!.bindToLifecycle(
                        context as AppCompatActivity,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                        imageCapture
                    )
                }
            }, ContextCompat.getMainExecutor(context))

        }
    }

    private fun checkCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setUpOrientationChangeListener() {
        with(binding) {

            val mOrientationListener: OrientationEventListener = object : OrientationEventListener(
                context
            ) {
                override fun onOrientationChanged(orientation: Int) {

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
                        val animator = ValueAnimator.ofFloat(changeCameraIv.rotation, 0f)


                        animator.interpolator = OvershootInterpolator()
                        animator.addUpdateListener { animation ->
                            changeCameraIv.rotation = animation?.animatedValue as Float
                        }
                        animator.duration = orientationAnimationDuration
                        animator.start()

                        previousPhotoIv.rotation = 360 - orientation.toFloat()

                    } else if (orientation == 90 || orientation == 270) {
                        val animator = ValueAnimator.ofFloat(changeCameraIv.rotation, 90f)
                        animator.interpolator = OvershootInterpolator()
                        animator.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation ->
                            val newAngle = animation?.animatedValue as Float
                            changeCameraIv.rotation = newAngle
                        })
                        animator.duration = orientationAnimationDuration
                        animator.start()

                        previousPhotoIv.rotation = 360 - orientation.toFloat()
                    }

                }
            }

            if (mOrientationListener.canDetectOrientation()) {
                mOrientationListener.enable()
            }

        }
    }

    private fun stopMoves() {
        animJob?.cancel()
        animJob = null
    }

    private fun flash() {
        with(binding) {

            photoFlashView.visibility = VISIBLE
            photoFlashView.apply {
                alpha = 1f
                animate().alpha(0f).duration = flashDuration
            }
            Handler(Looper.getMainLooper()).postDelayed({
                photoFlashView.visibility = GONE
            }, flashDuration)

        }
    }

    fun isEmulator(): Boolean {
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

}