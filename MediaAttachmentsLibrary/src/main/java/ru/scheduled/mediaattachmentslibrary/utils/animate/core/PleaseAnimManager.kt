package ru.scheduled.mediaattachmentslibrary.utils.animate.core

import android.animation.Animator
import android.view.View

import ru.scheduled.mediaattachmentslibrary.utils.animate.ViewCalculator

abstract class PleaseAnimManager(
    protected val animExpectations: List<AnimExpectation>,
    protected val viewToMove: View,
    protected val viewCalculator: ViewCalculator
) {

    abstract fun getAnimators(): List<Animator>

    abstract fun calculate()

}
