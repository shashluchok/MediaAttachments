package ru.scheduled.mediaattachmentslibrary.utils.animate.core.paddings

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.Utils.dpToPx
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.Utils.pxToDp
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.custom.CustomAnimExpectation

class PaddingSetAnimExpectation(
        private val paddingValue: Float,
        private val padding: Padding
) : CustomAnimExpectation() {

    override fun getAnimator(viewToMove: View): Animator? {
        val end = dpToPx(viewToMove.context, paddingValue)
        val start = when(padding) {
            Padding.TOP -> pxToDp(viewToMove.context, viewToMove.paddingTop.toFloat())
            Padding.BOTTOM -> pxToDp(viewToMove.context, viewToMove.paddingBottom.toFloat())
            Padding.RIGHT -> pxToDp(viewToMove.context, viewToMove.paddingRight.toFloat())
            Padding.LEFT -> pxToDp(viewToMove.context, viewToMove.paddingLeft.toFloat())
        }

        return when(padding) {
            Padding.TOP -> ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
                addUpdateListener {
                    viewToMove.updatePadding(top = it.animatedValue as Int)
                }
            }

            Padding.BOTTOM -> ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
                addUpdateListener {
                    viewToMove.updatePadding(bottom = it.animatedValue as Int)
                }
            }

            Padding.RIGHT -> ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
                addUpdateListener {
                    viewToMove.updatePadding(right = it.animatedValue as Int)
                }
            }

            Padding.LEFT -> ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
                addUpdateListener {
                    viewToMove.updatePadding(left = it.animatedValue as Int)
                }
            }
        }
    }

    private fun View.updatePadding(left: Int = paddingLeft,
                                   top: Int = paddingTop,
                                   right: Int = paddingRight,
                                   bottom: Int = paddingBottom) {
        setPadding(left, top, right, bottom)
    }
}