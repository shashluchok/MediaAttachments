package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.position.PositionAnimExpectation

class PositionAnimExpectationOriginal : PositionAnimExpectation() {
    init {
        isForTranslationX = true
        isForTranslationY = true
    }

    override fun getCalculatedValueX(viewToMove: View): Float? {
        return 0f
    }

    override fun getCalculatedValueY(viewToMove: View): Float? {
        return 0f
    }
}
