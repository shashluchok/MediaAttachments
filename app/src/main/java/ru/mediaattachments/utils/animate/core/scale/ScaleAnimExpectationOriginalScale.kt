package ru.mediaattachments.utils.animate.core.scale

import android.view.View

class ScaleAnimExpectationOriginalScale : ScaleAnimExpectation(null, null) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        return 1f
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {
        return 1f
    }
}
