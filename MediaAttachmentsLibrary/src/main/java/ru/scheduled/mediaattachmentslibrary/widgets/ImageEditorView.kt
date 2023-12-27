package ru.scheduled.mediaattachmentslibrary.widgets

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import com.theartofdev.edmodo.cropper.CropImageView
import ru.scheduled.mediaattachmentslibrary.databinding.ImageEditorViewBinding
import ru.scheduled.mediaattachmentslibrary.utils.hideKeyboard
import ru.scheduled.mediaattachmentslibrary.utils.isVisible
import ru.scheduled.mediaattachmentslibrary.utils.showKeyboard

private val minCropSize = Size(400, 300)
private const val noteMaxLines = 7

class ImageEditorView : ConstraintLayout {

    private var binding: ImageEditorViewBinding =
        ImageEditorViewBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private var isImageCropping = false
    private var currentRotation = 0

    private var onComplete: ((Bitmap, String) -> Unit)? = null

    init {
        with(binding) {

            initPhotoPreviewListeners()

            imageNoteEt.setUpOnKeyPreImePressedCallback {
                imageNoteEt.clearFocus()
                hideKeyboard()
            }

            imageNoteReadyIv.setOnClickListener {
                imageNoteEt.clearFocus()
                hideKeyboard()
                it.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({ it.isEnabled = true }, 200)

            }

            val keyListener = imageNoteEt.keyListener
            imageNoteEt.setOnFocusChangeListener { _, isFocused ->
                imageNoteReadyIv.isVisible = isFocused
                if (!isFocused) {
                    imageNoteEt.apply {
                        this.keyListener = null
                        setHorizontallyScrolling(true)
                        isSingleLine = true
                        ellipsize = TextUtils.TruncateAt.END
                    }
                } else {
                    imageNoteEt.apply {
                        this.keyListener = keyListener
                        isSingleLine = false
                        maxLines = noteMaxLines
                        ellipsize = null
                        setHorizontallyScrolling(false)
                        setSelection(length())

                    }
                    showKeyboard()
                }
            }

        }
    }

    fun setImageText(text: String) {
        binding.imageNoteEt.setText(text)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        binding.editablePhotoPreviewCiv.setImageBitmap(bitmap)
    }

    fun setOnCompleteCallback(callback: (editedImage: Bitmap, textNote: String) -> Unit) {
        onComplete = callback
    }

    fun setOnCloseClickCallback(callback: () -> Unit) {
        binding.editablePhotoClose.setOnClickListener {
            callback.invoke()
        }
    }

    private fun initPhotoPreviewListeners() {
        with(binding) {

            editablePhotoPreviewCiv.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    editablePhotoPreviewCiv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    editablePhotoPreviewCiv.apply {
                        maxZoom = 1
                        isAutoZoomEnabled = false
                        cropRect = editablePhotoPreviewCiv.wholeImageRect
                        isShowCropOverlay = false
                        setFixedAspectRatio(false)
                        scaleType = CropImageView.ScaleType.FIT_CENTER
                    }

                }
            })
            editablePhotoCropPhoto.setOnClickListener {
                isImageCropping = !isImageCropping
                setUpCropping(isCropping = isImageCropping)
            }

            editablePhotoAccept.setOnClickListener {
                if (isImageCropping) {
                    onComplete?.invoke(
                        editablePhotoPreviewCiv.croppedImage,
                        imageNoteEt.text?.toString() ?: "".trim()
                    )
                } else {
                    onComplete?.invoke(
                        editablePhotoPreviewCiv.croppedImage,
                        imageNoteEt.text?.toString() ?: "".trim()
                    )
                }
            }

            editablePhotoRotatePhoto.setOnClickListener {
                if (!isImageCropping) {
                    editablePhotoPreviewCiv.apply {
                        maxZoom = 2
                        isAutoZoomEnabled = true
                        cropRect = wholeImageRect
                        setFixedAspectRatio(false)
                        scaleType = CropImageView.ScaleType.FIT_CENTER
                        setMinCropResultSize(minCropSize.width, minCropSize.height)
                    }
                }
                currentRotation -= 90
                if (currentRotation == -360) currentRotation = 0
                editablePhotoPreviewCiv.rotateImage(-90)
            }

        }
    }

    private fun setUpCropping(isCropping: Boolean) {
        with(binding) {

            editablePhotoPreviewCiv.apply {
                if (isCropping) {
                    maxZoom = 2
                    isAutoZoomEnabled = true
                    cropRect = wholeImageRect
                    setFixedAspectRatio(false)
                    scaleType = CropImageView.ScaleType.FIT_CENTER
                    setMinCropResultSize(minCropSize.width, minCropSize.height)

                    isShowCropOverlay = true
                } else {
                    maxZoom = 1
                    isAutoZoomEnabled = false
                    cropRect = wholeImageRect
                    isShowCropOverlay = false
                    setFixedAspectRatio(false)
                    scaleType = CropImageView.ScaleType.FIT_CENTER
                }
            }

        }
    }
}