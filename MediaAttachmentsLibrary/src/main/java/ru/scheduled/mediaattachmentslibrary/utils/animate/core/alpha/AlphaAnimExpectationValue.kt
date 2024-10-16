package ru.scheduled.mediaattachmentslibrary.utils.animate.core.alpha

import android.view.View
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.alpha.AlphaAnimExpectation

class AlphaAnimExpectationValue(private val alpha: Float) : AlphaAnimExpectation() {
    override fun getCalculatedAlpha(viewToMove: View): Float? {
        return alpha
    }
}
