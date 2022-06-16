package ru.scheduled.mediaattachmentslibrary

import android.content.res.Resources
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.layout_tooltip.view.*
import kotlinx.coroutines.*
import java.time.Duration


class ToolTip(builder: Builder) {

    private var anchorView: View? = builder.anchorView
    private var rootView: ViewGroup? = builder.rootView
    private var arrowPosition: ArrowPosition = builder.arrowPosition
    private var message: String = builder.message
    private var duration: Long = builder.duration
    private var margin: Float = builder.margin

    private var animJob: Job? = null

    private var toolTipView:View? = null

    enum class ArrowPosition {
        TOP_LEFT, TOP_RIGHT, TOP_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER
    }

    fun hide(){
        animJob?.cancel()
        animJob = null
        toolTipView?.apply {
            toolTipMainCl.animate().alpha(0f).duration = 150
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                this@ToolTip.rootView?.removeView(toolTipMainCl)
                toolTipView = null
            }, 200)
        }
    }

    fun show() {
        if(toolTipView!=null) return
        rootView?.let { rootView ->
            animJob?.cancel()
            animJob = null
            toolTipView = LayoutInflater.from(rootView.context).inflate(R.layout.layout_tooltip,null)

            toolTipView?.apply {

                toolTipTextTv.text = message

                val constraintSet = ConstraintSet()
                constraintSet.clone(toolTipMainCl)

                when (arrowPosition) {
                    ArrowPosition.TOP_LEFT, ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_CENTER -> {
                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.START,
                            R.id.toolTipMainCl,
                            ConstraintSet.START,
                        )

                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.END,
                            R.id.toolTipMainCl,
                            ConstraintSet.END,
                        )

                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.TOP,
                            R.id.tooltipArrowIv,
                            ConstraintSet.BOTTOM,
                        )

                        if (arrowPosition == ArrowPosition.TOP_LEFT) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.START,
                                R.id.toolTipMainCl,
                                ConstraintSet.START,
                                8.toPx().toInt().toInt()
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.TOP,
                                R.id.toolTipMainCl,
                                ConstraintSet.TOP
                            )
                        }
                        if (arrowPosition == ArrowPosition.TOP_RIGHT) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.END,
                                R.id.toolTipMainCl,
                                ConstraintSet.END,
                                8.toPx().toInt()
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.TOP,
                                R.id.toolTipMainCl,
                                ConstraintSet.TOP
                            )
                        }
                        if (arrowPosition == ArrowPosition.TOP_CENTER) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.START,
                                R.id.toolTipMainCl,
                                ConstraintSet.START
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.TOP,
                                R.id.toolTipMainCl,
                                ConstraintSet.TOP
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.END,
                                R.id.toolTipMainCl,
                                ConstraintSet.END
                            )
                        }

                        constraintSet.applyTo(toolTipMainCl)

                    }
                    ArrowPosition.BOTTOM_LEFT, ArrowPosition.BOTTOM_RIGHT, ArrowPosition.BOTTOM_CENTER -> {

                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.START,
                            R.id.toolTipMainCl,
                            ConstraintSet.START,
                        )

                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.END,
                            R.id.toolTipMainCl,
                            ConstraintSet.END,
                        )

                        constraintSet.connect(
                            R.id.toolTipBodyCl,
                            ConstraintSet.BOTTOM,
                            R.id.tooltipArrowIv,
                            ConstraintSet.TOP,
                        )
                        if (arrowPosition == ArrowPosition.BOTTOM_LEFT) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.START,
                                R.id.toolTipMainCl,
                                ConstraintSet.START,
                                8.toPx().toInt()
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.BOTTOM,
                                R.id.toolTipMainCl,
                                ConstraintSet.BOTTOM
                            )
                        }
                        if (arrowPosition == ArrowPosition.BOTTOM_CENTER) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.START,
                                R.id.toolTipMainCl,
                                ConstraintSet.START,
                                8.toPx().toInt()
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.BOTTOM,
                                R.id.toolTipMainCl,
                                ConstraintSet.BOTTOM
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.END,
                                R.id.toolTipMainCl,
                                ConstraintSet.END,
                                8.toPx().toInt()
                            )
                        }
                        if (arrowPosition == ArrowPosition.BOTTOM_RIGHT) {
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.END,
                                R.id.toolTipMainCl,
                                ConstraintSet.END,
                                8.toPx().toInt()
                            )
                            constraintSet.connect(
                                R.id.tooltipArrowIv,
                                ConstraintSet.BOTTOM,
                                R.id.toolTipMainCl,
                                ConstraintSet.BOTTOM
                            )
                        }
                        constraintSet.applyTo(toolTipMainCl)

                        tooltipArrowIv.rotation = 180f


                    }


                }
                rootView.addView(toolTipMainCl)
                toolTipMainCl.also {
                    it.layoutParams.apply {
                        width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                        height =ConstraintLayout.LayoutParams.WRAP_CONTENT
                    }
                }
                toolTipMainCl.alpha = 0f

                animJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(duration)
                    if(isActive)
                    withContext(Dispatchers.Main){
                        hide()
                    }
                }

                tooltipCloseIv.setOnClickListener {
                    hide()
                }

                anchorView?.let {

                    it.viewTreeObserver?.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (toolTipMainCl.width != 0 && toolTipMainCl.height != 0) {
                                it.viewTreeObserver?.removeOnGlobalLayoutListener(
                                    this
                                )
                                val anchorViewLocation = IntArray(2)
                                it.getLocationOnScreen(anchorViewLocation)
                                val anchorViewX = anchorViewLocation[0]

                                val anchorCenterX = anchorViewX + it.width / 2


                                toolTipMainCl.apply {

                                    val rootY = when (arrowPosition) {
                                        ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_CENTER, ArrowPosition.TOP_LEFT -> {
                                            it.bottom + margin
                                        }
                                        else -> {
                                            anchorViewLocation[1] - margin - toolTipMainCl.height
                                        }
                                    }

                                    y = rootY
                                    x =
                                        (anchorCenterX.toFloat() - (toolTipMainCl.x + tooltipArrowIv.x + tooltipArrowIv.width / 2))
                                }
                                toolTipMainCl.animate().alpha(1f).duration = 150

                            }


                        }
                    })

                }


            }


        }
    }

    class Builder() {
        var anchorView: View? = null
        var rootView: ViewGroup? = null
        var arrowPosition: ToolTip.ArrowPosition = ToolTip.ArrowPosition.TOP_RIGHT
        var message: String = ""
        var duration:Long = 10000L
        var margin:Float = 0f
        fun setUpViews(targetView: View, containerView: ViewGroup): Builder {
            this.anchorView = targetView
            this.rootView = containerView
            return this
        }

        fun setUpArrowPosition(arrowPosition: ToolTip.ArrowPosition): Builder {
            this.arrowPosition = arrowPosition
            return this
        }

        fun setUpText(text: String): Builder {
            this.message = text
            return this
        }

        fun setUpDuration(duration: Long):Builder{
            this.duration = duration
            return this
        }

        fun setMargin(margin: Float):Builder{
            this.margin = margin
            return this
        }

        fun build(): ToolTip {
            return ToolTip(this)
        }
    }

    private  fun Int.toPx():Float {
        return this *  Resources.getSystem().displayMetrics.density
    }

}
