package ru.scheduled.mediaattachmentslibrary.utils.animate.core.scale

import android.view.View

class ScaleAnimExpectationHeight(private var height: Int, gravityHorizontal: Int?, gravityVertical: Int?) : ScaleAnimExpectation(gravityHorizontal, gravityVertical) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        return if (keepRatio) {
            getCalculatedValueScaleY(viewToMove)
        } else null
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {

        if (toDp) {
            this.height = dpToPx(this.height.toFloat(), viewToMove)
        }

        val viewToMoveHeight = viewToMove.height
        return if (this.height == 0 || viewToMoveHeight.toFloat() == 0f) {
            0f
        } else 1f * this.height / viewToMoveHeight
    }
}
