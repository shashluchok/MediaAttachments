package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationAlignRight(otherView: View) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionX = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return viewCalculator!!.finalPositionRightOfView(otherView) - getMargin(viewToMove) - viewToMove.width.toFloat()
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return null
    }
}
