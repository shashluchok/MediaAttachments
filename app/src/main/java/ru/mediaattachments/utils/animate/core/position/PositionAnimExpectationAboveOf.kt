package ru.mediaattachments.utils.animate.core.position

import android.view.View
import ru.mediaattachments.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationAboveOf(otherView: View) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionY = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return null
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return viewCalculator!!.finalPositionTopOfView(otherView) - getMargin(viewToMove) - viewToMove.height.toFloat()
    }
}
