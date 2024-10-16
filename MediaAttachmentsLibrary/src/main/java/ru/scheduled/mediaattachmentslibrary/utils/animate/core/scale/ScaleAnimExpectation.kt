package ru.scheduled.mediaattachmentslibrary.utils.animate.core.scale

import android.view.View

import ru.scheduled.mediaattachmentslibrary.utils.animate.core.AnimExpectation
import ru.scheduled.mediaattachmentslibrary.utils.animate.core.Utils

abstract class ScaleAnimExpectation(gravityHorizontal: Int?, gravityVertical: Int?) : AnimExpectation() {

    var toDp = false
    var keepRatio = false
    var gravityHorizontal: Int? = null
        private set
    var gravityVertical: Int? = null
        private set

    init {
        if (gravityHorizontal != null) {
            this.gravityHorizontal = gravityHorizontal
        }
        if (gravityVertical != null) {
            this.gravityVertical = gravityVertical
        }
    }

    protected fun dpToPx(value: Float, view: View): Int {
        val v = Utils.dpToPx(view.context, value).toInt()
        toDp = false
        return v
    }

    abstract fun getCalculatedValueScaleX(viewToMove: View): Float?

    abstract fun getCalculatedValueScaleY(viewToMove: View): Float?
}
