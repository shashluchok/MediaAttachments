package ru.scheduled.mediaattachmentslibrary.utils.animate.core.scale

import android.view.View

class ScaleAnimExpectationValues(private val scaleX: Float, private val scaleY: Float, gravityHorizontal: Int?, gravityVertical: Int?) : ScaleAnimExpectation(gravityHorizontal, gravityVertical) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        return scaleX
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {
        return scaleY
    }
}
