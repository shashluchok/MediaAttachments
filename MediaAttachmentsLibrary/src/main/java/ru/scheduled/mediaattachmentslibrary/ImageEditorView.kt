package ru.scheduled.mediaattachmentslibrary

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.image_editor_view.view.*
import kotlinx.android.synthetic.main.layout_media_sketch.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs


class ImageEditorView : ConstraintLayout {

    private var isImageCropping = false
    private var currentRotation = 0
    private var fullSizeImageRect = Rect()

    private var onComplete: ((Bitmap,String)->Unit)? = null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    )
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    fun setImageText(text:String){
        image_note_et.setText(text)
    }

    fun setImageBitmap(bitmap: Bitmap){
        editable_photo_preview_civ.setImageBitmap(bitmap)
    }

    fun setOnCompleteCallback(callback:(edittedImage:Bitmap, textNote:String)->Unit){
        onComplete = callback
    }

    fun setOnCloseClickCallback(callback:()->Unit){
        editable_photo_close.setOnClickListener {
            callback.invoke()
        }
    }

    private fun initPhotoPreviewListeners() {

        editable_photo_preview_civ.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                editable_photo_preview_civ.viewTreeObserver.removeOnGlobalLayoutListener(this)
                editable_photo_preview_civ.apply {
                    maxZoom = 1
                    isAutoZoomEnabled = false
                    cropRect = editable_photo_preview_civ.wholeImageRect
                    isShowCropOverlay = false
                    setFixedAspectRatio(false)
                    scaleType = CropImageView.ScaleType.FIT_CENTER
                }

            }
        })
        editable_photo_crop_photo.setOnClickListener {
            isImageCropping = !isImageCropping
            setUpCropping(isCropping = isImageCropping)
        }

        editable_photo_accept.setOnClickListener {
            if (isImageCropping) {
                onComplete?.invoke(editable_photo_preview_civ.croppedImage,image_note_et.text?.toString()?:"")
            } else {
                editable_photo_preview_civ.cropRect = editable_photo_preview_civ.wholeImageRect
                onComplete?.invoke(editable_photo_preview_civ.croppedImage,image_note_et.text?.toString()?:"" )
            }
        }

        editable_photo_rotate_photo.setOnClickListener {
            currentRotation -= 90
            if (currentRotation == -360) currentRotation = 0
            editable_photo_preview_civ.rotateImage(-90)
        }



    }

    private fun setUpCropping(isCropping: Boolean) {
        editable_photo_preview_civ.apply {
            if (isCropping) {
                maxZoom = 2
                isAutoZoomEnabled = true
                cropRect = wholeImageRect
                setFixedAspectRatio(false)
                scaleType = CropImageView.ScaleType.FIT_CENTER

                setMinCropResultSize(400, 300)
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

    private fun hideKeyboard() {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.GONE
        }

    init {
        View.inflate(context, R.layout.image_editor_view, this)
        val keyListener = image_note_et.keyListener
        initPhotoPreviewListeners()
        image_note_touch_filter.setOnClickListener{
            image_note_et.requestFocus()
        }

        image_note_et.setUpOnKeyPreImePressedCallback {
            image_note_et.clearFocus()
            hideKeyboard()
        }

        image_note_ready_iv.setOnClickListener {
            image_note_et.clearFocus()
            hideKeyboard()
        }

        image_note_et.setOnFocusChangeListener { _, isFocused ->
            editable_photo_nav_bar.isVisible = !isFocused
            image_note_ready_iv.isVisible = isFocused
            image_note_touch_filter.isVisible = !isFocused
            if(!isFocused){
                image_note_et.apply {
                    this.keyListener = null
                    setHorizontallyScrolling(true)
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                }
            }
            else {
                image_note_et.apply {
                    this.keyListener = keyListener
                    isSingleLine = false
                    maxLines = 7
                    ellipsize = null
                    setHorizontallyScrolling(false)
                    setSelection(length())

                }
                val keyboard = (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }

    }


}