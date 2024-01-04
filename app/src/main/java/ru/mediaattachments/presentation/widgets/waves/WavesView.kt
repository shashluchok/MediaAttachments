package ru.mediaattachments.presentation.widgets.waves

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import ru.mediaattachments.databinding.LayoutWavesBinding
import kotlin.math.sqrt

class WavesView(context: Context, attrs: AttributeSet, defStyle: Int) :
    ConstraintLayout(context, attrs, defStyle) {

    private var binding: LayoutWavesBinding? =
        LayoutWavesBinding.inflate(LayoutInflater.from(context), this, false)

    private lateinit var matrixFirst: Matrix
    private lateinit var matrixSecond: Matrix
    private lateinit var matrixThird: Matrix
    private lateinit var matrixFourth: Matrix

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        initView()
    }

    private fun initView() {
        addView(binding?.root)
        setUpWorms()
    }

    private fun setUpWorms() {
        matrixFirst = Matrix()
        matrixSecond = Matrix()
        matrixThird = Matrix()
        matrixFourth = Matrix()

        binding?.apply {

            root.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    toTheCenter(waveFirst, matrixFirst)
                    toTheCenter(waveSecond, matrixSecond)
                    toTheCenter(waveThird, matrixThird)
                    toTheCenter(waveFourth, matrixFourth)
                    runWormLinesAnimation(waveFirst, matrixFirst)
                    runWormLinesAnimation(waveSecond, matrixSecond)
                    runWormLinesAnimation(waveThird, matrixThird)
                    runWormLinesAnimation(waveFourth, matrixFourth)
                }
            })

        }
    }

    private fun runWormLinesAnimation(view: ImageView, viewMatrix: Matrix) {
        binding?.apply {

            val oldCenter = getCurrentCenter(view)
            val center = Pair(
                oldCenter.first + getCurrentTx(viewMatrix),
                oldCenter.second + getCurrentTy(viewMatrix)
            )


            val handler = Handler(Looper.getMainLooper())
            val savedMatrix = Matrix()
            savedMatrix.set(viewMatrix)


            val scaleConstraint: Float
            val angleFirst: Float
            val angleSecond: Float

            if (view == waveFirst) {
                scaleConstraint = 0.99f
                angleFirst = 360f
                angleSecond = 350f
            } else if (view == waveSecond) {
                scaleConstraint = 0.98f
                angleFirst = 0f
                angleSecond = 10f
            } else if (view == waveThird) {
                scaleConstraint = 0.97f
                angleFirst = 360f
                angleSecond = 350f
            } else {
                scaleConstraint = 0.96f
                angleFirst = 0f
                angleSecond = 10f
            }
            val angle = PropertyValuesHolder.ofFloat("angle", angleFirst, angleSecond)
            var scale =
                PropertyValuesHolder.ofFloat("scale", 1f, scaleConstraint)

            handler.post(object : Runnable {
                override fun run() {

                    if (calculateCurrentScale(viewMatrix) < 1) {
                        scale = PropertyValuesHolder.ofFloat(
                            "scale",
                            1f,
                            1 / calculateCurrentScale(viewMatrix)
                        )
                    } else if (calculateCurrentScale(viewMatrix) >= 1) {
                        scale =
                            PropertyValuesHolder.ofFloat("scale", 1f, scaleConstraint)
                    }

                    val animation = ValueAnimator.ofPropertyValuesHolder(angle, scale)
                    animation.interpolator = LinearInterpolator()
                    animation.addUpdateListener { _ ->
                        viewMatrix.set(savedMatrix)
                        val animatedAngle = animation.getAnimatedValue("angle") as Float
                        viewMatrix.postRotate(animatedAngle, center.first, center.second)
                        view.imageMatrix = viewMatrix
                    }
                    animation.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            savedMatrix.set(viewMatrix)
                        }
                    })
                    animation.duration = 2499
                    animation.start()
                    handler.postDelayed(this, 2500)
                }
            })

        }
    }

    private fun toTheCenter(view: ImageView, imageViewMatrix: Matrix) {
        translateXY(view, imageViewMatrix)
        view.imageMatrix = imageViewMatrix
    }

    private fun getCurrentTx(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MTRANS_X]
    }

    private fun getCurrentTy(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MTRANS_Y]
    }

    private fun calculateCurrentScale(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return sqrt(v[Matrix.MSCALE_X] * v[Matrix.MSCALE_X].toDouble() + v[Matrix.MSKEW_Y] * v[Matrix.MSKEW_Y])
            .toFloat()
    }

    private fun getCurrentCenter(view: ImageView): Pair<Float, Float> {
        val containerCenterX = view.drawable.intrinsicWidth / 2.toFloat()
        val containerCenterY = view.drawable.intrinsicHeight / 2.toFloat()
        return Pair(containerCenterX, containerCenterY)
    }

    private fun translateXY(view: ImageView, viewMatrix: Matrix) {
        binding?.apply {

            if (view == waveFirst) {
                val center: Pair<Float, Float> = getCurrentCenter(view)
                viewMatrix.postTranslate(
                    0 - center.first,
                    root.resources.displayMetrics.heightPixels - center.second
                )
                view.imageMatrix = viewMatrix
            }
            if (view == waveSecond) {
                val center: Pair<Float, Float> = getCurrentCenter(view)
                viewMatrix.postTranslate(
                    root.resources.displayMetrics.widthPixels - center.first,
                    0 - center.second
                )
                view.imageMatrix = viewMatrix
            }
            if (view == waveThird) {
                val center: Pair<Float, Float> = getCurrentCenter(view)
                viewMatrix.postTranslate(
                    root.resources.displayMetrics.widthPixels / 2 - center.first - center.first / 2,
                    root.resources.displayMetrics.heightPixels / 2 - center.second + center.second / 2
                )
                view.imageMatrix = viewMatrix
            }

        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding = null
    }
}