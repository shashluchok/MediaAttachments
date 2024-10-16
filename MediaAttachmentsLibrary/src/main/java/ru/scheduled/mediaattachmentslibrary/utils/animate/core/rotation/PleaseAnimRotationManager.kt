package ru.scheduled.mediaattachmentslibrary.utils.animate.core.rotation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.ViewCalculator
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.AnimExpectation
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.PleaseAnimManager

class PleaseAnimRotationManager(
    animExpectations: List<AnimExpectation>,
    viewToMove: View, viewCalculator: ViewCalculator
) : PleaseAnimManager(animExpectations, viewToMove, viewCalculator) {

    var rotation: Float? = null
        private set

    var rotationX: Float? = null
        private set

    var rotationY: Float? = null
        private set

    override fun calculate() {
        animExpectations.forEach { expectation ->
            if (expectation is RotationExpectation) {
                expectation.getCalculatedRotation(viewToMove)?.let {
                    this.rotation = it
                }
                expectation.getCalculatedRotationX(viewToMove)?.let {
                    this.rotationX = it
                }

                expectation.getCalculatedRotationY(viewToMove)?.let {
                    this.rotationY = it
                }
            }
        }
    }

    override fun getAnimators(): List<Animator> {
        val animations = mutableListOf<Animator>()

        calculate()

        rotation?.let {
            animations.add(ObjectAnimator.ofFloat(viewToMove, View.ROTATION, it))
        }
        rotationX?.let {
            animations.add(ObjectAnimator.ofFloat(viewToMove, View.ROTATION_X, it))
        }
        rotationY?.let {
            animations.add(ObjectAnimator.ofFloat(viewToMove, View.ROTATION_Y, it))
        }

        return animations
    }
}
