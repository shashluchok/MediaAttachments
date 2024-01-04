package ru.mediaattachments.presentation.widgets.imageeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.theartofdev.edmodo.cropper.CropImageView
import ru.mediaattachments.databinding.ImageEditorViewBinding
import ru.mediaattachments.utils.hideKeyboard
import ru.mediaattachments.utils.showKeyboard

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

            imageNoteEt.apply {
                setHorizontallyScrolling(true)
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                setUpOnKeyPreImePressedCallback {
                    imageNoteEt.clearFocus()
                    hideKeyboard()
                }
                clearFocus()
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
                if (!isImageCropping) {
                    editablePhotoPreviewCiv.cropRect = Rect(0, 0, Int.MAX_VALUE, Int.MAX_VALUE)
                }
                onComplete?.invoke(
                    editablePhotoPreviewCiv.croppedImage,
                    imageNoteEt.text?.toString() ?: "".trim()
                )
            }

            editablePhotoRotatePhoto.setOnClickListener {
                currentRotation -= 90
                if (currentRotation == -360) currentRotation = 0
                editablePhotoPreviewCiv.rotateImage(-90)
            }

        }
    }

    private fun setUpCropping(isCropping: Boolean) {
        binding.editablePhotoPreviewCiv.isShowCropOverlay = isCropping
    }
}