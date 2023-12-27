package ru.scheduled.mediaattachmentslibrary.widgets.sketch

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import ru.scheduled.mediaattachmentslibrary.R
import ru.scheduled.mediaattachmentslibrary.databinding.LayoutMediaSketchBinding


class SketchDrawingView : ConstraintLayout {

    private var binding: LayoutMediaSketchBinding =
        LayoutMediaSketchBinding.inflate(LayoutInflater.from(context), this, true)

    private var activeColor: Int = R.color.defaultActive
    private var disabledColor: Int = R.color.defaultNotActive
    private var isEraserEnabled = false

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {

        activeColor = attrs.getAttributeResourceValue(
            "http://schemas.android.com/apk/res-auto",
            "activeColor",
            R.color.defaultActive
        )
        disabledColor = attrs.getAttributeResourceValue(
            "http://schemas.android.com/apk/res-auto",
            "disabledColor",
            R.color.defaultNotActive
        )


        val theme = context.theme.obtainStyledAttributes(
            attrs, R.styleable.SketchDrawingView, 0, 0
        )
        try {
            with(binding) {
                sketchView.setLineWidth(
                    theme.getDimensionPixelSize(R.styleable.SketchDrawingView_penLineWidth, 4)
                )
                sketchView.setEraserWidth(
                    theme.getDimensionPixelSize(R.styleable.SketchDrawingView_eraserLineWidth, 120)
                )
            }
        } finally {
            theme.recycle();
        }

    }

    init {
        binding.sketchView.setOnDeleteLastCallback {
            isEraserEnabled = false
            onEraserEnabled(isEraserEnabled)
        }
        enableDrawBack(false)
        enableDrawForward(false)
        onEraserEnabled(isEnabled = false)

    }

    fun setOnEmptyCallback(callback: (isEmpty: Boolean) -> Unit) {
        binding.sketchView.setOnEmptyCallback(callback)
    }

    fun wasAnythingDrawn(): Boolean {
        return binding.sketchView.wasAnythingDrawn()
    }

    fun setEditingToolbarVisibility(isVisible: Boolean) {
        binding.mediaSketchBottomToolbar.visibility = if (isVisible) View.VISIBLE else View.GONE
        if (isVisible) setEditingToolbarClickListeners()
    }

    fun setExistingSketch(byteArray: ByteArray) {
        binding.sketchView.apply {
            reInit()
            setExistingSketchByteArray(byteArray)

        }
    }

    fun setOnFirstTouchCallback(callback: () -> Unit) {
        binding.sketchView.setOnFirstTouchEventAction {
            callback.invoke()
        }
    }

    fun getSketchByteArray(): ByteArray? {
        return binding.sketchView.getSketchByteArray()
    }

    private fun setEditingToolbarClickListeners() {
        with(binding) {

            sketchView.apply {
                onHasUnDoStack {
                    enableDrawBack(it)
                }
                onHasReDoStack {
                    enableDrawForward(it)
                }
            }

            mediaSketchDrawBack.setOnClickListener {
                binding.sketchView.drawBack()
            }
            mediaSketchDrawForward.setOnClickListener {
                binding.sketchView.drawForward()
            }

            mediaSketchEraser.setOnClickListener {
                if (!binding.sketchView.isEmpty()) {
                    if (!isEraserEnabled) {
                        isEraserEnabled = true
                        onEraserEnabled(isEraserEnabled)
                    }
                }

            }

            mediaSketchPen.setOnClickListener {
                if (isEraserEnabled) {
                    isEraserEnabled = false
                    onEraserEnabled(isEraserEnabled)
                }
            }

        }
    }

    private fun onEraserEnabled(isEnabled: Boolean) {
        with(binding) {
            mediaSketchEraserIv.imageTintList = null
            mediaSketchPenIv.imageTintList = null

            val penColor = if (isEnabled) disabledColor else activeColor
            val eraserColor = if (isEnabled) activeColor else disabledColor

            ImageViewCompat.setImageTintList(
                mediaSketchPenIv, ColorStateList.valueOf(
                    ContextCompat.getColor(context, penColor)
                )
            )

            ImageViewCompat.setImageTintList(
                mediaSketchEraserIv, ColorStateList.valueOf(
                    ContextCompat.getColor(context, eraserColor)
                )
            )
            sketchView.turnEraserMode(isEnabled)
        }
    }

    private fun enableDrawBack(on: Boolean) {
        with(binding) {

            mediaSketchDrawBackIv.imageTintList = null
            if (on) {
                ImageViewCompat.setImageTintList(
                    mediaSketchDrawBackIv, ColorStateList.valueOf(
                        ContextCompat.getColor(context, activeColor)
                    )
                )
            } else {
                ImageViewCompat.setImageTintList(
                    mediaSketchDrawBackIv, ColorStateList.valueOf(
                        resources.getColor(R.color.defaultTextLight)
                    )
                )
            }

        }
    }

    private fun enableDrawForward(on: Boolean) {
        with(binding) {

            mediaSketchDrawForwardIv.imageTintList = null
            if (on) {
                ImageViewCompat.setImageTintList(
                    mediaSketchDrawForwardIv, ColorStateList.valueOf(
                        ContextCompat.getColor(context, activeColor)
                    )
                )
            } else {
                ImageViewCompat.setImageTintList(
                    mediaSketchDrawForwardIv, ColorStateList.valueOf(
                        resources.getColor(R.color.defaultTextLight)
                    )
                )
            }

        }
    }

}
