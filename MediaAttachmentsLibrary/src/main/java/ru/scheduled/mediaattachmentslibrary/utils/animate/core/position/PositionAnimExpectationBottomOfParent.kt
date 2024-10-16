package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimExpectation

class PositionAnimExpectationBottomOfParent : PositionAnimExpectation() {

    init {
        isForPositionY = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return null
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        val viewParent = viewToMove.parent
        if (viewParent is View) {
            val parentView = viewParent as View
            return parentView.height.toFloat() - getMargin(viewToMove) - viewCalculator!!.finalHeightOfView(viewToMove)
        }
        return null
    }
}
