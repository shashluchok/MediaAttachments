package ru.mediaattachments.utils.animate.core.scale

import android.view.View


class ScaleAnimExpectationSameScaleAs(otherView: View) : ScaleAnimExpectationViewDependant(otherView, null, null) {

    override fun getCalculatedValueScaleX(viewToMove: View): Float? {
        return otherView.scaleX
    }

    override fun getCalculatedValueScaleY(viewToMove: View): Float? {
        return otherView.scaleY
    }
}
