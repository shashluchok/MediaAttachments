package ru.scheduled.mediaattachmentslibrary.utils.animate.core.scale

import android.view.View

class ScaleAnimExpectationSameWidthAs(otherView: View, gravityHorizontal: Int?, gravityVertical: Int?) : ScaleAnimExpectationViewDependant(otherView, gravityHorizontal, gravityVertical) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        val viewToMoveWidth = viewToMove.width

        val otherViewWidth = viewCalculator!!.finalWidthOfView(otherView)

        return if (otherViewWidth == 0f || viewToMoveWidth.toFloat() == 0f) {
            0f
        } else otherViewWidth / viewToMoveWidth
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {
        return if (keepRatio) {
            getCalculatedValueScaleX(viewToMove)
        } else null
    }
}
