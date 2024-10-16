package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimExpectation

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
