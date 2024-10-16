package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationRightOf(otherView: View) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionX = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return viewCalculator!!.finalPositionRightOfView(otherView) + getMargin(viewToMove)
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return null
    }
}
