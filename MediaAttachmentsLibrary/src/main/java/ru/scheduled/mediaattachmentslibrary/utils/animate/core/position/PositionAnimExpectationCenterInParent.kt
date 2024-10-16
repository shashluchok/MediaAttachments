package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimExpectation

class PositionAnimExpectationCenterInParent(var horizontal: Boolean, var vertical: Boolean) : PositionAnimExpectation() {

    init {
        isForPositionX = true
        isForPositionY = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        val viewParent = viewToMove.parent
        if (viewParent is View && horizontal) {
            val parentView = viewParent as View
            return parentView.width / 2f - viewToMove.width / 2f
        }
        return null
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        val viewParent = viewToMove.parent
        if (viewParent is View && vertical) {
            val parentView = viewParent as View
            return parentView.height / 2f - viewToMove.height / 2f
        }
        return null
    }
}
