package ru.mediaattachments.utils.animate.core.custom

import android.animation.Animator
import android.view.View

import ru.mediaattachments.utils.animate.core.AnimExpectation

abstract class CustomAnimExpectation : AnimExpectation() {
    abstract fun getAnimator(viewToMove: View): Animator?
}
