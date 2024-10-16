package ru.mediaattachments.utils.animate.core.position

import android.view.View
import ru.mediaattachments.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationLeftOf(otherView: View) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionX = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return viewCalculator!!.finalPositionLeftOfView(otherView) - getMargin(viewToMove) - viewToMove.width.toFloat()
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return null
    }
}
