package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimationViewDependant

class PositionAnimExpectationCenterBetweenViewAndParent(otherView: View, private val horizontal: Boolean, private val vertical: Boolean, private val toBeOnRight: Boolean, private val toBeOnBottom: Boolean) : PositionAnimationViewDependant(otherView) {

    init {
        isForPositionY = true
        isForPositionX = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        if (horizontal) {
            val viewParent = otherView.parent
            if (viewParent is View && horizontal) {
                val parentView = viewParent as View
                val centerOfOtherView = viewCalculator!!.finalCenterXOfView(otherView)
                if (toBeOnRight) {
                    val parentWidth = parentView.width.toFloat()
                    return (parentWidth + centerOfOtherView) / 2f - viewToMove.width / 2f
                } else {
                    return centerOfOtherView / 2f - viewToMove.width / 2f
                }
            }
        }
        return null
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        if (vertical) {
            val viewParent = viewToMove.parent
            if (viewParent is View && vertical) {
                val parentView = viewParent as View
                val centerOfOtherView = viewCalculator!!.finalCenterYOfView(otherView)
                if (toBeOnBottom) {
                    val parentHeight = parentView.height.toFloat()
                    return parentHeight + centerOfOtherView / 2f - viewToMove.height / 2f
                } else {
                    return centerOfOtherView / 2f - viewToMove.height / 2f
                }
            }
        }
        return null
    }
}
