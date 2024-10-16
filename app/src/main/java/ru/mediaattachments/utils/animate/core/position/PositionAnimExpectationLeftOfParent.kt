package ru.mediaattachments.utils.animate.core.position

import android.view.View
import ru.mediaattachments.utils.animate.core.position.PositionAnimExpectation

class PositionAnimExpectationLeftOfParent : PositionAnimExpectation() {
    init {
        isForPositionX = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return getMargin(viewToMove)
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return null
    }
}
