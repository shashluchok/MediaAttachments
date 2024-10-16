package ru.mediaattachments.utils.animate.core.scale

import android.view.View


class ScaleAnimExpectationWidth(private var width: Int, gravityHorizontal: Int?, gravityVertical: Int?) : ScaleAnimExpectation(gravityHorizontal, gravityVertical) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        if (toDp) {
            this.width = dpToPx(this.width.toFloat(), viewToMove)
        }

        val viewToMoveWidth = viewToMove.width
        return if (this.width == 0 || viewToMoveWidth.toFloat() == 0f) {
            0f
        } else 1f * this.width / viewToMoveWidth
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {
        return if (keepRatio) {
            getCalculatedValueScaleX(viewToMove)
        } else null
    }

}
