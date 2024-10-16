package ru.mediaattachments.utils.animate.core

import android.animation.Animator
import android.view.View

import ru.mediaattachments.utils.animate.ViewCalculator

abstract class PleaseAnimManager(
    protected val animExpectations: List<AnimExpectation>,
    protected val viewToMove: View,
    protected val viewCalculator: ViewCalculator
) {

    abstract fun getAnimators(): List<Animator>

    abstract fun calculate()

}
