package ru.mediaattachments.utils.animate.core.alpha

import android.view.View

import ru.mediaattachments.utils.animate.core.AnimExpectation

abstract class AlphaAnimExpectation : AnimExpectation() {
    abstract fun getCalculatedAlpha(viewToMove: View): Float?
}
