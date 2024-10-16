package ru.mediaattachments.utils.animate.core.anim3d

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Build
import android.view.View

import ru.mediaattachments.utils.animate.ViewCalculator
import ru.mediaattachments.utils.animate.core.AnimExpectation
import ru.mediaattachments.utils.animate.core.PleaseAnimManager

class PleaseAnimCameraDistanceManager(
    animExpectations: List<AnimExpectation>, viewToMove: View,
    viewCalculator: ViewCalculator
) : PleaseAnimManager(animExpectations, viewToMove, viewCalculator) {

    private var mCurrentCameraDistance: Float? = null
    var cameraDistance: Float? = null
        private set

    override fun calculate() {
        animExpectations.forEach { expectation ->
            if (expectation is CameraDistanceExpectation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mCurrentCameraDistance = viewToMove.cameraDistance
                }
                expectation.getCalculatedCameraDistance(viewToMove)?.let { cameraDistance ->
                    this.cameraDistance = cameraDistance
                }
            }
        }
    }

    override fun getAnimators(): List<Animator> {
        val animations = mutableListOf<Animator>()
        calculate()
        if (cameraDistance != null && mCurrentCameraDistance != null) {
            val animator = ValueAnimator.ofFloat(mCurrentCameraDistance!!, cameraDistance!!)
            animator.addUpdateListener { valueAnimator -> viewToMove.cameraDistance = valueAnimator.animatedValue as Float }
            animations.add(animator)
        }
        return animations
    }

}
