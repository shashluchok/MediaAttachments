package ru.mediaattachments.utils.animate.core.position

import android.view.View
import ru.mediaattachments.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationBelowOf(otherView: View) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionY = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return null
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return viewCalculator!!.finalPositionBottomOfView(otherView) + getMargin(viewToMove)
    }
}
